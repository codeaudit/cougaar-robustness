/*
 * <copyright>
 *  Copyright 2002 Object Services and Consulting, Inc. (OBJS),
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
 * 10 May 2002: Created. (OBJS)
 */

package org.cougaar.core.mts;

import java.util.*;

import org.cougaar.core.mts.acking.AgentID;


/**
 **  Enforce message ordering on incoming messages based on message 
 **  number contained in message number attribute.
 **/

public class MessageOrderingAspect extends StandardAspect
{
  private static final Hashtable sequenceTable = new Hashtable();

  private static boolean debug = false;

  public MessageOrderingAspect () 
  {}

  public Object getDelegate (Object delegate, Class type) 
  {
    if (type == ReceiveLink.class) 
    {
      return (new Link ((ReceiveLink) delegate));
    }
 
    return null;
  }

  private class Link extends ReceiveLinkDelegateImplBase
  {
    private ReceiveLink link;
    private Vector queue;
    private Comparator queueSort;

    private Link (ReceiveLink link) 
    {
      super (link); 
      this.link = link;
      queue = new Vector (4);
      queueSort = new QueueSort();
    }

    public MessageAttributes deliverMessage (AttributedMessage msg) 
    {
      //  Sanity checks

// put in check that message numbering aspect is in?

      if (MessageUtils.hasMessageNumber (msg) == false)
      {
        throw new RuntimeException ("Msg missing number!");
      }

      MessageAttributes attrs = new SimpleMessageAttributes();
      String status;

      //  Handle the different categories of message sequence numbers

      int num = MessageUtils.getMessageNumber (msg);

      if (num == 0)
      {
        //  Unordered messages (like heartbeats) have a message number of 0.
        //  Local messages, if numbered, are also given a message number of 0.

        doDelivery (msg);
        status = MessageAttributes.DELIVERY_STATUS_DELIVERED;
      }
      else if (num > 0)
      {
        //  Regular ordered messages

        int nextNum = getNextMsgNumInSequence (msg);

        if (num < nextNum)
        {
          //  Duplicate message!
        
          status = MessageAttributes.DELIVERY_STATUS_DROPPED_DUPLICATE;
        }
        else if (num == nextNum)
        {
          //  We have a match!  

          doDelivery (msg);
          setNextMsgNumInSequence (msg, nextNum+1);

          //  Now see how many (if any) waiting messages can be delivered

          int len = queue.size();

          if (len > 0)
          {
            AttributedMessage msgs[] = new AttributedMessage[len];
            msgs = (AttributedMessage[]) queue.toArray (msgs);

            if (len > 1) Arrays.sort (msgs, 0, len, queueSort);

            for (int i=0; i<len; i++)
            {
              num = MessageUtils.getMessageNumber (msgs[i]);
              nextNum = getNextMsgNumInSequence (msgs[i]);

              if (num == nextNum) 
              {
                doDelivery (msgs[i]);
                setNextMsgNumInSequence (msgs[i], nextNum+1);
                queue.remove (msgs[i]);
              }
              else break;
            }
          }  

          status = MessageAttributes.DELIVERY_STATUS_DELIVERED;
        }
        else
        {
          //  Out of sequence message - put on waiting queue

          queue.add (msg);
          status = MessageAttributes.DELIVERY_STATUS_HELD;
        }
      }
      else // num < 0  
      {
        //  Negative numbers are reserved for unordered but ackable messages
        //  such as acks and pings.  Acks are never delivered to agents but
        //  pings are.

        if (debug)
        {
          System.err.println ("MessageOrdering: Delivering msg num " +num);
        }

        doDelivery (msg);
        status = MessageAttributes.DELIVERY_STATUS_DELIVERED;
      }

      attrs.setAttribute (MessageAttributes.DELIVERY_ATTRIBUTE, status);
      return attrs;
    }

    private void doDelivery (AttributedMessage msg)
    {
      if (debug)
      {
        System.err.println ("MessageOrdering: Delivering msg num " +MessageUtils.getMessageNumber(msg));
      }

      super.deliverMessage (msg);
    }
  }

  private static class QueueSort implements Comparator
  {
    public int compare (Object m1, Object m2)
    {
      if (m1 == null)  // drive nulls to bottom (top is index 0)
      {
        if (m2 == null) return 0;
        else return 1;
      }
      else if (m2 == null) return -1;

      //  Sort on message number

      int n1 = MessageUtils.getMessageNumber ((AttributedMessage) m1);
      int n2 = MessageUtils.getMessageNumber ((AttributedMessage) m2);

      if (n1 == n2) return 0;
      return (n1 > n2 ? 1 : -1);
    }

    public boolean equals (Object obj)
    {
      return (this == obj);
    }
  }

  //  We maintain a global table of sequence numbers as there are
  //  other clients of this information within MTS besides us here.
  //  WAIT - maybe not any longer - wait a bit and see how it turns
  //  out - make private for now.

  private static int getNextMsgNumInSequence (AttributedMessage msg)
  {
    return getNextMsgNumInSequence (MessageUtils.getFromAgent(msg), MessageUtils.getToAgent(msg));
  }

  private static int getNextMsgNumInSequence (AgentID fromAgent, AgentID toAgent)
  {
    synchronized (sequenceTable)
    {
      String key = AgentID.makeSequenceID (fromAgent, toAgent);
      Int num = (Int) sequenceTable.get (key);
      if (num != null) return num.value;

      //  Positive message numbers start at 1

      setNextMsgNumInSequence (fromAgent, toAgent, 1);
      return 1;
    }
  }

  private static void setNextMsgNumInSequence (AttributedMessage msg, int msgNum)
  {
    setNextMsgNumInSequence (MessageUtils.getFromAgent(msg), MessageUtils.getToAgent(msg), msgNum);
  }

  private static void setNextMsgNumInSequence (AgentID fromAgent, AgentID toAgent, int msgNum)
  {
    synchronized (sequenceTable)
    {
      String key = AgentID.makeSequenceID (fromAgent, toAgent);
      Int num = (Int) sequenceTable.get (key);
      if (num == null) sequenceTable.put (key, new Int (msgNum));
      else num.value = msgNum;
    }
  }

  private static class Int  // because Java Integers are not mutable
  {
    public int value;
    public Int (int v) { value = v; }
  }
}
