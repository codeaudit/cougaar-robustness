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


class AckBackend extends MessageDelivererDelegateImplBase 
{
  public AckBackend (MessageDeliverer deliverer) 
  {
    super (deliverer);
  }

  public MessageAttributes deliverMessage (AttributedMessage msg, MessageAddress dest) throws MisdeliveredMessageException
  {
    //  Special Case: Just pass on local messages - they are excluded from acking.

    if (MessageUtils.isLocalMessage (msg))
    {
      return super.deliverMessage (msg, dest);
    }

    //  Sanity check.  Note: The dest argument appears vestigial and is not supported.

    if (!msg.getTarget().equals(dest))
    {
      System.err.println ("ERROR: message target ["+msg.getTarget()+"] != dest ["+dest+"]");
      throw new MisdeliveredMessageException (msg);
    }

    //  Sanity check: Message must have ack in it!

    Ack ack = MessageUtils.getAck (msg);
    String msgString = MessageUtils.toString (msg);

    if (ack == null)
    {
      if (MessageAckingAspect.isAckingOn()) 
      {
        System.err.println ("ERROR: Message has no ack in it! : " +msgString);
        throw new MisdeliveredMessageException (msg);
      }

      return super.deliverMessage (msg, dest);
    }

    ack.setReceiveTime (now());  // establish message delivery time
    ack.setMsg (msg);            // reset transient msg field

    //  Show message reception

    String msgShortStr = MessageUtils.toShortString (msg);

    if (MessageAckingAspect.showTraffic) 
    {
      String lnk = MessageAckingAspect.getLinkType (ack.getSendLink());  // HACK: should be from system, not msg!
      String sequence = MessageUtils.getSequenceID (msg);
      System.err.println ("\nReceived " +msgShortStr+ " via " +lnk+ " of " +sequence);
    }

    //  Here we do integrity checks on the received message.  Who knows where
    //  this "message" really came from, where it has been, and what happened on
    //  the way...  These are only some basic checks, other message security
    //  entities may do more.

    //  NOTE: We are currently limited to throwing MisdeliveredMessageException's,
    //  may want to create new exceptions in the future.

// HACK - no appropriate official status defined yet

    MessageAttributes result = new SimpleMessageAttributes();
//  String status = "MessageRejected";
    String status = MessageAttributes.DELIVERY_STATUS_DELIVERED;
    result.setAttribute (MessageAttributes.DELIVERY_ATTRIBUTE, status);

/*
    Can't do this integrity check yet - msg system architecture wrong for it

    if (getLinkType (ack.getSendLink()) != actual receive link type)
    {
      if (MessageAckingAspect.debug) 
      {
        System.err.println ("ALERT msg came in on wrong link - msg ignored: " +msgString); 
        return result;
      }
    }
*/
    int msgNum = MessageUtils.getMessageNumber (msg);

    if (ack.isRegularAck() && msgNum <= 0)
    {
      if (MessageAckingAspect.debug) 
      {
        System.err.println ("Invalid msg number (msg ignored): " +msgString);
      }

      return result;
    }

    if (ack.isPureAck() && msgNum >= 0)
    {
      if (MessageAckingAspect.debug) 
      {
        System.err.println ("Invalid msg number (pure ack msg ignored): " +msgString);
      }

      return result;
    }

    if (ack.isPureAckAck() && msgNum >= 0)
    {
      if (MessageAckingAspect.debug) 
      {
        System.err.println ("Invalid msg number (pure ack-ack msg ignored): " +msgString);
      }

      return result;
    }

    String fromNode = MessageUtils.getFromAgentNode (msg);

    if (fromNode == null || fromNode.equals(""))
    {
      if (MessageAckingAspect.debug) 
      {
        System.err.println ("From node missing (msg ignored): " +msgString);
      }

      return result;
    }

    if (ack.getSendTime() < now() - MessageAckingAspect.messageAgeWindow*60*1000)
    {
      //  Kind of a hack, but the start of something.  May want to do things like set
      //  max age based on transport type ...
      
      if (MessageAckingAspect.debug) 
      {
        System.err.println ("Msg too old (msg ignored): " +msgString);
      }

      return result;
    }

    if (ack.getSendTime() > now() + MessageAckingAspect.messageAgeWindow*60*1000)
    {
      //  Kind of a hack, but the start of something.  May want to do things like set
      //  max age based on transport type ...
      
      if (MessageAckingAspect.debug) 
      {
        System.err.println ("Msg too far in the future (msg ignored): " +msgString);
      }

      return result;
    }

    if (MessageAckingAspect.isAckingOn())
    {
      try
      {
        if (MessageAckingAspect.debug) 
        {
          System.err.println ("Received " +msgShortStr+ " contains acks: ");
          AckList.printAcks ("specific", ack.getSpecificAcks());
          AckList.printAcks ("  latest", ack.getLatestAcks());
          System.err.println ("Inbound roundtrip time: " + ack.getSenderRoundtripTime());
        }

        if (ack.getSpecificAcks() != null)
        {
          for (Enumeration a=ack.getSpecificAcks().elements(); a.hasMoreElements(); )
          {
            NumberList.checkListValidity ((AckList) a.nextElement());
          }
        }

        if (ack.getLatestAcks() != null)
        {
          for (Enumeration a=ack.getLatestAcks().elements(); a.hasMoreElements(); )
          {
            NumberList.checkListValidity ((AckList) a.nextElement());
          }
        }
      }
      catch (Exception e)
      {
        //  NOTE: Maybe just throw out bad acks?

        if (MessageAckingAspect.debug) 
        {
          System.err.println ("Msg contains invalid acks (msg ignored): " +msgString);
        }

        return result;
      }
    
      if (ack.getSendCount() < 1)
      {
        if (MessageAckingAspect.debug) 
        {
          System.err.println ("Msg has bad sendCount (msg ignored): " +msgString);
        }

        return result;
      }

      if (ack.isRegularAck() && ack.getResendMultiplier() < 1)
      {
        if (MessageAckingAspect.debug) 
        {
          System.err.println ("Msg has bad resendMultiplier (msg ignored): " +msgString);
        }

        return result;
      }
    }

    //  At this point we feel the received message is legit, and we try to add it to our
    //  received message numbers list.  If we have already seen this message we reject 
    //  it as a duplicate.  Note again there is redundant functionality for this in the 
    //  MessageOrderingAspect, but that is ok because message ordering is orthogonal to 
    //  acking and is not required to be active.

    if (MessageUtils.isAckableMessage (msg))  // skip non-acked msgs like heartbeats & traffic masking
    {
      if (MessageAckingAspect.addSuccessfulReceive (msg) == false)
      {
        if (MessageAckingAspect.showTraffic) 
        {
          System.err.println ("Duplicate msg ignored: " +MessageUtils.toString (msg));
        }

        return result;
      }
    }

    //  Ack-related processing

    if (MessageAckingAspect.isAckingOn())
    {
      //  Record the latest roundtrip report and the link we received the message on

      MessageAckingAspect.updateRoundtripTimeReport
      (
        fromNode,
        ack.getSendLink(),
        ack.getPredictedReceiveLink(),
        ack.getSenderRoundtripTime()
      );

      MessageAckingAspect.setLastReceiveLink (fromNode, ack.getSendLink());

      //  Handle the acks contained within the message

      Vector latestAcks = updateFields (ack.getLatestAcks(), ack);
      MessageAckingAspect.addReceivedAcks (fromNode, latestAcks);

      long ackSendableTime = 0;

      if (ack.isRegularAck() || ack.isPureAck())
      {
        //  Record the specific acks contained within this message.  Used to acknowledge 
        //  sent messages waiting for acks and measuring transport roundtrip times.
        //  This call also dings the ack waiter to announce new ack data has arrived.

        if (ack.isRegularAck())
        {
          Vector specificAcks = updateFields (ack.getSpecificAcks(), ack);
          MessageAckingAspect.addReceivedAcks (fromNode, specificAcks);
        }

        if (MessageUtils.isAckableMessage(msg) && ack.isRegularAck())
        {
          //  Add the message to our outbound acks list (we send back acks for all 
          //  regular messages received, until those acks are acked).  We record
          //  the time that this is set, so that we know any message sent back to the 
          //  source node after this time will include this ack (as long as needed).
      
          MessageAckingAspect.addAckToSend (msg);
          ackSendableTime = now();
/*
System.out.println ("ackBackend: added ack to send for "+msgString);
System.out.println ("ackBackend: acks to send now:");
AckList.printAcks (MessageAckingAspect.getAcksToSend(MessageUtils.getFromAgentNode(msg)));
*/
        }
      }
      else if (ack.isPureAckAck())
      {
        //  Remove the acks contained within this message from our sending list, 
        //  as they are now officially acked.

        MessageAckingAspect.removeAcksToSend (fromNode, ack.getSpecificAcks());
      }

      //  Handle setting up possible return messages for the message received -
      //  for regular messages pure acks, and for pure acks pure ack-acks.

      if (MessageUtils.isAckableMessage(msg) && ack.isRegularAck())
      {
        //  Create a pure ack for this message add it to our pure ack sender.  The sender ensures
        //  pure acks for received  messages get sent if there is not enough regular message 
        //  traffic back to the originating node to piggyback acks on.

        PureAck pureAck = new PureAck();
        pureAck.setAckSendableTime (ackSendableTime);
        int firstAck = (int)((float)ack.getAckWindowUnitTimeSlot() * MessageAckingAspect.firstAckPlacingFactor);
        pureAck.setSendDeadline (ack.getReceiveTime() + firstAck);
//pureAck.setSendDeadline (ack.getReceiveTime() + 15);  // testing
        PureAckMessage pureAckMsg = new PureAckMessage (msg, pureAck);
        MessageAckingAspect.pureAckSender.add (pureAckMsg);
      }
      else if (ack.isPureAck())
      {
        //  Create an ack-ack for this ack message and add it to our pure ack-ack sender.  
        //  The sender ensures acks-acks for received acks get sent if there is not 
        //  enough regular message traffic back to the originating node to let it know
        //  that its ack has been received.  Only one ack-ack at most is sent in return
        //  for any and all the acks that one message can potentially generate.

        PureAckAck pureAckAck = new PureAckAck ((PureAck)ack);
        int onlyAckAck = (int)((float)ack.getSenderRoundtripTime() * MessageAckingAspect.ackAckPlacingFactor);
        pureAckAck.setSendDeadline (ack.getReceiveTime() + onlyAckAck);
//pureAckAck.setSendDeadline (ack.getReceiveTime() + 5);  // testing
        PureAckAckMessage pureAckAckMsg = new PureAckAckMessage ((PureAckMessage)msg, pureAckAck);
        MessageAckingAspect.pureAckAckSender.add (pureAckAckMsg);
      }
    }

    //  Finally send on the message if it the kind we deliver, otherwise declare success

    if (!ack.isPureAck() && !ack.isPureAckAck())
    {
      return super.deliverMessage (msg, dest);
    }
    else
    {
      //  We don't send pure acks et al on, but they were successfully delivered to us

      status = MessageAttributes.DELIVERY_STATUS_DELIVERED;
      result.setAttribute (MessageAttributes.DELIVERY_ATTRIBUTE, status);
      return result;
    }
  }

  private Vector updateFields (Vector acks, Ack ack)
  {
    if (acks == null) return new Vector();

    //  Update the fields of the individual acks

    long rtime = ack.getReceiveTime();
    String rlink = MessageAckingAspect.getLinkType (ack.getLastReceiveLink());
    String slink = MessageAckingAspect.getLinkType (ack.getSendLink());

    for (Enumeration a=acks.elements(); a.hasMoreElements(); )
    {
      AckList ackList = (AckList) a.nextElement();
      ackList.setReceiveTime (rtime);
      ackList.setSendLinkType (rlink);
      ackList.setReceiveLinkType (slink);
    }

    return acks;
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
