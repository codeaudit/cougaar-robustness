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

import org.cougaar.tools.robustness.ma.controllers.RobustnessController;

import org.cougaar.core.mts.MessageAddress;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.AlarmService;
import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.service.ThreadService;

import org.cougaar.core.thread.Schedulable;

import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.service.wp.Callback;
import org.cougaar.core.service.wp.Response;

import java.net.URI;

import org.cougaar.tools.robustness.ma.ldm.*;

import org.cougaar.util.log.*;

import java.util.*;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

/**
 * Holds community status information used by robustness health monitor
 * components.  Inputs from all robustness health monitor peers are aggregated
 * into a combined community view and status change events are dissemminated to
 * local listeners as status changes are received.  An expiration can also
 * be associated each monitored agent or node state.  A status change event will
 * be generated if a status update is not received within the expiration
 * period.
 */
public class CommunityStatusModel {

  public static final int AGENT = 0;
  public static final int NODE  = 1;

  public static final long NOTIFICATION_INTERVAL = 2 * 1000;
  public static final long LOCATION_UPDATE_INTERVAL = 15 * 1000;

  private long DEFAULT_TTL = 3 * 60 * 1000; // Time to Live for status entries,
                                 // used in calculation of status expirations

  private Logger logger = LoggerFactory.getInstance().createLogger(CommunityStatusModel.class);
  private ServiceBroker serviceBroker;
  private AlarmService alarmService;
  private ThreadService threadService;
  private WhitePagesService whitePagesService;

  /**
   * Status info for a single node/agent.
   */
  private class StatusEntry implements java.io.Serializable {
    Date timestamp;
    Attributes attrs;
    long ttl;
    String name;
    String currentLocation;
    String priorLocation;
    int type;            // AGENT or NODE
    int currentState;
    String leaderVote;
  }

  private String communityName;
  private Attributes communityAttrs;
  private String thisAgent;
  private SortedMap statusMap = new TreeMap();

  private Set prior = new HashSet();

  private String preferredLeader = null;
  private String leader = null;

  private RobustnessController controller;

  // States used by leader elector.  These are initially set to states which
  // should never occur.  Once a controller is assigned (see setController()
  // method) the correct state are retrieved.
  private int normalState = -2;
  private int triggerState = -2;

  private List eventQueue = new ArrayList();
  private List changeListeners = new ArrayList();

  /**
   * Constructor.
   * @param thisAgent      Name of health monitor agent
   * @param communityName  Name of monitored community
   * @param defaultTTL     How long health status from a monitored node/agent
   *                       is considered valid
   * @param leaderState    Valid state for leader
   * @param as             Alarm service for notification timer
   */
  public CommunityStatusModel(String               thisAgent,
                              String               communityName,
                              ServiceBroker        sb) {
    this.communityName = communityName;
    this.thisAgent = thisAgent;
    this.serviceBroker = sb;
    whitePagesService =
        (WhitePagesService)serviceBroker.getService(this, WhitePagesService.class, null);
    threadService =
         (ThreadService)serviceBroker.getService(this, ThreadService.class, null);
    alarmService =
       (AlarmService)serviceBroker.getService(this, AlarmService.class, null);
    alarmService.addRealTimeAlarm(new ChangeNotificationTimer(NOTIFICATION_INTERVAL));
    alarmService.addRealTimeAlarm(new LocationUpdateTimer(LOCATION_UPDATE_INTERVAL));
  }

  public void setController(RobustnessController rc) {
    this.controller = rc;
    DEFAULT_TTL = controller.getDefaultStateExpiration();
    normalState = controller.getNormalState();
    triggerState = controller.getLeaderElectionTriggerState();
  }

  /**
   * Returns list of all monitored agents on specified node.
   * @return Agent names
   */
  public String[] agentsOnNode(String nodeName) {
    synchronized (statusMap) {
      List l = new ArrayList();
      for (Iterator it = statusMap.values().iterator(); it.hasNext();) {
        StatusEntry se = (StatusEntry)it.next();
        if (se != null && nodeName.equals(se.currentLocation) && se.type == AGENT) {
          l.add(se.name);
        }
      }
      l.addAll(prior);
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
   * @return curent state
   */
  public int getCurrentState(String name) {
      return (name != null && statusMap.containsKey(name))
          ? ((StatusEntry)statusMap.get(name)).currentState
          : -1;
  }

  public long getStateExpiration(String name) {
      return (name != null && statusMap.containsKey(name))
          ? ((StatusEntry)statusMap.get(name)).ttl
          : -1;
  }

  /**
   * Returns type code (AGENT or NODE) for monitored entity.
   * @return AGENT or NODE
   */
  public int getType(String name) {
      return (name != null && statusMap.containsKey(name))
          ? ((StatusEntry)statusMap.get(name)).type
          : -1;
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
  public Date getTimestamp(String name) {
    return ((StatusEntry)statusMap.get(name)).timestamp;
  }

  /**
   * Set current state using prior or default expiration.
   * @param name  Name of agent or node
   * @param state New state
   */
  public void setCurrentState(String name, int state) {
    StatusEntry se = (StatusEntry)statusMap.get(name);
    if (se != null) {
      setCurrentState(name, state, (se.ttl > 0 ? se.ttl : DEFAULT_TTL));
    } else {
      logger.error("No status entry found for " + name);
    }
  }

  /**
   * Set current state with a specified expiration.
   * @param name  Name of agent or node
   * @param state New state
   * @param ttl   How long the state is considered valid
   */
  public void setCurrentState(String name, int state, long ttl) {
    StatusEntry se = (StatusEntry)statusMap.get(name);
    se.timestamp = new Date();
    if (se.currentState != state) {
      int priorState = se.currentState;
      se.currentState = state;
      se.ttl = ttl;
      logger.debug("setCurrentState" +
                  " agent=" + name +
                  " newState=" + se.currentState +
                  " priorState=" + priorState +
                  " ttl=" + ttl);
     if (se.type == NODE) {
       electLeader();
     }
     //updateLocation(name, 0);
     queueChangeEvent(
       new CommunityStatusChangeEvent(CommunityStatusChangeEvent.STATE_CHANGE,
                                      se.name,
                                      se.type,
                                      se.currentState,
                                      priorState,
                                      null,
                                      null,
                                      null,
                                      null));
   }
  }

  /**
   * Set expiration for curent state.
   * @param name Name of node or agent
   * @param ttl  How long a state update is considered valid
   */
  public void setStateTTL(String name, long ttl) {
    StatusEntry se = (StatusEntry)statusMap.get(name);
      se.ttl = ttl;
      logger.debug("setStatusTTL" +
                  " agent=" + name +
                  " currentState=" + se.currentState +
                  " ttl=" + ttl);
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
      logger.warn("No StatusEntry found: name=" + name);
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
      logger.warn("No StatusEntry found: name=" + name);
      return null;
    }
  }

  /**
   * Returns name of node that a specified node recognizes as the robustness
   * leader.
   * @param name Name of peer node
   */
  public String getLeaderVote(String name) {
    StatusEntry se = (StatusEntry)statusMap.get(name);
    return (se != null ? se.leaderVote : null);
  }

  /**
   * Set an agent or nodes location.
   * @param name Name of agent or node
   * @param loc  Name of containing node or host
   */
  public void setLocation(String name, String loc) {
    logger.debug("setLocation: agent=" + name + " loc=" + loc);
    StatusEntry se = (StatusEntry)statusMap.get(name);
    se.timestamp = new Date();
    if (se.currentLocation == null || !se.currentLocation.equals(loc)) {
      se.priorLocation = se.currentLocation;
      if (thisAgent.equals(se.priorLocation)) {
        //logger.info("Adding " + name + " to Prior list, prior=" + priorLocation + " new=" + loc);
        //prior.add(name);
      }
      se.currentLocation = loc;
      queueChangeEvent(
        new CommunityStatusChangeEvent(CommunityStatusChangeEvent.LOCATION_CHANGE,
                                       se.name,
                                       se.type,
                                       se.currentState,
                                       -1,
                                       se.currentLocation,
                                       se.priorLocation,
                                       null,
                                       null));
    }
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
                                       -1,
                                       -1,
                                       -1,
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
   * @param community Updated Community instance associated with monitored
   * community
   */
  public void update(Community community) {
    synchronized (statusMap) {
      communityAttrs = community.getAttributes();
      try {
        Attribute attr = communityAttrs.get("Manager");
        if (attr != null)
          setPreferredLeader((String)attr.get());
      } catch (NamingException ne) {}
      for (Iterator it = community.getEntities().iterator(); it.hasNext(); ) {
        Entity entity = (Entity) it.next();
        if (entity instanceof Agent && !statusMap.containsKey(entity.getName())) {
          int type = AGENT;
          Attribute entityTypeAttr = entity.getAttributes().get("EntityType");
          if (entityTypeAttr != null && entityTypeAttr.contains("Node")) {
            type = NODE;
            electLeader();
          }
          StatusEntry se = new StatusEntry();
          se.name = entity.getName();
          se.type = type;
          se.currentState = -1;
          se.ttl = DEFAULT_TTL;
          se.timestamp = new Date();
          se.attrs = entity.getAttributes();
          statusMap.put(se.name, se);
          updateLocations(new String[]{entity.getName()}, 0);
          queueChangeEvent(
            new CommunityStatusChangeEvent(CommunityStatusChangeEvent.MEMBERS_ADDED,
                                           se.name,
                                           se.type,
                                           se.currentState,
                                           -1,
                                           se.currentLocation,
                                           null,
                                           null,
                                           null));
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
  public void applyUpdates(String nodeName, int nodeStatus, AgentStatus[] as, String leader) {
    synchronized (statusMap) {
      logger.debug("ApplyUpdates from node " + nodeName + " agents=" +
                   as.length);
      //boolean leaderVoteChange = false;
      StatusEntry se = (StatusEntry)statusMap.get(nodeName);
      // Apply node status changes
      if (se == null) {
        se = new StatusEntry();
        se.name = nodeName;
        se.type = NODE;
        se.timestamp = new Date();
        se.ttl = DEFAULT_TTL;
        se.currentLocation = nodeName;
        se.priorLocation = null;
        se.currentState = nodeStatus;
        se.leaderVote = leader;
        statusMap.put(se.name, se);
        //leaderVoteChange = true;
        queueChangeEvent(
            new CommunityStatusChangeEvent(CommunityStatusChangeEvent.
                                           MEMBERS_ADDED,
                                           se.name,
                                           se.type,
                                           se.currentState,
                                           -1,
                                           null,
                                           null,
                                           null,
                                           null));
      } else {
        if (se.currentState != nodeStatus) {
          int priorState = se.currentState;
          se.currentState = nodeStatus;
          se.timestamp = new Date();
          queueChangeEvent(
              new CommunityStatusChangeEvent(CommunityStatusChangeEvent.
                                             STATE_CHANGE,
                                             se.name,
                                             se.type,
                                             se.currentState,
                                             priorState,
                                             null,
                                             null,
                                             null,
                                             null));
        }
        if (se.leaderVote == null || !se.leaderVote.equals(leader)) {
          se.leaderVote = leader;
          //leaderVoteChange = true;
        }
      }
      se.timestamp = new Date();
      // Apply agent status changes
      for (int i = 0; i < as.length; i++) {
        se = (StatusEntry)statusMap.get(as[i].getName());
        if (prior.contains(as[i].getName())) {
          logger.info("Removing " + as[i].getName() + " from Prior list, update received from " + nodeName);
          prior.remove(as[i].getName());
        }
        if (se != null) {
          setCurrentState(as[i].getName(), as[i].getStatus());
          if (se.currentLocation == null || !se.currentLocation.equals(as[i].getLocation())) {
            se.priorLocation = se.currentLocation;
            //logger.info(priorLocation + "->" + as[i].getLocation());
            se.currentLocation = as[i].getLocation();
            queueChangeEvent(
                new CommunityStatusChangeEvent(CommunityStatusChangeEvent.
                                               LOCATION_CHANGE,
                                               se.name,
                                               se.type,
                                               se.currentState,
                                               -1,
                                               se.currentLocation,
                                               se.priorLocation,
                                               null,
                                               null));
          }
          se.timestamp = new Date();
        }
      }
      //if (leaderVoteChange)
        electLeader();
    }
  }

  /**
   * Returns an array of all agent and node names in monitored community.
   * @return Array of names
   */
  public String[] listAllEntries() {
    return (String[])statusMap.keySet().toArray(new String[0]);
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
   * Compares 2 arrays of agent names for equality.
   * @param agentArray1  Sorted array of agent names
   * @param agentArray2  Sorted array of agent names
   * @return true if arrays contain same names
   */
  private static boolean agentNamesEqual(String[] agentArray1, String[] agentArray2) {
    return (agentArray1.length == agentArray2.length &&
            Arrays.equals(agentArray1, agentArray2));
  }

  /**
   * Returns a long representing current time.
   * @return Current time
   */
  private long now() {
    return (new Date()).getTime();
  }

  /**
   * Determines if a StatusEntry has expired.
   * @return Return true if current time is greater than entries expiration
   * time.
   */
  public boolean isExpired(StatusEntry se) {
    return se.currentState >= 0 &&
           se.ttl > 0 &&
           se.timestamp.getTime() + se.ttl < now();
  }

  /**
   * Determines if an AgentStatus has expired.
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

  public void updateLocations(String agentNames[], int newStateIfLocal) {
        try {
          for (int i = 0; i < agentNames.length; i++)
            updateLocation(agentNames[i], newStateIfLocal);
        } catch (Exception ex) {
          logger.error("Exception in updateLocations", ex);
        }
    }

    /** find an agent's address by looking in the white pages */
    private void updateLocation(final String agentName,
                             final int newStateIfLocal) throws Exception {
     //logger.info("updateLocation: name=" + agentName);
      whitePagesService.get(agentName, "topology",
              new Callback() {
        public void execute(Response resp) {
          if (resp.isAvailable()) {
            if (resp.isSuccess()) {
              AddressEntry entry = ((Response.Get)resp).getAddressEntry();
              try {
                if (entry != null) {
                  URI uri = entry.getURI();
                  String host = uri.getHost();
                  String node = uri.getPath().substring(1);
                  logger.debug("updateLocation:" +
                              " agent=" + agentName +
                              " host=" + host +
                              " node=" + node +
                              " modelValue=" + getLocation(agentName) +
                              " state=" + getCurrentState(agentName));
                  if (!node.equals(getLocation(agentName))) {
                    setLocation(agentName, node);
                    //setCurrentState(agentName, newStateIfLocal);
                    if (node.equals(thisAgent) && getCurrentState(agentName) != newStateIfLocal) {
                      setCurrentState(agentName, newStateIfLocal);
                      //logger.info("Setting state: agent=" + agentName + " state=" + newStateIfLocal);
                    }
                  }
                } else {
                  logger.info("AddressEntry is null: agent=" + agentName);
                }
              } catch (Exception ex) {
                logger.error("Exception in updateLocation:", ex);
              } finally {
                resp.removeCallback(this);
              }
            } else {
              //resp.addCallback(this);
            }
          } else {
            logger.info("Response not available: agent=" + agentName);
            //resp.addCallback(this);
          }
        }
      });
    }

  public boolean hasAttribute(String name, String id) {
    return hasAttribute(getAttributes(name), id);
  }

  public boolean hasAttribute(String id) {
    return hasAttribute(getCommunityAttributes(), id);
  }

  private boolean hasAttribute(Attributes attrs, String id) {
    return attrs != null && attrs.get(id) != null;
  }

  public String getStringAttribute(String name, String id) {
    return getStringAttribute(getAttributes(name), id);
  }

  public String getStringAttribute(String id) {
    return getStringAttribute(getCommunityAttributes(), id);
  }

  private String getStringAttribute(Attributes attrs, String id) {
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

  private long getLongAttribute(Attributes attrs, String id) {
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
        if (se != null && se.type == NODE && !isExpired(se) &&
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
    //logger.info("electLeader");
    SortedSet candidates = getCandidates();
    List currentVotes = getVotes();
    String newLeader =
        LeaderElection.getLeader(candidates, currentVotes);
    StatusEntry se = (StatusEntry) statusMap.get(thisAgent);
    if (se != null) {
      if (newLeader == null) {
        se.leaderVote =
            LeaderElection.chooseCandidate(preferredLeader,
                                           candidates,
                                           currentVotes);
        newLeader = LeaderElection.getLeader(candidates, getVotes());
      } else {
        se.leaderVote = newLeader;
      }
    }
    setLeader(newLeader);
  }

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
        if (se != null && isExpired(se)) {
          //logger.info("Expired: name=" + se.name + " state=" + se.currentState + " ttl=" + se.ttl);
          queueChangeEvent(
            new CommunityStatusChangeEvent(CommunityStatusChangeEvent.STATE_EXPIRATION,
                                           se.name,
                                           se.type,
                                           se.currentState,
                                           -1,
                                           null,
                                           null,
                                           null,
                                           null));
        }
      }
    }
    //logger.info("Status: leader=" + getLeader() + " expired=" + isExpired(getLeader()));
    if (getLeader() == null ||
        getCurrentState(getLeader()) == triggerState) {
      String currentLeader = getLeader();
      logger.debug("checkExpirations:" +
                  " leader=" + leader +
                  " leaderState=" + (currentLeader == null ? null : getCurrentState(currentLeader) +
                  " triggerState=" + triggerState));
      electLeader();
    }
  }

  private void updateAgentLocations() {
    updateLocations(listEntries(AGENT), 0);
  }

  /**
   * Adds a StatusChangeListener to community.
   * @param scl  StatusChangeListener to add
   */
  public void addChangeListener(StatusChangeListener scl) {
    synchronized (changeListeners) {
      if (!changeListeners.contains(scl))
        changeListeners.add(scl);
    }
  }

  /**
   * Removes a StatusChangeListener from community.
   * @param scl  StatusChangeListener to remove
   */
  public void removeChangeListener(StatusChangeListener scl) {
    synchronized (changeListeners) {
      if (changeListeners.contains(scl))
        changeListeners.remove(scl);
    }
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
    //if (!eventQueue.isEmpty() && getLeader() != null) {
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
    synchronized (changeListeners) {
      for (Iterator it = changeListeners.iterator(); it.hasNext(); ) {
        StatusChangeListener csl = (StatusChangeListener) it.next();
        csl.statusChanged(csce, this);
      }
    }
  }

  /**
   * Timer used to trigger periodic checks for model changes.
   */
  private class ChangeNotificationTimer implements Alarm {
    private long expirationTime = -1;
    private boolean expired = false;
    public ChangeNotificationTimer(long delay) {
      expirationTime = delay + System.currentTimeMillis();
    }

    public void expire() {
      if(!expired) {
        try {
          threadService.getThread(this, new Runnable() {
            public void run() {
              checkExpirations();
              sendEvents();
            }
          }, "ModelUpdateThread").start();
        } finally {
          alarmService.addRealTimeAlarm(new ChangeNotificationTimer(
              NOTIFICATION_INTERVAL));
          expired = true;
        }
      }
    }

    public long getExpirationTime() { return expirationTime; }
    public boolean hasExpired() { return expired; }
    public synchronized boolean cancel() {
      if (!expired)
        return expired = true;
      return false;
    }
  }

  /**
   * Timer used to trigger periodic checks for agent location changes.
   */
  private class LocationUpdateTimer implements Alarm {
    private long expirationTime = -1;
    private boolean expired = false;
    public LocationUpdateTimer(long delay) {
      expirationTime = delay + System.currentTimeMillis();
    }

    public void expire() {
      if(!expired) {
        try {
          threadService.getThread(this, new Runnable() {
            public void run() {
              updateAgentLocations();
            }
          }, "LocationUpdateThread").start();
        } catch (Exception ex) {
          logger.error("Excepiton in LocationUpdateThread", ex);
        } finally {
          alarmService.addRealTimeAlarm(new LocationUpdateTimer(
              LOCATION_UPDATE_INTERVAL));
          expired = true;
        }
      }
    }

    public long getExpirationTime() { return expirationTime; }
    public boolean hasExpired() { return expired; }
    public synchronized boolean cancel() {
      if (!expired)
        return expired = true;
      return false;
    }
  }

}
