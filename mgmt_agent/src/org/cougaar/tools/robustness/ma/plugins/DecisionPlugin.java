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

import org.cougaar.robustness.restart.plugin.*;
import java.util.*;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.SimplePlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.mts.MessageAddress;

import org.cougaar.util.UnaryPredicate;

/**
 * This plugin determines an appropriate course of action to fix agents that
 * have been placed into a HEALTH_CHECK state by the HealthMonitor plugin.  In
 * cases where the monitored agent is determined to be dead this plugin will
 * initiate a restart.
 */
public class DecisionPlugin extends SimplePlugin {

  private LoggingService log;
  private BlackboardService bbs = null;

  // Defines default values for configurable parameters.
  private static String defaultParams[][] = {
    {}
  };
  ManagementAgentProperties decisionProps =
    ManagementAgentProperties.makeProps(this.getClass().getName(), defaultParams);

  protected void setupSubscriptions() {

    log =  (LoggingService) getBindingSite().getServiceBroker().
      getService(this, LoggingService.class, null);

    bbs = getBlackboardService();

    // Initialize configurable paramaeters from defaults and plugin arguments.
    updateParams(decisionProps);
    bbs.publishAdd(decisionProps);

    // Subscribe to ManagementAgentProperties to receive parameter changes
    mgmtAgentProps =
      (IncrementalSubscription)bbs.subscribe(propertiesPredicate);

    // Subscribe to HeartbeatStatus objects to receive notification of
    // agents that have been placed into the "HEALTH_CHECK" state
    healthStatus =
      (IncrementalSubscription)bbs.subscribe(healthStatusPredicate);

    // Subscribe to RestartLocationRequest objects
    restartRequests =
      (IncrementalSubscription)bbs.subscribe(restartRequestPredicate);

    // Subscribe to AgentStart objects
    agentStartStatus =
      (IncrementalSubscription)bbs.subscribe(agentStartStatusPredicate);

  }

  public void execute() {

    // Get Parameter changes
    for (Iterator it = mgmtAgentProps.getAddedCollection().iterator();
         it.hasNext();) {
      ManagementAgentProperties props = (ManagementAgentProperties)it.next();
      //updateParams(props);
    }

     // Get HealthStatus objects
    for (Iterator it = healthStatus.getChangedCollection().iterator();
         it.hasNext();) {
      HealthStatus hs = (HealthStatus)it.next();
      log.debug("Received HEALTH_CHECK for agent " + hs.getAgentId());
      evaluate(hs);
    }

     // Get AgentStart objects
    for (Iterator it = agentStartStatus.getChangedCollection().iterator();
         it.hasNext();) {
      AgentStart action = (AgentStart)it.next();
      /*
      String statusStr = null;
      switch (action.getStatus()) {
        case Action.FAIL:
          statusStr = "FAIL";
          break;
        case Action.SUCCESS:
          statusStr = "SUCCESS";
          break;
        default:
      }
      */
      //log.debug("Received AgentStart response, restart result= " + action.status);
      log.debug("Received AgentStart response, restart result= ");
      bbs.publishRemove(action);
    }

    // Get RestartLocationRequests
    for (Iterator it = restartRequests.getChangedCollection().iterator();
         it.hasNext();) {
      RestartLocationRequest req = (RestartLocationRequest)it.next();
      int status = req.getStatus();
      switch (status) {
        case RestartLocationRequest.SUCCESS:
          String nodeName = "EmptyNode";
          Iterator nodeIt = req.getAgents().iterator();
          if (nodeIt.hasNext()) nodeName = getNodeName((MessageAddress)nodeIt.next());
          restartAgents(req.getAgents(), nodeName);
          //restartNode(nodeName, req.getHost());
          bbs.publishRemove(req);
          break;
        case RestartLocationRequest.FAIL:
          log.error("RestartLocationRequest failed");
          bbs.publishRemove(req);
          break;
        default:
      }
    }
  }

  /**
   * Retrieves the name of the node that the specified agent was running on.
   */
  private String getNodeName(MessageAddress agent) {
    // Hard-coded to "EmptyNode" for now.  Will need to get the name for the
    // TopologyService at some point.
    return "EmptyNode";
  }

  /**
   * Evaluate the status of agent
   * @param hs
   */
  private void evaluate(HealthStatus hs) {
    int status = hs.getStatus();
    switch (status) {
      case HealthStatus.NO_RESPONSE:
        // Agent is most likely dead.  Initiate a restart.
        RestartLocationRequest req = new RestartLocationRequest();
        req.addAgent(hs.getAgentId());
        bbs.publishAdd(req);
        hs.setState(HealthStatus.RESTART);
        bbs.publishChange(hs);
        break;
      case HealthStatus.DEGRADED:
        // Agent is alive but operating under stress.  For now just increase
        // the Heartbeat failure rate threshold.  Eventually this should
        // include logic to determine if the agent is simply busy or if there
        // is a hardware problem or external attack.
        adjustHbSensitivity(hs, 0.1f);  // Increase threshold by 10%
        hs.setState(HealthStatus.NORMAL);
        bbs.publishChange(hs);
        break;
      default:
    }
  }

  /**
   * Initiates a restart.
   * @param agents  Set of MessageAddresses of agents to be restarted
   * @param host    Name of restart host
   */
  private void restartAgents(Set agents, String nodeName) {
    if (log.isInfoEnabled()) {
      StringBuffer msg = new StringBuffer("Initiating agent restart: agent(s)=[");
      for (Iterator it = agents.iterator(); it.hasNext();) {
        msg.append(((MessageAddress)it.next()).toString());
        if (it.hasNext()) msg.append(" ");
      }
      msg.append("], nodeName=" + nodeName);
      log.info(msg.toString());
    }
    for (Iterator it = agents.iterator(); it.hasNext();) {
      String agentName = ((MessageAddress)it.next()).toString();
      AgentStart as = new AgentStart(nodeName, agentName);
      bbs.publishAdd(as);
    }
  }

  /**
   * Initiates a restart.
   * @param agents  Set of MessageAddresses of agents to be restarted
   * @param host    Name of restart host
   */
  private void restartNode(String nodeName, String hostName) {
    if (log.isInfoEnabled()) {
      StringBuffer msg = new StringBuffer("Initiating node restart: ");
      msg.append("hostName=" + hostName);
      msg.append(", nodeName=" + nodeName);
      log.info(msg.toString());
    }
    NodeStart nodeStart = new NodeStart(hostName, nodeName);
    bbs.publishAdd(nodeStart);
  }

  /**
   * Increase the HeartbeatFailureRateThreshold by specified value.
   * @param hs    HealthStatus object associated with monitored agent
   * @param value Adjustment value
   */
  private void adjustHbSensitivity(HealthStatus hs, float value) {
    float hbFailureRateThreshold = hs.getHbFailureRateThreshold();
    hbFailureRateThreshold = hbFailureRateThreshold * (1.0f + value);
    hs.setHbFailureRateThreshold(hbFailureRateThreshold);
    if (log.isInfoEnabled()) {
      StringBuffer msg = new StringBuffer("Adjusting heartbeat sensitivity: ");
      msg.append("agent=" + hs.getAgentId());
      msg.append(", hbFailureRateThreshold=" + hbFailureRateThreshold);
      log.info(msg.toString());
    }
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
      decisionProps.setProperty(name, value);
    }
  }

  /**
   * Sets externally configurable parameters using supplied Properties object.
   * @param props Propertie object defining paramater names and values.
   */
  private void updateParams(Properties props) {
    // Nothing defined for now
  }

 /**
  * Predicate for AgentStart objects
  */
  private IncrementalSubscription agentStartStatus;
  private UnaryPredicate agentStartStatusPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof AgentStart);
  }};

 /**
  * Predicate for HealthStatus objects
  */
  private IncrementalSubscription healthStatus;
  private UnaryPredicate healthStatusPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof HealthStatus) {
        HealthStatus hs = (HealthStatus)o;
        return hs.getState().equals(HealthStatus.HEALTH_CHECK);
      }
      return false;
  }};

  /**
   * Predicate for RestartLocationRequest objects
   */
  private IncrementalSubscription restartRequests;
  private UnaryPredicate restartRequestPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof RestartLocationRequest);
  }};

  /**
   * Predicate for Management Agent properties
   */
  private IncrementalSubscription mgmtAgentProps;
  private UnaryPredicate propertiesPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof ManagementAgentProperties) {
        ManagementAgentProperties props = (ManagementAgentProperties)o;
        String myName = this.getClass().getName();
        String forName = props.getPluginName();
        return (myName.equals(forName) || myName.endsWith(forName));
      }
      return false;
  }};
}