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
package org.cougaar.tools.robustness.ma.util;

import org.cougaar.tools.robustness.ma.ldm.HealthMonitorRequest;
import org.cougaar.tools.robustness.ma.ldm.HealthMonitorRequestImpl;
import org.cougaar.tools.robustness.ma.ldm.RelayAdapter;

import org.cougaar.core.blackboard.BlackboardClientComponent;
import org.cougaar.core.blackboard.IncrementalSubscription;

import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.EventService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.SchedulerService;
import org.cougaar.core.service.UIDService;

import org.cougaar.core.component.BindingSite;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.SimpleMessageAddress;

import org.cougaar.core.mobility.AbstractTicket;
import org.cougaar.core.mobility.AddTicket;
import org.cougaar.core.mobility.RemoveTicket;
import org.cougaar.core.mobility.ldm.AgentControl;
import org.cougaar.core.mobility.ldm.MobilityFactory;

import org.cougaar.core.service.AlarmService;
import org.cougaar.core.agent.service.alarm.Alarm;

import org.cougaar.core.util.UID;

import org.cougaar.util.UnaryPredicate;

import java.util.ArrayList;
import java.util.Date;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

/**
 * Provides convenience methods for invoking restarts on local and remote
 * nodes.
 */
public class RestartHelper extends BlackboardClientComponent {

  // Status codes returned to listeners
  public static final int SUCCESS = 0;
  public static final int FAIL = 1;

  public static final long TIMER_INTERVAL = 10 * 1000;
  public static final long ACTION_TIMEOUT = 30 * 60 * 1000;
  public static final long MAX_CONCURRENT_ACTIONS = 5;

  private List localActionQueue = Collections.synchronizedList(new ArrayList());
  private List remoteRequestQueue = new ArrayList();
  private Map actionsInProcess = Collections.synchronizedMap(new HashMap());

  private WakeAlarm wakeAlarm;

  private Set myUIDs = new HashSet();
  private MobilityFactory mobilityFactory;
  private LoggingService logger;
  private UIDService uidService = null;
  protected EventService eventService;
  private List listeners = new ArrayList();

  // For interaction with Mobility
  private IncrementalSubscription agentControlSub;
  private UnaryPredicate agentControlPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof AgentControl) {
        AgentControl ac = (AgentControl)o;
        return (myUIDs.contains(ac.getOwnerUID()));
      } else {
        return false;
      }
    }
  };

  // For receiving Relays from remote nodes
  private IncrementalSubscription healthMonitorRequests;
  private UnaryPredicate healthMonitorRequestPredicate = new UnaryPredicate() {
    public boolean execute (Object o) {
      if (o instanceof HealthMonitorRequest) {
        HealthMonitorRequest hmr = (HealthMonitorRequest)o;
        return (hmr.getRequestType() == hmr.RESTART ||
                hmr.getRequestType() == hmr.KILL ||
                hmr.getRequestType() == hmr.ADD);
      }
      return false;
  }};

  public RestartHelper(BindingSite bs) {
    this.setBindingSite(bs);
    initialize();
    load();
    start();
  }

  /**
   * Load required services.
   */
  public void load() {
    setAgentIdentificationService(
      (AgentIdentificationService)getServiceBroker().getService(this, AgentIdentificationService.class, null));
    setAlarmService(
      (AlarmService)getServiceBroker().getService(this, AlarmService.class, null));
    setSchedulerService(
      (SchedulerService)getServiceBroker().getService(this, SchedulerService.class, null));
    setBlackboardService(
      (BlackboardService)getServiceBroker().getService(this, BlackboardService.class, null));
    eventService = (EventService) getServiceBroker().getService(this, EventService.class, null);
    logger =
      (LoggingService)getBindingSite().getServiceBroker().getService(this, LoggingService.class, null);
    logger = org.cougaar.core.logging.LoggingServiceWithPrefix.add(logger, agentId + ": ");
    DomainService ds =
        (DomainService) getServiceBroker().getService(this, DomainService.class, null);
    mobilityFactory = (MobilityFactory) ds.getFactory("mobility");
    uidService = (UIDService) getServiceBroker().getService(this, UIDService.class, null);
    super.load();
  }

  /**
   * Subscribe to HealthMonitorRequest relays and mobility objects.
   */
  public void setupSubscriptions() {
    agentControlSub =
        (IncrementalSubscription)blackboard.subscribe(agentControlPredicate);
    healthMonitorRequests =
        (IncrementalSubscription)blackboard.subscribe(healthMonitorRequestPredicate);
    // Start timer to periodically check RestartQueue
    wakeAlarm = new WakeAlarm((new Date()).getTime() + TIMER_INTERVAL);
    alarmService.addRealTimeAlarm(wakeAlarm);
  }

  public void execute() {

    // Remove failed actions
    if (!actionsInProcess.isEmpty()) {
      removeExpiredActions();
    }

    // Perform local restarts
    if (!localActionQueue.isEmpty()) {
      doNextAction();
    }

    // Forward non-local restarts to remote agent
    fireAll();

    // Get AgentControl objects
    for (Iterator it = agentControlSub.iterator(); it.hasNext();) {
      update(it.next());
    }

    for (Iterator it = healthMonitorRequests.getAddedCollection().iterator(); it.hasNext(); ) {
      HealthMonitorRequest hsm = (HealthMonitorRequest) it.next();
      if (logger.isDebugEnabled()) {
        logger.debug("Received " + hsm);
      }
      doAction(hsm);
    }
  }

  /**
   * Method used by clients to initiate a restart.  The destination node is
   * responsible for interaction with Mobility to perform the restart.  If the
   * local agent is not the destination a request is sent to the destination
   * via a blackboard Relay.
   * @param agentName      Name of agent to be restarted
   * @param origNode       Origin node
   * @param destNode       Destination node
   * @param communityName  Robustness community name
   */
  public void restartAgent(String agentName,
                           String origNode,
                           String destNode,
                           String communityName) {
    if (logger.isDebugEnabled()) {
      logger.debug("RestartAgent:" +
                   " destNode=" + destNode +
                   " agent=" + agentName);
    }
    if (agentId.toString().equals(destNode)) {
      // Restart locally
      doLocalAction(agentName, HealthMonitorRequest.RESTART);
    } else {
      // Queue request to remote agent
      fireLater(new RemoteRequest(new String[]{agentName},
                                  HealthMonitorRequest.RESTART,
                                  origNode,
                                  destNode,
                                  communityName));
    }
  }

  /**
   * Method used by clients to kill an agent.
   * @param agentName      Name of agent to be killed
   * @param currentNode    Current node
   */
  public void killAgent(String agentName,
                        String currentNode,
                        String communityName) {
    if (logger.isDebugEnabled()) {
      logger.debug("KillAgent:" +
                   " agent=" + agentName +
                   " currentNode=" + currentNode);
    }
    if (agentId.toString().equals(currentNode)) {
      // Kill local agent
      doLocalAction(agentName, HealthMonitorRequest.KILL);
    } else {
      // Queue request to remote agent
      fireLater(new RemoteRequest(new String[]{agentName},
                                  HealthMonitorRequest.KILL,
                                  null,
                                  currentNode,
                                  communityName));
    }
  }

  /**
   * Method used by clients to add an agent.
   * @param agentName      Name of agent to be added
   * @param destNode       Destination node
   */
  public void addAgent(String agentName,
                        String destNode,
                        String communityName) {
    if (logger.isDebugEnabled()) {
      logger.debug("AddAgent:" +
                   " agent=" + agentName +
                   " destNode=" + destNode);
    }
    if (agentId.toString().equals(destNode)) {
      // Add agent locally
      doLocalAction(agentName, HealthMonitorRequest.ADD);
    } else {
      // Queue request to remote agent
      fireLater(new RemoteRequest(new String[]{agentName},
                                  HealthMonitorRequest.ADD,
                                  null,
                                  destNode,
                                  communityName));
    }
  }

  protected void fireLater(RemoteRequest rr) {
    synchronized (remoteRequestQueue) {
      remoteRequestQueue.add(rr);
    }
    if (blackboard != null) {
      blackboard.signalClientActivity();
    }
  }

  private void fireAll() {
    int n;
    List l;
    synchronized (remoteRequestQueue) {
      n = remoteRequestQueue.size();
      if (n <= 0) {
        return;
      }
      l = new ArrayList(remoteRequestQueue);
      remoteRequestQueue.clear();
    }
    for (int i = 0; i < n; i++) {
      sendRemoteRequest((RemoteRequest) l.get(i));
    }
  }

  private void sendRemoteRequest(RemoteRequest rr) {
    if (logger.isDebugEnabled()) {
      logger.debug("sendRemoteRequest: " + rr);
    }
    UIDService uidService = (UIDService)getServiceBroker().getService(this,
        UIDService.class, null);
    if (uidService == null) {
      logger.warn("Unable to send request, can't get UidService");
      return;
    }
    HealthMonitorRequest hmr =
        new HealthMonitorRequestImpl(agentId,
                                     rr.communityName,
                                     rr.action,
                                     rr.agentNames,
                                     rr.origNode,
                                     rr.destNode,
                                     uidService.nextUID());
    RelayAdapter hmrRa = new RelayAdapter(agentId, hmr, hmr.getUID());
    hmrRa.addTarget(SimpleMessageAddress.getSimpleMessageAddress(rr.destNode));
    if (logger.isDebugEnabled()) {
      logger.debug("Publishing HealthMonitorRequest:" +
                   " request=" + hmr.getRequestTypeAsString() +
                   " targets=" + targetsToString(hmrRa.getTargets()) +
                   " community-" + hmr.getCommunityName() +
                   " agents=" + arrayToString(hmr.getAgents()) +
                   " destNode=" + hmr.getDestinationNode());
    }
    blackboard.publishAdd(hmrRa);

  }

  /**
   * @param agentName
   */
  protected void doAction(HealthMonitorRequest hmr) {
    String origNode = hmr.getOriginNode();
    String destNode = hmr.getDestinationNode();
    switch (hmr.getRequestType()) {
      case HealthMonitorRequest.RESTART:
        if (destNode != null) {
          if (!agentId.toString().equals(destNode)) {
            // Forward request to destination node
            if (logger.isDebugEnabled()) {
              logger.debug("doAction, forwarding request: " + hmr);
            }
            sendRemoteRequest(new RemoteRequest(hmr.getAgents(),
                                                hmr.getRequestType(),
                                                destNode,
                                                origNode,
                                                hmr.getCommunityName()));

          } else { // local request
            String agentNames[] = hmr.getAgents();
            for (int i = 0; i < agentNames.length; i++) {
              doLocalAction(agentNames[i], hmr.getRequestType());
            }
          }
        }
        break;
      case HealthMonitorRequest.ADD:
         if (destNode != null) {
           if (!agentId.toString().equals(destNode)) {
             // Forward request to destination node
             if (logger.isDebugEnabled()) {
               logger.debug("doAction, forwarding request: " + hmr);
             }
             sendRemoteRequest(new RemoteRequest(hmr.getAgents(),
                                                 hmr.getRequestType(),
                                                 origNode,
                                                 destNode,
                                                 hmr.getCommunityName()));

           } else { // local request
             String agentNames[] = hmr.getAgents();
             for (int i = 0; i < agentNames.length; i++) {
               doLocalAction(agentNames[i], hmr.getRequestType());
             }
           }
         }
         break;
      case HealthMonitorRequest.KILL:
        if (origNode != null && !agentId.toString().equals(origNode)) {
          // Forward request to destination node
          if (logger.isDebugEnabled()) {
            logger.debug("doAction, forwarding request: " + hmr);
          }
          sendRemoteRequest(new RemoteRequest(hmr.getAgents(),
                                              hmr.getRequestType(),
                                              destNode,
                                              origNode,
                                              hmr.getCommunityName()));

        } else { // local request
          String agentNames[] = hmr.getAgents();
          for (int i = 0; i < agentNames.length; i++) {
            doLocalAction(agentNames[i], hmr.getRequestType());
          }
        }
        break;
    }
  }

  /**
   * Queue request for local action and trigger execute() method.
   * @param agentName
   */
  protected void doLocalAction(String agentName, int action) {
    if (logger.isDebugEnabled()) {
      logger.debug("doLocalAction:" +
                   " agent=" + agentName +
                   " action=" + action);
    }
    if (!localActionQueue.contains(agentName)) {
      localActionQueue.add(new LocalRequest(agentName, action));
      blackboard.signalClientActivity();
    }
  }

  /**
   * Returns current time as long.
   * @return Current time.
   */
  private long now() { return System.currentTimeMillis(); }

  /**
   * Publish requests to Mobility to initiate requested action and perform necessary
   * bookkeeping to keep track of actions that are in process.
   */
  private void doNextAction() {
    if ((!localActionQueue.isEmpty()) &&
        (actionsInProcess.size() <= MAX_CONCURRENT_ACTIONS)) {
      if (logger.isDebugEnabled()) {
        logger.debug("doNextAction: " +
                     " localActionQueue=" + localActionQueue.size() +
                     " actionsInProcess=" + actionsInProcess.size());
      }
      LocalRequest request = (LocalRequest)localActionQueue.remove(0);
      MessageAddress agent = MessageAddress.getMessageAddress(request.agentName);
      request.expiration = now() + ACTION_TIMEOUT;
      actionsInProcess.put(agent.toString(), request);
      try {
        UID acUID = uidService.nextUID();
        myUIDs.add(acUID);
        Object ticketId = mobilityFactory.createTicketIdentifier();
        AbstractTicket ticket = null;
        switch (request.action) {
          case HealthMonitorRequest.RESTART:
          case HealthMonitorRequest.ADD:
            ticket = new AddTicket(ticketId, agent, agentId);
            break;
          case HealthMonitorRequest.KILL:
            ticket = new RemoveTicket(ticketId, agent, agentId);
            break;
        }
        AgentControl ac =
            mobilityFactory.createAgentControl(acUID, agentId, ticket);
        actionInitiated(agent, request.action, agentId);
        blackboard.publishAdd(ac);
        if (logger.isDebugEnabled()) {
          StringBuffer sb =
              new StringBuffer("Publishing AgentControl:" +
                               " myUid=" + myUIDs.contains(ac.getOwnerUID()) +
                               " status=" + ac.getStatusCodeAsString());
          if (ac.getAbstractTicket() instanceof AddTicket) {
            AddTicket at = (AddTicket) ac.getAbstractTicket();
            sb.append(" agent=" + at.getMobileAgent() +
                      " destNode=" + at.getDestinationNode());
          }
          logger.debug(sb.toString());
        }
      } catch (Exception ex) {
        if (logger.isErrorEnabled()) {
          logger.error("Exception in agent restart", ex);
        }
      }
    }
  }

  /**
   * Check actions that are in process and remove any that haven't been
   * completed within the expiration time.
   */
  private void removeExpiredActions() {
    long now = now();
    LocalRequest currentActions[] = getActionsInProcess();
    for (int i = 0; i < currentActions.length; i++) {
      if (currentActions[i].expiration < now) {
        if (logger.isInfoEnabled()) {
          logger.info("Action timeout: agent=" + currentActions[i].agentName);
        }
        actionComplete(currentActions[i].agentName, currentActions[i].action, agentId, FAIL);
      }
    }
  }

  /**
   * Evaluate updates to AgentControl objects used by Mobility.
   * @param o
   */
  public void update(Object o) {
    if (o instanceof AgentControl) {
      AgentControl ac = (AgentControl)o;
      if (myUIDs.contains(ac.getOwnerUID())) {
        AbstractTicket ticket = ac.getAbstractTicket();
        if (ticket instanceof AddTicket) {
          AddTicket addTicket = (AddTicket) ticket;
          switch (ac.getStatusCode()) {
            case AgentControl.CREATED:
              blackboard.publishRemove(ac);
              myUIDs.remove(ac.getOwnerUID());
              actionComplete(addTicket.getMobileAgent().toString(),
                             HealthMonitorRequest.RESTART,
                             addTicket.getDestinationNode(),
                             SUCCESS);
              break;
            case AgentControl.ALREADY_EXISTS:
              blackboard.publishRemove(ac);
              myUIDs.remove(ac.getOwnerUID());
              actionComplete(addTicket.getMobileAgent().toString(),
                             HealthMonitorRequest.RESTART,
                             addTicket.getDestinationNode(),
                             SUCCESS);
              break;
            case AgentControl.FAILURE:
              blackboard.publishRemove(ac);
              myUIDs.remove(ac.getOwnerUID());
              actionComplete(addTicket.getMobileAgent().toString(),
                             HealthMonitorRequest.RESTART,
                             addTicket.getDestinationNode(),
                             FAIL);
              break;
            case AgentControl.NONE:
              break;
            default:
              if (logger.isInfoEnabled()) {
                logger.info("Unexpected restart status" +
                            " statucCode=" + ac.getStatusCodeAsString() +
                            ", blackboard object not removed");
              }
          }
        }
      }
    }
  }

  /**
   * Add a RestartListener.
   * @param rl  RestartListener to add
   */
  public void addListener(RestartListener rl) {
    synchronized (listeners) {
      if (!listeners.contains(rl))
        listeners.add(rl);
    }
  }

  /**
   * Remove a RestartListener.
   * @param rl  RestartListener to remove
   */
  public void removeListener(RestartListener rl) {
    synchronized (listeners) {
      if (listeners.contains(rl))
        listeners.remove(rl);
    }
  }

  /**
    * Notify restart listeners.
    */
   private void actionInitiated(MessageAddress agent, int action, MessageAddress dest) {
     if (logger.isDebugEnabled()) {
       logger.debug("ActionInitiated: agent=" + agent + " action=" + action +
                    " dest=" + dest);
     }
     synchronized (listeners) {
       for (Iterator it = listeners.iterator(); it.hasNext(); ) {
         RestartListener rl = (RestartListener) it.next();
         rl.actionInitiated(agent.toString(), action, dest.toString());
       }
     }
   }

   /**
    * Returns array of pending requests
    * @return
    */
  private LocalRequest[] getActionsInProcess() {
    synchronized (actionsInProcess) {
      return (LocalRequest[])actionsInProcess.values().toArray(new LocalRequest[0]);
    }
  }

  /**
   * Notify restart listeners.
   */
  private void actionComplete(String agent, int action, MessageAddress dest, int status) {
    if (logger.isDebugEnabled()) {
      logger.debug("ActionComplete: agent=" + agent + " action=" + action +
                   " dest=" + dest + " status=" + status);
    }
    synchronized (listeners) {
      for (Iterator it = listeners.iterator(); it.hasNext(); ) {
        RestartListener rl = (RestartListener) it.next();
        rl.actionComplete(agent.toString(), action, dest.toString(), status);
      }
    }
    actionsInProcess.remove(agent);
    doNextAction();
  }


  /**
   * Sends Cougaar event via EventService.
   */
  protected void event(String message) {
    if (eventService != null && eventService.isEventEnabled())
      eventService.event(message);
  }

  private static String targetsToString(Collection targets) {
    StringBuffer sb = new StringBuffer("[");
    for (Iterator it = targets.iterator(); it.hasNext();) {
      sb.append(it.next());
      if (it.hasNext()) sb.append(",");
    }
    sb.append("]");
    return sb.toString();
  }

  private static String arrayToString(String[] strArray) {
    if (strArray == null) {
      return "null";
    }
    else {
      StringBuffer sb = new StringBuffer("[");
      for (int i = 0; i < strArray.length; i++) {
        sb.append(strArray[i]);
        if (i < strArray.length - 1)
          sb.append(",");
      }
      sb.append("]");
      return sb.toString();
    }
  }

  // Timer for periodically stimulating execute() method to check/process
  // restart queue
  private class WakeAlarm implements Alarm {
    private long expiresAt;
    private boolean expired = false;
    public WakeAlarm (long expirationTime) {
      expiresAt = expirationTime;
    }
    public long getExpirationTime() {
      return expiresAt;
    }
    public synchronized void expire() {
      if (!expired) {
        expired = true;
        if (blackboard != null) blackboard.signalClientActivity();
        wakeAlarm = new WakeAlarm((new Date()).getTime() + TIMER_INTERVAL);
        alarmService.addRealTimeAlarm(wakeAlarm);
      }
    }
    public boolean hasExpired() {
      return expired;
    }
    public synchronized boolean cancel() {
      boolean was = expired;
      expired = true;
      return was;
    }
  }

  private class RemoteRequest {
    private String[] agentNames;
    private int action;
    private String origNode;
    private String destNode;
    private String communityName;
    RemoteRequest (String[] agents, int action, String orig, String dest, String community) {
      this.agentNames = agents;
      this.action = action;
      this.origNode = orig;
      this.destNode = dest;
      this.communityName = community;
    }
    public String toString() {
      StringBuffer sb = new StringBuffer("RemoteRequest");
      sb.append(" agents=[");
      for (int i = 0; i < agentNames.length; i++) {
        sb.append(agentNames[i]);
        if (i < agentNames.length - 1) sb.append(", ");
      }
      sb.append("] action=" + action);
      sb.append(" orig=" + origNode);
      sb.append(" dest=" + destNode);
      sb.append(" comm=" + communityName);
      return sb.toString();
    }
  }

  private class LocalRequest {
    private String agentName;
    private int action;
    private long expiration;
    LocalRequest (String agent, int action) {
      this.agentName = agent;
      this.action = action;
    }
  }

}
