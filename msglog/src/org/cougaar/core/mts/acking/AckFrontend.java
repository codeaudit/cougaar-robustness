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


//  Note: Different kinds of messages come into the ack frontend: new messages,
//  messages being resent (because they were not acked in time), pure ack messages 
//  being sent for the first or more times, and pure ack-ack messages, being sent 
//  for the first and only time.

class AckFrontend extends DestinationLinkDelegateImplBase 
{
  private DestinationLink link;
  private MessageAttributes success;

  public AckFrontend (DestinationLink link) 
  {
    super (link);
    this.link = link;

    success = new SimpleMessageAttributes();
    String status = MessageAttributes.DELIVERY_STATUS_DELIVERED;
    success.setAttribute (MessageAttributes.DELIVERY_ATTRIBUTE, status);
  }

  public MessageAttributes forwardMessage (AttributedMessage msg) 
    throws UnregisteredNameException, NameLookupException, 
           CommFailureException, MisdeliveredMessageException
  {
    //  Sanity checks

    if (!MessageUtils.hasMessageNumber (msg))
    {
      //  This can happen if the order of the Acking aspect is wrong

      throw new CommFailureException (new Exception ("Msg has no number! (Check aspect order)"));
    }

    if (MessageUtils.isLocalMessage (msg))
    {
      return super.forwardMessage (msg);
    }

    //  Deal with the different kinds of acking messages we can get

    Ack ack = MessageUtils.getAck (msg);
    int msgNum = MessageUtils.getMessageNumber (msg);

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

    //  Try sending the message

    String toNode = MessageUtils.getToAgentNode (msg);

    try
    {
      ack.setSendLink (link.getProtocolClass());

      if (MessageAckingAspect.ackingOn)
      {
        //  Set the specific and latest acks for the message

        if (ack.isRegularAck())
        {
          //  We set the specific acks only once.  That is because we need to know
          //  what acks have been acked when this message is acked, and if we set 
          //  (potentially) different acks with each resend of this message, we 
          //  are not going to know which send or resend made it to the other 
          //  side and thus what acks we can safely retire.

          if (ack.getSendCount() == 0) 
          {
            ack.setSpecificAcks (MessageAckingAspect.getAcksToSend (toNode));
          }
        }
        else if (ack.isPureAckAck())
        {
          //  The specific acks within a pure ack-ack message have already been
          //  set and they do not change.
        }
        else // pure ack messages
        {
          //  For pure ack messages we only send the latest acks

          ack.setSpecificAcks (null);
        }

        ack.setLatestAcks (MessageAckingAspect.getAcksToSend (toNode));

        //  Stuff for computing roundtrip times and whatnot

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
          //  NOTE:  Some messages like heartbeats & traffic masking are not acked 
          //  (but they can carry acks).

          if (MessageUtils.isAckableMessage (msg))  
          {
            if (MessageAckingAspect.hasMessageBeenAcked (msg)) 
            {
              MessageAckingAspect.ackWaiter.add (msg);
              return success;
            }
          }
        }
        else if (ack.isPureAck())
        {
          PureAck pureAck = (PureAck) ack;
          
          //  Simple filters that retire pure ack messages

          if (pureAck.getLatestAcks().isEmpty()) 
          {
            if (MessageAckingAspect.debug)
            {
              String msgString = ((PureAckMessage)msg).toString();
              System.err.println ("AckFrontend: Latest acks empty, dropping " +msgString);
            }

            return success;
          }

          if (!pureAck.stillAckingSrcMsg())
          {
            if (MessageAckingAspect.debug)
            {
              String msgString = ((PureAckMessage)msg).toString();
              System.err.println ("AckFrontend: Src msg out of acks, dropping " +msgString);
            }

            return success;
          }

          if (!MessageAckingAspect.findAckToSend ((PureAckMessage)msg))
          {
            if (MessageAckingAspect.debug)
            {
              String msgString = ((PureAckMessage)msg).toString();
              System.err.println ("AckFrontend: Pure ack acked, dropping " +msgString);
            }

            return success;
          }

          //  Check if some other message has beat the ack outta here

          long lastSendTime = MessageAckingAspect.getLastSendTime (link, toNode);

          if (lastSendTime > pureAck.getAckSendableTime())
          {
            //  No need to send the ack over this link, reschedule it to consider
            //  another link.

            int roundtrip = MessageAckingAspect.getRoundtripTimeForAck (link, pureAck);
            long deadline = lastSendTime + (long)((float)roundtrip * MessageAckingAspect.interAckSpacingFactor);
            pureAck.setSendDeadline (deadline);

            if (MessageAckingAspect.debug)
            {
              String msgString = ((PureAckMessage)msg).toString();
              System.err.println ("AckFrontend: Rescheduling " +msgString);
            }

            MessageAckingAspect.pureAckSender.add ((PureAckMessage)msg);
            return success;
          }
        }

        ack.incrementSendCount();
      }
      else ack.setResendMultiplier (0);  // no acking = no resends

      if (MessageAckingAspect.showTraffic) 
      {
        String str = (ack.getSendCount()<=1 ? "Sending  " : "REsending ") + MessageUtils.toShortString (msg);
        String lnk = MessageAckingAspect.getLinkType (ack.getSendLink());
        String sequence = MessageUtils.getSequenceID (msg);

        System.err.println ("\n" +str+ " via " +lnk+ " of " +sequence);

        if (MessageAckingAspect.ackingOn && MessageAckingAspect.debug)
        {
          System.err.println (str+ " contains acks: ");
          AckList.printAcks ("specific", ack.getSpecificAcks());
          AckList.printAcks ("  latest", ack.getLatestAcks());
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

        //  We reschedule pure acks so they will be sent again if needed

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
        if (MessageAckingAspect.debug) System.err.println ("\nFailure sending Msg " +msgNum+ ": " +e);
      }

      throw new CommFailureException (e);
    }

    //  At this point the first send leg has been successful (otherwise we
    //  would have gotten an exception above).  If we are acking and the message
    //  is ackable we put the it on our ack waiting list.

    if (ack.isRegularAck() && MessageAckingAspect.ackingOn) 
    {
      if (MessageUtils.isAckableMessage (msg))
      {
        MessageAckingAspect.ackWaiter.add (msg);
      }
    }

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
