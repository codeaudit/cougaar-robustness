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
 * 09 May 2002: Created. (OBJS)
 */

package org.cougaar.core.mts;

import java.util.Hashtable;


/**
 **  Assign numbers to messages.  Message numbers are used in areas
 **  such as message ordering (sequencing), acking, and history.
 **
 **  Number Range   Message Properties
 **  
 **  1,2,3 ...      acked, ordered, not loseable (exception: local msgs)
 **  0              not acked, not ordered, loseable
 **  -1,-2,-3 ...   ackable, not ordered, loseable
 **/

public class MessageNumberingAspect extends StandardAspect
{
  private static final String AGENT_OUTGOING_SEQ_TABLE = "AgentOutgoingMsgNumSequenceTable";
  private static final Integer ZERO = new Integer (0);
  private static final int POSITIVE = 1;
  private static final int NEGATIVE = -1;

  public MessageNumberingAspect () 
  {}

  public Object getDelegate (Object delegate, Class type) 
  {
    if (type == DestinationLink.class) 
    {
      return (new Link ((DestinationLink) delegate));
    }
 
    return null;
  }

  //  Note that once a message number > 0 is assigned, if message
  //  ordering is on, the message must be sent or it will halt
  //  all further communication with the remote agent until the
  //  missing message is received.  So assigning a positive message 
  //  number is a strong committment.

  private class Link extends DestinationLinkDelegateImplBase 
  {
    private DestinationLink link;
    
    private Link (DestinationLink link) 
    {
      super (link); 
      this.link = link;
    }

    public MessageAttributes forwardMessage (AttributedMessage msg) 
      throws UnregisteredNameException, NameLookupException, 
  	         CommFailureException, MisdeliveredMessageException
    {
      //  Skip numbering if the message already has a number

      if (MessageUtils.hasMessageNumber (msg)) return link.forwardMessage (msg);

      //  Set from and to agents as needed

      AgentID fromAgent = MessageUtils.getFromAgent (msg);

      if (fromAgent == null) 
      {
        MessageAddress agentAddr = MessageUtils.getOriginatorAgent (msg);
        fromAgent = AgentID.getAgentID (this, getServiceBroker(), agentAddr);
        MessageUtils.setFromAgent (msg, fromAgent);
      }

      if (!isLocalAgent (MessageUtils.getOriginatorAgent(msg)))
      {
        String s = "Sending agent " +fromAgent+ " no longer on local node!";
        loggingService.error (s);
        throw new CommFailureException (new Exception (s));
      }

      AgentID toAgent = MessageUtils.getToAgent (msg);

      if (toAgent == null)
      {
        MessageAddress agentAddr = MessageUtils.getTargetAgent (msg);
        toAgent = AgentID.getAgentID (this, getServiceBroker(), agentAddr);
        MessageUtils.setToAgent (msg, toAgent);
      }

      //  Set the message number based on the type of message

      int n;
            
      if (isLocalMessage (msg))  // every msg needs a number, even local ones
      {
        //  Local messages are numbered with positive numbers in order to avoid
        //  a race condition where messages from the agent state of a newly 
        //  arrived message that have now become local (because the agent they
        //  have been talking to is on this node) are in competition with the
        //  new agent itself (2 sources of msgs).

        n = getNextMessageNumber (POSITIVE, msg);  
        MessageUtils.setMessageTypeToLocal (msg);
      }
      else if (MessageUtils.isTrafficMaskingMessage (msg))
      {
        n = 0;
      }
      else if (MessageUtils.isSomePureAckMessage (msg))
      {
        n = getNextMessageNumber (NEGATIVE, msg);
      }
      else if (MessageUtils.isHeartbeatMessage (msg))
      {
        n = 0;
      }
      else if (MessageUtils.isPingMessage (msg))
      {
        n = getNextMessageNumber (NEGATIVE, msg);
      }
      else if (MessageUtils.isRegularMessage (msg))
      {
        n = getNextMessageNumber (POSITIVE, msg);
      }
      else
      {
        //  Unknown message type - should not occur, except during system development

        loggingService.fatal ("Unknown msg type! : " +MessageUtils.getMessageType(msg));
        n = -1;  // will nearly always result in a duplicate msg on the remote side
      }

      setMessageNumber (msg, n);
      return link.forwardMessage (msg);
    }

    private boolean isLocalAgent (MessageAddress agent)
    {
      return getRegistry().isLocalClient (agent);
    }

    private boolean isLocalMessage (AttributedMessage msg)
    {
      return isLocalAgent (MessageUtils.getTargetAgent(msg));
    }

    public String toString ()
    {
      return link.toString();
    }
  }

  protected static void setMessageNumber (AttributedMessage msg, int n)
  {
    msg.setAttribute (MessageUtils.MSG_NUM, (n==0 ? ZERO : new Integer (n)));
  }

  private int getNextMessageNumber (int type, AttributedMessage msg) throws CommFailureException
  {
    //  NOTE:  Positive message numbers start at 1, not 0.  This is because
    //  some messages have negative message numbers (eg. acks, pings)
    //  and there is no -0.  Also, now we are using 0 as the message 
    //  number for un-ordered and un-acked messages, as well as local
    //  messages.  Negative message numbers start at -1.

    MessageAddress agentAddr = MessageUtils.getOriginatorAgent (msg);
    Hashtable agentTable = getMessageNumberTableForAgent (agentAddr);
  
    synchronized (agentTable)
    {
      AgentID toAgent = MessageUtils.getToAgent (msg);
      String key = toAgent.getNumberSequenceKey();
      SequenceNumbers n = (SequenceNumbers) agentTable.get (key);
      if (n == null) { n = new SequenceNumbers (1, -1); agentTable.put (key, n); }
      return (type==POSITIVE ? n.nextPositiveNumber++ : n.nextNegativeNumber--);
    }
  }

  private Hashtable getMessageNumberTableForAgent (MessageAddress agentAddr)
    throws CommFailureException
  {
    //  Get agent state for the agent

    AgentState agentState = getRegistry().getAgentState (agentAddr);
    
    if (agentState == null)
    {
      String s = "AgentState missing for agent " +agentAddr;
      throw new CommFailureException (new Exception (s));
    }

    //  Get the number table from the agent state

    synchronized (agentState)
    {
      Hashtable agentTable = (Hashtable) agentState.getAttribute (AGENT_OUTGOING_SEQ_TABLE);

      if (agentTable == null)
      {
        //  If not in state, we create the table

        agentTable = new Hashtable();
        agentState.setAttribute (AGENT_OUTGOING_SEQ_TABLE, agentTable);
      }        

      return agentTable;
    }
  }

  private static class SequenceNumbers implements java.io.Serializable
  {
    public int nextPositiveNumber;
    public int nextNegativeNumber;

    public SequenceNumbers (int pos, int neg) 
    {
      nextPositiveNumber = pos;
      nextNegativeNumber = neg;
    }
  }
}
