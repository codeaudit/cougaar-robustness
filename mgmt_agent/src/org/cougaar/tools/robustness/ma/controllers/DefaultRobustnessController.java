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
import org.cougaar.tools.robustness.ma.ldm.HealthMonitorRequest;
import org.cougaar.tools.robustness.ma.util.DeconflictListener;
import org.cougaar.tools.robustness.ma.util.HeartbeatListener;
import org.cougaar.tools.robustness.ma.util.LoadBalancer;
import org.cougaar.tools.robustness.ma.util.LoadBalancerListener;
import org.cougaar.tools.robustness.ma.util.MoveHelper;
import org.cougaar.tools.robustness.ma.util.MoveListener;
import org.cougaar.tools.robustness.ma.util.PingHelper;
import org.cougaar.tools.robustness.ma.util.PingResult;
import org.cougaar.tools.robustness.ma.util.PingListener;
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

import java.util.*;

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
      logger.debug("Heartbeats started: agent=" + name);
      newState(name, DefaultRobustnessController.ACTIVE);
    }
    public void heartbeatStopped(String name) {
      logger.debug("Heartbeats stopped: agent=" + name);
    }
    public void heartbeatFailure(String name) {
      logger.info("Heartbeat timeout: agent=" + name +
                  " state=" + stateName(getState(name)));
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
      if (thisAgent.equals(preferredLeader())) {
        //for deconflict: the applicability condition of one active agent should
        //be true and the defense op mode should be disabled.
        if (isDeconflictionEnabled()) {
          if (getDeconflictHelper().isDefenseApplicable(name)) {
            getDeconflictHelper().changeApplicabilityCondition(name);
          }
          getDeconflictHelper().opmodeDisabled(name);
        }
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
      logger.info("Expired Status:" + " agent=" + name + " state=ACTIVE");
      if ((isLocal(name) || isNode(name)) ||
          (thisAgent.equals(preferredLeader()) && getState(getLocation(name)) == DEAD) ||
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
                                  implements DeconflictListener {
    { addDeconflictListener(this); }
    public void enter(String name) {
      if (isDeconflictionEnabled()) {
        if (!getDeconflictHelper().isDefenseApplicable(name)) {
          getDeconflictHelper().changeApplicabilityCondition(name);
        }
        if (getDeconflictHelper().isOpEnabled(name)) {
          newState(name, RESTART);
        }
      } else {
        newState(name, RESTART);
      }
    }

    public void defenseOpModeEnabled(String name) {
      if (isDeconflictionEnabled() &&
          !getDeconflictHelper().isDefenseApplicable(name)) {
        getDeconflictHelper().changeApplicabilityCondition(name);
      }
      // Verify that agent state hasn't changed while defense was disabled
      if (getState(name) != DefaultRobustnessController.ACTIVE) {
        newState(name, RESTART);
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
    public void enter(final String name) {
      setExpiration(name, NEVER);
      communityReady = false; // For ACME Community Ready Events
      if (isAgent(name)) {
        if (thisAgent.equals(preferredLeader()) ||
          name.equals(preferredLeader()) && isLeader(thisAgent)) {
          newState(name, DECONFLICT);
        }
      } else if (isNode(name)) {
        deadNodes.add(name);
        if (!useGlobalSolver()) {
          newState(agentsOnNode(name), DEAD);
          if (thisAgent.equals(preferredLeader())) { removeFromCommunity(name); }
        } else {
          if (isLeader(thisAgent)) {
            getLayout(new LoadBalancerListener() {
              public void layoutReady(Map layout) {
                if (getState(name) == DEAD) {
                  logger.info("layout from EN4J: " + layout);
                  RestartDestinationLocator.setPreferredRestartLocations(layout);
                  newState(agentsOnNode(name), DEAD);
                  if (thisAgent.equals(preferredLeader())) { removeFromCommunity(name); }
                } else { // Abort, node no longer classified as DEAD
                  deadNodes.remove(name);
                  if (logger.isInfoEnabled()) {
                    logger.info("Restart aborted: node=" + name +
                                " state=" + stateName(getState(name)));
                  }
                }
              }
            });
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
          logger.debug("Next Status:" + " agent=" + name + " state=INITIAL");
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
        logger.info("Received FORCED_RESTART request:" + hmr);
        String agentsToRestart[] = hmr.getAgents();
        for (int i = 0; i < agentsToRestart.length; i++) {
          killAgent(agentsToRestart[i]);
          restartAgent(agentsToRestart[i]);
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

  /**
   * Initializes services and loads state controller classes.
   */
  public void initialize(final MessageAddress agentId,
                         final BindingSite bs,
                         final CommunityStatusModel csm) {
    super.initialize(agentId, bs, csm);
    thisAgent = agentId.toString();
    String propValue = System.getProperty(COLLECT_NODE_STATS_PROPERTY);
    collectNodeStats = propValue != null && propValue.equalsIgnoreCase("true");
    addController(INITIAL,        "INITIAL", new InitialStateController());
    //addController(LOCATED,        "LOCATED", new LocatedStateController());
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

  protected void collectNodeStats(String source) {
    if (collectNodeStats) {
      if (nodeLatencyStats == null) { initializeNodeStats(); }
      if (thisAgent.equals(preferredLeader()) &&
          !source.equals(getLocation(thisAgent))) {
        StatsEntry se = (StatsEntry) runStats.get(source);
        if (se.samples >= MIN_SAMPLES && se.high >= MIN_SAMPLE_VALUE) {
          StatCalc newSc = (StatCalc) nodeLatencyStats.get(source);
            if (se.samples == MIN_SAMPLES ||                    // Take snapshot at MIN_SAMPLES
               newSc == null ||
               se.high > newSc.getHigh()) {                     // Record highs thereafter
              if (originalStats.contains(source)) {
                newSc = (StatCalc) originalStats.get(source).clone();
                if (se.high > newSc.getMean() / 2) { //   sanity check
                  newSc.enter(se.high);
                  nodeLatencyStats.put(newSc);
                  saveNodeStats();
                }
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

  protected void saveNodeStats() {
    NodeLatencyStatistics all = new NodeLatencyStatistics(nodeLatencyStats.values());
    for (Iterator it = originalStats.list().iterator(); it.hasNext();) {
      String id = (String)it.next();
      if (!all.contains(id)) all.put(originalStats.get(id));
    }
    all.save();
  }

  // load persisted node stats from file and make a copy
  protected void initializeNodeStats() {
    originalStats = new NodeLatencyStatistics();
    nodeLatencyStats = new NodeLatencyStatistics();
    originalStats.load();
    logger.detail("initializeNodeStats: " + nodeLatencyStats.toXML());
  }

  /**
   * Calculate expiration time for node status.  This value determines how long
   * the manager waits for a status update before reporting the problem and
   * initiating a restart.  The expiration time is based on statistical data
   * obtained from the community attributes.
   * @param nodeName String  Name of node
   * @return long Expiration in milliseconds
   */
  protected long getNodeStatusExpiration(String nodeName) {

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
    logger.info("getNodeStateExpiration: node=" + nodeName + " expiration=" + expiration);
    return expiration;
  }

  private boolean canRestartAgent(String name) {
    return thisAgent.equals(preferredLeader()) ||
        (isLeader(thisAgent) && name.equals(preferredLeader()) && (getActiveHosts().size() > 1));
  }

  protected void restartAgent(String name) {
    String dest =
        RestartDestinationLocator.getRestartLocation(name, getExcludedNodes());
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
    if ((wakeAlarm != null) &&
        ((wakeAlarm.hasExpired()))) {
      if (thisAgent.equals(preferredLeader())) {
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
      /*if (!didRestart) {
        didRestart = true;
        if (doInitialPing()) {
          pingAll();
        }
      }*/
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
      logger.info("New node detected: name=" + name);
    }
    if (didRestart && getState(name) != DefaultRobustnessController.ACTIVE) {
      doPing(name, DefaultRobustnessController.ACTIVE, DEAD);
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
    return getLongAttribute(EXPECTED_AGENTS_ATTRIBUTE, -1);  }

  public String preferredLeader() {
    return model.getStringAttribute(model.MANAGER_ATTR);
  }

  boolean loadBalanceInProcess = false;

  protected long now() {
    return System.currentTimeMillis();
  }

  /**
   * Get layout from EN4J load balancer.  Check node status prior to submitting
   * request to EN4J.
   * @param lbl Listener method invoked after layout has been returned by EN4J
   */
  protected void getLayout(final LoadBalancerListener lbl) {
    logger.info("getLayout:");
    // Ping "ACTIVE" nodes to verify they're still alive before invoking EN4J
    String allNodes[] = model.listEntries(model.NODE);
    Set excludedNodes = getExcludedNodes();
    Set nodesToPing = new HashSet();
    for (int i = 0; i < allNodes.length; i++) {
      if (!deadNodes.contains(allNodes[i]) &&
          !excludedNodes.contains(allNodes[i]))
        nodesToPing.add(allNodes[i]);
    }
    final long pingTimeout = getLongAttribute(PING_TIMEOUT_ATTRIBUTE, DEFAULT_PING_TIMEOUT);
    PingListener pl = new PingListener() {
      public void pingComplete(PingResult[] pingResults) {
        // Update list of DEAD nodes
        String deadNodesFromModel[] = model.listEntries(model.NODE, DEAD);
        for (int i = 0; i < deadNodesFromModel.length; i++) {
          deadNodes.add(deadNodesFromModel[i]);
        }
        for (int i = 0; i < pingResults.length; i++) {
          if (pingResults[i].getStatus() == PingResult.FAIL) {
            deadNodes.add(pingResults[i].getName());
            logger.info("Found new DEAD node during LoadBalance node check: " +
                        pingResults[i].getName());
            model.setCurrentState(pingResults[i].getName(), DEAD);
          }
        }
        List excludedNodes = new ArrayList(getExcludedNodes());
        List vacantNodes = getVacantNodes();
        // Update list of NEW nodes
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
        if (!loadBalanceInProcess) {
          // Guard against overlapping doLayout requests
          loadBalanceInProcess = true;
          LoadBalancerListener newLbl = new LoadBalancerListener() {
            public void layoutReady(Map layout) {
              lbl.layoutReady(layout);
              loadBalanceInProcess = false;
            }
          };
          // Use ping timeout to calculate annealTime:
          //  - convert ms to secs
          //  - divide by 2 to provide some margin
          //  - divide by 9 for EN multi-pass
          long annealTime = pingTimeout > 0 ? pingTimeout / 1000 / 2 / 9 :
              LoadBalancer.DEFAULT_ANNEAL_TIME;
          long minAnnealTime = getLongAttribute("MINIMUM_ANNEAL_TIME", MINIMUM_ANNEAL_TIME);
          if (annealTime < minAnnealTime) annealTime = minAnnealTime;
          logger.debug("doLayout:" +
                      " annealTime=" + annealTime +
                      " newNodes=" + vacantNodes +
                      " deadNodes=" + deadNodes +
                      " excludedNodes=" + excludedNodes);
          getLoadBalancer().doLayout( (int) annealTime,
                                     true,
                                     vacantNodes,
                                     new ArrayList(deadNodes),
                                     excludedNodes,
                                     newLbl);

        }
      }
    };
    getPingHelper().ping((String[])nodesToPing.toArray(new String[0]), pingTimeout, pl);
  }

  /**
   * Ping an agent and update current state based on result.
   * @param agent           Agent to ping
   * @param stateOnSuccess  New state if ping succeeds
   * @param stateOnFail     New state if ping fails
   */
  protected void doPing(String agent,
                        final int stateOnSuccess,
                        final int stateOnFail) {
    PingHelper pinger = getPingHelper();
    if (pinger.pingInProcess(agent)) {
      logger.detail("Duplicate ping requested, new ping not performed: agent=" + agent);
    } else {
      doPing(new String[]{agent}, stateOnSuccess, stateOnFail);
    }
  }

  protected void checkLoadBalance() {
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
              logger.debug("layout from EN4J: " + layout);
              getLoadBalancer().moveAgents(layout);
            }
          };
          logger.info("autoLoadBalance");
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
  }

}
