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

import java.util.*;
import javax.naming.*;
import javax.naming.directory.*;

import org.cougaar.tools.robustness.sensors.SensorFactory;
import org.cougaar.tools.robustness.sensors.HeartbeatRequest;
import org.cougaar.tools.robustness.sensors.HeartbeatEntry;
import org.cougaar.tools.robustness.sensors.HeartbeatHealthReport;
import org.cougaar.tools.robustness.sensors.PingRequest;

//import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.blackboard.IncrementalSubscription;
//import org.cougaar.core.plugin.SimplePlugin;
import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.SimpleMessageAddress;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.component.ServiceRevokedEvent;

//import org.cougaar.core.service.TopologyEntry;
//import org.cougaar.core.service.TopologyReaderService;

import org.cougaar.core.service.AlarmService;
import org.cougaar.core.agent.service.alarm.Alarm;

import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;

import org.cougaar.core.service.community.*;
import org.cougaar.community.*;

import org.cougaar.core.service.EventService;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.util.UID;

import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Application;

/**
 * This plugin monitors a set of agents and publishes HealthStatus
 * messages to the blackboard when it detects an anomaly that suggests a
 * monitored agent may be dead or experiencing an abnormal stress.
 * The set of agents to be monitored is determined by community membership.
 * This plugin may be externally configured with the following parameters
 * that may either be specified as plugin arguments or in a
 * ManagementAgentProperty object that is published to local the Blackboard.
 * <PRE>
 * Parameter Name                Use
 * --------------   ---------------------------------------------------
 * community        String that defines the name of the community to be
 *                  monitored.  The names of individual agents within this
 *                  community are obtained from the Name Server via the
 *                  CommunityPlugin.
 * hbReqTimeout     Defines the timeout period (in milliseconds) for a
 *                  Heartbeat Request,
 * hbReqRetries     Defines the number of times to retry a HeartbeatRequest
 *                  when a failure is encountered
 * hbReqRetryFreq   Defines the interval between HeartbeatRequest retries
 * hbFreq           Defines the frequency (in milliseconds) at which the
 *                  monitored agents are to send heartbeats to the management
 *                  agent.
 * hbTimeout        Defines the timeout period (in milliseconds) for
 *                  heartbeats.
 * hbPctLate        Defines a tolerance for late heartbeats before they are
 *                  reported in a HeartbeatHealthReport from sensor plugins.
 *                  The value is defined as a Float number representing a
 *                  percentage of the hbTimeout.
 * hbWindow         Defines the interval (in milliseconds) over which a
 *                  Heartbeat failure rate is calculated.  The heartbeat failure
 *                  rate is the percentage of late hearbeats vs expected
 *                  heartbeats.
 * hbFailRate       Defines the heartbeat failure rate threshold.  When the
 *                  calculated heartbeat failure rate exceeds this threshold
 *                  the monitored agent is placed in a "HEALTH-CHECK" state
 *                  for further evaluation by the Management Agent.  This value
 *                  is defined as a Float number that represents the maximum
 *                  acceptable heartbeat failure rate.
 * activePingFreq   Defines the frequency at which agents are pinged.  This
 *                  is performed in addition to Heartbeat monitoring.  If
 *                  active pings are not required set this parameter to 0.
 * pingTimeout      Defines the ping timeout period (in milliseconds).
 * pingRetries      Defines the number of times to retry a ping
 *                  when a failure is encountered
 * evalFreq         Defines how often (in milliseconds) the HealthStatus of
 *                  monitored agents is evaluated.
 * restartTimeout   Defines how long to wait (in milliseconds) for a restart to
 *                  complete before determining that the restart has failed. If
 *                  a negative number is specified no timeouts will occur.  This
 *                  would in effect turn off restart retries.
 * restartRetryFreq Defines how often (in milliseconds) to retry a failed
 *                  restart after it has failed.
 * </PRE>
 */

public class HealthMonitorPlugin extends SimplePlugin implements
  CommunityChangeListener {

  // Defines default values for configurable parameters.
  private static String defaultParams[][] = {
    {"hbReqTimeout",     "120000"},
    {"hbReqRetries",          "0"},
    {"hbReqRetryFreq",   "120000"},
    {"hbFreq",            "60000"},
    {"hbTimeout",        "120000"},
    {"hbPctLate",          "50.0"},
    {"hbWindow",         "600000"},
    {"hbFailRate",         "50.0"},
    {"activePingFreq",       "-1"},
    {"pingTimeout",      "600000"},
    {"pingRetries",           "0"},
    {"evalFreq",          "10000"},
    {"restartTimeout",   "-1"},
    {"restartRetryFreq", "600000"},
    {"numAgents",            "-1"}
  };
  ManagementAgentProperties healthMonitorProps =
    ManagementAgentProperties.makeProps(this.getClass().getName(), defaultParams);


  /////////////////////////////////////////////////////////////////////////
  //  Externally configurable parameters
  /////////////////////////////////////////////////////////////////////////
  private long  heartbeatRequestTimeout;
  private int   heartbeatRequestRetries;
  private long  heartbeatFrequency;
  private long  heartbeatRequestRetryFrequency;
  private long  heartbeatTimeout;
  private float heartbeatPctLateThreshold;

  // Defines the interval over which a Heartbeat failure rate is calculated
  // A "Heartbeat failure" is a Heartbeat that exceeded the limit defined
  // by the heartbeatTimeout and heartbeatPctLateThreshold parameters but
  // the agent was subsequently determined to be alive via a ping.
  // The Heartbeat Failure Rate defines the number of late heartbeats
  // as a percentage of total heartbeats expected during the timeframe defined
  // by the heartbeatFailureRateWindow.
  private long  heartbeatFailureRateWindow;

  // Defines the threshold for late heartbeats.  If this threshold is exceeded
  // the agent is put into the "HEALTH_CHECK" state for analysis by the
  // Decision plugin.
  private float heartbeatFailureRateThreshold;

  private long pingTimeout;
  private int  pingRetries;

  // Determines how often our internal evaluation Thread is run
  private long evaluationFrequency = 10000;  // Defaults to 10 seconds

  // Determines how long to wait for a restart to complete
  private long restartTimeout;

  // Determines how often to retry a failed restart
  private long restartRetryFrequency;

  // Determines how often to actively ping agents
  private long activePingFrequency;

  // Name of community to monitor
  private String communityToMonitor = null;

  private int numAgents = -1;

  /////////////////////////////////////////////////////////////////////////
  //  End of externally configurable parameters
  /////////////////////////////////////////////////////////////////////////

  private SensorFactory sensorFactory;
  //private ClusterIdentifier myAgent;
  private MessageAddress myAgent;
  private LoggingService log;
  private BlackboardService bbs;
  private WhitePagesService wps;

  private EventService eventService;

  // UIDs associated with pending pings
  private Collection pingUIDs = new Vector();

  // HealthStatus objects for agents in monitored community
  private Map membersHealthStatus = new HashMap();

  private CommunityService communityService = null;
  //private TopologyReaderService topologyService = null;

  private CommunityRoster roster = null;
  private boolean communityChanged = false;

  /**
   * This method obtains a roster for the community to be monitored and sends
   * a HeartbeatRequest message to all active members.  The roster is updated
   * as community members are added/removed.  Heartbeat requests are sent
   * to new agents.  A subscription to HealthReports is also created to receive
   * HealthReports for monitored agents.
   */
  protected void setupSubscriptions() {

    // Setup logger
    log =  (LoggingService) getBindingSite().getServiceBroker().
      getService(this, LoggingService.class, null);

    bbs = getBlackboardService();

    DomainService domainService =
      (DomainService) getBindingSite().getServiceBroker().
      getService(this, DomainService.class, null);

    eventService =
      (EventService) getBindingSite().getServiceBroker().
      getService(this, EventService.class, null);

    sensorFactory =
      ((SensorFactory) domainService.getFactory("sensors"));
    if (sensorFactory == null) {
      log.error("Unable to get 'sensors' domain");
    }

    //myAgent = getClusterIdentifier();
    myAgent = getMessageAddress();

    communityService = getCommunityService();
    //topologyService = getTopologyReaderService();
    wps = getWhitePagesService();

    // Subscribe to HeartbeatRequests to determine if HeartbeatRequest was
    // received/accepted by monitored agent
    heartbeatRequests =
      (IncrementalSubscription)bbs.subscribe(heartbeatRequestPredicate);

    // Subscribe to PingRequests to receive ping results
    pingRequests =
      (IncrementalSubscription)bbs.subscribe(pingRequestPredicate);

    // Subscribe to HeartbeatHealthReports to receive notification of failed
    // heartbeats
    heartbeatHealthReports =
      (IncrementalSubscription)bbs.subscribe(heartbeatPredicate);

    // Subscribe to ManagementAgentProperties to get changes to configurable
    // parameters.
    mgmtAgentProps =
      (IncrementalSubscription)bbs.subscribe(propertiesPredicate);

    communityService.addListener(this);

    // Start evaluation thread to periodically update and analyze the Health
    // Status of monitored agents
    startEvaluationThread(evaluationFrequency);

  }

  private int checkCtr = 0;
  private boolean getCommunityToMonitor() {
    // Find name of community to monitor
    Collection communities =
        communityService.search("(CommunityManager=" + myAgent.toString() + ")");
    if (!communities.isEmpty()) {
      communityToMonitor = (String) communities.iterator().next();
      // Initialize configurable paramaeters from defaults and plugin arguments.
      try {
        bbs.openTransaction();
        getPropertiesFromCommunityAttributes();
        // Set configurable parameters
        updateParams(healthMonitorProps);
        bbs.publishAdd(healthMonitorProps);

        // Print informational message defining current parameters
        StringBuffer startMsg = new StringBuffer();
        startMsg.append("agent=" + myAgent);
        startMsg.append(" community=" + communityToMonitor);
        startMsg.append(" " + paramsToString());
        log.info(startMsg.toString());
      }
      catch (Exception ex) {
        log.error(ex.getMessage(), ex);
      }
      finally {
        bbs.closeTransaction();
        return true;
      }
    }
    else {
      if (++checkCtr == 20) {
        log.error("Agent \"" + myAgent.toString() + "\" " +
            "is not identified as a \"CommunityManager\" in any active community!\n" +
            "Check that the community to monitor has this agent identified\n" +
            "in its \"CommunityManager\" attribute and that this agent is\n" +
            "also identified as an Entity within the monitored community with\n" +
            "the attribute \"Role=ManagementAgent\".");
      }
      return false;
    }
  }

  /**
   * Creates a printable representation of current parameters.
   * @return  Text string of current parameters
   */
  private String paramsToString() {
    StringBuffer sb = new StringBuffer();
    for (Enumeration enum = healthMonitorProps.propertyNames(); enum.hasMoreElements();) {
      String propName = (String)enum.nextElement();
      sb.append(propName + "=" +
        healthMonitorProps.getProperty(propName) + " ");
    }
    return sb.toString();
  }

  /**
   * Receives CommunityRoster from CommunityPlugin and HealthReports for
   * monitored agents.
   */
  public void execute() {

    // Get Parameter changes
    for (Iterator it = mgmtAgentProps.getChangedCollection().iterator();
         it.hasNext();) {
      ManagementAgentProperties props = (ManagementAgentProperties)it.next();
      updateParams(props);
      log.info("Parameters modified: " + paramsToString());
    }

    // Get HeartbeatRequests
    for (Iterator it = heartbeatRequests.getChangedCollection().iterator();
         it.hasNext();) {
      HeartbeatRequest req = (HeartbeatRequest)it.next();
      int status = req.getStatus();
      Collection targets = req.getTargets();
      for (Iterator targetIt = targets.iterator(); targetIt.hasNext();) {
        MessageAddress target = (MessageAddress)targetIt.next();
        HealthStatus hs = getHealthStatus(target);
        log.debug("HeartbeatRequest: agent=" + hs.getAgentId() +
          " status=" + HeartbeatRequest.statusToString(status));
        if (hs != null) hs.setHeartbeatRequestStatus(status);
        if (log.isDebugEnabled() &&
            (req.getStatus() == HeartbeatRequest.ACCEPTED ||
             req.getStatus() == HeartbeatRequest.FAILED)) {
          log.debug("HeartbeatRequest: agent=" + hs.getAgentId() +
          " status=" + HeartbeatRequest.statusToString(status));
        }
      }
    }

    // Get PingRequests
    for (Iterator it = pingRequests.getChangedCollection().iterator();
         it.hasNext();) {
      PingRequest req = (PingRequest)it.next();
      if (req.getSource().equals(myAgent)) {
        int status = req.getStatus();
        HealthStatus hs = getHealthStatus(req.getTarget());
        hs.setPingStatus(status);
        if (log.isDebugEnabled()) {
          log.debug("PingRequest changed: agent=" + hs.getAgentId() +
            " status=" + status);
        }
      }
    }

    // Get HeartbeatHealthReports
    for (Iterator it = heartbeatHealthReports.getAddedCollection().iterator();
         it.hasNext();) {
      HeartbeatHealthReport hbhr = (HeartbeatHealthReport)it.next();
      HeartbeatEntry hbe[] = hbhr.getHeartbeats();
      for (int i = 0; i < hbe.length; i++) {
        HealthStatus hs = getHealthStatus(hbe[i].getSource());
        hs.setHeartbeatStatus(HealthStatus.HB_TIMEOUT);
        if (hs.getState().equals(HealthStatus.NORMAL)) {
          hs.addHeartbeatTimeout(now());
          hs.setHeartbeatEntry(hbe[i]);
          log.debug("HeartbeatTimeout: agent=" + hs.getAgentId() +
            ", heartbeatEntry=" + hbe[i].toString());
        }
      }
      bbs.publishRemove(hbhr);
    }

  }


  /**
   * Periodically evaluates the HealthStatus of monitored agents.
   */
  private void evaluateHealthStatus() {
    /* This method implements a state machine that evaluates the state of each
       monitored agent during each evaluation cycle.  Specific actions are
       performed for each state and when appropriate the state is transitioned
       to reflect the agents new condition.
    */
    //System.out.print("*");
    // Process any Roster changes
    if (communityToMonitor == null) {
      communityChanged = getCommunityToMonitor();
    } else {
      if (communityChanged) {
        processRosterChanges();
        communityChanged = false;
      }
      Collection currentAgents = findMonitoredAgents();
      for (Iterator it = currentAgents.iterator(); it.hasNext(); ) {
        HealthStatus hs = getHealthStatus( (MessageAddress) it.next());
        String state = hs.getState();

        //************************************************************************
         // State: INITIAL  - Agents in this state have recently been started
         //                   restarted, or moved.  The primary task in this state
         //                   is to start generation of Heartbeats.  Once
         //                   Heartbeats are activated the agents state is
         //                   transition to NORMAL.
         //************************************************************************
        if (state.equals(HealthStatus.INITIAL)) {
          if (!hs.getPriorState().equals(HealthStatus.INITIAL) ||
              hs.getHeartbeatRequestStatus() == HealthStatus.UNDEFINED) {
            log.debug("Sending HeartbeatRequest to agent '" + hs.getAgentId() +
                      "'");
            sendHeartbeatRequest(hs);
          }
          else {
            switch (hs.getHeartbeatRequestStatus()) {
              case HeartbeatRequest.NEW:
              case HeartbeatRequest.SENT:
                if (elapsedTime(hs.getHeartbeatRequestTime(), now()) >
                    (hs.getHbReqTimeout() * 2)) {
                  log.warn("HeartbeatRequest timeout: agent=" + hs.getAgentId());
                  hs.setHeartbeatRequestStatus(HeartbeatRequest.FAILED);
                }
                break;
              case HeartbeatRequest.ACCEPTED:
                log.debug("HeartbeatRequest ACCEPTED: agent=" + hs.getAgentId());
                if (hs.getStatus() == HealthStatus.RESTARTED) {
                  event("Restart succeeded:" +
                        " agent=" + hs.getAgentId() +
                        " node=" + hs.getNode());
                }
                else if (hs.getStatus() == HealthStatus.MOVED) {
                  event("Move succeeded:" +
                        " agent=" + hs.getAgentId() +
                        " node=" + hs.getNode());
                }
                hs.setState(HealthStatus.NORMAL);
                hs.setStatus(HealthStatus.OK);
                hs.setHeartbeatStatus(HealthStatus.HB_NORMAL);
                hs.setHbReqRetryCtr(0);
                break;
              case HeartbeatRequest.REFUSED:
                log.warn("HeartbeatRequest REFUSED: agent=" + hs.getAgentId());
                break;
              case HeartbeatRequest.FAILED:
                int retries = hs.getHbReqRetryCtr();
                if (hs.getHbReqRetries() == -1 || retries < hs.getHbReqRetries()) {
                  if (retries == 0) {
                    log.warn("HeartbeatRequest FAILED (retrying): agent=" +
                             hs.getAgentId());
                  }
                  hs.setHbReqRetryCtr(++retries);
                  // Setting status to undefined forces a retry
                  hs.setHeartbeatRequestStatus(HealthStatus.UNDEFINED);
                }
                else {
                  if (hs.getPingStatus() == HealthStatus.UNDEFINED) {
                    hs.setPingStatus(HealthStatus.PING_REQUIRED);
                  }
                  int pingStatus = hs.getPingStatus();
                  if (pingStatus == HealthStatus.PING_REQUIRED) {
                    doPing(hs);
                  }
                  else if (pingStatus == PingRequest.NEW ||
                           pingStatus == PingRequest.SENT) {
                    if (elapsedTime(hs.getPingTimestamp(), now()) >
                        (hs.getPingTimeout() * 2)) {
                      log.warn("PingRequest timeout: agent=" + hs.getAgentId());
                      hs.setPingStatus(PingRequest.FAILED);
                    }
                  }
                  else if (pingStatus == PingRequest.RECEIVED) {
                    log.info("HeartbeatRequest timeout, ping successful:" +
                             " agent=" + hs.getAgentId());
                    if (hs.getStatus() == HealthStatus.RESTARTED) {
                      event("Restart succeeded:" +
                            " agent=" + hs.getAgentId() +
                            " node=" + hs.getNode());
                    }
                    else if (hs.getStatus() == HealthStatus.MOVED) {
                      event("Move succeeded:" +
                            " agent=" + hs.getAgentId() +
                            " node=" + hs.getNode());
                    }
                    hs.setState(HealthStatus.NORMAL);
                    hs.setStatus(HealthStatus.OK);
                    hs.setHeartbeatStatus(HealthStatus.HB_INACTIVE);
                    hs.setHbReqRetryCtr(0);
                  }
                  else if (pingStatus == PingRequest.FAILED) {
                    log.warn("HeartbeatRequest timeout, ping failed:" +
                             " agent=" + hs.getAgentId());
                    hs.setPingRetryCtr(0);
                    hs.setPingStatus(HealthStatus.UNDEFINED);
                    hs.setHeartbeatStatus(HealthStatus.NO_RESPONSE);
                    doHealthCheck(hs, HealthStatus.NO_RESPONSE);
                  }
                }
                break;
              default:
            }
          }
          //************************************************************************
           // State: NORMAL   - Agents in this state have previously been confirmed
           //                   to be alive and are sending periodic Heartbeats.  When
           //                   late Heartbeats are detected the agent is pinged to
           //                   determine if it is still alive.  If the ping fails
           //                   or if the frequency of late heartbeats exceeds a
           //                   threshold the agents state is transitioned to
           //                   HEALTH_CHECK.
           //************************************************************************
        }
        else if (state.equals(HealthStatus.NORMAL)) {
          if (hs.getHeartbeatStatus() == HealthStatus.HB_TIMEOUT) {
            if (hs.getPingStatus() == HealthStatus.UNDEFINED) {
              hs.setPingStatus(HealthStatus.PING_REQUIRED);
            }
            int pingStatus = hs.getPingStatus();
            if (pingStatus == HealthStatus.PING_REQUIRED) {
              // Late heartbeat, ping agent to see if agent is alive
              doPing(hs);
            }
            else if (pingStatus == PingRequest.NEW ||
                     pingStatus == PingRequest.SENT) {
              if (elapsedTime(hs.getPingTimestamp(), now()) >
                  (hs.getPingTimeout() * 2)) {
                log.warn("PingRequest timeout: agent=" + hs.getAgentId());
                hs.setPingStatus(PingRequest.FAILED);
              }
            }
            else if (pingStatus == PingRequest.RECEIVED) {
              // See if agent has moved

              String location = getLocation(hs.getAgentId().toString());
              /*
              if (!location.equals(hs.getNode())) {
                log.info("Agent move detected: agent=" + hs.getAgentId() +
                         " priorNode=" + hs.getNode() +
                         " newNode=" + location);
                hs.setNode(location);
                hs.setState(HealthStatus.INITIAL);
                hs.setStatus(HealthStatus.MOVED);
                // Agent hasn't moved, log timeout and see if threshold was
                // exceeded
              }
              else {
              */
                log.debug("Heartbeat timeout, ping successful:" +
                          " agent=" + hs.getAgentId() +
                          " hBPctLate=" + hs.getHeartbeatEntry().getPercentLate());
                hs.setHeartbeatStatus(HealthStatus.HB_NORMAL);
                /*
                               if (!hs.hbFailureRateInSpec()) {
                  log.warn("Exceeded Heartbeat timeout threshold: agent="
                    + hs.getAgentId());
                  doHealthCheck(hs, HealthStatus.DEGRADED);
                               } else {
                  hs.setHeartbeatStatus(HealthStatus.HB_NORMAL);
                               }
                 */
              //}
              hs.setPingStatus(HealthStatus.UNDEFINED);
              hs.setPingRetryCtr(0);
            }
            else if (pingStatus == PingRequest.FAILED) {
              int retries = hs.getPingRetryCtr();
              if (retries < hs.getPingRetries()) {
                log.warn("Heartbeat timeout, ping failed (retrying):" +
                         " agent=" + hs.getAgentId() +
                         " hBPctLate=" + hs.getHeartbeatEntry().getPercentLate());
                hs.setPingRetryCtr(++retries);
                hs.setPingStatus(HealthStatus.PING_REQUIRED);
              }
              else {
                log.error("Heartbeat timeout, ping failed:" +
                          " agent=" + hs.getAgentId() +
                          " hBPctLate=" + hs.getHeartbeatEntry().getPercentLate());
                hs.setPingRetryCtr(0);
                hs.setPingStatus(HealthStatus.UNDEFINED);
                doHealthCheck(hs, HealthStatus.NO_RESPONSE);
              }
            }

          }
          else if ( (hs.getHeartbeatStatus() == HealthStatus.HB_NORMAL ||
                     hs.getHeartbeatStatus() == HealthStatus.HB_INACTIVE) &&
                   hs.getActivePingFrequency() > 0) {
            // Ping agents periodically even if we haven't received a
            // HealthReport
            if (activePingFrequency != -1 &&
                (hs.getPingTimestamp() == null ||
                 elapsedTime(hs.getPingTimestamp(), now()) >
                 hs.getActivePingFrequency())) {
              hs.setPingStatus(HealthStatus.PING_REQUIRED);
            }
            int pingStatus = hs.getPingStatus();
            if (pingStatus == HealthStatus.PING_REQUIRED) {
              log.debug("Active Ping: agent=" + hs.getAgentId());
              doPing(hs);
            }
            else if (pingStatus == PingRequest.NEW ||
                     pingStatus == PingRequest.SENT) {
              if (elapsedTime(hs.getPingTimestamp(), now()) >
                  (hs.getPingTimeout() * 2)) {
                log.warn("Active Ping Timeout: agent=" + hs.getAgentId());
                hs.setPingStatus(PingRequest.FAILED);
              }
            }
            else if (pingStatus == PingRequest.RECEIVED) {
              // As expected
              if (hs.getPingRetryCtr() > 0) {
                // If this is a successful retry print success message
                log.info("Active Ping SUCCEEDED: agent=" + hs.getAgentId());
                hs.setPingRetryCtr(0);
              }
              hs.setPingStatus(HealthStatus.UNDEFINED);
            }
            else if (pingStatus == PingRequest.FAILED) {
              int retries = hs.getPingRetryCtr();
              if (retries < hs.getPingRetries()) {
                log.warn("Active Ping FAILED (retrying): agent=" +
                         hs.getAgentId());
                hs.setPingRetryCtr(++retries);
                hs.setPingStatus(HealthStatus.PING_REQUIRED);
              }
              else {
                log.error("Active Ping FAILED: agent=" + hs.getAgentId());
                hs.setPingRetryCtr(0);
                hs.setPingStatus(HealthStatus.UNDEFINED);
                doHealthCheck(hs, HealthStatus.NO_RESPONSE);
              }
            }

          }
          //************************************************************************
           // State: RESTART         - Agents in this state are being restarted
           //                          (most likely by our DecisionPlugin).
           //                          The condition is simply logged by this plugin.
           //                          The DecisionPlugin will transition the state
           //                          to either INITIAL or FAILED_RESTART depending
           //                          on the result of the restart action.
           //************************************************************************
        }
        else if (state.equals(HealthStatus.RESTART)) {
          if (!state.equals(hs.getPriorState()))
            log.debug("Agent restart in process: agent=" + hs.getAgentId());
          if (restartTimeout >= 0 &&
              elapsedTime(hs.getLastRestartAttempt(), now()) > restartTimeout) {
            log.warn("Agent restart timed out: agent=" + hs.getAgentId());
            hs.setState(HealthStatus.FAILED_RESTART);
          }
          //************************************************************************
           // State: RESTART_COMPLETE - Agents in this state have just completed
           //                          a restart.
           //************************************************************************
        }
        else if (state.equals(HealthStatus.RESTART_COMPLETE)) {
          log.debug("Restart complete: agent=" + hs.getAgentId());
          hs.setState(HealthStatus.INITIAL);
          hs.setStatus(HealthStatus.RESTARTED);
          //hs.setNode(getLocation(hs.getAgentId().toString()));
          //hs.setHeartbeatRequestStatus(HealthStatus.UNDEFINED);
          //************************************************************************
           // State: FAILED_RESTART  - Agents in this state were previously
           //                          determined to be dead and have not been
           //                          successfully restarted.  The agent will
           //                          periodically be put back into the HEALTH_CHECK
           //                          state to initiate a retry of the restart.
           //************************************************************************
        }
        else if (state.equals(HealthStatus.FAILED_RESTART)) {
          if (!state.equals(hs.getPriorState())) {
            log.error("Agent restart failed: agent=" + hs.getAgentId());
            event("Restart failed: agent=" + hs.getAgentId());
          }
          // While in this state periodically ping agent to see if it responds.
          //   If a response is received put agent back into NORMAL state.  Otherwise,
          //   retry restarts at specified interval.
          int pingStatus = hs.getPingStatus();
          switch (pingStatus) {
            case HealthStatus.UNDEFINED:
              if (hs.getLastRestartAttempt() == null ||
                  elapsedTime(hs.getLastRestartAttempt(), now()) >
                  restartRetryFrequency) {
                log.info("Retrying restart: agent=" + hs.getAgentId());
                hs.setLastRestartAttempt(now());
                doHealthCheck(hs, HealthStatus.NO_RESPONSE);
              }
              else if (hs.getPingTimestamp() == null ||
                       elapsedTime(hs.getPingTimestamp(), now()) >
                       hs.getHbFrequency()) {
                doPing(hs);
              }
              break;
            case PingRequest.RECEIVED:
              hs.setState(HealthStatus.INITIAL);
              hs.setPingStatus(HealthStatus.UNDEFINED);
              hs.setHeartbeatStatus(HealthStatus.HB_NORMAL);
              hs.setPingRetryCtr(0);
              break;
            case PingRequest.FAILED:
              hs.setPingRetryCtr(0);
              hs.setPingStatus(HealthStatus.UNDEFINED);
              break;
            default:
          }

          //************************************************************************
           // State: FAILED_MOVE     - Agents in this state have recently been the
           //                          subject of a move attempt that failed.  The
           //                          move failure is reported and the agent is
           //                          placed into the NORMAL state.
           //************************************************************************
        }
        else if (state.equals(HealthStatus.FAILED_MOVE)) {
          // Move failed resume normal monitoring
          log.warn("Agent move failed: agent=" + hs.getAgentId());
          event("Move failed: agent=" + hs.getAgentId());
          hs.setState(HealthStatus.NORMAL);
          hs.setHeartbeatStatus(HealthStatus.HB_NORMAL);
          //************************************************************************
           // State: MOVE  - Agents in this state are currently in the process of
           //                moving.  The condition is simply logged by this plugin.
           //                The VacatePlugin or MovePlugin will transition the state
           //                to either INITIAL or FAILED_MOVE depending on the result
           //                of the move action.
           //************************************************************************
        }
        else if (state.equals(HealthStatus.MOVE)) {
          if (!state.equals(hs.getPriorState()))
            log.debug("Agent move in process: agent=" + hs.getAgentId());
            //************************************************************************
             // State: HEALTH_CHECK  - Agents in this state are being analyzed by the
             //                        DecisionPlugin.  The DecisionPlugin will
             //                        transition the state upon completion of its
             //                        analysis and subsequent action (such as
             //                        restart).
             //************************************************************************
        }
        else if (state.equals(HealthStatus.HEALTH_CHECK)) {
          if ( (elapsedTime(hs.getHealthCheckTime(), now()) > restartTimeout) &&
              (restartTimeout > 0)) {
            log.warn("Agent Health Check timed out: agent=" + hs.getAgentId());
            doHealthCheck(hs, hs.getStatus());
          }
          //************************************************************************
           // State: ROBUSTNESS_INIT_FAIL  - Agents in this state are alive but
           //                        are not responding to HeartbeatRequests or
           //                        Pings.
           //************************************************************************
        }
        else if (state.equals(HealthStatus.INIT_FAIL)) {
          if (!state.equals(hs.getPriorState()))
            log.error("Robustness init fail: agent=" + hs.getAgentId());
        }
        else {
          log.warn("Invalid run state: agent=" + hs.getAgentId() + ", state=" +
                   state);
        }
        // Capture this state for use next cycle
        hs.setPriorState(state);
      }
      printStats();
    }
  }

  /**
   * Evaluate roster for additions/deletions.  Update membersHealthStatus
   * collection to maintain consistency with roster.
   * @param roster
   */
  private void processRosterChanges() {
    Collection cmList = null;
    if (communityChanged) {
      roster = communityService.getRoster(communityToMonitor);
      cmList = roster.getMembers();
      communityChanged = false;
      // If first time, copy members from roster to local list
      if (membersHealthStatus.isEmpty()) {
        for (Iterator it = cmList.iterator(); it.hasNext(); ) {
          CommunityMember cm = (CommunityMember) it.next();
          if (cm.isAgent() &&
              !cm.getName().equals(myAgent.toString()) &&
              !isNodeAgent(cm.getName())) {
            HealthStatus hs = newHealthStatus(SimpleMessageAddress.
                                              getSimpleMessageAddress(cm.
                getName()));
            addHealthStatus(hs);
            //log.info("Adding " + cm.getName());
            bbs.openTransaction();
            bbs.publishAdd(hs);
            bbs.closeTransaction();
          }
        }
      }
      else {
        // Look for additions
        Collection newMembers = new Vector();
        for (Iterator it = cmList.iterator(); it.hasNext(); ) {
          CommunityMember cm = (CommunityMember) it.next();
          MessageAddress agent = SimpleMessageAddress.getSimpleMessageAddress(
              cm.getName());
          if (cm.isAgent() &&
              !isNodeAgent(cm.getName()) &&
              !cm.getName().equals(myAgent.toString()) &&
              !hasHealthStatus(agent)) {
            HealthStatus hs = newHealthStatus(agent);
            addHealthStatus(hs);
            newMembers.add(hs);
            //log.info("Adding " + cm.getName());
            bbs.openTransaction();
            bbs.publishAdd(hs);
            bbs.closeTransaction();
          }
        }
        // Look for deletions
        for (Iterator it = findMonitoredAgents().iterator(); it.hasNext(); ) {
          HealthStatus hs = getHealthStatus( (MessageAddress) it.next());
          boolean found = false;
          for (Iterator it1 = cmList.iterator(); it1.hasNext(); ) {
            CommunityMember cm = (CommunityMember) it1.next();
            if (hs.getAgentId().equals(SimpleMessageAddress.
                                       getSimpleMessageAddress(cm.getName()))) {
              found = true;
              break;
            }
          }
          if (!found) {
            log.info("Removed " + hs.getAgentId());
            it.remove();
            bbs.openTransaction();
            bbs.publishRemove(hs);
            bbs.closeTransaction();
          }
        }
      }
    }
  }

  /**
   * Creates a new HealthStatus object to include any agent-specific parameters.
   * @param agentId Message address for agent
   * @return  new HealthStatus object
   */
  private HealthStatus newHealthStatus(MessageAddress agentId) {
    // Create a HealthStatus object with default parameters
    HealthStatus hs = new HealthStatus(getUIDService().nextUID(),
                         agentId,
                         communityToMonitor,
                         getLocation(agentId.toString()),
                         getBindingSite().getServiceBroker(),
                         heartbeatRequestTimeout,
                         heartbeatRequestRetries,
                         heartbeatFrequency,
                         heartbeatTimeout,
                         heartbeatPctLateThreshold,
                         heartbeatFailureRateWindow,
                         heartbeatFailureRateThreshold,
                         pingTimeout,
                         pingRetries,
                         activePingFrequency);
    // Get agent attributes from community service and set any agent-specific
    // parameters
    Attributes attrs =
      communityService.getEntityAttributes(communityToMonitor, agentId.toString());
    NamingEnumeration enum = attrs.getAll();
    StringBuffer sb = new StringBuffer();
    try {
      while (enum.hasMoreElements()) {
        Attribute attr = (Attribute)enum.nextElement();
        if (attr.getID().equalsIgnoreCase("hbReqTimeout")) {
          hs.setHbReqTimeout(Long.parseLong((String)attr.get()));
          sb.append(" hbReqTimeout=" + hs.getHbReqTimeout());
        } else if (attr.getID().equalsIgnoreCase("hbReqRetries")) {
          hs.setHbReqRetries(Long.parseLong((String)attr.get()));
          sb.append(" hbReqRetries=" + hs.getHbReqRetries());
        } else if (attr.getID().equalsIgnoreCase("hbFreq")) {
          hs.setHbFrequency(Long.parseLong((String)attr.get()));
          sb.append(" hbFreq=" + hs.getHbFrequency());
        } else if (attr.getID().equalsIgnoreCase("hbTimeout")) {
          hs.setHbTimeout(Long.parseLong((String)attr.get()));
          sb.append(" hbTimeout=" + hs.getHbTimeout());
        } else if (attr.getID().equalsIgnoreCase("hbPctLate")) {
          hs.setHbPctLate(Float.parseFloat((String)attr.get()));
          sb.append(" hbPctLate=" + hs.getHbPctLate());
        } else if (attr.getID().equalsIgnoreCase("hbWindow")) {
          hs.setHbWindow(Long.parseLong((String)attr.get()));
          sb.append(" hbWindow=" + hs.getHbWindow());
        } else if (attr.getID().equalsIgnoreCase("hbFailRate")) {
          hs.setHbFailRateThreshold(Float.parseFloat((String)attr.get()));
          sb.append(" hbFailRate=" + hs.getFailureRate());
        } else if (attr.getID().equalsIgnoreCase("pingTimeout")) {
          hs.setPingTimeout(Long.parseLong((String)attr.get()));
          sb.append(" pingTimeout=" + hs.getPingTimeout());
        } else if (attr.getID().equalsIgnoreCase("pingRetries")) {
          hs.setPingRetries(Long.parseLong((String)attr.get()));
          sb.append(" pingRetries=" + hs.getPingRetries());
        } else if (attr.getID().equalsIgnoreCase("activePingFreq")) {
          hs.setActivePingFrequency(Long.parseLong((String)attr.get()));
          sb.append(" activePingFreq=" + hs.getActivePingFrequency());
        }
      }
    } catch (Exception ex) {
      log.error("Exception parsing agent health monitor parameters, " + ex);
    }
    if (log.isInfoEnabled()) {
      log.info("Adding " + hs.getAgentId() +
        (sb.length() > 0 ? " (" + sb.toString().trim() + ")" : ""));
    }
    return hs;
  }

  /**
   * Determines if an agent is a Node Agent.
   * @param agentName Name of agent
   * @return True if node agent
   */
  private boolean isNodeAgent(String agentName) {
    /*try {
      TopologyEntry te = topologyService.getEntryForAgent(agentName);
      return (te != null && te.getType() == te.NODE_AGENT_TYPE);
    } catch (Exception ex) {
      log.error("Exception getting agent location from TopologyReaderService", ex);
    }
    return false;*/
    try {
      AddressEntry[] entrys = wps.get("MTS");
      for(int i=0; i<entrys.length; i++) {
        String nodeName = entrys[i].getApplication().toString();
        nodeName = nodeName.substring(0, nodeName.indexOf("(MTS)"));
        if(agentName.equals(nodeName))
          return true;
      }
    }catch(Exception e){
      log.error("Exception getting agent location from WhitePagesService", e);
    }
    return false;
  }

  /**
   * Gets agents current location.
   * @param agentName Name of agent
   * @return Name of node
   */
  private String getLocation(String agentName) {
    // Get agents current location
    String node = "";
    try {
      /*TopologyEntry te = topologyService.getEntryForAgent(agentName);
      node = te.getNode();*/
      AddressEntry entrys[] = wps.get(agentName);
      for(int i=0; i<entrys.length; i++) {
        if(entrys[i].getApplication().toString().equals("topology")) {
          String uri = entrys[i].getAddress().toString();
          if(uri.startsWith("node:")) {
            node = uri.substring(uri.lastIndexOf("/")+1, uri.length());
            break;
          }
        }
      }
    } catch (Exception ex) {
      log.error("Exception getting agent location for WhitePagesService", ex);
    }
    return node;
  }

  /**
   * Publishes HealthStatus object to Blackboard for receipt for further
   * analysis and dispositioning by Decision plugin.
   * @param hs      Agents HealthStatus object
   * @param status  Status code
   */
  private void doHealthCheck(HealthStatus hs, int status) {
    //log.info("doHealthCheck: agent=" + hs.getAgentId());
    hs.setState(HealthStatus.HEALTH_CHECK);
    hs.setHealthCheckTime(now());
    hs.setStatus(status);
    bbs.openTransaction();
    bbs.publishChange(hs);
    bbs.closeTransaction();
  }

  /**
   * Utility method for calculating difference between 2 time values.
   * @param start
   * @param end
   * @return Difference (in ms) between two Date objects
   */
  private long elapsedTime(Date start, Date end) {
    return end.getTime() - start.getTime();
  }


  /**
   * Starts a thread that periodically checks for blackboard objects of interest
   * and evaluates the status of monitored agents.
   * @param interval
   */
  private void startEvaluationThread(final long interval) {
    // Starts thread to periodically check HealthStatus of monitored agents
    Runnable healthStatusTimer = new Runnable() {
      public void run() {
        while(true) {
          try { Thread.sleep(interval);
          } catch (Exception ex) {
            log.error(ex.getMessage());
          }
          //getPings();
          evaluateHealthStatus();
        }
      }
    };
    ServiceBroker sb = getBindingSite().getServiceBroker();
    ThreadService ts =
      (ThreadService)sb.getService(this, ThreadService.class, null);
    Schedulable evaluationThread =
      ts.getThread(this, healthStatusTimer, "HealthStatusTimerThread");
    evaluationThread.start();
  }

  /**
   * Determines if an agent has an associated HealthStatus object.
   * collection.
   * @param agentId
   * @return  True if the agent is currently being monitored
   */
  private boolean hasHealthStatus(MessageAddress agentId) {
    synchronized (membersHealthStatus) {
      return membersHealthStatus.containsKey(agentId);
    }
  }

  /**
   * Returns the HealthStatus object associated with a specified agent.
   * @param agentId
   * @return  HealthStatus object associated with specified agent ID
   */
  private HealthStatus getHealthStatus(MessageAddress agentId) {
    synchronized (membersHealthStatus) {
      return (HealthStatus)membersHealthStatus.get(agentId);
    }
  }

  /**
   * Returns a collection of AgentIds associated with the agents currently
   * being monitored.
   * @return  Collection of MessageAddresses
   */
  private Collection findMonitoredAgents() {
    Collection agentIds = new Vector();
    synchronized (membersHealthStatus) {
      for (Iterator it = membersHealthStatus.keySet().iterator(); it.hasNext();)
        agentIds.add(it.next());
    }
    return agentIds;
  }

  /**
   * Adds an agent to the HealthStatus map.
   * @param hs Agents HealthStatus object
   */
  private void addHealthStatus(HealthStatus hs) {
    synchronized (membersHealthStatus) {
      membersHealthStatus.put(hs.getAgentId(), hs);
    }
  }

  /**
   * Removes an agent from the HealthStatus map.
   * @param agentId
   */
  private void removeHealthStatus(MessageAddress agentId) {
    synchronized (membersHealthStatus) {
      membersHealthStatus.remove(agentId);
    }
  }

  /**
   * Send HeartbeatRequest.
   * @param hs HealthStatus object containing ID of agent
   */
  private void sendHeartbeatRequest(HealthStatus hs) {

    HeartbeatRequest hbr = hs.getHeartbeatRequest();

    // Remove any prior HeartbeatRequest
    if (hbr != null) {
      bbs.openTransaction();
      bbs.publishRemove(hbr);
      bbs.closeTransaction();
    }

      // Create/send new request
    if (sensorFactory != null) {
      hs.setHeartbeatRequestTime(now());
      Set targets = new HashSet();
      targets.add(hs.getAgentId());
      hbr = sensorFactory.newHeartbeatRequest(
                  myAgent,                 // Source address
                  targets,                 // Target addresses
                  hs.getHbReqTimeout(),    // Request timeout
                  hs.getHbFrequency(),     // Heartbeat frequency
                  hs.getHbTimeout(),       // Heartbeat timeout
                  true,                    // Only out of spec.
                  hs.getHbPctLate()        // Percent out of spec
      );
      hs.setHeartbeatRequest(hbr);
      bbs.openTransaction();
      bbs.publishAdd(hbr);
      bbs.closeTransaction();
    }
  }

  /**
   * Sends a ping to a monitored agent.
   * @param hs HealthStatus object associated with agent to ping
   */
  private void doPing(HealthStatus hs) {
    // Check to see if there is already a ping in-process for this agent
    if (hs.getPingStatus() != PingRequest.SENT) {
      log.debug("Performing ping: agent=" + hs.getAgentId());
      hs.setPingTimestamp(now());
      PingRequest pr = hs.getPingRequest();
      // Remove any prior PingRequests
      if (pr != null) {
        bbs.openTransaction();
        bbs.publishRemove(pr);
        bbs.closeTransaction();
        // Remove UID from list
        if (pingUIDs.contains(pr.getUID())) pingUIDs.remove(pr.getUID());
      }
      // Create/send new request
      pr = sensorFactory.newPingRequest(myAgent,
                                        hs.getAgentId(),
                                        hs.getPingTimeout());
      pingUIDs.add(pr.getUID());
      hs.setPingRequest(pr);
      bbs.openTransaction();
      bbs.publishAdd(pr);
      bbs.closeTransaction();
    }
  }

  private boolean allNormalLastTime = false;
  private Map previousStateMap = null;

  private String states[] = new String[]{"INITIAL", "NORMAL", "HEALTH_CHECK",
                                 "RESTART", "RESTART_COMPLETE", "FAILED_RESTART",
                                 "MOVE", "FAILED_MOVE", "ROBUSTNESS_INIT_FAIL"};

  /**
   * Print agent/state information.
   */
  private void printStats() {
    Collection agents = findMonitoredAgents();
    Map stateMap = new HashMap();
    for (int i = 0; i < states.length; i++) {
      stateMap.put(states[i], new Vector());
    }
    for (Iterator it = agents.iterator(); it.hasNext();) {
      HealthStatus hs = getHealthStatus((MessageAddress)it.next());
      List l = (List)stateMap.get(hs.getState());
      l.add(hs.getAgentId());
    }
    // Determine if anything has changed since last time
    boolean changed = (previousStateMap == null);
    if (previousStateMap != null) {
      for (Iterator it = stateMap.entrySet().iterator(); it.hasNext();) {
        Map.Entry me = (Map.Entry)it.next();
        String stateName = (String)me.getKey();
        List agentList = (List)me.getValue();
        List prevAgentList = (List)previousStateMap.get(stateName);
        if (agentList.size() != prevAgentList.size() ||
            !agentList.containsAll(prevAgentList)) {
          changed = true;
          break;
        }
      }
    }
    if (changed) {
      int totalAgents = agents.size();
      int agentsInNormalState = ((List)stateMap.get("NORMAL")).size();
      if (agentsInNormalState == totalAgents &&
          totalAgents > 0 &&
          (numAgents == -1 || numAgents == totalAgents)) {
        event("Robustness Community Ready");
        log.info(totalAgents + " Agents Monitored - All in NORMAL state");
        allNormalLastTime = true;
      } else {
        log.info("Agents Monitored: " + totalAgents);
        for (Iterator it = stateMap.entrySet().iterator(); it.hasNext();) {
          Map.Entry me = (Map.Entry)it.next();
          String stateName = (String)me.getKey();
          List agentList = (List)me.getValue();
          if (stateName.equals("NORMAL")) {
            log.info("  - " + stateName + ": " + agentList.size());
          } else {
            if (agentList.size() > 0) {
            log.info("  - " + stateName + ": " + agentList.size() + " " + agentList);
            }
          }
        }
      }
    }
    previousStateMap = stateMap;

  }

  /**
   * Gets externally configurable parameters defined in community attributes.
   */
  private void getPropertiesFromCommunityAttributes() {
    //StringBuffer sb = new StringBuffer("getPropertiesFromCommunityAttributes:");
    Attributes attrs =
      communityService.getCommunityAttributes(communityToMonitor);
    try {
      NamingEnumeration enum = attrs.getAll();
      while (enum.hasMore()) {
        Attribute attr = (Attribute)enum.nextElement();
        String id = attr.getID();
        if (healthMonitorProps.containsKey(id)) {
          String value = (String)attr.get();
          healthMonitorProps.setProperty(id, value);
          //sb.append(" " + id + "=" + value);
        }
      }
      //log.info(sb.toString());
    } catch (NamingException ne) {
      log.error("Exception getting attributes from CommunityService, " + ne);
    }
  }

  /**
   * Updates Community Attributes that define configurable robustness parameters.
   * @param props  Robustness parameters
   */
  private void updateCommunityAttributes(Properties props) {
    if (communityToMonitor != null) {
      ModificationItem mods[] = new ModificationItem[props.size()];
      int index = 0;
      for (Enumeration enum = props.propertyNames(); enum.hasMoreElements();) {
        String id = (String)enum.nextElement();
        String value = props.getProperty(id);
        Attribute attr = new BasicAttribute(id, value);
        mods[index++] =
          new ModificationItem(DirContext.REPLACE_ATTRIBUTE, attr);
      }
      if (mods.length > 0) {
        communityService.modifyCommunityAttributes(communityToMonitor, mods);
      }
    }
  }

  /**
   * Sets externally configurable parameters using supplied Properties object.
   * @param props Propertie object defining paramater names and values.
   */
  private void updateParams(Properties props) {
    heartbeatRequestTimeout = Long.parseLong(props.getProperty("hbReqTimeout"));
    heartbeatRequestRetries = Integer.parseInt(props.getProperty("hbReqRetries"));
    heartbeatRequestRetryFrequency = Long.parseLong(props.getProperty("hbReqRetryFreq"));
    heartbeatFrequency = Long.parseLong(props.getProperty("hbFreq"));
    heartbeatTimeout = Long.parseLong(props.getProperty("hbTimeout"));
    heartbeatPctLateThreshold = Float.parseFloat(props.getProperty("hbPctLate"));
    heartbeatFailureRateWindow = Long.parseLong(props.getProperty("hbWindow"));
    heartbeatFailureRateThreshold = Float.parseFloat(props.getProperty("hbFailRate"));
    pingTimeout = Long.parseLong(props.getProperty("pingTimeout"));
    pingRetries = Integer.parseInt(props.getProperty("pingRetries"));
    evaluationFrequency = Long.parseLong(props.getProperty("evalFreq"));
    restartTimeout = Long.parseLong(props.getProperty("restartTimeout"));
    restartRetryFrequency = Long.parseLong(props.getProperty("restartRetryFreq"));
    activePingFrequency = Long.parseLong(props.getProperty("activePingFreq"));
    numAgents = Integer.parseInt(props.getProperty("numAgents"));
    updateCommunityAttributes(props);
  }

  /**
   * Obtains plugin parameters
   * @param obj List of "name=value" parameters
   */
  public void setParameter(Object obj) {
    List args = (List)obj;
    for (Iterator it = args.iterator(); it.hasNext();) {
      String arg = (String)it.next();
      String name = arg.substring(0,arg.indexOf("="));
      String value = arg.substring(arg.indexOf('=')+1);
      healthMonitorProps.setProperty(name, value);
    }
  }

  /**
   * Returns current date/time;
   * @return Current date
   */
  private Date now() {
    return new Date();
  }

  public void communityChanged(CommunityChangeEvent cce) {
    //log.info("CommunityChangeEvent: " + cce);
    //if (cce.getType() == cce.ADD_COMMUNITY && communityToMonitor == null)
      //getCommunityToMonitor();
    if (communityToMonitor != null && cce.getCommunityName().equals(communityToMonitor) &&
        (cce.getType() == cce.ADD_ENTITY || cce.getType() == cce.REMOVE_ENTITY)) {
      communityChanged = true;
    }
  }

  public String getCommunityName() {
    return communityToMonitor;
  }

  /**
   * Predicate for HeartbeatRequests.
   */
  private IncrementalSubscription heartbeatRequests;
  private UnaryPredicate heartbeatRequestPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof HeartbeatRequest);
  }};

  /**
   * Predicate for Heartbeats.
   */
  private IncrementalSubscription heartbeatHealthReports;
  private UnaryPredicate heartbeatPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof HeartbeatHealthReport);
  }};


  private IncrementalSubscription pingRequests;
  private UnaryPredicate pingRequestPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof PingRequest) {
        PingRequest pr = (PingRequest)o;
        return (pingUIDs.contains(pr.getUID()));
      }
      return false;
  }};


  /**
   * Gets reference to CommunityService.
   * @return Reference to CommunityService
   */
  private CommunityService getCommunityService() {
    int counter = 0;
    ServiceBroker sb = getBindingSite().getServiceBroker();
    while (!sb.hasService(CommunityService.class)) {
      // Print a message after waiting for 30 seconds
      if (++counter == 60) log.info("Waiting for CommunityService ... ");
      try { Thread.sleep(500); } catch (Exception ex) {log.error(ex.getMessage());}
    }
    return (CommunityService)sb.getService(this, CommunityService.class, null);
  }

  /**
   * Sends Cougaar event via EventService.
   */
  private void event(String message) {
    if (eventService != null && eventService.isEventEnabled())
      eventService.event(message);
  }

  /**
   * Gets reference to WhitePagesService.
   * @return Reference to WhitePagesService.
   */
  //modified at Mar.04, 2003 to match cougaar-10.2 by qing
  private WhitePagesService getWhitePagesService() {
    int counter = 0;
    ServiceBroker sb = getBindingSite().getServiceBroker();
    while (!sb.hasService(WhitePagesService.class)) {
      // Print a message after waiting for 30 seconds
      if (++counter == 60) log.info("Waiting for WhitePagesService ... ");
      try { Thread.sleep(500); } catch (Exception ex) {log.error(ex.getMessage());}
    }
    return (WhitePagesService)sb.getService(this, WhitePagesService.class, null);
  }


  /**
   * Predicate for Management Agent properties
   */
  String myPluginName = getClass().getName();
  private IncrementalSubscription mgmtAgentProps;
  private UnaryPredicate propertiesPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof ManagementAgentProperties) {
        ManagementAgentProperties props = (ManagementAgentProperties)o;
        String forName = props.getPluginName();
        return (myPluginName.equals(forName) || myPluginName.endsWith(forName));
      }
      return false;
  }};

}
