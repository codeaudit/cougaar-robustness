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
 * 5 Sept 2002: Created. (OBJS)
 */

package org.cougaar.core.mts;

import java.util.*;
//import org.cougaar.core.topology.*;

public class NameSupportCacheAspect extends StandardAspect
{
  private static final String callTimeoutProp = 
    "org.cougaar.message.transport.aspects.NameSupportCacheAspect.callTimeout";
  private boolean timeoutp;
  private int callTimeout;
  private Cache cache = null;
  private boolean calledOnce = false;

  public NameSupportCacheAspect() {}
 
  public Object getDelegate (Object delegate, Class type) 
  {
    if (type == NameSupport.class) {
      if (calledOnce) {
        loggingService.fatal("getDelegate called more than once. delegate="+delegate+
                             ",type="+type, new RuntimeException());
      } else {
        calledOnce = true;
      }
      return (new NameSupportDelegate((NameSupport)delegate));
    } else if (type == DestinationLink.class) {
      return new Link ((DestinationLink)delegate);
    }
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

  private class NameSupportDelegate extends NameSupportDelegateImplBase 
  {
    private NameSupport nameSupport;
    //private Cache cache;
    
    private NameSupportDelegate(NameSupport nameSupport)
    {
      super(nameSupport); 
      this.nameSupport = nameSupport;
      if (cache == null)
        cache = new Cache(new NameSupportFetcher(nameSupport), "NameSupport");
    }

    public MessageAddress getNodeMessageAddress () 
    {
      if (doDebug()) debug ("getNodeMessageAddress() called");
      MessageAddress addr = nameSupport.getNodeMessageAddress();
      if (doDebug()) debug ("getNodeMessageAddress() returned");
      return addr;
    }

    public void registerAgentInNameServer (Object proxy, MessageAddress address,
      String transportType)
    {
      if (doDebug()) debug ("registerAgentInNameServer() called: address="+
        address+" transportType="+transportType);
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
      callTimeout = Integer.valueOf(System.getProperty(callTimeoutProp,"1000")).intValue();
         
      if (doDebug()) 
        debug("lookupAddressInNameServer address="+address+
              ",transportType="+transportType+",timeout="+callTimeout+"ms");
      
      NameSupportKey key = new NameSupportKey(address,transportType);
      Cache.CacheEntry entry = cache.fetch(key, false, callTimeout);
      Object value = null;
      synchronized (entry) {
        if (entry.hasValue()) {
          value = entry.getValue();
          if (entry.isStale()) {
            if (doDebug()) 
              debug("stale value was fetched addr="+address+",transportType="+transportType);
          } else {
            if (doDebug()) 
              debug("current value was fetched addr="+address+",transportType="+transportType);
          }
        } else {
          if (doDebug()) 
            debug("no value was fetched addr="+address+",transportType="+transportType);
        }
      }
      debug("returned value="+value+", for addr="+address+",transportType="+transportType);
      return value;
    }

    public Iterator lookupMulticast (MulticastMessageAddress address) 
    {  
      if (doDebug()) debug ("lookupMulticast() called: address=" +address);
      Iterator iter = nameSupport.lookupMulticast (address);
      if (doDebug()) debug ("lookupMulticast() returned");
      return iter;
    }
  }
   
  private String stackTraceToString (Exception e) 
  {
    java.io.StringWriter stringWriter = new java.io.StringWriter();
    java.io.PrintWriter printWriter = new java.io.PrintWriter (stringWriter);
    e.printStackTrace (printWriter);
    return stringWriter.getBuffer().toString();
  }
  
  public class NameSupportKey 
  {
    private MessageAddress addr;
    private String transportType;
           
    public NameSupportKey(MessageAddress addr, String transportType)
    {
      if (addr == null || transportType == null)
        if (loggingService.isWarnEnabled())
          loggingService.warn("addr or transport type are null",new Exception());
      this.addr = addr;
      this.transportType = transportType;
    }
                 
    public MessageAddress getAddr() { return addr; }
    public String getTransportType() { return transportType; }
          
    public boolean equals(Object o) 
    {
      if (o == this) return true;
      if (!(o instanceof NameSupportKey)) return false;
      NameSupportKey key = (NameSupportKey)o; 
      if (key.getAddr().equals(addr) 
          && key.getAddr().equals(addr)) 
        return true;
      return false;
    }

    public int hashCode() {  return addr.hashCode()+transportType.hashCode(); }

    public String toString() { return addr.getAddress()+transportType; }
  }

  public class NameSupportFetcher implements Cache.Fetcher
  {
    private final NameSupport backer;

    public NameSupportFetcher(NameSupport backer) 
    {
      this.backer = backer;
    }
        
    public Object fetch(Object o) 
    {
      if (o instanceof NameSupportKey) 
      {
        NameSupportKey key = (NameSupportKey)o;
        return backer.lookupAddressInNameServer(key.getAddr(), 
			          key.getTransportType());
      }
      if (loggingService.isWarnEnabled())
        loggingService.warn("Illegal object = "+o+" returned from fetch",new Exception());
      return null;
    }
  }

  private class Link extends DestinationLinkDelegateImplBase 
  {
    DestinationLink link;

    private Link (DestinationLink link) 
    {
      super (link);
      this.link = link;
    }
            
    public MessageAttributes forwardMessage (AttributedMessage message) 
      throws UnregisteredNameException, NameLookupException, 
             CommFailureException, MisdeliveredMessageException
    {
      MessageAttributes attrs = null;
      try {
        attrs = link.forwardMessage (message);
      } catch (UnregisteredNameException une) {
        if (doDebug()) debug("forwardMessage: caught exception="+une);
        dirty(message);
        throw une;
      } catch (NameLookupException nle) {
        if (doDebug()) debug("forwardMessage: caught exception="+nle);
        dirty(message);
        throw nle;
      } catch (CommFailureException cfe) {
        if (doDebug()) debug("forwardMessage: caught exception="+cfe);
        dirty(message);
        throw cfe;
      } catch (MisdeliveredMessageException mme) {
        if (doDebug()) debug("forwardMessage: caught exception="+mme);
        dirty(message);
        throw mme;
      } catch (Exception e) {
        if (doDebug()) debug("forwardMessage: caught exception="+e);
        dirty(message);
        throw new CommFailureException(e);
      }
      return attrs;
    }

    private void dirty(AttributedMessage message) {
      MessageAddress address = message.getTarget();
      Class protocolClass = link.getProtocolClass();
      String protocolType = null;
      if (protocolClass.equals(RMILinkProtocol.class))
        protocolType = "-RMI";
      else if (protocolClass.equals(SSLRMILinkProtocol.class))
        protocolType = "-SSLRMI";
      else if (protocolClass.equals(SerializedRMILinkProtocol.class))
        protocolType = "-SerializedRMI";
      else
        try {
          protocolType = (String)protocolClass.getField("PROTOCOL_TYPE").get(null);  
        } catch (Exception ee) { ee.printStackTrace(); }
      NameSupportKey key = new NameSupportKey(address,protocolType);
      if (doDebug())
        debug("calling Cache.dirty on key="+key);
      cache.dirty(key);
    }
  }
}
