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

/*
 * Class contains descriptive information about an agent. getAgentID() is used to
 * instantiate objects from MessageAddress objects. Uses the WhitePages to populate data
 * slots about the agent.
 */
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

  /*
   * Create an AgentID instance
   * @param nodeName node
   * @param agentName agent name
   * @param agentIncarnation incarnation # of agent
   */
  public AgentID (String nodeName, String agentName, String agentIncarnation)
  {
    this.nodeName = nodeName;
    this.agentName = agentName;
    this.agentIncarnation = agentIncarnation;
  }

  /*
   * Creates a new AgentID from another one
   * @param aid AgentID instance
   */
  public AgentID (AgentID aid)
  {
    this.nodeName = aid.nodeName;
    this.agentName = aid.agentName;
    this.agentIncarnation = aid.agentIncarnation;
  }

  /*
   * @return node name
   */
  public String getNodeName ()
  {
    return nodeName;
  }

  /*
   * Set node name
   * @param name node name
   */
  public void setNodeName (String name)
  {
    nodeName = name;
  }

  /*
   * @return agent name
   */
  public String getAgentName ()
  {
    return agentName;
  }

  /*
   * @return agent incarnation #
   */
  public String getAgentIncarnation ()
  {
    return agentIncarnation;
  }

  /*
   * @return agent incarnation # as a long value
   */
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

  /*
   * Generates Sequence number key
   * @return (agent name / incarnation #)
   */
  public String getNumberSequenceKey ()
  {
    return agentName+ "/" +agentIncarnation;
  }

  /*
   * Generates paired number sequence key for the two provided agents
   * @param fromAgent from agent
   * @param toAgent to agent
   * @return agent pair ID (fromAgentName/fromAgentIncarnation::toAgentName/toAgentIncaration)
   */
  public static String makeAgentPairID (AgentID fromAgent, AgentID toAgent)
  {
    return fromAgent.getNumberSequenceKey() +"::"+ toAgent.getNumberSequenceKey();
  }

  /*
   * @return (node name / agent name / incarnation #)
   */
  public String getID ()
  {
    return nodeName+ "/" +agentName+ "/" +agentIncarnation;
  }

  /*
   * @return (node name / agent name)
   */
  public String getShortID ()
  {
    return nodeName+ "/" +agentName;
  }

  /*
   * @return the value from getID()
   */
  public String toString ()
  {
    return getID();
  }

  /*
   * @return the value from getShortID()
   */
  public String toShortString ()
  {
    return getShortID();
  }

  /*
   * Generate acking seq id
   * @param fromAgent from agent
   * @param toAgent to Agent
   * @return acking sequence ID (fromAgent::toAgent)
   */
  public static String makeAckingSequenceID (AgentID fromAgent, AgentID toAgent)
  {
    return fromAgent +"::"+ toAgent;
  }

  /*
   * Generate acking seq id
   * @param fromAgent from agent
   * @param toAgent to Agent
   * @return short sequence ID (fromAgent to toAgent)
   */
  public static String makeShortSequenceID (AgentID fromAgent, AgentID toAgent)
  {
    return fromAgent.toShortString() +" to "+ toAgent.toShortString();
  }

  /*
   * @param object ANother AgentID
   * @return TRUE if objects are equal (nodename, agentname & incarnation #)
   */
  public boolean equals (Object obj)
  {
    if (!(obj instanceof AgentID)) { 
        return false;
        
    }
    AgentID that = (AgentID) obj;
    if (!this.nodeName.equals         (that.nodeName)) {
        return false;
    }
    
    if (!this.agentName.equals        (that.agentName)) {
        return false;
    }
    
    if (!this.agentIncarnation.equals (that.agentIncarnation)) {
        return false;
    }
    
    return true;
  }

  /*
   * Creates an AgentID instance
   * @param requestor Requesting object
   * @param sb ServiceBroker
   * @param agent MessageAddress of the agent
   * @return new AgentID instance
   */
  public static AgentID getAgentID (Object requestor, ServiceBroker sb, MessageAddress agent) 
    throws NameLookupException
  {
    return getAgentID (requestor, sb, agent, false);    
  }

  /*
   * Creates an AgentID instance
   * @param requestor Requesting object
   * @param sb ServiceBroker
   * @param agent MessageAddress of the agent
   * @param refreshCache Does nothing now
   * @return new AgentID instance
   */
  public static AgentID getAgentID (Object requestor, ServiceBroker sb, MessageAddress agent, boolean refreshCache) 
    throws NameLookupException
  {
    if (agent == null) {
        return null;
    }
    
    String node = null; //102
    String agentName = agent.getAddress();
    String incarnation = null; //102

    try {
      if (wp == null) {
          wp = (WhitePagesService)sb.getService(requestor, WhitePagesService.class, null); //102
      }
      
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
      if (log.isDebugEnabled()) { //102
        log.debug(stackTraceToString(te)); //102
      }
    } catch (Exception e) {
      Logger log = Logging.getLogger(AgentID.class); //102
      if (log.isDebugEnabled()) { //102
        log.debug(stackTraceToString(e)); //102
      }
    } finally {
        sb.releaseService(requestor, WhitePagesService.class, wp);
    }
    if (node == null || incarnation ==null) {
      Exception e = new Exception ("WhitePagesService has no entry for agent: " + agent); //102
      throw new NameLookupException(e);
    }
    return new AgentID(node, agentName, incarnation); //102
  }

  /*
   * Converts an exception's stacktrace to a string
   * @param Exception the exception
   * @return String containing stacktrace
   */
  private static String stackTraceToString (Exception e)
  {
    java.io.StringWriter stringWriter = new java.io.StringWriter();
    java.io.PrintWriter printWriter = new java.io.PrintWriter (stringWriter);
    e.printStackTrace (printWriter);
    return stringWriter.getBuffer().toString();
  }
}
