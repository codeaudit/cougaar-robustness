/*
 * <copyright>
 *  Copyright 2002,2003 Object Services and Consulting, Inc. (OBJS),
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
 * 04 Mar 2003: Ported to 10.2 - replaced TopologyService with WhitePagesService (OBJS)
 * 04 Jun 2002: Created. (OBJS)
 */

package org.cougaar.core.mts;

import java.net.URI; //102

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.wp.WhitePagesService; //102
import org.cougaar.core.service.wp.AddressEntry; //102
import org.cougaar.core.service.wp.Application; //102
import org.cougaar.util.log.Logger; //102
import org.cougaar.util.log.Logging;

public class AgentID implements java.io.Serializable
{
  private static final int callTimeout;

  private static WhitePagesService wp; //102

  private String nodeName;
  private String agentName;
  private String agentIncarnation;

  static
  {
    //  Read external properties

    String s = "org.cougaar.message.transport.mts.AgentID.callTimeout";  //102
    callTimeout = Integer.valueOf(System.getProperty(s,"500")).intValue();
  }

  public AgentID (String nodeName, String agentName, String agentIncarnation)
  {
    this.nodeName = nodeName;
    this.agentName = agentName;
    this.agentIncarnation = agentIncarnation;
  }

  public AgentID (AgentID aid)
  {
    this.nodeName = aid.nodeName;
    this.agentName = aid.agentName;
    this.agentIncarnation = aid.agentIncarnation;
  }

  public String getNodeName ()
  {
    return nodeName;
  }

  public void setNodeName (String name)
  {
    nodeName = name;
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

  public String getNumberSequenceKey ()
  {
    return agentName+ "/" +agentIncarnation;
  }

  public static String makeAgentPairID (AgentID fromAgent, AgentID toAgent)
  {
    return fromAgent.getNumberSequenceKey() +"::"+ toAgent.getNumberSequenceKey();
  }

  public String getID ()
  {
    return nodeName+ "/" +agentName+ "/" +agentIncarnation;
  }

  public String getShortID ()
  {
    return nodeName+ "/" +agentName;
  }

  public String toString ()
  {
    return getID();
  }

  public String toShortString ()
  {
    return getShortID();
  }

  public static String makeAckingSequenceID (AgentID fromAgent, AgentID toAgent)
  {
    return fromAgent +"::"+ toAgent;
  }

  public static String makeShortSequenceID (AgentID fromAgent, AgentID toAgent)
  {
    return fromAgent.toShortString() +" to "+ toAgent.toShortString();
  }

  public boolean equals (Object obj)
  {
    if (!(obj instanceof AgentID)) return false;
    AgentID that = (AgentID) obj;
    if (!this.nodeName.equals         (that.nodeName))         return false;
    if (!this.agentName.equals        (that.agentName))        return false;
    if (!this.agentIncarnation.equals (that.agentIncarnation)) return false;
    return true;
  }

  public static AgentID getAgentID (Object requestor, ServiceBroker sb, MessageAddress agent) 
    throws NameLookupException
  {
    return getAgentID (requestor, sb, agent, false);    
  }

  public static AgentID getAgentID (Object requestor, ServiceBroker sb, MessageAddress agent, boolean refreshCache) 
    throws NameLookupException
  {
    if (agent == null) return null;

    String node = null; //102
    String agentName = agent.getAddress();
    String incarnation = null; //102

    try {
      if (wp == null) wp = (WhitePagesService)sb.getService(requestor, WhitePagesService.class, null); //102

      AddressEntry ae = wp.get(agentName, Application.getApplication("topology"), "node", callTimeout); //102
      URI uri = ae.getAddress();  //102
      node = uri.getPath().substring(1);  //102

      ae = wp.get(agentName, Application.getApplication("topology"), "version", callTimeout); //102
      uri = ae.getAddress(); //102
      String path = uri.getPath(); //102
      int i = path.indexOf('/', 1); //102
      incarnation = path.substring(1,i); //102

    } catch (WhitePagesService.TimeoutException te) { //102
      // timeout with no stale value available!
      Logger log = Logging.getLogger(AgentID.class); //102
      if (log.isDebugEnabled()) //102
        log.debug(stackTraceToString(te)); //102
    } catch (Exception e) {
      Logger log = Logging.getLogger(AgentID.class); //102
      if (log.isDebugEnabled()) //102
        log.debug(stackTraceToString(e)); //102
    } finally {
        sb.releaseService(requestor, WhitePagesService.class, wp);
    }
    if (node == null || incarnation ==null) {
      Exception e = new Exception ("WhitePagesService has no entry for agent: " + agent); //102
      throw new NameLookupException(e);
    }
    return new AgentID(node, agentName, incarnation); //102
  }

  private static String stackTraceToString (Exception e)
  {
    java.io.StringWriter stringWriter = new java.io.StringWriter();
    java.io.PrintWriter printWriter = new java.io.PrintWriter (stringWriter);
    e.printStackTrace (printWriter);
    return stringWriter.getBuffer().toString();
  }
}
