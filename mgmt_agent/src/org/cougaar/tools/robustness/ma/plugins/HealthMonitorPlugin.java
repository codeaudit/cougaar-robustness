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
  private Collection membersHealthStatus = new Vector();

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
    Date now = new Date();
    //synchronized (membersHealthStatus) {
    for (Iterator it = membersHealthStatus.iterator(); it.hasNext();) {
      HealthStatus hs = (HealthStatus)it.next();
      String state = hs.getState();
      //log.debug(hs.getAgentId() + " state=" + state);
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
            hs.setState(HealthStatus.NORMAL);
            hs.setHeartbeatStatus(HealthStatus.OK);
            hs.setHeartbeatRequestRetries(0);
            break;
          case HeartbeatRequest.REFUSED:
            log.warn("HeartbeatRequest for agent '" + hs.getAgentId() + "' REFUSED");
            //doHealthCheck(hs, HealthStatus.NO_RESPONSE);
            break;
          case HeartbeatRequest.FAILED:
            int retries = hs.getHeartbeatRequestRetries();
            if (retries < heartbeatRequestRetries) {
              log.debug("HeartbeatRequest for agent '" + hs.getAgentId() + "' FAILED, retrying");
			        hs.setHeartbeatRequestRetries(++retries);
              hs.setHeartbeatRequestStatus(HealthStatus.UNDEFINED);
		        } else {
              log.warn("HeartbeatRequest for agent '" + hs.getAgentId() + "' FAILED");
			        hs.setHeartbeatRequestRetries(0);
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
                //log.debug("Heartbeat timeout: agent=" + hs.getAgentId());
                log.debug("Performing ping: agent=" + hs.getAgentId());
                hs.setPingTimestamp(new Date());
                doPing(hs.getAgentId());
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
                hs.setPingRetries(0);
                break;
              case PingRequest.FAILED:
                int retries = hs.getPingRetries();
                if (retries < pingRetries) {
                  log.debug("Ping failed: agent=" + hs.getAgentId() + ", retrying");
			            hs.setPingRetries(++retries);
                  hs.setPingStatus(HealthStatus.UNDEFINED);
		            } else {
                  log.error("Ping failed: agent=" + hs.getAgentId());
			            hs.setPingRetries(0);
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
            doPing(hs.getAgentId());
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
    //}
  }

  /**
   * Evaluate roster for additions/deletions.  Update membersHealthStatus
   * collection to maintain consistency with roster.
   * @param roster
   */
  private void processRosterChanges(CommunityRoster roster) {
    Collection cmList = roster.getMembers();
    // If first time, copy members from roster to local list
    //synchronized (membersHealthStatus) {
    if (membersHealthStatus.isEmpty()) {
      //log.debug(roster.toString());
      for (Iterator it = cmList.iterator(); it.hasNext();) {
        CommunityMember cm = (CommunityMember)it.next();
        if (cm.isAgent()) {
          //HealthStatus hs = new HealthStatus(getAgentId());
          HealthStatus hs = new HealthStatus(
              new ClusterIdentifier(cm.getName()),
              communityToMonitor,
              getBindingSite().getServiceBroker(),
              heartbeatFrequency,
              heartbeatFailureRateWindow,
              heartbeatFailureRateThreshold);
          hs.setState(HealthStatus.INITIAL);
          membersHealthStatus.add(hs);
          log.info("Adding " + cm.getName());
        }
      }
    } else {
      // Look for additions
      Collection newMembers = new Vector();
      for (Iterator it = cmList.iterator(); it.hasNext();) {
        CommunityMember cm = (CommunityMember)it.next();
        if (cm.isAgent() && !hasHealthStatus(new ClusterIdentifier(cm.getName()))) {
          HealthStatus hs = new HealthStatus(
            new ClusterIdentifier(cm.getName()),
            communityToMonitor,
            getBindingSite().getServiceBroker(),
            heartbeatFrequency,
            heartbeatFailureRateWindow,
            heartbeatFailureRateThreshold);
          hs.setState(HealthStatus.INITIAL);
          membersHealthStatus.add(hs);
          newMembers.add(hs);
          log.info("Adding " + cm.getName());
        }
      }
      // Look for deletions
      for (Iterator it = membersHealthStatus.iterator(); it.hasNext();) {
        HealthStatus hs = (HealthStatus)it.next();
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
    //}
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
    Thread evaluationThread = new Thread("HealthStatusTimerThread") {
      public void run() {
        while(true) {
          try { Thread.sleep(interval); } catch (Exception ex) {}
          getPings();
          evaluateHealthStatus();
        }
      }
    };
    evaluationThread.setPriority(Thread.NORM_PRIORITY);
    evaluationThread.start();
  }

  /**
   * Determines if an agent has an associated HealthStatus object.
   * collection.
   * @param agentId
   * @return  True if the agent is currently being monitored
   */
  private boolean hasHealthStatus(ClusterIdentifier agentId) {
    //synchronized (membersHealthStatus) {
    for (Iterator it = membersHealthStatus.iterator(); it.hasNext();) {
      HealthStatus hs = (HealthStatus)it.next();
      if (hs.getAgentId().equals(agentId)) return true;
    }
    //}
    return false;
  }

  /**
   * Returns the HealthStatus object associated with a specified agent.
   * @param agentId
   * @return  HealthStatus object associated with specified agent ID
   */
  private HealthStatus getHealthStatus(MessageAddress agentId) {
    //synchronized (membersHealthStatus) {
    for (Iterator it = membersHealthStatus.iterator(); it.hasNext();) {
      HealthStatus hs = (HealthStatus)it.next();
      if (hs.getAgentId().equals(agentId)) return hs;
    }
    //}
    return null;
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
                  myAgent,                  // Source address
                  targets,                  // Target addresses
                  heartbeatRequestTimeout,  // Request timeout
                  heartbeatFrequency,       // Heartbeat frequency
                  heartbeatTimeout,         // Heartbeat timeout
                  true,                     // Only out of spec.
                  heartbeatPctLateThreshold // Percent out of spec
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
  private void doPing(MessageAddress target) {
    PingRequest req = sensorFactory.newPingRequest(myAgent, target, pingTimeout);
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
