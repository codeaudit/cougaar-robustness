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
 * 09 Jun 2002: Revamped for 9.2.x. (OBJS)
 * 23 Apr 2001: Split out from MessageAckingAspect. (OBJS)
 */

package org.cougaar.core.mts.acking;

import java.util.*;

import org.cougaar.core.mts.MessageUtils;
import org.cougaar.core.thread.CougaarThread;


class PureAckAckSender implements Runnable
{
  private Vector queue;
  private PureAckAckMessage messages[];
  private boolean haveNewMessages;
  private long maxWait;

  public PureAckAckSender () 
  {
    queue = new Vector();
    messages = new PureAckAckMessage[16];
    haveNewMessages = false;
    maxWait = 0;
  }

  public void add (PureAckAckMessage ackMsg) 
  {
    synchronized (queue) 
    {
      queue.add (ackMsg);
      haveNewMessages = true;
      queue.notify();
    }
  }
  
  private void remove (PureAckAckMessage ackMsg) 
  {
    synchronized (queue) 
    {
      queue.remove (ackMsg);
    }
  }
  
  public void run() 
  {
    int len;

    while (true) 
    {
      synchronized (queue) 
      {
        //  Wait until we have some new messages or we have timed out to 
        //  re-examine old messages.

        long waitTime, elapsedTime, waitStart;

        long minMaxWait = 200;   // HACK - should calc this
        maxWait = Math.max (minMaxWait, maxWait); 

        waitStart = now();

        while (true)
        {
          //  Check for new messages before waiting.  They can come
          //  in during waiting or during queue processing.

          if (haveNewMessages)
          {
            haveNewMessages = false;
            if (queue.size() > 0) break;
          }

          if (queue.size() > 0)
          {
            //  If we have waited long enough, break out; otherwise
            //  recalculate how much longer to wait.

            elapsedTime = now() - waitStart;
            if (elapsedTime >= maxWait) break;
            else waitTime = maxWait - elapsedTime;
          }
          else waitTime = 0;  // 0 = wait till notify (or interrupt)

          //  Wait until timeout, notify, or interrupt

          //System.err.println ("\nPureAckAckSender: WAIT waitTime= "+waitTime);
          CougaarThread.wait (queue, waitTime);
          //System.err.println ("\nPureAckAckSender: RUN");
        }

        messages = (PureAckAckMessage[]) queue.toArray (messages);  // try array reuse
        len = queue.size();
      }

      //  Send one ack-ack when and if needed

      int minTimeLeft = Integer.MAX_VALUE;

      for (int i=0; i<len; i++)
      {
        PureAckAckMessage ackMsg = messages[i];
        PureAckAck pureAckAck = (PureAckAck) MessageUtils.getAck (ackMsg);

        String node = MessageUtils.getToAgentNode (ackMsg);
        long lastSendTime = MessageAckingAspect.getLastSendTime (node);

        if (lastSendTime < pureAckAck.getReceiveTime())
        {
          int timeLeft = (int)(pureAckAck.getSendDeadline() - now());

          if (MessageAckingAspect.debug)
          {
            System.err.println ("PureAckAckSender: "+pureAckAck+": timeLeft="+timeLeft);
          }

          if (timeLeft <= 0)
          {
            //  Time to send the ackAck

            remove (ackMsg);  // remove first to avoid race condition with send
            MessageSender.sendMsg (ackMsg);
          }
          else 
          {
            if (timeLeft < minTimeLeft) minTimeLeft = timeLeft;
          }
        }
        else remove (ackMsg);  // no need to send this AckAck message
      }

      Arrays.fill (messages, null);  // remove references

      //  Figure a reasonable time to wait until we re-examine any remaining messages

      maxWait = (minTimeLeft != Integer.MAX_VALUE ? minTimeLeft : 0);
    }
  }

  private static long now ()
  {
    return System.currentTimeMillis();
  }
}
