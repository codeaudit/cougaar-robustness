/*
 * <copyright>
 *  Copyright 2001 Object Services and Consulting, Inc. (OBJS),
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
 * 23 Apr  2001: Split out from MessageAckingAspect. (OBJS)
 */

package org.cougaar.core.mts.acking;

import org.cougaar.core.mts.*;
import org.cougaar.core.service.LoggingService;

import java.io.*;
import java.util.*;


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
    //  Sanity Check:  All messages must have a number

    if (!MessageUtils.hasMessageNumber (msg))
    {
      //  NOTE:  This can happen if this aspect is called in the wrong order

      String s = "Msg has no number! (Check the aspect order)";
      log.error (s);  
      throw new CommFailureException (new Exception (s));
    }

    //  Special Case:  Local messages are completely excluded from acking

    if (MessageUtils.isLocalMessage (msg)) return super.forwardMessage (msg);

    //  Deal with new & resending messages

    Ack ack = MessageUtils.getAck (msg);
    int msgNum = MessageUtils.getMessageNumber (msg);

    if (ack == null)
    {
      //  A new message (to acking) - insert an ack into it

      ack = new Ack (msg);
      MessageUtils.setAck (msg, ack);
      ack.addLinkSelection (link);  // the first selection (already made)
    }
    else
    {
      //  We have a message being resent.  If it has been acked in the
      //  meantime we can send it to the ackWaiter for final processing.

      if (MessageAckingAspect.hasMessageBeenAcked (msg)) 
      {
        MessageAckingAspect.ackWaiter.add (msg);
        return success;
      }
    }

    //  Prep the message for sending

    String msgString = MessageUtils.toString (msg);
    String toNode = MessageUtils.getToAgentNode (msg);

    ack.setSendLink (link.getProtocolClass().getName());
    ack.setRTT (MessageAckingAspect.getBestFullRTTForLink (link, toNode, msg));
    ack.setResendMultiplier (MessageAckingAspect.resendMultiplier);
    
    //  Set the specific acks based on the ack type

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
        if (log.isDebugEnabled()) log.debug ("AckFrontend: Latest acks empty, dropping " +msgString);
        return success;
      }

      if (!pureAck.stillAckingSrcMsg())
      {
        if (log.isDebugEnabled()) log.debug ("AckFrontend: Src msg out of acks, dropping " +msgString);
        return success;
      }

      if (!MessageAckingAspect.findAckToSend ((PureAckMessage)msg))
      {
        if (log.isDebugEnabled()) log.debug ("AckFrontend: Pure ack acked, dropping " +msgString);
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
        if (log.isDebugEnabled()) log.debug ("AckFrontend: Rescheduling " +msgString);
        MessageAckingAspect.pureAckSender.add ((PureAckMessage)msg);
        return success;
      }
    }

    //  Last chance to bail before sending the message

    if (MessageAckingAspect.hasMessageBeenAcked (msg)) 
    {
      MessageAckingAspect.ackWaiter.add (msg);
      return success;
    }

    //  Try sending the message

    try
    {
      if (MessageAckingAspect.showTraffic)
      {
        if (log.isInfoEnabled())
        {
          StringBuffer buf = new StringBuffer();
          buf.append ("\n\n");
          String hdr = (ack.getSendCount()==0 ? "Sending  " : "REsending ") + MessageUtils.toShortString (msg);
          buf.append (hdr);
          buf.append (" via ");
          buf.append (MessageAckingAspect.getLinkType (ack.getSendLink()));
          buf.append (" of ");
          buf.append (MessageUtils.getAckingSequenceID (msg));
          buf.append ("\n");
          buf.append (hdr);
          buf.append (" contains acks: \n");
          AckList.printAcks (buf, "specific", ack.getSpecificAcks());
          AckList.printAcks (buf, "  latest", ack.getLatestAcks());
          // System.err.println ("Outbound roundtrip time: " +ack.getSenderRoundtripTime());
          log.info (buf.toString());         
        }
      }

      ack.incrementSendCount();
      ack.setSendTime (now());

      //  NOTE - will put msg on ack waiter queue HERE before send in the near future

      success = link.forwardMessage (msg);  // message send
    }
    catch (Exception e)
    {
      //  Ok, the send failed.  We throw an exception and a new link is selected
      //  by the policy and we come back to the ack frontend and try it all again.

      ack.decrementSendCount();  // didn't actually send
      if (log.isWarnEnabled()) log.warn ("Failure sending " +msgString+ ":\n" +stackTraceToString(e));
      throw new CommFailureException (e);
    }

    //  Ok, the first (and possibly only) leg of the message send has completed successfuly

    MessageAckingAspect.setLastSendTime (ack.getSendLink(), toNode, ack.getSendTime());
    MessageAckingAspect.setLastSuccessfulLinkUsed (toNode, link.getProtocolClass());

    if (ack.isAck() && MessageUtils.getMessageNumber(msg) != 0)
    {
      //  We put ackable messages on the ack waiter queue to wait for their acks and
      //  possibly be resent.  If the protocol link used to send the message is being 
      //  excluded from acking, we first declare that the ack has arrived. 

      if (MessageAckingAspect.isExcludedLink (link))
      {
        //  Manufacture an ack for our message over an apparently separately acked link

        AckList ackList = new AckList (MessageUtils.getFromAgent(msg), MessageUtils.getToAgent(msg));
        ackList.add (MessageUtils.getMessageNumber (msg));
        Vector v = new Vector();
        v.add (ackList);
        MessageAckingAspect.addReceivedAcks (toNode, v);
      }

      MessageAckingAspect.ackWaiter.add (msg);
    }
    else if (ack.isPureAck())
    {
      //  We reschedule pure acks so they will be sent again if needed

      PureAck pureAck = (PureAck) ack;
      long lastSendTime = pureAck.getSendTime();
      float rtt = (float) ack.getRTT();
      long deadline = lastSendTime + (long)(rtt * MessageAckingAspect.interAckSpacingFactor);
      pureAck.setSendDeadline (deadline);
      MessageAckingAspect.pureAckSender.add ((PureAckMessage)msg);
    }

    return success;  // msg successfully sent (so far)
  }

  //  Utility methods

  private String stackTraceToString (Exception e)
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

  private static String toDate (long time)
  {
    if (time == 0) return "0";

    //  Return date string with milliseconds

    String date = (new Date(time)).toString();
    String d1 = date.substring (0, 19);
    String d2 = date.substring (19);
    long ms = time % 1000;
    return d1 + "." + ms + d2;
  }
}
