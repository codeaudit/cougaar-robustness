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
 * 06 Jun 2002: Completely revamped for Cougaar 9.2.x (OBJS)
 * 08 Jan 2002: Egregious temporary hack to handle last minute traffic
 *              masking messages.  (OBJS)
 * 07 Jan 2002: Implemented new acking model. (OBJS)
 * 29 Nov 2001: Created. (OBJS)
 */

package org.cougaar.core.mts.acking;

import java.io.*;
import java.util.*;

import org.cougaar.core.mts.*;
import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.ThreadService;

/**
 **  An aspect which implements a message acking scheme.
 **/

public class MessageAckingAspect extends StandardAspect
{
  static final boolean debug;

  static final boolean showTraffic;
  static final boolean ackingOn;
  static final int     messageAgeWindow;
  static final int     initialEmailRoundtripTime;
  static final int     initialNNTPRoundtripTime;
  static final int     initialOtherRoundtripTime;
  static final int     resendMultiplier;
  static final float   firstAckPlacingFactor;
  static final float   interAckSpacingFactor;
  static final float   ackAckPlacingFactor;
  static final int     runningAveragePoolSize;
  static final int     maxMessageResends;

  static AckWaiter ackWaiter;
  static PureAckSender pureAckSender;
  static PureAckAckSender pureAckAckSender;

  static final Object serializationLock = new Object();

  private static final Hashtable receivedAcksTable = new Hashtable();
  private static final Hashtable acksToSendTable = new Hashtable();
  private static final Hashtable successfulReceivesTable = new Hashtable();
  private static final Hashtable successfulSendsTable = new Hashtable();

  private static final Hashtable lastReceiveLinkTable = new Hashtable();
  private static final Hashtable lastSuccessfulLinkTable = new Hashtable();
  private static final Hashtable lastSendTimeTable = new Hashtable();
  private static final Hashtable roundtripTable = new Hashtable();

  private static MessageAckingAspect instance;

  private ThreadService threadService;

  static
  {
    //  Read external properties

    String s = "org.cougaar.message.transport.aspects.acking.debug";
//    debug = Boolean.valueOf(System.getProperty(s,"false")).booleanValue();
debug = true;

    s = "org.cougaar.message.transport.aspects.acking.showTraffic";
    showTraffic = debug || Boolean.valueOf(System.getProperty(s,"false")).booleanValue();

    s = "org.cougaar.message.transport.aspects.acking.on";
    ackingOn = Boolean.valueOf(System.getProperty(s,"true")).booleanValue();

    s = "org.cougaar.message.transport.aspects.acking.initEmailRndTrip";
    initialEmailRoundtripTime = Integer.valueOf(System.getProperty(s,"5000")).intValue();

    s = "org.cougaar.message.transport.aspects.acking.initNNTPRndTrip";
    initialNNTPRoundtripTime = Integer.valueOf(System.getProperty(s,"10000")).intValue();

    s = "org.cougaar.message.transport.aspects.acking.initOtherRndTrip";
    initialOtherRoundtripTime = Integer.valueOf(System.getProperty(s,"1000")).intValue();

    s = "org.cougaar.message.transport.aspects.acking.resendMultiplier";
    resendMultiplier = Integer.valueOf(System.getProperty(s,"7")).intValue();

    s = "org.cougaar.message.transport.aspects.acking.firstAckPlacingFactor";
    float def = ((float)resendMultiplier)/2.0f;
    firstAckPlacingFactor = Float.valueOf(System.getProperty(s,""+def)).floatValue();

    s = "org.cougaar.message.transport.aspects.acking.interAckSpacingFactor";
    interAckSpacingFactor = Float.valueOf(System.getProperty(s,"1.5")).floatValue();

    s = "org.cougaar.message.transport.aspects.acking.ackAckPlacingFactor";
    ackAckPlacingFactor = Float.valueOf(System.getProperty(s,"0.5")).floatValue();

    s = "org.cougaar.message.transport.aspects.acking.msgAgeWindowInMinutes";
    messageAgeWindow = Integer.valueOf(System.getProperty(s,"30")).intValue();

    s = "org.cougaar.message.transport.aspects.acking.runningAvgPoolSize";
    runningAveragePoolSize = Integer.valueOf(System.getProperty(s,"5")).intValue();

    s = "org.cougaar.message.transport.aspects.acking.maxMessageResends";
    maxMessageResends = Integer.valueOf(System.getProperty(s,"1000")).intValue();
  }

  public MessageAckingAspect () 
  {}

  public Object getDelegate (Object delegate, Class type) 
  {
	if (type == SendQueue.class) 
    {
      if (instance == null) startup();  // HACK to call thread service at right time

	  return new MessageSender ((SendQueue) delegate);
    }
    else if (type == DestinationLink.class) 
    {
      //  Avoid the loopback link - local messages are not acked!

      DestinationLink link = (DestinationLink) delegate;
      String linkProtocol = link.getProtocolClass().getName();
      
      if (!linkProtocol.equals ("org.cougaar.core.mts.LoopbackLinkProtocol")) 
      {
        return new AckFrontend (link);
      }
    }
 
    return null;
  }

  public Object getReverseDelegate (Object delegate, Class type) 
  {
    if (type == MessageDeliverer.class) 
    {
      return new AckBackend ((MessageDeliverer) delegate);
    }
 
    return null;
  }

  public static boolean isAckingOn ()
  {
    return ackingOn;
  }

  public void startup () 
  {
    if (isAckingOn()) 
    {
      synchronized (MessageAckingAspect.class)
      {
        if (instance == null)
        {
          //  Kick off worker threads

          ackWaiter = new AckWaiter();
          threadService().getThread (this, ackWaiter, "AckWaiter").start();

          pureAckSender = new PureAckSender();
          threadService().getThread (this, pureAckSender, "PureAckSender").start();

          pureAckAckSender = new PureAckAckSender();
          threadService().getThread (this, pureAckAckSender, "PureAckAckSender").start();

          instance = this;
        }
      }
    }
  }

  private ThreadService threadService () 
  {
	if (threadService != null) return threadService;
	ServiceBroker sb = getServiceBroker();
	threadService = (ThreadService) sb.getService (this, ThreadService.class, null);
	return threadService;
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

      //  Add and prune

      for (Enumeration a=acks.elements(); a.hasMoreElements(); )
      {
        AckList ackList = (AckList) a.nextElement();

        AgentID fromAgent = ackList.getFromAgent();
        AgentID toAgent = ackList.getToAgent();
        String key = AgentID.makePairKey (fromAgent, toAgent);

        Vector currentAcks = (Vector) table.get (key);        
        
        if (currentAcks == null)
        {
          currentAcks = new Vector();
          table.put (key, currentAcks);
        }
        
        currentAcks.add (ackList);

        //  Prune the all the current acks against messages already acked

        Vector prunedAcks = new Vector();
        NumberList alreadyAckedMsgs = getSuccessfulSends (fromAgent, toAgent);

        for (Enumeration c=currentAcks.elements(); c.hasMoreElements(); )
        {
          AckList cackList = (AckList) c.nextElement();
          cackList.remove (alreadyAckedMsgs);
          if (cackList.size() > 0) prunedAcks.add (cackList);
        }

        if (prunedAcks.size() > 0) table.put (key, prunedAcks);
      }
    }

    //  Ding the ack waiter to let him know that new ack data has arrived

    ackWaiter.ding();
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
      String node = fromAgent.getNodeName();
      Hashtable table = (Hashtable) receivedAcksTable.get (node);
      if (table == null) return new Vector();

      String key = AgentID.makePairKey (fromAgent, toAgent);
      Vector acks = (Vector) table.get (key);
      if (acks == null) acks = new Vector();
      return acks;
    }
  }

  static boolean hasMessageBeenAcked (AttributedMessage msg)
  {
    //  First try here

    if (wasSuccessfulSend (msg)) return true;

    //  Then here

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

      String key = AgentID.makePairKey (fromAgent, toAgent);
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

    synchronized (acksToSendTable)
    {
      Hashtable table = (Hashtable) acksToSendTable.get (node);
      if (table == null) return;

      for (Enumeration a=acks.elements(); a.hasMoreElements(); )
      {
        AckList removals = (AckList) a.nextElement();
        String key = removals.getPairKey();
        Vector currentAcks = (Vector) table.get (key);
        if (currentAcks == null) continue;

        for (Enumeration c=currentAcks.elements(); c.hasMoreElements(); )
        {
          AckList current = (AckList) c.nextElement();
          try { current.remove (removals); }
          catch (Exception e) { e.printStackTrace(); }
          if (current.isEmpty()) currentAcks.remove (current);
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
      return new Vector (table.values());  // return copy
    }
  }

  static boolean findAckToSend (Ack ack)
  {
    AgentID fromAgent = MessageUtils.getFromAgent (ack.getMsg());
    AgentID toAgent = MessageUtils.getToAgent (ack.getMsg());
    int msgNum = MessageUtils.getMessageNumber (ack.getMsg());
    return findAckToSend (fromAgent, toAgent, msgNum);
  }

  static boolean findAckToSend (AgentID fromAgent, AgentID toAgent, int msgNum)
  {
    synchronized (acksToSendTable)
    {
      String node = fromAgent.getNodeName();
      Hashtable table = (Hashtable) acksToSendTable.get (node);
      if (table == null) return false;

      String key = AgentID.makePairKey (fromAgent, toAgent);
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
    synchronized (successfulReceivesTable)
    {
      String node = fromAgent.getNodeName();
      Hashtable table = (Hashtable) successfulReceivesTable.get (node);

      if (table == null)
      {
        table = new Hashtable();
        successfulReceivesTable.put (node, table);
      }

      String key = AgentID.makePairKey (fromAgent, toAgent);
      NumberList receives = (NumberList) table.get (key);

      if (receives == null) 
      { 
        receives = new NumberList(); 
        table.put (key, receives); 
      }

      return receives.add (msgNum);  // not added if already in list
    }
  }


  //  successfulSendsTable

  static boolean addSuccessfulSend (AttributedMessage msg)
  {
    AgentID fromAgent = MessageUtils.getFromAgent (msg);
    AgentID toAgent = MessageUtils.getToAgent (msg);
    int msgNum = MessageUtils.getMessageNumber (msg);
    return addSuccessfulSend (fromAgent, toAgent, msgNum);
  }

  static boolean addSuccessfulSend (AgentID fromAgent, AgentID toAgent, int msgNum)
  {
    synchronized (successfulSendsTable)
    {
      String node = toAgent.getNodeName();
      Hashtable table = (Hashtable) successfulSendsTable.get (node);

      if (table == null)
      {
        table = new Hashtable();
        successfulSendsTable.put (node, table);
      }

      String key = AgentID.makePairKey (fromAgent, toAgent);
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

      String key = AgentID.makePairKey (fromAgent, toAgent);
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
    return getSuccessfulSends(fromAgent,toAgent).find (msgNum);
  }

  
  //  lastReceiveLinkTable

  private static final Classname UDPReceiveLink =
    new Classname (org.cougaar.core.mts.udp.IncomingUDPLinkProtocol.class);
  private static final Classname SocketReceiveLink =
    new Classname (org.cougaar.core.mts.socket.IncomingSocketLinkProtocol.class);
  private static final Classname EmailReceiveLink =
    new Classname (org.cougaar.core.mts.email.IncomingEmailLinkProtocol.class);

  static void setLastReceiveLink (String node, Classname link)
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

  static Classname getLastReceiveLink (String node)
  {
    synchronized (lastReceiveLinkTable)
    {
      return (Classname) lastReceiveLinkTable.get (node);
    }
  }

  static Classname getReceiveLinkPrediction (String node)
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

  static void setLastSendTime (Classname sendLink, String node, long sendTime)
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

  static long getLastSendTime (Classname sendLink, String node)
  {
    return getLastSendTime (sendLink, node);
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


  //  roundtripTimeTable

  //  Roundtrip time is defined to the be time a message is sent subtracted
  //  from the time the first ack is received for it.  Note that it may not
  //  be the first time the message was sent nor the first ack sent.

  private static class RoundtripData
  {
    public RunningAverage measurement;
    public int report;
  }

  static void updateRoundtripTimeMeasurement (Ack ack, AckList ackList)
  {
    if (ack.getSendCount() > 1) 
    {
      //  HACK - cannot currently match acks with resent messages.
      //  More than matching, there are other issues here that we will
      //  get into later.  Shouldn't be a big deal for now.

      return;  
    }

    synchronized (roundtripTable)
    {
      String node = MessageUtils.getToAgentNode (ack.getMsg());
      String key = node +"::"+ ackList.getSendLinkType();
      Hashtable table = (Hashtable) roundtripTable.get (key);

      if (table == null)
      {
        table = new Hashtable();
        roundtripTable.put (key, table);
      }

      key = ackList.getReceiveLinkType();
      RoundtripData data = (RoundtripData) table.get (key);

      if (data == null)
      {
        data = new RoundtripData();
        table.put (key, data);
      }

      if (data.measurement == null)
      {
        //  This is a highly simplistic way to deal with a time-varying series
        //  of data samples, but hopefully it is ok for a first cut.  We will
        //  need something that will work well with the short (transient), mid,
        //  and long term variations that occur in the kinds of comm channels
        //  we are using (basically udp, socket, email, and nntp).

        //  HACK - set averages to be fairly fast reacting for now

        data.measurement = new RunningAverage (runningAveragePoolSize, 0.0, 0);  
      }

      int roundtripTime = (int) (ackList.getReceiveTime() - ack.getSendTime());
      if (roundtripTime > 0) data.measurement.add (roundtripTime);

//System.out.println ("Update roundtrip measurement: time=" +roundtripTime+" avg=" 
//+data.measurement.getAverage()+ " nsamples="+data.measurement.getNumSamples()); 
    }    
  }

  static void updateRoundtripTimeReport (String node, Classname sendLink, Classname receiveLink, int report)
  {
    updateRoundtripTimeReport (node, getLinkType (sendLink), getLinkType (receiveLink), report);
  }

  static void updateRoundtripTimeReport (String node, String sendLinkType, String receiveLinkType, int report)
  {
//System.out.println ("Update roundtrip report: time=" +report);

    if (report < 1) return;

    synchronized (roundtripTable)
    {
      String key = node +"::"+ sendLinkType;
      Hashtable table = (Hashtable) roundtripTable.get (key);

      if (table == null)
      {
        table = new Hashtable();
        roundtripTable.put (key, table);
      }

      key = receiveLinkType;
      RoundtripData data = (RoundtripData) table.get (key);

      if (data == null)
      {
        data = new RoundtripData();
        table.put (key, data);
      }

      data.report = report;
    }    
  }

  static int getRoundtripTimeMeasurement (String node, Classname sendLink, Classname receiveLink)
  {
    return getRoundtripTimeMeasurement (node, getLinkType (sendLink), getLinkType (receiveLink));
  }

  static int getRoundtripTimeMeasurement (String node, String sendLinkType, String receiveLinkType)
  {
    synchronized (roundtripTable)
    {
      String key = node +"::"+ sendLinkType;
      Hashtable table = (Hashtable) roundtripTable.get (key);

      if (table != null)
      {
        key = receiveLinkType;
        RoundtripData data = (RoundtripData) table.get (key);
      
        if (data != null)
        {
          if (data.measurement != null) return (int) data.measurement.getAverage();
        }
      }
      
      return getInitialRoundtripTime (sendLinkType);
    }
  }

  static int getRoundtripTimeReport (String node, Classname sendLink, Classname receiveLink)
  {
    return getRoundtripTimeReport (node, getLinkType (sendLink), getLinkType (receiveLink));
  }

  static int getRoundtripTimeReport (String node, String sendLinkType, String receiveLinkType)
  {
    synchronized (roundtripTable)
    {
      String key = node +"::"+ sendLinkType;
      Hashtable table = (Hashtable) roundtripTable.get (key);

      if (table != null)
      {
        key = receiveLinkType;
        RoundtripData data = (RoundtripData) table.get (key);
      
        if (data != null)
        {
          if (data.report > 0) return data.report;
        }
      }

      return getInitialRoundtripTime (sendLinkType);
    }
  }

  static int getInitialRoundtripTime (String sendLinkType)
  {
    //  What to use for the initial roundtrip time?  Note that these initial values 
    //  do not get into the running average, only real measured roundtrip times do.

// UDP ???
// socket ???
    
    if (sendLinkType.equals ("email"))
    {
      return initialEmailRoundtripTime;      
    }
    else if (sendLinkType.equals ("nntp"))
    {
      return initialNNTPRoundtripTime;      
    }
    else // all other link types
    {
      return initialOtherRoundtripTime;      
    }
  }

  public static int getBestRoundtripTimeForLink (DestinationLink link, String node)
  {
    synchronized (roundtripTable)
    {
      String sendLinkType = getLinkType (link.getProtocolClass());
      String key = node +"::"+ sendLinkType;
      Hashtable table = (Hashtable) roundtripTable.get (key);

      if (table != null)
      {
        //  Go through the table and find the smallest non-zero roundtrip time

        int time, smallestTime = Integer.MAX_VALUE;
      
        for (Enumeration e=table.elements(); e.hasMoreElements(); )
        {
          RoundtripData data = (RoundtripData) e.nextElement();

          if (data.measurement != null)
          {
            time = (int) data.measurement.getAverage();
            if (time > 0 && time < smallestTime) smallestTime = time;
          }
        }

        if (smallestTime != Integer.MAX_VALUE) return smallestTime;
      }
      
      return getInitialRoundtripTime (sendLinkType);
    }
  }

  public static int getRoundtripTimeForAck (DestinationLink link, PureAck pureAck)
  {
    String node = MessageUtils.getToAgent(pureAck.getMsg()).getNodeName();

    String sendLinkType = getLinkType (link.getProtocolClass());

    Classname lastReceiveLink = getLastReceiveLink (node);
    if (lastReceiveLink == null) lastReceiveLink = pureAck.getSendLink();
    String receiveLinkType = getLinkType (lastReceiveLink);

    return getRoundtripTimeMeasurement (node, sendLinkType, receiveLinkType);
  }


  //  Utility methods and classes

  static boolean hasRegularAck (AttributedMessage msg)
  {
    Ack ack = MessageUtils.getAck (msg);
    if (ack != null && ack.isRegularAck()) return true;
    return false;
  }

  public static void addToPureAckSender (PureAckMessage ackMsg)  // used by policy to reschedule acks
  {
    pureAckSender.add (ackMsg);
  }

  private static class Int
  {
    public int value;
    public Int (int v) { value = v; }
  }

  private static class LongInt
  {
    public long value;
    public LongInt (long v) { value = v; }
  }

  static void updateMessageHistory (Ack ack, AckList ackList)
  {
    //  HACK - not doing anything here yet
  }

  static String getLinkType (Classname name)
  {
    return AdaptiveLinkSelectionPolicy.getLinkType (name.toString());
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
