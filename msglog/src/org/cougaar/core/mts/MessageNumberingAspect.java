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
 **  1,2,3 ...      acked, ordered, not loseable
 **  0              not acked, not ordered, loseable
 **  -1,-2,-3 ...   ackable, not ordered, loseable
 **/

public class MessageNumberingAspect extends StandardAspect
{
  private static final Hashtable numberSequenceTables = new Hashtable();
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

      if (!isLocalAgent (fromAgent))  // ensure the from agent is still local
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
        n = 0;  
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

        String type = MessageUtils.getMessageType (msg);
        loggingService.error ("Unknown msg type! : " +type);

        n = -1;  // will nearly always result as a duplicate msg on the remote side
      }

      setMessageNumber (msg, n);

//System.err.println ("MessageNumberingAspect: outgoing msg= "+MessageUtils.toString(msg));

      return link.forwardMessage (msg);
    }

    private boolean isLocalAgent (AgentID agent)
    {
      return getRegistry().getIdentifier().equals(agent.getNodeName());
    }

    private boolean isLocalMessage (AttributedMessage msg)
    {
      return getRegistry().isLocalClient (MessageUtils.getTargetAgent (msg));
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

    AgentID fromAgent = MessageUtils.getFromAgent (msg);
    MessageAddress agentAddr = MessageUtils.getOriginatorAgent (msg);
    Hashtable agentTable = getAgentNumberSequenceTable (fromAgent, agentAddr);
  
    synchronized (agentTable)
    {
      AgentID toAgent = MessageUtils.getToAgent (msg);
      String key = toAgent.getNumberSequenceKey();
      SequenceNumbers n = (SequenceNumbers) agentTable.get (key);
      if (n == null) { n = new SequenceNumbers (1, -1); agentTable.put (key, n); }
      return (type==POSITIVE ? n.nextPositiveNumber++ : n.nextNegativeNumber--);
    }
  }

  private Hashtable getAgentNumberSequenceTable (AgentID agent, MessageAddress agentMsgAddr)
    throws CommFailureException
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
          throw new CommFailureException (new Exception (s));
        }

        synchronized (agentState)
        {
          agentTable = (Hashtable) agentState.getAttribute (AGENT_OUTGOING_SEQ_TABLE);

          if (agentTable == null)
          {
            agentTable = new Hashtable();
            agentState.setAttribute (AGENT_OUTGOING_SEQ_TABLE, agentTable);
System.err.println ("creating new outgoing msg num seq table for local agent " +agentMsgAddr);
          }        
else System.err.println ("using stored outgoing msg num seq table from AgentState for new local agent " +agentMsgAddr);
        }

        numberSequenceTables.put (key, agentTable);
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
