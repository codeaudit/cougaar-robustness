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
 * 08 May 2003: Remove PureAckAckMessages queued for an agent that has restarted (102B)
 * 24 Sep 2002: Revamped queue adds scheduling. (OBJS)
 * 08 Jun 2002: Revamped and streamlined for 9.2.x. (OBJS)
 * 23 Apr 2002: Split out from MessageAckingAspect. (OBJS)
 */

package org.cougaar.core.mts.acking;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;

import org.cougaar.core.mts.AgentID;
import org.cougaar.core.mts.MessageUtils;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.thread.CougaarThread;

/**
 **  Pure ack-acks are sent to stop the flow of pure acks
 **  in the situation when the receiving node is not ordinarily
 **  going to be sending back any traffic to the node (and thus
 **  have acks piggybacked on it.  Only one ack-ack at most is
 **  sent in response to pure acks.
 **/

class PureAckAckSender implements Runnable
{
  private MessageAckingAspect aspect;
  private LoggingService log;
  private Vector queue;
  private PureAckAckMessage messages[];
  private Comparator deadlineSort;
  private long minSendDeadline;

  public PureAckAckSender (MessageAckingAspect aspect) 
  {
    this.aspect = aspect;
    queue = new Vector();
    messages = new PureAckAckMessage[32];
    deadlineSort = new DeadlineSort();
    minSendDeadline = 0;
  }

  public void add (PureAckAckMessage paam) 
  {
    PureAckAck pureAckAck = (PureAckAck) MessageUtils.getAck (paam);
    long deadline = pureAckAck.getSendDeadline();

    if (debug()) log.debug ("PureAckAckSender: adding timeout=" +(deadline-now())+ " " +paam);

    synchronized (queue) 
    {
      queue.add (paam);
      boolean buildup = (queue.size() > 32);
      offerNewSendDeadline (!buildup ? deadline : Math.min(1000+now(),deadline));
    }
  }
  
  private void remove (PureAckAckMessage paam) 
  {
    synchronized (queue) 
    {
      queue.remove (paam);
      if (queue.size() == 0) offerNewSendDeadline (0);
    }
  }
    
  private void offerNewSendDeadline (long deadline)
  {
    if (deadline < 0) return;

    synchronized (queue) 
    {
      if (deadline < minSendDeadline || (minSendDeadline == 0 && deadline > 0))
      {
        minSendDeadline = deadline;
        queue.notify();
      }
    }
  }
  
  private boolean debug ()
  {
    if (log == null) log = aspect.getTheLoggingService();
    return (log != null ? log.isDebugEnabled() : false);
  }
  
  public void run () 
  {
    while (true)
    {
      String s = "PureAckAckSender: Unexpected exception, restarting thread";

      try
      { 
        try 
        { 
          doRun(); 
        } 
        catch (Exception e) 
        {
          s += ": " + stackTraceToString (e);
          log.error (s);
        }
      }
      catch (Exception e)
      {
        try { e.printStackTrace(); } catch (Exception ex) { /* !! */ }
      }
    }
  }

  private void doRun () 
  {
    int len;

    while (true) 
    {
      //  Wait until we have some new messages or we have timed out to 
      //  re-examine old messages.

      synchronized (queue) 
      {
        while (true)
        {
          //  Check how long to wait before we need to satisfy a send deadline

          long waitTime = 0;  // 0 = wait till notify (or interrupt)

          if (queue.size() > 0)
          {
            waitTime = minSendDeadline - now();
            if (waitTime <= 0) { minSendDeadline = 0;  break; }
          }

          //  Wait until timeout, notify, or interrupt

          try { queue.wait (waitTime); } catch (Exception e) {}
        }

        messages = (PureAckAckMessage[]) queue.toArray (messages);  // try array reuse
        len = queue.size();
        if (len > 1) Arrays.sort (messages, 0, len, deadlineSort);
      }

      //  Check if it is time to send a pure ack-ack message

      if (len > 0 && debug()) 
      {
        log.debug ("PureAckAckSender: reviewing queue (" +len+ " msg" +(len==1? ")" : "s)"));
      }

      for (int i=0; i<len; i++)
      {
        PureAckAckMessage paam = messages[i];
        PureAckAck pureAckAck = (PureAckAck) MessageUtils.getAck (paam);

        String node = MessageUtils.getToAgentNode (paam);
        long lastSendTime = MessageAckingAspect.getLastSendTime (node);

        //  We sent at most one ack-ack, and then only if there has
        //  not been any traffic back to the sending node since the
        //  time we received a pure ack from it.

        if (lastSendTime < pureAckAck.getReceiveTime())
        {
          long sendDeadline = pureAckAck.getSendDeadline();
          long timeLeft = sendDeadline - now();

          if (debug()) log.debug ("PureAckAckSender: timeLeft=" +timeLeft+ "  " +
            MessageUtils.toShortString(paam)+ " " +MessageUtils.toShortSequenceID(paam));

          if (timeLeft <= 0)
          {
            //  Time to send the ack-ack

            if (debug()) log.debug ("PureAckAckSender: Sending pure ack-ack " +paam);
            remove (paam);  // remove first to avoid race condition with send
            aspect.sendMessage (paam);
          }
          else 
          {
            offerNewSendDeadline (sendDeadline);
          }
        }
        else
        {
          if (debug()) log.debug ("PureAckAckSender: Dropping no longer needed pure ack-ack " +paam);
          remove (paam);  // done sending this pure ack-ack message
        }
      }

      Arrays.fill (messages, null);  // release references
    }
  }

  private static class DeadlineSort implements Comparator
  {
    public int compare (Object paam1, Object paam2)
    {
      if (paam1 == null)  // drive nulls to bottom (top is index 0)
      {
        if (paam2 == null) return 0;
        else return 1;
      }
      else if (paam2 == null) return -1;

      //  Sort on send deadline (sooner deadlines come first)

      PureAckAck paa1 = (PureAckAck) MessageUtils.getAck ((PureAckAckMessage) paam1);
      PureAckAck paa2 = (PureAckAck) MessageUtils.getAck ((PureAckAckMessage) paam2);

      long d1 = paa1.getSendDeadline();
      long d2 = paa2.getSendDeadline();

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

  private static String stackTraceToString (Exception e)
  {
    java.io.StringWriter stringWriter = new java.io.StringWriter();
    java.io.PrintWriter printWriter = new java.io.PrintWriter (stringWriter);
    e.printStackTrace (printWriter);
    return stringWriter.getBuffer().toString();
  }

  // discard PureAckAckMessages queued for an agent that has since restarted   //102B
  void removePureAckAckMessages(AgentID toAgent) 
  {
    if (debug()) 
      log.debug("PureAckAckSender: enter removePureAckAckMessages("+toAgent+")");
    synchronized(queue) {
      Iterator i = queue.iterator();
      while (i.hasNext()) {
        PureAckAckMessage paam = (PureAckAckMessage)i.next();
        if (MessageUtils.getToAgent(paam).equals(toAgent)) 
          if (debug()) 
            log.debug("PureAckAckSender: remove PureAckAckMessage: " +MessageUtils.toString(paam));
          i.remove();
      }
      if (queue.size() == 0) offerNewSendDeadline (0);
    }
    if (debug()) 
      log.debug("PureAckAckSender: exit removePureAckAckMessages("+toAgent+")");
  }

}
