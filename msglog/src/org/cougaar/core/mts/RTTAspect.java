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
 * 21 Jul 2002: Created. (OBJS)
 */

package org.cougaar.core.mts;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.acking.RunningAverage;

import java.io.Serializable;
import java.util.*;

/**
 **  Measure the round trip time (RTT) for messages.  The RTT is defined to
 **  be the time a message takes to travel from this node to its destination
 **  node plus the time it takes for another message (of no necessary relation
 **  to the first) to travel from the destination node back to this one.
 **  
 **  Note: There is basically a "one network" assumption here currently, ie.
 **  that all messages to all agents on a node travel over the same network
 **  (although if extra networks are confined to individual protocols then no
 **  problem). When this situation changes this code will need to be updated.
 **/

public class RTTAspect extends StandardAspect implements Serializable  // for embedded class
{
  public static final String RTT_ATTRIBUTE = "RTT_DATA";

  private static final int samplePoolsize;
  private static final int startDelay;
  private static final float percentChangeLimit;
  private static final int changeLimitDelay;

  private static final ServiceImpl serviceImpl = new ServiceImpl();
  private static final Hashtable latestReceptionsTable = new Hashtable();
  private static final Hashtable roundtripTable = new Hashtable();
  private static final Comparator timeSort = new TimeSort();

  static
  {
    //  Read external properties

    String s = "org.cougaar.message.transport.aspects.rtt.samplePoolsize";
    samplePoolsize = Integer.valueOf(System.getProperty(s,"10")).intValue();

    s = "org.cougaar.message.transport.aspects.rtt.startDelay";
    startDelay = Integer.valueOf(System.getProperty(s,"2")).intValue();

    s = "org.cougaar.message.transport.aspects.rtt.percentChangeLimit";
    percentChangeLimit = Float.valueOf(System.getProperty(s,"0.25")).floatValue();

    s = "org.cougaar.message.transport.aspects.rtt.changeLimitDelay";
    changeLimitDelay = Integer.valueOf(System.getProperty(s,"20")).intValue();
  }

  public RTTAspect () 
  {}

  //  Aspect delegation - RTT outbound to DestinationLink, inbound to MessageDeliverer

  public Object getDelegate (Object delegate, Class type) 
  {
    if (type == DestinationLink.class) 
    {
      //  RTTs not maintained for local messages

      DestinationLink link = (DestinationLink) delegate;
      String linkProtocol = link.getProtocolClass().getName();
      
      if (!linkProtocol.equals ("org.cougaar.core.mts.LoopbackLinkProtocol")) 
      {
        return new RTTOutbound (link);
      }
    }
 
    return null;
  }

  public Object getReverseDelegate (Object delegate, Class type) 
  {
    if (type == MessageDeliverer.class) 
    {
      return new RTTInbound ((MessageDeliverer) delegate);
    }
 
    return null;
  }

  //  External RTT service 

  public void load () 
  {
    super.load();
    Provider provider = new Provider();
    getServiceBroker().addService (RTTService.class, provider);
  }

  private class Provider implements ServiceProvider 
  {
    public Object getService (ServiceBroker sb, Object requestor, Class serviceClass) 
    {
      if (serviceClass == RTTService.class) return serviceImpl;
      return null;
    }

    public void releaseService (ServiceBroker sb, Object requestor, Class serviceClass, Object service)
    {}
  }

  private static class ServiceImpl implements RTTService
  {
    public int getBestRoundtripTimeForLink (DestinationLink link, String node)
    {
      return getBestRTTForLink (link, node);
    }
  }

  //  Implementation

  private static class MessageTimes
  {
    public String sendLink;
    public long sendTime;     // by sender's clock
    public long receiveTime;  // by receiver's clock

    public MessageTimes (String sendLink, long sendTime, long receiveTime)
    {
      this.sendLink = sendLink;
      this.sendTime = sendTime;
      this.receiveTime = receiveTime;
    }

    public void setTimes (long sendTime, long receiveTime)
    {
      this.sendTime = sendTime;
      this.receiveTime = receiveTime;
    }

    public String toString ()
    {
      return "sendLink=" +sendLink+ " sendTime=" +sendTime+ " receiveTime=" +receiveTime;
    }
  }

  private void recordLatestMessageReception (String sendNode, String sendLink, long sendTime, long receiveTime)
  {
    //  Sanity check

    if (sendNode == null || sendLink == null) 
    {
      loggingService.error ("Missing sendNode or sendLink - missing msg attribute?");
      return;
    }

    //  Record message reception if it is the latest

    synchronized (latestReceptionsTable)
    {
      Hashtable nodeTable = (Hashtable) latestReceptionsTable.get (sendNode);

      if (nodeTable == null)
      {
        nodeTable = new Hashtable();
        latestReceptionsTable.put (sendNode, nodeTable);
      }

      MessageTimes latest = (MessageTimes) nodeTable.get (sendLink);
      if (latest != null && latest.receiveTime > receiveTime) return;  // not latest for link
      if (latest == null) nodeTable.put (sendLink, new MessageTimes (sendLink, sendTime, receiveTime));
      else latest.setTimes (sendTime, receiveTime);
    }
  }

  private MessageTimes[] getLatestMessageReceptions (String sendNode)
  {
    //  Sanity check

    if (sendNode == null)
    {
      loggingService.error ("Missing sendNode - missing msg attribute?");
      return new MessageTimes[0];
    }

    //  Get latest message receptions for all links of node

    synchronized (latestReceptionsTable)
    {
      Hashtable table = (Hashtable) latestReceptionsTable.get (sendNode);
      Vector v = (table != null ? new Vector (table.values()) : new Vector());
      return (MessageTimes[]) v.toArray (new MessageTimes[v.size()]);
    }
  }

  private class RTTStart implements Serializable  // enclosing class must be serializable too
  {
    private String sendLink;
    private long adjustedSendTime;

    public void setSendLink (String link)
    {
      sendLink = link;
    }

    public String getSendLink ()
    {
      return sendLink;
    }

    public void setAdjustedSendTime (long time)
    {
      adjustedSendTime = time;
    }

    public long getAdjustedSendTime ()
    {
      return adjustedSendTime;
    }

    public String toString ()
    {
      return "sendLink=" +sendLink+ " adjSendTime=" +adjustedSendTime;
    }
  }

  private class RTTOutbound extends DestinationLinkDelegateImplBase 
  {
    private DestinationLink link;
 
    public RTTOutbound (DestinationLink link) 
    {
      super (link);
      this.link = link;
    }

    public MessageAttributes forwardMessage (AttributedMessage msg) 
      throws UnregisteredNameException, NameLookupException, 
             CommFailureException, MisdeliveredMessageException
    {
      //  Set a send time for the message.  Kind of a hack, but need BBN 
      //  support to have link protocols set the send time at the time a 
      //  message is actually sent.

      long sendTime = now();
      MessageUtils.setMessageSendTime (msg, sendTime);
      
      //  Get a candidate set of times from our stored data about the
      //  destination node - these are the latest inbound times of
      //  messages from the destination node, that, in concert with
      //  information from this message being sent, the destination
      //  node will be able to calculate RTTs for a set of currently
      //  active protocol links.

      int MAX_RTTS = 5;  // HACK: arbitrary for now

      String node = MessageUtils.getToAgentNode (msg);
      MessageTimes times[] = getLatestMessageReceptions (node);
      Vector v = null;

      if (times.length > 0)
      {
        v = new Vector();
        Arrays.sort (times, timeSort);  // latest receives first

        for (int i=0; i<MAX_RTTS && i<times.length; i++) 
        {
          RTTStart start = new RTTStart();
          start.setSendLink (times[i].sendLink);
          long nodeTime = sendTime - times[i].receiveTime;  // time msg spent here
          start.setAdjustedSendTime (times[i].sendTime + nodeTime);
          v.add (start);
        }
      }

//inject bug to test link delivery 
//v = new Vector();
//v.add (new String ("a bug"));

      msg.setAttribute (RTT_ATTRIBUTE, v);

      //  Send the message on its way

      return link.forwardMessage (msg);
    }
  }

  class RTTInbound extends MessageDelivererDelegateImplBase 
  {
    MessageDeliverer deliverer;

    public RTTInbound (MessageDeliverer deliverer)
    {
      super (deliverer);
      this.deliverer = deliverer;
    }

    public MessageAttributes deliverMessage (AttributedMessage msg, MessageAddress dest) throws MisdeliveredMessageException
    {
      //  RTTs not maintained for local messages

      if (MessageUtils.isLocalMessage (msg)) return super.deliverMessage (msg, dest);

      //  Record the receive time for the message.  Like with the send time above, kind
      //  of a hack, but need BBN support to have link protocols set the receive time
      //  at the time a message is actually received.

      long receiveTime = now();

      //  Record this latest message reception

      String sendNode = MessageUtils.getFromAgentNode (msg);
      String sendLink = MessageUtils.getSendProtocolLink (msg);
      long sendTime = MessageUtils.getMessageSendTime (msg);
      recordLatestMessageReception (sendNode, sendLink, sendTime, receiveTime);     

      //  Now we turn around and view this received message as completing a
      //  round trip from us to the node we got it from (now the destination
      //  node).  We take the "first half" roundtrip times in the message and
      //  make real RTTs out of them and save them.

      Vector v = (Vector) msg.getAttribute (RTT_ATTRIBUTE);

      if (v != null)
      {
        String destinationNode = sendNode;
        String receiveLink = convertSplitLinks (sendLink);

        for (Enumeration e=v.elements(); e.hasMoreElements(); )
        {
          RTTStart tripStart = (RTTStart) e.nextElement();
          sendLink = tripStart.getSendLink();
          int rtt = (int) (receiveTime - tripStart.getAdjustedSendTime());
          updateRTT (sendLink, destinationNode, receiveLink, rtt);
        }
      }

      //  Send the message on its way

      return deliverer.deliverMessage (msg, dest);
    }
  }

  private static void updateRTT (String sendLink, String node, String receiveLink, int rtt)
  {
    synchronized (roundtripTable)
    {
      String key = sendLink +"-"+ node;
      Hashtable table = (Hashtable) roundtripTable.get (key);

      if (table == null)
      {
        table = new Hashtable();
        roundtripTable.put (key, table);
      }

      key = receiveLink;
      RunningAverage avgRTT = (RunningAverage) table.get (key);

      if (avgRTT == null)
      {
        //      samplePoolsize:  number of latest samples avg is calc from
        //          startDelay:  throw away initial samples (which may be wildly off)
        //  percentChangeLimit:  limit the amount the latest sample can change avg
        //    changeLimitDelay:  how many samples before change limit kicks in

        avgRTT = new RunningAverage (samplePoolsize, startDelay, percentChangeLimit, changeLimitDelay);
        table.put (key, avgRTT);
      }

      if (rtt > 0) avgRTT.add (rtt);
/*
      System.err.println ("\nupdateRTT:"+
                          "\n  sendLink= "+sendLink+
                          "\n      node= "+node+
                          "\n  recvLink= "+receiveLink+
                          "\n       RTT= " +rtt+ " avg= " +avgRTT.getAverage()); 
*/
    }    
  }

  private static int getBestRTTForLink (DestinationLink link, String node)
  {
    synchronized (roundtripTable)
    {
      String sendLink = link.getProtocolClass().getName();
      String key = sendLink +"-"+ node;
      Hashtable table = (Hashtable) roundtripTable.get (key);

      if (table != null)
      {
        //  Go through the table and find the smallest non-zero roundtrip time

        int time, smallestTime = Integer.MAX_VALUE;
      
        for (Enumeration e=table.elements(); e.hasMoreElements(); )
        {
          RunningAverage avgRTT = (RunningAverage) e.nextElement();
          time = (int) avgRTT.getAverage();
          if (time > 0 && time < smallestTime) smallestTime = time;
        }

        if (smallestTime != Integer.MAX_VALUE) return smallestTime;
      }
      
      return 0;  // no rtt available
    }
  }

  private static long getLastRTTSampleTimestamp (DestinationLink link, String node)
  {
    synchronized (roundtripTable)
    {
      String sendLink = link.getProtocolClass().getName();
      String key = sendLink +"-"+ node;
      Hashtable table = (Hashtable) roundtripTable.get (key);

      long lastTimestamp = 0;

      if (table != null)
      {
        //  Go through the table and find the latest timestamp

        for (Enumeration e=table.elements(); e.hasMoreElements(); )
        {
          RunningAverage avgRTT = (RunningAverage) e.nextElement();
          long timestamp = avgRTT.getLastSampleTimestamp();
          if (timestamp > lastTimestamp) lastTimestamp = timestamp;
        }
      }
      
      return lastTimestamp;
    }
  }

  //  Utility classes and methods

  private static class TimeSort implements Comparator
  {
    public int compare (Object mt1, Object mt2)
    {
      if (mt1 == null)  // drive nulls to bottom (top is index 0)
      {
        if (mt2 == null) return 0;
        else return 1;
      }
      else if (mt2 == null) return -1;

      //  Sort on message receive time

      long t1 = ((MessageTimes) mt1).receiveTime;
      long t2 = ((MessageTimes) mt2).receiveTime;

      if (t1 == t2) return 0;
      return (t1 > t2 ? 1 : -1);  // later times to top
    }

    public boolean equals (Object obj)
    {
      return (this == obj);
    }
  }

  private String convertSplitLinks (String link)
  {
    //  Turn Outgoing links into Incoming ones (depends on link naming convention)

    int i = link.indexOf (".Outgoing");
    if (i > 0) return link.substring(0,i)+ ".Incoming" +link.substring(i+9);
    else return link;
  }

  private static long now ()
  {
    return System.currentTimeMillis();
  }
}
