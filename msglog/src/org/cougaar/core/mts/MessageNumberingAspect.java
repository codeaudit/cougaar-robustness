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

import org.cougaar.core.service.TopologyReaderService;
import org.cougaar.core.service.TopologyEntry;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.mts.acking.AgentID;


/**
 **  Assign numbers to messages.  Message numbers are used in areas
 **  such as message ordering (sequencing), acking, and history.
 **/

public class MessageNumberingAspect extends StandardAspect
{
  private static final Integer ZERO = new Integer (0);
  private static final Hashtable positiveNumberTable = new Hashtable();
  private static final Hashtable negativeNumberTable = new Hashtable();

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
  //  ordering is on the message must be sent or it will halt
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
      int n;

//System.err.println ("MessageNumberingAspect: incoming msg= "+MessageUtils.toString(msg));

      //  Assign a number to the message if it doesn't already have one

      if (!MessageUtils.hasMessageNumber (msg))
      {
        if (isLocalMessage (msg))
        {
          //  NOTE: In acking and message ordering, messages without numbers are
          //  considered errors, so we give even local messages a number.

          n = 0;
          MessageUtils.setMessageTypeToLocal (msg);
        }
        else if (MessageUtils.isTrafficMaskingMessage (msg))
        {
          n = 0;
        }
        else if (MessageUtils.isSomePureAckMessage (msg))
        {
          n = getNextNegativeMessageNumber (msg);
        }
        else if (MessageUtils.isHeartbeatMessage (msg))
        {
          n = 0;
        }
        else if (MessageUtils.isPingMessage (msg))
        {
          n = getNextNegativeMessageNumber (msg);
        }
        else if (MessageUtils.isRegularMessage (msg))
        {
          n = getNextPositiveMessageNumber (msg);
        }
        else
        {
          //  Unknown message type - should not occur, except during system development

          String type = MessageUtils.getMessageType (msg);
          loggingService.error ("Unknown msg type! : " +type);

          n = -1;  // will nearly always result as a duplicate msg on the remote side
        }

        setMessageNumber (msg, n);
      }

      //  TEMP HACK - insure agent settings regardless of msg number

      AgentID fromAgent = MessageUtils.getFromAgent (msg);

      if (fromAgent == null) 
      {
        fromAgent = getAgentID (MessageUtils.getOriginatorAgent (msg));
        MessageUtils.setFromAgent (msg, fromAgent);
      }

      AgentID toAgent = MessageUtils.getToAgent (msg);

      if (toAgent == null)
      {
        toAgent = getAgentID (MessageUtils.getTargetAgent (msg));
        MessageUtils.setToAgent (msg, toAgent);
      }

//System.err.println ("MessageNumberingAspect: outgoing msg= "+MessageUtils.toString(msg));

      return link.forwardMessage (msg);
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

  private int getNextPositiveMessageNumber (AttributedMessage msg) throws NameLookupException
  {
    //  Assign the next POSITIVE message number in the sequence between the
    //  local and remote agents.

    AgentID fromAgent = MessageUtils.getFromAgent (msg);

    if (fromAgent == null) 
    {
      fromAgent = getAgentID (MessageUtils.getOriginatorAgent (msg));
      MessageUtils.setFromAgent (msg, fromAgent);
    }

    AgentID toAgent = MessageUtils.getToAgent (msg);

    if (toAgent == null)
    {
      toAgent = getAgentID (MessageUtils.getTargetAgent (msg));
      MessageUtils.setToAgent (msg, toAgent);
    }

    //  IMPORTANT: Check for local messages - they should not get here,
    //  but in case they do they get a msg number of 0.

    if (fromAgent.getNodeName().equals (toAgent.getNodeName())) 
    {
      MessageUtils.setMessageTypeToLocal (msg);
      return 0;
    }

    //  NOTE:  Positive message numbers start at 1, not 0.  This is because
    //  some messages have negative message numbers (eg. acks, pings)
    //  and there is no -0.  Also, now we are using 0 as the message 
    //  number for un-ordered and un-acked messages, as well as local
    //  messages (messages from one agent to another in the same node).
  
    synchronized (positiveNumberTable)
    {
      String key = AgentID.makeSequenceID (fromAgent, toAgent);
      Int n = (Int) positiveNumberTable.get (key);
      if (n == null) { n = new Int (1); positiveNumberTable.put (key, n); }
      return n.value++;
    }
  }

  private int getNextNegativeMessageNumber (AttributedMessage msg) throws NameLookupException
  {
    //  Assign the next NEGATIVE message number in the sequence between the
    //  local and remote agents.

    AgentID fromAgent = MessageUtils.getFromAgent (msg);

    if (fromAgent == null) 
    {
      fromAgent = getAgentID (MessageUtils.getOriginatorAgent (msg));
      MessageUtils.setFromAgent (msg, fromAgent);
    }

    AgentID toAgent = MessageUtils.getToAgent (msg);

    if (toAgent == null)
    {
      toAgent = getAgentID (MessageUtils.getTargetAgent (msg));
      MessageUtils.setToAgent (msg, toAgent);
    }

    //  IMPORTANT: Check for local messages - they should not get here,
    //  but in case they do they get a msg number of 0.

    if (fromAgent.getNodeName().equals (toAgent.getNodeName())) 
    {
      MessageUtils.setMessageTypeToLocal (msg);
      return 0;
    }

    //  NOTE:  Negative message numbers start at -1.
  
    synchronized (negativeNumberTable)
    {
      String key = AgentID.makeSequenceID (fromAgent, toAgent);
      Int n = (Int) negativeNumberTable.get (key);
      if (n == null) { n = new Int (-1); negativeNumberTable.put (key, n); }
      return n.value--;
    }
  }

  protected static void setMessageNumber (AttributedMessage msg, int n)
  {
    msg.setAttribute (MessageUtils.MSG_NUM, (n==0 ? ZERO : new Integer (n)));
  }

  private AgentID getAgentID (MessageAddress agent) throws NameLookupException
  {
    if (agent == null) return null;

	ServiceBroker sb = getServiceBroker();
    Class sc = TopologyReaderService.class;
	TopologyReaderService topologySvc = (TopologyReaderService) sb.getService (this, sc, null);

    TopologyEntry entry = topologySvc.getEntryForAgent (agent.getAddress());

    if (entry == null)
    {
      Exception e = new Exception ("MessageNumberingAspect: Topology service blank on agent! : " +agent);
      throw new NameLookupException (e);
    }

    String nodeName = entry.getNode();
    String agentName = agent.toString();
    String agentIncarnation = "" + entry.getIncarnation();

    return new AgentID (nodeName, agentName, agentIncarnation);
  }

  private static class Int  // because Java Integers are not mutable
  {
    public int value;
    public Int (int v) { value = v; }
  }
}
