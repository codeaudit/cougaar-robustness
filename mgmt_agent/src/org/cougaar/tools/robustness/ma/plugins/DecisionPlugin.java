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
import org.cougaar.tools.robustness.ma.ldm.RestartLocationRequest;
import java.util.*;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.SimplePlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.agent.ClusterIdentifier;

import org.cougaar.core.mobility.ldm.*;
import org.cougaar.core.mobility.AddTicket;

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
  private static String defaultParams[][] = new String[0][0];

  ManagementAgentProperties decisionProps =
    ManagementAgentProperties.makeProps(this.getClass().getName(), defaultParams);

  protected MobilityFactory mobilityFactory;

  IncrementalSubscription sub;

  /*
  public void setDomainService(DomainService domain) {
    log.info("setDomainService");
    this.domain = domain;
    mobilityFactory = (MobilityFactory) domain.getFactory("mobility");
  }
  */

  protected void setupSubscriptions() {
    log =  (LoggingService) getBindingSite().getServiceBroker().
      getService(this, LoggingService.class, null);

    DomainService domainService =
      (DomainService) getBindingSite().getServiceBroker().
      getService(this, DomainService.class, null);

    mobilityFactory = (MobilityFactory) domainService.getFactory("mobility");
    if (mobilityFactory == null) {
      log.error("Mobility factory (and domain) not enabled");
      //throw new RuntimeException("Mobility factory (and domain) not enabled");
    }

    bbs = getBlackboardService();

    sub = (IncrementalSubscription) bbs.subscribe(AGENT_CONTROL_PRED);
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

    // Subscribe to AgentControl objects
    agentControlStatus =
      (IncrementalSubscription) bbs.subscribe(AGENT_CONTROL_PRED);

    // Print informational message defining current parameters
    StringBuffer startMsg = new StringBuffer();
    startMsg.append("DecisionPlugin started: ");
    startMsg.append(" " + paramsToString());
    log.info(startMsg.toString());
  }

  public void execute() {

    // Get Parameter changes
    for (Iterator it = mgmtAgentProps.getChangedCollection().iterator();
         it.hasNext();) {
      ManagementAgentProperties props = (ManagementAgentProperties)it.next();
      updateParams(props);
      log.info("Parameters modified: " + paramsToString());
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
      String statusStr = null;
      switch (action.getStatus()) {
        case AgentAction.FAIL:
          statusStr = "FAIL";
          break;
        case AgentAction.SUCCESS:
          statusStr = "SUCCESS";
          break;
        default:
      }
      log.debug("Received AgentStart response, restart result= " + statusStr);
      bbs.publishRemove(action);
    }

    // Get AgentControl objects
    if (agentControlStatus.hasChanged()) {
      for (Enumeration en = agentControlStatus.getAddedList(); en.hasMoreElements(); ) {
	      AgentControl ac = (AgentControl) en.nextElement();
        if (log.isDebugEnabled()) log.debug("ADDED " + ac);
      }
      for (Enumeration en = agentControlStatus.getChangedList(); en.hasMoreElements(); ) {
	      AgentControl ac = (AgentControl) en.nextElement();
        if (log.isDebugEnabled()) log.debug("CHANGED " + ac);
      }
      for (Enumeration en = agentControlStatus.getRemovedList(); en.hasMoreElements(); ) {
	      AgentControl ac = (AgentControl) en.nextElement();
        if (log.isDebugEnabled()) log.debug("REMOVED " + ac);
      }
    }

    // Get RestartLocationRequests
    for (Iterator it = restartRequests.getChangedCollection().iterator();
         it.hasNext();) {
      RestartLocationRequest req = (RestartLocationRequest)it.next();
      int status = req.getStatus();
      switch (status) {
        case RestartLocationRequest.SUCCESS:
          restartAgents(req.getAgents(), req.getNode());
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
   * Evaluate the status of agent
   * @param hs
   */
  private void evaluate(HealthStatus hs) {
    int status = hs.getStatus();
    switch (status) {
      case HealthStatus.NO_RESPONSE:
        // Agent is most likely dead.  Initiate a restart.
        RestartLocationRequest req =
          new RestartLocationRequest(RestartLocationRequest.LOCATE_NODE);
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
   * Returns a string representation of a Set of agent addresses.
   * @param agents Set of agent addresses
   * @return String of agent names
   */
  private String agentSetToString(Set agents) {
    StringBuffer sb = new StringBuffer();
    if (agents != null) {
      for (Iterator it = agents.iterator(); it.hasNext();) {
        sb.append(((MessageAddress)it.next()).toString());
        if (it.hasNext()) sb.append(" ");
      }
    }
    return sb.toString();
  }

  /**
   * Initiates a restart.
   * @param agents  Set of MessageAddresses of agents to be restarted
   * @param host    Name of restart host
   */
  private void restartAgents(Set agents, String nodeName) {
    if (nodeName == null) {
      log.warn("Unable to perform restart, no node selected: agents=[" +
        agentSetToString(agents) + "]");
    } else {
      if (log.isInfoEnabled()) {
        log.info("Initiating agent restart: agent(s)=[" +
          agentSetToString(agents) + "], nodeName=" + nodeName);
      }
      for (Iterator it = agents.iterator(); it.hasNext();) {
        String agentName = ((MessageAddress)it.next()).toString();
        //AgentStart as = new AgentStart(nodeName, agentName);
        //bbs.publishAdd(as);

        // add the AgentControl request
        addAgent(agentName, nodeName);
      }
    }
  }

  protected void addAgent(String newAgent, String destNode) {

    MessageAddress newAgentAddr = null;
    MessageAddress destNodeAddr = null;
    if (newAgent != null) {
      newAgentAddr = new ClusterIdentifier(newAgent);
    }
    if (destNode != null) {
      destNodeAddr = new MessageAddress(destNode);
    }
    Object ticketId = mobilityFactory.createTicketIdentifier();
    AddTicket addTicket = new AddTicket(ticketId, newAgentAddr, destNodeAddr);

    AgentControl ac =
      mobilityFactory.createAgentControl(null, destNodeAddr, addTicket);

    if (log.isDebugEnabled()) log.debug("CREATED " + ac);
    bbs.publishAdd(ac);
  }

  /**
   * Increase the HeartbeatFailureRateThreshold by specified value.
   * @param hs    HealthStatus object associated with monitored agent
   * @param value Adjustment value
   */
  private void adjustHbSensitivity(HealthStatus hs, float value) {
    float hbFailureRateThreshold = hs.getHbFailRate();
    hbFailureRateThreshold = hbFailureRateThreshold * (1.0f + value);
    hs.setHbFailRate(hbFailureRateThreshold);
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
   * Creates a printable representation of current parameters.
   * @return  Text string of current parameters
   */
  private String paramsToString() {
    StringBuffer sb = new StringBuffer();
    for (Enumeration enum = decisionProps.propertyNames(); enum.hasMoreElements();) {
      String propName = (String)enum.nextElement();
      sb.append(propName + "=" +
        decisionProps.getProperty(propName) + " ");
    }
    return sb.toString();
  }

  /**
   * Sets externally configurable parameters using supplied Properties object.
   * @param props Propertie object defining paramater names and values.
   */
  private void updateParams(Properties props) {
     // None for now
  }

 /**
  * Predicate for AgentControl objects
  */
  private IncrementalSubscription agentControlStatus;
  protected UnaryPredicate AGENT_CONTROL_PRED = new UnaryPredicate() {
	  public boolean execute(Object o) {
	    return (o instanceof AgentControl);
  }};

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