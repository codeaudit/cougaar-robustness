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

import org.cougaar.robustness.restart.plugin.*;
import java.util.*;

import org.cougaar.core.agent.ClusterIdentifier;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.SimplePlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.TopologyEntry;
import org.cougaar.core.service.TopologyReaderService;
import org.cougaar.core.mts.MessageAddress;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.component.ServiceRevokedEvent;

import org.cougaar.util.UnaryPredicate;

import org.cougaar.tools.server.*;
import org.cougaar.tools.server.system.ProcessStatus;

import org.cougaar.core.service.community.*;
import org.cougaar.community.*;

/**
 * This plugin ...
 */
public class RestartLocatorPlugin extends SimplePlugin {

  private LoggingService log;
  private BlackboardService bbs = null;
  private ClusterIdentifier myAgent = null;
  private CommunityService communityService = null;
  private TopologyReaderService topologyService = null;

  // Name of community to monitor
  private String communityToMonitor = null;

  private String restartNodeName;

  // Defines default values for configurable parameters.
  private static String defaultParams[][] = new String[0][0];

  ManagementAgentProperties restartLocatorProps =
    ManagementAgentProperties.makeProps(this.getClass().getName(), defaultParams);

  private List memberList = new Vector();  // List of community members
  private Map availNodes = new HashMap();

  protected void setupSubscriptions() {

    log =  (LoggingService) getBindingSite().getServiceBroker().
      getService(this, LoggingService.class, null);

    bbs = getBlackboardService();

    myAgent = getClusterIdentifier();

    communityService = getCommunityService();
    topologyService = getTopologyReaderService();

    // Find name of community to monitor
    Collection communities = communityService.search("(CommunityManager=" +
      myAgent.toString() + ")");
    if (!communities.isEmpty())
      restartLocatorProps.setProperty("community",
                                      (String)communities.iterator().next());

    // Initialize configurable paramaeters from defaults and plugin arguments.
    updateParams(restartLocatorProps);
    bbs.publishAdd(restartLocatorProps);

    // Subscribe to ManagementAgentProperties to receive parameter changes
    mgmtAgentProps =
      (IncrementalSubscription)bbs.subscribe(propertiesPredicate);

    // Subscribe to RestartLocationRequest objects
    restartRequests =
      (IncrementalSubscription)bbs.subscribe(restartRequestPredicate);

    // Print informational message defining current parameters
    StringBuffer startMsg = new StringBuffer();
    startMsg.append("RestartLocatorPlugin started: ");
    startMsg.append(paramsToString());
    log.info(startMsg.toString());
  }

  public void execute() {

    // Get Parameter changes
    for (Iterator it = mgmtAgentProps.getChangedCollection().iterator();
         it.hasNext();) {
      ManagementAgentProperties props = (ManagementAgentProperties)it.next();
      updateParams(props);
      log.info("Parameters modified: " + paramsToString());
    }

    // Get RestartLocationRequests
    for (Iterator it = restartRequests.getAddedCollection().iterator();
         it.hasNext();) {
      RestartLocationRequest req = (RestartLocationRequest)it.next();
      CandidateNode dest = selectDestination(req.getAgents());
      if (dest != null) {
        req.setHost(dest.host);
        req.setNode(dest.node);
        req.setStatus(RestartLocationRequest.SUCCESS);
        if (log.isInfoEnabled()) {
          StringBuffer msg =
            new StringBuffer("Received a RestartLocation request: agent(s)=[");
          for (Iterator it1 = req.getAgents().iterator(); it1.hasNext();) {
            msg.append(((MessageAddress)it1.next()).toString());
            if (it.hasNext()) msg.append(" ");
          }
          msg.append("], selectedNode=" + req.getNode());
          //msg.append(", useCount=" + (Integer)availHosts.get(req.getHost()));
          log.info(msg.toString());
        }
      } else {
        req.setStatus(RestartLocationRequest.FAIL);
        log.error("No host/node available for restart");
      }
      bbs.publishChange(req);
    }
  }

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
      restartLocatorProps.setProperty(name, value);
    }
  }


  /**
   * Sets externally configurable parameters using supplied Properties object.
   * @param props Properties object defining paramater names and values.
   */
  private void updateParams(Properties props) {
    communityToMonitor = props.getProperty("community");
  }

  /**
   * Creates a printable representation of current parameters.
   * @return  Text string of current parameters
   */
  private String paramsToString() {
    StringBuffer sb = new StringBuffer();
    for (Enumeration enum = restartLocatorProps.propertyNames(); enum.hasMoreElements();) {
      String propName = (String)enum.nextElement();
      sb.append(propName + "=" +
        restartLocatorProps.getProperty(propName) + " ");
    }
    return sb.toString();
  }

  private void startNode(String hostName) {
    String nodeName = hostName + "-" + restartNodeName;
    if (!nodeRunning(hostName, nodeName)) {
      NodeStart ns = new NodeStart(hostName, nodeName);
      bbs.publishAdd(ns);
    }
  }

  private Collection getCandidateNodes(Set agents) {
    CommunityRoster roster = communityService.getRoster(communityToMonitor);
    Collection cmList = roster.getMembers();
    Map candidateNodes = new HashMap();
    Collection deadAgentNodes = new Vector();
    for (Iterator it = cmList.iterator(); it.hasNext();) {
      CommunityMember cm = (CommunityMember)it.next();
      if (cm.isAgent()) {
        String agentName = cm.getName();
        TopologyEntry te = topologyService.getEntryForAgent(agentName);
        String hostName = te.getHost();
        String nodeName = te.getNode();
        if(agents.contains(cm.getAgentId())) {
          deadAgentNodes.add(nodeName);
        } else {
          if (candidateNodes.containsKey(nodeName)) {
            CandidateNode cn = (CandidateNode)candidateNodes.get(nodeName);
            ++cn.numAgents;
          } else {
            CandidateNode cn = new CandidateNode();
            cn.host = hostName;
            cn.node = nodeName;
            cn.numAgents = 1;
            candidateNodes.put(nodeName, cn);
          }
        }
      }
    }
    // Remove dead agent nodes from candidate nodes
    for (Iterator it = deadAgentNodes.iterator(); it.hasNext();) {
      String nodeName = (String)it.next();
      if (candidateNodes.containsKey(nodeName)) candidateNodes.remove(nodeName);
    }
    return candidateNodes.values();
  }

  /**
   * Selects a destination host/node for a restart.  The selection is an
   * existing node used by the community with the fewest number of agents.
   * @return  Name of selected host
   */
  private CandidateNode selectDestination(Set agents) {
    Collection candidateNodes = getCandidateNodes(agents);
    CandidateNode selectedNode = null;
    int agentCount = -1;
    for (Iterator it = candidateNodes.iterator(); it.hasNext();) {
      CandidateNode cn = (CandidateNode)it.next();
      if (agentCount < 0 || cn.numAgents < agentCount)
        selectedNode = cn;
    }
    return selectedNode;
  }

	private boolean nodeRunning(String hostName, String nodeName) {

    // create a remote-host-registry instance
    RemoteHostRegistry hostReg = RemoteHostRegistry.getInstance();

    try {
    // contact the host
    RemoteHost rhost =  hostReg.lookupRemoteHost(hostName, 8484, true);

    // verify that hosts exists, it has a running appserver, and a node
    // process is running
    return (rhost.ping() != 0 &&
            hasProcess(rhost, hostName + "-" + nodeName));

    } catch (Exception ex) {
      log.debug("Exception checking node status, " + ex);
    }
    return false;
  }

  private boolean hasProcess(RemoteHost rhost, String name) throws Exception {
    // list the running processes on the server
    List runningProcs =
      rhost.listProcessDescriptions();
    for (Iterator it = runningProcs.iterator(); it.hasNext();) {
      ProcessDescription pd = (ProcessDescription)it.next();
      if(pd.getName().equals(name)) return true;
    }
    return false;
  }

  /**
   * Gets reference to CommunityService.
   */
  private CommunityService getCommunityService() {
    ServiceBroker sb = getBindingSite().getServiceBroker();
    if (sb.hasService(CommunityService.class)) {
      return (CommunityService)sb.getService(this, CommunityService.class,
        new ServiceRevokedListener() {
          public void serviceRevoked(ServiceRevokedEvent re) {}
      });
    } else {
      log.error("CommunityService not available");
      return null;
    }
  }

  /**
   * Gets reference to TopologyReaderService.
   */
  private TopologyReaderService getTopologyReaderService() {
    ServiceBroker sb = getBindingSite().getServiceBroker();
    if (sb.hasService(TopologyReaderService.class)) {
      return (TopologyReaderService)sb.getService(this, TopologyReaderService.class,
        new ServiceRevokedListener() {
          public void serviceRevoked(ServiceRevokedEvent re) {}
      });
    } else {
      log.error("TopologyReaderService not available");
      return null;
    }
  }

  /**
   * Predicate for RestartLocationRequest objects
   */
  private IncrementalSubscription restartRequests;
  private UnaryPredicate restartRequestPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof RestartLocationRequest) {
        RestartLocationRequest req = (RestartLocationRequest)o;
        return (req.getStatus() == RestartLocationRequest.NEW);
      }
      return false;
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

  class CandidateNode {
    String host;
    String node;
    int numAgents;
  }
}