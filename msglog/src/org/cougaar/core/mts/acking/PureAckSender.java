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
  private Vector queue;
  private PureAckMessage messages[];
  private boolean haveNewMessages;
  private long minSendDeadline;

  public PureAckSender () 
  {
    queue = new Vector();
    messages = new PureAckMessage[32];
    haveNewMessages = false;
    minSendDeadline = Long.MAX_VALUE;
  }

  public void add (PureAckMessage ackMsg) 
  {
    synchronized (queue) 
    {
      queue.add (ackMsg);
      haveNewMessages = true;
      queue.notify();
    }
  }
  
  private void remove (PureAckMessage ackMsg) 
  {
    synchronized (queue) 
    {
      queue.remove (ackMsg);
    }
  }
  
  public void run() 
  {
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

          //System.err.println ("\nPureAckSender: WAIT waitTime= "+waitTime);
          CougaarThread.wait (queue, waitTime);
          //System.err.println ("\nPureAckSender: RUN");
        }

        messages = (PureAckMessage[]) queue.toArray (messages);  // try array reuse
      }

      //  See if it's time to send an ack.  Purge any acks that are no
      //  longer needed to be sent.

      minSendDeadline = Long.MAX_VALUE;

      for (int i=0; i<messages.length; i++)
      {
        PureAckMessage ackMsg = messages[i];
        PureAck pureAck = (PureAck) MessageUtils.getAck (ackMsg);

        if (MessageAckingAspect.findAckToSend (pureAck))
        {
          long sendDeadline = pureAck.getSendDeadline();
          long timeLeft = sendDeadline - now();

          if (MessageAckingAspect.debug)
          {
            System.err.println ("PureAckSender: "+ackMsg+": timeLeft="+timeLeft);
          }

          if (timeLeft <= 0)
          {
            //  Time to send/resend the ack

            remove (ackMsg);  // remove first to avoid race condition with send
            MessageSender.sendMsg (ackMsg);
          }
          else 
          {
            if (sendDeadline < minSendDeadline) minSendDeadline = sendDeadline;
          }
        }
        else remove (ackMsg);  // done sending this ack
      }

      Arrays.fill (messages, null);  // release references
    }
  }

  private static long now ()
  {
    return System.currentTimeMillis();
  }
}
