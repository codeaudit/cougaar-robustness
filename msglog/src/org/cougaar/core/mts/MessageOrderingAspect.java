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


/**
 **  Enforce message ordering on incoming messages based on the message 
 **  number contained in the message number attribute.
 **/

public class MessageOrderingAspect extends StandardAspect
{
  private static final Hashtable numberSequenceTables = new Hashtable();
  private static final String AGENT_INCOMING_SEQ_TABLE = "AgentIncomingMsgNumSequenceTable";

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
      //  Sanity check

      if (MessageUtils.hasMessageNumber (msg) == false)
      {
        String s = "Message has no number! : " +MessageUtils.toString(msg);
        loggingService.error (s);
        throw new RuntimeException (s);
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

        int nextNum = getNextMessageNumber (msg);

        if (num < nextNum)
        {
          //  Duplicate message!
        
          status = MessageAttributes.DELIVERY_STATUS_DROPPED_DUPLICATE;
        }
        else if (num == nextNum)
        {
          //  We have a match!  

          doDelivery (msg);
          setNextMessageNumber (msg, nextNum+1);

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
              nextNum = getNextMessageNumber (msgs[i]);

              if (num == nextNum) 
              {
                doDelivery (msgs[i]);
                setNextMessageNumber (msgs[i], nextNum+1);
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
        //  pings are (acks are filtered out in the acking aspect).

        doDelivery (msg);
        status = MessageAttributes.DELIVERY_STATUS_DELIVERED;
      }

      attrs.setAttribute (MessageAttributes.DELIVERY_ATTRIBUTE, status);
      return attrs;
    }

    private void doDelivery (AttributedMessage msg)
    {
      if (loggingService.isDebugEnabled()) 
        loggingService.debug ("Delivering msg " +MessageUtils.toString(msg));

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

      //  Sort on message number (lower numbers come first)

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

  private int getNextMessageNumber (AttributedMessage msg) 
  {
    //  NOTE:  Positive message numbers start at 1, not 0.  This is because
    //  some messages have negative message numbers (eg. acks, pings)
    //  and there is no -0.  Messages with numbers 0 or less are unordered,
    //  so we don't maintain anything on them here.

    AgentID toAgent = MessageUtils.getToAgent (msg);
    MessageAddress agentAddr = MessageUtils.getTargetAgent (msg);
    Hashtable agentTable = getAgentNumberSequenceTable (toAgent, agentAddr);

    if (agentTable == null) 
    {
      //  HACK!  What to do here?

      throw new RuntimeException (new MisdeliveredMessageException (msg));
    }

    synchronized (agentTable)
    {
      AgentID fromAgent = MessageUtils.getFromAgent (msg);
      String key = fromAgent.getNumberSequenceKey();
      Int n = (Int) agentTable.get (key);
      if (n != null) return n.value;

      //  Positive message numbers start at 1

      setNextMessageNumber (msg, 1);
      return 1;
    }
  }

  private void setNextMessageNumber (AttributedMessage msg, int msgNum)
  {
    AgentID toAgent = MessageUtils.getToAgent (msg);
    MessageAddress agentAddr = MessageUtils.getTargetAgent (msg);
    Hashtable agentTable = getAgentNumberSequenceTable (toAgent, agentAddr);

    if (agentTable == null) 
    {
      //  HACK!  What to do here?

      throw new RuntimeException (new MisdeliveredMessageException (msg));
    }
  
    synchronized (agentTable)
    {
      AgentID fromAgent = MessageUtils.getFromAgent (msg);
      String key = fromAgent.getNumberSequenceKey();
      Int n = (Int) agentTable.get (key);
      if (n == null) agentTable.put (key, new Int (msgNum));
      else n.value = msgNum;
    }
  }

  private Hashtable getAgentNumberSequenceTable (AgentID agent, MessageAddress agentMsgAddr)
  {
    Hashtable agentTable;

    synchronized (numberSequenceTables)
    {
      String key = agent.getNumberSequenceKey();
      agentTable = (Hashtable) numberSequenceTables.get (key);

      if (agentTable == null)
      {
        //  Try to get the table from the agent state

        AgentState agentState = getRegistry().getAgentState (agentMsgAddr);
        
        if (agentState == null)
        {
          String s = "AgentState missing for agent " +agentMsgAddr;
          loggingService.error (s);
          return null;
        }

        synchronized (agentState)
        {
          agentTable = (Hashtable) agentState.getAttribute (AGENT_INCOMING_SEQ_TABLE);

          if (agentTable == null)
          {
            agentTable = new Hashtable();
            agentState.setAttribute (AGENT_INCOMING_SEQ_TABLE, agentTable);
System.err.println ("creating new incoming msg num seq table for local agent " +agentMsgAddr);
          }        
else System.err.println ("using stored incoming msg num seq table from AgentState for new local agent " +agentMsgAddr);
        }

        numberSequenceTables.put (key, agentTable);
      }

      return agentTable;
    }
  }

  private static class Int implements java.io.Serializable  // for mutable int objects
  {
    public int value;
    public Int (int v) { value = v; }
  }
}
