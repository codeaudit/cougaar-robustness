/*
 * <copyright>
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
 */
package org.cougaar.tools.robustness.ma.plugins;

import org.cougaar.tools.robustness.ma.ldm.VacateRequest;
import org.cougaar.tools.robustness.ma.ldm.VacateRequestRelay;
import org.cougaar.tools.robustness.ma.ldm.RestartLocationRequest;
import java.util.*;

import org.cougaar.core.blackboard.IncrementalSubscription;
//import org.cougaar.core.plugin.SimplePlugin;
import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.TopologyReaderService;
import org.cougaar.core.service.TopologyEntry;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.NamingService;
import org.cougaar.core.service.community.*;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.SimpleMessageAddress;
//import org.cougaar.core.agent.ClusterIdentifier;

import org.cougaar.core.mobility.AbstractTicket;
import org.cougaar.core.mobility.MoveTicket;
import org.cougaar.core.mobility.ldm.MobilityFactory;
import org.cougaar.core.mobility.ldm.AgentControl;

import org.cougaar.util.UnaryPredicate;

import org.cougaar.util.CougaarEvent;
import org.cougaar.util.CougaarEventType;

import org.cougaar.core.util.UID;


/**
 * This plugin moves all community members from a specified node or host to
 * another node/host when it receives a VacateRequest.
 */
public class VacatePlugin extends SimplePlugin {

  private LoggingService log;
  private TopologyReaderService trs;
  private BlackboardService bbs = null;
  private CommunityService cs;
  private MobilityFactory mobilityFactory;

  // My unique ID
  UID myUID;

  // Defines default values for configurable parameters.
  private static String defaultParams[][] = new String[0][0];

  ManagementAgentProperties vacateProps =
    ManagementAgentProperties.makeProps(this.getClass().getName(), defaultParams);

  protected void setupSubscriptions() {
    myUID = this.getUIDService().nextUID();

    log =  (LoggingService) getBindingSite().getServiceBroker().
      getService(this, LoggingService.class, null);

    trs = (TopologyReaderService)getBindingSite().getServiceBroker().
      getService(this, TopologyReaderService.class, null);

    cs = (CommunityService)getBindingSite().getServiceBroker().
      getService(this, CommunityService.class, null);

    DomainService domainService =
      (DomainService) getBindingSite().getServiceBroker().
      getService(this, DomainService.class, null);
    mobilityFactory =
      (MobilityFactory) domainService.getFactory("mobility");
    if (mobilityFactory == null) {
      log.error("Unable to get 'mobility' domain");
    }

    bbs = getBlackboardService();

    // Initialize configurable paramaeters from defaults and plugin arguments.
    //updateParams(vacateProps);
    bbs.publishAdd(vacateProps);

    // Subscribe to ManagementAgentProperties to receive parameter changes
    mgmtAgentProps =
      (IncrementalSubscription)bbs.subscribe(propertiesPredicate);

    // Subscribe to VacateRequest objects
    vacateRequests =
      (IncrementalSubscription)bbs.subscribe(vacateRequestPredicate);

    restartLocationRequests =
      (IncrementalSubscription)bbs.subscribe(restartLocationRequestPredicate);

    // Subscribe to AgentControl objects
    agentControlStatus =
      (IncrementalSubscription) bbs.subscribe(AGENT_CONTROL_PRED);

    // Print informational message defining current parameters
    StringBuffer startMsg = new StringBuffer();
    startMsg.append("VacatePlugin started: ");
    startMsg.append(" " + paramsToString());
    log.debug(startMsg.toString());
  }

  public void execute() {

    // Get Parameter changes
    for (Iterator it = mgmtAgentProps.getChangedCollection().iterator();
         it.hasNext();) {
      //ManagementAgentProperties props = (ManagementAgentProperties)it.next();
      //updateParams(props);
      log.info("Parameters modified: " + paramsToString());
    }

     // Get VacateRequest objects
    for (Iterator it = vacateRequests.getAddedCollection().iterator();
         it.hasNext();) {
      VacateRequestRelay relay = (VacateRequestRelay)it.next();
      VacateRequest vr = (VacateRequest)relay.getContent();
      if (log.isInfoEnabled())
        log.info("Received VacateRequest: host=" + vr.getHost() +
          " node=" + vr.getNode());
      if(vr.getRequestType() == VacateRequest.VACATE_HOST) {
        RestartLocationRequest rlr =
          new RestartLocationRequest(RestartLocationRequest.LOCATE_HOST, myUID);
        Collection excludedHosts = new Vector();
        excludedHosts.add(vr.getHost());
        rlr.setExcludedHosts(excludedHosts);
        rlr.setStatus(RestartLocationRequest.NEW);
        bbs.publishAdd(rlr);
        log.debug("Publishing RestartLocationRequest: type is " + rlr.getRequestType());
        bbs.publishRemove(vr);
      }
    }

    // Get updated RestartLocationRequest objects
    // Create a new node on destination for each node on existing host and
    // move agents to new host/node.
    for(Iterator it = restartLocationRequests.getChangedCollection().iterator(); it.hasNext();) {
      RestartLocationRequest rlr = (RestartLocationRequest)it.next();
      if(rlr.getStatus() == RestartLocationRequest.SUCCESS) {
        String destHost = rlr.getHost();
        for(Iterator iter = rlr.getExcludedHosts().iterator(); iter.hasNext();) {
          String hostName = (String)iter.next();
          // Get a map that contains all nodes/agents from my community on
          // the specified host
          Map nodeMap = getNodeMap(hostName);
          Set nodes = nodeMap.keySet();
          for(Iterator nit = nodes.iterator(); nit.hasNext();) {
            String currentNodeName = (String)nit.next();
            //String newNodeName = newNodeName(destHost, currentNodeName);
            String newNodeName = rlr.getNode();
           List agents = (List)nodeMap.get(currentNodeName);
            for(Iterator ait = agents.iterator(); ait.hasNext();) {
              String agent = (String)ait.next();
              MoveTicket ticket = new MoveTicket(
                mobilityFactory.createTicketIdentifier(),
                SimpleMessageAddress.getSimpleMessageAddress(agent),
                SimpleMessageAddress.getSimpleMessageAddress(currentNodeName),
                SimpleMessageAddress.getSimpleMessageAddress(newNodeName),
                false);
              MessageAddress AgentAddr = SimpleMessageAddress.getSimpleMessageAddress(agent);
              AgentControl ac =
                mobilityFactory.createAgentControl(myUID,
                                                   AgentAddr,
                                                   ticket);
              // Changes agents HealthStatus state to indicate that a
              // move is in process
              HealthStatus hs = getHealthStatus(AgentAddr);
              if (hs != null) hs.setState(HealthStatus.MOVE);
              publishChange(hs);
              bbs.publishAdd(ac);
              CougaarEvent.postComponentEvent(CougaarEventType.START,
                                              getAgentIdentifier().toString(),
                                              this.getClass().getName(),
                                              "Moving agent:" +
                                              " agent=" + hs.getAgentId() +
                                              " destNode=" + newNodeName);
              log.info("Moving agent: agent=" + agent + " destHost=" + destHost +
                " destNode=" + newNodeName);
              log.debug("Published AgentControl: " + ac);
            }
          }
        }
      } else if (rlr.getStatus() == RestartLocationRequest.FAIL) {
        log.error("Vacate request failed");
      }
      bbs.publishRemove(rlr);
    }

    // Get AgentControl objects
    // Update agents HealthStatus object to reflect move results
    if (agentControlStatus.hasChanged()) {
      for (Enumeration en = agentControlStatus.getChangedList(); en.hasMoreElements(); ) {
	      AgentControl ac = (AgentControl) en.nextElement();
        AbstractTicket ticket = ac.getAbstractTicket();
        if (ticket instanceof MoveTicket) {
          MoveTicket moveTicket = (MoveTicket)ticket;
          HealthStatus hs = getHealthStatus(moveTicket.getMobileAgent());
          if (hs != null) {
            hs.setLastRestartAttempt(new Date());
            if (ac.getStatusCode() == ac.MOVED) {
              hs.setState(HealthStatus.INITIAL);
              hs.setStatus(HealthStatus.MOVED);
              publishChange(hs);
              bbs.publishRemove(ac);
            } else {
              hs.setState(HealthStatus.FAILED_MOVE);
              publishChange(hs);
              bbs.publishRemove(ac);
              log.error("Unexpected status code from mobility, status=" +
                ac.getStatusCodeAsString() + " agent=" + moveTicket.getMobileAgent() +
                " destNode=" + moveTicket.getDestinationNode() + " " + ac);
            }
          }
        }
      }
    }
  }


  /**
   * Defines a name for a new node.  The new node name will consist of the
   * old node name with a number appended.
   * @param destHost    Name of destination host
   * @param oldNodeName Name of existing node
   * @return New node name
   */
 /* private String newNodeName(String destHost, String oldNodeName) {
    // Current cougaar configuration does not support the capability
    // to create nodes on demand.  For now this method simply returns the
    // destination host name concatenated with the phrase "-RestartNode".
    return destHost + "-RestartNode";
  }*/

  /**
   * Creates an emtpy node on the destination host
   * @param destHost Destination Host
   * @parma nodeName Name of node to create
   * @return         True if success
   */
 /* private boolean createNode(String destHost, String nodeName) {
    // Current cougaar configuration does not support the capability
    // to create nodes on demand.  For now this method simply returns true.
    return true;
  }*/

  /**
   * Obtains plugin parameters
   * @param obj List of "name=value" parameters
   */
  public void setParameter(Object obj) {
    List args = (List)obj;
    for (Iterator it = args.iterator(); it.hasNext();) {
      String arg = (String)it.next();
      String name = arg.substring(0,arg.indexOf("="));
      String value = arg.substring(arg.indexOf('=')+1);
      vacateProps.setProperty(name, value);
    }
  }

  /**
   * Creates a printable representation of current parameters.
   * @return  Text string of current parameters
   */
  private String paramsToString() {
    StringBuffer sb = new StringBuffer();
    for (Enumeration enum = vacateProps.propertyNames(); enum.hasMoreElements();) {
      String propName = (String)enum.nextElement();
      sb.append(propName + "=" +
        vacateProps.getProperty(propName) + " ");
    }
    return sb.toString();
  }

  /**
   * Sets externally configurable parameters using supplied Properties object.
   * @param props Propertie object defining paramater names and values.
   */
  /*
  private void updateParams(Properties props) {
     // None for now
  }
  */

  /**
   * Get a Map that contains all nodes and agents on a specified host that are
   * members of the community monitored by this ManagementAgent.
   * @param hostName  Name of host to be vacated
   * @return          Map of community nodes/agents on host
   */
  private Map getNodeMap(String hostName) {
    Map nodes = new HashMap();
    String community = null;
    Collection communities =
      cs.search("(CommunityManager=" + getMessageAddress().getAddress() + ")");
    if (!communities.isEmpty()) {
      community = (String)communities.iterator().next();
      CommunityRoster roster = cs.getRoster(community);
      Collection agentIds = roster.getMemberAgents();
      for (Iterator it = agentIds.iterator(); it.hasNext();) {
        MessageAddress aid = (MessageAddress)it.next();
        TopologyEntry te = trs.getEntryForAgent(aid.toString());
        if (te.getHost().equals(hostName)) {
          String node = te.getNode();
          String agent = te.getAgent();
          if (!nodes.containsKey(node)) nodes.put(node, new Vector());
          List agentList = (List)nodes.get(node);
          if(!agentList.contains(agent)) agentList.add(agent);
        }
      }
    }
    return nodes;
  }


  /**
   * Gets HealthStatus object associated with named agent.
   * @param agentId  MessageAddress of agent
   * @return         Agents HealthStatus object
   */
  private HealthStatus getHealthStatus(MessageAddress agentId) {
    Collection c = bbs.query(healthStatusPredicate);
    for (Iterator it = c.iterator(); it.hasNext();) {
      HealthStatus hs = (HealthStatus)it.next();
      if (hs.getAgentId().equals(agentId)) {
        return hs;
      }
    }
    log.warn("No HealthStatus object found for agent " + agentId);
    return null;
  }


 /**
  * Predicate for VacateRequest objects
  */
  private IncrementalSubscription vacateRequests;
  private UnaryPredicate vacateRequestPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof VacateRequestRelay);
  }};

  /**
   * Predicate for RestartLocationRequest objects
   */
  private IncrementalSubscription restartLocationRequests;
  private UnaryPredicate restartLocationRequestPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof RestartLocationRequest) {
        RestartLocationRequest rlr = (RestartLocationRequest)o;
        return (myUID.equals(rlr.getOwnerUID()));
      }
      return false;
    }
  };


 /**
  * Predicate for AgentControl objects
  */
  private IncrementalSubscription agentControlStatus;
  protected UnaryPredicate AGENT_CONTROL_PRED = new UnaryPredicate() {
	  public boolean execute(Object o) {
	    if (o instanceof AgentControl) {
        AgentControl ac = (AgentControl)o;
        return (myUID.equals(ac.getOwnerUID()));
      }
      return false;
  }};


 /**
  * Predicate for HealthStatus objects
  */
  private UnaryPredicate healthStatusPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof HealthStatus);
  }};

  /**
   * Predicate for Management Agent properties
   */
  String myPluginName = getClass().getName();
  private IncrementalSubscription mgmtAgentProps;
  private UnaryPredicate propertiesPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof ManagementAgentProperties) {
        ManagementAgentProperties props = (ManagementAgentProperties)o;
        String forName = props.getPluginName();
        return (myPluginName.equals(forName) || myPluginName.endsWith(forName));
      }
      return false;
  }};
}
