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
 * 18 Aug 2002: Mucho changes to support Cougaar 9.2+ and agent mobility. (OBJS)
 * 23 Apr 2002: Split out from MessageAckingAspect. (OBJS)
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
      buf.append (MessageUtils.toShortSequenceID(msg));
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

    if (!MessageIntegrity.areMessageAttributesOK (msg, log))
    {
      log.error ("AckBackend: Message attributes fail integrity test (msg ignored): " +msgString);
      return success;
    }
/*
Mobility issue: arriving agent is now local, msg was for it when it was on other node

    //  Is this message for us?  We only check the node.  We don't check that the target
    //  agent exists in this node here because we want the acks out of the message.

    if (!MessageUtils.getToAgentNode(msg).equals (aspect.getThisNode()))
    {
      log.error ("AckBackend: Message not for this node (msg ignored): " +msgString);
      return success;
    }
*/
    //  Can't do this integrity check yet - msg system architecture wrong for it
/*
    if (getLinkType (ack.getSendLink()) != actual receive link type)
    {
      if (MessageAckingAspect.debug) 
      {
        log.error ("AckBackend: ALERT msg came in on wrong link - msg ignored: " +msgString); 
        return success;
      }
    }
*/
    //  Check if message is too old or too ahead.  May want to move to protocol 
    //  indexed age windows.

    if (ack.getSendTime() < now() - MessageAckingAspect.messageAgeWindowInMinutes*60*1000)
    {
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
      if (log.isWarnEnabled()) 
      {
        String date = (new Date(ack.getSendTime())).toString();
        log.warn ("AckBackend: Msg too far in the future [" +date+ "] (msg ignored): " +msgString);
      }

      return success;
    }

    //  Handle messages from out-of-date senders or to out-of-date receivers.
    //  NOTE:  May want to send NACKs back for these messages.

    // NACK Notes
    // if (regular msg)  not pure ack, ack ack, ping, etc
    // if (not a duplicate) // only send once?
    //   send it or schedule it
    //   if sched, need nack vs. pure nack
    //   a pure nack has no msg num - it is not acked?

if (!MessageAckingAspect.skipIncarnationCheck || ack.getSendLink().equals("org.cougaar.core.mts.email.OutgoingEmailLinkProtocol"))
{
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
}
else
{
  if (log.isDebugEnabled()) log.debug ("Skipping incarnation check");
}

    //  At this point we feel the received message is probably ok, so now we deliver
    //  it and record its reception while making sure that it is not a message we have 
    //  already received and delivered.  Note that there is also checking for duplicate
    //  messages in the MessageOrderingAspect, but that is ok because message ordering 
    //  is orthogonal to acking and is not required to be active.

    int msgNum = MessageUtils.getMessageNumber (msg);

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
        //  A duplicate message - right now we drop them, although we do schedule a pure 
        //  ack for ackable messages.

        if (ack.isAck() && msgNum != 0)
        {
          if (log.isInfoEnabled()) log.info ("AckBackend: Duplicate msg dropped, " +
            "pure ack scheduled: " +msgString);

          //  Disable any acks the message is carrying as a precaution for funny business

          ack.setLatestAcks (null);
          ack.setSpecificAcks (null);
        }
        else
        {
          if (log.isInfoEnabled()) log.info ("AckBackend: Duplicate msg ignored: " +msgString);
          return success;
        }
      }
    }

    //  Disburse any acks contained in the message

    String fromNode = MessageUtils.getFromAgentNode (msg);
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

    //  Mobility Note:  If a pure ack message came to us addressed to another node 
    //  (assumed to be the old node) we skip sending back an ack-ack.

    boolean skip = false;
  
    if (ack.isPureAck() && !MessageUtils.getToAgentNode(msg).equals(aspect.getThisNode()))
    {
      if (log.isDebugEnabled()) log.debug ("AckBackend: Skipping ack-ack scheduling for ack not " +
        "addressed to us: " +msgString);
      skip = true;
    }

    if (!skip && !MessageAckingAspect.isExcludedLink (ack.getSendLink()))
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
    catch (NameLookupException e)
    {
      //  Thrown by AgentID.getAgentID when can't get and don't have any topo data for agent
    } 
    catch (Exception e) 
    { 
      if (log.isDebugEnabled()) log.debug ("AgentID.getAgentID for " +agentAddr+
        ": " +stackTraceToString(e));
    }

    if (topoAgent == null) 
    {
      if (log.isInfoEnabled()) log.info ("AckBackend: No topology info for agent " +agent.getAgentName());
      return false;  // we have to assume
    }

    //  Compare data

    // if (!topoAgent.getNodeName().equals(agent.getNodeName())) return false;
    if (topoAgent.getAgentIncarnationAsLong() > agent.getAgentIncarnationAsLong()) return false;

    return true;
  }

  private static String toDateString (long time)
  {
    TimeZone tz = TimeZone.getDefault();
    Calendar cal = Calendar.getInstance (tz);
    Date date = new Date (time);
    return DateFormat.getDateTimeInstance().format (date);        
  }

  private static long now ()
  {
    return System.currentTimeMillis();
  }

  private static String stackTraceToString (Exception e)
  {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter (stringWriter);
    e.printStackTrace (printWriter);
    return stringWriter.getBuffer().toString();
  }
}
