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
 * 23 Apr  2001: Split out from MessageAckingAspect. (OBJS)
 */

package org.cougaar.core.mts.acking;

import java.io.*;
import java.util.*;

import org.cougaar.core.mts.*;

/**
 **  Container of ack info.
**/

public class Ack implements Serializable
{
  private Vector specificAcks;
  private Vector latestAcks;
  private Classname lastReceiveLink;
  private Classname sendLink;
  private Classname predictedReceiveLink;
  private int senderRoundtripTime;
  private int receiverRoundtripTime;
  private int resendMultiplier;
  private long sendTime;
  private int sendCount;

  private transient AttributedMessage msg;
  private transient Vector linksUsed;
  private transient long receiveTime;

  public Ack (AttributedMessage msg)
  {
    setMsg (msg);
    sendCount = 0;
    linksUsed = new Vector();
  }

  void setMsg (AttributedMessage msg)
  {
    this.msg = msg;
  }

  public AttributedMessage getMsg ()
  {
    return msg;
  }

  void setSpecificAcks (Vector acks)
  {
    specificAcks = acks;
  }

  public Vector getSpecificAcks ()
  {
    return specificAcks;      
  }

  void setLatestAcks (Vector acks)
  {
    latestAcks = acks;
  }

  public Vector getLatestAcks ()
  {
    return latestAcks;      
  }

  public void setLastReceiveLink (Class link)
  {
    setLastReceiveLink (new Classname (link));
  }

  public void setLastReceiveLink (Classname link)
  {
    lastReceiveLink = link;
  }

  public Classname getLastReceiveLink ()
  {
    return lastReceiveLink;
  }

  public void setSendLink (Class link)
  {
    setSendLink (new Classname (link));
  }

  public void setSendLink (Classname link)
  {
    sendLink = link;
  }

  public Classname getSendLink ()
  {
    return sendLink;
  }

  public void setPredictedReceiveLink (Class link)
  {
    setPredictedReceiveLink (new Classname (link));
  }

  public void setPredictedReceiveLink (Classname link)
  {
    predictedReceiveLink = link;
  }

  public Classname getPredictedReceiveLink ()
  {
    return predictedReceiveLink;
  }

  public void setSenderRoundtripTime (int time)
  {
    senderRoundtripTime = time;
  }

  public int getSenderRoundtripTime ()
  {
    return senderRoundtripTime;
  }

  public void setReceiverRoundtripTime (int time)
  {
    receiverRoundtripTime = time;
  }

  public int getReceiverRoundtripTime ()
  {
    return receiverRoundtripTime;
  }

  public void setResendMultiplier (int x)
  {
    resendMultiplier = x;
  }

  public int getAckWindowUnitTimeSlot ()
  {
    // return (senderRoundtripTime + receiverRoundtripTime);
    return Math.max (senderRoundtripTime, receiverRoundtripTime);
  }

  public int getResendMultiplier ()
  {
    return resendMultiplier;
  }

  public int getResendAckWindow ()
  {
    return getAckWindowUnitTimeSlot() * resendMultiplier;
  }

  public void setSendTime (long time)
  {
    sendTime = time;
  }

  public long getSendTime ()
  {
    return sendTime;
  }

  public int getSendCount ()
  {
    return sendCount;
  }

  public void incrementSendCount ()
  {
    sendCount++;
  }

  public void decrementSendCount ()
  {
    sendCount--;
  }

  public boolean haveLinkSelection (DestinationLink link)
  {
    synchronized (linksUsed)
    {
      //  Links are not directly comparable!!!
      //  Must use protocol class to compare them.

      Class protocolClass = link.getProtocolClass();

      for (Enumeration e=linksUsed.elements(); e.hasMoreElements(); )
      {
        Class usedLinkClass = (Class) e.nextElement();
        if (usedLinkClass == protocolClass) return true;
      }

      return false;
    }
  }

  public boolean addLinkSelection (DestinationLink link)
  {
    synchronized (linksUsed)
    {
      //  We only add it if we don't already have it

      boolean addit = (haveLinkSelection (link) == false);
      if (addit) linksUsed.add (link.getProtocolClass());
      return addit;
    }
  }

  public Class getFirstLinkSelection ()
  {
    synchronized (linksUsed)
    {
      return (Class) linksUsed.firstElement();
    }
  }

  public void clearLinkSelections ()
  {
    synchronized (linksUsed)
    {            
      linksUsed.clear();
    }
  }

  public void setReceiveTime (long time)
  {
    receiveTime = time;
  }

  public long getReceiveTime ()
  {
    return receiveTime;
  }

  public boolean isRegularAck ()
  {
    return (getClass() == org.cougaar.core.mts.acking.Ack.class);
  }

  public boolean isPureAck ()
  {
    return (getClass() == org.cougaar.core.mts.acking.PureAck.class);
  }

  public boolean isPureAckAck ()
  {
    return (getClass() == org.cougaar.core.mts.acking.PureAckAck.class);
  }

  public String getType ()
  {
    if (isRegularAck()) return "RegularAck";
    if (isPureAck())    return "PureAck";
    if (isPureAckAck()) return "PureAckAck";
    return                     "Unknown";
  }
    
  public String toString ()
  {
    return getType() + " in " + MessageUtils.toString (getMsg());
  }
}
