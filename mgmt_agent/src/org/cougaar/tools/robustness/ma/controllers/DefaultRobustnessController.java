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
import org.cougaar.tools.robustness.ma.ReaffiliationNotificationHandler;
import org.cougaar.tools.robustness.ma.HostLossThreatAlertHandler;
import org.cougaar.tools.robustness.ma.SecurityAlertHandler;
import org.cougaar.tools.robustness.ma.ldm.HealthMonitorRequest;
import org.cougaar.tools.robustness.ma.util.CoordinatorHelper;
import org.cougaar.tools.robustness.ma.util.CoordinatorHelperFactory;
import org.cougaar.tools.robustness.ma.util.CoordinatorListener;
import org.cougaar.tools.robustness.ma.util.HeartbeatListener;
import org.cougaar.tools.robustness.ma.util.LoadBalancer;
import org.cougaar.tools.robustness.ma.util.LoadBalancerListener;
import org.cougaar.tools.robustness.ma.util.MoveHelper;
import org.cougaar.tools.robustness.ma.util.MoveListener;
import org.cougaar.tools.robustness.ma.util.PingHelper;
import org.cougaar.tools.robustness.ma.util.RestartHelper;
import org.cougaar.tools.robustness.ma.util.RestartListener;
import org.cougaar.tools.robustness.ma.util.RestartDestinationLocator;
import org.cougaar.tools.robustness.ma.util.StatCalc;
import org.cougaar.tools.robustness.ma.util.NodeLatencyStatistics;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.service.community.CommunityResponseListener;
import org.cougaar.core.service.community.CommunityResponse;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.core.mts.MessageAddress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultRobustnessController extends RobustnessControllerBase {

  public static final int INITIAL        = 0;
  //public static final int LOCATED        = 1;
  public static final int ACTIVE         = 2;
  public static final int HEALTH_CHECK   = 3;
  public static final int DEAD           = 4;
  public static final int RESTART        = 5;
  public static final int FAILED_RESTART = 6;
  public static final int MOVE           = 7;
  public static final int DECONFLICT     = 8;
  public static final int FORCED_RESTART = 9;

  // Determines how often the status summary is logged
  public static final long STATUS_INTERVAL = 2 * 60 * 1000;

  /**
   * State Controller: INITIAL
   * <pre>
   *   Entry:             Agent discovered by a NodeHealthMonitorPlugin
   *   Actions performed: Start heartbeats
   *   Next state:        ACTIVE if heartbeats successfully started
   *                      DEAD if heartbeats can't be started
   * </pre>
   */
  class InitialStateController extends    StateControllerBase
                               implements HeartbeatListener {
    { addHeartbeatListener(this); }
    public void enter(String name) {
      if (isLocal(name)) {
        if (name.equals(thisAgent)) {
          newState(name, DefaultRobustnessController.ACTIVE);
        } else {
          startHeartbeats(name);
        }
      }
    }
    public void heartbeatStarted(String name) {
      if (logger.isDebugEnabled()) {
        logger.debug("Heartbeats started: agent=" + name);
      }
      newState(name, DefaultRobustnessController.ACTIVE);
    }
    public void heartbeatStopped(String name) {
      if (logger.isDebugEnabled()) {
        logger.debug("Heartbeats stopped: agent=" + name);
      }
    }
    public void heartbeatFailure(String name) {
      if (logger.isInfoEnabled()) {
        logger.info("Heartbeat timeout: agent=" + name +
                    " state=" + stateName(getState(name)));
      }
      if (isLocal(name)) {
        switch (getState(name)) {
          case DefaultRobustnessController.ACTIVE:
            doPing(name, DefaultRobustnessController.ACTIVE, DEAD);
            break;
          default: // Retry
            startHeartbeats(name);
            break;
        }
      }
    }
  }

  /**
   * State Controller: ACTIVE
   * <pre>
   *   Entry:             Agent determined to be alive (via heartbeat or
   *                      node status update)
   *   Actions performed: None
   *   Next state:        DEAD on expired status
   * </pre>
   */
  class ActiveStateController extends StateControllerBase {
    public void enter(String name) {
      if (isSentinel()) {
        // Cleanup previous restart
        getCoordinatorHelper().opmodeDisabled(name);
        // Set current diagnosis to "Live"
        getCoordinatorHelper().setDiagnosis(name, CoordinatorHelper.LIVE);
        checkCommunityReady();
      }
      if (isAgent(name) || thisAgent.equals(name)) {
        setExpiration(name, NEVER);  // Set expiration to NEVER for all agents,
                                     // let heartbeatListener maintain status
      } else if (isNode(name)) {
        setExpiration(name, (int)getNodeStatusExpiration(name));
      }
    }
    public void expired(String name) {
      if (logger.isInfoEnabled()) {
        logger.info("Expired Status:" + " agent=" + name + " state=ACTIVE");
      }
      if ((isLocal(name) || isNode(name)) ||
          (isSentinel() && getState(getLocation(name)) == DEAD) ||
          isLeader(name)) {
        newState(name, DEAD);
      }
    }
  }

  /**
   * State Controller: DECONFLICT
   * <pre>
   *   Entry:             Agent determined to be DEAD or the defense op mode
   *                      is enabled.
   *   Actions performed: RESTART a dead agent when defense is enabled
   *   Next state:        ACTIVE if status updated while defense is disabled
   *                      RESTART if status not changed while defense disabled
   * </pre>
   */
  class DeconflictStateController extends    StateControllerBase
                                  implements CoordinatorListener {
    public void enter(String name) {
      if (isCoordinatorEnabled()) {
        if (getCoordinatorHelper().isOpEnabled(name)) {
          newState(name, RESTART);
        } else if (!getCoordinatorHelper().isDefenseApplicable(name)) {
          getCoordinatorHelper().setDiagnosis(name, CoordinatorHelper.DEAD);
        }
      } else {
        newState(name, HEALTH_CHECK);
      }
    }

    public void actionEnabled(String agentName) {
      if (logger.isDetailEnabled()) {
        logger.detail("deconflictCallback: agent=" + agentName);
      }
      if (isCoordinatorEnabled() &&
          !getCoordinatorHelper().isDefenseApplicable(agentName)) {
        getCoordinatorHelper().setDiagnosis(agentName, CoordinatorHelper.DEAD);
      }
      // Verify that agent state hasn't changed while defense was disabled
      if (getState(agentName) == DECONFLICT) {
        newState(agentName, RESTART);
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
      if (isNode(name) ||
          isSentinel() ||
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
    public void enter(final String name) {
      setExpiration(name, NEVER);
      communityReady = false; // For ACME Community Ready Events
      if (isAgent(name) &&
          isSentinel() &&
          (getPriorState(name) == DefaultRobustnessController.ACTIVE ||
          getPriorState(name) < INITIAL || getPriorState(name) == HEALTH_CHECK)) {
        newState(name, DECONFLICT);
      } else if (isNode(name)) {
        deadNodes.add(name);
        if (!useGlobalSolver()) {
          newState(agentsOnNode(name), DEAD);
          if (isSentinel()) { removeFromCommunity(name); }
        } else {
          if (isLeader(thisAgent)) {
            if (agentsOnNode(name).size() > 0) {
              final long minAnnealTime = getLongAttribute("MINIMUM_ANNEAL_TIME", MINIMUM_ANNEAL_TIME);
                getLayout(new LoadBalancerListener() {
                public void layoutReady(Map layout) {
                  if (getState(name) == DEAD) {
                    if (logger.isInfoEnabled()) {
                      logger.info("layout from EN4J: " + layout);
                    }
                    RestartDestinationLocator.setPreferredRestartLocations(layout);
                    newState(agentsOnNode(name), DEAD);
                    if (isSentinel()) {
                      removeFromCommunity(name);
                    }
                  } else { // Abort, node no longer classified as DEAD
                    deadNodes.remove(name);
                    if (logger.isInfoEnabled()) {
                      logger.info("Restart aborted: node=" + name +
                                  " state=" + stateName(getState(name)));
                    }
                  }
                }
              });
            } else {
              removeFromCommunity(name);
            }
          } else {
            newState(agentsOnNode(name), DEAD);
          }
        }
      }
      if (isLocal(name)) {
        stopHeartbeats(name);
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
          restartAgent(name);
        }
    }

    public void expired(String name) {
      if (isLeader(thisAgent)) {
        newState(name, FAILED_RESTART);
      }
    }

    public void actionInitiated(String name, int action, String dest) {
      if (action == HealthMonitorRequest.RESTART) {
        event("Restarting agent=" + name + " dest=" + dest);
      }
    }

    public void actionComplete(String name, int action, String dest, int status) {
      String community = model.getCommunityName();
      if (action == HealthMonitorRequest.RESTART) {
        if (status == RestartHelper.SUCCESS) {
          event("Restart complete: agent=" + name + " location=" + dest +
                " community=" + community);
          RestartDestinationLocator.restartSuccess(name);
          if (logger.isDebugEnabled()) {
            logger.debug("Next Status:" + " agent=" + name + " state=INITIAL");
          }
          //newState(name, DefaultRobustnessController.INITIAL);
          model.setLocationAndState(name, thisAgent, INITIAL);
        } else {
          event("Restart failed: agent=" + name + " location=" + dest +
                " community=" + community);
          newState(name, FAILED_RESTART);
        }
      }
    }
  }

  // Track restart timeouts on nodes that are thought to be alive.  In these
  // cases a retry is only attempted on the second timeout.
  private List retries = Collections.synchronizedList(new ArrayList());

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
      if (logger.isInfoEnabled()) {
        logger.info("New state (FAILED_RESTART):" +
                    " name=" + name);
      }
      if (isLeader(thisAgent) &&
          isAgent(name)) {
        if (getState(model.getLocation(name)) == DefaultRobustnessController.ACTIVE) {
          if (retries.contains(name)) {
            // Retry the restart on second timeout if destination node is alive
            retries.remove(name);
            newState(name, RESTART);
          } else {
            retries.add(name);
          }
        } else {
          // If destination node isn't alive retry restart immediately
          newState(name, RESTART);
        }
      }
    }
    public void expired(String name) {
      if (isLeader(thisAgent) && isAgent(name)) {
        newState(name, RESTART);
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
      if (logger.isDebugEnabled()) {
        logger.debug("Expired Status:" + " agent=" + name + " state=MOVE");
      }
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
   * State Controller: FORCED_RESTART
   * <pre>
   *   Entry:             External request received
   *   Actions performed: Kill and restart specified agent
   *   Next state:        ACTIVE
   * </pre>
   */
  class ForcedRestartStateController extends StateControllerBase {
    private IncrementalSubscription forcedRestartRequests;
    public void setupSubscriptions() {
      forcedRestartRequests = (IncrementalSubscription)blackboard.subscribe(new UnaryPredicate() {
        public boolean execute (Object o) {
          return (o instanceof HealthMonitorRequest &&
          ((HealthMonitorRequest)o).getRequestType() == HealthMonitorRequest.FORCED_RESTART);
        }
      });
    }
    public void execute() {
      for (Iterator it = forcedRestartRequests.getAddedCollection().iterator(); it.hasNext(); ) {
        HealthMonitorRequest hmr = (HealthMonitorRequest) it.next();
        if (logger.isInfoEnabled()) {
          logger.info("Received FORCED_RESTART request:" + hmr);
        }
        String agentsToRestart[] = hmr.getAgents();
        for (int i = 0; i < agentsToRestart.length; i++) {
          killAgent(agentsToRestart[i]);
          String curNode = model.getLocation(agentsToRestart[i]);
          String destNode = hmr.getDestinationNode();
          if (destNode == null) {
            Set excludedNodes = getExcludedNodes();  // dead nodes
            excludedNodes.add(curNode);  // agents current node
            destNode =
                RestartDestinationLocator.getRestartLocation(agentsToRestart[i],
                getExcludedNodes());
          }
          restartAgent(agentsToRestart[i], destNode);
        }
      }
    }
  }

  private Set deadNodes = Collections.synchronizedSet(new HashSet());
  private String thisAgent;
  private WakeAlarm wakeAlarm;
  boolean communityReady = false;
  boolean didRestart = false;

  private Map runStats = new HashMap();
  private NodeLatencyStatistics originalStats;
  private NodeLatencyStatistics nodeLatencyStats;
  private boolean collectNodeStats = false;
  private boolean suppressPingsOnRestart = false;

  /**
   * Initializes services and loads state controller classes.
   */
  public void initialize(final MessageAddress agentId,
                         final BindingSite bs,
                         final CommunityStatusModel csm) {
    super.initialize(agentId, bs, csm);
    thisAgent = agentId.toString();
    String propValue = System.getProperty(COLLECT_NODE_STATS_PROPERTY);
    collectNodeStats = (propValue != null && propValue.equalsIgnoreCase("true"));
    propValue = System.getProperty("org.cougaar.tools.robustness.deconfliction.leashOnRestart");
    suppressPingsOnRestart =
        (propValue != null && propValue.equalsIgnoreCase("true"));
    addController(INITIAL,        "INITIAL", new InitialStateController());
    addController(DefaultRobustnessController.ACTIVE, "ACTIVE",  new ActiveStateController());
    addController(HEALTH_CHECK,   "HEALTH_CHECK",  new HealthCheckStateController());
    addController(DEAD,           "DEAD",  new DeadStateController());
    addController(RESTART,        "RESTART", new RestartStateController());
    addController(FAILED_RESTART, "FAILED_RESTART", new FailedRestartStateController());
    addController(MOVE,           "MOVE", new MoveStateController());
    addController(DECONFLICT,     "DECONFLICT", new DeconflictStateController());
    addController(FORCED_RESTART, "FORCED_RESTART", new ForcedRestartStateController());
    RestartDestinationLocator.setCommunityStatusModel(csm);
    RestartDestinationLocator.setLoggingService(logger);
    new HostLossThreatAlertHandler(getBindingSite(), agentId, this, csm);
    new SecurityAlertHandler(getBindingSite(), agentId, this, csm);
    new ReaffiliationNotificationHandler(getBindingSite(), agentId, csm);
  }

/**
 * Invoked when a status update is received from a monitoring node.
 * @param nodeName Name of reporting node
 */
  protected void statusUpdateReceived(String nodeName) {
    updateStats(nodeName);
  }

  private void updateStats(String source) {
    long updateInterval =
        getLongAttribute(STATUS_UPDATE_INTERVAL_ATTRIBUTE,
                         DEFAULT_STATUS_UPDATE_INTERVAL);
    long now = now();
    StatsEntry se = (StatsEntry) runStats.get(source);
    if (se == null) {
      runStats.put(source, new StatsEntry(source, now));
    } else {
      long elapsed = now - se.last;
      if (elapsed >= updateInterval) {  // filter bad events
        long latency = elapsed - updateInterval;
        if (latency > se.high) {
          se.high = latency;
        }
        ++se.samples;
        collectNodeStats(source);
      }
      se.last = now;
    }
  }

  private void collectNodeStats(String source) {
    if (collectNodeStats) {
      if (nodeLatencyStats == null) { initializeNodeStats(); }
      if (isSentinel() &&
          !source.equals(getLocation(thisAgent))) {
        StatsEntry se = (StatsEntry) runStats.get(source);
        if (se.samples >= MIN_SAMPLES) {
          if (se.high < MIN_SAMPLE_VALUE) {
            se.high = MIN_SAMPLE_VALUE;
          }
          StatCalc newSc = (StatCalc) nodeLatencyStats.get(source);
            if (se.samples == MIN_SAMPLES ||                    // Take snapshot at MIN_SAMPLES
               newSc == null ||
               se.high > newSc.getHigh()) {                     // Record highs thereafter
              if (originalStats.contains(source)) {
                newSc = (StatCalc) originalStats.get(source).clone();
                //if (se.high > newSc.getMean() / 2) { //   sanity check
                  newSc.enter(se.high);
                  nodeLatencyStats.put(newSc);
                  saveNodeStats();
                //}
              } else {
                newSc = new StatCalc(model.getCommunityName(), source);
                newSc.enter(se.high);
                nodeLatencyStats.put(newSc);
                saveNodeStats();
              }
            }
        }
      }
    }
  }

  private void saveNodeStats() {
    NodeLatencyStatistics all =
        new NodeLatencyStatistics(model.getCommunityName(),
                                  nodeLatencyStats.values());
    for (Iterator it = originalStats.list().iterator(); it.hasNext();) {
      String id = (String)it.next();
      if (!all.contains(id)) all.put(originalStats.get(id));
    }
    all.save();
  }

  // load persisted node stats from file and make a copy
  private void initializeNodeStats() {
    originalStats = new NodeLatencyStatistics(model.getCommunityName());
    nodeLatencyStats = new NodeLatencyStatistics(model.getCommunityName());
    originalStats.load();
    if (logger.isDetailEnabled()) {
      logger.detail("initializeNodeStats: " + originalStats.toXML());
    }
  }

  /**
   * Calculate expiration time for node status.  This value determines how long
   * the manager waits for a status update before reporting the problem and
   * initiating a restart.  The expiration time is based on statistical data
   * obtained from the community attributes.
   * @param nodeName String  Name of node
   * @return long Expiration in milliseconds
   */
  private long getNodeStatusExpiration(String nodeName) {

    // Community-wide default values
    long defaultMean = getLongAttribute("DEFAULT_STATUS_LATENCY_MEAN",
                                        DEFAULT_STATUS_LATENCY_MEAN);
    long defaultStdDev = getLongAttribute("DEFAULT_STATUS_LATENCY_STDDEV",
                                          DEFAULT_STATUS_LATENCY_STDDEV);
    long updateInterval = getLongAttribute("STATUS_UPDATE_INTERVAL",
                                           DEFAULT_STATUS_UPDATE_INTERVAL);
    long restartConf = getLongAttribute("RESTART_CONFIDENCE",
                                        DEFAULT_RESTART_CONFIDENCE);

    // Node-specific values; community defaults used if not available
    long nodeMean = getLongAttribute(nodeName,
                                     "STATUS_LATENCY_MEAN",
                                     defaultMean);
    long nodeStdDev = getLongAttribute(nodeName,
                                       "STATUS_LATENCY_STDDEV",
                                       defaultStdDev);

    long expiration = updateInterval +
                      nodeMean +
                      (nodeStdDev * restartConf);
    if (logger.isInfoEnabled()) {
      logger.info("getNodeStateExpiration: node=" + nodeName + " expiration=" +
                  expiration);
    }
    return expiration;
  }

  private boolean canRestartAgent(String name) {
    return isSentinel() ||
        (isLeader(thisAgent) && name.equals(preferredLeader()) && (getActiveHosts().size() > 1));
  }

  private void restartAgent(String name) {
    String dest =
        RestartDestinationLocator.getRestartLocation(name, getExcludedNodes());
    if (logger.isInfoEnabled()) {
      logger.info("Restarting agent:" +
                  " agent=" + name +
                  " origin=" + getLocation(name) +
                  " dest=" + dest);
    }
    if (retries.contains(name)) {
      retries.remove(name);
    }
    if (name != null && dest != null) {
      restartAgent(name, dest);
      RestartDestinationLocator.restartOneAgent(dest);
    } else {
      if (logger.isErrorEnabled()) {
        logger.error("Invalid restart parameter: " +
                     " agent=" + name +
                     " dest=" + dest);
      }
      // If no valid location is returned, restart on this node
      if (name != null) {
        dest = getLocation(thisAgent);
        restartAgent(name, dest);
        RestartDestinationLocator.restartOneAgent(dest);
      }
    }
  }

  private Set getActiveHosts() {
    Set hosts = new HashSet();
    String activeNodes[] = model.listEntries(model.NODE, DefaultRobustnessController.ACTIVE);
    for (int i = 0; i < activeNodes.length; i++) {
      hosts.add(model.getLocation(activeNodes[i]));
    }
    return hosts;
  }

  private boolean isCoordinatorEnabled() {
   return getCoordinatorHelper() != null;
  }

  private boolean useGlobalSolver() {
    String solverModeAttr = model.getStringAttribute(SOLVER_MODE_ATTRIBUTE);
    return (solverModeAttr != null && solverModeAttr.equalsIgnoreCase("global"));
  }

  private boolean autoLoadBalance() {
    String autoLoadBalanceAttr = model.getStringAttribute(AUTO_LOAD_BALANCE_ATTRIBUTE);
    return (autoLoadBalanceAttr != null && autoLoadBalanceAttr.equalsIgnoreCase("true"));
  }

  private void removeFromCommunity(final String name) {
    if (logger.isInfoEnabled()) {
      logger.info("removeFromCommunity:" +
                  " community=" + model.getCommunityName() +
                  " entity=" + name);
    }
    communityService.leaveCommunity(model.getCommunityName(), name,
      new CommunityResponseListener() {
        public void getResponse(CommunityResponse resp) {
          if (logger.isInfoEnabled()) {
            logger.info("removeFromCommunity:" +
                        " entity=" + name +
                        " result=" + resp.getStatusAsString());
          }
        }
      });
  }

  public void setupSubscriptions() {
    didRestart = blackboard.didRehydrate();
    wakeAlarm = new WakeAlarm((new Date()).getTime() + STATUS_INTERVAL);
    alarmService.addRealTimeAlarm(wakeAlarm);
  }

  public void execute() {
    if ((wakeAlarm != null) && ((wakeAlarm.hasExpired()))) {
      if (isSentinel()) {
        if (logger.isInfoEnabled()) {
          logger.info(statusSummary());
        }
        checkCommunityReady();
        checkLoadBalance();
      }
      wakeAlarm = new WakeAlarm(now() + STATUS_INTERVAL);
      alarmService.addRealTimeAlarm(wakeAlarm);
    }
  }

  /**
   * Send Community Ready event if all expected agents are found and active.
   */
  private void checkCommunityReady() {
    if (isSentinel() &&
        isLeader(thisAgent) &&
        communityReady == false &&
        agentsAndLocationsActive()) {
      communityReady = true;
      event("Community " + model.getCommunityName() + " Ready");
      RestartDestinationLocator.clearRestarts();
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
    return !inactiveNode;
  }

  /**
   * Receives notification of membership changes.
   */
  public void memberAdded(String name) {
    //robustness manager should publish deconfliction objects for every agent member.
    if (isSentinel()) {
      if (logger.isInfoEnabled()) {
        logger.info("memberAdded: name=" + name);
      }
      coordinatorHelper.addAgent(name);
      if (isNode(name)) {
        if (logger.isInfoEnabled()) {
          logger.info("New node detected: name=" + name);
        }
        if (deadNodes.contains(name)) {
          deadNodes.remove(name);
        }
      }
    }
    if (didRestart && getState(name) == -1 && !suppressPingsOnRestart) {
      newState(name, HEALTH_CHECK);
    }
  }

  public void memberRemoved(String name) {
    if (isSentinel()) {
      if (logger.isInfoEnabled()) {
        logger.info("memberRemoved: name=" + name);
      }
      coordinatorHelper.removeAgent(name);
    }
  }

  /**
   * Receives notification of leader changes.
   */
  public void leaderChange(String priorLeader, String newLeader) {
    if (logger.isInfoEnabled()) {
      logger.info("LeaderChange: prior=" + priorLeader + " new=" + newLeader);
    }
    if (isLeader(thisAgent) && model.getCurrentState(preferredLeader()) == DEAD) {
      newState(preferredLeader(), RESTART);
    }
    checkCommunityReady();
    if (didRestart && !suppressPingsOnRestart) {
      newState(model.listEntries(model.AGENT, -1), HEALTH_CHECK);
      newState(model.listEntries(model.NODE, -1), HEALTH_CHECK);
    }
    // Setup defense coordination
    if (isSentinel() && !isCoordinatorEnabled()) {
      coordinatorHelper = CoordinatorHelperFactory.getCoordinatorHelper(getBindingSite());
      coordinatorHelper.addListener((DeconflictStateController)getController(DECONFLICT));
      String enable = System.getProperty(DECONFLICTION, PROPERTY_ENABLED);
      if (enable.equalsIgnoreCase(PROPERTY_ENABLED)) {
        if (logger.isInfoEnabled()) {
          logger.info("Deconfliction enabled: community=" + model.getCommunityName());
        }
        String allAgents[] = model.listEntries(CommunityStatusModel.AGENT);
        for (int i = 0; i < allAgents.length; i++) {
          coordinatorHelper.addAgent(allAgents[i]);
        }
      }
    }
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

  private long expectedAgents() {
    return getLongAttribute(EXPECTED_AGENTS_ATTRIBUTE, -1);  }

  private String preferredLeader() {
    return model.getStringAttribute(model.MANAGER_ATTR);
  }

  private boolean isSentinel() {
    return thisAgent.equals(preferredLeader());
  }

  boolean loadBalanceInProcess = false;

  private long now() {
    return System.currentTimeMillis();
  }

  private List pendingLBRequests = Collections.synchronizedList(new ArrayList());
  /**
   * Get layout from EN4J load balancer.  Check node status prior to submitting
   * request to EN4J.
   * @param lbl Listener method invoked after layout has been returned by EN4J
   */
  private void getLayout(final LoadBalancerListener lbl) {
    synchronized (pendingLBRequests) {
      // Guard against overlapping doLayout requests
      if (loadBalanceInProcess) {
        pendingLBRequests.add(lbl);
      } else {
        loadBalanceInProcess = true;
        List excludedNodes = new ArrayList(getExcludedNodes());
        List vacantNodes = getVacantNodes();
        // Update list of NEW nodes
        if (!vacantNodes.isEmpty()) {
          for (Iterator it = vacantNodes.iterator(); it.hasNext(); ) {
            String newNode = (String) it.next();
            if (!isVacantNode(newNode) ||
                excludedNodes.contains(newNode) ||
                deadNodes.contains(newNode)) {
              it.remove();
            }
          }
        }
        LoadBalancerListener newLbl = new LoadBalancerListener() {
          public void layoutReady(Map layout) {
            loadBalanceInProcess = false;
            lbl.layoutReady(layout);
            if (!pendingLBRequests.isEmpty()) {
              getLayout( (LoadBalancerListener) pendingLBRequests.remove(0));
            }
          }
        };
        long annealTime = getLongAttribute("MINIMUM_ANNEAL_TIME",
                                           MINIMUM_ANNEAL_TIME);
        if (logger.isDebugEnabled()) {
          logger.debug("doLayout:" +
                       " annealTime=" + annealTime +
                       " newNodes=" + vacantNodes +
                       " deadNodes=" + deadNodes +
                       " excludedNodes=" + excludedNodes);
        }
        getLoadBalancer().doLayout( (int) annealTime,
                                   true,
                                   vacantNodes,
                                   new ArrayList(deadNodes),
                                   excludedNodes,
                                   newLbl);

      }
    }
  }

  /**
   * Ping an agent and update current state based on result.
   * @param agent           Agent to ping
   * @param stateOnSuccess  New state if ping succeeds
   * @param stateOnFail     New state if ping fails
   */
  private void doPing(String agent,
                        final int stateOnSuccess,
                        final int stateOnFail) {
    PingHelper pinger = getPingHelper();
    if (pinger.pingInProcess(agent)) {
      if (logger.isDetailEnabled()) {
        logger.detail(
            "Duplicate ping requested, new ping not performed: agent=" + agent);
      }
    } else {
      doPing(new String[]{agent}, stateOnSuccess, stateOnFail);
    }
  }

  private void pingAll(final int stateOnSuccess,
                         final int stateOnFail) {
    doPing(model.listEntries(model.AGENT), stateOnSuccess, stateOnFail);
  }

  private void checkLoadBalance() {
    // Don't load balance if community is not ready
    if (autoLoadBalance() && communityReady) {
      List vacantNodes = getVacantNodes();
      if (!deadNodes.isEmpty() || !vacantNodes.isEmpty()) {
        List excludedNodes = new ArrayList(getExcludedNodes());
        // Remove occupied nodes from newNodes list
        if (!vacantNodes.isEmpty()) {
          for (Iterator it = vacantNodes.iterator(); it.hasNext(); ) {
            String newNode = (String)it.next();
            if (!isVacantNode(newNode) ||
                excludedNodes.contains(newNode) ||
                deadNodes.contains(newNode)) {
              it.remove();
            }
          }
        }
        // invoke load balancer if there are new (vacant) nodes
        if (!vacantNodes.isEmpty()) {
          LoadBalancerListener lbl = new LoadBalancerListener() {
            public void layoutReady(Map layout) {
              if (logger.isDebugEnabled()) {
                logger.debug("layout from EN4J: " + layout);
              }
              getLoadBalancer().moveAgents(layout);
            }
          };
          if (logger.isInfoEnabled()) {
            logger.info("autoLoadBalance");
          }
          getLoadBalancer().doLayout(LoadBalancer.DEFAULT_ANNEAL_TIME,
                                     true,
                                     new ArrayList(vacantNodes),
                                     new ArrayList(deadNodes),
                                     excludedNodes,
                                     lbl);
        }
      }
    }
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
     long samples = 0;
     long high = 0;
     StatsEntry se = (StatsEntry)runStats.get(activeNodes[i]);
     if (se != null) {
       samples =  se.samples;
       high = (long)se.high;
     }
     int agentsOnNode = model.entitiesAtLocation(activeNodes[i], model.AGENT).length;
     summary.append(activeNodes[i] + "(" + agentsOnNode + "," +
                    samples + "," + high + ")");
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
   // Find agents without valid state
   String agentsInUnknownState[] = model.listEntries(CommunityStatusModel.AGENT, -1);
   if (agentsInUnknownState.length > 0) {
     summary.append(" UNKNOWN=" + agentsInUnknownState.length + arrayToString(agentsInUnknownState));
   }
   return summary.toString();
 }

  private class StatsEntry {
    String source;
    int samples;
    long last;  // Timestamp of last status update
    long high;  // Longest elapsed time
    StatsEntry(String source, long last) {
      this.source = source;
      this.last = last;
    }
    public String toString() {
      return "source=" + source + " samples=" + samples + " high=" + high;
    }
  }

}
