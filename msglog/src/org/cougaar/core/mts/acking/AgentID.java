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
 * 04 Jun 2002: Created. (OBJS)
 */

package org.cougaar.core.mts.acking;


public class AgentID implements java.io.Serializable
{
  private String nodeName;
  private String agentName;
  private String agentIncarnation;

  public AgentID (String nodeName, String agentName, String agentIncarnation)
  {
    this.nodeName = nodeName;
    this.agentName = agentName;
    this.agentIncarnation = agentIncarnation;
  }

  public String getNodeName ()
  {
    return nodeName;
  }

  public String getAgentName ()
  {
    return agentName;
  }

  public String getAgentIncarnation ()
  {
    return agentIncarnation;
  }

  public long getAgentIncarnationAsLong ()
  {
    try
    {
      return Long.parseLong (agentIncarnation);
    }
    catch (Exception e)
    {
      return -1;
    }
  }

  public String getID ()
  {
    return nodeName+ "/" +agentName+ "/" +agentIncarnation;
  }

  public String toString ()
  {
    return getID();
  }

  public static String makeSequenceID (AgentID fromAgent, AgentID toAgent)
  {
    return fromAgent +"::"+ toAgent;
  }
}
