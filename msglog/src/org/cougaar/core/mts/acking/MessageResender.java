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
 * 12 Aug 2002: Reworked to resend autonomously. Renamed from AckWaiter. (OBJS)
 * 08 Jun 2002: Revamped and streamlined for 9.2.x. (OBJS)
 * 23 Apr 2002: Split out from MessageAckingAspect. (OBJS)
 */

package org.cougaar.core.mts.acking;

import java.util.*;

import org.cougaar.core.mts.*;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.thread.CougaarThread;


/**
 **  This is where sent messages wait for their acks, and get resent
 **  if none show up in time.
 **/

class MessageResender implements Runnable
{
  private MessageAckingAspect aspect;
  private LoggingService log;
  private Vector queue;
  private AttributedMessage messages[];
  private boolean haveNewData;
  private Comparator deadlineSort;
  private long minResendDeadline;

  public MessageResender (MessageAckingAspect aspect) 
  {
    this.aspect = aspect;
    queue = new Vector();
    messages = new AttributedMessage[32];
    haveNewData = false;
    deadlineSort = new DeadlineSort();
    minResendDeadline = Long.MAX_VALUE;
  }

  public void add (AttributedMessage msg) 
  {
    //  Sanity checks

    if (MessageUtils.isSomePureAckMessage (msg)) return;
    if (MessageUtils.getMessageNumber(msg) == 0) return;

    if (debug()) log.debug ("MessageResender: adding " +MessageUtils.toString(msg));

    //  Possibly add (or remove) the message to (from) the agent state

    if (MessageUtils.isRegularMessage (msg))
    {
      AgentState agentState = aspect.getAgentState (MessageUtils.getOriginatorAgent(msg));

      if (agentState != null)
      {
        boolean acked = MessageAckingAspect.wasSuccessfulSend (msg);
        boolean zeroCount = (MessageUtils.getAck(msg).getSendCount() == 0);
        boolean removed = false;
        boolean added = false;

        if (acked || zeroCount)
        {
          synchronized (agentState)
          {
            Vector v = (Vector) agentState.getAttribute (MessageAckingAspect.SENT_BUT_NOT_ACKED_MSGS);
         
            if (acked)
            {
              if (v != null) 
              {
                v.remove (msg);
                removed = true;
              }
            }
            else if (zeroCount)
            {
              if (v == null) 
              {
                v = new Vector();
                agentState.setAttribute (MessageAckingAspect.SENT_BUT_NOT_ACKED_MSGS, v);
              }

              v.add (msg);
              added = true;
            }
          }
        }

        if (debug() && (removed || added)) 
        {
          String s = (removed? "removed" : "added");
          log.debug ("MessageResender: "+s+" to agentState: " +MessageUtils.toString(msg));
        }
      } 
    }

    //  Add the message to the waiting queue

    synchronized (queue) 
    {
      queue.add (msg);
      ding();  // set minResendDeadline if nothing else
    }
  }

  public void remove (AttributedMessage msg) 
  {
    //  Sanity checks

    if (MessageUtils.isSomePureAckMessage (msg)) return;
    if (MessageUtils.getMessageNumber(msg) == 0) return;

    if (debug()) log.debug ("MessageResender: removing " +MessageUtils.toString(msg));

    //  Possibly remove the message from the agent state

    if (MessageUtils.isRegularMessage (msg))
    {
      AgentState agentState = aspect.getAgentState (MessageUtils.getOriginatorAgent(msg));
      boolean removed = false;

      if (agentState != null)
      {
        if (MessageUtils.getAck(msg).getSendCount()==0 || MessageAckingAspect.wasSuccessfulSend(msg))
        {
          synchronized (agentState)
          {
            Vector v = (Vector) agentState.getAttribute (MessageAckingAspect.SENT_BUT_NOT_ACKED_MSGS);
         
            if (v != null) 
            {
              v.remove (msg);
              removed = true;
            }
          }
        }
      }

      if (removed && debug()) log.debug ("MessageResender: removed from agentState: " +MessageUtils.toString(msg));
    }

    //  Remove the message from the waiting queue.  This can raise the minResendDeadline, 
    //  but doesn't seem like enough of an event to do a ding().

    synchronized (queue) 
    {
      queue.remove (msg);
    }
  }

  public boolean scheduleImmediateResend (AttributedMessage msg) 
  {
    boolean sched = false;

    synchronized (queue) 
    {
      sched = queue.contains (msg);  // msg must already be on queue
    }

    if (debug()) // don't want to log inside sync (log has alien methods)
    {
      String s = (sched ? "S" : "DID NOT s") + "chedule immediate resend ";
      log.debug ("MessageResender: " + s + MessageUtils.toString(msg));
    }

    if (sched)
    {
      Ack ack = MessageUtils.getAck (msg);
      ack.setSendTime (now() - (ack.getResendTimeout() + ack.getResendDelay() + 1));
      ding();
    }

    return sched;
  }

  public void ding ()
  {
    //  We get dinged when new messages are added to our queue and when
    //  new acks are received by the ack backend.

    synchronized (queue) 
    {
      haveNewData = true;  // new msgs or acks
      queue.notify();
    }
  }

  private boolean debug ()
  {
    if (log == null) log = aspect.getTheLoggingService();
    return (log != null ? log.isDebugEnabled() : false);
  }

  public void run() 
  {
    int len;

    while (true) 
    {
      //  Wait until we have some new messages or acks or we have timed out to 
      //  re-examine old messages.

      synchronized (queue) 
      {
        while (true)
        {
          //  If we have new data (new msgs and/or acks) and a non-empty msg queue, 
          //  don't wait.  New data can arrive during waiting or during queue processing.

          if (haveNewData)
          {
            haveNewData = false;
            if (queue.size() > 0) break;
          }

          //  Check how long to wait before we need to satisfy a resend deadline

          long waitTime = 0;  // 0 = wait till notify (or interrupt)

          if (queue.size() > 0)
          {
            if (minResendDeadline < Long.MAX_VALUE)
            {
              waitTime = minResendDeadline - now();
              if (waitTime <= 0) break;
            }
          }

          //  Wait for a specified time or until notify

          try { queue.wait (waitTime); } catch (Exception e) {}
        }

        messages = (AttributedMessage[]) queue.toArray (messages);  // try array reuse
        len = queue.size();
        if (len > 1) Arrays.sort (messages, 0, len, deadlineSort);
      }

      //  Next we try to match the messages with the acks we've collected so far.
      //  Possible many-to-many relationship between the messages and the acks.

      if (len > 0 && debug()) 
      {
        log.debug ("MessageResender: reviewing queue (" +len+ " msg" +(len==1? ")" : "s)"));
      }

      for (int i=0; i<len; i++)
      {
        AttributedMessage msg = messages[i];
        
        //  Avoid already successful sends (when a message is acked it is declared
        //  and recorded as a successful send).

        if (MessageAckingAspect.wasSuccessfulSend (msg))
        {
          if (debug()) log.debug ("MessageResender: Dropping already successful " +MessageUtils.toString(msg));
          remove (msg);
          messages[i] = null;
          continue;
        }

        //  Search for match of message with current received ack data

        int msgNum = MessageUtils.getMessageNumber (msg);
        AckList ackList = AckList.findFirst (MessageAckingAspect.getReceivedAcks(msg), msgNum);

        if (ackList != null)
        {
          //  We have an ack!!

          if (debug()) log.debug ("MessageResender: Got ack for " +MessageUtils.toString(msg));
          Ack ack = MessageUtils.getAck (msg);
          String toNode = MessageUtils.getToAgentNode (msg);
          MessageAckingAspect.addSuccessfulSend (msg);                
          MessageAckingAspect.updateMessageHistory (ack, ackList);
          MessageAckingAspect.removeAcksToSend (toNode, ack.getSpecificAcks()); // acks now acked
          remove (msg);
          messages[i] = null;
        }
      }
      
      //  For any remaining un-acked messages we need to decide if
      //  it is time to try to resend the message.

      minResendDeadline = Long.MAX_VALUE;

      for (int i=0; i<len; i++) if (messages[i] != null)
      {
        AttributedMessage msg = messages[i];
        Ack ack = MessageUtils.getAck (msg);

        //  See if time to resend message

        int timeout = ack.getResendTimeout() + ack.getResendDelay();
        long resendDeadline = ack.getSendTime() + timeout;
        long timeLeft = resendDeadline - now();

        if (debug()) log.debug ("MessageResender: Msg " +MessageUtils.getMessageNumber(msg)+
          ": timeout=" +timeout+ "  timeLeft=" +timeLeft+ "  " +MessageUtils.toShortSequenceID(msg));

        if (timeLeft <= 0)
        {
          //  Time to resend the message.  SendMessage re-inserts the message at the
          //  front of the whole send pipeline: it'll come through the AckFrontend
          //  again on its way out to an outgoing link.

          if (debug()) log.debug ("MessageResender: Resending " +MessageUtils.toString(msg));
          remove (msg);  // remove first to avoid race condition with send
          aspect.sendMessage (msg);

          //  Start adding delay to resending this message if it keeps coming back around

          int highSendCount = ack.getNumberOfLinkChoices() + 1;
          if (ack.getSendCount() > highSendCount) ack.addResendDelay (500);
        }
        else
        {
          if (resendDeadline < minResendDeadline) minResendDeadline = resendDeadline;
        }
      }

      Arrays.fill (messages, null);  // release references
    }
  }

  private static class DeadlineSort implements Comparator
  {
    public int compare (Object m1, Object m2)
    {
      if (m1 == null)  // drive nulls to bottom (top is index 0)
      {
        if (m2 == null) return 0;
        else return 1;
      }
      else if (m2 == null) return -1;

      //  Sort on resend deadline (sooner deadlines come first)

      Ack a1 = MessageUtils.getAck ((AttributedMessage) m1);
      Ack a2 = MessageUtils.getAck ((AttributedMessage) m2);

      long d1 = a1.getSendTime() + a1.getResendTimeout() + a1.getResendDelay();
      long d2 = a2.getSendTime() + a2.getResendTimeout() + a2.getResendDelay();

      if (d1 == d2) return 0;
      return (d1 > d2 ? 1 : -1);
    }

    public boolean equals (Object obj)
    {
      return (this == obj);
    }
  }

  private static long now ()
  {
    return System.currentTimeMillis();
  }
}
