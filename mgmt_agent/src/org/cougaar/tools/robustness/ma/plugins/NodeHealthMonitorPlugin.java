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
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.SimpleMessageAttributes;

import org.cougaar.mts.std.Constants;

import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;

import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.service.wp.Callback;
import org.cougaar.core.service.wp.Response;

import org.cougaar.core.node.NodeControlService;

import org.cougaar.core.agent.service.alarm.Alarm;

import org.cougaar.core.util.UID;

import org.cougaar.core.blackboard.IncrementalSubscription;

import org.cougaar.util.UnaryPredicate;

import org.cougaar.core.service.community.Community;
import org.cougaar.core.service.community.CommunityService;
import org.cougaar.core.service.community.CommunityChangeEvent;
import org.cougaar.core.service.community.CommunityChangeListener;
import org.cougaar.core.service.community.CommunityResponseListener;
import org.cougaar.core.service.community.CommunityResponse;
import org.cougaar.core.service.community.Entity;

import org.cougaar.tools.robustness.ma.RestartManagerConstants;
import org.cougaar.tools.robustness.ma.controllers.RobustnessController;
import org.cougaar.tools.robustness.ma.CommunityStatusModel;
import org.cougaar.tools.robustness.ma.ldm.RelayAdapter;
import org.cougaar.tools.robustness.ma.ldm.AgentStatus;
import org.cougaar.tools.robustness.ma.ldm.NodeStatusRelay;
import org.cougaar.tools.robustness.ma.ldm.NodeStatusRelayImpl;
import org.cougaar.tools.robustness.ma.ldm.HealthMonitorRequest;
import org.cougaar.tools.robustness.ma.ldm.HealthMonitorResponse;

import org.cougaar.tools.robustness.ma.ReaffiliationNotificationHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
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
 * </pre>
 */
public class NodeHealthMonitorPlugin extends ComponentPlugin
    implements RestartManagerConstants {

  public static long updateInterval;
  public static List statusListeners = new ArrayList();

  // Info associated with this health monitor
  private String myName;
  private String myType = "Agent";
  private String myNode;
  private String myHost;
  private String myCommunity;
  private boolean joinedStartupCommunity = false;
  private boolean getTopologyFlag = true;
  public boolean communityChanged = false;

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
  private CommunityService commSvc;
  private WhitePagesService whitePagesService;

  private NodeControlService nodeControlService;

  public void setWhitePagesService(WhitePagesService wps) {
    whitePagesService = wps;
  }

  public void setCommunityService(CommunityService cs) {
    commSvc = cs;
  }

  // Join Robustness Community designated by startup parameter
  private void joinStartupCommunity() {
    final String initialCommunity = System.getProperty(COMMUNITY_PROPERTY);
    if (initialCommunity != null) {
      if (logger.isDebugEnabled()) {
        logger.debug("Joining community " + initialCommunity);
      }
      UID joinRequestUID = uidService.nextUID();
      myUIDs.add(joinRequestUID);
      Attributes memberAttrs = new BasicAttributes();
      Attribute roles = new BasicAttribute(ROLE_ATTRIBUTE, "Member");
      roles.add(HEALTH_MONITOR_ROLE);
      memberAttrs.put(roles);
      memberAttrs.put(ENTITY_TYPE_ATTRIBUTE, myType);
      memberAttrs.put("CanBeManager", "False");
       commSvc.joinCommunity(initialCommunity, myName, CommunityService.AGENT,
         memberAttrs, false, null, new CommunityResponseListener() {
           public void getResponse(CommunityResponse resp) {
             if (logger.isDebugEnabled()) {
               logger.debug("joinCommunity:" +
                            " agent=" + myName +
                            " community=" + initialCommunity +
                            " result=" + resp.getStatusAsString());
             }
           }
         });
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug("No initial community defined");
      }
    }
    joinedStartupCommunity = true;
  }

  public NodeControlService getNodeControlService() {
    if (nodeControlService == null) {
      nodeControlService =
          (NodeControlService) getBindingSite().getServiceBroker().getService(this,
          NodeControlService.class, null);
    }
    return nodeControlService;
  }

  public void setupSubscriptions() {
    myName = agentId.toString();

    // Get required services
    logger =
      (LoggingService)getBindingSite().getServiceBroker().getService(this, LoggingService.class, null);
    logger = org.cougaar.core.logging.LoggingServiceWithPrefix.add(logger, agentId + ": ");
    uidService =
      (UIDService) getBindingSite().getServiceBroker().getService(this, UIDService.class, null);

    // Subscribe to Node Status updates sent by peer Health Monitors via Relay
    nodeStatusRelaySub =
        (IncrementalSubscription)blackboard.subscribe(nodeStatusRelayPredicate);
    statusListeners.add(new StatusListener());

    // Subscribe to external requests
    healthMonitorRequests =
        (IncrementalSubscription)blackboard.subscribe(healthMonitorRequestPredicate);

    Collection communities = commSvc.searchCommunity(null,
                                             "(" + COMMUNITY_TYPE_ATTRIBUTE + "=" +
                                             ROBUSTNESS_COMMUNITY_TYPE + ")",
                                             false,
                                             Community.COMMUNITIES_ONLY,
                                             null);
    if (communities != null && !communities.isEmpty()) {
      processCommunityChanges(communities);
    }
    commSvc.addListener(new CommunityChangeListener() {
      public String getCommunityName() { return null; }
      public void communityChanged(CommunityChangeEvent cce) {
        Attributes attrs = cce.getCommunity().getAttributes();
        Attribute attr = attrs.get(COMMUNITY_TYPE_ATTRIBUTE);
        if (attr != null && attr.contains(ROBUSTNESS_COMMUNITY_TYPE)) {
           myCommunity = cce.getCommunityName();
           communityChanged = true;
        }
      }
    });

    // Start timer to periodically check status of agents
    String updateIntervalStr =
        System.getProperty(STATUS_UPDATE_INTERVAL_ATTRIBUTE,
                           Long.toString(DEFAULT_STATUS_UPDATE_INTERVAL));
    updateInterval = Long.parseLong(updateIntervalStr);
    if (logger.isDebugEnabled()) {
      logger.debug("updateInterval=" + updateInterval + " default=" + DEFAULT_STATUS_UPDATE_INTERVAL);
    }
    wakeAlarm = new WakeAlarm(now() + updateInterval * MS_PER_MIN);
    alarmService.addRealTimeAlarm(wakeAlarm);
    new ReaffiliationNotificationHandler(getBindingSite(), agentId);
  }

  /**
   * Return current time in milliseconds.
   * @return Current time
   */
  private long now() {
    return System.currentTimeMillis();
  }

  public void execute() {
    if ((wakeAlarm != null) && wakeAlarm.hasExpired()) {
      // Determine identify node and host
      if (myNode == null && getTopologyFlag) {
        getTopologyFlag = false;
        getTopologyInfo();
      }

      if (myNode != null && !joinedStartupCommunity) {
        joinStartupCommunity();
      }
      if (communityChanged && commSvc != null) {
        communityChanged = false;
        Community community = commSvc.getCommunity(myCommunity,
          new CommunityResponseListener() {
            public void getResponse(CommunityResponse resp) {
              processCommunityChanges(Collections.singleton(resp.getContent()));
            }
          });
        if (community != null) {
          processCommunityChanges(Collections.singleton(community));
        }
      }
      updateAndSendNodeStatus();
      long tmp = getLongAttribute(STATUS_UPDATE_INTERVAL_ATTRIBUTE, updateInterval);
      if (tmp != updateInterval) {
        if (logger.isInfoEnabled()) {
          logger.info("Changing update interval: old=" + updateInterval +
                      " new=" + tmp);
        }
        updateInterval = tmp;
      }
      if (model != null) {
        model.doPeriodicTasks();
      }
      wakeAlarm = new WakeAlarm(now() +
                                getLongAttribute(STATUS_UPDATE_INTERVAL_ATTRIBUTE, updateInterval) * MS_PER_MIN);
      alarmService.addRealTimeAlarm(wakeAlarm);
    }

    // Get status from health monitor peers
    Collection nsCollection = nodeStatusRelaySub.getAddedCollection();
    for (Iterator it = nsCollection.iterator(); it.hasNext(); ) {
      NodeStatusRelay nsr = (NodeStatusRelay)it.next();
      if (logger.isDebugEnabled()) {
        logger.debug("Received added NodeStatusRelay:" +
                     " community=" + nsr.getCommunityName() +
                     " source=" + nsr.getSource() +
                     " numAgents=" + nsr.getAgentStatus().length +
                     " leaderVote=" + nsr.getLeaderVote() +
                     " listeners=" + statusListeners.size());
      }
      // send status updates to listeners
      for (Iterator it1 = statusListeners.iterator(); it1.hasNext();) {
        StatusListener listener = (StatusListener)it1.next();
        listener.update(nsr);
      }
    }
    nsCollection = nodeStatusRelaySub.getChangedCollection();
    for (Iterator it = nsCollection.iterator(); it.hasNext(); ) {
      NodeStatusRelay nsr = (NodeStatusRelay)it.next();
      if (logger.isDebugEnabled()) {
        logger.debug("Received changed NodeStatusRelay:" +
                     " community=" + nsr.getCommunityName() +
                     " source=" + nsr.getSource() +
                     " numAgents=" + nsr.getAgentStatus().length +
                     " leaderVote=" + nsr.getLeaderVote() +
                     " listeners=" + statusListeners.size());
      }
      // send status updates to listeners
      for (Iterator it1 = statusListeners.iterator(); it1.hasNext();) {
        StatusListener listener = (StatusListener)it1.next();
        listener.update(nsr);
      }
    }

    // Get HealthMonitorRequests
    for (Iterator it = healthMonitorRequests.getAddedCollection().iterator(); it.hasNext(); ) {
      HealthMonitorRequest hsm = (HealthMonitorRequest)it.next();
      switch (hsm.getRequestType()) {
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
    Set targets = new HashSet();
    Set entities = community.search("(" + ROLE_ATTRIBUTE + "=" + HEALTH_MONITOR_ROLE + ")",
                                    Community.AGENTS_ONLY);
    for (Iterator it1 = entities.iterator(); it1.hasNext(); ) {
      Entity entity = (Entity) it1.next();
      //targets.add(MessageAddress.getMessageAddress(entity.getName()));
      if (model != null && model.getType(entity.getName()) == model.NODE) {
        targets.add(getMessageAddressWithTimeout(entity.getName(),
                                                 updateInterval * MS_PER_MIN));
      }
     }
    return targets;
  }

  private boolean initialAttributesLogged = false;
  /**
   * Update set of communities to monitor.
   */
  private void processCommunityChanges(Collection communities) {
    for(Iterator it = communities.iterator(); it.hasNext(); ) {
      Community community = (Community)it.next();
      if (model == null || !community.getName().equals(model.getCommunityName())) {
        //blackboard.openTransaction();
        initializeModel(community.getName());
        //blackboard.closeTransaction();
      }
      model.update(community);
      if (!initialAttributesLogged) {
        initialAttributesLogged = true;
        if (logger.isInfoEnabled()) {
          logger.info("Robustness community attributes:" +
                      " community=" + community.getName() +
                      " attributes=" + attrsToString(community.getAttributes()));
        }
      }
      long interval = model.getLongAttribute(STATUS_UPDATE_INTERVAL_ATTRIBUTE);
      if (interval <= 0) { // Current status update interval not defined yet
        // Get default interval
        long newInterval = getLongAttribute(STATUS_UPDATE_INTERVAL_ATTRIBUTE, updateInterval);
        if (newInterval > 0) {
          // Add to community
          if (logger.isDebugEnabled()) {
            logger.debug("Setting update interval attribute: interval=" +
                         newInterval);
          }
          changeAttributes(community,
                           null,
                           new Attribute[]{new BasicAttribute(STATUS_UPDATE_INTERVAL_ATTRIBUTE, Long.toString(newInterval))});
        }
      }
      if (nodeStatusRelay == null &&
          myType.equalsIgnoreCase("Node") &&
          controller != null) {
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
          if(!target.equals(agentId)) {
            nodeStatusRelay.addTarget(target);
          }
        }
        blackboard.publishAdd(nodeStatusRelay);
        if (logger.isDetailEnabled()) {
          logger.detail("publishAdd NodeStatusRelay:" +
                       " targets=" + targetsToString(nodeStatusRelay.getTargets()) +
                       " community=" + community.getName() +
                       " agents=" + detailedAgentStatus(nsr.getAgentStatus()));
        } else if (logger.isDebugEnabled()) {
          logger.debug("publishAdd NodeStatusRelay:" +
                       " targets=" + targetsToString(nodeStatusRelay.getTargets()) +
                       " community=" + community.getName() +
                       " agents=" + agentStatus.length);
        }
      }
    }
  }

  private synchronized void initializeModel(String communityName) {
    if (logger.isDebugEnabled()) {
      logger.debug("Initialize CommunityStatusModel");
    }
    if (model != null) {
      blackboard.publishRemove(model);
      model = null;
      controller = null;
      blackboard.publishRemove(nodeStatusRelay);
      nodeStatusRelay = null;
    }
    model = new CommunityStatusModel(myName,
                                     communityName,
                                     getBindingSite());
    String controllerClassname =
        System.getProperty(CONTROLLER_CLASS_PROPERTY,
                           DEFAULT_CONTROLLER_CLASSNAME);
    try {
      controller =
          (RobustnessController) Class.forName(controllerClassname).newInstance();
      controller.initialize(agentId, getBindingSite(), model);
    } catch (Exception ex) {
      if (logger.isErrorEnabled()) {
        logger.error("Exception creating RobustnessController", ex);
      }
    }
    model.setController(controller);
    model.addChangeListener(controller);
    blackboard.publishAdd(model);

    if (logger.isInfoEnabled()) {
      logger.info("Monitoring community:" +
                  " community=" + communityName +
                  " controller=" + controller.getClass().getName());
    }
  }

  private MessageAddress getMessageAddressWithTimeout(String target, long timeout) {
    MessageAttributes attrs = new SimpleMessageAttributes();
    attrs.setAttribute(Constants.SEND_TIMEOUT, new Integer((int)timeout));
    return MessageAddress.getMessageAddress(target, attrs);
  }

  private AgentStatus[] getLocalAgentStatus(String communityName) {
    List l = new ArrayList();
    Community community = commSvc.getCommunity(communityName, null);
    if (community != null) {
      // Agents found using NodeControlService
      Set agents = listLocalAgents();
      for (Iterator it = agents.iterator(); it.hasNext();) {
        String agentName = (String)it.next();
        if (community.hasEntity(agentName) && !myName.equals(agentName)) {
          int state = model.getCurrentState(agentName);
          state = state < model.INITIAL ? model.INITIAL : state;
          l.add(new AgentStatus(agentName,
                                ((Long)agentVersions.get(agentName)).longValue(),
                                state));
        }
      }
    }
    return (AgentStatus[])l.toArray(new AgentStatus[0]);
  }

  Map agentVersions = Collections.synchronizedMap(new HashMap());

  private Set listLocalAgents() {
    Set names = new HashSet();
    NodeControlService ncs = getNodeControlService();
    if (ncs != null) {
      Set agentAddresses = ncs.getRootContainer().getAgentAddresses();
      for (Iterator it = agentAddresses.iterator(); it.hasNext(); ) {
        String agent = it.next().toString();
        names.add(agent);
        if (!agentVersions.containsKey(agent)) {
          agentVersions.put(agent, new Long(now()));
          if (logger.isDebugEnabled()) {
            logger.debug("New agent detected: agent=" + agent);
          }
        }
      }
    }
    return names;
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
    if (model != null && controller != null) {
      model.applyUpdates(nodeName,
                         nodeStatus,
                         agentStatus,
                         leader,
                         host);
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug("Model not initialized");
      }
    }
  }

  private UnaryPredicate communityPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof Community);
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
      if (model.contains(myName)) {
        // Get status of local agents
        AgentStatus agentStatus[] = getLocalAgentStatus(communityName);

        // Dissemminate status to peer managers
        if (nodeStatusRelay != null) {
          NodeStatusRelayImpl nsr = (NodeStatusRelayImpl) nodeStatusRelay.
              getContent();
          nsr.setAgentStatus(agentStatus);
          nsr.setLeaderVote(model.getLeaderVote(myName));
          Set targets = findHealthMonitorPeers(nsr.getCommunityName());
          nodeStatusRelay.clearTargets();
          for (Iterator it1 = targets.iterator(); it1.hasNext(); ) {
            MessageAddress target = (MessageAddress) it1.next();
            if (!target.equals(agentId))
              nodeStatusRelay.addTarget(target);
          }
          // send status updates to listeners
          for (Iterator it1 = statusListeners.iterator(); it1.hasNext();) {
            StatusListener listener = (StatusListener)it1.next();
            listener.update(nsr);
          }
          if (logger.isDetailEnabled()) {
            logger.detail("publishChange NodeStatusRelay:" +
                         " source=" + nsr.getSource() +
                         " targets=" +
                         targetsToString(nodeStatusRelay.getTargets()) +
                         " community=" + nsr.getCommunityName() +
                         " agents=" + detailedAgentStatus(nsr.getAgentStatus()) +
                         " leaderVote=" + nsr.getLeaderVote() +
                         " location=" + nsr.getLocation());
          } else if (logger.isDebugEnabled()) {
            logger.debug("publishChange NodeStatusRelay:" +
                         " source=" + nsr.getSource() +
                         " targets=" +
                         targetsToString(nodeStatusRelay.getTargets()) +
                         " community=" + nsr.getCommunityName() +
                         " agents=" + nsr.getAgentStatus().length +
                         " leaderVote=" + nsr.getLeaderVote() +
                         " location=" + nsr.getLocation());
          }
          blackboard.publishChange(nodeStatusRelay);
        }
      }
    }
  }

  private String detailedAgentStatus(AgentStatus[] as) {
    StringBuffer sb = new StringBuffer("[");
    for (int i = 0; i < as.length; i++) {
      sb.append(as[i].getName() + ":" + controller.stateName(as[i].getStatus()));
      if (i < as.length - 1) {
        sb.append(", ");
      }
    }
    sb.append("]");
    return sb.toString();
  }

  /** Find component info using white pages */
  private void getTopologyInfo() {
    Callback cb = new Callback() {
      public void execute(Response resp) {
        boolean isAvailable = resp.isAvailable();
        boolean isSuccess = resp.isSuccess();
        AddressEntry entry = null;
        if (isAvailable && isSuccess) {
          entry = ((Response.Get)resp).getAddressEntry();
        }
        if (entry != null) {
          try {
            URI uri = entry.getURI();
            myHost = uri.getHost();
            myNode = uri.getPath().substring(1);
            myType = (myName.equals(myNode)) ? "Node" : "Agent";
            communityChanged = true;
            wakeAlarm.expire();
            if (logger.isInfoEnabled()) {
              logger.info("topologyInfo:" +
                          " name=" + myName +
                          " type=" + myType +
                          " host=" + myHost +
                          " node=" + myNode);
            }
          } catch (Exception ex) {
            if (logger.isErrorEnabled()) {
              logger.error("Exception in getTopologyInfo:", ex);
            }
          }
        } else {
          getTopologyFlag = true;
        }
        if (blackboard != null) {
          blackboard.signalClientActivity();
        }
        if (logger.isDebugEnabled()) {
          logger.debug("getTopologyInfo callback:" +
                       " name=" + myName +
                       " resp.isAvailable=" + isAvailable +
                       " resp.isSuccess=" + isSuccess +
                       " entry=" + entry);
        }
      }
    };
    whitePagesService.get(myName, "topology", cb);
  }

  /**
   * Modify one or more attributes of a community or entity.
   * @param communityName  Target community
   * @param entityName     Name of entity or null to modify community attributes
   * @param newAttrs       New attributes
   */
  protected void changeAttributes(String communityName, final String entityName, final Attribute[] newAttrs) {
    Community community =
      commSvc.getCommunity(communityName, new CommunityResponseListener() {
        public void getResponse(CommunityResponse resp) {
          changeAttributes((Community) resp.getContent(), entityName, newAttrs);
        }
      }
    );
    if (community != null) {
      changeAttributes(community, entityName, newAttrs);
    }
  }

  /**
   * Modify one or more attributes of a community or entity.
   * @param community      Target community
   * @param entityName     Name of entity or null to modify community attributes
   * @param newAttrs       New attributes
   */
  protected void changeAttributes(final Community community, final String entityName, Attribute[] newAttrs) {
    if (community != null) {
      List mods = new ArrayList();
      for (int i = 0; i < newAttrs.length; i++) {
        try {
          Attributes attrs = community.getAttributes();
          Attribute attr = attrs.get(newAttrs[i].getID());
          if (attr == null || !attr.contains(newAttrs[i].get())) {
            int type = attr == null
                ? DirContext.ADD_ATTRIBUTE
                : DirContext.REPLACE_ATTRIBUTE;
            mods.add(new ModificationItem(type, newAttrs[i]));
          }
        } catch (NamingException ne) {
          if (logger.isErrorEnabled()) {
            logger.error("Error setting community attribute:" +
                         " community=" + community.getName() +
                         " attribute=" + newAttrs[i]);
          }
        }
      }
      if (!mods.isEmpty()) {
        CommunityResponseListener crl = new CommunityResponseListener() {
          public void getResponse(CommunityResponse resp) {
            if (resp.getStatus() != CommunityResponse.SUCCESS) {
              if (logger.isWarnEnabled()) {
                logger.warn(
                    "Unexpected status from CommunityService modifyAttributes request:" +
                    " status=" + resp.getStatusAsString() +
                    " community=" + community.getName());
              }
            }
          }
      };
        commSvc.modifyAttributes(community.getName(),
                            entityName,
                            (ModificationItem[])mods.toArray(new ModificationItem[0]),
                            crl);
      }
    }
  }

  /**
   * Creates a string representation of an Attribute set.
   */
  protected String attrsToString(Attributes attrs) {
    StringBuffer sb = new StringBuffer("[");
    try {
      for (NamingEnumeration enum = attrs.getAll(); enum.hasMore();) {
        Attribute attr = (Attribute)enum.next();
        sb.append(attr.getID() + "=(");
        for (NamingEnumeration enum1 = attr.getAll(); enum1.hasMore();) {
          sb.append((String)enum1.next());
          if (enum1.hasMore())
            sb.append(",");
          else
            sb.append(")");
        }
        if (enum.hasMore()) sb.append(",");
      }
      sb.append("]");
    } catch (NamingException ne) {}
    return sb.toString();
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

  private class StatusListener {
    void update(NodeStatusRelay nsr) {
      updateCommunityStatus(nsr.getCommunityName(),
                            nsr.getSource().toString(),
                            nsr.getNodeStatus(),
                            nsr.getAgentStatus(),
                            nsr.getLeaderVote(),
                            nsr.getLocation());
    }
  }

}
