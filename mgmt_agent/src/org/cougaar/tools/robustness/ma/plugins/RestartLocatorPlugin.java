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
import java.util.*;
import javax.naming.directory.*;
import javax.naming.*;

import org.cougaar.tools.robustness.sensors.SensorFactory;
import org.cougaar.tools.robustness.sensors.PingRequest;

import org.cougaar.core.agent.ClusterIdentifier;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.SimplePlugin;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.TopologyEntry;
import org.cougaar.core.service.TopologyReaderService;
import org.cougaar.core.mts.MessageAddress;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.component.ServiceRevokedEvent;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.util.UID;

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

  private SensorFactory sensorFactory;

  // Name of community to monitor
  private String communityToMonitor = null;

  // Collection of UIDs associated with pending pings
  private Collection pingUIDs = new Vector();

  // In process RestartLocationRequests
  private Map restartRequestMap = new HashMap();

  // Defines default values for configurable parameters.
  private static String defaultParams[][] = {
    {"restartNode",  ""},
    {"pingTimeout",  "120000"}
  };

  ManagementAgentProperties restartLocatorProps =
    ManagementAgentProperties.makeProps(this.getClass().getName(), defaultParams);

  // For capturing community topology
  private Map hosts;
  private Map nodes;

  private List specifiedHosts;
  private long pingTimeout;

  protected void setupSubscriptions() {

    log =  (LoggingService) getBindingSite().getServiceBroker().
      getService(this, LoggingService.class, null);

    bbs = getBlackboardService();

    DomainService domainService =
      (DomainService) getBindingSite().getServiceBroker().
      getService(this, DomainService.class, null);

    sensorFactory =
      ((SensorFactory) domainService.getFactory("sensors"));
    if (sensorFactory == null) {
      log.error("Unable to get 'sensors' domain");
    }

    myAgent = getClusterIdentifier();

    communityService = getCommunityService();
    topologyService = getTopologyReaderService();

    // Find name of community to monitor
    Collection communities = communityService.search("(CommunityManager=" +
      myAgent.toString() + ")");
    if (!communities.isEmpty()) {
      communityToMonitor = (String)communities.iterator().next();

      // Initialize configurable paramaeters from defaults and plugin arguments.
      getPropertiesFromCommunityAttributes();
    } else {
      log.error("Agent \"" + myAgent.toString() +
        "\" not identified as a \"CommunityManager\" in any active community!.");
      log.error("Check that the community to monitor has this agent identified\n");
      log.error("in its \"CommunityManager\" attribute and that this agent is\n");
      log.error("also identified as an Entity within the monitored community with\n");
      log.error("the attribute \"Role=ManagementAgent\".");
    }

    // Initialize configurable paramaeters from defaults and plugin arguments.
    updateParams(restartLocatorProps);
    bbs.publishAdd(restartLocatorProps);

    // Subscribe to ManagementAgentProperties to receive parameter changes
    mgmtAgentProps =
      (IncrementalSubscription)bbs.subscribe(propertiesPredicate);

    // Subscribe to RestartLocationRequest objects
    restartRequests =
      (IncrementalSubscription)bbs.subscribe(restartRequestPredicate);

    // Subscribe to PingRequests to receive ping results
    pingRequests =
      (IncrementalSubscription)bbs.subscribe(pingRequestPredicate);

    // Print informational message defining current parameters
    log.info(paramsToString());

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
      //log.info("Got RestartLocationRequest: agent(s)=" + req.getAgents());
      getCommunityTopology();
      Collection destinations =
        destinations = selectDestinationNodes(req.getAgents(), req.getExcludedNodes(),
          req.getExcludedHosts());
      if (destinations != null && destinations.size() > 0) {
        // get first candidate destination
        log.debug("RestartLocationRequest: " + destinationsToString(destinations));
        restartRequestMap.put(req, destinations);
        Destination dest = (Destination)destinations.iterator().next();
        doPing(new ClusterIdentifier(dest.node));
      } else {
        req.setStatus(RestartLocationRequest.FAIL);
        bbs.publishChange(req);
        StringBuffer msg =
          new StringBuffer("No node available for restart: agent(s)=[");
        for (Iterator it1 = req.getAgents().iterator(); it1.hasNext();) {
          msg.append(((MessageAddress)it1.next()).toString());
          if (it1.hasNext()) msg.append(" ");
        }
        msg.append("]");
        log.warn(msg.toString());
      }
    }

    // Get PingRequests
    for (Iterator it = pingRequests.getChangedCollection().iterator();
         it.hasNext();) {
      PingRequest req = (PingRequest)it.next();
      int status = req.getStatus();
      String node = req.getTarget().toString();
      //if (log.isDebugEnabled()) {
      //  log.debug("PingRequest changed, agent=" + node + " request" + req);
      //}
      switch (status) {
        case PingRequest.SENT:
          break;
        case PingRequest.RECEIVED:
          for (Iterator it1 = restartRequestMap.entrySet().iterator(); it1.hasNext();) {
            Map.Entry me = (Map.Entry)it1.next();
            RestartLocationRequest rlr = (RestartLocationRequest)me.getKey();
            Collection destinations = (Collection)me.getValue();
            for (Iterator it2 = destinations.iterator(); it2.hasNext();) {
              Destination dest = (Destination)it2.next();
              if (dest.node.equals(node) && dest.pingStatus == PingRequest.NEW) {
                dest.pingStatus = PingRequest.RECEIVED;
                //log.debug("Ping succeeded, agent=" + node);
                // Remove PingRequest from BB and our internal list
                if (pingUIDs.contains(req.getUID())) pingUIDs.remove(req.getUID());
                bbs.publishRemove(req);
                rlr.setHost(dest.host);
                rlr.setNode(dest.node);
                rlr.setStatus(RestartLocationRequest.SUCCESS);
                bbs.publishChange(rlr);
                it1.remove();
                if (log.isInfoEnabled()) {
                  StringBuffer msg =
                    new StringBuffer("Completed RestartLocation request: agent(s)=[");
                  for (Iterator it3 = rlr.getAgents().iterator(); it3.hasNext();) {
                    msg.append(((MessageAddress)it3.next()).toString());
                    if (it3.hasNext()) msg.append(" ");
                  }
                  msg.append("], selectedNode=" + rlr.getNode() + ", selectedHost=" + rlr.getHost());
                  log.debug(msg.toString());
                  break;
                }
              }
            }
          }
          break;
        case PingRequest.FAILED:
          for (Iterator it1 = restartRequestMap.entrySet().iterator(); it1.hasNext();) {
            Map.Entry me = (Map.Entry)it1.next();
            RestartLocationRequest rlr = (RestartLocationRequest)me.getKey();
            Collection destinations = (Collection)me.getValue();
            for (Iterator it2 = destinations.iterator(); it2.hasNext();) {
              Destination dest = (Destination)it2.next();
              if (dest.node.equals(node) && dest.pingStatus == PingRequest.NEW) {
                dest.pingStatus = PingRequest.FAILED;
                log.debug("Ping failed, agent=" + node);
                if (it2.hasNext()) {
                  dest = (Destination)it2.next();
                  doPing(new ClusterIdentifier(dest.node));
                } else {
                  rlr.setStatus(RestartLocationRequest.FAIL);
                  StringBuffer msg =
                    new StringBuffer("No nodes/hosts available for restart: agent(s)=[");
                  for (Iterator it3 = rlr.getAgents().iterator(); it3.hasNext();) {
                    msg.append(((MessageAddress)it3.next()).toString());
                    if (it3.hasNext()) msg.append(" ");
                  }
                  msg.append("]");
                  it1.remove();
                  log.debug(msg.toString());
                  bbs.publishChange(rlr);
                }
              }
            }
          }
          if (pingUIDs.contains(req.getUID())) pingUIDs.remove(req.getUID());
          bbs.publishRemove(req);
          break;
        default:
      }
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
    String specifiedRestartNodes = restartLocatorProps.getProperty("restartNode");
    if (specifiedRestartNodes != null && specifiedRestartNodes.trim().length() > 0) {
      Attribute restartNodesAttr = new BasicAttribute("RestartNode");
      StringTokenizer st = new  StringTokenizer(specifiedRestartNodes, " ");
      while (st.hasMoreTokens()) {
        restartNodesAttr.add(st.nextToken());
      }
      ModificationItem mods[] = new ModificationItem[]{
        new ModificationItem(DirContext.REPLACE_ATTRIBUTE, restartNodesAttr),
      };
      communityService.modifyCommunityAttributes(communityToMonitor, mods);
    }
    Collection restartNodes = getSpecifiedNodes();
    StringBuffer sb = new StringBuffer("[");
    for (Iterator it = restartNodes.iterator(); it.hasNext();) {
      sb.append((String)it.next());
      if (it.hasNext()) {
        sb.append(",");
      }
    }
    sb.append("]");
    props.setProperty("restartNode",sb.toString());
    pingTimeout = Long.parseLong(props.getProperty("pingTimeout"));
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
    specifiedHosts = new Vector();
    if (topologyService == null) {
      log.error("TopologyService is null");
    } else {
      try {
        CommunityRoster roster = communityService.getRoster(communityToMonitor);
        Collection cmList = roster.getMembers();
        // Find hosts/nodes/agents associated with community members
        for (Iterator it = cmList.iterator(); it.hasNext();) {
          CommunityMember cm = (CommunityMember)it.next();
          if (cm.isAgent()) {
            String agentName = cm.getName();
            TopologyEntry te = topologyService.getEntryForAgent(agentName);
            if (te == null) {
              log.debug("Null TopologyEntry: agent =" + agentName);
              continue;
            }
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
        // Add RestartNodes associated with hosts currently used by community members
        /*
        for (Iterator it = hosts.keySet().iterator(); it.hasNext();) {
          String nodeAgentName = (String)it.next() + "-RestartNode";
          TopologyEntry te = topologyService.getEntryForAgent(nodeAgentName);
          if (te != null) {
            String hostName = te.getHost();
            String nodeName = nodeAgentName;
            if (!hosts.containsKey(hostName)) hosts.put(hostName, new Vector());
            Collection c = (Collection)hosts.get(hostName);
            if (!c.contains(nodeName)) c.add(nodeName);
            if (!nodes.containsKey(nodeName)) nodes.put(nodeName, new Vector());
          }
        }
        // Add RestartNodes associated with specified Restart Hosts
        String specifiedRestartHosts = restartLocatorProps.getProperty("restartHosts");
        if (specifiedRestartHosts != null && specifiedRestartHosts.trim().length() > 0) {
          StringTokenizer st = new  StringTokenizer(specifiedRestartHosts, " ");
          while (st.hasMoreTokens()) {
            String nodeAgentName = st.nextToken() + "-RestartNode";
            TopologyEntry te = topologyService.getEntryForAgent(nodeAgentName);
            if (te != null) {
              String hostName = te.getHost();
              String nodeName = nodeAgentName;
              specifiedHosts.add(hostName);
              if (!hosts.containsKey(hostName)) hosts.put(hostName, new Vector());
              Collection c = (Collection)hosts.get(hostName);
              if (!c.contains(nodeName)) c.add(nodeName);
              if (!nodes.containsKey(nodeName)) nodes.put(nodeName, new Vector());
            }
          }
        }
        */
      } catch (Exception ex) {
        log.error("Exception getting community topology", ex);
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
    if (log.isDebugEnabled()) {
      log.debug("SelectNodes: candidates=" + candidateNodes +
        " excludedHosts=" + excludedHosts +
        " excludedNodes=" + excludedNodes);
    }
    Collection selectedNodes = new Vector();
    for (Iterator it = candidateNodes.iterator(); it.hasNext();) {
      String nodeName = (String)it.next();
      if (excludedNodes != null && !excludedNodes.contains(nodeName)) {
        String hostName =
          topologyService.getParentForChild(topologyService.HOST,
                                            topologyService.NODE,
                                            nodeName);
        if (hostName != null &&
            (excludedHosts == null || !excludedHosts.contains(hostName))) {
          Set agents =
            topologyService.getChildrenOnParent(topologyService.AGENT,
                                              topologyService.NODE,
                                              nodeName);
          if (agents == null || excludedAgents == null) {
            selectedNodes.add(nodeName);
          } else {
            boolean nodeHasExcludedAgent = false;
            for (Iterator it1 = excludedAgents.iterator(); it1.hasNext();) {
              String excludedAgent = ((MessageAddress)it1.next()).toString();
              if (agents.contains(excludedAgent)) {
                nodeHasExcludedAgent = true;
                break;
              }
            }
            if (!nodeHasExcludedAgent) selectedNodes.add(nodeName);
          }
        }
      }
    }
    return selectedNodes;
  }

  /**
   * Get collection of specified restart nodes from community attributes entry
   * in Name Server.
   * @return Collection of specified nodes
   */
  private Collection getSpecifiedNodes() {
    Collection specifiedNodes = new Vector();
    try {
      Attributes attrs = communityService.getCommunityAttributes(communityToMonitor);
      Attribute attr = attrs.get("RestartNode");
      if (attr != null) {
        NamingEnumeration ne = attr.getAll();
        while (ne.hasMore()) {
          specifiedNodes.add(ne.next());
        }
      }
    } catch (Exception ex) {
      log.error("Error getting 'RestartNode' attribute for community'" +
        communityToMonitor + "' from Name Server");
    }
    return specifiedNodes;
  }

  /**
   * Select a destination node for restarting an agent.
   * @param excludedAgents  Collection of name of agents that are being
   *                        moved/restarted
   * @param excludedNodes   Collection of node names that should not be
   *                        considered as a restart location
   * @param excludedHosts   Collection of host names that should not be
   *                        considered as a restart location
   * @return  Collection of restart Destinations
   */
  private Collection selectDestinationNodes(Collection excludedAgents,
      Collection excludedNodes, Collection excludedHosts) {
    List destinations = new Vector();
    Collection selectedNodes = new Vector();
    Collection specifiedNodes = getSpecifiedNodes();
    if (specifiedNodes.size() > 0) {
      selectedNodes = selectNodes(specifiedNodes, excludedAgents, excludedNodes, excludedHosts);
    } else {
      selectedNodes = selectNodes(nodes.keySet(), excludedAgents, excludedNodes, excludedHosts);
    }
    //log.debug("SelectedNodes=" + selectedNodes);
    for (Iterator it = selectedNodes.iterator(); it.hasNext();) {
      Destination dest = new Destination();
      dest.node = (String)it.next();
      Collection agents = (Collection)nodes.get(dest.node);
      dest.numAgents = (agents == null) ? 0 : agents.size();
      for (Iterator it1 = hosts.entrySet().iterator(); it1.hasNext();) {
        Map.Entry me = (Map.Entry)it1.next();
        String host = (String)me.getKey();
        Collection residentNodes = (Collection)me.getValue();
        if (residentNodes.contains(dest.node)) {
          dest.host = host;
          break;
        }
      }
      destinations.add(dest);
    }
    Collections.sort(destinations);
    return destinations;
  }

  private String destinationsToString(Collection destinations) {
    StringBuffer sb = new StringBuffer("Destinations=[");
    for (Iterator it = destinations.iterator(); it.hasNext();) {
      Destination dest = (Destination)it.next();
      sb.append(dest.host + ":" + dest.node + "(" + dest.numAgents + ")");
      if (it.hasNext()) sb.append(", ");
    }
    sb.append("]");
    return sb.toString();
  }


  /**
   * Gets reference to CommunityService.
   * @return Reference to CommunityService
   */
  private CommunityService getCommunityService() {
    ServiceBroker sb = getBindingSite().getServiceBroker();
    if (sb.hasService(CommunityService.class)) {
      return (CommunityService)sb.getService(this, CommunityService.class, null);
    } else {
      log.error("CommunityService not available");
      return null;
    }
  }

  /**
   * Gets externally configurable parameters defined in community attributes.
   */
  private void getPropertiesFromCommunityAttributes() {
    Attributes attrs =
      communityService.getCommunityAttributes(communityToMonitor);
    try {
      NamingEnumeration enum = attrs.getAll();
      while (enum.hasMore()) {
        Attribute attr = (Attribute)enum.nextElement();
        String id = attr.getID();
        if (restartLocatorProps.containsKey(id)) {
          restartLocatorProps.setProperty(id, (String)attr.get());
        }
      }
    } catch (NamingException ne) {
      log.error("Exception getting attributes from CommunityService, " + ne);
    }
  }

  /**
   * Gets reference to TopologyReaderService.
   * @return Reference to TopologyReaderService
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
   * Sends a ping to a candidate restart node.
   * @param addr NodeAgent address
   */
  private void doPing(ClusterIdentifier addr) {
    PingRequest pr = sensorFactory.newPingRequest(myAgent,
                                   addr,
                                   pingTimeout);
    pingUIDs.add(pr.getUID());
    /*
    if (log.isDebugEnabled()) {
      log.debug("Performing ping: source=" + pr.getSource() + "(" +
        pr.getSource().getClass().getName() + ") target=" +
        pr.getTarget() + "(" + pr.getTarget().getClass().getName() + ")");
    }
    */
    bbs.publishAdd(pr);
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

  private IncrementalSubscription pingRequests;
  private UnaryPredicate pingRequestPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof PingRequest) {
        PingRequest pr = (PingRequest)o;
        return (pingUIDs.contains(pr.getUID()));
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

  class Destination implements Comparable {
    String host;
    String node;
    int numAgents;
    int pingStatus = PingRequest.NEW;
    public int compareTo(Object o) throws ClassCastException {
      return numAgents - ((Destination)o).numAgents;
    }
  }
}