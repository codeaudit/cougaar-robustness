/*
 * <copyright>
 *  Copyright 2002,2003,2004 Object Services and Consulting, Inc. (OBJS),
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
 * 23 Feb 2004: Ported to 11.0
 * 27 May 2003: Ported to 10.4 - rewrote WP access code (104B)
 * 04 Mar 2003: Ported to 10.2 - replaced TopologyService with WhitePagesService (OBJS)
 * 04 Jun 2002: Created. (OBJS)
 */

package org.cougaar.mts.std;

import java.net.URI; //102
import java.net.URISyntaxException; //104B
import java.util.Hashtable; //104B

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.wp.WhitePagesService; //102
import org.cougaar.core.service.wp.AddressEntry; //102
import org.cougaar.core.service.wp.Callback; //104B
import org.cougaar.core.service.wp.Request; //104B
import org.cougaar.core.service.wp.Response; //104B
import org.cougaar.util.log.Logger; //104B
import org.cougaar.util.log.Logging; //104B

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.mts.base.MessageTransportRegistryService;
import org.cougaar.mts.base.UnregisteredNameException;

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
  //private static String wpAgentName = null; //104B
  private static Hashtable nodeCbTbl= new Hashtable(); //104B
  private static Hashtable incCbTbl= new Hashtable(); //104B
  private static Object cbLock = new Object(); //104B

 //1045B
  private static int numWPservers;
  //private static Random randomNumGen;
  private static Hashtable wpAgentTbl = new Hashtable();

  static
  {
    //  Read external properties

    String s = "org.cougaar.message.transport.mts.AgentID.callTimeout";  //102
    callTimeout = Integer.valueOf(System.getProperty(s,"500")).intValue();

    //1045B
    s = "org.cougaar.message.transport.mts.AgentID.numberOfWPServers";  
    numWPservers = Integer.valueOf(System.getProperty(s,"12")).intValue();

    //1045B
    //randomnumgen = new Random(System.currentTimeMillis()); 
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
    throws UnregisteredNameException
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
    throws UnregisteredNameException
  {
    if (log == null) 
      log = Logging.getLogger(AgentID.class);

    if (log.isDebugEnabled())
      log.debug("agent="+agent);

    if (agent == null) {
        return null;
    }
    
    String node = null;
    String agentName = agent.getAddress();
    String incarnation = null;
    AddressEntry inc_ae = null;
    AddressEntry node_ae = null;
    CbTblEntry inc_cbte = null;
    CbTblEntry node_cbte = null;
    String inc_wp = null;
    String node_wp = null;
    String inc_cb = null;
    String node_cb = null;
    String node_registry = null;

    try {
      if (wp == null)
        wp = (WhitePagesService)sb.getService(requestor, 
                                              WhitePagesService.class, 
                                              null); //102

      //104B Temporary hack to handle messages to node agent where WP Server 
      //     resides without having to access the remote WP Server by sending
      //     it a message and therefore causing a deadlock
      //1045B Make this handle multiple WP servers
      synchronized (wpAgentTbl) {
	  if (log.isDebugEnabled())
	      log.debug("wpAgentTbl="+wpAgentTbl);
	  if (wpAgentTbl.isEmpty()) {
	      if (log.isDebugEnabled())
		  log.debug("wpAgentTbl is empty.");
	      AddressEntry ae = null;
	      //int wpServerNum = randomNumGen.nextInt(numWPservers)+1;
	      if (log.isDebugEnabled())
		  log.debug("numWPservers="+numWPservers);
	      int i;
	      for(i=1; i<numWPservers; i++) {
		  if (log.isDebugEnabled())
		      log.debug("i="+i);
		  String wpAgentName = null;
		  String alias = null;
		  if (log.isDebugEnabled())
		      log.debug("alias="+alias);
		  if (i == 1) {
		      if (log.isDebugEnabled())
			  log.debug("i="+i);
		      alias = "WP";
		      if (log.isDebugEnabled())
			  log.debug("alias="+alias);
		  } else {
		      if (log.isDebugEnabled())
			  log.debug("i="+i);
		      alias = "WP-" + i;
		      if (log.isDebugEnabled())
			  log.debug("alias="+alias);
		  }
		  ae = wp.get(alias, "alias", -1); // -1 = only try local cache
		  if (log.isDebugEnabled())
		      log.debug("ae="+ae);
		  if (ae != null && (ae.getURI() != null))
		      wpAgentName = ae.getURI().getPath().substring(1);
		  if (log.isDebugEnabled())
		      log.debug("wpAgentName="+wpAgentName);
		  if (wpAgentName == null) {
		      if (wpAgentTbl.isEmpty()) {
			  if (log.isDebugEnabled())
			      log.debug("WP alias not found yet. Will retry.");
			  return null;
		      }
		  } else {
		      wpAgentTbl.put(wpAgentName,wpAgentName);
		  }
	      }
	  }
      }
      if (wpAgentTbl.get(agentName) != null)
	  return new AgentID(agentName, agentName, "0");
      
      //104B First check via registry if node is local before going to WhitePages
      if (registry == null)
        registry = (MessageTransportRegistryService)sb.getService(requestor, 
                                                                  MessageTransportRegistryService.class,
                                                                  null);
      // if local agent, get node from registry
      if (registry.isLocalClient(agent))
          node_registry = registry.getIdentifier();

      // get inc from wp cache
      if (log.isDebugEnabled())
          log.debug("Calling wp.get("+agentName+","+VERSION+",-1)");
      inc_ae = wp.get(agentName, VERSION, -1); 
      if (log.isDebugEnabled())
          log.debug("inc_ae="+inc_ae);
      if ((inc_ae != null) && (inc_ae.getURI() != null)) {
          String path = inc_ae.getURI().getPath();
          int i = path.indexOf('/', 1);
          inc_wp = path.substring(1,i);
          if (log.isDebugEnabled())
	      log.debug("found incarnation in wp cache = "+inc_wp);
      }
      // get node from wp cache
      if (log.isDebugEnabled())
	  log.debug("Calling wp.get("+agentName+","+TOPOLOGY+",-1)");
      node_ae = wp.get(agentName, TOPOLOGY, -1); 
      if (log.isDebugEnabled())
	  log.debug("node_ae="+node_ae);
      if ((node_ae != null) && (node_ae.getURI() != null)) { //cache hit
	  node_wp = node_ae.getURI().getPath().substring(1);
	  if (log.isDebugEnabled())
	      log.debug("found node in wp cache = "+node_wp);
      }

      // get inc & node from callbacks
      synchronized (cbLock) {

	  // get the callback table entry for this agent's incarnation
	  inc_cbte = (CbTblEntry)incCbTbl.get(agentName);
	  // if none, create one (first lookup)
	  if (inc_cbte == null) {
	      inc_cbte = new CbTblEntry();
	      incCbTbl.put(agentName,inc_cbte);
	  }
	  //if (log.isDebugEnabled())
	  //    log.debug("inc_cbte="+inc_cbte+",incCbTbl="+incCbTbl);	  

	  // get the callback table entry for this agent's node
	  node_cbte = (CbTblEntry)nodeCbTbl.get(agentName);
	  // if none, create one (first lookup)
	  if (node_cbte == null) {
	      node_cbte = new CbTblEntry();
	      nodeCbTbl.put(agentName,node_cbte);
	  }
	  //if (log.isDebugEnabled())
	  //    log.debug("node_cbte="+node_cbte+",nodeCbTbl="+nodeCbTbl);

          // get inc from cbte
	  if (inc_cbte.result != null) {
	      String path = inc_cbte.result.getPath();
	      int i = path.indexOf('/', 1);
	      inc_cb = path.substring(1,i);
	      if (log.isDebugEnabled())
		  log.debug("found incarnation in cbte = "+inc_cb);
	  }
	  //get node from cbte   
	  if (node_cbte.result != null) {
	      node_cb = node_cbte.result.getPath().substring(1);
	      if (log.isDebugEnabled())
		  log.debug("found node in cbte = "+node_cb);
	  }

          // Decide which to use and what to do
          // Because you can tell which is newer from the inc,
          // use it to make decisions, and just use node from 
          // same source.  Sometimes, there won't be a node,
          // so fail in that case.
          if (inc_wp != null) {
	      if (inc_cb != null) {
		  if (inc_wp.equals(inc_cb)) {  // no change - use it
		      incarnation = inc_wp;
		      // favor wp over cb - arbitrary
                      node = ((node_registry != null) ? node_registry : ((node_wp != null) ? node_wp : node_cb)); 
		  } else {
		      long inc_wp_l = Long.parseLong(inc_wp);
		      long inc_cb_l = Long.parseLong(inc_cb);
		      if (inc_wp_l > inc_cb_l) {  // newer wp, update cb
			  incarnation = inc_wp;
			  inc_cbte.result = inc_ae.getURI();
			  node = ( (node_registry != null) ? node_registry : node_wp);
			  if (node_ae != null) node_cbte.result = node_ae.getURI();
                      } else {  //newer cb, doCallback
			  incarnation = inc_cb;
			  node = ( (node_registry != null) ? node_registry : node_cb);
			  //doCallback(incCbTbl, inc_cbte, agentName, VERSION);
			  //doCallback(nodeCbTbl, node_cbte, agentName, TOPOLOGY);
		      }
		  }
	      } else { // cb is null, use wp & update cb
		  incarnation = inc_wp;
		  inc_cbte.result = inc_ae.getURI();
		  node = ( (node_registry != null) ? node_registry : node_wp);
		  if (node_ae != null) node_cbte.result = node_ae.getURI();
	      }
	  } else {  // no inc from wp
	      if (inc_cb != null) { // use cb, and start a callback
		  incarnation = inc_cb;
		  node = ( (node_registry != null) ? node_registry : node_cb);
		  //doCallback(incCbTbl, inc_cbte, agentName, VERSION);
		  //doCallback(nodeCbTbl, node_cbte, agentName, TOPOLOGY);
	      } else { //nothing from nowhere, start callback
		  //doCallback(incCbTbl, inc_cbte, agentName, VERSION);
		  //doCallback(nodeCbTbl, node_cbte, agentName, TOPOLOGY);
	      }
	  }
      }
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
    if (node == null || incarnation == null) {
      if (incarnation == null && inc_cbte != null)
        doCallback(incCbTbl, inc_cbte, agentName, VERSION);
      if (node == null && node_cbte != null)
        doCallback(nodeCbTbl, node_cbte, agentName, TOPOLOGY);
      Exception e = new Exception ("Insufficient local information to create an AgentID for agent: " + agent); //104B
      throw new UnregisteredNameException(agent);
    }
    return new AgentID(node, agentName, incarnation); //102
  }

  // must be called inside synchronized code
  private static boolean doCallback (Hashtable tbl, CbTblEntry cbte, String name, String type) {
    if (cbte.pending) {
      return false;
    } else {
      cbte.pending = true;
      if (log.isDebugEnabled())	  
        log.debug("cbLock="+cbLock+",tbl="+tbl);
      AgentIDCallback cb = AgentIDCallback.getAgentIDCallback(cbLock, tbl);
      if (log.isDebugEnabled())
        log.debug("Calling wp.get("+name+","+type+","+cb+")");
      wp.get(name, type, cb);
      return true;
    }
  }

  /*
   * Remove entries from AgentID caches and start callback.
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
	  String inc_cb = path.substring(1,i);
          String inc_id = id.getAgentIncarnation();
	  if (!inc_cb.equals(inc_id)) {
	      long inc_cb_l = Long.parseLong(inc_cb);
	      long inc_id_l = Long.parseLong(inc_id);
	      if (inc_id_l > inc_cb_l) {
		  String s = "version:///" + inc_id + "/" + inc_id;
		  try {
		      inc_cbte.result = new URI(s);
		  } catch (URISyntaxException e) {
		      log.error("decache: error parsing " + s, e);
                      inc_cbte.result = null;
		  }
		  result = true;
		  //doCallback(incCbTbl, inc_cbte, agentName, VERSION);
	          // if inc changes, update node, too
		  CbTblEntry node_cbte = (CbTblEntry)nodeCbTbl.get(agentName);
		  if ((node_cbte != null) && (node_cbte.result != null)) {
		      String node_cb = node_cbte.result.getPath().substring(1);
		      if (!node_cb.equals(id.getNodeName())) {
			  s = "node://unknown/" + id.getNodeName();
			  try {
			      node_cbte.result = new URI(s);
			  } catch (URISyntaxException e) {
			      log.error("decache: error parsing " + s, e);
			      node_cbte.result = null;
			  }
			  //doCallback(nodeCbTbl, node_cbte, agentName, TOPOLOGY);
		      }
		  }
	      }
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
