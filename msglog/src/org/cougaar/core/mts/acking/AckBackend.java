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
import java.text.DateFormat;

import org.cougaar.core.mts.*;
import org.cougaar.core.service.LoggingService;


class AckBackend extends MessageDelivererDelegateImplBase 
{
  private MessageAckingAspect aspect;
  private LoggingService log;
  private MessageAttributes success;

  public AckBackend (MessageDeliverer deliverer, MessageAckingAspect aspect) 
  {
    super (deliverer);
    this.aspect = aspect;
    log = aspect.getTheLoggingService();

    success = new SimpleMessageAttributes();
    String status = MessageAttributes.DELIVERY_STATUS_DELIVERED;
    success.setAttribute (MessageAttributes.DELIVERY_ATTRIBUTE, status);
  }

  public MessageAttributes deliverMessage (AttributedMessage msg, MessageAddress dest) throws MisdeliveredMessageException
  {
    String msgString = MessageUtils.toString (msg);
    if (log.isDebugEnabled()) log.debug ("AckBackend: entered by " +msgString);

    //  Special Case:  Local messages are completely excluded from acking

    if (MessageUtils.isLocalMessage (msg)) 
    {
      if (log.isDebugEnabled()) log.debug ("AckBackend: delivering local msg: " +msgString);
      return super.deliverMessage (msg, dest);
    }

    //  Sanity Check:  The dest argument appears vestigial and is not supported

    if (!msg.getTarget().equals(dest))
    {
      log.error ("AckBackend: Message target ["+msg.getTarget()+"] != dest ["+dest+"]");
      throw new MisdeliveredMessageException (msg);
    }

    //  Sanity Check: Message must have some kind of ack in it!

    Ack ack = MessageUtils.getAck (msg);

    if (ack == null)
    {
      log.error ("AckBackend: Message has no ack in it! (msg ignored): " +msgString);
      return success;
    }

    //  Get started

    ack.setReceiveTime (now());  // establish our own message reception time
    ack.setMsg (msg);            // reset transient msg field

    if (log.isInfoEnabled())
    {
      StringBuffer buf = new StringBuffer();
      buf.append ("<");
      buf.append (AdaptiveLinkSelectionPolicy.getLinkLetter(ack.getSendLink()));
      buf.append (" ");
      buf.append (MessageUtils.getMessageNumber(msg));
      buf.append (".");
      buf.append (ack.getSendCount());
      buf.append (" ");
      buf.append (MessageUtils.getMessageTypeLetter(msg));

      if (ack.isSomePureAck())
      {
        buf.append ("(");
        buf.append (MessageUtils.getSrcMsgNumber(msg));
        buf.append (")");
      }

      buf.append (" ");
      buf.append (MessageUtils.toAltShortSequenceID(msg));
      log.info (buf.toString());
    }

    if (log.isDebugEnabled())
    {
      StringBuffer buf = new StringBuffer();
      buf.append ("\n\n");
      buf.append ("Received ");
      buf.append (MessageUtils.toShortString (msg));
      buf.append (" via ");
      buf.append (MessageAckingAspect.getLinkType (ack.getSendLink()));
      buf.append (" of ");
      buf.append (MessageUtils.getAckingSequenceID (msg));
      buf.append ("\n");
      AckList.printAcks (buf, "specific", ack.getSpecificAcks());
      AckList.printAcks (buf, "  latest", ack.getLatestAcks());
      log.info (buf.toString());         
    }

    //  We do a series of integrity checks on the received message.  Who knows where
    //  this "message" really came from, where it has been, and what happened on
    //  the way...  These are only some basic checks; other entites such as message 
    //  security (if activated) may have done or do other checks.  
    //
    //  NOTE:  For now we return success even if we find the message mal-formed and
    //  reject it (ignore it).  If the message came from a legimate source it will
    //  resend it, and perhaps this time be well-formed.

    String type = MessageUtils.getMessageType (msg);

    if (type == null)
    {
      log.error ("AckBackend: Missing message type (msg ignored): " +msgString);
      return success;
    }

    int i;
    String validTypes[] = MessageUtils.getValidMessageTypes();
    for (i=0; i<validTypes.length; i++) if (type.equals (validTypes[i])) break;

    if (i == validTypes.length)
    {
      log.error ("AckBackend: Message not a valid type ("+type+") (msg ignored): " +msgString);
      return success;
    }

    if (!MessageUtils.hasMessageNumber (msg))
    {
      log.error ("AckBackend: Missing message number (msg ignored): " +msgString);
      return success;
    }

    int msgNum = MessageUtils.getMessageNumber (msg);

    if (ack.isAck() && msgNum < 0)
    {
      if (!MessageUtils.isPingMessage (msg))  // pings are an exception
      {
        log.error ("AckBackend: Invalid msg number (msg ignored): " +msgString);
        return success;
      }
    }

    if (ack.isPureAck() && msgNum >= 0)
    {
      log.error ("AckBackend: Invalid msg number (pure ack msg ignored): " +msgString);
      return success;
    }

    if (ack.isPureAckAck() && msgNum >= 0)
    {
      log.error ("AckBackend: Invalid msg number (pure ack-ack msg ignored): " +msgString);
      return success;
    }

    if (ack.isSomePureAck() && !MessageUtils.haveSrcMsgNumber(msg))
    {
      log.error ("AckBackend: Missing src msg number (some pure ack msg ignored): " +msgString);
      return success;
    }

    String fromNode = MessageUtils.getFromAgentNode (msg);

    if (fromNode == null || fromNode.equals(""))
    {
      log.error ("AckBackend: Missing from node (msg ignored): " +msgString);
      return success;
    }

    String toNode = MessageUtils.getToAgentNode (msg);

    if (toNode == null || toNode.equals(""))
    {
      log.error ("AckBackend: Missing to node (msg ignored): " +msgString);
      return success;
    }

    if (!aspect.getThisNode().equals (toNode))
    {
      log.error ("AckBackend: Message not for this node (msg ignored): " +msgString);
      return success;
    }
    
    if (ack.getSendCount() < 1)
    {
      int sendCount = ack.getSendCount();
      log.error ("AckBackend: Msg has bad sendCount (" +sendCount+ ") (msg ignored): " +msgString);
      return success;
    }

    if (ack.isAck() && ack.getResendMultiplier() < 1)
    {
      log.error ("AckBackend: Msg has bad resendMultiplier (msg ignored): " +msgString);
      return success;
    }

    try
    {
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
      log.error ("AckBackend: Msg contains invalid acks (msg ignored): " +msgString);
      return success;
    }
/*
    Can't do this integrity check yet - msg system architecture wrong for it

    if (getLinkType (ack.getSendLink()) != actual receive link type)
    {
      if (MessageAckingAspect.debug) 
      {
        log.error ("AckBackend: ALERT msg came in on wrong link - msg ignored: " +msgString); 
        return success;
      }
    }
*/
    if (ack.getSendTime() < now() - MessageAckingAspect.messageAgeWindowInMinutes*60*1000)
    {
      //  Kind of a hack, but the start of something.  May want to do things like set
      //  max age based on transport type ...

      if (log.isWarnEnabled()) 
      {
        long mins = (now()-ack.getSendTime()) / (60*1000);
        String date = (new Date (ack.getSendTime())).toString();
        log.warn ("AckBackend: Msg too old [" +mins+ "minutes: " +date+ "] (msg ignored): " +msgString);
      }

      return success;
    }

    if (ack.getSendTime() > now() + MessageAckingAspect.messageAgeWindowInMinutes*60*1000)
    {
      //  Kind of a hack, but the start of something.  May want to do things like set
      //  max age based on transport type ...

      if (log.isWarnEnabled()) 
      {
        String date = (new Date(ack.getSendTime())).toString();
        log.warn ("AckBackend: Msg too far in the future [" +date+ "] (msg ignored): " +msgString);
      }

      return success;
    }

    //  Handle messages from out-of-date senders or to out-of-date receivers
    //  NOTE:  May want to send NACKs back for these messages.

// NACK Notes
// if (regular msg)  not pure ack, ack ack, ping, etc
// if (not a duplicate) // only send once?
//   send it or schedule it
//   if sched, need nack vs. pure nack
//   a pure nack has no msg num - it is not acked?

    if (!isLatestAgentIncarnation (MessageUtils.getFromAgent (msg), MessageUtils.getOriginatorAgent (msg)))
    {
      if (log.isInfoEnabled()) 
        log.info ("AckBackend: Msg has out of date (or unknown) sender (msg ignored): " +msgString);
      return success;
    }

    if (!isLatestAgentIncarnation (MessageUtils.getToAgent (msg), MessageUtils.getTargetAgent (msg)))
    {
      if (log.isInfoEnabled()) 
        log.info ("AckBackend: Msg has out of date (or unknown) recipient (msg ignored): " +msgString);
      return success;
    }

    //  At this point we feel the received message is legitimate, so now we deliver
    //  it and record its reception while making sure that it is not a message we have 
    //  already received and delivered.  Note that there is also checking for duplicate
    //  messages in the MessageOrderingAspect, but that is ok because message ordering 
    //  is orthogonal to acking and is not required to be active.

    synchronized (this)
    {
      if (!MessageAckingAspect.wasSuccessfulReceive (msg))
      {
        //  Actually deliver the message.  Mis-deliveries throw an exception.

        if (ack.isAck()) super.deliverMessage (msg, dest);

        //  Add the message to our list of received messages.  We don't do this
        //  for non-acked msgs like heartbeats & traffic masking messages.

        if (msgNum != 0) MessageAckingAspect.addSuccessfulReceive (msg);
      }
      else
      {
        //  A duplicate message - right now we just drop them cold

        if (log.isInfoEnabled()) log.info ("AckBackend: Duplicate msg ignored: " +msgString);

// TODO: sched pure acks for dups (only reg. message dups)

        return success;
      }
    }

    //  Disburse the acks contained in the message

    MessageAckingAspect.addReceivedAcks (fromNode, ack.getLatestAcks());  // all have latest acks

    long ackSendableTime = 0;

    if (ack.isAck())
    {
      //  Record the specific acks contained within this message.  Used to acknowledge 
      //  sent messages waiting for acks.  This call also dings the message resender to
      //  announce that new ack data has arrived for the waiting messages.

      MessageAckingAspect.addReceivedAcks (fromNode, ack.getSpecificAcks());

      if (msgNum != 0)
      {
        //  Add the message data to our outbound acks list (we send back acks for
        //  these messages we've received, until those acks are acked).  We record
        //  the time that this is set, so that we know any message sent back to the 
        //  source node after this time will include this ack (as long as needed).
    
        MessageAckingAspect.addAckToSend (msg);
        ackSendableTime = now();
      }
    }
    else if (ack.isPureAck())
    {
      //  Pure acks only have the latest acks, already dealt with above
    }
    else if (ack.isPureAckAck())
    {
      //  Remove the specific acks contained within this pure ack-ack message 
      //  from our ack sending list as those acks are now officially acked.

      MessageAckingAspect.removeAcksToSend (fromNode, ack.getSpecificAcks());
    }

    //  Handle setting up possible return messages for the message received.
    //  For regular messages we send back pure ack messages, and for pure ack 
    //  messages we send back pure ack-ack messages.  Howecer, if the link
    //  the received message came in on is excluded from acking, then we won't
    //  send back any kind of pure ack messages.

    if (!MessageAckingAspect.isExcludedLink (ack.getSendLink()))
    {
      if (ack.isAck() && msgNum != 0)
      {
        //  Create a pure ack for this message add it to our pure ack sender.  The sender ensures
        //  pure acks for received  messages get sent if there is not enough regular message 
        //  traffic back to the originating node to piggyback acks on.

        PureAck pureAck = new PureAck();
        pureAck.setAckSendableTime (ackSendableTime);
        float myTimeout = (float)(ack.getResendTimeout() - ack.getRTT());
        int firstAck = (int)(myTimeout * MessageAckingAspect.firstAckPlacingFactor);
        pureAck.setSendDeadline (ack.getReceiveTime() + firstAck);
//pureAck.setSendDeadline (ack.getReceiveTime() + 15);  // testing
        PureAckMessage pureAckMsg = new PureAckMessage (msg, pureAck);

        if (log.isDebugEnabled()) 
        {
          long t = pureAck.getSendDeadline() - now();
          log.debug ("AckBackend: Created new pure ack msg with timeout of " +t+ ": " +pureAckMsg);
        }

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
        float rtt = (float) ack.getRTT();
        int onlyAckAck = (int)(rtt * MessageAckingAspect.ackAckPlacingFactor);
        pureAckAck.setSendDeadline (ack.getReceiveTime() + onlyAckAck);
//pureAckAck.setSendDeadline (ack.getReceiveTime() + 5);  // testing
        PureAckAckMessage pureAckAckMsg = new PureAckAckMessage ((PureAckMessage)msg, pureAckAck);

        if (log.isDebugEnabled()) 
        {
          long t = pureAckAck.getSendDeadline() - now();
          log.debug ("AckBackend: Created new pure ack-ack msg with timeout of " +t+ ": " +pureAckAckMsg);
        }

        MessageAckingAspect.pureAckAckSender.add (pureAckAckMsg);

        //  We can set a final send deadline for ack-acks, in case it gets bounced
        //  around by send retries (ack-acks are never resent) or other delays.

        // long deadline = now() + 2*onlyAckAck;        
        // MessageUtils.setSendDeadline (msg, deadline);
      }
    }

    //  We don't send pure acks et al on, but they were successfully delivered to us

    return success;
  }

  //  Utility methods

  private boolean isLatestAgentIncarnation (AgentID agent, MessageAddress agentAddr)
  {
    if (agent == null) return false;

    //  Check topology server to see if the given agent is still current

    AgentID topoAgent = null;

    try 
    { 
      topoAgent = AgentID.getAgentID (aspect, aspect.getServiceBroker(), agentAddr); 
    }
    catch (NameLookupException nle)
    {}
    catch (Exception e) 
    { 
      e.printStackTrace(); 
    }

    if (topoAgent == null) 
    {
      if (log.isInfoEnabled()) log.info ("AckBackend: No topology info for agent " +agent.getAgentName());
      return false;  // correct ans is don't know
    }

    //  Compare data

    if (!topoAgent.getNodeName().equals(agent.getNodeName())) return false;
    if (topoAgent.getAgentIncarnationAsLong() > agent.getAgentIncarnationAsLong()) return false;

    return true;
  }

  private static long now ()
  {
    return System.currentTimeMillis();
  }

  private static String toDateString (long time)
  {
    TimeZone tz = TimeZone.getDefault();
    Calendar cal = Calendar.getInstance (tz);
    Date date = new Date (time);
    return DateFormat.getDateTimeInstance().format (date);        
  }
}
