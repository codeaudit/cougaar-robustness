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
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.service.UIDService;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ServiceBroker;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.SimpleMessageAddress;

import org.cougaar.core.mobility.AbstractTicket;
import org.cougaar.core.mobility.MoveTicket;
import org.cougaar.core.mobility.ldm.AgentControl;
import org.cougaar.core.mobility.ldm.MobilityFactory;

import org.cougaar.core.service.AlarmService;
import org.cougaar.core.agent.service.alarm.Alarm;

import org.cougaar.core.thread.Schedulable;

import org.cougaar.core.util.UID;

import org.cougaar.util.UnaryPredicate;

import java.util.ArrayList;
import java.util.Date;
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

  class MoveQueueEntry {
    MessageAddress agent;
    MessageAddress origNode;
    MessageAddress destNode;
    long expiration;
    MoveQueueEntry(MessageAddress a, MessageAddress o, MessageAddress d) {
      agent = a;
      origNode = o;
      destNode = d;
    }
  }

  public static final long TIMER_INTERVAL = 10000;
  public static final long MOVE_TIMEOUT = 60000;
  public static final long MAX_CONCURRENT_MOVES = 1;

  private List moveQueue = new ArrayList();
  private Map movesInProcess = new HashMap();

  private Set myUIDs = new HashSet();
  private MobilityFactory mobilityFactory;
  private LoggingService logger;
  private UIDService uidService = null;
  protected EventService eventService;
  private List listeners = new ArrayList();

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
        return (hmr.getRequestType() == hmr.MOVE);
      }
      return false;
  }};

/**
 * Constructor requires BindingSite to initialize needed services.
 * @param bs
 */
  public MoveHelper(BindingSite bs) {
    this.setBindingSite(bs);
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

  public void start() {
    super.start();
    alarmService.addRealTimeAlarm(new MoveTimer(TIMER_INTERVAL));
  }

  /**
   * Subscribe to mobility AgentControl objects and remote HealthMonitorRequests.
   */
  public void setupSubscriptions() {
    agentControlSub =
        (IncrementalSubscription)blackboard.subscribe(agentControlPredicate);
    healthMonitorRequests =
        (IncrementalSubscription)blackboard.subscribe(healthMonitorRequestPredicate);
  }

  public void execute() {
    // Get AgentControl objects
    for (Iterator it = agentControlSub.iterator(); it.hasNext();) {
      moveUpdate(it.next());
    }
    for (Iterator it = healthMonitorRequests.getAddedCollection().iterator(); it.hasNext(); ) {
      HealthMonitorRequest hsm = (HealthMonitorRequest) it.next();
      logger.debug("Received HealthMonitorRequest:" + hsm);
      if (hsm.getRequestType() == HealthMonitorRequest.MOVE) {
        String agentNames[] = hsm.getAgents();
        for (int i = 0; i < agentNames.length; i++) {
          moveAgent(agentNames[i], hsm.getDestinationNode());
        }
      }
    }
  }

  public void moveAgent(final String agentName, final String origNode, final String destNode, final String communityName) {
    logger.debug("MoveAgent:" +
                " origNode=" + origNode +
                " destNode=" + destNode +
                " agent=" + agentName);
    if (agentId.toString().equals(origNode)) {
      moveAgent(agentName, destNode);
    } else {
      ThreadService ts =
        (ThreadService) getServiceBroker().getService(this, ThreadService.class, null);
      Schedulable pingThread = ts.getThread(this, new Runnable() {
        public void run() {
          UIDService uidService = (UIDService) getServiceBroker().getService(this,
              UIDService.class, null);
          HealthMonitorRequest hmr =
              new HealthMonitorRequestImpl(agentId,
                                           communityName,
                                           HealthMonitorRequest.MOVE,
                                           new String[] {agentName},
                                           origNode,
                                           destNode,
                                           uidService.nextUID());
          RelayAdapter hmrRa =
              new RelayAdapter(agentId, hmr, hmr.getUID());
          hmrRa.addTarget(SimpleMessageAddress.
                          getSimpleMessageAddress(origNode));
          if (logger.isDebugEnabled()) {
            logger.debug("Publishing HealthMonitorRequest:" +
                         " request=" + hmr.getRequestTypeAsString() +
                         " targets=" + targetsToString(hmrRa.getTargets()) +
                         " community-" + hmr.getCommunityName() +
                         " agents=" + arrayToString(hmr.getAgents()) +
                         " origNode=" + hmr.getOriginNode() +
                         " destNode=" + hmr.getDestinationNode());
          }
          blackboard.openTransaction();
          blackboard.publishAdd(hmrRa);
          blackboard.closeTransaction();
        }
    }, "PingThread");
    getServiceBroker().releaseService(this, ThreadService.class, ts);
    pingThread.start();
    }
  }

  protected void moveAgent(String agentName, String destNode) {
    moveQueue.add(new MoveQueueEntry(SimpleMessageAddress.getSimpleMessageAddress(agentName),
                                     agentId,
                                     SimpleMessageAddress.getSimpleMessageAddress(destNode)));
  }

  private long now() { return (new Date()).getTime(); }

  /**
   * Move next agent on queue if number of moves in process does not exceed
   * maximum.  Check for moves that have timed out and remove.
   */
  private void moveNext() {
    removeExpiredMoves();
    synchronized (movesInProcess) {
      if ( (!moveQueue.isEmpty()) &&
          (movesInProcess.size() <= MAX_CONCURRENT_MOVES)) {
        logger.debug("MoveNext: " +
                     " MoveQueue=" + moveQueue.size() +
                     " MovesInProcess=" + movesInProcess.size());
        final MoveQueueEntry mqe = (MoveQueueEntry) moveQueue.remove(0);
        try {
          ThreadService ts =
          (ThreadService) getServiceBroker().getService(this, ThreadService.class, null);
          Schedulable moveRequestThread = ts.getThread(this, new Runnable() {
          public void run() {
            Object ticketId = mobilityFactory.createTicketIdentifier();
            MoveTicket moveTicket = new MoveTicket(ticketId,
                mqe.agent,    // agent to move
                mqe.origNode, //current node
                mqe.destNode, //destination node
                false);       // forced restart
            UID acUID = uidService.nextUID();
            myUIDs.add(acUID);
            AgentControl ac =
                mobilityFactory.createAgentControl(acUID, agentId, moveTicket);
            mqe.expiration = now() + MOVE_TIMEOUT;
            movesInProcess.put(mqe.agent, mqe);
            moveInitiated(mqe.agent, mqe.origNode, mqe.destNode);
            event("Moving agent: agent=" + mqe.agent + " orig=" + mqe.origNode +
                  " dest=" + mqe.destNode);
            blackboard.openTransaction();
            blackboard.publishAdd(ac);
            blackboard.closeTransaction();
            if (logger.isDebugEnabled()) {
              StringBuffer sb =
                  new StringBuffer("Publishing AgentControl:" +
                                   " myUid=" + myUIDs.contains(ac.getOwnerUID()) +
                                   " status=" + ac.getStatusCodeAsString());
              if (ac.getAbstractTicket()instanceof MoveTicket) {
                MoveTicket mt = (MoveTicket) ac.getAbstractTicket();
                sb.append(" agent=" + mt.getMobileAgent() +
                          " origNode=" + mt.getOriginNode() +
                          " destNode=" + mt.getDestinationNode());
              }
              logger.debug(sb.toString());
            }
          }
        }, "MoveRequestThread");
        getServiceBroker().releaseService(this, ThreadService.class, ts);
        moveRequestThread.start();
        } catch (Exception ex) {
          logger.error("Exception in agent move", ex);
        }
      }
    }
  }

  /**
   * Removed timed out moves.
   */
  private void removeExpiredMoves() {
      long now = now();
      for (Iterator it = movesInProcess.entrySet().iterator(); it.hasNext();) {
        Map.Entry me = (Map.Entry)it.next();
        MessageAddress agent = (MessageAddress)me.getKey();
        MoveQueueEntry mqe = (MoveQueueEntry)me.getValue();
        if (mqe.expiration < now) {
          it.remove();
          logger.debug("Move timeout: agent=" + agent);
          moveComplete(mqe.agent, mqe.origNode, mqe.destNode, FAIL);
        }
      }
  }

  /**
   * Evalutes changed AgentControl object for updated move status information.
   * @param o
   */
  public void moveUpdate(Object o) {
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
              logger.info("Unexpected move status" +
                          " statucCode=" + ac.getStatusCodeAsString() +
                          ", blackboard object not removed");
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
    logger.debug("MoveInitiated: agent=" + agent);
    synchronized (listeners) {
      for (Iterator it = listeners.iterator(); it.hasNext(); ) {
        MoveListener ml = (MoveListener) it.next();
        ml.moveInitiated(agent.toString(), orig.toString(), dest.toString());
      }
    }
    moveNext();
  }


  /**
   * Notify move listeners.
   */
  private void moveComplete(MessageAddress agent, MessageAddress orig, MessageAddress dest, int status) {
    logger.debug("MoveComplete: agent=" + agent);
    synchronized (listeners) {
      for (Iterator it = listeners.iterator(); it.hasNext(); ) {
        MoveListener ml = (MoveListener) it.next();
        ml.moveComplete(agent.toString(), orig.toString(), dest.toString(), status);
      }
    }
    movesInProcess.remove(agent);
    moveNext();
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
  private class MoveTimer implements Alarm {
    private long expirationTime = -1;
    private boolean expired = false;
    public MoveTimer(long delay) {
      expirationTime = delay + System.currentTimeMillis();
    }

    public void expire() {
      if (!expired) {
        moveNext();
      }
      alarmService.addRealTimeAlarm(new MoveTimer(TIMER_INTERVAL));
    }

    public long getExpirationTime() {
      return expirationTime;
    }

    public boolean hasExpired() {
      return expired;
    }

    public synchronized boolean cancel() {
      if (!expired)
        return expired = true;
      return false;
    }
  }
}
