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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.HashSet;
import java.util.List;
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
    final String initialCommunity = System.getProperty(COMMUNITY_PROPERTY);
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
       commSvc.joinCommunity(initialCommunity, myName, CommunityService.AGENT,
         memberAttrs, false, null, new CommunityResponseListener() {
           public void getResponse(CommunityResponse resp) {
             logger.debug("joinCommunity:" +
                          " agent=" + myName +
                          " community=" + initialCommunity +
                          " result=" + resp.getStatusAsString());
           }
         });
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

    // Subscribe to Node Status updates sent by peer Health Monitors via Relay
    nodeStatusRelaySub =
        (IncrementalSubscription)blackboard.subscribe(nodeStatusRelayPredicate);

    // Subscribe to external requests
    healthMonitorRequests =
        (IncrementalSubscription)blackboard.subscribe(healthMonitorRequestPredicate);

    commSvc.addListener(new CommunityChangeListener() {
      public String getCommunityName() { return null; }
      public void communityChanged(CommunityChangeEvent cce) {
        //logger.info(cce.toString());
        Attributes attrs = cce.getCommunity().getAttributes();
        Attribute attr = attrs.get("CommunityType");
        if (attr != null && attr.contains("Robustness")) {
          processCommunityChanges(Collections.singleton(cce.getCommunity()));
        }
      }
    });

    // Start timer to periodically check status of agents
    String updateIntervalStr =
        System.getProperty(STATUS_UPDATE_PROPERTY,
                           Long.toString(DEFAULT_STATUS_UPDATE_INTERVAL));
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
    if ((wakeAlarm != null) && wakeAlarm.hasExpired()) {
      // Determine identify node and host
      if (myNode == null && getTopologyFlag) {
        getTopologyFlag = false;
        getTopologyInfo();
      }

      if (myNode != null && !joinedStartupCommunity) {
        joinStartupCommunity();
      }
      updateAndSendNodeStatus();
      long tmp = getLongAttribute(CURRENT_STATUS_UPDATE_ATTRIBUTE, updateInterval);
      if (tmp != updateInterval) {
        logger.info("Changing update interval: old=" + updateInterval + " new=" + tmp);
        updateInterval = tmp;
      }
      wakeAlarm = new WakeAlarm(now() + getLongAttribute(STATUS_UPDATE_ATTRIBUTE, updateInterval));
      alarmService.addRealTimeAlarm(wakeAlarm);
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
    Set targets = new HashSet();
    Set entities = community.search("(Role=" + HEALTH_MONITOR_ROLE + ")",
                                    Community.AGENTS_ONLY);
    for (Iterator it1 = entities.iterator(); it1.hasNext(); ) {
      Entity entity = (Entity) it1.next();
      targets.add(getMessageAddressWithTimeout(entity.getName(), updateInterval));
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
      if (model == null) {
        blackboard.openTransaction();
        initializeModel(community.getName());
        blackboard.closeTransaction();
      }
      if (!initialAttributesLogged) {
        initialAttributesLogged = true;
        logger.info("Robustness community attributes:" +
                    " community=" + community.getName() +
                    " attributes=" + attrsToString(community.getAttributes()));
      }
      long interval = model.getLongAttribute(CURRENT_STATUS_UPDATE_ATTRIBUTE);
      if (interval <= 0) { // Current status update interval not defined yet
        // Get default interval
        long newInterval = getLongAttribute(STATUS_UPDATE_ATTRIBUTE, updateInterval);
        if (newInterval > 0) {
          // Add to community
          logger.debug("Setting update interval attribute: interval=" + newInterval);
          changeAttributes(community,
                           null,
                           new Attribute[]{new BasicAttribute(CURRENT_STATUS_UPDATE_ATTRIBUTE, Long.toString(newInterval))});
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
        blackboard.openTransaction();
        blackboard.publishAdd(nodeStatusRelay);
        blackboard.closeTransaction();
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

  private synchronized void initializeModel(String communityName) {
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

  private MessageAddress getMessageAddressWithTimeout(String target, long timeout) {
    MessageAttributes attrs = new SimpleMessageAttributes();
    attrs.setAttribute(Constants.SEND_TIMEOUT, new Integer((int)timeout));
    return MessageAddress.getMessageAddress(target, attrs);
  }

  private AgentStatus[] getLocalAgentStatus(String communityName) {
    List l = new ArrayList();
    Community community = commSvc.getCommunity(communityName, null);
    if (community != null) {
      String agents[] = model.entitiesAtLocation(myName);
      for (int i = 0; i < agents.length; i++) {
        //if (model.getCurrentState(agents[i]) == controller.getNormalState()) {
        if (community.hasEntity(agents[i]) && !myName.equals(agents[i])) {
          l.add(new AgentStatus(agents[i], myName, model.getCurrentState(agents[i])));
        }
      }
    }
    return (AgentStatus[])l.toArray(new AgentStatus[0]);
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
    model.applyUpdates(nodeName,
                       nodeStatus,
                       agentStatus,
                       leader,
                       host);
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
      // Get status of local agents
      AgentStatus agentStatus[] = getLocalAgentStatus(communityName);

      // Dissemminate status to peer managers
      if (nodeStatusRelay != null) {
       NodeStatusRelayImpl nsr = (NodeStatusRelayImpl)nodeStatusRelay.
            getContent();
        nsr.setAgentStatus(agentStatus);
        nsr.setLeaderVote(model.getLeaderVote(myName));
        Set targets = findHealthMonitorPeers(nsr.getCommunityName());
        nodeStatusRelay.clearTargets();
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
          logger.error("Error setting community attribute:" +
                       " community=" + community.getName() +
                       " attribute=" + newAttrs[i]);
        }
      }
      if (!mods.isEmpty()) {
        CommunityResponseListener crl = new CommunityResponseListener() {
          public void getResponse(CommunityResponse resp) {
            if (resp.getStatus() != CommunityResponse.SUCCESS) {
              logger.warn("Unexpected status from CommunityService modifyAttributes request:" +
                          " status=" + resp.getStatusAsString() +
                          " community=" + community.getName());
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

}
