/*
 * <copyright>
 *  Copyright 2002 Object Services and Consulting, Inc.
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
 * 24 Sep 2002: Created. (OBJS)
 */

package org.cougaar.core.mts;

import org.cougaar.core.mts.acking.*;
import org.cougaar.core.component.ServiceBroker;
import java.net.Socket;

public class RMISendTimeoutAspect extends StandardAspect 
{
  private static final String CONNECT_TIMEOUT = 
    "org.cougaar.message.transport.aspects.RMISendTimeoutAspect.connectTimeout";
  private static final String READ_TIMEOUT = 
    "org.cougaar.message.transport.aspects.RMISendTimeoutAspect.readTimeout";
  private static final String WRITE_TIMEOUT = 
    "org.cougaar.message.transport.aspects.RMISendTimeoutAspect.writeTimeout";
  private static int connectTimeout;
  private int readTimeout;
  private int writeTimeout;
  private RMISocketControlService socCtlSvc;
  private SocketClosingService socCloseSvc;

  public RMISendTimeoutAspect() 
  {}

  public void load() 
  {
    super.load();
    ServiceBroker sb = getServiceBroker();
    socCtlSvc = (RMISocketControlService)sb.getService(this, 
					                         RMISocketControlService.class,
					                         null);
    socCloseSvc = (SocketClosingService)sb.getService(this, 
					                        SocketClosingService.class,
					                        null);
  }

  public Object getDelegate(Object o, Class type)
  {
    if ((type == DestinationLink.class)) {
      DestinationLink link = (DestinationLink)o;
	Class c = link.getProtocolClass();
      // this is only applicable to one of the RMI-based Link Protocols
	if (RMILinkProtocol.class.isAssignableFrom(c))
        return new RMISendTimeoutDestinationLink(link);
    }
    return null;
  }

  static int getConnectTimeout() 
  { 
    connectTimeout = Integer.valueOf(System.getProperty(CONNECT_TIMEOUT,"2000")).intValue();
    return connectTimeout;
  }

  private class RMISendTimeoutDestinationLink extends DestinationLinkDelegateImplBase 
  {
    DestinationLink link;
	
    private RMISendTimeoutDestinationLink(DestinationLink link) 
    {
	super(link);
      this.link = link;
    }
	
    public MessageAttributes forwardMessage (AttributedMessage msg) 
      throws UnregisteredNameException, NameLookupException, 
             CommFailureException, MisdeliveredMessageException
    {
      String s = null;  
      if (loggingService.isDebugEnabled()) {
        s = MessageUtils.getMessageNumber(msg) + "."
            + (MessageUtils.getAck(msg).getSendCount()+1) + " "
            + MessageUtils.getMessageTypeLetter(msg) + "("
            + (MessageUtils.getAck(msg).isSomePureAck() ? String.valueOf(MessageUtils.getSrcMsgNumber(msg)) : "") + ") "
            + MessageUtils.toShortSequenceID(msg) + " via "   
            + AdaptiveLinkSelectionPolicy.getLinkType(link.getProtocolClass().getName());         
        loggingService.debug("forwardMessage: enter " + s);
      }
      
      // set up connect timeout    
      // this is set up in mts.SocketFactory, which calls getConnectTimeout()

      // set up read timeout
      readTimeout = Integer.valueOf(System.getProperty(READ_TIMEOUT,"2000")).intValue();
      if (readTimeout > 0) 
	  socCtlSvc.setSoTimeout(getDestination(), readTimeout);

      // set up write timeout
      writeTimeout = Integer.valueOf(System.getProperty(WRITE_TIMEOUT,"10000")).intValue();
      Socket sock = null;
      if (writeTimeout > 0 && socCloseSvc != null && socCtlSvc != null) {
        MessageAddress addr = getDestination();
        sock = socCtlSvc.getSocket(addr);
        socCloseSvc.scheduleClose(sock, writeTimeout);
      }

      // might want to catch exceptions here after I see what happens
      // should I still copy the message?  I don't think so.
      MessageAttributes attrs = null;
      attrs = link.forwardMessage(msg);
      if (sock != null)
        socCloseSvc.unscheduleClose(sock);
      return attrs;
    }
  }
}
