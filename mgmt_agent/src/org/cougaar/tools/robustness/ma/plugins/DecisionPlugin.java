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
import org.cougaar.core.mobility.AbstractTicket;
import org.cougaar.core.mobility.AddTicket;
import org.cougaar.core.mobility.RemoveTicket;
import org.cougaar.core.mobility.MoveTicket;

import org.cougaar.core.util.UID;

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

  // Unique ID assiciated with this plugin
  private UID myUID;

  // Collection of UIDs associated with my AgentControl objects
  private Collection agentControlUIDs = new Vector();

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

    myUID = getUIDService().nextUID();

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

    // Subscribe to AgentControl objects
    agentControlStatus =
      (IncrementalSubscription) bbs.subscribe(AGENT_CONTROL_PRED);

    // Print informational message defining current parameters
    StringBuffer startMsg = new StringBuffer();
    startMsg.append("DecisionPlugin started: ");
    startMsg.append(" " + paramsToString());
    log.debug(startMsg.toString());
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
    //for (Iterator it = healthStatus.getCollection().iterator();
         it.hasNext();) {
      HealthStatus hs = (HealthStatus)it.next();
      if (hs.getState().equals(HealthStatus.HEALTH_CHECK)) {
        log.debug("Received HEALTH_CHECK for agent " + hs.getAgentId());
        evaluate(hs);
      }
    }

    // Get AgentControl objects
    if (agentControlStatus.hasChanged()) {
      for (Enumeration en = agentControlStatus.getChangedList(); en.hasMoreElements(); ) {
	      AgentControl ac = (AgentControl) en.nextElement();
        AbstractTicket ticket = ac.getAbstractTicket();
        if (ticket instanceof AddTicket) {
          AddTicket addTicket = (AddTicket)ticket;
          HealthStatus hs = getHealthStatus(addTicket.getMobileAgent());
          if (hs != null) {
            hs.setHeartbeatRequestStatus(HealthStatus.UNDEFINED);
            log.debug("Changed mobility status: agent-" + hs.getAgentId() +
              " statusCode=" + ac.getStatusCodeAsString());
            if (ac.getStatusCode() == ac.CREATED) {
              hs.setState(HealthStatus.RESTART_COMPLETE);
              hs.setStatus(HealthStatus.RESTARTED);
              publishChange(hs);
              bbs.publishRemove(ac);
            } else if (ac.getStatusCode() == ac.ALREADY_EXISTS) {
              // Agent is alive but not responding to HeartbeatRequests or Pings
              //hs.setState(HealthStatus.ROBUSTNESS_INIT_FAIL);
              //hs.setStatus(HealthStatus.DEGRADED);
              log.warn("Restart of active agent, action=ADD status=" +
                ac.getStatusCodeAsString() + " agent=" + addTicket.getMobileAgent() +
                " destNode=" + addTicket.getDestinationNode());
              moveAgent(addTicket.getMobileAgent(), addTicket.getDestinationNode());
              //hs.setState(HealthStatus.RESTART_COMPLETE);
              //hs.setStatus(HealthStatus.RESTARTED);
              //publishChange(hs);
              //bbs.publishRemove(ac);
            } else {
              hs.setState(HealthStatus.FAILED_RESTART);
              publishChange(hs);
              bbs.publishRemove(ac);
              log.error("Unexpected status code from mobility, action=ADD status=" +
                ac.getStatusCodeAsString() + " agent=" + addTicket.getMobileAgent() +
                " destNode=" + addTicket.getDestinationNode());
            }
          }
        }
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

          for (Iterator it1 = req.getAgents().iterator(); it1.hasNext();) {
            HealthStatus hs = getHealthStatus((MessageAddress)it1.next());
            if (hs != null) {
              hs.setLastRestartAttempt(new Date());
              hs.setState(HealthStatus.FAILED_RESTART);
              publishChange(hs);
            }
          }
          /*
          log.error("Unable to restart agent(s), no destination node available:" +
            " agents=" + req.getAgents());
          bbs.publishRemove(req);
          */

          // RestartLocator did not return a destination, try to restart agent at
          // its prior location
          HealthStatus hs =
            getHealthStatus((MessageAddress)req.getAgents().iterator().next());
          restartAgents(req.getAgents(), hs.getNode());
          bbs.publishRemove(req);

          break;
        default:
      }
    }
  }

  /**
   * Gets HealthStatus object associated with named agent.
   * @param agentId  MessageAddress of agent
   * @return         Agents HealthStatus object
   */
  private HealthStatus getHealthStatus(MessageAddress agentId) {
    Collection c = bbs.query(healthStatusPredicate);
    for (Iterator it = c.iterator(); it.hasNext();) {
      HealthStatus hs = (HealthStatus)it.next();
      if (hs.getAgentId().equals(agentId)) {
        return hs;
      }
    }
    log.warn("No HealthStatus object found for agent " + agentId);
    return null;
  }

  /**
   * Evaluate the status of agent
   * @param hs
   */
  private void evaluate(HealthStatus hs) {
    int status = hs.getStatus();
    //log.info("Evaluate: agent=" + hs.getAgentId() + " status=" + hs.getStatus());
    switch (status) {
      case HealthStatus.DEAD:
      case HealthStatus.NO_RESPONSE:
        // Agent is most likely dead.  Initiate a restart.
        RestartLocationRequest req =
          new RestartLocationRequest(RestartLocationRequest.LOCATE_NODE, myUID);
        req.addAgent(hs.getAgentId());
        bbs.publishAdd(req);
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
        log.info("Restarting agent: agent(s)=[" +
          agentSetToString(agents) + "], nodeName=" + nodeName);
      }
      for (Iterator it = agents.iterator(); it.hasNext();) {
        MessageAddress agentAddr = (MessageAddress)it.next();

        // Update agents status object
        HealthStatus hs = getHealthStatus(agentAddr);
        hs.setLastRestartAttempt(new Date());
        hs.setState(HealthStatus.RESTART);
        hs.setStatus(HealthStatus.DEAD);
        bbs.publishChange(hs);

        // add the AgentControl request
        addAgent(agentAddr.toString(), nodeName);
      }
    }
  }

  /**
   * Initiates a restart at agents current node.
   * @param agentAddr  MessageAddresses of agent to be restarted
   * @param nodeAddr   MessageAddresses of destination node
   */
  private void moveAgent(MessageAddress agentAddr, MessageAddress nodeAddr) {
    MoveTicket ticket = new MoveTicket(
      mobilityFactory.createTicketIdentifier(),
        agentAddr,
        nodeAddr,
        nodeAddr,
        true);
    UID acUID = getUIDService().nextUID();
    AgentControl ac =
      mobilityFactory.createAgentControl(acUID, agentAddr, ticket);
    bbs.publishAdd(ac);
  }

  /**
   * Kills an agent.
   * @param agent  MessageAddress of agent to be killed
   */
  private void killAgent(MessageAddress agent, MessageAddress node) {
    Object ticketId = mobilityFactory.createTicketIdentifier();
    RemoveTicket removeTicket =
      new RemoveTicket(ticketId, agent, node);

    UID acUID = getUIDService().nextUID();
    agentControlUIDs.add(acUID);
    AgentControl ac =
      mobilityFactory.createAgentControl(acUID, node, removeTicket);

    if (log.isDebugEnabled())
      log.debug("Publishing AgentControl(RemoveTicket) for mobility: " + ac);
    bbs.publishAdd(ac);
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

    UID acUID = getUIDService().nextUID();
    agentControlUIDs.add(acUID);
    AgentControl ac =
      mobilityFactory.createAgentControl(acUID, destNodeAddr, addTicket);

    if (log.isDebugEnabled())
      log.debug("Publishing AgentControl(AddTicket) for mobility: " + ac);
    bbs.publishAdd(ac);
  }

  /**
   * Increase the HeartbeatFailureRateThreshold by specified value.
   * @param hs    HealthStatus object associated with monitored agent
   * @param value Adjustment value
   */
  private void adjustHbSensitivity(HealthStatus hs, float value) {
    float hbFailureRateThreshold = hs.getHbFailRateThreshold();
    hbFailureRateThreshold = hbFailureRateThreshold * (1.0f + value);
    hs.setHbFailRateThreshold(hbFailureRateThreshold);
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
	    if (o instanceof AgentControl) {
        AgentControl ac = (AgentControl)o;
        return (agentControlUIDs.contains(ac.getOwnerUID()));
      }
      return false;
  }};


 /**
  * Predicate for HealthStatus objects
  */
  private IncrementalSubscription healthStatus;
  private UnaryPredicate healthStatusPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof HealthStatus);
  }};

  /**
   * Predicate for RestartLocationRequest objects
   */
  private IncrementalSubscription restartRequests;
  private UnaryPredicate restartRequestPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof RestartLocationRequest) {
        RestartLocationRequest rlr = (RestartLocationRequest)o;
        return (myUID.equals(rlr.getOwnerUID()));
      }
      return false;
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
