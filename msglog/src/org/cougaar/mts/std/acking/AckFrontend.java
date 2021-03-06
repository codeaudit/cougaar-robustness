/*
 * <copyright>
 *  Copyright 2001-2004 Object Services and Consulting, Inc. (OBJS),
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
 *  1 Mar 2004: Port to 11.0
 * 09 Jun 2003: Don't count WP failure as a link use, because WP callbacks cause them to be frequent. (104B)
 * 22 Apr 2003: Added Event when message sent. (102B)
 * 18 Aug 2002: Mucho changes to support Cougaar 9.2+ and agent mobility. (OBJS)
 * 23 Apr 2002: Split out from MessageAckingAspect. (OBJS)
 */

package org.cougaar.mts.std.acking;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Vector;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.SimpleMessageAttributes;
import org.cougaar.mts.base.AttributedMessage;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.DestinationLinkDelegateImplBase;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.UnregisteredNameException;
import org.cougaar.mts.std.AdaptiveLinkSelectionPolicy;
import org.cougaar.mts.std.MessageUtils;
import org.cougaar.mts.std.RTTService;
import org.cougaar.mts.std.udp.OutgoingUDPLinkProtocol;
import org.cougaar.mts.std.socket.OutgoingSocketLinkProtocol;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.EventService;

//  Note: Different kinds of messages come into the ack frontend: new messages,
//  messages being resent (because they were not acked in time), pure ack messages 
//  being sent for the first or more times, and pure ack-ack messages, sent only
//  once.

class AckFrontend extends DestinationLinkDelegateImplBase 
{
  private DestinationLink link;
  private MessageAckingAspect aspect;
  private LoggingService log;
  private MessageAttributes success;

  public AckFrontend (DestinationLink link, MessageAckingAspect aspect)
  {
    super (link);
    this.link = link;
    this.aspect = aspect;
    log = aspect.getTheLoggingService();

    success = new SimpleMessageAttributes();
    String status = MessageAttributes.DELIVERY_STATUS_DELIVERED;
    success.setAttribute (MessageAttributes.DELIVERY_ATTRIBUTE, status);
  }

  public MessageAttributes forwardMessage (AttributedMessage msg) 
    throws UnregisteredNameException, NameLookupException, 
           CommFailureException, MisdeliveredMessageException
  {
    String msgString = MessageUtils.toString (msg);
    if (log.isDebugEnabled()) log.debug ("AckFrontend: entered by " +msgString);

    //  Sanity Check:  All messages must have a number

    if (!MessageUtils.hasMessageNumber (msg))
    {
      //  NOTE:  This can happen if this aspect is called in the wrong order

      String s = "Msg has no number! (Check the aspect order): " +msgString;
      log.error (s);  
      throw new CommFailureException (new Exception (s));
    }

    //  Special Case:  Acking is turned off

    if (!MessageAckingAspect.isAckingOn())
    {
      if (log.isDebugEnabled()) log.debug ("AckFrontend: acking turned off, forwarding " +msgString);
      MessageAttributes attr = super.forwardMessage (msg);
      MessageAckingAspect.recordMessageSend (msg);  // for msg auditing
      return attr;
    }

    //  Special Case:  Local messages are completely excluded from acking

    if (MessageUtils.isLocalMessage (msg)) 
    {
      if (log.isDebugEnabled()) log.debug ("AckFrontend: forwarding local msg: " +msgString);
      return super.forwardMessage (msg);
    }

    //  Deal with new & retry/resend messages

    Ack ack = MessageUtils.getAck (msg);
    String toNode = MessageUtils.getToAgentNode (msg);

    if (ack == null)
    {
      //  A new message (to acking) - insert an ack into it

      ack = new Ack (msg);
      MessageUtils.setAck (msg, ack);
      ack.addLinkSelection (link);  // the first selection (already made)
      ack.setResendMultiplier (MessageAckingAspect.resendMultiplier);
    }
    else if (ack.isAck())
    {
      //  If the message has been acked already  we can just return success

      if (MessageAckingAspect.hasMessageBeenAcked (msg))
      {
        if (log.isInfoEnabled()) log.info("AckFrontend: Dropping acked resend " +msgString);
        return success;
      }
    }

    //  Update various ack fields

    ack.setSendLink (link.getProtocolClass().getName());
    ack.setRTT (getRTT (ack, link, toNode, msg));

    //  Set the specific acks in the message based on the ack type it contains

    if (ack.isAck())
    {
      //  We set the specific acks here only once.  That is because we need to
      //  know what acks have been acked when this message is acked, and if we
      //  set (potentially) different acks with each resend of this message, we 
      //  are not going to know which send or resend made it to the other 
      //  side and thus what acks we can safely retire.

      if (ack.getSendCount() == 0) 
      {
        ack.setSpecificAcks (MessageAckingAspect.getAcksToSend (toNode));
      }
    }
    else if (ack.isPureAck())
    {
      //  For pure ack messages we only send the latest acks

      ack.setSpecificAcks (null);
    }
    else if (ack.isPureAckAck())
    {
      //  The specific acks within a pure ack-ack message have already been
      //  set and they do not change.
    }

    //  We always set the latest acks

    ack.setLatestAcks (MessageAckingAspect.getAcksToSend (toNode));

    //  Special handling for pure acks

    if (ack.isPureAck())
    {
      //  Simple filters that retire pure ack messages

      PureAck pureAck = (PureAck) ack;
      
      if (pureAck.getLatestAcks().isEmpty()) 
      {
        if (log.isInfoEnabled()) log.info("AckFrontend: Pure ack latest acks empty, dropping " +msgString);
        return success;
      }

      if (!pureAck.stillAckingSrcMsg())
      {
        if (log.isInfoEnabled()) log.info("AckFrontend: Pure ack src msg out of acks, dropping " +msgString);
        return success;
      }

      if (!MessageAckingAspect.findAckToSend ((PureAckMessage)msg))
      {
	  if (log.isInfoEnabled()) log.info("AckFrontend: Pure ack acked, dropping " +msgString);
        return success;
      }

      //  Check if some other message has beat the pure ack outta here, meaning we
      //  don't need to send it as its information has been piggy-backed on the
      //  other message via its regular (embedded) ack.

      long lastSendTime = MessageAckingAspect.getLastSendTime (link, toNode);

      if (lastSendTime > pureAck.getAckSendableTime())
      {
        //  No need to send the ack over this link, reschedule it to consider
        //  another link.

        float rtt = (float) ack.getRTT();
        long deadline = lastSendTime + (long)(rtt * MessageAckingAspect.interAckSpacingFactor);
        pureAck.setSendDeadline (deadline);
        if (log.isDebugEnabled()) log.debug ("AckFrontend: Rescheduling pure ack " +msgString);
        MessageAckingAspect.pureAckSender.add ((PureAckMessage)msg);
        return success;
      }
    }

    //  Last chance to bail before sending the message

    if (MessageAckingAspect.hasMessageBeenAcked (msg)) 
    {
      if (log.isInfoEnabled()) log.info("AckFrontend: Dropping acked resend " +msgString);
      return success;
    }

    if (MessageUtils.getSendDeadline (msg) < now())
    {
      if (log.isInfoEnabled()) log.info("AckFrontend: Dropping msg past its send deadline " +msgString);
      return success;
    }

    //  Try sending the message

    String sendInfo = "null";

    try
    {
      if (log.isInfoEnabled())
      {
        StringBuffer buf = new StringBuffer();
        buf.append (">");
        buf.append (AdaptiveLinkSelectionPolicy.getLinkLetter(link));
        buf.append (" ");
        buf.append (MessageUtils.getMessageNumber(msg));
        buf.append (".");
        buf.append (ack.getSendCount()+1);  // what gets sent
        buf.append (" ");
        buf.append (MessageUtils.getMessageTypeLetter(msg));

        if (ack.isSomePureAck())
        {
          buf.append ("(");
          buf.append (MessageUtils.getSrcMsgNumber(msg));
          buf.append (")");
        }

        buf.append (" ");
        buf.append (MessageUtils.toShortSequenceID(msg));

        sendInfo = buf.toString();
        log.info (sendInfo + " (try)");         
      }

      if (log.isDebugEnabled())
      {
        StringBuffer buf = new StringBuffer();
//      buf.append ("\n\n");
        buf.append (ack.getSendCount()==0 ? "Sending  " : "REsending ");
        buf.append (MessageUtils.toShortString (msg));
        buf.append (" via ");
        buf.append (MessageAckingAspect.getLinkType (ack.getSendLink()));
        buf.append (" of ");
        buf.append (MessageUtils.getAckingSequenceID (msg));
        buf.append ("\n");
        AckList.printAcks (buf, "specific", ack.getSpecificAcks());
        AckList.printAcks (buf, "  latest", ack.getLatestAcks());
        log.debug (buf.toString());         
      }

      //  The message resender manages resending the message as needed.  We do this 
      //  even for excluded links as we want to handle the case where the send gets
      //  stuffed up down the aspect chain for some reason (eg. delayed RMI).

      ack.setSendTime (now());
      if (ack.isAck()) MessageAckingAspect.messageResender.add (msg);

      ack.incrementSendTry();
      ack.incrementSendCount();

      success = link.forwardMessage (msg);  // send message down the chain (may stall)
    }
    catch (Exception e)
    {
      //  Ok, the send failed.  If this is the first send leg for this message, we 
      //  remove the message from the message resender and throw an exception, otherwise
      //  we notify the message resender to reschedule the send.  Either way, a new link 
      //  selection will be made and the message will comes back thru and try again.

      if (log.isInfoEnabled())  log.info (sendInfo + " (fail)");
      if (log.isDebugEnabled()) log.debug ("Failure sending " +msgString+ ":\n" +e);

      ack.decrementSendCount();  // didn't actually send

      //104B Don't count WP failure as a link use, because callbacks cause them to be frequent
      if ((e instanceof UnregisteredNameException) || (e instanceof NameLookupException))
        ack.removeLinkSelection(link);

      if (ack.getSendCount() == 0) 
      {
        //  Cougaar is in control of retries

        if (ack.isAck()) MessageAckingAspect.messageResender.remove (msg);
        throw new CommFailureException (e);  
      }
      else 
      {
        //  We are in control of resends

        if (ack.isAck())
        {
          //  The resend is actually not so immediate if we have been trying 
          //  for a while - delay is added in to handle the case when no link
          //  able to send out messages.

          MessageAckingAspect.messageResender.scheduleImmediateResend (msg);
          return success;  // not really of course
        }
        else throw new CommFailureException (e);  // pure acks are handled differently
      }
    }

    //  Ok, the first (and possibly only) leg of the message send has completed successfully

/* 102B This caused too many events for Acme
    //102B Added this Event to indicate a successful send 
    EventService es = (EventService)aspect.getServiceBroker().getService(this, EventService.class, null);
    if (es.isEventEnabled())
      es.event("Message Sent via " +link.getProtocolClass().getName()+
               " from " +MessageUtils.getFromAgent(msg).getNodeName()+
               " to " +MessageUtils.getToAgent(msg).getNodeName() );
*/

    MessageAckingAspect.recordMessageSend (msg);  // for msg auditing
    MessageAckingAspect.setLastSendTime (ack.getSendLink(), toNode, ack.getSendTime());
    MessageAckingAspect.setLastSuccessfulLinkUsed (toNode, link.getProtocolClass());

    if (ack.isAck() && MessageUtils.getMessageNumber(msg) != 0)
    {
      //  If the protocol link used to send the message is excluded from acking, we create
      //  an ack for it so the message resender can process the message and not resend it.  It
      //  is certainly a race to put this ack out there before the message gets resent, but in
      //  most cases no resend should occur unless it really needed to.  Of course the 
      //  receiver is protecting himself from duplicates, so only some effort would be lost.

      if (MessageAckingAspect.isExcludedLink (link))
      {
        //  Manufacture an ack for the message and add it to our received acks list

        AckList ackList = new AckList (MessageUtils.getFromAgent(msg), MessageUtils.getToAgent(msg));
        ackList.add (MessageUtils.getMessageNumber (msg));
        Vector v = new Vector();
        v.add (ackList);
        if (log.isDebugEnabled()) log.debug ("AckFrontend: Adding ack for excluded link msg: " +msgString);
        MessageAckingAspect.addReceivedAcks (toNode, v);
      }
    }
    else if (ack.isPureAck())
    {
      //  We reschedule pure acks so they will be sent again if needed

      PureAck pureAck = (PureAck) ack;
      long lastSendTime = pureAck.getSendTime();
      float rtt = (float) ack.getRTT();
      long deadline = lastSendTime + (long)(rtt * MessageAckingAspect.interAckSpacingFactor);
      pureAck.setSendDeadline (deadline);

      if (log.isDebugEnabled()) 
      {
        long t = pureAck.getSendDeadline() - now();
        log.debug ("AckFrontend: Resched next pure ack msg with timeout of " +t+ ": " +msgString);
      }

      MessageAckingAspect.pureAckSender.add ((PureAckMessage)msg);
    }

    return success;  // msg successfully sent (so far)
  }

  //  Utility methods

  private int getRTT (Ack ack, DestinationLink link, String node, AttributedMessage msg)
  {
    int rtt = getBestFullRTTForLink (link, node, msg);

    //  HACK - need generic DestinationLink methods rather than protocol specific ones

    if (ack.getSendLink().equals("org.cougaar.core.mts.udp.OutgoingUDPLinkProtocol"))
    {
      if (OutgoingUDPLinkProtocol.doInbandAcking())
      {
        int timeout = OutgoingUDPLinkProtocol.getSocketTimeout();
        if (timeout > rtt) rtt = timeout;
      }
    }
    else if (ack.getSendLink().equals("org.cougaar.core.mts.socket.OutgoingSocketLinkProtocol"))
    {
      if (OutgoingSocketLinkProtocol.doInbandAcking())
      {
        int timeout = OutgoingSocketLinkProtocol.getSocketTimeout();
        if (timeout > rtt) rtt = timeout;
      }
    }

    return rtt;
  }

  private int getBestFullRTTForLink (DestinationLink link, String node, AttributedMessage msg)
  {
    int rtt = 0;
    RTTService rttService = (RTTService) aspect.getServiceBroker().getService (aspect, RTTService.class, null);
    if (rttService != null) rtt = rttService.getBestFullRTTForLink (link, node);
    if (rtt <= 0) try { rtt = link.cost (msg); } catch (Exception e) { rtt = 2000; }  // HACK
    return rtt;
  }

  private static String stackTraceToString (Exception e)
  {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter (stringWriter);
    e.printStackTrace (printWriter);
    return stringWriter.getBuffer().toString();
  }

  private static long now ()
  {
    return System.currentTimeMillis();
  }
}
