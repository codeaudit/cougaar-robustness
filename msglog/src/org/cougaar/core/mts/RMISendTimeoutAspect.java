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
 * 10 Mar 2003: Integrate with B10_2 changes to MTS to support RMI timeouts.
 * 24 Sep 2002: Created. (OBJS)
 */

package org.cougaar.core.mts;

import org.cougaar.core.mts.acking.*;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.StateModelException;
import org.cougaar.util.GenericStateModel;
import java.net.Socket;
import java.util.ArrayList;
import java.util.ListIterator;

public class RMISendTimeoutAspect extends StandardAspect 
{
  // eventually, this needs to be self tuning, based on normal measured performance,
  // not declared like this, but hopefully, this will suffice for now.

  private static final String CONNECT_TIMEOUT = 
    "org.cougaar.message.transport.aspects.RMISendTimeoutAspect.connectTimeout";
  private static final String READ_TIMEOUT = 
    "org.cougaar.message.transport.aspects.RMISendTimeoutAspect.readTimeout";
  private static final String WRITE_TIMEOUT = 
    "org.cougaar.message.transport.aspects.RMISendTimeoutAspect.writeTimeout";

  private int connectTimeout;
  private int readTimeout;
  private int writeTimeout;

  private RMISocketControlService socCtlSvc;
  private SocketClosingService socCloseSvc;
  private SocketControlProvisionService policyProvider; //102B
  private RMISocketControlPolicy policy; //102B
  private LoggingService log; //102B

  public RMISendTimeoutAspect() 
  {}

  public void load() 
  {
    super.load();
    ServiceBroker sb = getServiceBroker();
    socCtlSvc = (RMISocketControlService)sb.getService(this, RMISocketControlService.class, null);
    socCloseSvc = (SocketClosingService)sb.getService(this, SocketClosingService.class, null);
    log = (LoggingService)sb.getService(this, LoggingService.class, null); //102B
    policyProvider = (SocketControlProvisionService)sb.getService(this, SocketControlProvisionService.class, null); //102B
    policy = new RMISocketControlPolicy(); //102B
    policyProvider.setPolicy(policy); //102B
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
      // ( this is set up in mts.SocketFactory, which
      //   calls RMISocketControlPolicy.getConnectTimeout() )

      // set up read timeout
      readTimeout = Integer.valueOf(System.getProperty(READ_TIMEOUT,"5000")).intValue();
      if (readTimeout > 0) 
	  socCtlSvc.setSoTimeout(getDestination(), readTimeout);

      // set up write timeout
      writeTimeout = Integer.valueOf(System.getProperty(WRITE_TIMEOUT,"10000")).intValue();
      ArrayList soclist = null;                                            //102B
      if (writeTimeout > 0 && socCloseSvc != null && socCtlSvc != null) {
        MessageAddress addr = getDestination();
        soclist = socCtlSvc.getSocket(addr);                               //102B
        ListIterator socks = soclist.listIterator();                       //102B
        while (socks.hasNext()) {                                          //102B
          Socket sock = (Socket)socks.next();                              //102B
          socCloseSvc.scheduleClose(sock, writeTimeout);    
        }
      }
      MessageAttributes attrs = null;
      
      attrs = link.forwardMessage(msg);
      
      // if it makes it here, then the message was sent without            //102B
      // timing out, so unschedule the timeouts                            //102B
      if (soclist != null) {                                               //102B
        ListIterator socks = soclist.listIterator();                       //102B
        while (socks.hasNext()) {                                          //102B
          Socket sock = (Socket)socks.next();                              //102B
          socCloseSvc.unscheduleClose(sock);
        }
      }

      return attrs;
    }
  }

  public class RMISocketControlPolicy implements SocketControlPolicy
  {
    public int getConnectTimeout(SocketFactory factory, String host, int port) 
    {
      if (!factory.isMTS()) 
      {
        return 0;  //no timeout
      }
      else
      {
        int connectTimeout = Integer.valueOf(System.getProperty(CONNECT_TIMEOUT,"5000")).intValue();
  
        if (log.isDebugEnabled()) {
          log.debug("getConnectTimeout is " +connectTimeout+ 
                    " for " +host+ ':' +port+
                    " mts=" +factory.isMTS()+
                    " ssl=" +factory.usesSSL()
                    //, new Throwable()
                    );
        }
        return connectTimeout;
      }
    }
  
    // the rest are here because SocketControlPolicy is a Component, but I'm not using it that way
    public void initialize() throws StateModelException {}
    public void load() throws StateModelException {}
    public void start() throws StateModelException {}
    public void suspend() throws StateModelException {}
    public void resume() throws StateModelException {}
    public void stop() throws StateModelException {}
    public void halt() throws StateModelException {}
    public void unload() throws StateModelException {}
    public int getModelState() { return GenericStateModel.ACTIVE; }
  }

}
