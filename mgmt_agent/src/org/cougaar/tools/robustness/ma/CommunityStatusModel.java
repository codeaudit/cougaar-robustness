/*
 * <copyright>
 *  Copyright 2001-2003 Mobile Intelligence Corp
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
 */

package org.cougaar.tools.robustness.ma;

import org.cougaar.core.service.community.Agent;
import org.cougaar.core.service.community.Community;
import org.cougaar.core.service.community.Entity;

import org.cougaar.community.Filter;
import org.cougaar.community.SearchStringParser;

import org.cougaar.tools.robustness.ma.controllers.RobustnessController;

import org.cougaar.core.mts.MessageAddress;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.service.AlarmService;
import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.SchedulerService;
import org.cougaar.core.qos.metrics.MetricsService;
import org.cougaar.core.qos.metrics.Metric;
import org.cougaar.core.blackboard.BlackboardClientComponent;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.persist.NotPersistable;

import org.cougaar.core.node.NodeControlService;

import org.cougaar.tools.robustness.ma.ldm.*;

import org.cougaar.util.log.*;

import java.util.*;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;

/**
 * Holds community status information used by robustness health monitor
 * components.  Inputs from all robustness health monitor peers are aggregated
 * into a combined community view and status change events are dissemminated to
 * local listeners as status changes are received.  An expiration can also
 * be associated each monitored agent or node state.  A status change event will
 * be generated if a status update is not received within the expiration
 * period.
 */
public class CommunityStatusModel extends BlackboardClientComponent
    implements NotPersistable {

  public static final int AGENT = 0;
  public static final int NODE  = 1;

  // Predefined states.  These must match the states used by the associated
  // RobustnessController.
  public static final int UNDEFINED = -1;
  public static final int INITIAL = 0;
  public static final int LOCATED = 1;

  // Default state expiration
  public static final int NEVER = -1;

  // Community attribute designating the communities manager agent
  public static final String MANAGER_ATTR = "RobustnessManager";
  public static final String HEALTH_MONITOR = "HealthMonitor";

  // Defines how often expirations are checked and events are dissemminated
  public static final long TIMER_INTERVAL = 10 * 1000;
  private WakeAlarm wakeAlarm;        // Event notification alarm

  // Defines how often the local node is check for new/removed agents
  public static final long LOCATION_UPDATE_INTERVAL = 15 * 1000;

  // Services used
  private LoggingService logger;
  private ServiceBroker serviceBroker;
  private NodeControlService nodeControlService;
  private MetricsService metricsService;

  /**
   * Status info for a single node/agent.
   */
  protected class StatusEntry implements java.io.Serializable {
    long timestamp;
    Attributes attrs;
    long expiration;
    String name;
    String currentLocation;
    String priorLocation;
    int type;
    int currentState;
    int priorState;
    String leaderVote;
    StatusEntry(String name, int type, Attributes attrs) {
      this.name = name;
      this.type = type;
      this.attrs = attrs;
      this.currentState = UNDEFINED;
      this.expiration = NEVER;
      this.timestamp = now();
    }
  }

  private String thisAgent;

  // Name of monitored community
  private String communityName;

  private Attributes communityAttrs;  // Community-level attributes
  private SortedMap statusMap = new TreeMap();  // Agent/node status entries

  // Agent identified by "RobustnessManager=" attribute in community
  private String preferredLeader = null;

  // Current community leader as elected by all HealthMonitors
  private String leader = null;

  // Controller associated with this model
  private RobustnessController controller;

  // States used by leader elector.  These are initially set to states which
  // should never occur.  Once a controller is assigned (via setController()
  // method) the correct state are retrieved.
  private int normalState = -2;
  private int triggerState = -2;

  private List eventQueue = new ArrayList();
  private List changeListeners = Collections.synchronizedList(new ArrayList());

  /**
   * Constructor.
   * @param thisAgent      Name of health monitor agent
   * @param communityName  Name of monitored community
   * @param bs             BindingSite
   */
  public CommunityStatusModel(String      thisAgent,
                              String      communityName,
                              BindingSite bs) {
    this.communityName = communityName;
    setBindingSite(bs);
    serviceBroker = getServiceBroker();
    setAgentIdentificationService(
        (AgentIdentificationService)serviceBroker.getService(this,
                                                  AgentIdentificationService.class, null));
    logger = (LoggingService)serviceBroker.getService(this, LoggingService.class, null);
    logger = org.cougaar.core.logging.LoggingServiceWithPrefix.add(logger,
        agentId + ": ");
    initialize();
    load();
    start();
    this.thisAgent = thisAgent;
  }

  /**
   * Load required services.
   */
  public void load() {
    setSchedulerService(
      (SchedulerService)getServiceBroker().getService(this, SchedulerService.class, null));
    setAlarmService((AlarmService)serviceBroker.getService(this, AlarmService.class, null));
    setBlackboardService(
      (BlackboardService)getServiceBroker().getService(this, BlackboardService.class, null));
    nodeControlService =
      (NodeControlService)serviceBroker.getService(this, NodeControlService.class, null);
    metricsService = (MetricsService)serviceBroker.getService(this, MetricsService.class, null);
    super.load();
  }

  /**
   * Setup wakeAlarm.
   */
  public void setupSubscriptions() {
    wakeAlarm = new WakeAlarm(now() + TIMER_INTERVAL);
    alarmService.addRealTimeAlarm(wakeAlarm);
  }

  /**
   * Performs periodic tasks when wakeAlarm fires.
   */
  public void execute() {
    if ((wakeAlarm != null) &&
        ((wakeAlarm.hasExpired()))) {
      logger.debug("Perform periodic tasks");
      checkExpirations();
      sendEvents();
      if (getType(thisAgent) == NODE) {
        findLocalAgents();
      }
      wakeAlarm = new WakeAlarm(now() + TIMER_INTERVAL);
      alarmService.addRealTimeAlarm(wakeAlarm);
    }
  }

  /**
   * Sets reference to RobustnessController associated with this model.
   * @param rc  RobustnessController
   */
  public void setController(RobustnessController rc) {
    this.controller = rc;
    normalState = controller.getNormalState();
    triggerState = controller.getLeaderElectionTriggerState();
  }

  /**
   * Returns list of all monitored entities at specified location.
   * @return Entity names
   */
  public String[] entitiesAtLocation(String location) {
    synchronized (statusMap) {
      List l = new ArrayList();
      for (Iterator it = statusMap.values().iterator(); it.hasNext();) {
        StatusEntry se = (StatusEntry)it.next();
        if (se != null && se.name != null && location.equals(se.currentLocation)) {
          l.add(se.name);
        }
      }
      return (String[])l.toArray(new String[0]);
    }
  }

  /**
   * Returns list of all monitored entities at specified location.
   * @return Entity names
   */
  public String[] entitiesAtLocation(String location, int type) {
    synchronized (statusMap) {
      List l = new ArrayList();
      for (Iterator it = statusMap.values().iterator(); it.hasNext();) {
        StatusEntry se = (StatusEntry)it.next();
        if (se != null && se.name != null && location.equals(se.currentLocation) && se.type == type) {
          l.add(se.name);
        }
      }
      return (String[])l.toArray(new String[0]);
    }
  }

  /**
   * @return name of monitored community
   */
  public String getCommunityName() {
    return communityName;
  }

  /**
   * @return  Community Attributes
   */
  public Attributes getCommunityAttributes() {
    return communityAttrs;
  }

  /**
   * @return Returns true if monitored community contains specified agent/node
   * name.
   */
  public boolean contains(String name) {
    return statusMap.containsKey(name);
  }

  /**
   * Returns current state for specified agent/node.
   * @return current state
   */
  public int getCurrentState(String name) {
      return (name != null && statusMap.containsKey(name))
          ? ((StatusEntry)statusMap.get(name)).currentState
          : UNDEFINED;
  }

  /**
   * Returns prior state for specified agent/node.
   * @return prior state
   */
  public int getpriorState(String name) {
      return (name != null && statusMap.containsKey(name))
          ? ((StatusEntry)statusMap.get(name)).priorState
          : UNDEFINED;
  }

  /**
   * Returns expiration for current state.
   * @param name
   * @return Expiration period in milliseconds
   */
  public long getStateExpiration(String name) {
      return (name != null && statusMap.containsKey(name))
          ? ((StatusEntry)statusMap.get(name)).expiration
          : UNDEFINED;
  }

  /**
   * Returns type code (AGENT or NODE) for monitored entity.
   * @return AGENT or NODE
   */
  public int getType(String name) {
      return (name != null && statusMap.containsKey(name))
          ? ((StatusEntry)statusMap.get(name)).type
          : UNDEFINED;
  }

  /**
   * Returns Attributes associated with entity
   * @return Entity Attributes
   */
  public Attributes getAttributes(String name) {
      return (name != null && statusMap.containsKey(name))
          ? ((StatusEntry)statusMap.get(name)).attrs
          : null;
  }

  /**
   * Returns timestamp for last status update.
   * @return Time of last update
   */
  public long getTimestamp(String name) {
    return ((StatusEntry)statusMap.get(name)).timestamp;
  }

  /**
   * Set current state with a specified expiration.
   * @param name  Name of agent or node
   * @param state New state
   * @param expiration  How long the state is considered valid
   */
  public void setCurrentState(String name, int state, long expiration) {
    StatusEntry se = (StatusEntry)statusMap.get(name);
    if (se != null) {
      se.timestamp = now();
      if (se.currentState != state) {
        se.priorState = se.currentState;
        se.currentState = state;
        se.expiration = expiration;
        if (logger.isDebugEnabled() && controller != null) {
          logger.debug("setCurrentState" +
                       " agent=" + name +
                       " newState=" + controller.stateName(se.currentState) +
                       " priorState=" + controller.stateName(se.priorState) +
                       " expiration=" +
                       (expiration == NEVER ? "NEVER" :
                        Long.toString(expiration)));
        }
        if (hasAttribute(se.attrs, "Role", HEALTH_MONITOR)) {
          electLeader();
        }
        queueChangeEvent(
            new CommunityStatusChangeEvent(CommunityStatusChangeEvent.
                                           STATE_CHANGE, se));
      }
    } else {
      logger.debug("setCurrentState: status entry not found, agent=" + name);
    }
  }

  /**
   * Set current state with a specified expiration.
   * @param name  Name of agent or node
   * @param state New state
   */
  public void setCurrentState(String name, int state) {
    StatusEntry se = (StatusEntry)statusMap.get(name);
    if (se != null) setCurrentState(name, state, se.expiration);
  }

  /**
   * Set expiration for curent state.
   * @param name Name of node or agent
   * @param expiration  How long a state update is considered valid
   */
  public void setStateExpiration(String name, long expiration) {
    StatusEntry se = (StatusEntry)statusMap.get(name);
    if (se != null) {
      se.expiration = expiration;
      logger.debug("setStatusExpiration" +
                   " agent=" + name +
                   " currentState=" + se.currentState +
                   " expiration=" + expiration);
    } else {
      logger.info("setStateExpiration: No status entry found for '" + name + "'");
    }
  }

  /**
   * Update status timestamp.
   * @param name Name of node or agent
   */
  public void setTimestamp(String name) {
    StatusEntry se = (StatusEntry)statusMap.get(name);
      se.timestamp = now();
  }

  /**
   * Returns name of an agents current node.
   * @param name Name of agent or node
   */
  public String getLocation(String name) {
    StatusEntry se = (StatusEntry)statusMap.get(name);
    if (se != null) {
      return se.currentLocation;
    } else {
      logger.debug("No StatusEntry found: name=" + name);
      return null;
    }
  }

  /**
   * Returns name of an agents prior node.
   * @param name Name of agent or node
   */
  public String getPriorLocation(String name) {
    StatusEntry se = (StatusEntry)statusMap.get(name);
    if (se != null) {
      return se.priorLocation;
    } else {
      logger.debug("No StatusEntry found: name=" + name);
      return null;
    }
  }

  /**
   * Returns name of agent that a specified node recognizes as the robustness
   * leader.
   * @param name Name of peer node
   */
  public String getLeaderVote(String name) {
    StatusEntry se = (StatusEntry)statusMap.get(name);
    return (se != null ? se.leaderVote : null);
  }

  /**
   * Returns name of agent that a specified node recognizes as the robustness
   * leader.
   * @param name Name of peer node
   */
  public void setLeaderVote(String name, String vote) {
    //logger.info("agent=" + name + " vote=" + vote);
    StatusEntry se = (StatusEntry)statusMap.get(name);
    se.leaderVote = vote;
  }

  /**
   * Set an agent or nodes location.
   * @param name Name of agent or node
   * @param loc  Name of containing node or host
   */
  public void setLocation(String name, String loc) {
    logger.debug("setLocation: agent=" + name + " loc=" + loc);
    StatusEntry se = (StatusEntry)statusMap.get(name);
    if (se != null) {
      se.timestamp = now();
      if (se.currentLocation == null || !se.currentLocation.equals(loc)) {
        se.priorLocation = se.currentLocation;
        se.currentLocation = loc;
        queueChangeEvent(
            new CommunityStatusChangeEvent(CommunityStatusChangeEvent.
                                           LOCATION_CHANGE, se));
      }
    } else {
      logger.debug("No StatusEntry found: name=" + name);
    }
  }

  /**
   * Sets location and state as a single atomic operation before generating
   * update events.
   * @param name  Name of agent or node
   * @param loc   New location
   * @param state New state
   */
  public void setLocationAndState(String name, String loc, int state) {
    logger.debug("setLocationAndState" +
                 " agent=" + name +
                 " location=" + loc +
                 " state=" + state);
    StatusEntry se = (StatusEntry)statusMap.get(name);
    se.timestamp = now();
    int changeFlags = 0;
    if (se != null) {
      if (se.currentLocation == null || !se.currentLocation.equals(loc)) {
        changeFlags = changeFlags | CommunityStatusChangeEvent.LOCATION_CHANGE;
        se.priorLocation = se.currentLocation;
        se.currentLocation = loc;
        logger.debug("setLocation: agent=" + name + " loc=" + loc);
      }
      if (se.currentState != state) {
        changeFlags = changeFlags | CommunityStatusChangeEvent.STATE_CHANGE;
        se.priorState = se.currentState;
        se.currentState = state;
        logger.debug("setCurrentState" +
                     " agent=" + name +
                     " newState=" + se.currentState +
                     " priorState=" + se.priorState +
                     " expiration=" + se.expiration);
      }
      if (changeFlags > 0) {
        queueChangeEvent(
            new CommunityStatusChangeEvent(changeFlags, se));
      }
      if (hasAttribute(se.attrs, "Role", HEALTH_MONITOR)) {
        electLeader();
      }
    } else {
      logger.debug("No StatusEntry found: name=" + name);
    }
  }

  /**
   * Search for entries with attributes matching specified JNDI-style
   * filter.
   * @param filter    JNDI style search filter
   * @return List of entity names satisfying search filter
   */
  public List search(String filter) {
    List matches = new ArrayList();
    SearchStringParser parser = new SearchStringParser();
    try {
      Filter f = parser.parse(filter);
      synchronized (statusMap) {
        for (Iterator it = statusMap.values().iterator(); it.hasNext(); ) {
          StatusEntry se = (StatusEntry)it.next();
          if (f.match(se.attrs)) {
            matches.add(se.name);
          }
        }
      }
    } catch (Exception ex) {
      logger.error("Exception in search, filter=" + filter);
    }
    return matches;
  }

  /**
   * Returns name of the health monitor leader recognized by this agent/node.
   * @return name of current leader
   */
  public String getLeader() {
    return leader;
  }

  /**
   * Set name of recognized leader.
   * @param name of leader agent/node
   */
  public void setLeader(String newLeader) {
    if ((this.leader != null && newLeader == null) ||
        (newLeader != null && this.leader == null) ||
        (this.leader != null && newLeader != null && !this.leader.equals(newLeader))) {
      String priorLeader = leader;
      this.leader = newLeader;
      queueChangeEvent(
        new CommunityStatusChangeEvent(CommunityStatusChangeEvent.LEADER_CHANGE,
                                       null,
                                       UNDEFINED,
                                       UNDEFINED,
                                       UNDEFINED,
                                       null,
                                       null,
                                       newLeader,
                                       priorLeader));
    }
  }

  private void setPreferredLeader(String pl) {
    if (pl != null && preferredLeader == null) {
      logger.info("Preferred leader: " + pl);
    }
    preferredLeader = pl;
  }

  /**
   * Determines if specified agent/node name is the current health monitor leader.
   */
  public boolean isLeader(String l) {
    return (this.leader != null && this.leader.equals(l));
  }

  /**
   * Update internal elements based on current membership and attributes in
   * monitored community.
   * @param community Community instance associated with monitored
   * community
   */
  public void update(Community community) {
    if (communityName.equals(community.getName())) {
      synchronized (statusMap) {
        communityAttrs = community.getAttributes();
        try {
          Attribute attr = communityAttrs.get(MANAGER_ATTR);
          if (attr != null)
            setPreferredLeader( (String) attr.get());
        } catch (NamingException ne) {}
        // Add new members
        for (Iterator it = community.getEntities().iterator(); it.hasNext(); ) {
          Entity entity = (Entity) it.next();
          if (entity instanceof Agent) {
            if (!statusMap.containsKey(entity.getName())) {
              int type = AGENT;
              Attribute entityTypeAttr = entity.getAttributes().get(
                  "EntityType");
              if (entityTypeAttr != null && entityTypeAttr.contains("Node")) {
                type = NODE;
              }
              StatusEntry se = new StatusEntry(entity.getName(), type,
                                               entity.getAttributes());
              statusMap.put(se.name, se);
              queueChangeEvent(
                  new CommunityStatusChangeEvent(CommunityStatusChangeEvent.
                                                 MEMBERS_ADDED, se));
              //setCurrentState(se.name, INITIAL);
            } else {
              StatusEntry se = (StatusEntry) statusMap.get(entity.getName());
              if (se.attrs == null) {
                se.attrs = entity.getAttributes();
              }
            }
          }
        }
        // Remove members
        String entitiesInModel[] = listAllEntries();
        for (int i = 0; i < entitiesInModel.length; i++) {
          if (!community.hasEntity(entitiesInModel[i])) {
            StatusEntry se = (StatusEntry)statusMap.remove(entitiesInModel[i]);
            queueChangeEvent(
                new CommunityStatusChangeEvent(CommunityStatusChangeEvent.
                                               MEMBERS_REMOVED, se));
          }
        }
      }
    }
  }

  /**
   * Applies updated status from a node.  The update also identifies the name
   * of the leader recognized by the reporting node.  The leader name is used
   * in the selection performed by the LeaderElection class.
   * @param nodeName Name of node providing status
   * @param nodeStatus Status of node agent
   * @param as         Status of nodes child agents
   * @param leader     Name of health monitor leader recognized by reporting node
   */
  public void applyUpdates(String nodeName, int nodeStatus, AgentStatus[] as, String vote, String host) {
    synchronized (statusMap) {
      logger.debug("ApplyUpdates:" +
                   " node=" + nodeName +
                   " nodeStatus=" + controller.stateName(nodeStatus) +
                   " numAgents=" + as.length);
      if (statusMap.containsKey(nodeName)) {
        setCurrentState(nodeName, nodeStatus);
        setLeaderVote(nodeName, vote);
        setLocation(nodeName, host);
      } else {
        StatusEntry se = new StatusEntry(nodeName, NODE, null);
        statusMap.put(se.name, se);
        setCurrentState(se.name, controller.getNormalState());
        //setCurrentState(se.name, INITIAL);
        queueChangeEvent(
          new CommunityStatusChangeEvent(CommunityStatusChangeEvent.MEMBERS_ADDED, se));
      }
      // Apply agent status changes
      for (int i = 0; i < as.length; i++) {
        String agentName = as[i].getName();
        if (statusMap.containsKey(agentName)) {
          if (isLocalAgent(agentName)) {
            setLocation(agentName, thisAgent);
          } else {
            setLocation(agentName, as[i].getLocation());
            setCurrentState(agentName, as[i].getStatus());
          }
        }
      }
    }
    queueChangeEvent(
      new CommunityStatusChangeEvent(CommunityStatusChangeEvent.STATUS_UPDATE_RECEIVED,
                                     nodeName,
                                     UNDEFINED,
                                     UNDEFINED,
                                     UNDEFINED,
                                     null,
                                     null,
                                     null,
                                     null));
  }

  /**
   * Returns an array of all agent and node names in monitored community.
   * @return Array of names
   */
  public String[] listAllEntries() {
    synchronized (statusMap) {
      return (String[]) statusMap.keySet().toArray(new String[0]);
    }
  }

  /**
   * Returns an array of all agent or node names in monitored community.
   * @param type AGENT or NODE
   * @return Array of names
   */
  public String[] listEntries(int type) {
    List l = new ArrayList();
    synchronized (statusMap) {
      for (Iterator it = statusMap.values().iterator(); it.hasNext(); ) {
        StatusEntry se = (StatusEntry)it.next();
        if (se != null && se.type == type) {
          l.add(se.name);
        }
      }
    }
    return (String[])l.toArray(new String[0]);
  }

  /**
   * Returns names of agents or nodes in a specified state.
   * @param type AGENT or NODE
   * @param state Selected state
   * @return Array of names
   */
  public String[] listEntries(int type, int state) {
    List l = new ArrayList();
    synchronized (statusMap) {
      for (Iterator it = statusMap.values().iterator(); it.hasNext(); ) {
        StatusEntry se = (StatusEntry)it.next();
        if (se != null && se.type == type && se.currentState == state) {
          l.add(se.name);
        }
      }
    }
    return (String[])l.toArray(new String[0]);
  }

  /**
   * Returns a long representing current time.
   * @return Current time
   */
  private long now() {
    return System.currentTimeMillis();
  }

  /**
   * Determines if a StatusEntry has expired.
   * @param se StatusEntry associated with agent/node
   * @return Return true if current time is greater than entries expiration
   * time.
   */
  public boolean isExpired(StatusEntry se) {
    return se.currentState >= 0 &&
           se.expiration > 0 &&
           se.timestamp + se.expiration < now();
  }

  /**
   * Determines if an AgentStatus has expired.
   * @param name  Agent or node name
   * @return Return true if current time is greater than entries expiration
   * time.
   */
  public boolean isExpired(String name) {
    return isExpired((StatusEntry)statusMap.get(name));
  }

  /**
   * Retrieves leader votes from nodes.  Does not include votes from
   * expired agents or null votes.
   * @return List of non-null votes from active agents
   */
  protected List getVotes() {
    List votes = new ArrayList();
    synchronized (statusMap) {
      for (Iterator it = statusMap.values().iterator(); it.hasNext(); ) {
        StatusEntry se = (StatusEntry)it.next();
        if (se != null &&
            se.type == NODE &&
            !isExpired(se) &&
            se.currentState == normalState &&
            se.leaderVote != null)
          votes.add(se.leaderVote);
      }
      return votes;
    }
  }

  /**
   * Indicates whether an attribute is defined for specified agent/node.
   * @param name  Agent or node name
   * @param id    Attribute id
   * @return      True if attribute is defined
   */
  public boolean hasAttribute(String name, String id) {
    return hasAttribute(getAttributes(name), id);
  }

  /**
   * Indicates whether an attribute is defined for monitored community.
   * @param id    Attribute id
   * @return      True if attribute is defined
   */
  public boolean hasAttribute(String id) {
    return hasAttribute(getCommunityAttributes(), id);
  }

  /**
   * Indicates whether an attribute is defined.
   * @param attrs  Attribute set
   * @param id    Attribute id
   * @return      True if attribute is defined
   */
  public boolean hasAttribute(Attributes attrs, String id) {
    return attrs != null && attrs.get(id) != null;
  }

  /**
   * Indicates whether an attribute is defined and contains specified value.
   * @param attrs  Attribute set
   * @param id    Attribute id
   * @param value Attribute value
   * @return      True if attribute is defined and contains specified value
   */
  public boolean hasAttribute(Attributes attrs, String id, String value) {
    if (attrs == null) return false;
    Attribute attr = attrs.get(id);
    return attr != null && attr.contains(value);
  }

  /**
   * Get specified attribute for agent or node and return value as String.
   * @param name  Name of agent or node
   * @param id    Attribute id
   * @return      Attribute value as String or null if attribute doesn't exist
   */
  public String getStringAttribute(String name, String id) {
    return getStringAttribute(getAttributes(name), id);
  }

  /**
   * Get specified attribute for monitored community and return value as String.
   * @param id    Attribute id
   * @return      Attribute value as String or null if attribute doesn't exist
   */
  public String getStringAttribute(String id) {
    return getStringAttribute(getCommunityAttributes(), id);
  }

  public String getStringAttribute(Attributes attrs, String id) {
    String value = null;
    try {
      if (attrs != null && id != null) {
        Attribute attr = attrs.get(id);
        if (attr != null)
          value = (String)attr.get();
      }
    } catch (Exception ex) {}
    return value;
  }

  public long getLongAttribute(String name, String id) {
    return getLongAttribute(getAttributes(name), id);
  }

  public long getLongAttribute(String id) {
    return getLongAttribute(getCommunityAttributes(), id);
  }

  public long getLongAttribute(Attributes attrs, String id) {
    long value = Long.MIN_VALUE;
    try {
      if (attrs != null && id != null) {
        Attribute attr = attrs.get(id);
        if (attr != null)
          value = Long.parseLong((String)attr.get());
      }
    } catch (Exception ex) {}
    return value;
  }

  public boolean getBooleanAttribute(String name, String id) {
    return getBooleanAttribute(getAttributes(name), id);
  }

  public boolean getBooleanAttribute(String id) {
    return getBooleanAttribute(getCommunityAttributes(), id);
  }

  public boolean getBooleanAttribute(Attributes attrs, String id) {
    boolean result = false;
    try {
      if (attrs != null && id != null) {
        Attribute attr = attrs.get(id);
        if (attr != null) {
          String attrValue = (String) attr.get();
          result = attrValue != null &&
                   ("True".equalsIgnoreCase(attrValue) ||
                   ("Yes".equalsIgnoreCase(attrValue)));
        }
      }
    } catch (Exception ex) {}
    return result;
  }

  public String getAttribute(String name, String id) {
    return getAttribute(getAttributes(name), id);
  }

  public String getAttribute(String id) {
    return getAttribute(getCommunityAttributes(), id);
  }

  public String getAttribute(Attributes attrs, String id) {
    String value = null;
    try {
      if (attrs != null && id != null) {
        Attribute attr = attrs.get(id);
        if (attr != null)
          value = ((String)attr.get());
      }
    } catch (Exception ex) {}
    return value;
  }

  public double getDoubleAttribute(String name, String id) {
    return getDoubleAttribute(getAttributes(name), id);
  }

  public double getDoubleAttribute(String id) {
    return getDoubleAttribute(getCommunityAttributes(), id);
  }

  public double getDoubleAttribute(Attributes attrs, String id) {
    double value = Double.NaN;
    try {
      if (attrs != null && id != null) {
        Attribute attr = attrs.get(id);
        if (attr != null)
          value = Double.parseDouble((String)attr.get());
      }
    } catch (Exception ex) {}
    return value;
  }

  /**
   * Finds agents/nodes that could porentially become the health monitor
   * leader.  Expired nodes/agents are not selected.
   * @return Set of agent/node names
   */
  protected SortedSet getCandidates() {
    SortedSet candidates = new TreeSet();
    synchronized (statusMap) {
      for (Iterator it = statusMap.values().iterator(); it.hasNext(); ) {
        StatusEntry se = (StatusEntry)it.next();
        /*logger.info("getCandidates" +
                    " name=" + se.name +
                    " hasAttr=" + hasAttribute(se.attrs, "Role", "HealthMonitor") +
                    " isExpired=" + isExpired(se) +
                    " currentState=" + se.currentState +
                    " normalState=" + normalState);*/
        if (se != null &&
            se.attrs != null  &&
            hasAttribute(se.attrs, "Role", HEALTH_MONITOR) &&
            !isExpired(se) &&
            se.currentState == normalState) {
          candidates.add(se.name);
        }
      }
    }
    return candidates;
  }

  /**
   * Find expired agents or nodes.
   * @return Array of agents/nodes names with expired status
   */
  private String[] getExpired() {
    List l = new ArrayList();
    synchronized (statusMap) {
      for (Iterator it = statusMap.values().iterator(); it.hasNext(); ) {
        StatusEntry se = (StatusEntry)it.next();
        if (se != null && isExpired(se)) {
          l.add(se.name);
        }
      }
    }
    return (String[])l.toArray(new String[0]);
  }

  /**
   * Find expired agents or nodes.
   * @param type Restricts selection to agents or nodes
   * @return Array of agents or nodes names with expired status
   */
  public String[] getExpired(int type) {
    List l = new ArrayList();
    synchronized (statusMap) {
      for (Iterator it = statusMap.values().iterator(); it.hasNext(); ) {
        StatusEntry se = (StatusEntry)it.next();
        if (se != null && se.type == type && isExpired(se)) {
          l.add(se.name);
        }
      }
    }
    return (String[])l.toArray(new String[0]);
  }

  /**
   * Performs leader election using current state and peer votes.
   */
  private void electLeader() {
    if (logger.isDebugEnabled()) {
      String current = getLeader();
      String preferred = preferredLeader;
      int currentLeaderState = current != null
                                 ? getCurrentState(current)
                                 : -1;
      logger.debug("electLeader:" +
                  " currentLeader=" + current + "(" + currentLeaderState + ")" +
                  " preferredLeader=" + preferred +
                  " triggerState=" + triggerState);
    }
    try {
      if (getLeader() == null ||
          !getLeader().equals(preferredLeader) ||
          getCurrentState(getLeader()) == triggerState) {
        SortedSet candidates = getCandidates();
        StatusEntry se = (StatusEntry)statusMap.get(thisAgent);
        if (se != null) {
          setLeaderVote(thisAgent,
              LeaderElection.chooseCandidate(preferredLeader,
                                             candidates,
                                             getVotes()));
          String newLeader =
              LeaderElection.getLeader(candidates, getVotes(), getLeader());
          setLeader(newLeader);
        }
      }
    } catch (Exception ex) {
      logger.error(ex.getMessage() + " thisAgent=" + thisAgent, ex);
    }
  }

  // Contains names of expired agents that have already been included
  // in a change event.  Used to avoid sending multiple expiration changes events
  private Set priorExpirations = Collections.synchronizedSet(new HashSet());

  /**
   * Check for agents or nodes with expired status.  CommunityStatusChangeEvents
   * are generated when a status expiration is detected.
   */
  private void checkExpirations() {
    logger.debug("check expirations");
    boolean foundExpired = false;
    synchronized (statusMap) {
      for (Iterator it = statusMap.values().iterator(); it.hasNext();) {
        StatusEntry se = (StatusEntry)it.next();
        if (se != null) {
          boolean isExpired = isExpired(se);
          if (isExpired) {
            if (!priorExpirations.contains(se.name)) {
              priorExpirations.add(se.name);
              queueChangeEvent(
                  new CommunityStatusChangeEvent(
                  CommunityStatusChangeEvent.STATE_EXPIRATION,
                  se.name,
                  se.type,
                  se.currentState,
                  UNDEFINED,
                  null,
                  null,
                  null,
                  null));
            }
          } else {  // not expired
            if (priorExpirations.contains(se.name)) {
              priorExpirations.remove(se.name);
            }
          }
        }
      }
    }
    //logger.info("Status: leader=" + getLeader() + " expired=" + isExpired(getLeader()));
    //if (getLeader() == null ||
    //    getCurrentState(getLeader()) == triggerState) {
      String currentLeader = getLeader();
      logger.debug("electLeader:" +
                  " leader=" + leader +
                  " leaderState=" + (currentLeader == null ? null : getCurrentState(currentLeader) +
                  " triggerState=" + triggerState));
      electLeader();
    //}
  }

  private void findLocalAgents() {
    String allAgents[] = listAllEntries();
    for (int i = 0; i < allAgents.length; i++) {
      MessageAddress addr = MessageAddress.getMessageAddress(allAgents[i]);
      if (nodeControlService.getRootContainer().containsAgent(addr)) {
        if (!thisAgent.equals(getLocation(allAgents[i]))) {
          setLocationAndState(allAgents[i], thisAgent, INITIAL);
          logger.debug("Found agent " + allAgents[i] + " at node " + thisAgent);
        }
      }
    }
  }

  private boolean isLocalAgent(String name) {
    return (nodeControlService != null &&
            nodeControlService.getRootContainer().containsAgent(MessageAddress.getMessageAddress(name)) &&
            thisAgent.equals(getLocation(name)));

  }

  /**
   * Adds a StatusChangeListener to community.
   * @param scl  StatusChangeListener to add
   */
  public void addChangeListener(StatusChangeListener scl) {
    if (!changeListeners.contains(scl))
      changeListeners.add(scl);
  }

  /**
   * Removes a StatusChangeListener from community.
   * @param scl  StatusChangeListener to remove
   */
  public void removeChangeListener(StatusChangeListener scl) {
    if (changeListeners.contains(scl))
      changeListeners.remove(scl);
  }

  /**
   * Add a CommunityStatusChangeEvent to dissemmination queue.
   * @param csce CommunityStatusChangeEvent to send
   */
  protected void queueChangeEvent(CommunityStatusChangeEvent csce) {
    synchronized (eventQueue) {
      eventQueue.add(csce);
      logger.debug("queueEvent: (" + eventQueue.size() + ") " + csce);
    }
  }

  /**
   * Distributes all events in queue to listeners.
   */
  protected void sendEvents() {
    if (!eventQueue.isEmpty()) {
      CommunityStatusChangeEvent[] events = new CommunityStatusChangeEvent[0];
      synchronized (eventQueue) {
        logger.debug("sendEvents:" +
                    " numEvents=" + eventQueue.size());
        //dumpEventQueue();
        events =
            (CommunityStatusChangeEvent[]) eventQueue.toArray(new CommunityStatusChangeEvent[0]);
        eventQueue.clear();
      }
      notifyListeners(events);
    }
  }

  private void dumpEventQueue() {
    synchronized (eventQueue) {
      for (Iterator it = eventQueue.iterator(); it.hasNext();) {
        CommunityStatusChangeEvent csce = (CommunityStatusChangeEvent)it.next();
        logger.info(csce.toString());
      }
    }
  }

  /**
   * Send CommunityStatusChangeEvent to listeners.
   */
  protected void notifyListeners(CommunityStatusChangeEvent[] csce) {
    List listenersToNotify = new ArrayList();
    synchronized (changeListeners) {
      listenersToNotify.addAll(changeListeners);
    }
    for (Iterator it = listenersToNotify.iterator(); it.hasNext(); ) {
      StatusChangeListener csl = (StatusChangeListener) it.next();
      csl.statusChanged(csce);
    }
  }

  /**
   * Get metrics information of the given node.
   */
  public double getMetricAsDouble(String nodeName, String searchString) {
    Metric metric = getMetric(nodeName, searchString);
    return metric != null ? metric.doubleValue() : 0.0;
  }

  /**
   * Get metrics information of the given node.
   */
  public long getMetricAsLong(String nodeName, String searchString) {
    Metric metric = getMetric(nodeName, searchString);
    return metric != null ? metric.longValue() : 0;
  }

  public Metric getMetric(String nodeName, String searchString) {
    Metric metric = null;
    int tryCount = 0;
    double cred = org.cougaar.core.qos.metrics.Constants.NO_CREDIBILITY;
    double desiredMetricsCredibility = org.cougaar.core.qos.metrics.Constants.USER_BASE_CREDIBILITY;
    while (tryCount < 2 && cred < desiredMetricsCredibility) {
      tryCount ++;
      String basePath = "Node(" + nodeName + ")" +
          org.cougaar.core.qos.metrics.Constants.PATH_SEPR;
      metric = metricsService.getValue(basePath + searchString);
      if (metric != null) metric.getCredibility();
    }
    return metric;
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
