/*
 * <copyright>
 *  Copyright 2002-2003 Object Services and Consulting, Inc. (OBJS),
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
 * 
 * 08 May 2003: Remove PureAckMessages queued for an agent that has restarted (102B)
 * 22 Sep 2002: Revamped queue adds scheduling. (OBJS)
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
 **  This is where we send pure ack messages - ack messages
 **  that are not piggybacked on regular Cougaar messages
 **  because there is not one available to piggyback on.
 **  Many of these sends are likely weeded out in the
 **  adaptive link selection because there they can be
 **  determined to be superfluous (eg. another msg with this
 **  ack in it has already been sent to the destination node
 **  within the timeframe for this ack).
**/

class PureAckSender implements Runnable
{
  private MessageAckingAspect aspect;
  private LoggingService log;
  private Vector queue;
  private PureAckMessage messages[];
  private Comparator deadlineSort;
  private long minSendDeadline;

  public PureAckSender (MessageAckingAspect aspect) 
  {
    this.aspect = aspect;
    queue = new Vector();
    messages = new PureAckMessage[32];
    deadlineSort = new DeadlineSort();
    minSendDeadline = 0;
  }

  public void add (PureAckMessage pam) 
  {
    PureAck pureAck = (PureAck) MessageUtils.getAck (pam);
    long deadline = pureAck.getSendDeadline();

    if (debug()) log.debug ("PureAckSender: adding timeout=" +(deadline-now())+ " " +pam);

    synchronized (queue) 
    {
      queue.add (pam);
      boolean buildup = (queue.size() > 32);
      offerNewSendDeadline (!buildup ? deadline : Math.min(1000+now(),deadline));
    }
  }
  
  private void remove (PureAckMessage pam) 
  {
    synchronized (queue) 
    {
      queue.remove (pam);
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
      String s = "PureAckSender: Unexpected exception, restarting thread";

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

        messages = (PureAckMessage[]) queue.toArray (messages);  // try array reuse
        len = queue.size();
        if (len > 1) Arrays.sort (messages, 0, len, deadlineSort);
      }

      //  Check if it is time to send a pure ack message

      if (len > 0 && debug()) 
      {
        log.debug ("PureAckSender: reviewing queue (" +len+ " msg" +(len==1? ")" : "s)"));
      }

      for (int i=0; i<len; i++)
      {
        //  We only consider sending a pure ack message as long as there is
        //  a need to send an ack for the src message we received that created
        //  this pure ack message.  If that is no longer the case, we can
        //  drop this pure ack message.

        PureAckMessage pam = messages[i];

        if (MessageAckingAspect.findAckToSend (pam))
        {
          PureAck pureAck = (PureAck) MessageUtils.getAck (pam);
          long sendDeadline = pureAck.getSendDeadline();
          long timeLeft = sendDeadline - now();

          if (debug()) log.debug ("PureAckSender: timeLeft=" +timeLeft+ "  " +
            MessageUtils.toShortString(pam)+ " " +MessageUtils.toShortSequenceID(pam));

          if (timeLeft <= 0)
          {
            //  Time to send/resend the ack

            if (debug()) log.debug ("PureAckSender: Sending pure ack " +pam);
            remove (pam);  // remove first to avoid race condition with send
            aspect.sendMessage (pam);
          }
          else 
          {
            offerNewSendDeadline (sendDeadline);
          }
        }
        else
        {
          if (debug()) log.debug ("PureAckSender: Dropping no longer needed pure ack " +pam);
          remove (pam);  // done sending this pure ack message
        }
      }

      Arrays.fill (messages, null);  // release references
    }
  }

  private static class DeadlineSort implements Comparator
  {
    public int compare (Object pam1, Object pam2)
    {
      if (pam1 == null)  // drive nulls to bottom (top is index 0)
      {
        if (pam2 == null) return 0;
        else return 1;
      }
      else if (pam2 == null) return -1;

      //  Sort on send deadline (sooner deadlines come first)

      PureAck pa1 = (PureAck) MessageUtils.getAck ((PureAckMessage) pam1);
      PureAck pa2 = (PureAck) MessageUtils.getAck ((PureAckMessage) pam2);

      long d1 = pa1.getSendDeadline();
      long d2 = pa2.getSendDeadline();

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

  // discard PureAckMessages queued for an agent that has since restarted   //102B
  void removePureAckMessages(AgentID toAgent) 
  {
    if (debug()) 
      log.debug("PureAckSender: enter removePureAckMessages("+toAgent+")");
    synchronized(queue) {
      Iterator i = queue.iterator();
      while (i.hasNext()) {
        PureAckMessage pam = (PureAckMessage)i.next();
        if (MessageUtils.getToAgent(pam).equals(toAgent)) 
          if (debug()) 
            log.debug("PureAckSender: remove PureAckMessage: " +MessageUtils.toString(pam));
          i.remove();
      }
      if (queue.size() == 0) offerNewSendDeadline (0);
    }
    if (debug()) 
      log.debug("PureAckSender: exit removePureAckMessages("+toAgent+")");
  }
  
}
