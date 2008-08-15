/*
 * <copyright>
 *  Copyright 2002-2004 Object Services and Consulting, Inc. (OBJS),
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
 * 08 Jul 2003: Switched to getReverseDelegate for compatibility with 
 *              Security's MessageProtectionAspectImpl. (B10_4_1)
 * 10 May 2002: Created. (OBJS)
 */

package org.cougaar.mts.std;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Vector;
import org.cougaar.core.mts.AgentState;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.SimpleMessageAttributes;
import org.cougaar.core.service.LoggingService;
import org.cougaar.mts.base.AttributedMessage;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.StandardAspect;
import org.cougaar.mts.base.ReceiveLink;
import org.cougaar.mts.base.ReceiveLinkDelegateImplBase;

/**
 **  Enforce message ordering on incoming messages based on the message 
 **  number contained in the message number attribute. (Must be placed
 **  in aspect list before MessageProtectionAspectImpl.)
 **/

public class MessageOrderingAspect extends StandardAspect
{
  private static final String AGENT_INCOMING_SEQ_TABLE = "AgentIncomingMsgNumSequenceTable";
  private static final String QUEUE_ADD_TIME = "MessageOrderingQueueAddTime";
  private static final String QUEUE_REPORT_COUNT = "MessageOrderingQueueReportCount";

  private static final int waitingMsgReportInterval;

  private LoggingService log;

  static
  {
    //  Read external properties

    String s = "org.cougaar.message.transport.aspects.ordering.waitingMsgReportIntervalMinutes";
    waitingMsgReportInterval = Integer.valueOf(System.getProperty(s,"5")).intValue();
  }

  public MessageOrderingAspect () 
  {}

  public void load ()
  {
    super.load();
    log = loggingService;
  }
  
  public Object getReverseDelegate (Object delegate, Class type)   //B10_4_1 
  {
    if (type == ReceiveLink.class) return (new Link ((ReceiveLink) delegate));
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
        log.error (s);
        throw new RuntimeException (s);
      }

      MessageAttributes attrs = new SimpleMessageAttributes();
      String status;

      //  Handle the different categories of message sequence numbers

      int num = MessageUtils.getMessageNumber (msg);

      if (num == 0)
      {
        //  Unordered messages (like heartbeats) have a message number of 0.
        //  Local messages used to have a message number of 0, but now are
        //  numbered to avoid a race condition due to agent mobility.

        doDelivery (msg);
        status = MessageAttributes.DELIVERY_STATUS_DELIVERED;
      }
      else if (num > 0)
      {
        //  Regular ordered messages

        int nextNum = getNextMessageNumber (msg);

        if (num < nextNum)
        {
          //  Duplicate message

          if (log.isDebugEnabled()) log.debug ("Duplicate (dropped): " +MessageUtils.toString(msg));
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
                if (log.isDebugEnabled()) 
                  log.debug ("Take off waiting queue: " +MessageUtils.toString(msgs[i]));

                doDelivery (msgs[i]);
                setNextMessageNumber (msgs[i], nextNum+1);
                queue.remove (msgs[i]);
              }
              else if (log.isWarnEnabled()) 
              {
                //  Report any messages that have been waiting for a long time
                
                Long addTime = (Long) msgs[i].getAttribute (QUEUE_ADD_TIME);  

                if (addTime == null)
                {
                  log.warn ("Unexpected null Q addTime! " +MessageUtils.toString(msgs[i]));
                  addTime = new Long (now());
                  msgs[i].setLocalAttribute (QUEUE_ADD_TIME, addTime); 
                }

                int waitTime = (int)(now() - addTime.longValue());

                if (waitTime > waitingMsgReportInterval*60*1000)
                {
                  Int count = (Int) msgs[i].getAttribute (QUEUE_REPORT_COUNT);

                  if (count == null) 
                  { 
                    count = new Int (0);
                    msgs[i].setLocalAttribute (QUEUE_REPORT_COUNT, count);
                  } 

                  if (waitTime >= (count.value+1)*waitingMsgReportInterval*60*1000)
                  {
                    log.warn ("Msg has been on waiting queue for " +(waitTime/(60*1000))+
                              " minutes: " +MessageUtils.toString(msgs[i]));
                    count.value++;
                  }
                }
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
          msg.setLocalAttribute (QUEUE_ADD_TIME, new Long (now()));
          status = MessageAttributes.DELIVERY_STATUS_HELD;
          if (log.isDebugEnabled()) log.debug ("  Put on waiting queue: " +MessageUtils.toString(msg));
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

    MessageAddress agentAddr = MessageUtils.getTargetAgent (msg);
    Hashtable agentTable = getMessageNumberTableForAgent (agentAddr);
    if (agentTable == null) throw new RuntimeException (new MisdeliveredMessageException (msg));

    synchronized (agentTable)
    {
      AgentID fromAgent = MessageUtils.getFromAgent (msg);
      String key = fromAgent.getNumberSequenceKey();
      Int n = (Int) agentTable.get (key);
      if (n != null) {
	  //if (log.isDebugEnabled()) log.debug("getNextMessageNumber("+MessageUtils.toString(msg)+") = "+n.value);
	  return n.value;
      } else {
	  //  Positive message numbers start at 1
	  setNextMessageNumber(msg, 1);
          //if (log.isDebugEnabled()) log.debug("getNextMessageNumber("+MessageUtils.toString(msg)+") = 1");
	  return 1;
      }
    }
  }

  private void setNextMessageNumber (AttributedMessage msg, int msgNum)
  {
    MessageAddress agentAddr = MessageUtils.getTargetAgent (msg);
    Hashtable agentTable = getMessageNumberTableForAgent (agentAddr);
    if (agentTable == null) throw new RuntimeException (new MisdeliveredMessageException (msg));

    synchronized (agentTable)
    {
      AgentID fromAgent = MessageUtils.getFromAgent (msg);
      String key = fromAgent.getNumberSequenceKey();
      Int n = (Int) agentTable.get (key);
      //if (log.isDebugEnabled()) log.debug("setNextMessageNumber("+MessageUtils.toString(msg)+","+msgNum+")");
      if (n == null) agentTable.put (key, new Int (msgNum));
      else n.value = msgNum;
    }
  }

  private Hashtable getMessageNumberTableForAgent (MessageAddress agentAddr)
  {
    //  Get agent state for the agent (if we can)

    AgentState agentState = getRegistry().getAgentState (agentAddr);
    if (agentState == null) return null;

    //  Get the number table from the agent state

    synchronized (agentState)
    {
      Hashtable agentTable = (Hashtable) agentState.getAttribute (AGENT_INCOMING_SEQ_TABLE);

      if (agentTable == null)
      {
        //  If not in state, we create the table

        agentTable = new Hashtable();
        agentState.setAttribute (AGENT_INCOMING_SEQ_TABLE, agentTable);
      }        

      return agentTable;
    }
  }

  private static class Int implements java.io.Serializable  // for mutable int objects
  {
    public int value;
    public Int (int v) { value = v; }
  }

  private static long now ()
  {
    return System.currentTimeMillis();
  }
}
