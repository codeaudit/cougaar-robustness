/*
 * <copyright>
 *  Copyright 2002-2003 Object Services and Consulting, Inc. (OBJS),
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
 * Dec 16 2002: Created. (OBJS)
 * Mar 07 2003: Ported to 10.2
 */

package org.cougaar.core.mts;

import org.cougaar.core.mts.logging.*;

import org.cougaar.core.service.LoggingService;

import java.net.URI;

/**
 **  Emit logging entries that are used to audit Cougaar messaging.
 **  This aspect logs when a Cougaar message first arrives at
 **  at the SendLink station and when it leaves the ReceiveLink station.
 **  This aspect should be the first(inner) aspect if it is expected to
 **  audit all MTS messaging, but can be repositioned to audit specific
 **  situations.  A warning is logged if it is not the inner aspect.
 *
 * This class takes two system properties: <p>
 *
 * - org.cougaar.message.transport.aspects.messageaudit.includeLocalMsgs <p>
 * Which controls whether local messages are also included.<p>
 * - org.cougaar.message.transport.aspects.messageaudit.filterPingsHBs<p>
 * Which controls whether heartbeats & pings are included. Default is <b>true</b>.
 *
 *
 **/
public class MessageAuditAspect extends StandardAspect implements AttributeConstants {
  private static final String BEFORE_SENDLINK_TAG = "Before:SendLink.sendMessage";
  private static final String BEFORE_SENDQ_TAG = "Before:SendQueue.sendMessage";
  private static final String BEFORE_ROUTER_TAG = "Before:Router.routeMessage";
  private static final String BEFORE_DESTQ_HOLD_TAG = "Before:DestinationQueue.holdMessage";
  private static final String BEFORE_DESTQ_DISPATCH_TAG = "Before:DestinationQueue.dispatchNextMessage";
  private static final String BEFORE_DESTLINK_TAG = "Before:DestinationLink.forwardMessage";
  private static final String BEFORE_MSGWRITER_FINOUT_TAG = "Before:MessageWriter.finishOutput";
  private static final String AFTER_MSGREADER_FINATTR_TAG = "After:MessageReader.finalizeAttributes";
  private static final String BEFORE_MSGDELIVERER_TAG = "After:BeforeMessageDeliverer.deliverMessage";
  private static final String AFTER_MSGDELIVERER_TAG = "After:MessageDeliverer.deliverMessage";
  private static final String AFTER_RECVLINK_TAG = "After:ReceiveLink.deliverMessage";
  private static boolean includeLocalMsgs;
  private static boolean enableFilter;
  private LoggingService log;
  private MessageTransportRegistryService reg;
  //private static final int SEND = 0;
  //private static final int RECEIVE = 1;
  private int seqnum = 0; 

  private LogEventRouter eventRouter = null;

  private static MessageAuditAspect thisIns = null;
 
  static {     
    //  Read external properties
    String s = "org.cougaar.message.transport.aspects.messageaudit.includeLocalMsgs";
    includeLocalMsgs = Boolean.valueOf(System.getProperty(s,"true")).booleanValue();

    s = "org.cougaar.message.transport.aspects.messageaudit.filterPingsHBs";
    enableFilter = Boolean.valueOf(System.getProperty(s,"true")).booleanValue();
    
  }

  
  /*
   * Initializer
   */
  public MessageAuditAspect () {}

  /*
   * Initialization method
   */
  public void load () {
    super.load();
    log = loggingService;
    reg = getRegistry();
    thisIns = this;
    
    eventRouter = new LogEventRouter(log);
    
    if ( !log.isInfoEnabled() ) {
        log.warn("** INFO logging level not enabled, this aspect is not active **");        
    } else {
        if ( enableFilter ) {
            log.info("** Filtering enabled: Filtering out pings and heartbeats **");        
        }
        if ( includeLocalMsgs ) {
            log.info("** Configured to ignore local messages **");        
        }
    }
    
  }

  /*
   * This method returns the proper delegate instance 
   *
   * @return delegate instance
   */
  public Object getDelegate (Object delegate, Class type) {
    if (type == SendLink.class) {
      return (new SendLinkDelegate((SendLink)delegate));
    } else if (type == SendQueue.class) {
      return new SendQueueDelegate((SendQueue)delegate);
    } else if (type == Router.class) {
      return new RouterDelegate((Router)delegate);
    } else if (type == DestinationQueue.class) {
      return new DestinationQueueDelegate((DestinationQueue)delegate);
    } else if (type == DestinationLink.class) {
      return new DestinationLinkDelegate((DestinationLink)delegate);
    } else if (type == MessageDeliverer.class) {
      return new MessageDelivererDelegate((MessageDeliverer)delegate);
    } else if (type == ReceiveLink.class) {
      return (new ReceiveLinkDelegate((ReceiveLink)delegate));
//    } else if (type == NameSupport.class) {
//      return new NameSupportDelegate((NameSupport)delegate);
    } else if (type == MessageReader.class) {
      return new MessageReaderDelegate((MessageReader)delegate);
    } else if (type == MessageWriter.class) {
      return new MessageWriterDelegate((MessageWriter)delegate);
    } else {
      return null;
    }
  }

  /*
   * SendLinkDelegate aspect extension
   */
  private class SendLinkDelegate extends SendLinkDelegateImplBase {
    private SendLinkDelegate(SendLink link) {
      super(link);
    }
    
    /*
     * This method appends data about the sender into the AttributedMessage since the
     * receiver may not have access to detailed node/incarnation information about
     * the sender.
     *
     * @param msg AttributeMessage being sent
     *
     */
    public void sendMessage (AttributedMessage msg) {
      boolean doit = true;
      if (!includeLocalMsgs && reg.isLocalClient(msg.getTarget())) 
        doit = false;
      if (doit && log.isInfoEnabled() && !filter(msg) ) {
	  MessageAddress fromAgent = msg.getOriginator();
          AgentID agentID = null; 
          String fromNode = null;
          long fromIncarnation = -1;
          try {
              agentID = AgentID.getAgentID (MessageAuditAspect.this, MessageAuditAspect.this.getServiceBroker(), fromAgent);
              fromNode = agentID.getNodeName();
              fromIncarnation = agentID.getAgentIncarnationAsLong();          
          } catch (UnregisteredNameException nle) {
              fromNode = "NameLookupException_for_"+fromAgent;
              log.warn("UnregisteredNameException looking up name for "+fromAgent+"\n"+nle);
          } catch (Exception e) {
              fromNode = "Exception_for_"+fromAgent;
              log.warn("Exception looking up name for "+fromAgent+"\n"+e);
          }          
          msg.setAttribute(Constants.AUDIT_ATTRIBUTE_NUMSEQ, new Integer(seqnum++));
          msg.setAttribute(Constants.AUDIT_ATTRIBUTE_FROM_INCARNATION, new Long(fromIncarnation));
          msg.setAttribute(Constants.AUDIT_ATTRIBUTE_FROM_NODE, fromNode);
          eventRouter.routeEvent(createAuditData(BEFORE_SENDLINK_TAG, msg, true));
      }
      super.sendMessage (msg);
    }
  }

  /*
   * SendQueueDelegate aspect extension
   */
  public class SendQueueDelegate extends SendQueueDelegateImplBase {
    public SendQueueDelegate (SendQueue queue) {
      super(queue);
    }
	
    public void sendMessage(AttributedMessage msg) {
      boolean doit = true;
      if (!includeLocalMsgs && reg.isLocalClient(msg.getTarget()) ) 
        doit = false;
      if (doit && log.isInfoEnabled() && !filter(msg) )
        eventRouter.routeEvent(createAuditData(BEFORE_SENDQ_TAG, msg, true));
      super.sendMessage(msg);
    }
  }

  /*
   * RouterDelegate aspect extension
   */
  public class RouterDelegate extends RouterDelegateImplBase {
    public RouterDelegate (Router router) {
      super(router);
    }

    public void routeMessage(AttributedMessage msg) {
      boolean doit = true;
      if (!includeLocalMsgs && reg.isLocalClient(msg.getTarget())) 
        doit = false;
      if (doit && log.isInfoEnabled() && !filter(msg) )
        eventRouter.routeEvent(createAuditData(BEFORE_ROUTER_TAG, msg, true));
      super.routeMessage(msg);
    }
  }

  /*
   * DestinationQueueDelegate aspect extension
   */
  public class DestinationQueueDelegate extends DestinationQueueDelegateImplBase {
    DestinationQueue queue;
    public DestinationQueueDelegate (DestinationQueue queue) {
      super(queue);
      this.queue = queue;
    }

    public void holdMessage(AttributedMessage msg) {
      boolean doit = true;
      if (!includeLocalMsgs && reg.isLocalClient(msg.getTarget())) 
        doit = false;
      if (doit && log.isInfoEnabled() && !filter(msg) )
        eventRouter.routeEvent(createAuditData(BEFORE_DESTQ_HOLD_TAG, msg, true, "q", queue.getDestination().toString()));
      super.holdMessage(msg);
    }

    public void dispatchNextMessage(AttributedMessage msg) {
      boolean doit = true;
      if (!includeLocalMsgs && reg.isLocalClient(msg.getTarget())) 
        doit = false;
      if (doit && log.isInfoEnabled() && !filter(msg) )
        eventRouter.routeEvent(createAuditData(BEFORE_DESTQ_DISPATCH_TAG, msg, true, "q", queue.getDestination().toString()));
      super.dispatchNextMessage(msg);
   }
  }
 
  /*
   * DestinationLinkDelegate aspect extension
   */
  public class DestinationLinkDelegate extends DestinationLinkDelegateImplBase {
    DestinationLink link;
    public DestinationLinkDelegate (DestinationLink link) {
      super(link);
      this.link = link;
    }

    public MessageAttributes forwardMessage(AttributedMessage msg) 
      throws UnregisteredNameException, 
             NameLookupException, 
             CommFailureException,
             MisdeliveredMessageException {
      boolean doit = true;
      if (!includeLocalMsgs && reg.isLocalClient(msg.getTarget())) 
        doit = false;
      if (doit && log.isInfoEnabled() && !filter(msg) )
        eventRouter.routeEvent(createAuditData(BEFORE_DESTLINK_TAG, msg, true, "link", link.getProtocolClass().toString()));
      return super.forwardMessage(msg);
    }
  }

  /*
   * MessageWriterDelegate aspect extension
   */
  public class MessageWriterDelegate extends MessageWriterDelegateImplBase {
    AttributedMessage msg;

    MessageWriterDelegate(MessageWriter delegate) {
      super(delegate);
    }

    public void finalizeAttributes(AttributedMessage msg) {
      super.finalizeAttributes(msg);
      this.msg = msg;
    }
    
    public void finishOutput() throws java.io.IOException {
      boolean doit = true;
      if (!includeLocalMsgs && reg.isLocalClient(msg.getTarget())) 
        doit = false;
      if (doit && log.isInfoEnabled() && !filter(msg) )
        eventRouter.routeEvent(createAuditData(BEFORE_MSGWRITER_FINOUT_TAG, msg, true));
      super.finishOutput();
    }
  }

  /*
   * MessageReaderDelegate aspect extension
   */
  public class MessageReaderDelegate extends MessageReaderDelegateImplBase {
    MessageReaderDelegate(MessageReader delegate) {
      super(delegate);
    }

    public void finalizeAttributes(AttributedMessage msg) {
      super.finalizeAttributes(msg);
      boolean doit = true;
      if (!includeLocalMsgs && reg.isLocalClient(msg.getTarget())) 
        doit = false;
      if (doit && log.isInfoEnabled() && !filter(msg) )
        eventRouter.routeEvent(createAuditData(AFTER_MSGREADER_FINATTR_TAG, msg, false));
    }
  }

  /*
   * MessageDelivererDelegate aspect extension
   */
  public class MessageDelivererDelegate extends MessageDelivererDelegateImplBase {
    public MessageDelivererDelegate (MessageDeliverer deliverer) {
      super(deliverer);
    }
	
    public MessageAttributes deliverMessage(AttributedMessage msg, MessageAddress dest) 
      throws MisdeliveredMessageException 
    {
      MessageAttributes attrs = super.deliverMessage(msg, dest);
      boolean doit = true;
      if (!includeLocalMsgs && reg.isLocalClient(msg.getOriginator()))
        doit = false;
      if (doit && log.isInfoEnabled() && !filter(msg) )
        eventRouter.routeEvent(createAuditData(AFTER_MSGDELIVERER_TAG, msg, false, "dest", dest.toString()));
      return attrs;
    }  
  }

  /*
   * ReceiveLinkDelegate aspect extension
   */
  private class ReceiveLinkDelegate extends ReceiveLinkDelegateImplBase {
    private ReceiveLinkDelegate(ReceiveLink link) {
      super(link); 
    }
    public MessageAttributes deliverMessage (AttributedMessage msg) {
      MessageAttributes attrs = super.deliverMessage(msg);
      boolean doit = true;
      if (!includeLocalMsgs && reg.isLocalClient(msg.getOriginator()))
        doit = false;
      if (doit && 
          log.isInfoEnabled() &&
          !filter(msg) &&
          attrs != null &&
          attrs.getAttribute(DELIVERY_ATTRIBUTE).equals(DELIVERY_STATUS_DELIVERED))
        eventRouter.routeEvent(createAuditData(AFTER_RECVLINK_TAG, msg, false));
      return attrs;
    }
  }

  /*
   * @param tag The overall XML tag used to bound this event data
   * @param msg The attributed message
   * @param sendp TRUE if this msg is being sent, FALSE if it is being received
   * @return A LogEventWrapper to emit
   */
  private LogEventWrapper createAuditData (String tag, AttributedMessage msg, boolean sendp) {
    return createAuditData(tag, msg, sendp, null, null);
  }

  //static int test =1;
  /*
   * @param tag The overall XML tag used to bound this event data
   * @param msg The attributed message
   * @param sendp TRUE if this msg is being sent, FALSE if it is being received
   * @param key unused
   * @param val unused
   * @return A LogEventWrapper to emit
   */
  private LogEventWrapper createAuditData (String tag, AttributedMessage msg, boolean sendp, String key, String val) {

	MessageAddress fromAgent = msg.getOriginator();
	MessageAddress toAgent = msg.getTarget();

	String to   = null;

	if (sendp) { //Get Sender data
           to = toAgent.toString();
	} else { //This is an incoming msg - Grab node/incarnation # for sender from msg
            try {
                AgentID agent = AgentID.getAgentID (this, this.getServiceBroker(), toAgent);
                to = agent.getNodeName() + "." + agent.getAgentName() + "." + agent.getAgentIncarnation();          
            } catch (UnregisteredNameException nle) {
                to = "NameLookupException."+toAgent.toString();
            } catch (Exception e) {
                to = e.toString()+toAgent;
            }
	}
  
        String from = (String)msg.getAttribute(Constants.AUDIT_ATTRIBUTE_FROM_NODE) + "." + 
 	  fromAgent + "." + 
	  (Long)msg.getAttribute(Constants.AUDIT_ATTRIBUTE_FROM_INCARNATION); 

        Integer num = (Integer)msg.getAttribute(Constants.AUDIT_ATTRIBUTE_NUMSEQ) ;
        String numS;
        numS = (num==null)? "null" : num.toString(); //num HAS been null before **

        String msgtype = (String) msg.getAttribute (org.cougaar.core.mts.Constants.MSG_TYPE);
        if (msgtype == null) {
            msgtype = "UNTYPED";
        }
        String[] data = {"TYPE", "TRAFFIC_EVENT", "lpName", tag, "time", ""+now(), "from", from, "to", to, "num", numS, "msgtype", msgtype };
        return new LogEventWrapper(log, LoggingService.DEBUG, data, null, "LP");
  }

  
  private boolean filter(AttributedMessage msg) {
      
      if (!enableFilter) return false; // do not filter
      
      String type = (String) msg.getAttribute (org.cougaar.core.mts.Constants.MSG_TYPE);
      if ( type == null ) { // no idea what type so do not filter.
          return false;
      }
      
      if (type.equals (org.cougaar.core.mts.Constants.MSG_TYPE_HEARTBEAT) || 
          type.equals (org.cougaar.core.mts.Constants.MSG_TYPE_PING) ||
          type.equals (org.cougaar.core.mts.Constants.MSG_TYPE_TRAFFIC_MASK) ||
          type.equals (org.cougaar.core.mts.Constants.MSG_TYPE_PURE_ACK) ||
          type.equals (org.cougaar.core.mts.Constants.MSG_TYPE_PURE_ACK_ACK) ) {
              return true;
      }
     
      return false;
      
  }
  

  /*
   * @return current time in milliseconds
   */
  private static long now () {
    return System.currentTimeMillis();
  }
  
}
