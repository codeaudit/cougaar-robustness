/*
 * <copyright>
 *  Copyright 2002-2003 Object Services and Consulting, Inc. (OBJS),
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
 * 20 Mar 2003: Commented out printAcks(String,Vector). (OBJS)
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

  public AckList (AgentID fromAgent, AgentID toAgent)
  {
    this.fromAgent = fromAgent;
    this.toAgent = toAgent;
  }

  public AckList (AckList original)
  {
    super (original);
    this.fromAgent = original.getFromAgent();
    this.toAgent = original.getToAgent();
  }

  public AgentID getFromAgent ()
  {
    return fromAgent;
  }

  public AgentID getToAgent ()
  {
    return toAgent;
  }

  public String getAckingSequenceID ()
  {
    return AgentID.makeAckingSequenceID (fromAgent, toAgent);
  }

  public String toString ()
  {
    return super.toString();
  }

  public String toStringFull ()
  {
    return "acks: " +super.toString()+ " sequence: " + getAckingSequenceID();
  }

  //  Utility methods

  public static boolean find (Vector acks, int msgNum)
  {
    if (acks == null) return false;

    for (Enumeration a=acks.elements(); a.hasMoreElements(); )
    {
      AckList ackList = (AckList) a.nextElement();
      if (ackList.find (msgNum)) return true;
    }

    return false;
  }

  public static AckList findFirst (Vector acks, int msgNum)
  {
    if (acks == null) return null;

    //  Ack lists are added to the vector on the end, so the
    //  earlist list will be the first.

    for (Enumeration a=acks.elements(); a.hasMoreElements(); )
    {
      AckList ackList = (AckList) a.nextElement();
      if (ackList.find (msgNum)) return ackList;
    }

    return null;
  }
 
/* //102B
  public static void printAcks (String tag, Vector acks)
  {
    StringBuffer buf = new StringBuffer();
    printAcks (buf, tag, acks);
    System.err.print (buf.toString());
  }
*/

  public static void printAcks (StringBuffer buf, String tag, Vector acks)
  {
    if (acks == null) return;

    if (tag == null) tag = "";
    String blank = blankString (tag.length());
    boolean firstTime = true;

    for (Enumeration a=acks.elements(); a.hasMoreElements(); )
    {
      String marker = (firstTime ? tag : blank);  firstTime = false;
      buf.append ("  " +marker+ " " +((AckList)a.nextElement()).toStringFull()+ "\n");
    }
  }

  private static String blankString (int len)
  {
    StringBuffer buf = new StringBuffer (len);
    for (int i=0; i<len; i++) buf.append (" ");
    return buf.toString();
  }
}
