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
public class TrafficAuditAspect3 extends StandardAspect implements AttributeConstants {
  private static final String SEND_TAG = "Before:SendLink.sendMessage";
  private static final String RECV_TAG = "After:ReceiveLink.deliverMessage";
  private static final boolean includeLocalMsgs;
  private LoggingService log;
  private MessageTransportRegistryService reg;
  private TopologyReaderService topo;
  //private static final int SEND = 0;
  //private static final int RECEIVE = 1;
  private int seqnum = 0; 
  private static final String AUDIT_ATTRIBUTE = "AuditSeqNum"; 

  static {
    //  Read external properties
    String s = "org.cougaar.message.transport.aspects.trafficaudit3.includeLocalMsgs";
    includeLocalMsgs = Boolean.valueOf(System.getProperty(s,"true")).booleanValue();
  }

  public TrafficAuditAspect3 () {}

  public void load () {
    super.load();
    log = loggingService;
    reg = getRegistry();
    topo = (TopologyReaderService)getServiceBroker().getService(this, TopologyReaderService.class, null);
  }

  public Object getDelegate (Object delegate, Class type) {
    if (type == SendLink.class) 
      return (new MySendLink ((SendLink) delegate));
    if (type == ReceiveLink.class)
      return (new MyReceiveLink ((ReceiveLink) delegate));
    return null;
  }

  private class MySendLink extends SendLinkDelegateImplBase {
    private MySendLink(SendLink link) {
      super(link);
    }
    
    public void sendMessage (AttributedMessage msg) {
      boolean doit = true;
      if (!includeLocalMsgs && reg.isLocalClient(msg.getTarget())) 
        doit = false;
      if (doit && log.isInfoEnabled()) {
        msg.setAttribute(AUDIT_ATTRIBUTE, new Integer(seqnum++));
        log.info(createAuditData(SEND_TAG, msg));
      }
      super.sendMessage (msg);
    }
  }

  private class MyReceiveLink extends ReceiveLinkDelegateImplBase {
    private MyReceiveLink(ReceiveLink link) {
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
        log.info(createAuditData(RECV_TAG, msg));
      return attrs;
    }
  }

  private String createAuditData (String tag, AttributedMessage msg) {
    MessageAddress fromAgent = msg.getOriginator();
    TopologyEntry fromEntry = topo.getEntryForAgent(fromAgent.toString());
    String fromNode = (fromEntry != null) ? fromEntry.getNode() : null;
    long fromIncarnation = (fromEntry != null) ? fromEntry.getIncarnation() : 0;
    MessageAddress toAgent = msg.getTarget();
    TopologyEntry toEntry = topo.getEntryForAgent(toAgent.toString());
    String toNode = (toEntry != null) ? toEntry.getNode() : null;
    long toIncarnation = (toEntry != null) ? toEntry.getIncarnation() : 0;
    return "<tag>" + tag + "</tag> " +
           "<ts>" + now() + "</ts>" +
           "<from>" + fromNode + "." + fromAgent + "." + fromIncarnation + "</from>" +
           "<to>" + toNode + "." + toAgent + "." + toIncarnation + "</to>" +
           "<num>" + (Integer)msg.getAttribute(AUDIT_ATTRIBUTE) + " </num>";
  }

  private static long now () {
    return System.currentTimeMillis();
  }
}
