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

import org.cougaar.tools.robustness.ma.ldm.RestartLocationRequest;
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
 * This plugin selects a destination node or host for an agent restart
 * or move.  By default any host or node currently used by a community
 * member is a candidate destination.  Additional nodes and hosts may
 * be specified using the "restartNodes" and/or "restartHosts" plugin
 * arguments.  Each argument accepts a space separated list of names.
 */
public class RestartLocatorPlugin extends SimplePlugin {

  private LoggingService log;
  private BlackboardService bbs = null;
  private ClusterIdentifier myAgent = null;
  private CommunityService communityService = null;
  private TopologyReaderService topologyService = null;

  // Name of community to monitor
  private String communityToMonitor = null;

  // Defines default values for configurable parameters.
  private static String defaultParams[][] = {
    {"restartNodes",  ""},
    {"restartHosts",  ""}
  };

  ManagementAgentProperties restartLocatorProps =
    ManagementAgentProperties.makeProps(this.getClass().getName(), defaultParams);

  // For capturing community topology
  private Map hosts;
  private Map nodes;

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

  /**
   * Invoked when new RestartLocationRequests are received.
   */
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
      getCommunityTopology();
      Destination dest = null;
      if (req.getRequestType() == RestartLocationRequest.LOCATE_NODE) {
        dest = selectDestinationNode(req.getAgents(), req.getExcludedNodes(),
          req.getExcludedHosts());
      } else if (req.getRequestType() == RestartLocationRequest.LOCATE_HOST) {
        dest = selectDestinationHost(req.getExcludedHosts());
      }
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
          msg.append("], selectedNode=" + req.getNode() + ", selectedHost=" + req.getHost());
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

  /**
   * Identifies the nodes and hosts that are currently used by community members.
   */
  private void getCommunityTopology() {
    hosts = new HashMap();
    nodes = new HashMap();
    CommunityRoster roster = communityService.getRoster(communityToMonitor);
    Collection cmList = roster.getMembers();
    for (Iterator it = cmList.iterator(); it.hasNext();) {
      CommunityMember cm = (CommunityMember)it.next();
      if (cm.isAgent()) {
        String agentName = cm.getName();
        TopologyEntry te = topologyService.getEntryForAgent(agentName);
        String hostName = te.getHost();
        String nodeName = te.getNode();
        if (!hosts.containsKey(hostName)) hosts.put(hostName, new Vector());
        Collection c = (Collection)hosts.get(hostName);
        if (!c.contains(nodeName)) c.add(nodeName);
        if (!nodes.containsKey(nodeName)) nodes.put(nodeName, new Vector());
        c = (Collection)nodes.get(nodeName);
        if (!c.contains(agentName)) c.add(agentName);
      }
    }
  }

  /**
   * Selects nodes to be considered as a destination for an agent move/restart.
   * @param candidateNodes  Collection of all potential nodes
   * @param excludedAgents  Collection of name of agents that are being
   *                        moved/restarted
   * @param excludedNodes   Collection of node names that should not be
   *                        considered as a restart location
   * @param excludedHosts   Collection of host names that should not be
   *                        considered as a restart location
   * @return                Collection of node names that may be used for an
   *                        agent move/restart
   */
  private Collection selectNodes(Collection candidateNodes,
      Collection excludedAgents, Collection excludedNodes, Collection excludedHosts) {
    Collection selectedNodes = new Vector();
    for (Iterator it = candidateNodes.iterator(); it.hasNext();) {
      String nodeName = (String)it.next();
      if (excludedNodes != null && !excludedNodes.contains(nodeName)) {
        String hostName = null;
        for (Iterator it1 = hosts.entrySet().iterator(); it1.hasNext();) {
          Map.Entry me = (Map.Entry)it1.next();
          hostName = (String)me.getKey();
          Collection residentNodes = (Collection)me.getValue();
          if (residentNodes.contains(nodeName)) break;
        }
        if (excludedHosts != null && !excludedHosts.contains(hostName)) {
          Collection residentAgents = (Collection)nodes.get(nodeName);
          boolean nodeHasExcludedAgent = false;
            for (Iterator it2 = excludedAgents.iterator(); it2.hasNext();) {
          String excludedAgent = ((ClusterIdentifier)it2.next()).toString();
            if (residentAgents.contains(excludedAgent)) {
              nodeHasExcludedAgent = true;
              break;
            }
          }
          if (!nodeHasExcludedAgent) selectedNodes.add(nodeName);
        }
      }
    }
    return selectedNodes;
  }

  /**
   * Selects hosts to be considered as a destination for an agent move/restart.
   * @param candidateHosts  Collection of all potential hosts
   * @param excludedHosts   Collection of host names that should not be
   *                        considered as a restart location
   * @return                Collection of host names that may be used for an
   *                        agent move/restart
   */
  private Collection selectHosts(Collection candidateHosts, Collection excludedHosts) {
    Collection selectedHosts = new Vector();
    for (Iterator it = candidateHosts.iterator(); it.hasNext();) {
      String hostName = (String)it.next();
      if (excludedHosts != null && !excludedHosts.contains(hostName)) {
        selectedHosts.add(hostName);
      }
    }
    return selectedHosts;
  }

  /**
   * Select a destination node for restarting an agent.
   * @param excludedAgents  Collection of name of agents that are being
   *                        moved/restarted
   * @param excludedNodes   Collection of node names that should not be
   *                        considered as a restart location
   * @param excludedHosts   Collection of host names that should not be
   *                        considered as a restart location
   * @return  restart Destination
   */
  private Destination selectDestinationNode(Collection excludedAgents,
      Collection excludedNodes, Collection excludedHosts) {
    Collection selectedNodes = new Vector();
    String specifiedRestartNodes = restartLocatorProps.getProperty("restartNodes");
    if (specifiedRestartNodes != null && specifiedRestartNodes.trim().length() > 0) {
      Collection specifiedNodes = new Vector();
      StringTokenizer st = new  StringTokenizer(specifiedRestartNodes, " ");
      while (st.hasMoreTokens()) specifiedNodes.add(st.nextToken());
      selectedNodes = selectNodes(specifiedNodes, excludedAgents, excludedNodes, excludedHosts);
    } else {
      selectedNodes = selectNodes(nodes.keySet(), excludedAgents, excludedNodes, excludedHosts);
    }
    int agentCount = -1;
    String nodeName = null;
    for (Iterator it = selectedNodes.iterator(); it.hasNext();) {
      String tmpName = (String)it.next();
      int tmpCount = ((Collection)nodes.get(tmpName)).size();
      if (agentCount < 0 || tmpCount > agentCount) {
        agentCount = tmpCount;
        nodeName = tmpName;
      }
    }
    String hostName = null;
    if (nodeName != null) {
      for (Iterator it = hosts.entrySet().iterator(); it.hasNext();) {
        Map.Entry me = (Map.Entry)it.next();
        String host = (String)me.getKey();
        Collection residentNodes = (Collection)me.getValue();
        if (residentNodes.contains(nodeName)) {
          hostName = host;
          break;
        }
      }
    }
    Destination dest = new Destination();
    dest.node = nodeName;
    dest.host = hostName;
    dest.numAgents = agentCount;
    return dest;
  }

  /**
   * Select a destination host for restarting/moving an agent or node.
   * @param excludedHosts  Collection of host names that should not be
   *                       considered as a possible destination
   * @return               Collection of host names
   */
  private Destination selectDestinationHost(Collection excludedHosts) {
    Collection selectedHosts = new Vector();
    String specifiedRestartHosts = restartLocatorProps.getProperty("restartHosts");
    if (specifiedRestartHosts != null && specifiedRestartHosts.trim().length() > 0) {
      Collection specifiedHosts = new Vector();
      StringTokenizer st = new  StringTokenizer(specifiedRestartHosts, " ");
      while (st.hasMoreTokens()) specifiedHosts.add(st.nextToken());
      selectedHosts = selectHosts(specifiedHosts, excludedHosts);
    } else {
      selectedHosts = selectHosts(hosts.keySet(), excludedHosts);
    }
    int nodeCount = -1;
    String hostName = null;
    for (Iterator it = selectedHosts.iterator(); it.hasNext();) {
      String tmpName = (String)it.next();
      int tmpCount = ((Collection)hosts.get(tmpName)).size();
      if (nodeCount < 0 || tmpCount > nodeCount) {
        nodeCount = tmpCount;
        hostName = tmpName;
      }
    }
    Destination dest = new Destination();
    dest.node = null;
    dest.host = hostName;
    dest.numAgents = nodeCount;
    return dest;
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

  class Destination {
    String host;
    String node;
    int numAgents;
  }
}