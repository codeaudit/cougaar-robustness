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

import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.AlarmService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;
//import org.cougaar.core.node.ComponentInitializerService;

import org.cougaar.core.service.EventService;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.service.ThreadService;

import org.cougaar.core.thread.Schedulable;

import org.cougaar.core.util.UID;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.core.service.community.CommunityService;
import org.cougaar.core.service.community.Community;
import org.cougaar.core.service.community.Agent;
import org.cougaar.core.service.community.Entity;
import org.cougaar.community.requests.JoinCommunity;
import org.cougaar.community.requests.SearchCommunity;

import org.cougaar.core.service.community.CommunityChangeEvent;
import org.cougaar.core.service.community.CommunityChangeListener;
import org.cougaar.core.service.community.CommunityResponseListener;
import org.cougaar.core.service.community.CommunityResponse;

import org.cougaar.tools.robustness.ma.controllers.RobustnessController;
import org.cougaar.tools.robustness.ma.controllers.DefaultRobustnessController;
import org.cougaar.tools.robustness.ma.CommunityStatusModel;

import org.cougaar.tools.robustness.ma.ldm.RelayAdapter;
import org.cougaar.tools.robustness.ma.ldm.AgentStatus;
import org.cougaar.tools.robustness.ma.ldm.NodeStatusRelay;
import org.cougaar.tools.robustness.ma.ldm.NodeStatusRelayImpl;
import org.cougaar.tools.robustness.ma.ldm.HealthMonitorRequest;
import org.cougaar.tools.robustness.ma.ldm.HealthMonitorResponse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;

import java.net.URI;

public class NodeHealthMonitorPlugin extends ComponentPlugin {

  public static final String COMMUNITY_PROP_NAME = "org.cougaar.tools.robustness.community";
  public static final String HEALTH_MONITOR_ROLE = "HealthMonitor";
  public static final String COMMUNITY_TYPE = "Robustness";
  public static final String ENTITY_TYPE = "Node";

  public static final long NODE_CHECK_INTERVAL = 10 * 1000;

  private String NODE_NAME;

  CommunityStatusModel model;
  RobustnessController controller;
  RelayAdapter nodeStatusRelay;

  private Set myUIDs = new HashSet();

  // Services used
  protected LoggingService logger;
  private UIDService uidService = null;
  private EventService eventService;

  private CommunityService commSvc;

  protected void setupSubscriptions() {
    NODE_NAME = agentId.toString();

    // Get required services
    logger =
      (LoggingService)getBindingSite().getServiceBroker().getService(this, LoggingService.class, null);
    logger = org.cougaar.core.logging.LoggingServiceWithPrefix.add(logger, agentId + ": ");

    uidService =
      (UIDService) getBindingSite().getServiceBroker().getService(this, UIDService.class, null);

    eventService =
      (EventService) getBindingSite().getServiceBroker().getService(this, EventService.class, null);

    commSvc =
      (CommunityService) getBindingSite().getServiceBroker().getService(this, CommunityService.class, null);

    // Join Robustness Community designated by startup parameter
    String initialCommunity = System.getProperty(COMMUNITY_PROP_NAME);
    if (initialCommunity != null) {
      logger.debug(COMMUNITY_PROP_NAME + "=" + initialCommunity);
      UID joinRequestUID = uidService.nextUID();
      myUIDs.add(joinRequestUID);
      Attributes memberAttrs = new BasicAttributes();
      Attribute roles = new BasicAttribute("Role", "Member");
      roles.add(HEALTH_MONITOR_ROLE);
      memberAttrs.put(roles);
      memberAttrs.put("EntityType", ENTITY_TYPE);
      blackboard.publishAdd(new JoinCommunity(initialCommunity,
                                              NODE_NAME,
                                              CommunityService.AGENT,
                                              memberAttrs,
                                              false,
                                              null,
                                              joinRequestUID));
    }

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
    getAlarmService().addRealTimeAlarm(new NodeCheckTimer(NODE_CHECK_INTERVAL));
  }

  protected void execute() {
    for (Iterator it = searchRequests.getChangedCollection().iterator(); it.hasNext(); ) {
      SearchCommunity cs = (SearchCommunity) it.next();
      Collection robustnessCommunities = (Collection)cs.getResponse().getContent();
      for (Iterator it1 = robustnessCommunities.iterator(); it1.hasNext();) {
        Community c = (Community)it1.next();
        logger.debug("Received changed SearchCommunity:" +
                    " community=" + c.getName());
      }
      processCommunityChanges(robustnessCommunities);
    }
    // Get status from other node agents in community
    Collection nsCollection = nodeStatusRelaySub.getAddedCollection();
    for (Iterator it = nsCollection.iterator(); it.hasNext(); ) {
      NodeStatusRelay nsr = (NodeStatusRelay)it.next();
      logger.debug("Received added NodeStatusRelay:" +
                  " community=" + nsr.getCommunityName() +
                  " source=" + nsr.getSource() +
                  " numAgents=" + nsr.getAgentStatus().length +
                  " leaderVote=" + nsr.getLeaderVote());
      updateCommunityStatus(nsr.getCommunityName(),
                            nsr.getSource().toString(),
                            nsr.getNodeStatus(),
                            nsr.getAgentStatus(),
                            nsr.getLeaderVote());
    }
    nsCollection = nodeStatusRelaySub.getChangedCollection();
    for (Iterator it = nsCollection.iterator(); it.hasNext(); ) {
      NodeStatusRelay nsr = (NodeStatusRelay)it.next();
      logger.debug("Received changed NodeStatusRelay:" +
                  " community=" + nsr.getCommunityName() +
                  " source=" + nsr.getSource() +
                  " numAgents=" + nsr.getAgentStatus().length +
                  " leaderVote=" + nsr.getLeaderVote());
      updateCommunityStatus(nsr.getCommunityName(),
                            nsr.getSource().toString(),
                            nsr.getNodeStatus(),
                            nsr.getAgentStatus(),
                            nsr.getLeaderVote());
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
   * Get name of all agents in this node.
   */
  private Set getAgentsInNode(String communityName) {
    Set agentsInNode = new HashSet();
    String namesFromModel[] = new String[0];
    if (model != null) {
      namesFromModel = model.agentsOnNode(NODE_NAME);
      for (int i = 0; i < namesFromModel.length; i++) {
        agentsInNode.add(namesFromModel[i]);
      }
    }
      logger.debug("AgentsInNode:" +
                   " agentsInNode=" + agentsInNode.size() + agentsInNode +
                   " agentsFromModel=" + namesFromModel.length);
    return agentsInNode;
  }

  private Set getHealthMonitorPeers(String communityName) {
    Community community = commSvc.getCommunity(communityName, null);
      if (community.getName().equals(communityName)) {
        return getHealthMonitorPeers(community);
      }
    return Collections.EMPTY_SET;
  }

  /**
   * Returns set of MessageAddress objects corresponding to member nodes of
   * specified community.
   */
   private Set getHealthMonitorPeers(Community community) {
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
    for (Iterator it = communities.iterator(); it.hasNext(); ) {
      Community community = (Community)it.next();
      if (model == null) {
        if (logger.isInfoEnabled()) {
          logger.info("Monitoring community: " + community.getName());
        }
        initializeModel(community.getName());
        AgentStatus agentStatus[] = getLocalAgentStatus(community.getName());
        NodeStatusRelayImpl nsr =
            new NodeStatusRelayImpl(agentId,
                                    community.getName(),
                                    controller.getNormalState(),
                                    agentStatus,
                                    null,
                                    uidService.nextUID());
        nodeStatusRelay = new RelayAdapter(agentId, nsr, nsr.getUID());

        Set targets = getHealthMonitorPeers(community);
        for (Iterator it1 = targets.iterator(); it1.hasNext(); ) {
          MessageAddress target = (MessageAddress)it1.next();
          if (!target.equals(agentId))
            nodeStatusRelay.addTarget(target);
        }

        blackboard.publishAdd(nodeStatusRelay);
        if (logger.isDebugEnabled()) {
          logger.debug("publishAdd NodeStatusRelay:" +
                       " targets=" + targetsToString(nodeStatusRelay.getTargets()) +
                       " community=" + community.getName() +
                       " agents=" + agentStatus.length);
        }
      }
      if (model != null)
        model.update(community);
    }
  }

  private void initializeModel(String communityName) {
    if (model == null) {
      model = new CommunityStatusModel(NODE_NAME,
                                       communityName,
                                       getServiceBroker());
      controller =
          new DefaultRobustnessController(NODE_NAME, getBindingSite(), model);
      model.setController(controller);
      model.addChangeListener(controller);
      if (logger.isInfoEnabled()) {
        logger.debug("Adding community to status map:" +
                     " community=" + communityName);
      }
    }
  }

  private AgentStatus[] getLocalAgentStatus(String communityName) {
    String agents[] = model.agentsOnNode(NODE_NAME);
    AgentStatus as[] = new AgentStatus[agents.length];
    for (int i = 0; i < as.length; i++) {
      as[i] = new AgentStatus(agents[i], NODE_NAME, model.getCurrentState(agents[i]));
    }
    return as;
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
                                     String leader) {
    if (model == null) {
      initializeModel(communityName);
    }
    String agentNames[] = new String[agentStatus.length];
    model.applyUpdates(nodeName,
                       nodeStatus,
                       agentStatus,
                       leader);
  }

  private String listServices() {
    StringBuffer sb = new StringBuffer();
    for (Iterator it = getServiceBroker().getCurrentServiceClasses(); it.hasNext();) {
      sb.append(((Class)it.next()).getName() + "\n");
    }
    return sb.toString();
  }

  /**
   * Sends Cougaar event via EventService.
   */
  private void event(String message) {
    if (eventService != null && eventService.isEventEnabled())
      eventService.event(message);
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
        nsr.setLeaderVote(model.getLeaderVote(NODE_NAME));
        Set targets = getHealthMonitorPeers(nsr.getCommunityName());
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
                     " leaderVote=" + nsr.getLeaderVote());
        blackboard.openTransaction();
        blackboard.publishChange(nodeStatusRelay);
        blackboard.closeTransaction();
      } else {
        //logger.info("RelayAdapter not found for community " +
        //            communityName);
      }
    }
  }

  /**
   * Timer used trigger periodic check of node composition and state.
   */
  private class NodeCheckTimer implements Alarm {
    private long expirationTime = -1;
    private boolean expired = false;
    public NodeCheckTimer (long delay) {
      expirationTime = delay + System.currentTimeMillis();
    }

    public void expire() {
      if (!expired) {
        try {
          ThreadService ts =
              (ThreadService)getServiceBroker().getService(this, ThreadService.class, null);
          Schedulable nodeStatusThread = ts.getThread(this, new Runnable() {
            public void run() {
              updateAndSendNodeStatus();
            }
          }, "NodeStatusThread");
          getServiceBroker().releaseService(this, ThreadService.class, ts);
          nodeStatusThread.start();
        } finally {
          AlarmService as = (AlarmService)getServiceBroker().getService(this,
              AlarmService.class, null);
          getAlarmService().addRealTimeAlarm(new NodeCheckTimer(
              NODE_CHECK_INTERVAL));
          getServiceBroker().releaseService(this, AlarmService.class, as);
          expired = true;
        }
      }
    }

    public long getExpirationTime() {
      return expirationTime;
    }

    public boolean hasExpired() {
      return expired;
    }

    public synchronized boolean cancel() {
      if (!expired)
        return expired = true;
      return false;
    }
}

}