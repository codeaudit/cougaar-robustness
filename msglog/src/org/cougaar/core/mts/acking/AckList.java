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
 * 29 May 2002: Created. (OBJS)
 */

package org.cougaar.core.mts.acking;

import java.io.*;
import java.util.*;

import org.cougaar.core.mts.*;


public class AckList extends NumberList
{
  private AgentID fromAgent;
  private AgentID toAgent;

  private transient String sendLinkType;
  private transient String receiveLinkType;
  private transient long receiveTime;

  public AckList (AgentID fromAgent, AgentID toAgent)
  {
    this.fromAgent = fromAgent;
    this.toAgent = toAgent;
  }

  public AgentID getFromAgent ()
  {
    return fromAgent;
  }

  public AgentID getToAgent ()
  {
    return toAgent;
  }

  public String getPairKey ()
  {
    return AgentID.makePairKey (fromAgent, toAgent);
  }

  public void setSendLinkType (String sendLinkType)
  {
    this.sendLinkType = receiveLinkType;
  }

  public String getSendLinkType ()
  {
    return receiveLinkType;
  }

  public void setReceiveLinkType (String receiveLinkType)
  {
    this.receiveLinkType = receiveLinkType;
  }

  public String getReceiveLinkType ()
  {
    return receiveLinkType;
  }

  public void setReceiveTime (long time)
  {
    receiveTime = time;
  }

  public long getReceiveTime ()
  {
    return receiveTime;
  }
    
  public String toString ()
  {
    return "from: " +fromAgent+ " to: " +toAgent+ " acks: " +super.toString();
  }

  //  Utility methods

  public static boolean find (Vector acks, int msgNum)
  {
    for (Enumeration a=acks.elements(); a.hasMoreElements(); )
    {
      AckList ackList = (AckList) a.nextElement();
      if (ackList.find (msgNum)) return true;
    }

    return false;
  }

  public static AckList findFirst (Vector acks, int msgNum)
  {
    //  Ack lists are added to the vector on the end, so the
    //  earlist list will be the first.

    for (Enumeration a=acks.elements(); a.hasMoreElements(); )
    {
      AckList ackList = (AckList) a.nextElement();
      if (ackList.find (msgNum)) return ackList;
    }

    return null;
  }

  public static void printAcks (Vector acks)
  {
    for (Enumeration a=acks.elements(); a.hasMoreElements(); )
    {
      System.err.println ((AckList) a.nextElement());
    }
  }
}
