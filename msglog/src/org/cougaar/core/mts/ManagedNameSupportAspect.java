/*
 * <copyright>
 *  Copyright 2002 Object Services and Consulting, Inc. (OBJS),
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 *
 * CHANGE RECORD 
 * 17 Aug 2002: Created. (OBJS)
 */

package org.cougaar.core.mts;

import java.util.*;

import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;


/**
 *  ManagedNameSupportAspect - wrap NameSupport to enable timing out on certain calls to
 *  the Cougaar name server so we don't get stuck when it is not available or is overloaded.
 */

public class ManagedNameSupportAspect extends StandardAspect
{
  private static final int callTimeout;
  private ThreadService threadService;
  private static final Hashtable addressLookupTable = new Hashtable();

  static
  {
    //  Read external properties

    String s = "org.cougaar.message.transport.aspects.managedNameSupport.callTimeout";
    callTimeout = Integer.valueOf(System.getProperty(s,"500")).intValue();
  }

  public ManagedNameSupportAspect () 
  {}

  public Object getDelegate (Object delegate, Class type) 
  {
    if (type == NameSupport.class) return (new ManagedNameSupport ((NameSupport) delegate));
    return null;
  }

  private boolean doDebug ()
  {
    return (loggingService != null && loggingService.isDebugEnabled());
  }

  private void debug (String s)
  {
    if (loggingService != null) loggingService.debug (s);
  }

  private ThreadService threadService () 
  {
    if (threadService != null) return threadService;
    threadService = (ThreadService) getServiceBroker().getService (this, ThreadService.class, null);
    return threadService;
  }

  private class ManagedNameSupport extends NameSupportDelegateImplBase 
  {
    private NameSupport nameSupport;
    
    private ManagedNameSupport (NameSupport nameSupport)
    {
      super (nameSupport); 
      this.nameSupport = nameSupport;
    }

    public MessageAddress getNodeMessageAddress () 
    {
      if (doDebug()) debug ("getNodeMessageAddress() called");
      MessageAddress addr = nameSupport.getNodeMessageAddress();
      if (doDebug()) debug ("getNodeMessageAddress() returned");
      return addr;
    }

    public void registerAgentInNameServer (Object proxy, MessageAddress address, String transportType)
    {
      if (doDebug()) debug ("registerAgentInNameServer() called: address="+address+" transportType="+transportType);
      nameSupport.registerAgentInNameServer (proxy, address, transportType);
      if (doDebug()) debug ("registerAgentInNameServer() returned");
    }

    public void unregisterAgentInNameServer (Object proxy, MessageAddress address, String transportType)
    {
      if (doDebug()) debug ("unregisterAgentInNameServer() called: address="+address+" transportType="+transportType);
      nameSupport.unregisterAgentInNameServer(proxy, address, transportType);
      if (doDebug()) debug ("Finshed calling unregisterAgentInNameServer() returned");
    }

    public void registerMTS (MessageAddress address) 
    {
      if (doDebug()) debug ("registerMTS() called: address=" +address);
      nameSupport.registerMTS (address);
      if (doDebug()) debug ("registerMTS() returned");
    }

    public Object lookupAddressInNameServer (MessageAddress address, String transportType)
    {
      if (doDebug()) 
      {
        debug 
        (
          "timed lookupAddressInNameServer() (timout=" +callTimeout+ "ms): address=" +
          address+ " transportType=" +transportType
        );
      }

      //  Make the lookup call in another thread

      AddressLookup addressLookup = new AddressLookup (nameSupport, address, transportType);
      String name = "AddressLookup_" +address+ "," + transportType;
      Schedulable thread = threadService().getThread (this, addressLookup, name);
      if (doDebug()) debug ("starting thread to lookup name " + name);
      thread.start();

      //  Wait till we get the address lookup or we time out

      final int POLL_TIME = 100;
      long callDeadline = now() + callTimeout;
      Object lookup = null;
      boolean hadException = false;
      boolean timedOut = false;

      while (true)
      {
        if (addressLookup.isFinished()) 
        {
          hadException = addressLookup.hadException();
          lookup = addressLookup.getLookup();
          break;
        }

        try { Thread.sleep (POLL_TIME); } catch (Exception e) {}

        if (now() > callDeadline) 
        {
          timedOut = true;
          break;
        }
      }

      //  If the call failed or timed out, try a value from our cache, else set the cache

      if (hadException || timedOut) 
      {
        lookup = getCachedAddressLookup (address, transportType);
        String s = (hadException ? "had exception" : "timed out");
        if (doDebug()) debug ("timed lookupAddressInNameServer() "+s+", using value from cache: " +lookup);
      }
      else 
      {
        if (doDebug()) debug ("timed lookupAddressInNameServer() completed on time");
        cacheAddressLookup (address, transportType, lookup);
      }

      if (doDebug()) debug ("timed lookupAddressInNameServer() finished");
      return lookup;
    }

    public Iterator lookupMulticast (MulticastMessageAddress address) 
    {
      if (doDebug()) debug ("lookupMulticast() called: address=" +address);
      Iterator iter = nameSupport.lookupMulticast (address);
      if (doDebug()) debug ("lookupMulticast() returned");
      return iter;
    }
  }

  private Object getCachedAddressLookup (MessageAddress address, String transportType)
  {
    synchronized (addressLookupTable)
    {
      String key = address + transportType;
      return addressLookupTable.get (key);
    }
  }

  private void cacheAddressLookup (MessageAddress address, String transportType, Object lookup)
  {
    synchronized (addressLookupTable)
    {
      String key = address + transportType;
      if (lookup != null) addressLookupTable.put (key, lookup);
      else addressLookupTable.remove (key);
    }
  }

  private class AddressLookup implements Runnable
  {
    private NameSupport nameSupport;
    private MessageAddress address;
    private String transportType;
    private Object lookup;
    private Exception exception;
    private boolean callFinished;

    public AddressLookup (NameSupport nameSupport, MessageAddress address, String transportType)
    {
      this.nameSupport = nameSupport;
      this.address = address;
      this.transportType = transportType;
    }

    public void run ()
    {
      lookup = null;
      exception = null;
      callFinished = false;

      try
      {
        if (doDebug()) debug ("timed lookupAddressInNameServer() called: address="+address+" transportType="+transportType);
        lookup = nameSupport.lookupAddressInNameServer (address, transportType);
        if (doDebug()) debug ("timed lookupAddressInNameServer() returned without exception");
      }
      catch (Exception e)
      {
        if (doDebug()) debug ("timed lookupAddressInNameServer() exception: " +stackTraceToString(e));
        exception = e;
      }

      callFinished = true;
    }

    public boolean isFinished ()
    {
      return callFinished;
    }

    public boolean hadException ()
    {
      return exception != null;
    }

    public Exception getException ()
    {
      return exception;
    }

    public Object getLookup ()
    {
      return lookup;
    }
  }

  private static long now ()
  {
    return System.currentTimeMillis();
  }

  private String stackTraceToString (Exception e)
  {
    java.io.StringWriter stringWriter = new java.io.StringWriter();
    java.io.PrintWriter printWriter = new java.io.PrintWriter (stringWriter);
    e.printStackTrace (printWriter);
    return stringWriter.getBuffer().toString();
  }
}
