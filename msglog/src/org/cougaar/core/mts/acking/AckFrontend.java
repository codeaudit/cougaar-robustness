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

import java.io.*;
import java.util.*;

import org.cougaar.core.mts.*;


//  Note: 4 kinds of messages come into the ack frontend: new messages,
//  regular ack enveloped messages being resent (because they were not acked 
//  in time), pure ack messages, which are derived from the envelope, and
//  are being sent for the first or more times, and pure ack-ack messages,
//  being sent for the first and only time.

class AckFrontend extends DestinationLinkDelegateImplBase 
{
  private DestinationLink link;

  public AckFrontend (DestinationLink link) 
  {
    super (link);
    this.link = link;

  }

  public MessageAttributes forwardMessage (AttributedMessage msg) 
    throws UnregisteredNameException, NameLookupException, 
           CommFailureException, MisdeliveredMessageException
  {
System.err.println ("AckFrontend: msg "+msg.hashCode()+" = "+MessageUtils.toString(msg));

    //  Sanity check:  Pass on local messages

    if (MessageUtils.isLocalMessage (msg))
    {
System.err.println ("got local msg");
      return link.forwardMessage (msg);
    }

    Ack ack = MessageUtils.getAck (msg);
    int msgNum = MessageUtils.getMessageNumber (msg);

System.err.println ("AckFrontend: msg "+msg.hashCode()+" msgNum="+msgNum+" has num="+MessageUtils.hasMessageNumber (msg));

    MessageAttributes success = new SimpleMessageAttributes();
    String status = MessageAttributes.DELIVERY_STATUS_DELIVERED;
    success.setAttribute (MessageAttributes.DELIVERY_ATTRIBUTE, status);

    //  Deal with the different kinds of messages we can get

    if (ack == null)
    {
      //  A new message (to acking) - insert an ack in it

      ack = new Ack (msg);
      MessageUtils.setAck (msg, ack);

      ack.addLinkSelection (link);  // the first selection (already made)
    }
    else
    {
      if (ack.isRegularAck())
      {
        //  We have a message being resent.  If it has been acked in the
        //  meantime we can send it to the ackWaiter for final processing.

        if (MessageAckingAspect.hasMessageBeenAcked (msg)) 
        {
          MessageAckingAspect.ackWaiter.add (msg);
          return success;
        }
      }
    }

    //  Check the send count.  If it has exceeded the number of resends that have 
    //  been specified, start throwing the comm failure exception.  This is just
    //  a band aid on a fundamental issue of when or whether to limit retries.

    if (ack.isRegularAck() && ack.getSendCount() >= MessageAckingAspect.maxMessageResends)
    {
      throw new CommFailureException (new Exception ("Exceeded max msg resends!"));  // HACK!
    }

    //  Try sending the message

    String toNode = MessageUtils.getToAgentNode (msg);

    try
    {
      ack.setSendLink (link.getProtocolClass());

      if (MessageAckingAspect.ackingOn)
      {
        //  Set the acks.  If this is a regular ack, we only set the acks once.
        //  That is because we need to know what acks have been acked when this message
        //  is acked, and if we set (potentially) different acks with each resend of
        //  this message, we are not going to know which send or resend made it to the
        //  other side and thus what acks we can safely retire.

        if (ack.isRegularAck())
        {
          if (ack.getSendCount() == 0) ack.setAcks (MessageAckingAspect.getAcksToSend (toNode));
        }
        else ack.setAcks (MessageAckingAspect.getAcksToSend (toNode));  // get latest acks

        Classname receiveLink = MessageAckingAspect.getLastReceiveLink (toNode);
        if (receiveLink == null) receiveLink = ack.getSendLink();
        ack.setLastReceiveLink (receiveLink);

        Classname predictedLink = MessageAckingAspect.getReceiveLinkPrediction (toNode);
        if (predictedLink == null) predictedLink = ack.getLastReceiveLink();
        ack.setPredictedReceiveLink (predictedLink);

        int senderRTT = MessageAckingAspect.getRoundtripTimeMeasurement 
        (
          toNode, ack.getSendLink(), ack.getPredictedReceiveLink()
        );

        int receiverRTT = MessageAckingAspect.getRoundtripTimeReport
        (
          toNode, ack.getLastReceiveLink(), ack.getSendLink()
        );

        ack.setSenderRoundtripTime (senderRTT);
        ack.setReceiverRoundtripTime (receiverRTT);
        ack.setResendMultiplier (MessageAckingAspect.resendMultiplier);

        //  Last Chance To Bail: Regular messages that have since been acked or
        //  pure acks that don't need to be sent any more are detected here.

        if (ack.isRegularAck())
        {
          if (MessageAckingAspect.hasMessageBeenAcked (msg)) 
          {
            MessageAckingAspect.ackWaiter.add (msg);
            return success;
          }
        }
        else if (ack.isPureAck())
        {
          PureAck pureAck = (PureAck) ack;

          //  Simple filters that retire acks

          if (pureAck.getAcks().isEmpty()) return success;
          if (!pureAck.stillAckingSrcMsg()) return success;
          if (!MessageAckingAspect.findAckToSend (pureAck)) return success;

          //  Check if some other message has beat the ack outta here

          long lastSendTime = MessageAckingAspect.getLastSendTime (link, toNode);

          if (lastSendTime > pureAck.getAckSendableTime())
          {
            //  No need to send the ack over this link, reschedule it to consider
            //  another link.

            int roundtrip = MessageAckingAspect.getRoundtripTimeForAck (link, pureAck);
            long deadline = lastSendTime + (long)((float)roundtrip * MessageAckingAspect.interAckSpacingFactor);
            pureAck.setSendDeadline (deadline);
            MessageAckingAspect.pureAckSender.add ((PureAckMessage)msg);
            return success;
          }
        }

        ack.incrementSendCount();
      }
      else ack.setResendMultiplier (0);  // no acking = no resends

      if (MessageAckingAspect.showTraffic) 
      {
        String str = (ack.getSendCount()<=1 ? "Sending " : "REsending");
        String ackType = ack.getType();
        String m = MessageUtils.toString (msg);
        String lnk = MessageAckingAspect.getLinkType (ack.getSendLink());

        System.err.println ("\n"+str+" "+ackType+" "+m+" via "+lnk);

        if (MessageAckingAspect.ackingOn && MessageAckingAspect.debug)
        {
          System.err.println (str+" "+ackType+" "+msgNum+" contains acks: ");
          AckList.printAcks (ack.getAcks());
          System.err.println ("Outbound roundtrip time: " +ack.getSenderRoundtripTime());
        }
      }

      //  Here we actually try sending the message

      ack.setSendTime (now());
      success = link.forwardMessage (msg);

      if (MessageAckingAspect.ackingOn)
      {
        MessageAckingAspect.setLastSendTime (ack.getSendLink(), toNode, ack.getSendTime());
        MessageAckingAspect.setLastSuccessfulLinkUsed (toNode, link.getProtocolClass());

        //  We reschedule acks so they will be sent again if needed

        if (ack.isPureAck())
        {
          PureAck pureAck = (PureAck) ack;
          long lastSendTime = pureAck.getSendTime();
          int roundtrip = MessageAckingAspect.getRoundtripTimeForAck (link, pureAck);
          long deadline = lastSendTime + (long)((float)roundtrip * MessageAckingAspect.interAckSpacingFactor);
          pureAck.setSendDeadline (deadline);
          MessageAckingAspect.pureAckSender.add ((PureAckMessage)msg);
          return success;
        }
      }
    }
    catch (Exception e)
    {
      //  Ok, the send failed.  We throw an exception and a new link is selected
      //  by the policy and we come back to the ack frontend and try it all again.

      if (MessageAckingAspect.ackingOn)
      {
        ack.decrementSendCount();  // didn't actually send
//        if (MessageAckingAspect.debug) System.err.println ("\nFailure sending msg " +msgNum+ ": " +e);
System.err.println ("\nAckFrontEnd: Failure sending msg "+msg.hashCode()+" = " +msgNum+ ": " +e);
      }

      throw new CommFailureException (e);
    }

    //  At this point the first send leg has been successful (otherwise we
    //  would have gotten an exception above).  If we are acking we put the 
    //  message on our ack waiting list.

    if (ack.isRegularAck() && MessageAckingAspect.ackingOn) MessageAckingAspect.ackWaiter.add (msg);

    //  Return results of forwarding message

    return success;
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
