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
//import org.cougaar.core.plugin.SimplePlugin;
import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.mts.MessageAddress;
//import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.mts.SimpleMessageAddress;

import org.cougaar.core.mobility.ldm.*;
import org.cougaar.core.mobility.AbstractTicket;
import org.cougaar.core.mobility.AddTicket;
import org.cougaar.core.mobility.RemoveTicket;
import org.cougaar.core.mobility.MoveTicket;

import org.cougaar.core.service.AlarmService;
import org.cougaar.core.agent.service.alarm.Alarm;

import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.service.wp.AddressEntry;

import org.cougaar.core.util.UID;

import org.cougaar.core.service.EventService;

import org.cougaar.util.UnaryPredicate;

/**
 * This plugin determines an appropriate course of action to fix agents that
 * have been placed into a HEALTH_CHECK state by the HealthMonitor plugin.  In
 * cases where the monitored agent is determined to be dead this plugin will
 * initiate a restart.
 */
public class DecisionPlugin extends SimplePlugin {

  // Defines default values for configurable parameters.
  private static String defaultParams[][] = {
    {"maxConcurrentRestarts", "1"},
    {"restartTimerInterval",  "2000"}
  };
  ManagementAgentProperties decisionProps =
    ManagementAgentProperties.makeProps(this.getClass().getName(), defaultParams);

  private LoggingService log;
  private BlackboardService bbs = null;

  // Unique ID assiciated with this plugin
  private UID myUID;

  // List of agents waiting to be restarted
  private List restartQueue = new Vector();

  private EventService eventService;

  private List restartsInProcess = new Vector();

  // Timer for periodically checking for restart queue
  private RestartTimer restartTimer;
  private long restartTimerInterval = 2000; // 2 second interval

  private int maxConcurrentRestarts = 1;

  // Collection of UIDs associated with my AgentControl objects
  private Collection agentControlUIDs = new Vector();

  protected MobilityFactory mobilityFactory;

  private WhitePagesService wps;

  IncrementalSubscription sub;

  private MessageAddress agentID;

  protected void setupSubscriptions() {

    agentID = this.getAgentIdentifier();

    myUID = getUIDService().nextUID();

    log =  (LoggingService) getBindingSite().getServiceBroker().
      getService(this, LoggingService.class, null);

    DomainService domainService =
      (DomainService) getBindingSite().getServiceBroker().
      getService(this, DomainService.class, null);

    eventService =
      (EventService) getBindingSite().getServiceBroker().
      getService(this, EventService.class, null);

    wps = (WhitePagesService)getBindingSite().getServiceBroker().
        getService(this, WhitePagesService.class, null);

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

    // Start restart timer
    getAlarmService().addRealTimeAlarm(new RestartTimer(restartTimerInterval));

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
      //ManagementAgentProperties props = (ManagementAgentProperties)it.next();
      //updateParams(props);
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
            //log.debug("Changed mobility status: agent-" + hs.getAgentId() +
            //  " statusCode=" + ac.getStatusCodeAsString());
            if (ac.getStatusCode() == ac.CREATED) {
              hs.setState(HealthStatus.RESTART_COMPLETE);
              hs.setStatus(HealthStatus.RESTARTED);
              hs.setNode(addTicket.getDestinationNode().toString());
              publishChange(hs);
              bbs.publishRemove(ac);
            } else if (ac.getStatusCode() == ac.ALREADY_EXISTS) {
              // Mobility thinks the agent is alive
              // We think its dead because its not responding to HeartbeatRequests or Pings

              log.warn("Restart of (possibly) active agent attempted, action=ADD status=" +
                ac.getStatusCodeAsString() + " agent=" + addTicket.getMobileAgent() +
                " destNode=" + addTicket.getDestinationNode());
              // Try a forced restart
              /*
                moveAgent(addTicket.getMobileAgent(), addTicket.getDestinationNode());
              */
              hs.setState(HealthStatus.FAILED_RESTART);
              publishChange(hs);
              bbs.publishRemove(ac);
            } else {
              hs.setState(HealthStatus.FAILED_RESTART);
              publishChange(hs);
              bbs.publishRemove(ac);
              log.error("Unexpected status code from mobility, action=ADD status=" +
                ac.getStatusCodeAsString() + " agent=" + addTicket.getMobileAgent() +
                " destNode=" + addTicket.getDestinationNode());
            }
            // Remove agent from list of pending restarts
            restartsInProcess.remove(addTicket.getMobileAgent());
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
          for (Iterator agentsToRestart = req.getAgents().iterator(); agentsToRestart.hasNext();) {
            restartAgent((MessageAddress)agentsToRestart.next(), req.getNode());
          }
          bbs.publishRemove(req);
          break;
        case RestartLocationRequest.FAIL:

          for (Iterator it1 = req.getAgents().iterator(); it1.hasNext();) {
            HealthStatus hs = getHealthStatus((MessageAddress)it1.next());
            if (hs != null) {
              hs.setState(HealthStatus.FAILED_RESTART);
              publishChange(hs);
            }
          }

          log.error("Unable to restart agent(s), no destination node available:" +
            " agents=" + req.getAgents());
          bbs.publishRemove(req);


          // RestartLocator did not return a destination, try to restart agent at
          // its prior location
          /*
          HealthStatus hs =
            getHealthStatus((MessageAddress)req.getAgents().iterator().next());
          restartAgents(req.getAgents(), hs.getNode());
          bbs.publishRemove(req);
          */

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
        // Agent is most likely dead.  A restart is required.
        queueAgentForRestart(hs.getAgentId());
        break;
      case HealthStatus.DEGRADED:
        // Agent is alive but operating under stress.  For now just increase
        // the Heartbeat failure rate threshold.  Eventually this should
        // include logic to determine if the agent is simply busy or if there
        // is a hardware problem or external attack.
        adjustHbSensitivity(hs, 10.0f);  // Increase threshold by 10%
        hs.setState(HealthStatus.NORMAL);
        bbs.publishChange(hs);
        break;
      default:
    }
  }

  /**
   * Sends Cougaar event via EventService.
   */
  private void event(String message) {
    if (eventService != null && eventService.isEventEnabled())
      eventService.event(message);
  }

  /**
   * Initiates a forced restart at agents current node.
   * @param agentAddr  MessageAddresses of agent to be restarted
   * @param nodeAddr   MessageAddresses of destination node
   */
  private void moveAgent(MessageAddress agentAddr, MessageAddress nodeAddr) {
    log.info("Performing forced restart, agent=" + agentAddr);
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

  protected void queueAgentForRestart(MessageAddress agent) {
    synchronized (restartQueue) {
      restartQueue.add(agent);
    }
  }

  protected void restartAgent(MessageAddress agent, String destNode) {

    MessageAddress destNodeAddr = null;
    if (destNode != null) {
      //destNodeAddr = new MessageAddress(destNode); //change in cougaar 10.0
      destNodeAddr = SimpleMessageAddress.getSimpleMessageAddress(destNode);
    }
    Object ticketId = mobilityFactory.createTicketIdentifier();
    AddTicket addTicket = new AddTicket(ticketId, agent, destNodeAddr);

    UID acUID = getUIDService().nextUID();
    agentControlUIDs.add(acUID);
    AgentControl ac =
      mobilityFactory.createAgentControl(acUID, destNodeAddr, addTicket);

    event("Restarting agent: agent=" + agent + " dest=" + destNodeAddr);
    if (log.isDebugEnabled()) {
      StringBuffer sb = new StringBuffer("AgentControl publication:" +
        " myUid=" + agentControlUIDs.contains(ac.getOwnerUID()) +
        " status=" + ac.getStatusCodeAsString());
      if (ac.getAbstractTicket() instanceof AddTicket) {
        AddTicket at = (AddTicket)ac.getAbstractTicket();
        sb.append(" agent=" + at.getMobileAgent() +
          " destNode=" + at.getDestinationNode());
      }
      log.debug(sb.toString());
    }
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
    maxConcurrentRestarts = Integer.parseInt(props.getProperty("maxConcurrentRestarts"));
    restartTimerInterval = Long.parseLong(props.getProperty("restartTimerInterval"));
  }

 /**
  * Predicate for AgentControl objects
  */
  private IncrementalSubscription agentControlStatus;
  protected UnaryPredicate AGENT_CONTROL_PRED = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof AgentControl) {
        AgentControl ac = (AgentControl)o;
        if (log.isDebugEnabled()) {
          StringBuffer sb = new StringBuffer("AgentControl subscription:" +
            " myUid=" + agentControlUIDs.contains(ac.getOwnerUID()) +
            " status=" + ac.getStatusCodeAsString());
          if (ac.getAbstractTicket() instanceof AddTicket) {
            AddTicket at = (AddTicket)ac.getAbstractTicket();
            sb.append(" agent=" + at.getMobileAgent() +
            " destNode=" + at.getDestinationNode());
          } else {
            sb.append(" ticket=" + ac.getAbstractTicket().getClass().getName());
          }
          log.debug(sb.toString());
        }
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

  /**
   * Periodically look at restart queue and pending restart list to see if any
   * agents need to be restarted.
   */
  private class RestartTimer implements Alarm {
    private long expirationTime = -1;
    private boolean expired = false;

    /**
     * Create an Alarm to go off in the milliseconds specified,.
     **/
    public RestartTimer (long delay) {
      expirationTime = delay + System.currentTimeMillis();
    }

    /** @return absolute time (in milliseconds) that the Alarm should
     * go off.
     **/
    public long getExpirationTime () {
      return expirationTime;
    }

    /**
     * Called by the cluster clock when clock-time >= getExpirationTime().
     **/
    public void expire () {
      if (!expired) {
        try {
          synchronized (restartQueue) {
            if (!restartQueue.isEmpty() && restartsInProcess.size() < maxConcurrentRestarts) {
              MessageAddress agent = (MessageAddress)restartQueue.remove(0);
              restartsInProcess.add(agent);
              bbs.openTransaction();
              HealthStatus hs = getHealthStatus(agent);
              hs.setLastRestartAttempt(new Date());
              hs.setState(HealthStatus.RESTART);

              RestartLocationRequest req =
                new RestartLocationRequest(RestartLocationRequest.LOCATE_NODE, myUID);
              Collection excludedNodes = new ArrayList();
              excludedNodes.add(hs.getNode());
              excludedNodes.add(getLocation(agentID.toString()));
              req.setExcludedNodes(excludedNodes);
              req.addAgent(agent);
              bbs.publishChange(hs);
              bbs.publishAdd(req);
              bbs.closeTransaction();
            }
          }
          getAlarmService().addRealTimeAlarm(new RestartTimer(restartTimerInterval));
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
         expired = true;
        }
      }
    }

    /** @return true IFF the alarm has expired or was canceled. **/
    public boolean hasExpired () {
      return expired;
    }

    /** can be called by a client to cancel the alarm.  May or may not remove
     * the alarm from the queue, but should prevent expire from doing anything.
     * @return false IF the the alarm has already expired or was already canceled.
     **/
    public synchronized boolean cancel () {
      if (!expired)
        return expired = true;
      return false;
    }
  }

}
