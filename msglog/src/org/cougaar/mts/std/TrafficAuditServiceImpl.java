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
 * 20 Nov 2002: Created. (OBJS)
 */

package org.cougaar.mts.std;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.LoggingService;
import org.cougaar.mts.base.AttributedMessage;
import org.cougaar.mts.base.MessageTransportRegistryService;

/**
 *  TrafficAuditService implementation and service provider
 *
 *  Put out logging messages that are used to analyze Cougaar message traffic.
**/

public class TrafficAuditServiceImpl implements TrafficAuditService, ServiceProvider
{
  private static final String AUDIT_TAG = "TRAFFIC_AUDIT";

  private static final String MSG_NUMBER =  "Numb";
  private static final String MSG_DROP =    "Drop";
  private static final String MSG_SEND =    "Send";
  private static final String MSG_RECEIVE = "Recv";
  private static final String MSG_DELIVER = "Delv";

  private static final boolean includeLocalMsgs;
  private LoggingService log;
  private MessageTransportRegistryService registry;

  static
  {
    //  Read external properties

    String s = "org.cougaar.message.transport.services.trafficaudit.includeLocalMsgs";
    includeLocalMsgs = Boolean.valueOf(System.getProperty(s,"false")).booleanValue();
  }

  public TrafficAuditServiceImpl (ServiceBroker sb) 
  {
    log = (LoggingService) sb.getService (this, LoggingService.class, null);
    if (log.isInfoEnabled()) log.info ("Creating " +this);
    sb.addService (TrafficAuditService.class, this);

	registry = (MessageTransportRegistryService) sb.getService 
      (this, MessageTransportRegistryService.class, null);
  }

  public Object getService (ServiceBroker sb, Object requestor, Class serviceClass) 
  {
    if (serviceClass == TrafficAuditService.class) return this;
    return null;
  }

  public void releaseService (ServiceBroker sb, Object requestor, Class serviceClass, Object service)
  {}

  public String toString ()
  {
    return getClass().getName();
  }

  //  TrafficAuditService interface fullfillment

  public void recordMessageNumbering (AttributedMessage msg)
  {
    //  We record numbering for now because recording a message entering the
    //  send queue - the logical and desirable point - is problematic in the
    //  current UL msg arch as we can't do msg numbering there very well, and 
    //  without a msg number for the message we can't id it consistently as
    //  we can once we do number it. 

    recordMessageTraffic (MSG_NUMBER, msg);
  }

  public void recordMessageDrop (AttributedMessage msg)
  {
    recordMessageTraffic (MSG_DROP, msg);
  }

  public void recordMessageSend (AttributedMessage msg)
  {
    recordMessageTraffic (MSG_SEND, msg);
  }

  public void recordMessageReceive (AttributedMessage msg)
  {
    recordMessageTraffic (MSG_RECEIVE, msg);
  }

  public void recordMessageDelivery (AttributedMessage msg)
  {
    recordMessageTraffic (MSG_DELIVER, msg);
  }

  private void recordMessageTraffic (String activity, AttributedMessage msg)
  {
    if (log.isInfoEnabled())
    {
      if (includeLocalMsgs || !isLocalMessage(msg)) 
      {
        String linkType = AdaptiveLinkSelectionPolicy.getLinkType (MessageUtils.getSendProtocolLink (msg));
        log.info (createAuditData (linkType, activity, msg));
      }
    }
  }

  private String createAuditData (String linkType, String activity, AttributedMessage msg)
  {
    StringBuffer buf = new StringBuffer();
    buf.append (AUDIT_TAG);
    buf.append (" ");
    buf.append (now());
    buf.append (" ");
    buf.append (activity);
    buf.append (" ");
    buf.append (MessageUtils.getSendCount (msg));
    buf.append (" ");
    buf.append (linkType);
    buf.append (" ");
    buf.append (MessageUtils.getMessageTypeString (msg));
    buf.append (" ");
    buf.append (MessageUtils.getMessageNumber (msg));
    buf.append (" ");
    buf.append (MessageUtils.getFromAgent(msg).toString());
    buf.append (" ");
    buf.append (MessageUtils.getToAgent(msg).toString());
    return buf.toString();
  }

  private boolean isLocalMessage (AttributedMessage msg)
  {
    if (MessageUtils.hasMessageType (msg)) return MessageUtils.isLocalMessage (msg);
    return isLocalAgent (MessageUtils.getTargetAgent(msg));
  }

  private boolean isLocalAgent (MessageAddress agent)
  {
    return registry.isLocalClient (agent);
  }

  private static long now ()
  {
    return System.currentTimeMillis();
  }
}
