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
import org.cougaar.core.mobility.MoveTicket;
import org.cougaar.core.mobility.ldm.AgentControl;
import org.cougaar.core.mobility.ldm.MobilityFactory;

import org.cougaar.core.service.AlarmService;
import org.cougaar.core.agent.service.alarm.Alarm;

import org.cougaar.core.util.UID;

import org.cougaar.util.UnaryPredicate;

import org.cougaar.tools.robustness.ma.CommunityStatusModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.Collections;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Provides interface to Mobility Service for performing agent moves.  The
 * destination node is responsible for coordinating the move.  If the
 * local agent is not the move destination a HealthMonitorRequest relay
 * is sent to destination node and is received by another instance of this
 * component.  If this node is the destination AgentControl objects are
 * published to local blackboard to intitiate move by the MobilityService.
 */
public class MoveHelper extends BlackboardClientComponent {

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
  protected RestartDestinationLocator restartLocator;

  // Subscription to AgentControl objects used by MobilityService
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

  // Subscription to HealthMonitorRequests from peer nodes
  private IncrementalSubscription healthMonitorRequests;
  private UnaryPredicate healthMonitorRequestPredicate = new UnaryPredicate() {
    public boolean execute (Object o) {
      if (o instanceof HealthMonitorRequest) {
        HealthMonitorRequest hmr = (HealthMonitorRequest)o;
        return (hmr.getRequestType() == HealthMonitorRequest.MOVE);
      }
      return false;
  }};

/**
 * Constructor requires BindingSite to initialize needed services.
 * @param bs
 */
  public MoveHelper(BindingSite bs, CommunityStatusModel model, RestartDestinationLocator rdl) {
    this.setBindingSite(bs);
    this.restartLocator = rdl;
    initialize();
    load();
    start();
  }

  /**
   * Load requires services.
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
   * Subscribe to mobility AgentControl objects and remote HealthMonitorRequests.
   */
  public void setupSubscriptions() {
    agentControlSub =
        (IncrementalSubscription)blackboard.subscribe(agentControlPredicate);
    healthMonitorRequests =
        (IncrementalSubscription)blackboard.subscribe(healthMonitorRequestPredicate);
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

  public void moveAgent(final String agentName, final String origNode,
                        final String destNode, final String communityName) {
    if (logger.isDebugEnabled()) {
      logger.debug("MoveAgent:" +
                   " agent=" + agentName +
                   " orig=" + origNode +
                   " dest=" + destNode);
    }
    if (agentId.toString().equals(destNode)) {
      // Restart locally
      doLocalAction(agentName, origNode, destNode);
    } else {
      // Queue request to remote agent
      fireLater(new RemoteRequest(new String[] {agentName},
                                  HealthMonitorRequest.MOVE,
                                  origNode,
                                  destNode,
                                  communityName));
    }
  }

  /**
   * @param agentName
   */
  protected void doAction(HealthMonitorRequest hmr) {
    String origNode = hmr.getOriginNode();
    String destNode = hmr.getDestinationNode();
    if (hmr.getRequestType() == HealthMonitorRequest.MOVE) {
      if (!agentId.toString().equals(origNode)) {
        // Forward request to origin node
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
          doLocalAction(agentNames[i], hmr.getOriginNode(),
                        hmr.getDestinationNode());
        }
      }
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
    hmrRa.addTarget(SimpleMessageAddress.getSimpleMessageAddress(rr.origNode));
    if (logger.isDebugEnabled()) {
      logger.debug("Publishing HealthMonitorRequest:" +
                   " request=" + hmr.getRequestTypeAsString() +
                   " targets=" + targetsToString(hmrRa.getTargets()) +
                   " community-" + hmr.getCommunityName() +
                   " agents=" + arrayToString(hmr.getAgents()) +
                   " origNode=" + hmr.getOriginNode() +
                   " destNode=" + hmr.getDestinationNode());
    }
    blackboard.publishAdd(hmrRa);

  }

  /**
   * Queue request for local action and trigger execute() method.
   * @param agentName
   */
  protected void doLocalAction(String agentName, String orig, String dest) {
    if (logger.isDebugEnabled()) {
      logger.debug("moveAgent:" +
                   " agent=" + agentName +
                   " origNode=" + orig +
                   " destNode=" + dest);
    }
    if (!localActionQueue.contains(agentName)) {
      localActionQueue.add(new LocalRequest(MessageAddress.getMessageAddress(agentName),
                                            MessageAddress.getMessageAddress(orig),
                                            MessageAddress.getMessageAddress(dest)) );
      blackboard.signalClientActivity();
    }
  }

  private long now() { return (new Date()).getTime(); }

  /**
   * Move next agent on queue if number of moves in process does not exceed
   * maximum.  Check for moves that have timed out and remove.
   */
  private void doNextAction() {
    if ((!localActionQueue.isEmpty()) &&
        (actionsInProcess.size() < MAX_CONCURRENT_ACTIONS)) {
      LocalRequest request = (LocalRequest)localActionQueue.remove(0);
      if (logger.isDebugEnabled()) {
        logger.debug("doNextAction: " +
                     " next=[" + request + "]" +
                     " localActionQueue=" + localActionQueue.size() +
                     " actionsInProcess=" + actionsInProcess.size());
      }
      request.expiration = now() + ACTION_TIMEOUT;
      actionsInProcess.put(request.agent, request);
      try {
        UID acUID = uidService.nextUID();
        myUIDs.add(acUID);
        Object ticketId = mobilityFactory.createTicketIdentifier();
        AbstractTicket ticket = new MoveTicket(ticketId,
                                    request.agent, // agent to move
                                    request.origNode, //current node
                                    request.destNode, //destination node
                                    false); // forced restart
        AgentControl ac =
            mobilityFactory.createAgentControl(acUID, agentId, ticket);
        moveInitiated(request.agent, request.origNode, request.destNode);
        event("Moving agent: agent=" + request.agent + " orig=" + request.origNode +
              " dest=" + request.destNode);
        blackboard.publishAdd(ac);
        if (logger.isDebugEnabled()) {
          logger.debug("Publishing AgentControl:" +
                       " myUid=" + myUIDs.contains(ac.getOwnerUID()) +
                       " status=" + ac.getStatusCodeAsString());
        }
      } catch (Exception ex) {
        if (logger.isErrorEnabled()) {
          logger.error("Exception in agent move", ex);
        }
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
   * Removed timed out moves.
   */
  private void removeExpiredActions() {
    long now = now();
    LocalRequest currentActions[] = getActionsInProcess();
    for (int i = 0; i < currentActions.length; i++) {
      if (currentActions[i].expiration < now) {
        if (logger.isInfoEnabled()) {
          logger.info("Move timeout: agent=" + currentActions[i].agent);
        }
        moveComplete(currentActions[i].agent, currentActions[i].origNode,
                       currentActions[i].destNode, FAIL);
      }
    }
  }

  /**
   * Evalutes changed AgentControl object for updated move status information.
   * @param o
   */
  /**
   * Evaluate updates to AgentControl objects used by Mobility.
   * @param o
   */
  public void update(Object o) {
    if (o instanceof AgentControl) {
      AgentControl ac = (AgentControl)o;
      if (myUIDs.contains(ac.getOwnerUID())) {
        AbstractTicket ticket = ac.getAbstractTicket();
        if (ticket instanceof MoveTicket) {
          MoveTicket moveTicket = (MoveTicket) ticket;
          switch (ac.getStatusCode()) {
            case AgentControl.CREATED:
              /*
              event("Move successful:" +
                    " agent=" + moveTicket.getMobileAgent() +
                    " dest=" + moveTicket.getDestinationNode() +
                    " status=" + ac.getStatusCodeAsString());
              */
              blackboard.publishRemove(ac);
              myUIDs.remove(ac.getOwnerUID());
              moveComplete(moveTicket.getMobileAgent(),
                           moveTicket.getOriginNode(),
                           moveTicket.getDestinationNode(),
                           SUCCESS);
              break;
            case AgentControl.ALREADY_EXISTS:
              /*
              event("Move successful:" +
                    " agent=" + moveTicket.getMobileAgent() +
                    " dest=" + moveTicket.getDestinationNode() +
                    " status=" + ac.getStatusCodeAsString());
              */
              blackboard.publishRemove(ac);
              myUIDs.remove(ac.getOwnerUID());
              moveComplete(moveTicket.getMobileAgent(),
                           moveTicket.getOriginNode(),
                           moveTicket.getDestinationNode(),
                           SUCCESS);
              break;
            case AgentControl.MOVED:
              /*
              event("Move successful:" +
                    " agent=" + moveTicket.getMobileAgent() +
                    " dest=" + moveTicket.getDestinationNode() +
                    " status=" + ac.getStatusCodeAsString());
              */
              blackboard.publishRemove(ac);
              myUIDs.remove(ac.getOwnerUID());
              moveComplete(moveTicket.getMobileAgent(),
                           moveTicket.getOriginNode(),
                           moveTicket.getDestinationNode(),
                           SUCCESS);
              break;
            case AgentControl.FAILURE:
              /*
              event("Move failed:" +
                    " agent=" + moveTicket.getMobileAgent() +
                    " dest=" + moveTicket.getDestinationNode() +
                    " status=" + ac.getStatusCodeAsString());
              */
              blackboard.publishRemove(ac);
              myUIDs.remove(ac.getOwnerUID());
              moveComplete(moveTicket.getMobileAgent(),
                           moveTicket.getOriginNode(),
                           moveTicket.getDestinationNode(),
                           FAIL);
              break;
            case AgentControl.NONE:
              break;
            default:
              if (logger.isInfoEnabled()) {
                logger.info("Unexpected move status" +
                            " statucCode=" + ac.getStatusCodeAsString() +
                            ", blackboard object not removed");
              }
          }
        }
      }
    }
  }

  /**
   * Add a MoveListener.
   * @param ml  MoveListener to add
   */
  public void addListener(MoveListener ml) {
    synchronized (listeners) {
      if (!listeners.contains(ml))
        listeners.add(ml);
    }
  }

  /**
   * Remove a MoveListener.
   * @param ml  MoveListener to remove
   */
  public void removeListener(MoveListener ml) {
    synchronized (listeners) {
      if (listeners.contains(ml))
        listeners.remove(ml);
    }
  }

  /**
   * Notify move listeners.
   */
  private void moveInitiated(MessageAddress agent, MessageAddress orig, MessageAddress dest) {
    if (logger.isDebugEnabled()) {
      logger.debug("MoveInitiated: agent=" + agent);
    }
    synchronized (listeners) {
      for (Iterator it = listeners.iterator(); it.hasNext(); ) {
        MoveListener ml = (MoveListener) it.next();
        ml.moveInitiated(agent.toString(), orig.toString(), dest.toString());
      }
    }
  }


  /**
   * Notify move listeners.
   */
  private void moveComplete(MessageAddress agent, MessageAddress orig, MessageAddress dest, int status) {
    if (logger.isDebugEnabled()) {
      logger.debug("MoveComplete: agent=" + agent);
    }
    synchronized (listeners) {
      for (Iterator it = listeners.iterator(); it.hasNext(); ) {
        MoveListener ml = (MoveListener) it.next();
        ml.moveComplete(agent.toString(), orig.toString(), dest.toString(), status);
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

  /**
   * Timer used to trigger periodic check for agents to move.
   */
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
    private MessageAddress agent;
    private MessageAddress origNode;
    private MessageAddress destNode;
    private long expiration;
    LocalRequest (MessageAddress agent, MessageAddress orig, MessageAddress dest) {
      this.agent = agent;
      this.origNode = orig;
      this.destNode = dest;
    }
    public String toString() {
      return "agent=" + agent + " orig=" + origNode + " dest=" + destNode;
    }
  }

}
