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
 * 09 Jun 2003: Added removeLinkSelection in support of in-band White Pages access.  (104B)
 * 05 Aug 2002: Revamped in light of new RTT service. (OBJS)
 * 23 Apr 2002: Split out from MessageAckingAspect. (OBJS)
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
  private static final int DEFAULT_MAX_RESEND_DELAY = 60*1000;

  private long sendTime;
  private String sendLink;
  private int sendCount;
  private Vector specificAcks;
  private Vector latestAcks;
  private int roundTripTime;
  private int resendMultiplier;

  private transient int sendTry;
  private transient int numLinkChoices;
  private transient int resendDelay;
  private transient int maxResendDelay;
  private transient AttributedMessage msg;
  private transient Vector linksUsed;
  private transient long receiveTime;

  public Ack ()  // for deserialization only
  {
    maxResendDelay = DEFAULT_MAX_RESEND_DELAY;
  }

  public Ack (AttributedMessage m)
  {
    maxResendDelay = DEFAULT_MAX_RESEND_DELAY;
    msg = m;
    linksUsed = new Vector();
  }

  protected Ack (Ack ack, AttributedMessage m)
  {
    sendTime = ack.sendTime;
    sendLink = ack.sendLink;
    sendCount = ack.sendCount;
    specificAcks = cloneAcks (ack.specificAcks);
    latestAcks = cloneAcks (ack.latestAcks);
    roundTripTime = ack.roundTripTime;
    resendMultiplier = ack.resendMultiplier;

    sendTry = ack.sendTry;
    numLinkChoices = ack.numLinkChoices;
    resendDelay = ack.resendDelay;
    maxResendDelay = ack.maxResendDelay;
    msg = m;
    linksUsed = cloneLinksUsed (ack.linksUsed);
    receiveTime = ack.receiveTime;
  }

  public static Ack clone (Ack ack, AttributedMessage msg)
  {
         if (ack.isAck())        return new Ack (ack, msg);
    else if (ack.isPureAck())    return new PureAck ((PureAck)ack, (PureAckMessage)msg);
    else if (ack.isPureAckAck()) return new PureAckAck ((PureAckAck)ack, (PureAckAckMessage)msg);
    else                         return null;
  }

  private Vector cloneAcks (Vector acks)
  {
    if (acks == null) return null;
    Vector v = new Vector();
    v.addAll (acks);
    return v;
  }

  private Vector cloneLinksUsed (Vector linksUsed)
  {
    if (linksUsed == null) return new Vector();

    synchronized (linksUsed)
    {
      Vector v = new Vector();
      v.addAll (linksUsed);
      return v;
    }
  }

  public synchronized void setSendTime (long time)
  {
    sendTime = time;
  }

  public synchronized long getSendTime ()
  {
    return sendTime;
  }

  public void setSendLink (String link)
  {
    sendLink = link;
  }

  public String getSendLink ()
  {
    return sendLink;
  }

  public int getSendTry ()
  {
    return sendTry;
  }

  public void incrementSendTry ()
  {
    sendTry++;
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

  void setMsg (AttributedMessage msg)
  {
    this.msg = msg;
  }

  public AttributedMessage getMsg ()
  {
    return msg;
  }

  public void setRTT (int rtt)
  {
    roundTripTime = rtt;
  }

  public int getRTT ()
  {
    return roundTripTime;
  }

  public int getAckWindow ()
  {
    return getRTT();
  }

  public void setResendMultiplier (int x)
  {
    resendMultiplier = x;
  }

  public int getResendMultiplier ()
  {
    return resendMultiplier;
  }

  public int getResendTimeout ()
  {
    return (getAckWindow() * resendMultiplier);
  }

  public void setMaxResendDelay (int max)
  {
    maxResendDelay = max;
  }

  public int getMaxResendDelay ()
  {
    return maxResendDelay;
  }

  public int getResendDelay ()
  {
    return resendDelay;
  }

  public void addResendDelay (int delay)
  {
    int newDelay = resendDelay + delay;
    if (newDelay < 0) newDelay = 0;
    if (newDelay > maxResendDelay) newDelay = maxResendDelay;
    resendDelay = newDelay;  // atomic update w/ valid value
  }

  public void setNumberOfLinkChoices (int n)
  {
    numLinkChoices = n;
  }

  public int getNumberOfLinkChoices ()
  {
    return numLinkChoices;
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

  //104B
  public void removeLinkSelection (DestinationLink link)
  {
    synchronized (linksUsed)
    {
      if (haveLinkSelection(link))
        linksUsed.remove(link.getProtocolClass());
    }
  }

  public Class getFirstLinkSelection ()
  {
    synchronized (linksUsed)
    {
      return (Class) linksUsed.firstElement();
    }
  }

  public int getNumberOfLinkSelections ()
  {
    synchronized (linksUsed)
    {
      return linksUsed.size();
    }
  }

  public void clearLinkSelections ()
  {
    synchronized (linksUsed)
    {            
      linksUsed.clear();
    }
  }

  public synchronized void setReceiveTime (long time)
  {
    receiveTime = time;
  }

  public synchronized long getReceiveTime ()
  {
    return receiveTime;
  }

  public boolean isAck ()
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

  public boolean isSomePureAck ()
  {
    return (isPureAck() || isPureAckAck());
  }

  public String getType ()
  {
    if (isAck())        return "Ack";
    if (isPureAck())    return "PureAck";
    if (isPureAckAck()) return "PureAckAck";
    return                     "<unknown!>Ack";
  }
    
  public String toString ()
  {
    return getType() + " in " + MessageUtils.toString (getMsg());
  }
}
