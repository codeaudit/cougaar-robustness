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

    //  Get the ack from the message and handle the case when there is no ack

    Ack ack = MessageUtils.getAck (msg);

    if (ack == null)
    {
      if (MessageAckingAspect.isAckingOn()) 
      {
        System.err.println ("ERROR: Message has no ack in it! : " + msg);
        throw new MisdeliveredMessageException (msg);
      }

      return super.deliverMessage (msg, dest);
    }

    ack.setReceiveTime (now());  // establish message delivery time
    ack.setMsg (msg);            // reset transient msg field

    int msgNum = MessageUtils.getMessageNumber (msg);
    String fromNode = MessageUtils.getFromAgentNode (msg);
    String hdr = "\nReceived " + MessageUtils.toString (msg);

    if (MessageAckingAspect.showTraffic) 
    {
      String lnk = MessageAckingAspect.getLinkType (ack.getSendLink());  // HACK - need this from system, not msg
      System.err.println (hdr+ " via " +lnk);
    }

    //  Here we do integrity checks on the received message.  Who knows where
    //  this "message" really came from, where it has been, and what happened on
    //  the way...  These are only some basic checks, certainly when message
    //  security is implemented things will get even tighter.

    //  NOTE: We are currently limited to throwing MisdeliveredMessageException's,
    //  may want to create new exceptions in the future.

// HACK - no appropriate official status defined yet

    MessageAttributes result = new SimpleMessageAttributes();
//  String status = "MessageRejected";
    String status = MessageAttributes.DELIVERY_STATUS_DELIVERED;
    result.setAttribute (MessageAttributes.DELIVERY_ATTRIBUTE, status);

/*
    Can't do this important integrity check yet - msg system architecture wrong for it

    if (getLinkType (ack.getSendLink()) != actual receive link type)
    {
      if (MessageAckingAspect.debug) 
      {
        System.err.println ("ALERT msg " +msgNum+ ": came in on wrong link - msg ignored."); 
        return result;
      }
    }
*/
    if (ack.isRegularAck() && msgNum <= 0)
    {
      if (MessageAckingAspect.debug) 
      {
        System.err.println (hdr+ ": invalid msg number - msg ignored.");
      }

      return result;
    }

    if (ack.isPureAck() && msgNum >= 0)
    {
      if (MessageAckingAspect.debug) 
      {
        System.err.println (hdr+ ": invalid msg number - ack msg ignored.");
      }

      return result;
    }

    if (ack.isPureAckAck() && msgNum >= 0)
    {
      if (MessageAckingAspect.debug) 
      {
        System.err.println (hdr+ ": invalid msg number - ack-ack msg ignored.");
      }

      return result;
    }

    if (fromNode == null || fromNode.equals(""))
    {
      if (MessageAckingAspect.debug) 
      {
        System.err.println (hdr+ " from unknown node - msg ignored.");
      }

      return result;
    }

    if (ack.getSendTime() < now() - MessageAckingAspect.messageAgeWindow*60*1000)
    {
      //  Kind of a hack, but the start of something.  May want to do things like set
      //  max age based on transport type ...
      
      if (MessageAckingAspect.debug) 
      {
        System.err.println (hdr+ " is too far in the past - msg ignored.");
      }

      return result;
    }

    if (ack.getSendTime() > now() + MessageAckingAspect.messageAgeWindow*60*1000)
    {
      //  Kind of a hack, but the start of something.  May want to do things like set
      //  max age based on transport type ...
      
      if (MessageAckingAspect.debug) 
      {
        System.err.println (hdr+ " is too far in the future - msg ignored.");
      }

      return result;
    }

    if (MessageAckingAspect.isAckingOn())
    {
      try
      {
        if (MessageAckingAspect.debug) 
        {
          System.err.println (hdr.substring(1)+ " contains acks: ");
          AckList.printAcks (ack.getAcks());
          System.err.println ("Inbound roundtrip time: " + ack.getSenderRoundtripTime());
        }

        for (Enumeration a=ack.getAcks().elements(); a.hasMoreElements(); )
        {
          NumberList.checkListValidity ((AckList) a.nextElement());
        }
      }
      catch (Exception e)
      {
        //  NOTE:  Maybe just throw out back acks?

        if (MessageAckingAspect.debug) 
        {
          System.err.println (hdr+ " has bad ack list - msg ignored.");
        }

        return result;
      }
    
      if (ack.getSendCount() < 1)
      {
        if (MessageAckingAspect.debug) 
        {
          System.err.println (hdr+ " has bad sendCount - msg ignored.");
        }

        return result;
      }

      if (ack.isRegularAck() && ack.getResendMultiplier() < 1)
      {
        if (MessageAckingAspect.debug) 
        {
          System.err.println (hdr+ " has bad resendMultiplier - msg ignored.");
        }

        return result;
      }
    }

    //  At this point we feel the received message is legit, and we try to add it to our
    //  received message numbers list.  If we have already seen this message we reject 
    //  it as a duplicate.  Note again there is redundant functionality for this in the 
    //  MessageOrderingAspect, but that is ok because message ordering is orthogonal to 
    //  acking and is not required to be active.

    if (msgNum != 0)
    {
      if (MessageAckingAspect.addSuccessfulReceive (msg) == false)
      {
        if (MessageAckingAspect.debug) 
        {
          System.err.println (hdr+ " is a duplicate - msg ignored.");
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

      //  Deal with the acks contained in the message

      Vector acks = ack.getAcks();
      long ackSendableTime = 0;

      if (ack.isRegularAck() || ack.isPureAck())
      {
        //  Update the fields of the individual acks (ackLists)

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

        //  Record the acks contained within this message.  Used to acknowledge 
        //  sent messages waiting for acks and measuring transport roundtrip times.
        //  This call also dings the ack waiter to announce new ack data has arrived.

        MessageAckingAspect.addReceivedAcks (fromNode, acks);

        if (ack.isRegularAck())
        {
          //  Add the message to our outbound acks list (we send back acks for all 
          //  regular messages received, until those acks are acked).  We record
          //  the time that this is set, so that we know any message sent back to the 
          //  source node after this time will include this ack (as long as needed).
      
          MessageAckingAspect.addAckToSend (msg);
          ackSendableTime = now();
        }
      }
      else if (ack.isPureAckAck())
      {
        //  Remove the acks contained within this message from our sending list, 
        //  as they are now officially acked.

        MessageAckingAspect.removeAcksToSend (fromNode, acks);
      }

      //  Handle setting up possible return messages for the message received -
      //  for regular messages pure acks, and for pure acks pure ack-acks.

      if (ack.isRegularAck())
      {
        //  Create a pure ack for this message add it to our pure ack sender.  The sender ensures
        //  pure acks for received  messages get sent if there is not enough regular message 
        //  traffic back to the originating node to piggyback acks on.

        PureAck pureAck = new PureAck();
        pureAck.setAckSendableTime (ackSendableTime);
        int firstAck = (int)((float)ack.getAckWindowUnitTimeSlot() * MessageAckingAspect.firstAckPlacingFactor);
        pureAck.setSendDeadline (ack.getReceiveTime() + firstAck);
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
        PureAckAckMessage pureAckAckMsg = new PureAckAckMessage (msg, pureAckAck);
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
