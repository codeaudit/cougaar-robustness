/*
 * <copyright>
 *  Copyright 2002-2004 Object Services and Consulting, Inc. (OBJS),
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
 * 13 Mar  2003: Integrate with B10_2 changes to NameSupport to support WhitePages timeouts [Bug 2545]. 
 * 13 Feb  2003: Port to 10.0 (OBJS)
 *  5 Sept 2002: Created. (OBJS)
 */

package org.cougaar.mts.std;

import java.net.URI;
import java.util.Iterator;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MulticastMessageAddress;
import org.cougaar.mts.base.NameSupport;
import org.cougaar.mts.base.NameSupportDelegateImplBase;
import org.cougaar.mts.base.StandardAspect;

public class NameSupportTimeoutAspect extends StandardAspect
{
  private static final String timeoutProp = 
    "org.cougaar.message.transport.aspects.NameSupportTimeoutAspect.timeout";
  //102B private boolean timeoutp;
  //102B private int timeout;
  //102B private Cache cache = null;
  private boolean calledOnce = false;

  public NameSupportTimeoutAspect() {}
 
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
    //102B } else if (type == DestinationLink.class) {
    //102B   return new Link ((DestinationLink)delegate);
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
    //102B private Cache cache;
    
    private NameSupportDelegate(NameSupport nameSupport)
    {
      super(nameSupport); 
      this.nameSupport = nameSupport;
      //102B if (cache == null)
      //102B  cache = new Cache(new NameSupportFetcher(nameSupport), "NameSupport");
    }

    public MessageAddress getNodeMessageAddress () 
    {
      if (doDebug()) debug ("getNodeMessageAddress() called");
      MessageAddress addr = nameSupport.getNodeMessageAddress();
      if (doDebug()) debug ("getNodeMessageAddress() returned");
      return addr;
    }

    public void registerAgentInNameServer (URI uri, MessageAddress address, //100
      String transportType)
    {
      if (doDebug()) debug ("registerAgentInNameServer() called: URI="+uri+ //100
                            " address="+address+" transportType="+transportType);
      nameSupport.registerAgentInNameServer(uri, address, transportType); //100
      if (doDebug()) debug ("registerAgentInNameServer() returned");
    }

    public void unregisterAgentInNameServer (URI uri, MessageAddress address, String transportType) //100
    {
      if (doDebug()) debug ("unregisterAgentInNameServer() called: URI="+uri+ //100
                            " address="+address+" transportType="+transportType);
      nameSupport.unregisterAgentInNameServer(uri, address, transportType); //100
      if (doDebug()) debug ("Finshed calling unregisterAgentInNameServer() returned");
    }

/* registerMTS dropped in 1043
    public void registerMTS (MessageAddress address) 
    {
      if (doDebug()) debug ("registerMTS() called: address=" +address);
      nameSupport.registerMTS (address);
      if (doDebug()) debug ("registerMTS() returned");
    }
*/

    public Iterator lookupMulticast (MulticastMessageAddress address) 
    {  
      if (doDebug()) debug ("lookupMulticast() called: address=" +address);
      Iterator iter = nameSupport.lookupMulticast (address);
      if (doDebug()) debug ("lookupMulticast() returned");
      return iter;
    }

    //102B
    public URI lookupAddressInNameServer (MessageAddress address, String transportType, int timeout)
    {
      int newTimeout = Integer.valueOf(System.getProperty(timeoutProp,"1000")).intValue();

      //if timeout is 0 (i.e no timeout), use our timeout, else use shortest timeout.
      if ((timeout != 0) && (timeout < newTimeout)) newTimeout = timeout;                              
         
      if (doDebug()) 
        debug("lookupAddressInNameServer address="+address+
              ",transportType="+transportType+
              ",timeout="+timeout+"ms,newTimeout="+newTimeout+"ms");

      return nameSupport.lookupAddressInNameServer(address,transportType,newTimeout);
    }

    public URI lookupAddressInNameServer (MessageAddress address, String transportType) //100
    {
      int timeout = Integer.valueOf(System.getProperty(timeoutProp,"1000")).intValue();
         
      if (doDebug()) 
        debug("lookupAddressInNameServer address="+address+
              ",transportType="+transportType+",timeout="+timeout+"ms");
      
      //102B
      //switch to the version of lookupAddressInNameServer with timeout
      //null is returned if entry isn't found in cache and isn't fetched before timeout
      return nameSupport.lookupAddressInNameServer(address,transportType,timeout);
    }
  }

/* //102B  The rest of this file now obsolete due to replacement of NamingService with 
           WhitePagesService and changes to NameSupport in response to [Bug 2545].  
           NameSupport.lookupAddressInNameServer now supports timeout, which it passes
           through to WhitePagesService, which now times out and caches.  
           
      NameSupportKey key = new NameSupportKey(address,transportType);
      Cache.CacheEntry entry = cache.fetch(key, false, timeout);
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
      if ((value != null) && !(value instanceof URI)) { //100
        loggingService.error("lookupAddressInNameServer returned non-URI Object="+value+", null substituted."); //100
        value = null; //100
      } 
      return (URI)value;
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
*/ //102B

}
