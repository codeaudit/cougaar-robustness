/*
 * <copyright>
 *  Copyright 1997-2001 Mobile Intelligence Corp
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
package org.cougaar.tools.robustness.ma.plugins;

import java.util.*;

import org.cougaar.tools.robustness.HealthReport;
import org.cougaar.tools.robustness.HeartbeatRequest;
import org.cougaar.tools.robustness.ma.RobustnessFactory;
import org.cougaar.tools.robustness.sensors.*;

import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.SimplePlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.mts.MessageAddress;

import org.cougaar.core.service.community.*;
import org.cougaar.community.*;

import org.cougaar.util.UnaryPredicate;

/**
 * This plugin monitors a set of agents and publishes messages the blackboard
 * when an agent is determined to be unresponsive.  These messages are picked
 * up by another plugin that is responsible for deciding whether the
 * unresponsive agent should be restarted.
 * The set of agents to be monitored is determined by community membership.
 */

public class HealthMonitorPlugin extends SimplePlugin {

  // Tuning parameters
  private long heartbeatInterval  = 5000;
  private long heartbeatTimeout   = 10000;
  private long pingTimeout        = 10000;
  private long evaluationInterval = 5000;

  private RobustnessFactory robustnessFactory;
  private PingFactory pingFactory;
  private ClusterIdentifier myAgent = null;
  private LoggingService log;
  private BlackboardService bbs = null;

  // Name of community to monitor
  private String communityToMonitor = null;

  // HealthStatus objects for agents in monitored community
  private Collection membersHealthStatus = null;

  /**
   * Obtains name of community to monitor from plugin parameter
   * @param obj CommunityName
   */
  public void setParameter(Object obj) {
    List args = (List)obj;
    if (args.size() == 1)
      communityToMonitor = (String)args.get(0);
  }

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

    DomainService domainService =
      (DomainService) getBindingSite().getServiceBroker().
      getService(this, DomainService.class, null);

    robustnessFactory =
      ((RobustnessFactory) domainService.getFactory("robustness"));
    if (robustnessFactory == null) {
      log.error("Unable to get 'robustness' domain");
    }

    pingFactory =
      ((PingFactory) domainService.getFactory("ping"));
    if (pingFactory == null) {
      log.error("Unable to get 'ping' domain");
    }

    bbs = getBlackboardService();

    myAgent = getClusterIdentifier();

    // Subscribe to ping requests
    //pingRequests = (IncrementalSubscription)bbs.subscribe(pingRequestPredicate);

    // Subscribe to CommunityRequests to get roster (and roster updates)
    // from CommunityPlugin
    communityRequests =
      (IncrementalSubscription)bbs.subscribe(communityRequestPredicate);

    // Send request to CommunityPlugin to return a list of members in the
    // Robustness community thats being monitored.  This roster will be
    // automatically updated by the CommunityPlugin when changes to community
    // membership occur.
    CommunityRequest cr = new CommunityRequestImpl();
    cr.setVerb("GET_ROSTER_WITH_UPDATES");
    cr.setTargetCommunityName(communityToMonitor);
    bbs.publishAdd(cr);

    startEvaluationThread(evaluationInterval);
    log.warn("HealthMonitorPlugin started:" +
      " agent=" + myAgent +
      " monitoredCommunity=" + communityToMonitor +
      " heartbeatInterval=" + heartbeatInterval + "ms" +
      " heartbeatTimeout=" + heartbeatTimeout + "ms" +
      " pingTimeout=" + pingTimeout + "ms" +
      " evaluationInterval=" + evaluationInterval + "ms");
  }

  /**
   * Receives CommunityRoster from CommunityPlugin and HealthReports for
   * monitored agents.
   */
  public void execute() {

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
   * Evaluate roster for additions/deletions.  Update membersHealthStatus
   * collection to maintain consistency with roster.
   * @param roster
   */
  private void processRosterChanges(CommunityRoster roster) {
    Collection cmList = roster.getMembers();
    // If first time, copy members from roster to local list
    if (membersHealthStatus == null) {
      log.info(roster.toString());
      membersHealthStatus = new Vector();
      for (Iterator it = cmList.iterator(); it.hasNext();) {
        CommunityMember cm = (CommunityMember)it.next();
        if (cm.isAgent()) {
          HealthStatus hs = new HealthStatus(cm.getAgentId());
          membersHealthStatus.add(hs);
          log.warn("Adding " + cm.getName());
        }
      }
      if (!membersHealthStatus.isEmpty()) sendHeartbeatRequest(membersHealthStatus);
    } else {
      // Look for additions
      Collection newMembers = new Vector();
      for (Iterator it = cmList.iterator(); it.hasNext();) {
        CommunityMember cm = (CommunityMember)it.next();
        if (cm.isAgent() && !hasHealthStatus(cm.getAgentId())) {
          HealthStatus hs = new HealthStatus(cm.getAgentId());
          membersHealthStatus.add(hs);
          newMembers.add(hs);
          log.warn("Adding " + cm.getName());
        }
        if (!newMembers.isEmpty()) sendHeartbeatRequest(newMembers);
      }
      // Look for deletions
      for (Iterator it = membersHealthStatus.iterator(); it.hasNext();) {
        HealthStatus hs = (HealthStatus)it.next();
        boolean found = false;
        for (Iterator it1 = cmList.iterator(); it1.hasNext();) {
          CommunityMember cm = (CommunityMember)it1.next();
          if (hs.getAgentId().equals(cm.getAgentId())) {
            found = true;
            break;
          }
        }
        if (!found) {
          log.warn("Removed " + hs.getAgentId());
          it.remove();
        }
      }
    }
  }

  /**
   * Query blackboard for HealthReports.  Update corresponding HealthStatus
   * objects.
   */
  private void getHealthReports() {
    // Get new HealthReports.
    Collection reports = bbs.query(healthReportsPredicate);
    for (Iterator it = reports.iterator(); it.hasNext();) {
      HealthReport hr = (HealthReport) it.next();
      HealthStatus hs = getHealthStatus(hr.getSource());
      if (hs != null) {
        if (hr.getCategory() == HealthReport.HEARTBEAT) {
          hs.heartbeatReceived(hr.getTimestamp());
        } else {
          hs.setStatus(hr.getStatus());
          hs.setTimestamp(hr.getTimestamp());
        }
      }
      bbs.openTransaction();
      bbs.publishRemove(hr);
      bbs.closeTransaction();
    }
  }

  private void getPings() {
    // Get Pings
    Collection pings = bbs.query(pingRequestPredicate);
    for (Iterator it = pings.iterator(); it.hasNext();) {
      PingRequest req = (PingRequest)it.next();
      if (req.getSource().equals(myAgent)) {
        int status = req.getStatus();
        HealthStatus hs = getHealthStatus(req.getTarget());
        hs.setPingStatus(status);
        switch (status) {
          case PingRequest.NEW:
            if (log.isDebugEnabled())
              log.debug("Ping status = NEW, ignored.");
            hs.setPingTimestamp(req.getTimeSent());
            break;
          case PingRequest.SENT:
            if (log.isDebugEnabled())
              log.debug("Ping status = SENT, ignored.");
            // The following is used to simulate a Ping timeout.  This will
            // ultimately be done in MTS
            if (elapsedTime(hs.getPingTimestamp(), new Date()) > pingTimeout)
              req.setStatus(PingRequest.FAILED);
            break;
          case PingRequest.RECEIVED:
            if (log.isDebugEnabled())
              log.debug("Ping: agent=" + req.getTarget() +
                      " status=RECEIVED" +
                      " timeSent=" + req.getTimeSent() +
                      " timeReceived=" + req.getTimeReceived() +
                      " roundTripTime=" + req.getRoundTripTime() +
                      " timeSent=" + req.getTimeSent());
            hs.setPingTimestamp(req.getTimeReceived());
            bbs.openTransaction();
            bbs.publishRemove(req);
            bbs.closeTransaction();
            break;
          case PingRequest.FAILED:
            if (log.isDebugEnabled())
              log.debug("Ping: agent=" + req.getTarget() +
                      " status=FAILED" +
                      " timeSent=" + req.getTimeSent() +
                      " timeReceived=" + req.getTimeReceived() +
                      " roundTripTime=" + req.getRoundTripTime() +
                      " timeSent=" + req.getTimeSent());
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

  private void evaluateHealthStatus() {
    Date now = new Date();
    for (Iterator it = membersHealthStatus.iterator(); it.hasNext();) {
      HealthStatus hs = (HealthStatus)it.next();
      if (hs.getStatus() == HealthReport.ALIVE ||
          hs.getStatus() == HealthReport.UNDEFINED) {
        if (elapsedTime(hs.getTimestamp(), now) > heartbeatTimeout) {
          log.error("No response: agent=" + hs.getAgentId());
          hs.setStatus(HealthReport.NO_RESPONSE);
          hs.setPingTimestamp(new Date());
          hs.setPingStatus(PingRequest.NEW);
          doPing(hs.getAgentId());
          log.error("Generating ping: agent=" + hs.getAgentId());
        }
      } else if (hs.getStatus() == HealthReport.NO_RESPONSE) {
        if (hs.getPingStatus() == PingRequest.RECEIVED) {
          hs.setStatus(HealthReport.ALIVE);
          hs.setTimestamp(hs.getPingTimestamp());
          hs.setPingStatus(-1);
          log.error("Reacquired agent: agent=" + hs.getAgentId());
        } else if (hs.getPingStatus() == PingRequest.FAILED) {
          log.error("Ping failed: agent=" + hs.getAgentId());
          log.error("Notifying DecisionPlugin:" +
            " agent=" + hs.getAgentId() +
            " status=NO_RESPONSE");
          hs.setPingStatus(-1);
          resendHeartbeatRequest(hs);
        }
      }
      /*
      if (elapsedTime(hs.lastHeartbeat(), now) > heartbeatTimeout) {
        if (hs.isHeartbeatTimedout()) {
          if (hs.isPingRequested()) {
            if (hs.getPingStatus() == PingRequest.FAILED) {
              log.error("Ping failed: agent=" + hs.getAgentId());
              hs.setPingRequested(false);
              // Requeue a Heartbeat request
              resendHeartbeatRequest(hs);
              log.error("Passing agent to DecisionPlugin: agent=" + hs.getAgentId());
            } else if (hs.getPingStatus() == PingRequest.RECEIVED) {
              hs.setHeartbeatTimedout(false);
              hs.setPingRequested(false);
              log.error("Ping received: agent=" + hs.getAgentId());
              resendHeartbeatRequest(hs);
            }
          }
        } else {
          hs.setHeartbeatTimedout(true);
          log.error("Heartbeat timeout: agent=" + hs.getAgentId());
          //resendHeartbeatRequest(hs);
          hs.setPingRequested(true);
          hs.setPingRequestTime(new Date());
          hs.setPingStatus(PingRequest.NEW);
          doPing(hs.getAgentId());
        }
      } else {
        if (hs.isHeartbeatTimedout()) {
          hs.setHeartbeatTimedout(false);
          hs.setPingRequested(false);
          log.error("Reacquired agent: agent=" + hs.getAgentId());
        }
      }
    */
    }
  }


  /**
   * Utility method for calculating difference between 2 time values.
   * @param start
   * @param end
   * @return
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
          getHealthReports();
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
   * @return
   */
  private boolean hasHealthStatus(ClusterIdentifier agentId) {
    for (Iterator it = membersHealthStatus.iterator(); it.hasNext();) {
      HealthStatus hs = (HealthStatus)it.next();
      if (hs.getAgentId().equals(agentId)) return true;
    }
    return false;
  }

  /**
   * Returns the HealthStatus object associated with a specified agent.
   * @param agentId
   * @return
   */
  private HealthStatus getHealthStatus(MessageAddress agentId) {
    for (Iterator it = membersHealthStatus.iterator(); it.hasNext();) {
      HealthStatus hs = (HealthStatus)it.next();
      if (hs.getAgentId().equals(agentId)) return hs;
    }
    return null;
  }

  /**
   * Use ABA messaging to send HeartbeatRequest objects to monitored agents.
   * @param targets
   */
  private void sendHeartbeatRequest(Collection targets) {
    if (robustnessFactory != null) {
      HeartbeatRequest hbr =
        robustnessFactory.newHeartbeatRequest(heartbeatInterval, myAgent);
      for (Iterator it = targets.iterator(); it.hasNext();) {
        HealthStatus hs = (HealthStatus)it.next();
        hs.setTimestamp(new Date());
        hbr.addTarget(hs.getAgentId());
      }
      if (hbr.getTargets().size() > 0) bbs.publishAdd(hbr);
    }
  }

  /**
   * Use ABA messaging to send HeartbeatRequest objects to agent identified in
   * HealthStatus object
   * @param hs HealthStatus object containing ID of agent
   */
  private void resendHeartbeatRequest(HealthStatus hs) {
    if (robustnessFactory != null) {
      HeartbeatRequest hbr =
        robustnessFactory.newHeartbeatRequest(heartbeatInterval, myAgent);
      hbr.addTarget(hs.getAgentId());
      bbs.openTransaction();
      bbs.publishAdd(hbr);
      bbs.closeTransaction();
    }
  }


  private void doPing(MessageAddress target) {
    PingRequest req = pingFactory.newPingRequest(myAgent, target, pingTimeout);
    log.warn("Sending ping: " + req);
    bbs.openTransaction();
    bbs.publishAdd(req);
    bbs.closeTransaction();
  }


  /**
   * Predicate for HealthReports.
   */
  private UnaryPredicate healthReportsPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof HealthReport);
  }};

  private IncrementalSubscription communityRequests;
  /**
   * Predicate for CommunityRequests.  Used to receive CommunityResponse objects
   * with CommunityRosters.
   */
  private UnaryPredicate communityRequestPredicate = new UnaryPredicate() {
    public boolean execute (Object o) {
      return (o instanceof CommunityRequest);
  }};


  private UnaryPredicate pingRequestPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof PingRequest);
  }};

}
