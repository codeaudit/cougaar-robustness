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
 * 04 Sep 2002: Created. (OBJS)
 */

package org.cougaar.core.mts;

import org.cougaar.core.service.LoggingService;

/**
 **  Put out logging messages that are used to analyze Cougaar message traffic.
 **/

public class TrafficAuditAspect extends StandardAspect
{
  private static final String AUDIT_TAG = "TRAFFIC_AUDIT";

  private static final boolean includeLocalMsgs;

  private LoggingService log;

  static
  {
    //  Read external properties

    String s = "org.cougaar.message.transport.aspects.trafficaudit.includeLocalMsgs";
    includeLocalMsgs = Boolean.valueOf(System.getProperty(s,"false")).booleanValue();
  }

  public TrafficAuditAspect () 
  {}

  public void load ()
  {
    super.load();
    log = loggingService;
  }

  public Object getDelegate (Object delegate, Class type) 
  {
    if (type == DestinationLink.class) return (new SendLink ((DestinationLink) delegate));
    if (type == ReceiveLink.class)     return (new RecvLink ((ReceiveLink) delegate));
    return null;
  }

  private class SendLink extends DestinationLinkDelegateImplBase 
  {
    private String linkType;

    private SendLink (DestinationLink link) 
    {
      super (link);
      linkType = AdaptiveLinkSelectionPolicy.getLinkType (link);
    }
    
    public MessageAttributes forwardMessage (AttributedMessage msg) 
      throws UnregisteredNameException, NameLookupException, 
  	         CommFailureException, MisdeliveredMessageException
    {
      //  Record send of message if it does not throw an exception.
      //  NOTE:  This aspect must be between the SendTimeoutAspect and
      //  the link protocols so that timeout exceptions are not seen
      //  here - we don't care about them - all we care about is if
      //  the message is sent or not.

      MessageAttributes success = super.forwardMessage (msg);

      if (log.isInfoEnabled())
      {
        boolean doit = true;
        if (!includeLocalMsgs && isLocalMessage(msg)) doit = false;
        if (doit) log.info (createAuditData (linkType, "send", msg));
      }

      return success;
    }
  }

  private class RecvLink extends ReceiveLinkDelegateImplBase
  {
    private RecvLink (ReceiveLink link) 
    {
      super (link); 
    }

    public MessageAttributes deliverMessage (AttributedMessage msg) 
    {
      //  Record reception of message and pass it on

      if (log.isInfoEnabled())
      {
        boolean doit = true;
        if (!includeLocalMsgs && isLocalMessage(msg)) doit = false;
        
        if (doit) 
        {
          String linkType = AdaptiveLinkSelectionPolicy.getLinkType (MessageUtils.getSendProtocolLink (msg));
          log.info (createAuditData (linkType, "recv", msg));
        }
      }

      return super.deliverMessage (msg);
    }
  }

  private String createAuditData (String linkType, String msgDirection, AttributedMessage msg)
  {
    StringBuffer buf = new StringBuffer();
    buf.append (AUDIT_TAG);
    buf.append (" ");
    buf.append (now());
    buf.append (" ");
    buf.append (msgDirection.equals("send") ? "Send " : "Recv ");
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
    return getRegistry().isLocalClient (agent);
  }

  private static long now ()
  {
    return System.currentTimeMillis();
  }
}
