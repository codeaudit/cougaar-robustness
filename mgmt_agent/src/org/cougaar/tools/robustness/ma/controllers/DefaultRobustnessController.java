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

import org.cougaar.tools.robustness.ma.CommunityStatusModel;
import org.cougaar.tools.robustness.ma.controllers.*;
import org.cougaar.tools.robustness.ma.HostLossThreatAlertHandler;
import org.cougaar.tools.robustness.ma.SecurityAlertHandler;
import org.cougaar.tools.robustness.ma.util.DeconflictHelper;
import org.cougaar.tools.robustness.ma.util.DeconflictListener;
import org.cougaar.tools.robustness.ma.util.HeartbeatListener;
import org.cougaar.tools.robustness.ma.util.LoadBalancer;
import org.cougaar.tools.robustness.ma.util.LoadBalancerListener;
import org.cougaar.tools.robustness.ma.util.MoveHelper;
import org.cougaar.tools.robustness.ma.util.MoveListener;
import org.cougaar.tools.robustness.ma.util.PingHelper;
import org.cougaar.tools.robustness.ma.util.PingListener;
import org.cougaar.tools.robustness.ma.util.RestartHelper;
import org.cougaar.tools.robustness.ma.util.RestartListener;
import org.cougaar.tools.robustness.ma.util.RestartDestinationLocator;

import org.cougaar.tools.robustness.threatalert.*;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceAvailableListener;
import org.cougaar.core.component.ServiceAvailableEvent;

import org.cougaar.core.mts.MessageAddress;

import java.util.*;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;

public class DefaultRobustnessController extends RobustnessControllerBase {

  public static final int INITIAL        = 0;
  public static final int LOCATED        = 1;
  public static final int ACTIVE         = 2;
  public static final int HEALTH_CHECK   = 3;
  public static final int DEAD           = 4;
  public static final int RESTART        = 5;
  public static final int FAILED_RESTART = 6;
  public static final int MOVE           = 7;
  public static final int DECONFLICT     = 8;

  // Determines how often the status summary is logged
  public static final long STATUS_INTERVAL = 2 * 60 * 1000;

  /**
   * State Controller: INITIAL
   * <pre>
   *   Entry:             Agent found in community descriptor
   *   Actions performed: None
   *   Next state:        LOCATED after found by a NodeHealthMonitorPlugin
   *                      HEALTH_CHECK if expired
   * </pre>
   */
  class InitialStateController extends StateControllerBase {
    public void enter(String name) {
      if (!monitorStartup() && !startupCompleted) {
        setExpiration(name, NEVER);
      } else if (doInitialPing() &&
        (isLeader(thisAgent) || isNode(name) || name.equals(preferredLeader()))) {
        doPing(name, DefaultRobustnessController.ACTIVE, DEAD);
      }
    }

    public void expired(String name) {
      logger.debug("Expired Status:" + " agent=" + name + " state=INITIAL" +
                  " expiration=" + model.getStateExpiration(name));
      newState(name, HEALTH_CHECK);
    }

  }

  /**
   * State Controller: LOCATED
   * <pre>
   *   Entry:             Agent discovered by a NodeHealthMonitorPlugin
   *   Actions performed: Start heartbeats
   *   Next state:        ACTIVE if heartbeats successfully started
   *                      HEALTH_CHECK if status expired
   * </pre>
   */
  class LocatedStateController extends    StateControllerBase
                               implements HeartbeatListener {
    { addHeartbeatListener(this); }
    public void enter(String name) {
      if (isLocal(name)) {
        if (isAgent(name)) {
          startHeartbeats(name);
          newState(name, DefaultRobustnessController.ACTIVE);
        } else { // is node
          model.setCurrentState(name, DefaultRobustnessController.ACTIVE, NEVER);
        }
      }
    }
    public void expired(String name) {
      logger.debug("Expired Status:" + " agent=" + name + " state=LOCATED");
      if (isLocal(name)) newState(name, HEALTH_CHECK);
    }
    public void heartbeatStarted(String name) {
      logger.debug("Heartbeats started: agent=" + name);
      newState(name, DefaultRobustnessController.ACTIVE);
    }
    public void heartbeatStopped(String name) {
      logger.debug("Heartbeats stopped: agent=" + name);
    }
    public void heartbeatFailure(String name) {
      if (getState(name) == DefaultRobustnessController.ACTIVE &&
          isLocal(name)) {
        logger.info("Heartbeat timeout: agent=" + name);
        //newState(name, HEALTH_CHECK);
        doPing(name, DefaultRobustnessController.ACTIVE, DEAD);
      }
    }
  }

  /**
   * State Controller: ACTIVE
   * <pre>
   *   Entry:             Agent confirmed to be alive (via heartbeat or ping)
   *   Actions performed: None
   *   Next state:        HEALTH_CHECK on expired status or failed heartbeat
   * </pre>
   */
  class ActiveStateController extends StateControllerBase {
    public void enter(String name) {
      logger.debug("New state: agent=" + name + " state=ACTIVE");
      if (isLeader(thisAgent)) {
        //for deconflict: the applicability condition of one active agent should
        //be true and the defense op mode should be disabled.
        if(getDeconflictHelper() != null) {
          if(getDeconflictHelper().isDefenseApplicable(name))
            getDeconflictHelper().changeApplicabilityCondition(name);
          getDeconflictHelper().opmodeDisabled(name);
        }
        checkCommunityReady();
      }
      if (isLocal(name)) {
        setExpiration(name, NEVER);  // Set expiration to NEVER for local agents,
                                     // let heartbeatListener maintain status
      }
    }
    public void expired(String name) {
      //logger.info("Expired Status:" + " agent=" + name + " state=ACTIVE");
      if (isNode(name) ||
          thisAgent.equals(preferredLeader()) ||
          isLeader(name)) {
        newState(name, HEALTH_CHECK);
      }
    }
  }

  /**
   * State Controller: HEALTH_CHECK
   * <pre>
   *   Entry:             Agents status is unknown
   *   Actions performed: Ping agent
   *   Next state:        ACTIVE on successful ping
   *                      DEAD on failed ping or expired status
   * </pre>
   */
  class HealthCheckStateController extends StateControllerBase {
    public void enter(String name) {
      logger.debug("New state: agent=" + name + " state=HEALTH_CHECK");
      if (isNode(name) ||
          thisAgent.equals(preferredLeader()) ||
          isLeader(name)) {
        doPing(name, DefaultRobustnessController.ACTIVE, DEAD);
      }
    }
  }

  /**
   * State Controller: DEAD
   * <pre>
   *   Entry:             Failed ping
   *   Actions performed: Notify deconflictor
   *   Next state:        DECONFLICT
   * </pre>
   */
  class DeadStateController extends StateControllerBase {
    public void enter(String name) {
      //int priorState = model.getpriorState(name);
      //if (priorState == DECONFLICT || priorState == RESTART) {
      //  newState(name, priorState);
      //} else {
      logger.info("New state (DEAD):" +
                  " name=" + name +
                  " preferredLeader=" + thisAgent.equals(preferredLeader()) +
                  " isAgent=" + isAgent(name));
      communityReady = false; // For ACME Community Ready Events
      if (isAgent(name) && thisAgent.equals(preferredLeader())) {
        // Interface point for Deconfliction
        // If Ok'd by Deconflictor set state to RESTART
        if(isDeconflictionEnabled()) {
          newState(name, DECONFLICT);
        } else {
          newState(name, RESTART);
        }
      } else if (isNode(name) && thisAgent.equals(preferredLeader())) {
        removeFromCommunity(name);
        setExpiration(name, NEVER);
        deadNodes.add(name);
        String agentsOnDeadNode[] = model.entitiesAtLocation(name);
        for (int i = 0; i < agentsOnDeadNode.length; i++) {
          newState(agentsOnDeadNode[i], HEALTH_CHECK);
        }
        if (thisAgent.equals(preferredLeader()) && useGlobalSolver()) {
          List excludedNodes = new ArrayList(getExcludedNodes());
          LoadBalancerListener lbl = new LoadBalancerListener() {
            public void layoutReady(Map layout) {
              logger.info("layout from EN4J: " + layout);
              RestartDestinationLocator.setPreferredRestartLocations(layout);
            }
          };
          if (!newNodes.isEmpty()) {
            for (Iterator it = newNodes.iterator(); it.hasNext(); ) {
              String newNode = (String)it.next();
              if (!isVacantNode(newNode) ||
                  excludedNodes.contains(newNode) ||
                  deadNodes.contains(newNode)) {
                it.remove();
              }
            }
          }
          long pingTimeout = getLongAttribute(name, "PING_TIMEOUT", PING_TIMEOUT);
          // Use ping timeout to calculate annealTime:
          //  - convert ms to secs
          //  - divide by 2 to provide some margin
          //  - divide by 9 for EN multi-pass
          long annealTime = pingTimeout > 0 ? pingTimeout/1000/2/9 : LoadBalancer.DEFAULT_ANNEAL_TIME;
          //long annealTime = LoadBalancer.DEFAULT_ANNEAL_TIME;
          logger.debug("Get restart destinations from LoadBalancer");
          getLoadBalancer().doLayout((int)annealTime,
                                     true,
                                     new ArrayList(newNodes),
                                     new ArrayList(deadNodes),
                                     new ArrayList(getExcludedNodes()),
                                     lbl);
          newNodes.clear();
          //deadNodes.clear();
        }
      } else if (isLeader(thisAgent) && name.equals(preferredLeader())) {
        newState(name, RESTART);
      }
      if (isLocal(name)) {
        stopHeartbeats(name);
      }
    }

    public void expired(String name) {
      logger.debug("Expired Status:" + " agent=" + name + " state=DEAD");
      if (thisAgent.equals(preferredLeader())) {
        if(getDeconflictHelper() != null) {
          newState(name, DECONFLICT);
        } else {
          newState(name, RESTART);
        }
      }
    }
  }

  /**
   * State Controller: RESTART
   * <pre>
   *   Entry:             Agent determined to be DEAD and restart authorized
   *                      by deconflictor
   *   Actions performed: Determine restart location
   *                      Restart agent
   *   Next state:        ACTIVE on successful restart
   *                      FAILED_RESTART on failed restart or expired status
   * </pre>
   */
  class RestartStateController extends StateControllerBase implements RestartListener {
    { addRestartListener(this); }
    public void enter(String name) {
      if (isAgent(name) && canRestartAgent(name)) {
        String dest = RestartDestinationLocator.getRestartLocation(name, getExcludedNodes());
        logger.info("Restarting agent:" +
                    " agent=" + name +
                    " origin=" + getLocation(name) +
                    " dest=" + dest);
        if (name != null && dest != null) {
          restartAgent(name, dest);
          RestartDestinationLocator.restartOneAgent(dest);
        } else {
          logger.error("Invalid restart parameter: " +
                       " agent=" + name +
                       " dest=" + dest);
          // If no valid location is returned, restart on this node
          if (name != null) {
            dest = getLocation(thisAgent);
            restartAgent(name, dest);
            RestartDestinationLocator.restartOneAgent(dest);
          }
        }
      }
    }

    public void expired(String name) {
      if (isLeader(thisAgent)) {
        newState(name, FAILED_RESTART);
      }
    }

    public void restartInitiated(String name, String dest) {
      event("Restarting agent=" + name + " dest=" + dest);
    }

    public void restartComplete(String name, String dest, int status) {
      String community = model.getCommunityName();
      if (status == RestartHelper.SUCCESS) {
        event("Restart complete: agent=" + name + " location=" + dest +
              " community=" + community);
        RestartDestinationLocator.restartSuccess(name);
        logger.debug("Next Status:" + " agent=" + name + " state=INITIAL");
        //newState(name, DefaultRobustnessController.INITIAL);
        model.setLocationAndState(name, thisAgent, LOCATED);
      } else {
        event("Restart failed: agent=" + name + " location=" + dest +
              " community=" + community);
        newState(name, FAILED_RESTART);
      }
    }
  }

  /**
   * State Controller: FAILED_RESTART
   * <pre>
   *   Entry:             Restart attempt failed
   *   Actions performed: None
   *   Next state:        HEALTH_CHECK
   * </pre>
   */
  class FailedRestartStateController extends StateControllerBase {

    public void enter(String name) {
      logger.info("New state (FAILED_RESTART):" +
                  " name=" + name);
      if (isLeader(thisAgent) && isAgent(name)) {
        newState(name, HEALTH_CHECK);
      }
    }
    public void expired(String name) {
      if (isLeader(thisAgent) && isAgent(name)) {
        newState(name, HEALTH_CHECK);
      }
    }
  }

  /**
   * State Controller: MOVE
   * <pre>
   *   Entry:             Agent move initiated/detected
   *   Actions performed: None
   *   Next state:        HEALTH_CHECK
   * </pre>
   */
  class MoveStateController extends StateControllerBase implements MoveListener {
    { addMoveListener(this); }
    public void enter(String name) {
      communityReady = false;
    }
    public void expired(String name) {
      logger.debug("Expired Status:" + " agent=" + name + " state=MOVE");
      if (isLeader(thisAgent) || isLocal(name)) {
        newState(name, HEALTH_CHECK);
      }
    }

    public void moveInitiated(String name, String orig, String dest) {
      event("Move initiated:" +
            " agent=" + name +
            " orig=" + orig +
            " dest=" + dest);
      newState(name, MOVE);
    }

    public void moveComplete(String name, String orig, String dest, int status) {
      event("Move complete:" +
            " agent=" + name +
            " orig=" + orig +
            " dest=" + dest +
            " status=" + (status == MoveHelper.SUCCESS ? "SUCCESS" : "FAIL"));
      if (status == MoveHelper.FAIL) {
        newState(name, HEALTH_CHECK);
      } else {
        newState(name, INITIAL);
      }
    }
  }

  /**
   * State Controller: DECONFLICT
   * <pre>
   *   Entry:             Agent determined to be DEAD or the defense op mode
   *                      is enabled.
   *   Actions performed: restart a dead agent or health check the agent
   *   Next state:        RESTART on dead agent
   *                      HEALTH_CHECK on undefined agent
   * </pre>
   */
  class DeconflictStateController extends    StateControllerBase
                               implements DeconflictListener {
    { addDeconflictListener(this); }
    public void enter(String name) {
      if (isDeconflictionEnabled()) {
        if (!getDeconflictHelper().isDefenseApplicable(name)) {
          logger.debug("change condition of " + name);
          getDeconflictHelper().changeApplicabilityCondition(name);
        }
        if (getDeconflictHelper().isOpEnabled(name)) {
          newState(name, RESTART);
        }
      } else {
        newState(name, RESTART);
      }
    }
    public void expired(String name) {
      logger.info("Expired Status:" + " agent=" + name + " state=DECONFLICT");
      //newState(name, HEALTH_CHECK);
      doPing(name, DefaultRobustnessController.ACTIVE, RESTART);
    }

    public void defenseOpModeEnabled(String name) {
      if (isDeconflictionEnabled() &&
          !getDeconflictHelper().isDefenseApplicable(name)) {
        getDeconflictHelper().changeApplicabilityCondition(name);
      }
      //newState(name, HEALTH_CHECK);
      doPing(name, DefaultRobustnessController.ACTIVE, RESTART);
    }
  }

  private List deadNodes = Collections.synchronizedList(new ArrayList());
  private List newNodes = Collections.synchronizedList(new ArrayList());
  private String thisAgent;
  private WakeAlarm wakeAlarm;
  boolean communityReady = false;
  boolean startupCompleted = false;
  boolean wasRestarted = false;

  /**
   * Initializes services and loads state controller classes.
   */
  public void initialize(final MessageAddress agentId,
                         final BindingSite bs,
                         final CommunityStatusModel csm) {
    super.initialize(agentId, bs, csm);
    thisAgent = agentId.toString();
    addController(INITIAL,        "INITIAL", new InitialStateController());
    addController(LOCATED,        "LOCATED", new LocatedStateController());
    addController(DefaultRobustnessController.ACTIVE, "ACTIVE",  new ActiveStateController());
    addController(HEALTH_CHECK,   "HEALTH_CHECK",  new HealthCheckStateController());
    addController(DEAD,           "DEAD",  new DeadStateController());
    addController(RESTART,        "RESTART", new RestartStateController());
    addController(FAILED_RESTART, "FAILED_RESTART", new FailedRestartStateController());
    addController(MOVE,           "MOVE", new MoveStateController());
    addController(DECONFLICT,     "DECONFLICT", new DeconflictStateController());
    RestartDestinationLocator.setCommunityStatusModel(csm);
    RestartDestinationLocator.setLoggingService(logger);
    new HostLossThreatAlertHandler(getBindingSite(), agentId, this, csm);
    new SecurityAlertHandler(getBindingSite(), agentId, this, csm);
  }

  private boolean canRestartAgent(String name) {
    String preferredLeader = preferredLeader();
    return thisAgent.equals(preferredLeader()) ||
        (isLeader(thisAgent) && name.equals(preferredLeader()) && (getActiveHosts().size() > 1));
  }

  private Set getActiveHosts() {
    Set hosts = new HashSet();
    String activeNodes[] = model.listEntries(model.NODE, DefaultRobustnessController.ACTIVE);
    for (int i = 0; i < activeNodes.length; i++) {
      hosts.add(model.getLocation(activeNodes[i]));
    }
    return hosts;
  }

  private boolean isDeconflictionEnabled() {
   return getDeconflictHelper() != null;
  }

  private boolean useGlobalSolver() {
    String solverModeAttr = model.getStringAttribute(SOLVER_MODE_ATTRIBUTE);
    return (solverModeAttr != null && solverModeAttr.equalsIgnoreCase("global"));
  }

  private boolean autoLoadBalance() {
    String autoLoadBalanceAttr = model.getStringAttribute(AUTO_LOAD_BALANCE_ATTRIBUTE);
    return (autoLoadBalanceAttr != null && autoLoadBalanceAttr.equalsIgnoreCase("true"));
  }

  private void removeFromCommunity(String name) {
    communityService.leaveCommunity(model.getCommunityName(), name, null);
  }

  public void setupSubscriptions() {
    logger.info("didRehydrate=" + blackboard.didRehydrate());
    if (blackboard.didRehydrate()) {
      wasRestarted = true;
      startupCompleted = true;
    }
    wakeAlarm = new WakeAlarm((new Date()).getTime() + STATUS_INTERVAL);
    alarmService.addRealTimeAlarm(wakeAlarm);
  }

  public void execute() {
    if ((wakeAlarm != null) &&
        ((wakeAlarm.hasExpired()))) {
      if (isLeader(thisAgent)) {
        logger.info(statusSummary());
        checkCommunityReady();
        checkLoadBalance();
      }
      wakeAlarm = new WakeAlarm((new Date()).getTime() + STATUS_INTERVAL);
      alarmService.addRealTimeAlarm(wakeAlarm);
    }
  }

  /**
   * Send Community Ready event if all expected agents are found and active.
   */
  protected void checkCommunityReady() {
    if (thisAgent.equals(preferredLeader()) &&
        isLeader(thisAgent) &&
        communityReady == false &&
        agentsAndLocationsActive()) {
      communityReady = true;
      if (!startupCompleted) {
        startupCompleted = true;
        if (doInitialPing()) {
          pingAll();
        }
      }
      event("Community " + model.getCommunityName() + " Ready");
      RestartDestinationLocator.clearRestarts();
    }
  }

  private void setPingTimeout() {
    if (pingStats.getCount() > 10) {
      long oldPingTimeout = getLongAttribute("PING_TIMEOUT", PING_TIMEOUT);
      long newPingTimeout = (long)pingStats.getMean() + ((long)pingStats.getStandardDeviation() * 4);
      long minPingTimeout = getLongAttribute("MINIMUM_PING_TIMEOUT", MINIMUM_PING_TIMEOUT);
      if (newPingTimeout < minPingTimeout) newPingTimeout = minPingTimeout;
      logger.info("Change PingTimeout: old=" + oldPingTimeout +
                  " new=" + newPingTimeout + " PingStats=(" + pingStats + ")");
      changeAttributes(model.getCommunityName(), null,
                       new Attribute[]{new BasicAttribute("PING_TIMEOUT", Long.toString(newPingTimeout))});
    }
  }

  /**
   * Verify that all expected agents and their current locations are ACTIVE.
   * @return True if the number of active agents equals the expected agents
   *         and their current locations are also active
   */
  private boolean agentsAndLocationsActive() {
    String agents[] =
        model.listEntries(model.AGENT, DefaultRobustnessController.ACTIVE);
    if (agents.length < expectedAgents()) return false;
    List nodes = new ArrayList();
    boolean inactiveNode = false;
    String location = null;
    String agent = null;
    for (int i = 0; i < agents.length; i++) {
      agent = agents[i];
      location = model.getLocation(agent);
      if (!nodes.contains(location)) {
        if (model.getCurrentState(location) != DefaultRobustnessController.ACTIVE) {
          inactiveNode = true;
          break;
        }
        nodes.add(location);
      }
    }
    /*
     logger.info("checkCommunityReady:" +
                " agents=" + expectedAgents() +
                " active=" + agents.length +
                (inactiveNode ? " inactiveNode=" + location + " agent=" + agent : ""));
     */
    return !inactiveNode;
  }

  /**
   * Receives notification of membership changes.
   */
  public void membershipChange(String name) {
    if (isNode(name)) {
      newNodes.add(name);
      logger.debug("New node detected: name=" + name);
    }
  }

  /**
   * Receives notification of leader changes.
   */
  public void leaderChange(String priorLeader, String newLeader) {
    logger.info("LeaderChange: prior=" + priorLeader + " new=" + newLeader);
    if (isLeader(thisAgent) && model.getCurrentState(preferredLeader()) == DEAD) {
      newState(preferredLeader(), RESTART);
    }
    if (thisAgent.equals(preferredLeader()) && startupCompleted && wasRestarted) {
      pingAll();
    }
    checkCommunityReady();
  }

  /**
   * Receives notification of change in agent location.
   */
  public void locationChange(String name, String priorLocation, String newLocation) {
    // If agent has moved off this node stop heartbeats
    if (thisAgent.equals(priorLocation)) stopHeartbeats(name);
    checkCommunityReady();
  }

  /**
   * Identifies state considered "Normal" by this controller.
   */
  public int getNormalState() { return DefaultRobustnessController.ACTIVE; }

  /**
   * State to trigger leader re-election.
   */
  public int getLeaderElectionTriggerState() { return DEAD; }

  public long expectedAgents() {
    return getLongAttribute("NumberOfAgents", -1);  }

  public String preferredLeader() {
    return model.getStringAttribute(model.MANAGER_ATTR);
  }

  private boolean doInitialPing() {
    if (model != null) {
      String attrVal = model.getAttribute("DO_INITIAL_PING");
      return (attrVal == null || attrVal.equalsIgnoreCase("True"));
    }
    return true;
  }

  private boolean monitorStartup() {
    if (model != null) {
      String attrVal = model.getAttribute("MONITOR_STARTUP");
      return (attrVal != null && attrVal.equalsIgnoreCase("True"));
    }
    return false;
  }

  protected void pingAll() {
    String agents[] = model.listAllEntries();
    for (int i = 0; i < agents.length; i++) {
      doPing(agents[i], DefaultRobustnessController.ACTIVE, DEAD);
    }
  }

  /**
   * Ping an agent and update current state based on result.
   * @param name            Agent to ping
   * @param stateOnSuccess  New state if ping succeeds
   * @param stateOnFail     New state if ping fails
   */
  protected void doPing(String name,
                        final int stateOnSuccess,
                        final int stateOnFail) {
    final long pingTimeout = calcPingTimeout(name);
    //logger.debug("doPing: agent=" + name + " timeout=" + pingTimeout);
    getPingHelper().ping(name, pingTimeout, new PingListener() {
      public void pingComplete(String name, int status, long time) {
        if (status == PingHelper.SUCCESS) {
          newState(name, stateOnSuccess);
          if (!isLocal(name)) {
            pingStats.enter(time);
            if (pingStats.getCount() == 10 || pingStats.getCount()%25 == 0) {
              setPingTimeout();
            }
          }
        } else {
          logger.info("Ping:" +
                       " agent=" + name +
                       " state=" + stateName(model.getCurrentState(name)) +
                       " timeout=" + pingTimeout +
                       " actual=" + time +
                       " result=" + (status == PingHelper.SUCCESS ? "SUCCESS" : "FAIL") +
                       (status == PingHelper.SUCCESS ? "" : " newState=" + stateName(stateOnFail)));
          newState(name, stateOnFail);
        }
      }
    });
  }

  protected long calcPingTimeout(String name) {
    double nodeLoadCoefficient = 1.0;
    String node = model.getLocation(name);
    double nodeLoad = getNodeLoadAverage(node);
    if (nodeLoad > 0) {
      nodeLoad = (nodeLoad > MAX_CPU_LOAD_FOR_ADJUSTMENT)
                 ? MAX_CPU_LOAD_FOR_ADJUSTMENT
                 : nodeLoad;
      nodeLoadCoefficient = 1.0 +
          (nodeLoad/MAX_CPU_LOAD_FOR_ADJUSTMENT * (MAX_CPU_LOAD_SCALING - 1.0));
    }
    double pingAdjustment = getDoubleAttribute(name, PING_ADJUSTMENT, 1.0);
    long pingTimeout = getLongAttribute(name, "PING_TIMEOUT", PING_TIMEOUT);
    long result = (long)(pingTimeout * nodeLoadCoefficient * pingAdjustment);
    logger.debug("calcPingTimeout:" +
                " agent=" + name +
                " baseTimeout=" + pingTimeout +
                " nodeLoadCoefficient=" + nodeLoadCoefficient +
                " pingAdjust=" + pingAdjustment +
                " result=" + result);

    return result;
  }

  protected Set getExcludedNodes() {
    Set excludedNodes = new HashSet();
    String allNodes[] = model.listEntries(model.NODE);
    for (int i = 0; i < allNodes.length; i++) {
      if (model.hasAttribute(model.getAttributes(allNodes[i]), "UseForRestarts", "False")) {
        excludedNodes.add(allNodes[i]);
      }
    }
    return excludedNodes;
  }

  protected boolean isVacantNode(String name) {
    return model.entitiesAtLocation(name).length == 0;
  }

  protected void checkLoadBalance() {
    // Don't load balance if community is not ready or is busy
    if (autoLoadBalance() && communityReady && !isCommunityBusy()) {
      if (!deadNodes.isEmpty() || !newNodes.isEmpty()) {
        List excludedNodes = new ArrayList(getExcludedNodes());
        // Remove occupied nodes from newNodes list
        if (!newNodes.isEmpty()) {
          for (Iterator it = newNodes.iterator(); it.hasNext(); ) {
            String newNode = (String)it.next();
            if (!isVacantNode(newNode) ||
                excludedNodes.contains(newNode) ||
                deadNodes.contains(newNode)) {
              it.remove();
            }
          }
        }
        // invoke load balancer if there are new (vacant) nodes
        if (!newNodes.isEmpty()) {
          LoadBalancerListener lbl = new LoadBalancerListener() {
            public void layoutReady(Map layout) {
              logger.debug("layout from EN4J: " + layout);
              getLoadBalancer().moveAgents(layout);
            }
          };
          logger.info("autoLoadBalance");
          getLoadBalancer().doLayout(LoadBalancer.DEFAULT_ANNEAL_TIME,
                                     true,
                                     new ArrayList(newNodes),
                                     new ArrayList(deadNodes),
                                     excludedNodes,
                                     lbl);
        }
        newNodes.clear();
        //deadNodes.clear();
      }
    }
  }

}
