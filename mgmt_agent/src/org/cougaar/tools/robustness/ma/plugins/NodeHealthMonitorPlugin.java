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

import org.cougaar.core.plugin.ComponentPlugin;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.SimpleMessageAddress;

import org.cougaar.core.service.AlarmService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.EventService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;

import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.service.wp.Callback;
import org.cougaar.core.service.wp.Response;

import org.cougaar.core.agent.service.alarm.Alarm;

import org.cougaar.core.util.UID;

import org.cougaar.core.blackboard.IncrementalSubscription;

import org.cougaar.util.UnaryPredicate;

import org.cougaar.core.service.community.Agent;
import org.cougaar.core.service.community.Community;
import org.cougaar.core.service.community.CommunityService;
import org.cougaar.core.service.community.CommunityChangeEvent;
import org.cougaar.core.service.community.CommunityChangeListener;
import org.cougaar.core.service.community.CommunityResponseListener;
import org.cougaar.core.service.community.CommunityResponse;
import org.cougaar.core.service.community.Entity;

import org.cougaar.community.requests.CommunityRequest;
import org.cougaar.community.requests.JoinCommunity;
import org.cougaar.community.requests.SearchCommunity;

import org.cougaar.tools.robustness.ma.controllers.RobustnessController;
import org.cougaar.tools.robustness.ma.CommunityStatusModel;
import org.cougaar.tools.robustness.ma.ldm.RelayAdapter;
import org.cougaar.tools.robustness.ma.ldm.AgentStatus;
import org.cougaar.tools.robustness.ma.ldm.NodeStatusRelay;
import org.cougaar.tools.robustness.ma.ldm.NodeStatusRelayImpl;
import org.cougaar.tools.robustness.ma.ldm.HealthMonitorRequest;
import org.cougaar.tools.robustness.ma.ldm.HealthMonitorResponse;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Set;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;

import java.net.URI;

/**
 * Primary class used to monitor/maintain agent liveness in a community.  This
 * plugin performs the following functions:
 * <pre>
 *   1) Detects the addition of the host agent to a robustness community to
 *      monitor.
 *   2) Creates a status model for the monitored community.
 *   3) Creates a RobustnessController for the monitored community.
 *   4) Periodically sends status update to peer health monitors in the
 *      monitored community.
 *   5) Receives status updates from peers and updates model.
 *   6) Respond to status requests from ARServlet.
 * </pre>
 */
public class NodeHealthMonitorPlugin extends ComponentPlugin {

  // Property used to define the name of a robustness community to monitor
  public static final String COMMUNITY_PROP_NAME = "org.cougaar.tools.robustness.community";

  // Defines attribute to use in selection of community to monitor
  public static final String HEALTH_MONITOR_ROLE = "HealthMonitor";
  public static final String COMMUNITY_TYPE =      "Robustness";

  // Defines class to use for Robustness Controller
  public static final String CONTROLLER_CLASS_PROPERTY =
      "org.cougaar.tools.robustness.controller.classname";
  public static final String DEFAULT_ROBUSTNESS_CONTROLLER_CLASSNAME =
      "org.cougaar.tools.robustness.ma.controllers.DefaultRobustnessController";

  // Defines how often status updates are broadcast to peers
  public static final String STATUS_UPDATE_PROPERTY =
      "org.cougaar.tools.robustness.update.interval";
  public static final String DEFAULT_STATUS_UPDATE_INTERVAL = "30000";
  public static long updateInterval;

  // Info associated with this health monitor
  private String myName;
  private String myType = "Agent";
  private String myNode;
  private String myHost;
  private boolean joinedStartupCommunity = false;
  private boolean getTopologyFlag = true;

  // Status model and controller associated with monitored community
  private CommunityStatusModel model;
  private RobustnessController controller;

  // Relay used to send status info to peers
  private RelayAdapter nodeStatusRelay;

  // Status send timer
  private WakeAlarm wakeAlarm;

  private Set myUIDs = new HashSet();

  // Services used
  private LoggingService logger;
  private UIDService uidService = null;
  private EventService eventService;
  private CommunityService commSvc;
  private WhitePagesService whitePagesService;

  public void setWhitePagesService(WhitePagesService wps) {
    whitePagesService = wps;
  }

  public void setCommunityService(CommunityService cs) {
    commSvc = cs;
  }

  // Join Robustness Community designated by startup parameter
  private void joinStartupCommunity() {
    String initialCommunity = System.getProperty(COMMUNITY_PROP_NAME);
    if (initialCommunity != null) {
      logger.debug("Joining community " + initialCommunity);
      UID joinRequestUID = uidService.nextUID();
      myUIDs.add(joinRequestUID);
      Attributes memberAttrs = new BasicAttributes();
      Attribute roles = new BasicAttribute("Role", "Member");
      roles.add(HEALTH_MONITOR_ROLE);
      memberAttrs.put(roles);
      memberAttrs.put("EntityType", myType);
      memberAttrs.put("CanBeManager", "False");
      blackboard.publishAdd(new JoinCommunity(initialCommunity,
                                              myName,
                                              CommunityService.AGENT,
                                              memberAttrs,
                                              false,
                                              null,
                                              joinRequestUID));
    } else {
      logger.debug("No initial community defined");
    }
    joinedStartupCommunity = true;
  }

  public void setupSubscriptions() {
    myName = agentId.toString();

    // Get required services
    logger =
      (LoggingService)getBindingSite().getServiceBroker().getService(this, LoggingService.class, null);
    logger = org.cougaar.core.logging.LoggingServiceWithPrefix.add(logger, agentId + ": ");
    uidService =
      (UIDService) getBindingSite().getServiceBroker().getService(this, UIDService.class, null);
    eventService =
      (EventService) getBindingSite().getServiceBroker().getService(this, EventService.class, null);

    // Subscribe to Node Status updates sent by peer Health Monitors via Relay
    nodeStatusRelaySub =
        (IncrementalSubscription)blackboard.subscribe(nodeStatusRelayPredicate);

    // Subscribe to external requests
    healthMonitorRequests =
        (IncrementalSubscription)blackboard.subscribe(healthMonitorRequestPredicate);

    // Publish SearchCommunity request to look for Robustness Communities
    searchRequests =
      (IncrementalSubscription)blackboard.subscribe(searchCommunityPredicate);
    UID searchRequestUID = uidService.nextUID();
    myUIDs.add(searchRequestUID);
    blackboard.publishAdd(new SearchCommunity(null,
                                              "(CommunityType=" + COMMUNITY_TYPE + ")",
                                              false,
                                              searchRequestUID,
                                              Community.COMMUNITIES_ONLY));

    // Start timer to periodically check status of agents
    String updateIntervalStr =
        System.getProperty(STATUS_UPDATE_PROPERTY, DEFAULT_STATUS_UPDATE_INTERVAL);
    updateInterval = Long.parseLong(updateIntervalStr);
    wakeAlarm = new WakeAlarm(now() + updateInterval);
    alarmService.addRealTimeAlarm(wakeAlarm);
  }

  /**
   * Return current time in milliseconds.
   * @return Current time
   */
  private long now() {
    return System.currentTimeMillis();
  }

  public void execute() {
    if ((wakeAlarm != null) &&
        ((wakeAlarm.hasExpired()))) {
      // Determine identify node and host
      if (myNode == null && getTopologyFlag) {
        getTopologyFlag = false;
        getTopologyInfo();
      }

      if (myNode != null && !joinedStartupCommunity) {
        joinStartupCommunity();
      }
      updateAndSendNodeStatus();
      long tmp = getLongAttribute("UPDATE_INTERVAL", updateInterval);
      if (tmp != updateInterval) {
        logger.info("Changing update interval: old=" + updateInterval + " new=" + tmp);
        updateInterval = tmp;
      }
      wakeAlarm = new WakeAlarm(now() + getLongAttribute("UPDATE_INTERVAL", updateInterval));
      alarmService.addRealTimeAlarm(wakeAlarm);
    }
    // Get updates in monitored community
    for (Iterator it = searchRequests.getChangedCollection().iterator(); it.hasNext(); ) {
      SearchCommunity cs = (SearchCommunity) it.next();
      Collection robustnessCommunities = (Collection)cs.getResponse().getContent();
      if (robustnessCommunities != null) {
        for (Iterator it1 = robustnessCommunities.iterator(); it1.hasNext();) {
          Community c = (Community)it1.next();
          logger.debug("Received changed SearchCommunity:" +
                      " community=" + (c != null ? c.getName() : null));
        }
        // update status model
        processCommunityChanges(robustnessCommunities);
      }
    }
    // Get status from health monitor peers
    Collection nsCollection = nodeStatusRelaySub.getAddedCollection();
    for (Iterator it = nsCollection.iterator(); it.hasNext(); ) {
      NodeStatusRelay nsr = (NodeStatusRelay)it.next();
      logger.debug("Received added NodeStatusRelay:" +
                  " community=" + nsr.getCommunityName() +
                  " source=" + nsr.getSource() +
                  " numAgents=" + nsr.getAgentStatus().length +
                  " leaderVote=" + nsr.getLeaderVote());
      // update status model
      updateCommunityStatus(nsr.getCommunityName(),
                            nsr.getSource().toString(),
                            nsr.getNodeStatus(),
                            nsr.getAgentStatus(),
                            nsr.getLeaderVote(),
                            nsr.getLocation());
    }
    nsCollection = nodeStatusRelaySub.getChangedCollection();
    for (Iterator it = nsCollection.iterator(); it.hasNext(); ) {
      NodeStatusRelay nsr = (NodeStatusRelay)it.next();
      logger.debug("Received changed NodeStatusRelay:" +
                  " community=" + nsr.getCommunityName() +
                  " source=" + nsr.getSource() +
                  " numAgents=" + nsr.getAgentStatus().length +
                  " leaderVote=" + nsr.getLeaderVote());
     // update status model
      updateCommunityStatus(nsr.getCommunityName(),
                            nsr.getSource().toString(),
                            nsr.getNodeStatus(),
                            nsr.getAgentStatus(),
                            nsr.getLeaderVote(),
                            nsr.getLocation());
    }

    // Get HealthMonitorRequests
    for (Iterator it = healthMonitorRequests.getAddedCollection().iterator(); it.hasNext(); ) {
      HealthMonitorRequest hsm = (HealthMonitorRequest)it.next();
      logger.debug("Received HealthMonitorRequest:" + hsm);
      switch (hsm.getRequestType()) {
        case HealthMonitorRequest.MOVE:
          break;
        case HealthMonitorRequest.GET_STATUS:
          int respStatus = HealthMonitorResponse.FAIL;
          String respObj = null;
          if (model != null) {
            respStatus = HealthMonitorResponse.SUCCESS;
            respObj = controller.getCompleteStatus();
          }
          hsm.setResponse(new HealthMonitorResponse(respStatus, respObj));
          blackboard.publishChange(hsm);
      }
    }

  }

  /**
   * Finds peer health monitors based on entity attributes in monitored community.
   * @param communityName  Name of monitored community
   * @return  Set of peer agent/node health monitors
   */
  private Set findHealthMonitorPeers(String communityName) {
    Community community = commSvc.getCommunity(communityName, null);
    if (community != null && community.getName().equals(communityName)) {
      return findHealthMonitorPeers(community);
    }
    return Collections.EMPTY_SET;
  }

  /**
   * Returns set of MessageAddress objects corresponding to member nodes of
   * specified community.
   */
  private Set findHealthMonitorPeers(Community community) {
    Set nodes = new HashSet();
    Set entities = community.search("(Role=" + HEALTH_MONITOR_ROLE + ")",
                                    Community.AGENTS_ONLY);
    for (Iterator it1 = entities.iterator(); it1.hasNext(); ) {
      Entity entity = (Entity) it1.next();
      nodes.add(SimpleMessageAddress.getSimpleMessageAddress(entity.getName()));
    }
    return nodes;
  }

  /**
   * Update set of communities to monitor.
   */
  private void processCommunityChanges(Collection communities) {
    for(Iterator it = communities.iterator(); it.hasNext(); ) {
      Community community = (Community)it.next();
      if (model == null) {
        initializeModel(community.getName());
      }
      if (nodeStatusRelay == null && myType.equalsIgnoreCase("Node")) {
        AgentStatus agentStatus[] = getLocalAgentStatus(community.getName());
        NodeStatusRelayImpl nsr =
            new NodeStatusRelayImpl(agentId,
                                    community.getName(),
                                    controller.getNormalState(),
                                    agentStatus,
                                    null,
                                    myHost,
                                    uidService.nextUID());
        nodeStatusRelay = new RelayAdapter(agentId, nsr, nsr.getUID());

        Set targets = findHealthMonitorPeers(community);
        for(Iterator it1 = targets.iterator(); it1.hasNext(); ) {
          MessageAddress target = (MessageAddress)it1.next();
          if(!target.equals(agentId))
            nodeStatusRelay.addTarget(target);
        }

        blackboard.publishAdd(nodeStatusRelay);
        if(logger.isDebugEnabled()) {
          logger.debug("publishAdd NodeStatusRelay:" +
                       " targets=" + targetsToString(nodeStatusRelay.getTargets()) +
                       " community=" + community.getName() +
                       " agents=" + agentStatus.length);
        }
      }
      model.update(community);
    }
  }

  private void initializeModel(String communityName) {
    if (model == null) {
      model = new CommunityStatusModel(myName,
                                       communityName,
                                       getBindingSite());
      blackboard.publishAdd(model);
      String controllerClassname =
          System.getProperty(CONTROLLER_CLASS_PROPERTY,
                             DEFAULT_ROBUSTNESS_CONTROLLER_CLASSNAME);
      try {
        controller =
            (RobustnessController)Class.forName(controllerClassname).newInstance();
        controller.initialize(agentId, getBindingSite(), model);
      } catch (Exception ex) {
        logger.error("Exception creating RobustnessController", ex);
      }
      model.setController(controller);
      model.addChangeListener(controller);
    }
    if (logger.isInfoEnabled()) {
      logger.info("Monitoring community:" +
                  " community=" + communityName +
                  " controller=" + controller.getClass().getName());
    }
  }

  private AgentStatus[] getLocalAgentStatus(String communityName) {
    String agents[] = model.entitiesAtLocation(myName);
    AgentStatus as[] = new AgentStatus[agents.length];
    for (int i = 0; i < as.length; i++) {
      as[i] = new AgentStatus(agents[i], myName, model.getCurrentState(agents[i]));
    }
    return as;
  }

  /**
   * Get community attribute from model.
   * @param id  Attribute identifier
   * @param defaultValue  Default value if attribute not found
   * @return Attribute value as a long
   */
  protected long getLongAttribute(String id, long defaultValue) {
    if (model != null && model.hasAttribute(id)) {
      return model.getLongAttribute(id);
    } else {
      return defaultValue;
    }
  }

  public static String targetsToString(Collection targets) {
    StringBuffer sb = new StringBuffer("[");
    for (Iterator it = targets.iterator(); it.hasNext();) {
      sb.append(it.next());
      if (it.hasNext()) sb.append(",");
    }
    sb.append("]");
    return sb.toString();
  }

  private void updateCommunityStatus(String communityName,
                                     String nodeName,
                                     int nodeStatus,
                                     AgentStatus[] agentStatus,
                                     String leader,
                                     String host) {
    if (model == null) {
      initializeModel(communityName);
    }
    String agentNames[] = new String[agentStatus.length];
    model.applyUpdates(nodeName,
                       nodeStatus,
                       agentStatus,
                       leader,
                       host);
  }

  private String listServices() {
    StringBuffer sb = new StringBuffer();
    for (Iterator it = getServiceBroker().getCurrentServiceClasses(); it.hasNext();) {
      sb.append(((Class)it.next()).getName() + "\n");
    }
    return sb.toString();
  }

  // Converts a collection of Entities to a compact string representation of names
  private String entityNames(Collection entities) {
    StringBuffer sb = new StringBuffer("[");
    for (Iterator it = entities.iterator(); it.hasNext();) {
      Entity entity = (Entity)it.next();
      sb.append(entity.getName() + (it.hasNext() ? "," : ""));
    }
    return(sb.append("]").toString());
  }


  private UnaryPredicate communityPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof Community);
    }
  };

  private IncrementalSubscription searchRequests;
  private UnaryPredicate searchCommunityPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof SearchCommunity) {
        SearchCommunity sc = (SearchCommunity)o;
        return (myUIDs.contains(sc.getUID()));
      }
      return false;
    }
  };

  /**
   * Predicate used to select CommunityManagerRequest relays published locally.
   */
  private IncrementalSubscription nodeStatusRelaySub;
  private UnaryPredicate nodeStatusRelayPredicate = new UnaryPredicate() {
    public boolean execute (Object o) {
      return (o instanceof NodeStatusRelay);
  }};

  private IncrementalSubscription healthMonitorRequests;
  private UnaryPredicate healthMonitorRequestPredicate = new UnaryPredicate() {
    public boolean execute (Object o) {
      return (o instanceof HealthMonitorRequest);
  }};

  private void updateAndSendNodeStatus() {
    if (model != null) {
      String communityName = model.getCommunityName();
      // Get status of local agents
      AgentStatus agentStatus[] = getLocalAgentStatus(communityName);

      // Dissemminate status to peer managers
      if (nodeStatusRelay != null) {
       NodeStatusRelayImpl nsr = (NodeStatusRelayImpl)nodeStatusRelay.
            getContent();
        nsr.setAgentStatus(agentStatus);
        nsr.setLeaderVote(model.getLeaderVote(myName));
        Set targets = findHealthMonitorPeers(nsr.getCommunityName());
        for (Iterator it1 = targets.iterator(); it1.hasNext(); ) {
          MessageAddress target = (MessageAddress)it1.next();
          if (!target.equals(agentId))
            nodeStatusRelay.addTarget(target);
        }
        logger.debug("publishChange NodeStatusRelay:" +
                     " source=" + nsr.getSource() +
                     " targets=" + targetsToString(nodeStatusRelay.getTargets()) +
                     " community=" + nsr.getCommunityName() +
                     " agents=" + nsr.getAgentStatus().length +
                     " leaderVote=" + nsr.getLeaderVote() +
                     " location=" + nsr.getLocation());
        blackboard.publishChange(nodeStatusRelay);
      }
    }
  }

  /** Find component info using white pages */
  private void getTopologyInfo() {
    Callback cb = new Callback() {
      public void execute(Response resp) {
        boolean isAvailable = resp.isAvailable();
        boolean isSuccess = resp.isSuccess();
        AddressEntry entry = null;
        String agentName = null;
        if (isAvailable && isSuccess) {
          entry = ((Response.Get)resp).getAddressEntry();
        }
        if (entry != null) {
          try {
            URI uri = entry.getURI();
            myHost = uri.getHost();
            myNode = uri.getPath().substring(1);
            myType = (myName.equals(myNode)) ? "Node" : "Agent";
            if (logger.isInfoEnabled()) {
              logger.info("topologyInfo:" +
                          " name=" + myName +
                          " type=" + myType +
                          " host=" + myHost +
                          " node=" + myNode);
            }
          } catch (Exception ex) {
            logger.error("Exception in getTopologyInfo:", ex);
          }
        } else {
          getTopologyFlag = true;
        }
        if (blackboard != null)
          blackboard.signalClientActivity();
        logger.debug("getTopologyInfo callback:" +
                     " name=" + myName +
                     " resp.isAvailable=" + isAvailable +
                     " resp.isSuccess=" + isSuccess +
                     " entry=" + entry);
      }
    };
    whitePagesService.get(myName, "topology", cb);
  }

  private class WakeAlarm implements Alarm {
    private long expiresAt;
    private boolean expired = false;
    public WakeAlarm (long expirationTime) {
      expiresAt = expirationTime;
    }
    public long getExpirationTime() {
      return expiresAt;
    }
    public synchronized void expire() {
      if (!expired) {
        expired = true;
        if (blackboard != null) blackboard.signalClientActivity();
      }
    }
    public boolean hasExpired() {
      return expired;
    }
    public synchronized boolean cancel() {
      boolean was = expired;
      expired = true;
      return was;
    }
  }


}
