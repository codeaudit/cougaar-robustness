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

import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.SimplePlugin;
import org.cougaar.core.mts.MessageAddress;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.component.ServiceRevokedEvent;

import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;

import org.cougaar.core.service.community.*;
import org.cougaar.community.*;

import org.cougaar.util.UnaryPredicate;

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
 * pingTimeout      Defines the ping timeout period (in milliseconds).
 * pingRetries      Defines the number of times to retry a ping
 *                  when a failure is encountered
 * evalFreq         Defines how often (in milliseconds) the HealthStatus of
 *                  monitored agents is evaluated.
 * </PRE>
 */

public class HealthMonitorPlugin extends SimplePlugin {

  // Defines default values for configurable parameters.
  private static String defaultParams[][] = {
    {"community",    ""},
    {"hbReqTimeout", "60000"},
    {"hbReqRetries", "2"},
    {"hbFreq",       "30000"},
    {"hbTimeout",    "10000"},
    {"hbPctLate",    "80.0"},
    {"hbWindow",     "360000"},  // Default to 3 minute window
    {"hbFailRate",   "0.5"},
    {"pingTimeout",  "10000"},
    {"pingRetries",  "2"},
    {"evalFreq",     "10000"}
  };
  ManagementAgentProperties healthMonitorProps =
    ManagementAgentProperties.makeProps(this.getClass().getName(), defaultParams);


  /////////////////////////////////////////////////////////////////////////
  //  Externally configurable parameters
  /////////////////////////////////////////////////////////////////////////
  private long  heartbeatRequestTimeout;
  private int   heartbeatRequestRetries;
  private long  heartbeatFrequency;
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
  private long evaluationFrequency;

  // Name of community to monitor
  private String communityToMonitor = null;

  /////////////////////////////////////////////////////////////////////////
  //  End of externally configurable parameters
  /////////////////////////////////////////////////////////////////////////


  private SensorFactory sensorFactory;
  private ClusterIdentifier myAgent = null;
  private LoggingService log;
  private BlackboardService bbs = null;

  // HealthStatus objects for agents in monitored community
  private Map membersHealthStatus = new HashMap();

  private CommunityService communityService = null;

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

    sensorFactory =
      ((SensorFactory) domainService.getFactory("sensors"));
    if (sensorFactory == null) {
      log.error("Unable to get 'sensors' domain");
    }

    myAgent = getClusterIdentifier();

    communityService = getCommunityService();

    // Find name of community to monitor
    Collection communities = communityService.search("(CommunityManager=" +
      myAgent.toString() + ")");
    if (!communities.isEmpty())
      healthMonitorProps.setProperty("community",
                                      (String)communities.iterator().next());

    // Initialize configurable paramaeters from defaults and plugin arguments.
    updateParams(healthMonitorProps);
    bbs.publishAdd(healthMonitorProps);

    // Subscribe to CommunityRequests to get roster (and roster updates)
    // from CommunityPlugin
    communityRequests =
      (IncrementalSubscription)bbs.subscribe(communityRequestPredicate);

    // Subscribe to HeartbeatRequests to determine if HeartbeatRequest was
    // received/accepted by monitored agent
    heartbeatRequests =
      (IncrementalSubscription)bbs.subscribe(heartbeatRequestPredicate);

    // Subscribe to HeartbeatHealthReports to receive notification of failed
    // heartbeats
    heartbeatHealthReports =
      (IncrementalSubscription)bbs.subscribe(heartbeatPredicate);

    // Subscribe to ManagementAgentProperties to get changes to configurable
    // parameters.
    mgmtAgentProps =
      (IncrementalSubscription)bbs.subscribe(propertiesPredicate);

    // Get Roster for community to monitor
    if (communityToMonitor != null && communityToMonitor.length() > 0) {
      sendRosterRequest(communityToMonitor);
    }

    // Start evaluation thread to periodically update and analyze the Health
    // Status of monitored agents
    startEvaluationThread(evaluationFrequency);

    // Print informational message defining current parameters
    StringBuffer startMsg = new StringBuffer();
    startMsg.append("HealthMonitorPlugin started: agent=" + myAgent);
    startMsg.append(" " + paramsToString());
    log.info(startMsg.toString());
  }

  /**
   * Send request to CommunityPlugin to return a list of members in the
   * Robustness community thats being monitored.  This roster will be
   * automatically updated by the CommunityPlugin when changes to community
   * membership occur.
   * @param communityName Name of community to be monitored
   */
  private void sendRosterRequest(String communityName) {
    CommunityRequest cr = new CommunityRequestImpl();
    cr.setVerb("GET_ROSTER_WITH_UPDATES");
    cr.setTargetCommunityName(communityToMonitor);
    bbs.publishAdd(cr);
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
      if (!props.getProperty("community").equals(communityToMonitor)) {
        communityToMonitor = props.getProperty("community");
        sendRosterRequest(communityToMonitor);
      }
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
        if (hs != null) hs.setHeartbeatRequestStatus(status);
      }
      if (status == HeartbeatRequest.REFUSED ||
          status == HeartbeatRequest.FAILED) {
        bbs.publishRemove(req);
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
          hs.addHeartbeatTimeout(new Date());
          log.debug("HeartbeatTimeout: agent=" + hs.getAgentId() +
            ", pctLate=" + hbe[i].getPercentLate());
        }
      }
      bbs.publishRemove(hbhr);
    }

    // Get CommunityRoster (and updates)
    for (Iterator it = communityRequests.getChangedCollection().iterator();
         it.hasNext();) {
      CommunityRequest req = (CommunityRequest)it.next();
      if (req.getVerb() != null &&
          req.getVerb().equals("GET_ROSTER_WITH_UPDATES")) {
        CommunityResponse resp = req.getCommunityResponse();
        CommunityRoster roster = (CommunityRoster)resp.getResponseObject();
        // Ensure membersHealthStatus collection is consistent with current
        // community membership
        processRosterChanges(roster);
      }
    }

  }

  /**
   * Periodically evaluates the HealthStatus of monitored agents.
   */
  private void evaluateHealthStatus() {
    Collection currentAgents = findMonitoredAgents();
    for (Iterator it = currentAgents.iterator(); it.hasNext();) {
      HealthStatus hs = getHealthStatus((MessageAddress)it.next());
      String state = hs.getState();
      log.debug("Evaluating agent " + hs.getAgentId() + " state=" + state);
      if (state.equals(HealthStatus.INITIAL)) {
        switch (hs.getHeartbeatRequestStatus()) {
          case HealthStatus.UNDEFINED:
            sendHeartbeatRequest(hs);
            log.debug("Sending HeartbeatRequest to agent '" + hs.getAgentId() + "'");
            break;
          case HeartbeatRequest.NEW:
          case HeartbeatRequest.SENT:
            //if (elapsedTime(hs.getHeartbeatRequestTime(), new Date()) > heartbeatRequestTimeout) {
            //  log.warn("HeartbeatRequest for agent '" + hs.getAgentId() + "' timed out");
            //  hs.setHeartbeatRequestStatus(HeartbeatRequest.FAILED);
            //}
            break;
          case HeartbeatRequest.ACCEPTED:
            log.debug("HeartbeatRequest for agent '" + hs.getAgentId() + "' ACCEPTED");
            hs.setState(HealthStatus.NORMAL);
            hs.setHeartbeatStatus(HealthStatus.OK);
            hs.setHbReqRetryCtr(0);
            break;
          case HeartbeatRequest.REFUSED:
            log.warn("HeartbeatRequest for agent '" + hs.getAgentId() + "' REFUSED");
            //doHealthCheck(hs, HealthStatus.NO_RESPONSE);
            break;
          case HeartbeatRequest.FAILED:
            int retries = hs.getHbReqRetryCtr();
            if (retries < hs.getHbReqRetries()) {
              log.info("HeartbeatRequest for agent '" + hs.getAgentId() + "' FAILED, retrying");
			        hs.setHbReqRetryCtr(++retries);
              hs.setHeartbeatRequestStatus(HealthStatus.UNDEFINED);
		        } else {
              log.warn("HeartbeatRequest for agent '" + hs.getAgentId() + "' FAILED");
			        hs.setHbReqRetryCtr(0);
              doHealthCheck(hs, HealthStatus.NO_RESPONSE);
			      }
            break;
          default:
        }
      } else if (state.equals(HealthStatus.NORMAL)) {
        int hbStatus = hs.getHeartbeatStatus();
        switch (hbStatus) {
          case HealthStatus.HB_TIMEOUT:
            int pingStatus = hs.getPingStatus();
            switch (pingStatus) {
              case HealthStatus.UNDEFINED:
                log.info("Heartbeat timeout: agent=" + hs.getAgentId());
                log.info("Performing ping: agent=" + hs.getAgentId());
                hs.setPingTimestamp(new Date());
                doPing(hs);
                break;
              case PingRequest.RECEIVED:
            	log.debug("Received ping, agent=" + hs.getAgentId());
                if (!hs.hbFailureRateInSpec()) {
                  log.error("Exceeded Heartbeat timeout threshold: agent="
                    + hs.getAgentId());
                  doHealthCheck(hs, HealthStatus.DEGRADED);
                }
                hs.setPingStatus(HealthStatus.UNDEFINED);
                hs.setHeartbeatStatus(HealthStatus.OK);
                hs.setPingRetryCtr(0);
                break;
              case PingRequest.FAILED:
                int retries = hs.getPingRetryCtr();
                if (retries < hs.getPingRetries()) {
                  log.debug("info failed: agent=" + hs.getAgentId() + ", retrying");
			            hs.setPingRetryCtr(++retries);
                  hs.setPingStatus(HealthStatus.UNDEFINED);
		            } else {
                  log.error("Ping failed: agent=" + hs.getAgentId());
			            hs.setPingRetryCtr(0);
                  hs.setPingStatus(HealthStatus.UNDEFINED);
                  doHealthCheck(hs, HealthStatus.NO_RESPONSE);
		            }
                break;
              default:
            }
          default:
        }
      } else if (state.equals(HealthStatus.RESTART)) {
        int pingStatus = hs.getPingStatus();
        //log.debug("agent=" + hs.getAgentId() + " state=" + state + " pingStatus=" + pingStatus);
        switch (pingStatus) {
          case HealthStatus.UNDEFINED:
            hs.setPingTimestamp(new Date());
            doPing(hs);
            log.debug("Sending ping: agent=" + hs.getAgentId() + ", state=RESTART");
            break;
          case PingRequest.RECEIVED:
            hs.setPingStatus(HealthStatus.UNDEFINED);
            hs.setHeartbeatRequestStatus(HealthStatus.UNDEFINED);
            hs.setHeartbeatStatus(HealthStatus.UNDEFINED);
            hs.setState(HealthStatus.INITIAL);
            log.info("Reacquired agent: agent=" + hs.getAgentId());
            break;
          case PingRequest.FAILED:
            log.debug("Ping failed: agent=" + hs.getAgentId() + ", state=RESTART");
            hs.setPingStatus(HealthStatus.UNDEFINED);  // Initiates retry
          default:
        }
      } else {
        log.warn("Invalid run state: agent=" + hs.getAgentId() + ", state=" + state);
      }
    }
  }

  /**
   * Evaluate roster for additions/deletions.  Update membersHealthStatus
   * collection to maintain consistency with roster.
   * @param roster
   */
  private void processRosterChanges(CommunityRoster roster) {
    Collection cmList = roster.getMembers();
    // If first time, copy members from roster to local list
    if (membersHealthStatus.isEmpty()) {
      //log.debug(roster.toString());
      for (Iterator it = cmList.iterator(); it.hasNext();) {
        CommunityMember cm = (CommunityMember)it.next();
        if (cm.isAgent()) {
          HealthStatus hs = newHealthStatus(new ClusterIdentifier(cm.getName()));
          hs.setState(HealthStatus.INITIAL);
          addHealthStatus(hs);
          //membersHealthStatus.put(hs.getAgentId(),hs);
          log.info("Adding " + cm.getName());
        }
      }
    } else {
      // Look for additions
      Collection newMembers = new Vector();
      for (Iterator it = cmList.iterator(); it.hasNext();) {
        CommunityMember cm = (CommunityMember)it.next();
        ClusterIdentifier agent = new ClusterIdentifier(cm.getName());
        if (cm.isAgent() && !hasHealthStatus(agent)) {
          HealthStatus hs = newHealthStatus(agent);
          hs.setState(HealthStatus.INITIAL);
          //membersHealthStatus.put(hs.getAgentId(),hs);
          addHealthStatus(hs);
          newMembers.add(hs);
          log.info("Adding " + cm.getName());
        }
      }
      // Look for deletions
      for (Iterator it = findMonitoredAgents().iterator(); it.hasNext();) {
        HealthStatus hs = getHealthStatus((MessageAddress)it.next());
        boolean found = false;
        for (Iterator it1 = cmList.iterator(); it1.hasNext();) {
          CommunityMember cm = (CommunityMember)it1.next();
          if (hs.getAgentId().equals(new ClusterIdentifier(cm.getName()))) {
            found = true;
            break;
          }
        }
        if (!found) {
          log.info("Removed " + hs.getAgentId());
          it.remove();
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
    HealthStatus hs = new HealthStatus(agentId,
                         communityToMonitor,
                         getBindingSite().getServiceBroker(),
                         heartbeatRequestTimeout,
                         heartbeatRequestRetries,
                         heartbeatFrequency,
                         heartbeatTimeout,
                         heartbeatPctLateThreshold,
                         heartbeatFailureRateWindow,
                         heartbeatFailureRateThreshold,
                         pingTimeout,
                         pingRetries);
    // Get agent attributes from community service and set any agent-specific
    // parameters
    Attributes attrs =
      communityService.getEntityAttributes(communityToMonitor, agentId.toString());
    NamingEnumeration enum = attrs.getAll();
    try {
      while (enum.hasMoreElements()) {
        boolean found = true;
        Attribute attr = (Attribute)enum.nextElement();
        if (attr.getID().equalsIgnoreCase("hbReqTimeout")) {
          hs.setHbReqTimeout(Long.parseLong((String)attr.get()));
        } else if (attr.getID().equalsIgnoreCase("hbReqRetries")) {
          hs.setHbReqRetries(Long.parseLong((String)attr.get()));
        } else if (attr.getID().equalsIgnoreCase("hbFreq")) {
          hs.setHbFrequency(Long.parseLong((String)attr.get()));
        } else if (attr.getID().equalsIgnoreCase("hbTimeout")) {
          hs.setHbTimeout(Long.parseLong((String)attr.get()));
        } else if (attr.getID().equalsIgnoreCase("hbPctLate")) {
          hs.setHbPctLate(Float.parseFloat((String)attr.get()));
        } else if (attr.getID().equalsIgnoreCase("hbWindow")) {
          hs.setHbWindow(Long.parseLong((String)attr.get()));
        } else if (attr.getID().equalsIgnoreCase("hbFailRate")) {
          hs.setHbFailRate(Float.parseFloat((String)attr.get()));
        } else if (attr.getID().equalsIgnoreCase("pingTimeout")) {
          hs.setPingTimeout(Long.parseLong((String)attr.get()));
        } else if (attr.getID().equalsIgnoreCase("pingRetries")) {
          hs.setPingRetries(Long.parseLong((String)attr.get()));
        } else {
          found = false;
        }
        if (log.isDebugEnabled() && found)
          log.debug("Setting agent health parameter: agent=" + agentId +
                    " parameter=" + attr.getID() +
                    " value=" + (String)attr.get());
      }
    } catch (Exception ex) {
      log.error("Exception parsing agent health monitor parameters, " + ex);
    }
    return hs;
  }

  /**
   * Get ping results.
   */
  private void getPings() {
    bbs.openTransaction();
    Collection pings = bbs.query(pingRequestPredicate);
    bbs.closeTransaction();
    for (Iterator it = pings.iterator(); it.hasNext();) {
      PingRequest req = (PingRequest)it.next();
      if (req.getSource().equals(myAgent)) {
        int status = req.getStatus();
        HealthStatus hs = getHealthStatus(req.getTarget());
        hs.setPingStatus(status);
        switch (status) {
          case PingRequest.SENT:
            // The following is used to simulate a Ping timeout.  This will
            // ultimately be done in MTS
            //if (elapsedTime(hs.getPingTimestamp(), new Date()) > pingTimeout)
            //  req.setStatus(PingRequest.FAILED);
            break;
          case PingRequest.RECEIVED:
          case PingRequest.FAILED:
            bbs.openTransaction();
            bbs.publishRemove(req);
            bbs.closeTransaction();
            break;
          default:
            if (log.isDebugEnabled())
              log.debug("PingRequest: illegal status = " + req.getStatus());
            bbs.openTransaction();
            bbs.publishRemove(req);
            bbs.closeTransaction();
        }
      }
    }
  }

  /**
   * Publishes HealthStatus object to Blackboard for receipt for further
   * analysis and dispositioning by Decision plugin.
   * @param hs      Agents HealthStatus object
   * @param status  Status code
   */
  private void doHealthCheck(HealthStatus hs, int status) {
    hs.setState(HealthStatus.HEALTH_CHECK);
    hs.setStatus(status);
    log.debug("Passing agent to DecisionPlugin: agent=" + hs.getAgentId());
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
          getPings();
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
  private boolean hasHealthStatus(ClusterIdentifier agentId) {
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
   * @param agentId
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
    if (sensorFactory != null) {
      hs.setHeartbeatRequestTime(new Date());
      Set targets = new HashSet();
      targets.add(hs.getAgentId());
      HeartbeatRequest hbr = hs.getHeartbeatRequest();
      if (hbr == null) {
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
      } else {
      	bbs.openTransaction();
      	bbs.publishChange(hbr);
      	bbs.closeTransaction();
	  }
      hs.setHeartbeatRequestStatus(hbr.getStatus());
    }
  }

  /**
   * Sends a ping to a monitored agent.
   * @param target  Monitored agents address
   */
  private void doPing(HealthStatus hs) {
    PingRequest req = sensorFactory.newPingRequest(myAgent,
                                                   hs.getAgentId(),
                                                   hs.getPingTimeout());
    bbs.openTransaction();
    bbs.publishAdd(req);
    bbs.closeTransaction();
  }


  /**
   * Sets externally configurable parameters using supplied Properties object.
   * @param props Propertie object defining paramater names and values.
   */
  private void updateParams(Properties props) {
    communityToMonitor = props.getProperty("community");
    heartbeatRequestTimeout = Long.parseLong(props.getProperty("hbReqTimeout"));
    heartbeatRequestRetries = Integer.parseInt(props.getProperty("hbReqRetries"));
    heartbeatFrequency = Long.parseLong(props.getProperty("hbFreq"));
    heartbeatTimeout = Long.parseLong(props.getProperty("hbTimeout"));
    heartbeatPctLateThreshold = Float.parseFloat(props.getProperty("hbPctLate"));
    heartbeatFailureRateWindow = Long.parseLong(props.getProperty("hbWindow"));
    heartbeatFailureRateThreshold = Float.parseFloat(props.getProperty("hbFailRate"));
    pingTimeout = Long.parseLong(props.getProperty("pingTimeout"));
    pingRetries = Integer.parseInt(props.getProperty("pingRetries"));
    evaluationFrequency = Long.parseLong(props.getProperty("evalFreq"));
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


  private IncrementalSubscription communityRequests;
  /**
   * Predicate for CommunityRequests.  Used to receive CommunityResponse objects
   * with CommunityRosters.
   */
  private UnaryPredicate communityRequestPredicate = new UnaryPredicate() {
    public boolean execute (Object o) {
      if (o instanceof CommunityRequest) {
        String communityName = ((CommunityRequest)o).getTargetCommunityName();
        return (communityName != null &&
                communityName.equals(communityToMonitor));
      }
      return false;
  }};


  private UnaryPredicate pingRequestPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof PingRequest);
  }};


  /**
   * Gets reference to CommunityService.
   */
  private CommunityService getCommunityService() {
    ServiceBroker sb = getBindingSite().getServiceBroker();
    if (sb.hasService(CommunityService.class)) {
      return (CommunityService)sb.getService(this, CommunityService.class,
        new ServiceRevokedListener() {
          public void serviceRevoked(ServiceRevokedEvent re) {}
      });
    } else {
      log.error("CommunityService not available");
      return null;
    }
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
