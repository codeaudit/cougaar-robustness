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
 * 08 Jun 2002: Revamped and streamlined for 9.2.x. (OBJS)
 * 23 Apr 2001: Split out from MessageAckingAspect. (OBJS)
 */

package org.cougaar.core.mts.acking;

import java.util.*;

import org.cougaar.core.mts.MessageUtils;
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
  private Vector queue;
  private PureAckAckMessage messages[];
  private boolean haveNewMessages;
  private long minSendDeadline;

  public PureAckAckSender () 
  {
    queue = new Vector();
    messages = new PureAckAckMessage[32];
    haveNewMessages = false;
    minSendDeadline = Long.MAX_VALUE;
  }

  public void add (PureAckAckMessage paam) 
  {
    synchronized (queue) 
    {
      queue.add (paam);
      haveNewMessages = true;
      queue.notify();
    }
  }
  
  private void remove (PureAckAckMessage paam) 
  {
    synchronized (queue) 
    {
      queue.remove (paam);
    }
  }
  
  public void run() 
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
          //  Check for new messages before waiting.  They can come
          //  in during waiting or during queue processing.

          if (haveNewMessages)
          {
            haveNewMessages = false;
            if (queue.size() > 0) break;
          }

          //  Check how long to wait before we need to satisfy a send deadline

          long waitTime = 0;  // 0 = wait till notify (or interrupt)

          if (queue.size() > 0)
          {
            waitTime = minSendDeadline - now();
            if (waitTime <= 0) break;
          }

          //  Wait until timeout, notify, or interrupt

          // System.err.println ("\nPureAckAckSender: WAIT waitTime= "+waitTime);
          try { queue.wait (waitTime); } catch (Exception e) {}
          // System.err.println ("\nPureAckAckSender: RUN");
        }

        messages = (PureAckAckMessage[]) queue.toArray (messages);  // try array reuse
        len = queue.size();
//System.err.println ("PureAckAckSender: len="+len);
      }

      //  Check if it is time to send a pure ack-ack message

      minSendDeadline = Long.MAX_VALUE;

      for (int i=0; i<len; i++)
      {
        PureAckAckMessage paam = messages[i];
//System.err.println ("PureAckAckSender: msg="+MessageUtils.toString(paam));
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

          if (MessageAckingAspect.debug)
          {
            String m = MessageUtils.toShortString (paam);
//          System.err.println ("PureAckAckSender: "+m+": timeLeft="+timeLeft);
          }

          if (timeLeft <= 0)
          {
            //  Time to send the ack-ack

            if (MessageAckingAspect.debug) 
            {
//            System.err.println ("PureAckAckSender: Launching " +paam);
            }

            remove (paam);  // remove first to avoid race condition with send
            MessageSender.sendMsg (paam);
          }
          else 
          {
            if (sendDeadline < minSendDeadline) minSendDeadline = sendDeadline;
          }
        }
        else
        {
//System.err.println ("PureAckAckSender: removing " +paam);
          remove (paam);  // done sending this pure ack-ack message
        }
      }

      Arrays.fill (messages, null);  // release references
    }
  }

  private static long now ()
  {
    return System.currentTimeMillis();
  }
}
