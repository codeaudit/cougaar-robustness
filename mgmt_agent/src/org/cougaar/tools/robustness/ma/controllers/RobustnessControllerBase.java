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
package org.cougaar.tools.robustness.ma.controllers;

import org.cougaar.tools.robustness.ma.RestartManagerConstants;
import org.cougaar.tools.robustness.ma.CommunityStatusModel;
import org.cougaar.tools.robustness.ma.StatusChangeListener;
import org.cougaar.tools.robustness.ma.CommunityStatusChangeEvent;

import org.cougaar.tools.robustness.ma.util.HeartbeatHelper;
import org.cougaar.tools.robustness.ma.util.HeartbeatListener;
import org.cougaar.tools.robustness.ma.util.PingHelper;
import org.cougaar.tools.robustness.ma.util.PingResult;
import org.cougaar.tools.robustness.ma.util.PingListener;
import org.cougaar.tools.robustness.ma.util.RestartHelper;
import org.cougaar.tools.robustness.ma.util.RestartListener;
import org.cougaar.tools.robustness.ma.util.MoveHelper;
import org.cougaar.tools.robustness.ma.util.MoveListener;
import org.cougaar.tools.robustness.ma.util.LoadBalancer;
import org.cougaar.tools.robustness.ma.util.CoordinatorHelper;
import org.cougaar.tools.robustness.ma.util.StatCalc;
import org.cougaar.tools.robustness.ma.util.RestartDestinationLocator;

import org.cougaar.core.blackboard.BlackboardClientComponent;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceAvailableListener;
import org.cougaar.core.component.ServiceAvailableEvent;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.AlarmService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.community.CommunityService;
import org.cougaar.core.service.EventService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.SchedulerService;

import org.cougaar.core.service.community.Community;
import org.cougaar.core.service.community.CommunityService;
import org.cougaar.core.service.community.CommunityResponse;
import org.cougaar.core.service.community.CommunityResponseListener;

import org.cougaar.core.mts.MessageAddress;

import org.cougaar.core.agent.service.alarm.Alarm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

/**
 * Robustness controller base class (for sledgehammer use case).  Dispatches
 * state controllers in response to raw events received from
 * CommunityStatysModel.  Also provides a number of utility methods and helper
 * classes for use by state controllers.
 */
public abstract class RobustnessControllerBase extends BlackboardClientComponent
    implements RestartManagerConstants, RobustnessController, StatusChangeListener {

  /**
   * Inner class used to maintain info about registered state controller.
   */
  class ControllerEntry {
    protected int state;
    protected String stateName;
    protected StateController controller;
    protected ControllerEntry(int state, String name, StateController sc) {
      this.state = state;
      this.stateName = name;
      this.controller = sc;
    }
  }

  // Map of ControllerEntry instances
  protected Map controllers = new HashMap();

  // Helper classes
  protected HeartbeatHelper heartbeatHelper;
  protected MoveHelper moveHelper;
  protected PingHelper pingHelper;
  protected RestartHelper restartHelper;
  protected LoadBalancer loadBalancer;
  protected CoordinatorHelper coordinatorHelper = null;

  protected CommunityService communityService;

  protected MessageAddress agentId;
  protected LoggingService logger;
  protected EventService eventService;
  protected StatCalc pingStats = new StatCalc();
  protected RestartDestinationLocator restartLocator;

  // Status model containing current information about monitored community
  protected CommunityStatusModel model;

  /**
   * @param agentId    Agent address
   * @param bs         BindingSite
   * @param csm        CommunityStatusModel for monitored community
   */
  public void initialize(MessageAddress agentId,
                         final          BindingSite bs,
                         final          CommunityStatusModel csm) {
    this.agentId = agentId;
    this.model = csm;
    setBindingSite(bs);
    logger =
      (LoggingService)getServiceBroker().getService(this, LoggingService.class, null);
    logger = org.cougaar.core.logging.LoggingServiceWithPrefix.add(logger, agentId + ": ");
    eventService = (EventService) bs.getServiceBroker().getService(this, EventService.class, null);
    heartbeatHelper = new HeartbeatHelper(bs);
    moveHelper = new MoveHelper(bs, model, restartLocator);
    pingHelper = new PingHelper(bs);
    restartHelper = new RestartHelper(bs);
    loadBalancer = new LoadBalancer(bs, this, model);
    final ServiceBroker sb = getBindingSite().getServiceBroker();
    if (sb.hasService(CommunityService.class)) {
      communityService =
          (CommunityService) sb.getService(this, CommunityService.class, null);
    } else {
      sb.addServiceListener(new ServiceAvailableListener() {
        public void serviceAvailable(ServiceAvailableEvent sae) {
          if (sae.getService().equals(CommunityService.class)) {
            communityService =
                (CommunityService) sb.getService(this, CommunityService.class, null);
          }
        }
      });
    }
    initialize();
    load();
    start();
  }

  /**
   * Load required services.
   */
  public void load() {
    setAgentIdentificationService(
      (AgentIdentificationService)getServiceBroker().getService(this, AgentIdentificationService.class, null));
    setAlarmService(
      (AlarmService)getServiceBroker().getService(this, AlarmService.class, null));
    setSchedulerService(
      (SchedulerService)getServiceBroker().getService(this, SchedulerService.class, null));
    setBlackboardService(
      (BlackboardService)getServiceBroker().getService(this, BlackboardService.class, null));
    eventService = (EventService) getServiceBroker().getService(this, EventService.class, null);
    super.load();
  }

  /**
   * Add a new StateController.
   * @param state     State number
   * @param stateName
   * @param sc        StateController
   */
  public void addController(int state, String stateName, StateController sc) {
    if (logger.isDebugEnabled()) {
      logger.debug("Adding state controller: state=" + stateName);
    }
    if (sc instanceof StateControllerBase) {
      StateControllerBase scb = (StateControllerBase) sc;
      scb.setBindingSite(getBindingSite());
      scb.initialize(getBindingSite(), model);
      scb.load();
      scb.start();
    }
    controllers.put(new Integer(state), new ControllerEntry(state, stateName, sc));
  }

  /**
   * Get a StateController.
   * @param state     State number
   * @return StateController controller
   */
  public StateController getController(int state) {
    ControllerEntry ce = (ControllerEntry)controllers.get(new Integer(state));
    return ce != null ? ce.controller : null;
  }

  protected RestartDestinationLocator getRestartLocator() {
    if (restartLocator == null) {
      restartLocator = new RestartDestinationLocator(model);
    }
    return restartLocator;
  }

  /**
   * Get reference to controllers move helper.
   */
  public MoveHelper getMoveHelper() {
    return moveHelper;
  }

  /**
   * Get reference to controllers heartbeat helper.
   */
  public HeartbeatHelper getHeartbeatHelper() {
    return heartbeatHelper;
  }

  /**
   * Get reference to controllers restart helper.
   */
  public RestartHelper getRestartHelper() {
    return restartHelper;
  }

  /**
   * Get reference to controllers ping helper.
   */
  public PingHelper getPingHelper() {
    return pingHelper;
  }

  /**
   * Get reference to controllers deconflict helper.
   */
  public CoordinatorHelper getCoordinatorHelper() {
    return coordinatorHelper;
  }

  /**
   * Get reference to controllers load balancer interface.
   */
  public LoadBalancer getLoadBalancer() {
    return loadBalancer;
  }

  /**
   * Default handler for receiving and dissemminating model change events.
   * @param csce  Change event
   * @param csm   Status model associated with event
   */
  public void statusChanged(CommunityStatusChangeEvent[] csce) {
    for(int i = 0; i < csce.length; i++) {
      if(logger.isDebugEnabled()) {
        logger.debug(csce[i].toString());
      }
      if(csce[i].statusReceived()) {
        statusUpdateReceived(csce[i].getName());
      }
      if(csce[i].membersAdded() || csce[i].membersRemoved()) {
        processMembershipChanges(csce[i]);
      }
      if(csce[i].stateChanged()) {
        processStatusChanges(csce[i]);
      }
      if(csce[i].leaderChanged()) {
        processLeaderChanges(csce[i]);
      }
      if(csce[i].locationChanged()) {
        processLocationChanges(csce[i]);
      }
      if(csce[i].stateExpired()) {
        processStatusExpirations(csce[i]);
      }
    }
  }

  /**
   * Invoked when a status update is received from a monitoring node.
   * @param csce CommunityStatusChangeEvent
   */
  protected void statusUpdateReceived(String nodeName) {}

  /**
   * Default handler for membership changes in monitored community.
   * @param csce  Change event
   * @param csm   Status model associated with event
   */
  protected void processMembershipChanges(CommunityStatusChangeEvent csce) {
    if (csce.membersAdded()) {
      memberAdded(csce.getName());
    } else if (csce.membersRemoved()) {
      memberRemoved(csce.getName());
    }
  }

  /**
   * Default handler for changes in health monitor leader.
   * @param csce  Change event
   * @param csm   Status model associated with event
   */
  protected void processLeaderChanges(CommunityStatusChangeEvent csce) {
    leaderChange(csce.getPriorLeader(), csce.getCurrentLeader());
  }

  /**
   * Receives notification of leader changes.
   */
  public void leaderChange(String priorLeader, String newLeader) {
  }

  /**
   * Processes state changes in a monitored agent/node.  When a state change
   * occurs the following actions are performed:
   * <pre>
   *   1) The expiration value for the new state expiration is updated using
   *      community attribute data.
   *   2) The enter() method is called on the new state handler
   *   3) The exit() method is called on the old state handler
   * </pre>
   * @param csce  Change event
   * @param csm   Status model associated with event
   */
  protected void processStatusChanges(CommunityStatusChangeEvent csce) {
    // Notify controllers of state transition
    if (logger.isDebugEnabled()) {
      logger.debug("State change:" +
                   " name=" + csce.getName() +
                   " state=" + stateName(csce.getCurrentState()) +
                   " prior=" + stateName(csce.getPriorState()));
    }
    for (Iterator it = controllers.values().iterator(); it.hasNext();) {
      ControllerEntry ce = (ControllerEntry)it.next();
      if (ce.state == csce.getCurrentState()) {
        setExpiration(csce.getName(), stateName(csce.getCurrentState()));
        ce.controller.enter(csce.getName());
      }
      if (ce.state == csce.getPriorState()) {
        ce.controller.exit(csce.getName());
      }
    }
  }

  /**
   * Default handler for changes in agent location.
   * @param csce  Change event
   * @param csm   Status model associated with event
   */
  protected void processLocationChanges(CommunityStatusChangeEvent csce) {
    locationChange(csce.getName(), csce.getPriorLocation(), csce.getCurrentLocation());
  }

  protected void processStatusExpirations(CommunityStatusChangeEvent csce) {
    if (logger.isDebugEnabled()) {
      logger.debug("Status expiration:" +
                   " name=" + csce.getName() +
                   " state=" + stateName(csce.getCurrentState()));
    }
    // Notify controllers of state expirations
    for (Iterator it = controllers.values().iterator(); it.hasNext();) {
      ControllerEntry ce = (ControllerEntry)it.next();
      if (ce.state == csce.getCurrentState()) {
        ce.controller.expired(csce.getName());
      }
    }
  }

  /**
   * Receives notification of change in agent location.
   */
  public void locationChange(String name, String priorLocation, String newLocation) {
  }

  /**
   * Returns name of state.
   * @param state  State code
   * @return       String representation of state code
   */
  public String stateName(int state) {
    ControllerEntry ce = (ControllerEntry)controllers.get(new Integer(state));
    if (ce != null) {
      return ce.stateName;
    } else {
      return "UNKNOWN";
    }
  }

  /**
   * Sends Cougaar event via EventService.
   */
  protected void event(String message) {
    if (eventService != null && eventService.isEventEnabled())
      eventService.event(message);
  }

  /**
   * Returns true if specified agent/node is currently manager of robustness
   * community.
   * @param name  Agent/node name
   * @return      True if leader
   */
  protected boolean isLeader(String name) {
    return (model != null && model.isLeader(name));
  }

  /**
   * Returns true if specified agent is running on local node.
   * @param name  Agent name
   * @return      True if running on local node
   */
  protected boolean isLocal(String name) {
    String thisAgent = agentId.toString();
    String myLocation = (isNode(thisAgent) ? thisAgent
                                           : model.getLocation(thisAgent));
    String agentLocation = model.getLocation(name);
    return (myLocation != null && myLocation.equals(agentLocation));
  }

  /**
   * Change state for specified agent/node.
   * @param name  Agent/node name
   * @param state New state
   */
  protected void newState(String name, int state) {
    model.setCurrentState(name, state, NEVER);
  }

  /**
   * Change state for ass specified agents/nodes.
   * @param names  Set of agent/node names
   * @param state New state
   */
  protected void newState(Set names, int state) {
    for (Iterator it = names.iterator(); it.hasNext(); ) {
      newState((String)it.next(), state);
    }
  }

  /**
   * Change state for ass specified agents/nodes.
   * @param names  Array of agent/node names
   * @param state New state
   */
  protected void newState(String[] names, int state) {
    for (int i = 0; i < names.length; i++) {
      newState(names[i], state);
    }
  }

  /**
   * Return name of all agents on specified node.
   * @param nodeName Name of node
   * @return Set of agent names
   */
  protected Set agentsOnNode(String nodeName) {
    Set agentNames = new HashSet();
    String agentsOnNode[] = model.entitiesAtLocation(nodeName, model.AGENT);
    for (int i = 0; i < agentsOnNode.length; i++) {
      agentNames.add(agentsOnNode[i]);
    }
    return agentNames;
  }

  /**
   * Return name of all agents on specified node.
   * @param nodeName Name of node
   * @param state  State filter
   * @return Set of agent names
   */
  protected Set agentsOnNode(String nodeName, int state) {
    Set agentNames = new HashSet();
    String agentsOnNode[] = model.entitiesAtLocation(nodeName, model.AGENT);
    for (int i = 0; i < agentsOnNode.length; i++) {
      if (getState(agentsOnNode[i]) == state) {
        agentNames.add(agentsOnNode[i]);
      }
    }
    return agentNames;
  }

  /**
   * Get current state for specified agent/node.
   * @param name  Agent/node name
   * @return Current state
   */
  protected int getState(String name) {
    return model.getCurrentState(name);
  }

  /**
   * Get prior state for specified agent/node.
   * @param name  Agent/node name
   * @return Current state
   */
  protected int getPriorState(String name) {
    return model.getPriorState(name);
  }

  /**
   * Set expiration for current state.
   * @param name  Agent/node name
   * @param expiration Expiration in milliseconds
   */
  protected void setExpiration(String name, int expiration) {
    if (logger.isDebugEnabled()) {
      logger.debug("setExpiration:" +
                   " name=" + name +
                   " expiration=" + expiration);
    }
    model.setStateExpiration(name, expiration);
  }

  /**
   * Returns true if name is associated with an Agent.
   * @param name  Agent/node name
   * @return True if type is AGENT
   */
  protected boolean isAgent(String name) {
    return (model.getType(name) == model.AGENT);
  }

  /**
   * Returns true if name is associated with a Node.
   * @param name  Agent/node name
   * @return True if type is NODE
   */
  protected boolean isNode(String name) {
    return (model.getType(name) == model.NODE);
  }

  /**
   * Set agents location.
   * @param name  Agent name
   * @location    Node name
   */
  protected void setLocation(String name, String location) {
    model.setLocation(name, location);
  }

  /**
   * Get agents current location.
   * @param name  Name of agent
   * @return Node name or null if unknown
   */
  protected String getLocation(String name) {
    return model.getLocation(name);
  }

  /**
   * Get community attribute from model.
   * @param id  Attribute identifier
   * @param defaultValue  Default value if attribute not found
   * @return Attribute value as a long
   */
  protected boolean getBooleanAttribute(String id, boolean defaultValue) {
    if (model.hasAttribute(id)) {
      return model.getBooleanAttribute(id);
    } else {
      return defaultValue;
    }
  }

  /**
   * Get community attribute from model.
   * @param id  Attribute identifier
   * @param defaultValue  Default value if attribute not found
   * @return Attribute value as a long
   */
  protected long getLongAttribute(String id, long defaultValue) {
    if (model.hasAttribute(id)) {
      return model.getLongAttribute(id);
    } else {
      return defaultValue;
    }
  }

  /**
   * Get agent/node attribute from model.
   * @param name Agent/node name
   * @param id  Attribute identifier
   * @param defaultValue  Default value if attribute not found
   * @return Attribute value as a long
   */
  protected long getLongAttribute(String name, String id, long defaultValue) {
    if (model.hasAttribute(name, id)) {
      return model.getLongAttribute(name, id);
    } else if (model.hasAttribute(id)) {
      return model.getLongAttribute(id);
    } else {
      return defaultValue;
    }
  }

  /**
   * Get community attribute from model.
   * @param id  Attribute identifier
   * @param defaultValue  Default value if attribute not found
   * @return Attribute value as a double
   */
  protected double getDoubleAttribute(String id, double defaultValue) {
    if (model.hasAttribute(id)) {
      return model.getDoubleAttribute(id);
    } else {
      return defaultValue;
    }
  }

  /**
   * Get agent/node attribute from model.
   * @param name Agent/node name
   * @param id  Attribute identifier
   * @param defaultValue  Default value if attribute not found
   * @return Attribute value as a double
   */
  protected double getDoubleAttribute(String name, String id, double defaultValue) {
    if (model.hasAttribute(name, id)) {
      return model.getDoubleAttribute(name, id);
    } else if (model.hasAttribute(id)) {
      return model.getDoubleAttribute(id);
    } else {
      return defaultValue;
    }
  }

  /**
   * Initiate an agent restart.
   * @param name Agent to be restarted
   * @param dest Name of destination node
   */
  protected void restartAgent(String name, String dest) {
    String orig = model.getLocation(name);
    restartHelper.restartAgent(name, orig, dest,
                               model.getCommunityName());
  }

  /**
   * Kill an agent.
   * @param name Agent to be killed
   */
  protected void killAgent(String name) {
    restartHelper.killAgent(name,
                            model.getLocation(name),
                            model.getCommunityName());
  }

  /**
   * Ping one or more agents and update current state based on result.
   * @param agents          Agents to ping
   * @param stateOnSuccess  New state if ping succeeds
   * @param stateOnFail     New state if ping fails
   */
  protected void doPing(String[] agents,
                        final int stateOnSuccess,
                        final int stateOnFail) {
    long pingTimeout = getLongAttribute(agents[0],
                                        PING_TIMEOUT_ATTRIBUTE,
                                        DEFAULT_PING_TIMEOUT);
    pingHelper.ping(agents, pingTimeout, new PingListener() {
      public void pingComplete(PingResult[] pr) {
        for (int i = 0; i < pr.length; i++) {
          if (pr[i].getStatus() == PingResult.FAIL) {
            if (logger.isInfoEnabled()) {
              logger.info("Ping:" +
                          " agent=" + pr[i].getName() +
                          " state=" +
                          stateName(model.getCurrentState(pr[i].getName())) +
                          " result=FAIL" +
                          " newState=" + stateName(stateOnFail));
            }
          } else {
            if (logger.isDebugEnabled()) {
              logger.debug("Ping:" +
                           " agent=" + pr[i].getName() +
                           " state=" +
                           stateName(model.getCurrentState(pr[i].getName())) +
                           " result=" +
                           (pr[i].getStatus() == PingResult.SUCCESS
                            ? "SUCCESS"
                            : "FAIL") +
                           (pr[i].getStatus() == PingResult.SUCCESS ? "" :
                            " newState=" + stateName(stateOnFail)));
            }
          }
          if (pr[i].getStatus() == PingResult.SUCCESS) {
            newState(pr[i].getName(), stateOnSuccess);
            pingStats.enter(pr[i].getRoundTripTime());
          } else {
            newState(pr[i].getName(), stateOnFail);
          }
        }
      }
    });
  }

  /**
   * Start heartbeats on specified agent.
   * @param name Agent name
   */
  protected void startHeartbeats(String name) {
    long hbReqTimeout = getLongAttribute(name,
                                         HEARTBEAT_REQUEST_TIMEOUT_ATTRIBUTE,
                                         DEFAULT_HEARTBEAT_REQUEST_TIMEOUT);
    long hbFreq = getLongAttribute(name,
                                   HEARTBEAT_FREQUENCY_ATTRIBUTE,
                                   DEFAULT_HEARTBEAT_FREQUENCY);
    long hbTimeout =getLongAttribute(name,
                                     HEARTBEAT_TIMEOUT_ATTRIBUTE,
                                     DEFAULT_HEARTBEAT_TIMEOUT);
    long hbPctOutofSpec = getLongAttribute(name,
                                           HEARTBEAT_PCT_OUT_OF_SPEC_ATTRIBUTE,
                                           DEFAULT_HEARTBEAT_PCT_OUT_OF_SPEC);
    getHeartbeatHelper().startHeartbeats(name,
                                         hbReqTimeout,
                                         hbFreq,
                                         hbTimeout,
                                         hbPctOutofSpec);
  }

  /**
   * Stop heartbeats on specified agent.
   * @param name Agent name
   */
  protected void stopHeartbeats(String name) {
    getHeartbeatHelper().stopHeartbeats(name);
  }

  /**
   * Add heartbeat listener to receive heartbeat events.
   * @param hbl Listener
   */
  protected void addHeartbeatListener(HeartbeatListener hbl) {
    getHeartbeatHelper().addListener(hbl);
  }

  /**
   * Add restart listener to receive restart events.
   * @param rl Listener
   */
  protected void addRestartListener(RestartListener rl) {
    getRestartHelper().addListener(rl);
  }

  /**
   * Add move listener to receive move events.
   * @param ml Listener
   */
  protected void addMoveListener(MoveListener ml) {
    getMoveHelper().addListener(ml);
  }

 /**
   * Returns a String containing top-level health status of monitored community.
   */
  public String statusSummary() {
    String activeNodes[] = model.listEntries(CommunityStatusModel.NODE, getNormalState());
    String agents[] = model.listEntries(CommunityStatusModel.AGENT);
    StringBuffer summary = new StringBuffer("community=" + model.getCommunityName());
    summary.append(" leader=" + model.getLeader());
    summary.append(" activeNodes=[");
    for (int i = 0; i < activeNodes.length; i++) {
      int agentsOnNode = model.entitiesAtLocation(activeNodes[i], model.AGENT).length;
      summary.append(activeNodes[i] + "(" + agentsOnNode + ")");
      if (i < activeNodes.length - 1) summary.append(",");
    }
    summary.append("]");
    summary.append(" agents=" + agents.length);
    for (Iterator it = controllers.values().iterator(); it.hasNext();) {
      int state = ( (ControllerEntry) it.next()).state;
      String agentsInState[] = model.listEntries(CommunityStatusModel.AGENT,
                                                 state);
      if (agentsInState.length > 0) {
        summary.append(" " + stateName(state) + "=" + agentsInState.length);
        if (!stateName(state).equals("ACTIVE") && agentsInState.length > 0)
          summary.append(arrayToString(agentsInState));
      }
    }
    return summary.toString();
  }

  /**
   * Creates an XML representation of an Attribute set.
   */
  protected String attrsToXML(Attributes attrs, String indent) {
    StringBuffer sb = new StringBuffer(indent + "<attributes>\n");
    if (attrs != null) {
      try {
        for (NamingEnumeration enum = attrs.getAll(); enum.hasMore(); ) {
          Attribute attr = (Attribute)enum.next();
          sb.append(indent + "  <attribute id=\"" + attr.getID() + "\" >\n");
          for (NamingEnumeration enum1 = attr.getAll(); enum1.hasMore(); ) {
            sb.append(indent + "    <value>" + enum1.next() + "</value>\n");
          }
          sb.append(indent + "  </attribute>\n");
        }
      } catch (NamingException ne) {}
    }
    sb.append(indent + "</attributes>\n");
    return sb.toString();
  }
  protected Set getExcludedNodes() {
    Set excludedNodes = new HashSet();
    String allNodes[] = model.listEntries(model.NODE);
    for (int i = 0; i < allNodes.length; i++) {
      if (model.hasAttribute(model.getAttributes(allNodes[i]),
                             USE_FOR_RESTARTS_ATTRIBUTE, "False")) {
        excludedNodes.add(allNodes[i]);
      }
    }
    return excludedNodes;
  }

  protected List getVacantNodes() {
    List vacantNodes = new ArrayList();
    String allNodes[] = model.listEntries(model.NODE);
    for (int i = 0; i < allNodes.length; i++) {
      if (isVacantNode(allNodes[i])) {
        vacantNodes.add(allNodes[i]);
      }
    }
    return vacantNodes;
  }

  protected boolean isVacantNode(String name) {
    return model.entitiesAtLocation(name, model.AGENT).length == 0;
  }

  /**
   * Set state expiration using attribute information obtained from community
   * descriptor.
   * If the agent/node has the XXX_EXPIRATION or XXX_TIMEOUT attribute defined
   * this value is used for the expiration.  If the attribute is not defined for the
   * agent/node the community-level attribute is used if it exists.  If neither
   * the community or agent/node defines the attribute, the value defined by
   * DEFAULT_EXPIRATION is used.
   * @param name       Agent/node name
   * @param stateName  State name
   */
  protected void setExpiration(String name, String stateName) {
    long expiration = DEFAULT_EXPIRATION;
    if (model.hasAttribute(name, stateName + "_EXPIRATION")) {
      expiration = model.getLongAttribute(name, stateName + "_EXPIRATION");
    } else if (model.hasAttribute(name, stateName + "_TIMEOUT")) {
      expiration = model.getLongAttribute(name, stateName + "_TIMEOUT");
    } else if (model.hasAttribute(stateName + "_EXPIRATION")){
      expiration = model.getLongAttribute(stateName + "_EXPIRATION");
    } else if (model.hasAttribute(stateName + "_TIMEOUT")){
      expiration = model.getLongAttribute(stateName + "_TIMEOUT");
    }
    model.setStateExpiration(name, expiration);
  }

  protected void setExpiration(String name) {
    String stateName = stateName(model.getCurrentState(name));
    setExpiration(name, stateName);
  }

  /**
   * Return current time in milliseconds.
   * @return
   */
  private long now() {
    return System.currentTimeMillis();
  }

  /**
   * Returns XML formatted dump of all community status information.
   * @return XML formatted status
   */
  public String getCompleteStatus() {
    StringBuffer sb = new StringBuffer("<community" +
                                      " name=\"" + model.getCommunityName() + "\"" +
                                      " leader=\"" + model.getLeader() + "\"" +
                                      " >\n");
    String nodeNames[] = model.listEntries(CommunityStatusModel.NODE);
    String leader = model.getLeader();
    if (leader != null && isAgent(leader)) {
      String nodesPlusLeader[] = new String[nodeNames.length + 1];
      System.arraycopy(nodeNames, 0, nodesPlusLeader, 0, nodeNames.length);
      nodesPlusLeader[nodesPlusLeader.length-1] = model.getLeader();
      nodeNames = nodesPlusLeader;
    }
    sb.append("  <healthMonitors count=\"" + nodeNames.length + "\" >\n");
    for (int i = 0; i < nodeNames.length; i++) {
      sb.append(healthMonitorStatusToXML("    ", nodeNames[i]) + "\n");
    }
    sb.append("  </healthMonitors>\n");
    String agentNames[] = model.listEntries(CommunityStatusModel.AGENT);
    sb.append("  <agents count=\"" + agentNames.length + "\" >\n");
    for (int i = 0; i < agentNames.length; i++) {
      sb.append(agentStatusToXML("    ", agentNames[i]) + "\n");
    }
    sb.append("  </agents>\n");
    sb.append("</community>\n");
    return sb.toString();
  }


  public String agentStatusToXML(String indent, String agentName) {
    long now = now();
    long expiresAt = NEVER;
    if (model.getStateExpiration(agentName) != NEVER) {
      expiresAt = ((model.getTimestamp(agentName) + model.getStateExpiration(agentName)) - now);
    }
    StringBuffer sb = new StringBuffer();
    sb.append(indent + "<agent name=\"" + agentName + "\" >\n");
    sb.append(indent + "  <status " +
        " state=\"" + stateName(model.getCurrentState(agentName)) + "\"" +
        " last=\"" + (now - model.getTimestamp(agentName)) + "\"" +
        " expires=\"" + (expiresAt == NEVER ? "NEVER" : Long.toString(expiresAt)) + "\" />\n");
    String priorLoc = model.getPriorLocation(agentName);
    sb.append(indent + "  <location " +
        " current=\"" + model.getLocation(agentName) + "\"" +
        " prior=\"" + (priorLoc == null ? "" : priorLoc) + "\" />\n");
    sb.append(attrsToXML(model.getAttributes(agentName), indent + "  "));
    sb.append(indent + "</agent>\n");
    return sb.toString();
  }

  public String healthMonitorStatusToXML(String indent, String name) {
    String type = model.getType(name) == model.AGENT ? "agent" : "node";
    long now = now();
    long expiresAt = NEVER;
    if (model.getStateExpiration(name) != NEVER) {
      expiresAt = ((model.getTimestamp(name) + model.getStateExpiration(name)) - now);
    }
    StringBuffer sb = new StringBuffer();
    sb.append(indent + "<healthMonitor name=\"" + name + "\" type=\"" + type + "\" >\n");
    sb.append(indent + "<vote>" + model.getLeaderVote(name) + "</vote>\n");
    sb.append(indent + "  <status " +
        " state=\"" + stateName(model.getCurrentState(name)) + "\"" +
        " last=\"" + (now - model.getTimestamp(name)) + "\"" +
        " expires=\"" + (expiresAt == NEVER ? "NEVER" : Long.toString(expiresAt)) + "\" />\n");
    sb.append(attrsToXML(model.getAttributes(name), indent + "  "));
    sb.append(indent + "</healthMonitor>\n");
    return sb.toString();
  }

  public static String arrayToString(String[] strArray) {
    StringBuffer sb = new StringBuffer("[");
    for (int i = 0; i < strArray.length; i++) {
      sb.append(strArray[i]);
      if (i < strArray.length - 1) sb.append(",");
    }
    sb.append("]");
    return sb.toString();
  }

  /**
   * Modify one or more attributes of a community or entity.
   * @param communityName  Target community
   * @param entityName     Name of entity or null to modify community attributes
   * @param newAttrs       New attributes
   */
  protected void changeAttributes(String communityName, final String entityName, final Attribute[] newAttrs) {
    Community community =
      communityService.getCommunity(communityName, new CommunityResponseListener() {
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
        communityService.modifyAttributes(community.getName(),
                            entityName,
                            (ModificationItem[])mods.toArray(new ModificationItem[0]),
                            crl);
      }
    }
  }

  /**
   * Timer used to trigger periodic check for agents to move.
   */
  class WakeAlarm implements Alarm {
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
