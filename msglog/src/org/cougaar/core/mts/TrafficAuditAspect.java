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
 */

package org.cougaar.core.mts;

import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.TopologyReaderService;
import org.cougaar.core.service.TopologyEntry;

/**
 **  Emit logging entries that are used to audit Cougaar messaging.
 **  This aspect logs when a Cougaar message first arrives at
 **  at the SendLink station and when it leaves the ReceiveLink station.
 **  This aspect should be the first(inner) aspect if it is expected to
 **  audit all MTS messaging, but can be repositioned to audit specific
 **  situations.  A warning is logged if it is not the inner aspect.
 **/
public class TrafficAuditAspect extends StandardAspect implements AttributeConstants {
  private static final String BEFORE_SENDLINK_TAG = "Before:SendLink.sendMessage";
  private static final String BEFORE_SENDQ_TAG = "Before:SendQueue.sendMessage";
  private static final String BEFORE_ROUTER_TAG = "Before:Router.routeMessage";
  private static final String BEFORE_DESTQ_HOLD_TAG = "Before:DestinationQueue.holdMessage";
  private static final String BEFORE_DESTQ_DISPATCH_TAG = "Before:DestinationQueue.dispatchNextMessage";
  private static final String BEFORE_DESTLINK_TAG = "Before:DestinationLink.forwardMessage";
  private static final String BEFORE_MSGWRITER_FINOUT_TAG = "Before:MessageWriter.finishOutput";
  private static final String AFTER_MSGREADER_FINATTR_TAG = "After:MessageReader.finalizeAttributes";
  private static final String BEFORE_MSGDELIVERER_TAG = "Before:MessageDeliverer.deliverMessage";
  private static final String AFTER_MSGDELIVERER_TAG = "After:MessageDeliverer.deliverMessage";
  private static final String AFTER_RECVLINK_TAG = "After:ReceiveLink.deliverMessage";
  private static final boolean includeLocalMsgs;
  private LoggingService log;
  private MessageTransportRegistryService reg;
  private TopologyReaderService topo;
  //private static final int SEND = 0;
  //private static final int RECEIVE = 1;
  private int seqnum = 0; 
  private static final String AUDIT_ATTRIBUTE_NUMSEQ = "AuditSeqNum"; 
  private static final String AUDIT_ATTRIBUTE_FROM_INCARNATION = "AuditSenderIncarNum";
  private static final String AUDIT_ATTRIBUTE_FROM_NODE = "AuditSenderNode";

  static {
    //  Read external properties
    String s = "org.cougaar.message.transport.aspects.trafficaudit.includeLocalMsgs";
    includeLocalMsgs = Boolean.valueOf(System.getProperty(s,"true")).booleanValue();
  }

  public TrafficAuditAspect () {}

  public void load () {
    super.load();
    log = loggingService;
    reg = getRegistry();
    topo = (TopologyReaderService)getServiceBroker().getService(this, TopologyReaderService.class, null);
  }

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

  private class SendLinkDelegate extends SendLinkDelegateImplBase {
    private SendLinkDelegate(SendLink link) {
      super(link);
    }
    
    public void sendMessage (AttributedMessage msg) {
      boolean doit = true;
      if (!includeLocalMsgs && reg.isLocalClient(msg.getTarget())) 
        doit = false;
      if (doit && log.isInfoEnabled()) {
	  MessageAddress fromAgent = msg.getOriginator();
        TopologyEntry fromEntry = topo.getEntryForAgent(fromAgent.toString());
        String fromNode = (fromEntry != null) ? fromEntry.getNode() : null;
        long fromIncarnation = (fromEntry != null) ? fromEntry.getIncarnation() : 0;
        msg.setAttribute(AUDIT_ATTRIBUTE_NUMSEQ, new Integer(seqnum++));
        msg.setAttribute(AUDIT_ATTRIBUTE_FROM_INCARNATION, new Long(fromIncarnation));
        msg.setAttribute(AUDIT_ATTRIBUTE_FROM_NODE, fromNode);
        log.info(createAuditData(BEFORE_SENDLINK_TAG, msg, true));
      }
      super.sendMessage (msg);
    }
  }

  public class SendQueueDelegate extends SendQueueDelegateImplBase {
    public SendQueueDelegate (SendQueue queue) {
      super(queue);
    }
	
    public void sendMessage(AttributedMessage msg) {
      boolean doit = true;
      if (!includeLocalMsgs && reg.isLocalClient(msg.getTarget())) 
        doit = false;
      if (doit && log.isInfoEnabled())
        log.info(createAuditData(BEFORE_SENDQ_TAG, msg, true));
      super.sendMessage(msg);
    }
  }

  public class RouterDelegate extends RouterDelegateImplBase {
    public RouterDelegate (Router router) {
      super(router);
    }

    public void routeMessage(AttributedMessage msg) {
      boolean doit = true;
      if (!includeLocalMsgs && reg.isLocalClient(msg.getTarget())) 
        doit = false;
      if (doit && log.isInfoEnabled())
        log.info(createAuditData(BEFORE_ROUTER_TAG, msg, true));
      super.routeMessage(msg);
    }
  }

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
      if (doit && log.isInfoEnabled())
        log.info(createAuditData(BEFORE_DESTQ_HOLD_TAG, msg, true, "q", queue.getDestination().toString()));
      super.holdMessage(msg);
    }

    public void dispatchNextMessage(AttributedMessage msg) {
      boolean doit = true;
      if (!includeLocalMsgs && reg.isLocalClient(msg.getTarget())) 
        doit = false;
      if (doit && log.isInfoEnabled())
        log.info(createAuditData(BEFORE_DESTQ_DISPATCH_TAG, msg, true, "q", queue.getDestination().toString()));
      super.dispatchNextMessage(msg);
   }
  }
 
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
      if (doit && log.isInfoEnabled())
        log.info(createAuditData(BEFORE_DESTLINK_TAG, msg, true, "link", link.getProtocolClass().toString()));
      return super.forwardMessage(msg);
    }
  }

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
      if (doit && log.isInfoEnabled())
        log.info(createAuditData(BEFORE_MSGWRITER_FINOUT_TAG, msg, true));
      super.finishOutput();
    }
  }

  public class MessageReaderDelegate extends MessageReaderDelegateImplBase {
    MessageReaderDelegate(MessageReader delegate) {
      super(delegate);
    }

    public void finalizeAttributes(AttributedMessage msg) {
      super.finalizeAttributes(msg);
      boolean doit = true;
      if (!includeLocalMsgs && reg.isLocalClient(msg.getTarget())) 
        doit = false;
      if (doit && log.isInfoEnabled())
        log.info(createAuditData(AFTER_MSGREADER_FINATTR_TAG, msg, false));
    }
  }

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
      if (doit && log.isInfoEnabled())
        log.info(createAuditData(BEFORE_MSGDELIVERER_TAG, msg, false, "dest", dest.toString()));
      return attrs;
    }  
  }

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
          attrs != null &&
          attrs.getAttribute(DELIVERY_ATTRIBUTE).equals(DELIVERY_STATUS_DELIVERED))
        log.info(createAuditData(AFTER_RECVLINK_TAG, msg, false));
      return attrs;
    }
  }

  private String createAuditData (String tag, AttributedMessage msg, boolean sendp) {
    return createAuditData(tag, msg, sendp, null, null);
  }

  private String createAuditData (String tag, AttributedMessage msg, boolean sendp, String key, String val) {

	MessageAddress fromAgent = msg.getOriginator();
	MessageAddress toAgent = msg.getTarget();

	String to   = null;

	if (sendp) { //Get Sender data
        to = toAgent.toString();
	} else { //This is an incoming msg - Grab node/incarnation # for sender from msg
        TopologyEntry toEntry = topo.getEntryForAgent(toAgent.toString());
    	  String toNode = (toEntry != null) ? toEntry.getNode() : null;
        long toIncarnation = (toEntry != null) ? toEntry.getIncarnation() : 0;
	  to = toNode + "." + toAgent + "." + toIncarnation;
	}

      String from = (String)msg.getAttribute(AUDIT_ATTRIBUTE_FROM_NODE) + "." + 
 	  fromAgent + "." + 
	  (Long)msg.getAttribute(AUDIT_ATTRIBUTE_FROM_INCARNATION); 

      return "<LP lpName=\"" + tag + 
             "\" time=\"" + now() +
             "\" from=\"" + from +
             "\" to=\""   + to + 
             "\" num=\"" + (Integer)msg.getAttribute(AUDIT_ATTRIBUTE_NUMSEQ) + "\" " +
             ((key != null) ? (key + "=\"" + val + "\" ") : "") +
             "/>";
  }

  private static long now () {
    return System.currentTimeMillis();
  }
}
