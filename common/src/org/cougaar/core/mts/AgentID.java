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
 * 27 May 2003: Ported to 10.4 - several changes to WP access (104B)
 * 04 Mar 2003: Ported to 10.2 - replaced TopologyService with WhitePagesService (OBJS)
 * 04 Jun 2002: Created. (OBJS)
 */

package org.cougaar.core.mts;

import java.net.URI; //102
import java.util.Hashtable; //104B

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.wp.WhitePagesService; //102
import org.cougaar.core.service.wp.AddressEntry; //102
import org.cougaar.core.service.wp.Callback; //104B
import org.cougaar.core.service.wp.Request; //104B
import org.cougaar.core.service.wp.Response; //104B
import org.cougaar.util.log.Logger; //104B
import org.cougaar.util.log.Logging; //104B

/*
 * Class contains descriptive information about an agent. getAgentID() is used to
 * instantiate objects from MessageAddress objects. Uses the WhitePages to populate data
 * slots about the agent.
 */
public class AgentID implements java.io.Serializable
{
  private static final int callTimeout;
  private static final String TOPOLOGY = "topology";
  private static final String VERSION = "version";

  private static WhitePagesService wp; //102
  private static MessageTransportRegistryService registry; //104B
  private static Logger log; //104B

  private String nodeName;
  private String agentName;
  private String agentIncarnation;
  private static String wpAgentName = null; //104B
  private static Hashtable nodeCbTbl= new Hashtable(); //104B
  private static Hashtable incCbTbl= new Hashtable(); //104B
  private static Object cbLock = new Object(); //104B

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
  public static AgentID getAgentID (Object requestor, ServiceBroker sb, 
                                    MessageAddress agent) 
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
  public static AgentID getAgentID (Object requestor, ServiceBroker sb, 
                                    MessageAddress agent, boolean refreshCache) 
    throws NameLookupException
  {
    if (log == null) 
      log = Logging.getLogger(AgentID.class);

    if (log.isDebugEnabled())
      log.debug("agent="+agent);

    if (agent == null) {
        return null;
    }
    
    String node = null; //102
    String agentName = agent.getAddress();
    String incarnation = null; //102

    try {
      if (wp == null) {
        wp = (WhitePagesService)sb.getService(requestor, 
                                              WhitePagesService.class, 
                                              null); //102
      }

      //104B Temporary hack to handle messages to node agent where WP Server 
      //     resides without having to access the remote WP Server by sending
      //     it a message and therefore causing a deadlock
      if (wpAgentName == null) {
        AddressEntry ae = wp.get("WP", "alias", -1); // -1 = only try local cache
        if (log.isDebugEnabled())
          log.debug("ae="+ae);
        wpAgentName = ae.getURI().getPath().substring(1);
        if (log.isDebugEnabled())
          log.debug("wpAgentName="+wpAgentName);
      }
      if (wpAgentName.equals(agentName))
        return new AgentID(agentName, agentName, "0");      
      
      //104B First check via registry if node is local before going to WhitePages
      if (registry == null)
        registry = (MessageTransportRegistryService)sb.getService(requestor, 
                                                                  MessageTransportRegistryService.class,
                                                                  null);
/* do I really need this?
      // short sleep just to give callbacks an opening
      if (log.isDebugEnabled()) log.debug("sleeping for 1/10 second");
      try {
        Thread.sleep(100);  
      } catch (Exception e){}
*/
      //104B all new code until end synchronized
      //104B converted WP access to callbacks 
//    synchronized (cbLock) {

      CbTblEntry node_cbte = null;
      CbTblEntry inc_cbte = null;

      // get node

      // if local agent, get node from registry instead of WP
      if (registry.isLocalClient(agent)) {
          node = registry.getIdentifier();

      } else {

	  synchronized (cbLock) {

	      // get the callback table entry for this agent's node
	      node_cbte = (CbTblEntry)nodeCbTbl.get(agentName);

	      // if none, create one (first lookup)
	      if (node_cbte == null) {
		  node_cbte = new CbTblEntry();
		  nodeCbTbl.put(agentName,node_cbte);
	      }

	      if (log.isDebugEnabled())
		  log.debug("node_cbte="+node_cbte+",nodeCbTbl="+nodeCbTbl);
	  }
 
          // check the cache first
          if (log.isDebugEnabled())
	      log.debug("Calling wp.get("+agentName+","+TOPOLOGY+",-1)");
          AddressEntry ae = wp.get(agentName, TOPOLOGY, -1); 
          if (log.isDebugEnabled())
	      log.debug("ae="+ae);
          if ((ae != null) && (ae.getURI() != null)) { //cache hit
	      node = ae.getURI().getPath().substring(1);
	      if (log.isDebugEnabled())
		  log.debug("found node="+node);
	      
	      // else, check the callback
          } else {
	      synchronized (cbLock) {
		  if (node_cbte.result != null) {
		      node = node_cbte.result.getPath().substring(1);
		      if (log.isDebugEnabled())
			  log.debug("found node="+node);
		      
		      // else, start a callback
		  } else if (!node_cbte.pending) {
		      node_cbte.pending = true;
		      if (log.isDebugEnabled())
			  log.debug("cbLock="+cbLock+",nodeCbTbl="+nodeCbTbl);
		      AgentIDCallback nodeCb = AgentIDCallback.getAgentIDCallback(cbLock, nodeCbTbl);
		      if (log.isDebugEnabled())
			  log.debug("Calling wp.get("+agentName+","+TOPOLOGY+","+nodeCb+")");
		      wp.get(agentName, TOPOLOGY, nodeCb);
	         
		      // else, callback is pending, so do nothing
		  }
	      }
	  }
      }
      
      // get incarnation

      synchronized (cbLock) {
	  // get the callback table entry for this agent's incarnation
	  inc_cbte = (CbTblEntry)incCbTbl.get(agentName);
	  
	  // if none, create one (first lookup)
	  if (inc_cbte == null) {
	      inc_cbte = new CbTblEntry();
	      incCbTbl.put(agentName,inc_cbte);
	  }
	  
	  if (log.isDebugEnabled())
	      log.debug("inc_cbte="+inc_cbte+",incCbTbl="+incCbTbl);
      }

      // check the cache first
      if (log.isDebugEnabled())
          log.debug("Calling wp.get("+agentName+","+VERSION+",-1)");
      AddressEntry ae = wp.get(agentName, VERSION, -1); 
      if (log.isDebugEnabled())
          log.debug("ae="+ae);
      if ((ae != null) && (ae.getURI() != null)) {
          String path = ae.getURI().getPath();
          int i = path.indexOf('/', 1);
          incarnation = path.substring(1,i);
          if (log.isDebugEnabled())
	      log.debug("found incarnation="+incarnation);

	  // else, check the callback
      } else {
	  synchronized (cbLock) {
	      if (inc_cbte.result != null) {
		  String path = inc_cbte.result.getPath();
		  int i = path.indexOf('/', 1);
		  incarnation = path.substring(1,i);
		  if (log.isDebugEnabled())
		      log.debug("found incarnation="+incarnation);

		  // else, start a callback
	      } else if (!inc_cbte.pending) {
		  inc_cbte.pending = true;
		  if (log.isDebugEnabled())
		      log.debug("cbLock="+cbLock+",incCbTbl="+incCbTbl);
		  AgentIDCallback incCb = AgentIDCallback.getAgentIDCallback(cbLock, incCbTbl);
		  if (log.isDebugEnabled())
		      log.debug("Calling wp.get("+agentName+","+VERSION+","+incCb+")");
		  wp.get(agentName, VERSION, incCb);
		  
		  // else, callback is pending, so do nothing
	      } 
	  }
      }

      // if we got both node & inc, then clear both results 
      // from callback tables, so that next time through
      // it will force a new lookup
/* *********************************************************** BEWARE: Commenting this out might cause problems later ********
        if ((node != null) && (incarnation != null)) {
	  if (node_cbte != null) {
            node_cbte.pending = false;
            node_cbte.result = null;
	  }
	  if (inc_cbte != null) {
            inc_cbte.pending = false;
            inc_cbte.result = null;
	  }
        }
*/

//      } //104B end synchronized 

    } catch (WhitePagesService.TimeoutException te) { //102
      // timeout with no stale value available!
      if (log.isDebugEnabled()) { //102
        log.debug(stackTraceToString(te)); //102
      }
    } catch (Exception e) {
      if (log.isDebugEnabled()) { //102
        log.debug(stackTraceToString(e)); //102
      }
    } finally {
        //104B Do I really want to release the service after each use?
        //104B sb.releaseService(requestor, WhitePagesService.class, wp);
    }
    if (node == null || incarnation ==null) {
      Exception e = new Exception ("WhitePagesService has no entry for agent: " + agent); //102
      throw new NameLookupException(e);
    }
    return new AgentID(node, agentName, incarnation); //102
  }

   /*
   * Remove entries from AgentID caches.
   * @return boolean if anything is decached
   */
   public synchronized static boolean decache (AgentID id) {
    boolean result = false;
    String agentName = id.getAgentName();
    // check inc
    CbTblEntry inc_cbte = (CbTblEntry)incCbTbl.get(agentName);
    if ((inc_cbte != null) && (inc_cbte.result != null)) {
      String path = inc_cbte.result.getPath();
      int i = path.indexOf('/', 1);
      String inc = path.substring(1,i);
      if (!inc.equals(id.getAgentIncarnation())) {
        inc_cbte.result = null;
        result = true;
      }
    }
    // check inc
    CbTblEntry node_cbte = (CbTblEntry)nodeCbTbl.get(agentName);
    if ((node_cbte != null) && (node_cbte.result != null)) {
      String node = node_cbte.result.getPath().substring(1);
      if (!node.equals(id.getNodeName())) {
        node_cbte.result = null;
        result = true;
      }
    }
    return result;
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
