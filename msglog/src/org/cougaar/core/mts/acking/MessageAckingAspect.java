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
 * 20 Aug 2002: Support for agent mobility. (OBJS)
 * 06 Jun 2002: Completely revamped for Cougaar 9.2.x (OBJS)
 * 08 Jan 2002: Egregious temporary hack to handle last minute traffic masking messages. (OBJS)
 * 07 Jan 2002: Implemented new acking model. (OBJS)
 * 29 Nov 2001: Created. (OBJS)
 */

package org.cougaar.core.mts.acking;

import java.io.*;
import java.util.*;

import org.cougaar.core.mts.*;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;


/**
 **  An aspect which implements a message acking scheme.
 **/

public class MessageAckingAspect extends StandardAspect
{
  public static final String SENT_BUT_NOT_ACKED_MSGS = "SentButNotAckedMessages";

  static final String  excludedLinks;
  static final int     resendMultiplier;
  static final float   firstAckPlacingFactor;
  static final float   interAckSpacingFactor;
  static final float   ackAckPlacingFactor;
  static final int     messageAgeWindowInMinutes;

  static MessageResender messageResender;
  static PureAckSender pureAckSender;
  static PureAckAckSender pureAckAckSender;

  private LoggingService log;
  private ThreadService threadService;

  private static final Hashtable receivedAcksTable = new Hashtable();
  private static final Hashtable acksToSendTable = new Hashtable();
  private static final Hashtable successfulReceivesTable = new Hashtable();
  private static final Hashtable successfulSendsTable = new Hashtable();
  private static final Hashtable lastReceiveLinkTable = new Hashtable();
  private static final Hashtable lastSuccessfulLinkTable = new Hashtable();
  private static final Hashtable lastSendTimeTable = new Hashtable();

  private static MessageAckingAspect instance;
  private static String thisNode;

  static
  {
    //  Read external properties

    String s = "org.cougaar.message.transport.aspects.acking.excludedLinks";
    String defaultList = "";
//  String defaultList = "org.cougaar.core.mts.RMILinkProtocol";  // comma separated list
//String defaultList = "org.cougaar.core.mts.OutgoingSocketLinkProtocol";  // comma separated list
    excludedLinks = System.getProperty (s, defaultList);

    s = "org.cougaar.message.transport.aspects.acking.resendMultiplier";
    resendMultiplier = Integer.valueOf(System.getProperty(s,"4")).intValue();

    s = "org.cougaar.message.transport.aspects.acking.firstAckPlacingFactor";
//  float def = ((float)resendMultiplier)/2.0f;  ??? where'd this value come from?  .7 sounds better
    firstAckPlacingFactor = Float.valueOf(System.getProperty(s,"0.7")).floatValue();

    s = "org.cougaar.message.transport.aspects.acking.interAckSpacingFactor";
    interAckSpacingFactor = Float.valueOf(System.getProperty(s,"1.5")).floatValue();

    s = "org.cougaar.message.transport.aspects.acking.ackAckPlacingFactor";
    ackAckPlacingFactor = Float.valueOf(System.getProperty(s,"0.5")).floatValue();

    s = "org.cougaar.message.transport.aspects.acking.msgAgeWindowInMinutes";
    messageAgeWindowInMinutes = Integer.valueOf(System.getProperty(s,"30")).intValue();
  }

  public MessageAckingAspect () 
  {}

  public void load ()
  {
    super.load();

    log = loggingService;
    thisNode = getRegistry().getIdentifier();

    synchronized (MessageAckingAspect.class)
    {
      if (instance == null)
      {
        //  Kick off worker threads

        messageResender = new MessageResender (this);
        threadService().getThread (this, messageResender, "MessageResender").start();

        pureAckSender = new PureAckSender (this);
        threadService().getThread (this, pureAckSender, "PureAckSender").start();

        pureAckAckSender = new PureAckAckSender (this);
        threadService().getThread (this, pureAckAckSender, "PureAckAckSender").start();

        instance = this;
      }
    }
  }

  public Object getDelegate (Object delegate, Class type) 
  {
    if (type == SendQueue.class) 
    {
      return new SendMessage ((SendQueue) delegate);
    }
    else if (type == SendLink.class) 
    {
      return new AgentArrivals ((SendLink) delegate);
    }
    else if (type == DestinationLink.class) 
    {
      DestinationLink link = (DestinationLink) delegate;
      if (link.getProtocolClass().getName().equals ("org.cougaar.core.mts.LoopbackLinkProtocol")) return null;
      return new AckFrontend (link, this);
    }
 
    return null;
  }

  public Object getReverseDelegate (Object delegate, Class type) 
  {
    if (type == MessageDeliverer.class) 
    {
      return new AckBackend ((MessageDeliverer)delegate, this);
    }

    return null;
  }

  private ThreadService threadService () 
  {
	if (threadService != null) return threadService;
	threadService = (ThreadService) getServiceBroker().getService (this, ThreadService.class, null);
	return threadService;
  }

  private class AgentArrivals extends SendLinkDelegateImplBase 
  {
    public AgentArrivals (SendLink link)
    {
      super (link);
    }

    public synchronized void registerClient (MessageTransportClient client)
    {
      if (log.isDebugEnabled()) log.debug ("AgentArrivals: entered register client");
        
      super.registerClient (client);

      //  At this point we may now access the AgentState of the agent.
      //  Extract any sent-but-not-acked messages he has and send them.

      MessageAddress myAddress = getAddress();
      AgentState myState = getRegistry().getAgentState (myAddress);
      Vector v = null;

      synchronized (myState) 
      {
        v = (Vector) myState.getAttribute (SENT_BUT_NOT_ACKED_MSGS);
        if (v != null) myState.setAttribute (SENT_BUT_NOT_ACKED_MSGS, null);
      }

      if (v == null) 
      {
        if (log.isDebugEnabled()) log.debug ("AgentArrivals: agentState is null for " +myAddress);
        return;  // no messages
      }

      if (log.isDebugEnabled()) log.debug ("AgentArrivals: agentState has " +v.size()+
        " message" +(v.size()==1? "" : "s")+ " for " +myAddress);

      //  Process and send each message in the agent state

      for (Enumeration e=v.elements(); e.hasMoreElements(); )
      {
        AttributedMessage msg = (AttributedMessage) e.nextElement();

        if (MessageUtils.isRegularMessage (msg))
        {
          //  Ensure message attributes ok

          if (!MessageIntegrity.areMessageAttributesOK (msg, log))
          {
            if (log.isDebugEnabled()) log.debug ("AgentArrivals: dropping message " +
              "that failed attributes integrity check: " +MessageUtils.toString(msg));
          }

          //  Fix up the message state for its new sending node

          MessageUtils.getFromAgent(msg).setNodeName (thisNode);
          MessageUtils.setAck (msg, null);
          MessageUtils.setSendProtocolLink (msg, null);

          //  Reclassify message as local if needed

          if (isLocalAgent (MessageUtils.getTargetAgent(msg))) MessageUtils.setMessageTypeToLocal (msg);

          //  Send the message

          if (log.isDebugEnabled()) log.debug ("AgentArrivals: sending " +MessageUtils.toString(msg));
          sendMessage (msg);
        }
        else
        {
          if (log.isDebugEnabled()) log.debug ("AgentArrivals: dropping non-regular message " +
            "that somehow got on agent state: " +MessageUtils.toString(msg));
        }
      }
    }

    public void sendMessage (AttributedMessage msg) 
    {
      super.sendMessage (msg);
    }
  }

  //  Global data structures

  //  receivedAcksTable

  static void addReceivedAcks (String node, Vector acks)
  {
    if (acks == null || acks.isEmpty()) return;

    synchronized (receivedAcksTable)
    {
      Hashtable table = (Hashtable) receivedAcksTable.get (node);

      if (table == null) 
      {
        table = new Hashtable();
        receivedAcksTable.put (node, table);
      }
//System.out.println ("adding received acks: node=" +node);

      //  Add and prune

      for (Enumeration a=acks.elements(); a.hasMoreElements(); )
      {
        AckList ackList = (AckList) a.nextElement();

        AgentID fromAgent = ackList.getFromAgent();
        AgentID toAgent = ackList.getToAgent();
        String key = AgentID.makeAckingSequenceID (fromAgent, toAgent);

        Vector currentAcks = (Vector) table.get (key);        
        
        if (currentAcks == null)
        {
          currentAcks = new Vector();
          table.put (key, currentAcks);
        }
        
        currentAcks.add (new AckList (ackList));  // a copy because we prune it

        //  Prune the all the current acks against messages already acked

        NumberList alreadyAckedMsgs = getSuccessfulSends (fromAgent, toAgent);
        Vector prunedAcks = new Vector();

        for (Enumeration c=currentAcks.elements(); c.hasMoreElements(); )
        {
          AckList cackList = (AckList) c.nextElement();
          cackList.remove (alreadyAckedMsgs);
//System.out.println ("adding received acks: pruned acks: " +cackList);
          if (!cackList.isEmpty()) prunedAcks.add (cackList);
        }
//System.out.println ("adding received acks: prunedAcks size = " +prunedAcks.size());
        table.put (key, prunedAcks);
      }
    }

    //  Ding the message resender to let him know that new ack data has arrived

    dingTheMessageResender();
  }

  static Vector getReceivedAcks (AttributedMessage msg)
  {
    AgentID fromAgent = MessageUtils.getFromAgent (msg);
    AgentID toAgent = MessageUtils.getToAgent (msg);
    return getReceivedAcks (fromAgent, toAgent);
  }

  static Vector getReceivedAcks (AgentID fromAgent, AgentID toAgent)
  {
    //  Returns the real thing (not a copy)

    synchronized (receivedAcksTable)
    {
      String node = toAgent.getNodeName();  // where the ack came from
      Hashtable table = (Hashtable) receivedAcksTable.get (node);
//System.out.println ("getReceivedAcks: node="+node+" table="+table);
      if (table == null) return new Vector();

      String key = AgentID.makeAckingSequenceID (fromAgent, toAgent);
      Vector acks = (Vector) table.get (key);
//System.out.println ("getReceivedAcks: key="+key+" acks="+acks);
      if (acks == null) acks = new Vector();
      return acks;
    }
  }

  static boolean hasMessageBeenAcked (AttributedMessage msg)
  {
    //  Simple denials

    if (!hasNonPureAck (msg)) return false;
    if (MessageUtils.getMessageNumber(msg) == 0) return false;

    //  Then try here

    if (wasSuccessfulSend (msg)) return true;

    //  And finally here

    AgentID fromAgent = MessageUtils.getFromAgent (msg);
    AgentID toAgent = MessageUtils.getToAgent (msg);
    Vector acks = getReceivedAcks (fromAgent, toAgent);

    int msgNum = MessageUtils.getMessageNumber (msg);

    for (Enumeration a=acks.elements(); a.hasMoreElements(); )
    {
      AckList ackList = (AckList) a.nextElement();
      if (ackList.find (msgNum)) return true;
    }

    return false;
  }


  //  acksToSendTable

  static boolean addAckToSend (AttributedMessage msg)
  {
//System.out.println ("addAckToSend: "+MessageUtils.toString(msg));
    AgentID fromAgent = MessageUtils.getFromAgent (msg);
    AgentID toAgent = MessageUtils.getToAgent (msg);
    int msgNum = MessageUtils.getMessageNumber (msg);
    return addAckToSend (fromAgent, toAgent, msgNum);
  }

  static boolean addAckToSend (AgentID fromAgent, AgentID toAgent, int msgNum)
  {
    synchronized (acksToSendTable)
    {
      String node = fromAgent.getNodeName();
      Hashtable table = (Hashtable) acksToSendTable.get (node);

      if (table == null)
      {
        table = new Hashtable();
        acksToSendTable.put (node, table);
      }

      String key = AgentID.makeAckingSequenceID (fromAgent, toAgent);
      AckList acks = (AckList) table.get (key);

      if (acks == null) 
      { 
        acks = new AckList (fromAgent, toAgent); 
        table.put (key, acks); 
      }

      return acks.add (msgNum);  // not added if already in list
    }
  }

  static void removeAcksToSend (String node, Vector acks)
  {
    if (acks == null || acks.isEmpty()) return;

//System.out.println ("removeAcksToSend:  node="+node+" :");
//AckList.printAcks (acks);

    synchronized (acksToSendTable)
    {
      Hashtable table = (Hashtable) acksToSendTable.get (node);
      if (table == null) return;

      for (Enumeration a=acks.elements(); a.hasMoreElements(); )
      {
        AckList removals = (AckList) a.nextElement();
        AckList current = (AckList) table.get (removals.getAckingSequenceID());

        if (current != null)
        {
          try { current.remove (removals); }
          catch (Exception e) { e.printStackTrace(); }

          if (current.isEmpty()) table.remove (removals.getAckingSequenceID());
        }
      }
    }
  }

  static Vector getAcksToSend (String node)
  {
    synchronized (acksToSendTable)
    {
      Hashtable table = (Hashtable) acksToSendTable.get (node);
      if (table == null) return new Vector();

      Vector acks = new Vector (table.size());

      for (Enumeration a=table.elements(); a.hasMoreElements(); ) 
      {
        //  Return copies, not actual table entries!

        AckList ackList = (AckList) a.nextElement();
        acks.add (new AckList (ackList));
      }

      return acks;
    }
  }

  static boolean findAckToSend (Ack ack)
  {
    AgentID fromAgent = MessageUtils.getFromAgent (ack.getMsg());
    AgentID toAgent = MessageUtils.getToAgent (ack.getMsg());
    int msgNum = MessageUtils.getMessageNumber (ack.getMsg());
    return findAckToSend (fromAgent, toAgent, msgNum);
  }

  static boolean findAckToSend (PureAckMessage pam)
  {
    AgentID srcFromAgent = MessageUtils.getToAgent (pam);
    AgentID srcToAgent = MessageUtils.getFromAgent (pam);
    int srcMsgNum = MessageUtils.getSrcMsgNumber (pam);
    return findAckToSend (srcFromAgent, srcToAgent, srcMsgNum);
  }

  static boolean findAckToSend (AgentID fromAgent, AgentID toAgent, int msgNum)
  {
    synchronized (acksToSendTable)
    {
      String node = fromAgent.getNodeName();
      Hashtable table = (Hashtable) acksToSendTable.get (node);
      if (table == null) return false;

      String key = AgentID.makeAckingSequenceID (fromAgent, toAgent);
      AckList acks = (AckList) table.get (key);
      if (acks == null) return false;

      return acks.find (msgNum);
    }
  }


  //  successfulReceivesTable

  static boolean addSuccessfulReceive (AttributedMessage msg)
  {
    AgentID fromAgent = MessageUtils.getFromAgent (msg);
    AgentID toAgent = MessageUtils.getToAgent (msg);
    int msgNum = MessageUtils.getMessageNumber (msg);
    return addSuccessfulReceive (fromAgent, toAgent, msgNum);
  }

  static boolean addSuccessfulReceive (AgentID fromAgent, AgentID toAgent, int msgNum)
  {
    if (msgNum == 0) return false;

    synchronized (successfulReceivesTable)
    {
      String key = AgentID.makeAgentPairID (fromAgent, toAgent);  // not acking seq id
      NumberList receives = (NumberList) successfulReceivesTable.get (key);

      if (receives == null) 
      { 
        receives = new NumberList(); 
        successfulReceivesTable.put (key, receives); 
      }

      return receives.add (msgNum);  // not added if already in list
    }
  }

  static boolean wasSuccessfulReceive (AttributedMessage msg)
  {
    AgentID fromAgent = MessageUtils.getFromAgent (msg);
    AgentID toAgent = MessageUtils.getToAgent (msg);
    int msgNum = MessageUtils.getMessageNumber (msg);
    return wasSuccessfulReceive (fromAgent, toAgent, msgNum);
  }

  static boolean wasSuccessfulReceive (AgentID fromAgent, AgentID toAgent, int msgNum)
  {
    if (msgNum == 0) return false;

    synchronized (successfulReceivesTable)
    {
      String key = AgentID.makeAgentPairID (fromAgent, toAgent);  // not acking seq id
      NumberList receives = (NumberList) successfulReceivesTable.get (key);
      if (receives == null) return false;
      return receives.find (msgNum);
    }
  }


  //  successfulSendsTable

  public static boolean addSuccessfulSend (AttributedMessage msg)
  {
    AgentID fromAgent = MessageUtils.getFromAgent (msg);
    AgentID toAgent = MessageUtils.getToAgent (msg);
    int msgNum = MessageUtils.getMessageNumber (msg);
    return addSuccessfulSend (fromAgent, toAgent, msgNum);
  }

  static boolean addSuccessfulSend (AgentID fromAgent, AgentID toAgent, int msgNum)
  {
    if (msgNum == 0) return false;

    synchronized (successfulSendsTable)
    {
      String node = toAgent.getNodeName();
      Hashtable table = (Hashtable) successfulSendsTable.get (node);

      if (table == null)
      {
        table = new Hashtable();
        successfulSendsTable.put (node, table);
      }

      String key = AgentID.makeAckingSequenceID (fromAgent, toAgent);
      NumberList sends = (NumberList) table.get (key);

      if (sends == null) 
      { 
        sends = new NumberList(); 
        table.put (key, sends); 
      }

      return sends.add (msgNum);  // not added if already in list
    }
  }

  static NumberList getSuccessfulSends (AgentID fromAgent, AgentID toAgent)
  {
    synchronized (successfulSendsTable)
    {
      String node = toAgent.getNodeName();
      Hashtable table = (Hashtable) successfulSendsTable.get (node);
      if (table == null) return new NumberList();

      String key = AgentID.makeAckingSequenceID (fromAgent, toAgent);
      NumberList sends = (NumberList) table.get (key);
      return new NumberList (sends);  // return copy (may be empty)
    }
  }

  static boolean wasSuccessfulSend (AttributedMessage msg)
  {
    AgentID fromAgent = MessageUtils.getFromAgent (msg);
    AgentID toAgent = MessageUtils.getToAgent (msg);
    int msgNum = MessageUtils.getMessageNumber (msg);
    return wasSuccessfulSend (fromAgent, toAgent, msgNum);
  }

  static boolean wasSuccessfulSend (AgentID fromAgent, AgentID toAgent, int msgNum)
  {
    if (msgNum == 0) return false;
    return getSuccessfulSends(fromAgent,toAgent).find (msgNum);
  }

  
  //  lastReceiveLinkTable

  private static final String UDPReceiveLink =
    new String ("org.cougaar.core.mts.udp.IncomingUDPLinkProtocol");
  private static final String SocketReceiveLink =
    new String ("org.cougaar.core.mts.socket.IncomingSocketLinkProtocol");
  private static final String EmailReceiveLink =
    new String ("org.cougaar.core.mts.email.IncomingEmailLinkProtocol");

  static void setLastReceiveLink (String node, String link)
  {
    //  Convert any split links

    if (link.equals ("org.cougaar.core.mts.udp.OutgoingUDPLinkProtocol"))
    {
      link = UDPReceiveLink;
    }
    else if (link.equals ("org.cougaar.core.mts.socket.OutgoingSocketLinkProtocol"))
    {
      link = SocketReceiveLink;
    }
    else if (link.equals ("org.cougaar.core.mts.socket.OutgoingEmailLinkProtocol"))
    {
      link = EmailReceiveLink;
    }

    synchronized (lastReceiveLinkTable)
    {
      lastReceiveLinkTable.put (node, link);
    }
  }

  static String getLastReceiveLink (String node)
  {
    synchronized (lastReceiveLinkTable)
    {
      return (String) lastReceiveLinkTable.get (node);
    }
  }

  static String getReceiveLinkPrediction (String node)
  {
    //  Hack, but reasonable

    return getLastReceiveLink (node);
  }


  //  lastSuccessfulLinkTable

  static void setLastSuccessfulLinkUsed (String node, Class linkClass)
  {
    synchronized (lastSuccessfulLinkTable)
    {
      lastSuccessfulLinkTable.put (node, linkClass);
    }
  }

  public static Class getLastSuccessfulLinkUsed (String node)
  {
    synchronized (lastSuccessfulLinkTable)
    {
      return (Class) lastSuccessfulLinkTable.get (node);
    }
  }


  //  lastSendTimeTable

  static void setLastSendTime (String sendLink, String node, long sendTime)
  {
    //  We don't update the last send time with the value given unless the
    //  value is later than the current one.  Just a little integrity 
    //  enforcement, important in a multi-threaded world.

    synchronized (lastSendTimeTable)
    {
      String key = sendLink + "::" + node;
      LongInt lastSendTime = (LongInt) lastSendTimeTable.get (key);
      if (lastSendTime != null && lastSendTime.value > sendTime) return;  // not last for link
      if (lastSendTime == null) lastSendTimeTable.put (key, new LongInt (sendTime));
      else lastSendTime.value = sendTime;

      key = "::" + node;
      lastSendTime = (LongInt) lastSendTimeTable.get (key);
      if (lastSendTime != null && lastSendTime.value > sendTime) return;  // not last for all links
      if (lastSendTime == null) lastSendTimeTable.put (key, new LongInt (sendTime));
      else lastSendTime.value = sendTime;
    }
  }

  public static long getLastSendTime (DestinationLink sendLink, String node)
  {
    return getLastSendTime (sendLink.getProtocolClass().getName(), node);
  }

  static long getLastSendTime (String node)
  {
    return getLastSendTime ("", node);
  }

  static long getLastSendTime (String sendLink, String node)
  {
    synchronized (lastSendTimeTable)
    {
      String key = sendLink + "::" + node;
      LongInt lastSendTime = (LongInt) lastSendTimeTable.get (key);
      return (lastSendTime != null ? lastSendTime.value : 0);
    }
  }

  //  Utility methods and classes

  LoggingService getTheLoggingService ()
  {
    return loggingService;
  }

  AgentState getAgentState (MessageAddress agent)
  {
    return getRegistry().getAgentState (agent);
  }

  String getThisNode ()
  {
    return thisNode;
  }

  boolean isLocalAgent (MessageAddress agent)
  {
    return getRegistry().isLocalClient (agent);
  }

  static void dingTheMessageResender ()
  {
    messageResender.ding();
  }

  static boolean isExcludedLink (DestinationLink link)
  {
    return isExcludedLink (link.getProtocolClass().getName());
  }

  static boolean isExcludedLink (String link)
  {
    return excludedLinks.indexOf (link) >= 0;
  }

  static boolean hasNonPureAck (AttributedMessage msg)
  {
    Ack ack = MessageUtils.getAck (msg);
    if (ack != null && ack.isAck()) return true;
    return false;
  }

  public static void addToPureAckSender (PureAckMessage ackMsg)  // used by policy to reschedule acks
  {
    pureAckSender.add (ackMsg);
  }

  private static class Int  // a mutable int
  {
    public int value;
    public Int (int v) { value = v; }
  }

  private static class LongInt  // a mutable long
  {
    public long value;
    public LongInt (long v) { value = v; }
  }

  static void updateMessageHistory (Ack ack, AckList ackList)
  {
    //  HACK - not doing anything here yet
  }

  static String getLinkType (String name)
  {
    return AdaptiveLinkSelectionPolicy.getLinkType (name);
  }

  static String getLinkType (Class c)
  {
    return AdaptiveLinkSelectionPolicy.getLinkType (c.getName());
  }

  public static float getInterAckSpacingFactor ()
  {
    return interAckSpacingFactor;
  }

  private static long now ()
  {
    return System.currentTimeMillis();
  }

  private static String toDate (long time)
  {
    if (time == 0) return "0";

    //  Return date string with milliseconds

    String date = (new Date(time)).toString();
    String d1 = date.substring (0, 19);
    String d2 = date.substring (19);
    long ms = time % 1000;
    return d1 + "." + ms + d2;
  }
}
