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
 * 23 Apr 2002: Split out from MessageAckingAspect. (OBJS)
 */

package org.cougaar.core.mts.acking;

import java.util.*;

import org.cougaar.core.mts.*;
import org.cougaar.core.thread.CougaarThread;


/**
 **  This is where sent messages wait for their acks, and get resent
 **  if none show up in time.
 **/

class AckWaiter implements Runnable
{
  private Vector queue;
  private AttributedMessage messages[];
  private boolean haveNewData;
  private long minResendDeadline;

  public AckWaiter () 
  {
    queue = new Vector();
    messages = new AttributedMessage[32];
    haveNewData = false;
    minResendDeadline = Long.MAX_VALUE;
  }

  public void add (AttributedMessage msg) 
  {
    //  Sanity checks

    if (!MessageUtils.isAckableMessage (msg)) return;
    if (!MessageAckingAspect.hasRegularAck (msg)) return;

    //  Add the message

    synchronized (queue) 
    {
      queue.add (msg);
      ding();          // set minResendDeadline if nothing else
    }
  }

  public void ding ()
  {
    //  We get dinged when new messages are added to the ack waiter and when
    //  new acks are received by the ack backend.

    synchronized (queue) 
    {
      haveNewData = true;  // new msgs or acks
      queue.notify();
    }
  }

  private void remove (AttributedMessage msg) 
  {
    synchronized (queue) 
    {
      queue.remove (msg);
    }
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

          // System.err.println ("\nAckWaiter: WAIT waitTime= " +waitTime+ " start= "+now());
          try { queue.wait (waitTime); } catch (Exception e) {}
          // System.err.println ("\nAckWaiter: WAIT end= "+now());
        }

        messages = (AttributedMessage[]) queue.toArray (messages);  // try array reuse
        len = queue.size();
      }

      //  Next we try to match the messages with the acks we've collected so far.
      //  Possible many-to-many relationship between the messages and the acks.

      for (int i=0; i<len; i++)
      {
        AttributedMessage msg = messages[i];
        
        //  Avoid already successful sends (when a message is acked it is
        //  declared (and recorded as) a successful send).
/*
System.err.println ("ackwaiter: msg"+i+" = "+MessageUtils.toString(msg));
System.err.println ("ackwaiter: msg"+i+" contains acks:");
AckList.printAcks (MessageUtils.getAck(msg).getAcks());
*/
        if (MessageAckingAspect.wasSuccessfulSend (msg))
        {
          if (MessageAckingAspect.debug) 
          {
            System.err.println ("\nAckWaiter: Dropping already successful " +MessageUtils.toString(msg));
          }

          remove (msg);
          messages[i] = null;
          continue;
        }

        //  Search for match of message with current received ack data

//System.err.println ("ackwaiter: recv'd acks: ");
//AckList.printAcks(MessageAckingAspect.getReceivedAcks(msg));

        int msgNum = MessageUtils.getMessageNumber (msg);
        AckList ackList = AckList.findFirst (MessageAckingAspect.getReceivedAcks(msg), msgNum);

        if (ackList != null)
        {
          //  We have an ack!!
        
          if (MessageAckingAspect.debug) 
          {
            System.err.println ("\nAckWaiter: Got ack for " +MessageUtils.toString(msg));
          }

          Ack ack = MessageUtils.getAck (msg);
          String toNode = MessageUtils.getToAgentNode (msg);

          MessageAckingAspect.addSuccessfulSend (msg);                
          MessageAckingAspect.updateRoundtripTimeMeasurement (ack, ackList);
          MessageAckingAspect.updateMessageHistory (ack, ackList);
          MessageAckingAspect.removeAcksToSend (toNode, ack.getSpecificAcks()); // acks now acked

          remove (msg); // remove from waiting queue
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

        long resendDeadline = ack.getSendTime() + ack.getResendAckWindow();
        long timeLeft = resendDeadline - now();

        if (MessageAckingAspect.debug)
        {
          // System.err.println ("AckWaiter: msg " +MessageUtils.getMessageNumber(msg)+
          //  ": ackWindow="+ack.getResendAckWindow()+" timeLeft="+timeLeft);
        }

        if (timeLeft <= 0)
        {
          //  Time to resend the message.  The MessageSender re-inserts the message 
          //  the front of the whole send pipeline: it'll come through the AckFrontend
          //  again on its way out to a outgoing link.  The link selection policy has 
          //  been modified to try a different link with each message resend, as it
          //  wants to try every possbile transport link in its efforts to get a 
          //  message through.  How this works may change when the message history 
          //  is better integrated with message acking. 

          if (MessageAckingAspect.debug) 
          {
            // System.err.println ("AckWaiter: Resending " +MessageUtils.toString(msg));
          }

          remove (msg);  // remove first to avoid race condition with send
          MessageSender.sendMsg (msg);
        }
        else
        {
          if (resendDeadline < minResendDeadline) minResendDeadline = resendDeadline;
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
