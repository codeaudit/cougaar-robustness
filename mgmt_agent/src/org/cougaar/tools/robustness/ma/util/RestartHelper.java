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
import org.cougaar.core.mobility.AddTicket;
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

import EDU.oswego.cs.dl.util.concurrent.Semaphore;

/**
 */

public class RestartHelper extends BlackboardClientComponent {

  public static final int SUCCESS = 0;
  public static final int FAIL = 1;

  public static final long TIMER_INTERVAL = 10000;
  public static final long RESTART_TIMEOUT = 60000;
  public static final long MAX_CONCURRENT_RESTARTS = 1;

  private List restartQueue = new ArrayList();
  private Map restartsInProcess = new HashMap();

  private Set myUIDs = new HashSet();
  private MobilityFactory mobilityFactory;
  private LoggingService logger;
  private UIDService uidService = null;
  protected EventService eventService;
  private List listeners = new ArrayList();

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

  private IncrementalSubscription healthMonitorRequests;
  private UnaryPredicate healthMonitorRequestPredicate = new UnaryPredicate() {
    public boolean execute (Object o) {
      if (o instanceof HealthMonitorRequest) {
        HealthMonitorRequest hmr = (HealthMonitorRequest)o;
        return (hmr.getRequestType() == hmr.RESTART);
      }
      return false;
  }};

  public RestartHelper(BindingSite bs) {
    this.setBindingSite(bs);
    initialize();
    load();
    start();
  }

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
    alarmService.addRealTimeAlarm(new RestartTimer(TIMER_INTERVAL));
  }

  public void setupSubscriptions() {
    agentControlSub =
        (IncrementalSubscription)blackboard.subscribe(agentControlPredicate);
    healthMonitorRequests =
        (IncrementalSubscription)blackboard.subscribe(healthMonitorRequestPredicate);
  }

  public void execute() {
    // Get AgentControl objects
    for (Iterator it = agentControlSub.iterator(); it.hasNext();) {
      update(it.next());
    }
    for (Iterator it = healthMonitorRequests.getAddedCollection().iterator(); it.hasNext(); ) {
      HealthMonitorRequest hsm = (HealthMonitorRequest) it.next();
      logger.debug("Received HealthMonitorRequest:" + hsm);
      if (hsm.getRequestType() == HealthMonitorRequest.RESTART) {
        String agentNames[] = hsm.getAgents();
        for (int i = 0; i < agentNames.length; i++) {
          restartAgent(agentNames[i]);
        }
      }
    }
  }


  public void restartAgent(String agentName, String origNode, String destNode, String communityName) {
    logger.debug("RestartAgent:" +
                " destNode=" + destNode +
                " agent=" + agentName);
    if (agentId.toString().equals(destNode)) {
      restartAgent(agentName);
    } else {
      UIDService uidService = (UIDService) getServiceBroker().getService(this, UIDService.class, null);
      HealthMonitorRequest hmr =
          new HealthMonitorRequestImpl(agentId,
          communityName,
          HealthMonitorRequest.RESTART,
          new String[]{agentName},
          origNode,
          destNode,
          uidService.nextUID());
      RelayAdapter hmrRa =
          new RelayAdapter(agentId, hmr, hmr.getUID());
      hmrRa.addTarget(SimpleMessageAddress.
                      getSimpleMessageAddress(destNode));
      if (logger.isDebugEnabled()) {
        logger.debug("Publishing HealthMonitorRequest:" +
                    " request=" + hmr.getRequestTypeAsString() +
                    " targets=" + targetsToString(hmrRa.getTargets()) +
                    " community-" + hmr.getCommunityName() +
                    " agents=" +
                    arrayToString(hmr.getAgents()) +
                    " destNode=" + hmr.getDestinationNode());
      }
      blackboard.openTransaction();
      blackboard.publishAdd(hmrRa);
      blackboard.closeTransaction();
    }
  }

  protected void restartAgent(String agentName) {
    logger.debug("RestartAgent:" +
                " agent=" + agentName);
    if (!restartQueue.contains(agentName)) restartQueue.add(agentName);
  }

  private long now() { return (new Date()).getTime(); }

  private void restartNext() {
    removeExpiredRestarts();
    synchronized (restartsInProcess) {
      if ( (!restartQueue.isEmpty()) &&
          (restartsInProcess.size() <= MAX_CONCURRENT_RESTARTS)) {
        logger.debug("RestartNext: " +
                     " RestartQueue=" + restartQueue.size() +
                     " restartsInProcess=" + restartsInProcess.size());
        final MessageAddress agent =
            SimpleMessageAddress.getSimpleMessageAddress( (String)
            restartQueue.remove(0));
        Long restartExpiration = new Long(now() + RESTART_TIMEOUT);
        restartsInProcess.put(agent, restartExpiration);
        try {

          Object ticketId = mobilityFactory.createTicketIdentifier();
          AddTicket addTicket = new AddTicket(ticketId, agent, agentId);
          UID acUID = uidService.nextUID();
          myUIDs.add(acUID);
          AgentControl ac =
              mobilityFactory.createAgentControl(acUID, agentId, addTicket);
          restartInitiated(agent);
          event("Restarting agent: agent=" + agent + " dest=" + agentId);
          blackboard.publishAdd(ac);
          if (logger.isInfoEnabled()) {
            StringBuffer sb =
                new StringBuffer("Publishing AgentControl:" +
                                 " myUid=" + myUIDs.contains(ac.getOwnerUID()) +
                                 " status=" + ac.getStatusCodeAsString());
            if (ac.getAbstractTicket()instanceof AddTicket) {
              AddTicket at = (AddTicket) ac.getAbstractTicket();
              sb.append(" agent=" + at.getMobileAgent() +
                        " destNode=" + at.getDestinationNode());
            }
            logger.debug(sb.toString());
          }
        }
        catch (Exception ex) {
          logger.error("Exception in agent restart", ex);
        }
      }
    }
  }

  private void removeExpiredRestarts() {
      long now = now();
      for (Iterator it = restartsInProcess.entrySet().iterator(); it.hasNext();) {
        Map.Entry me = (Map.Entry)it.next();
        MessageAddress agent = (MessageAddress)me.getKey();
        long expiration = ((Long)me.getValue()).longValue();
        if (expiration < now) {
          it.remove();
          logger.debug("Restart timeout: agent=" + agent);
          restartComplete(agent, FAIL);
        }
      }
  }


  public void update(Object o) {
    if (o instanceof AgentControl) {
      AgentControl ac = (AgentControl)o;
      if (myUIDs.contains(ac.getOwnerUID())) {
        AbstractTicket ticket = ac.getAbstractTicket();
        if (ticket instanceof AddTicket) {
          AddTicket addTicket = (AddTicket) ticket;
          switch (ac.getStatusCode()) {
            case AgentControl.CREATED:
              event("Restart successful:" +
                    " agent=" + addTicket.getMobileAgent() +
                    " dest=" + addTicket.getDestinationNode() +
                    " status=" + ac.getStatusCodeAsString());
              blackboard.publishRemove(ac);
              myUIDs.remove(ac.getOwnerUID());
              restartComplete(addTicket.getMobileAgent(),
                              SUCCESS);
              break;
            case AgentControl.ALREADY_EXISTS:
              event("Restart successful:" +
                    " agent=" + addTicket.getMobileAgent() +
                    " dest=" + addTicket.getDestinationNode() +
                    " status=" + ac.getStatusCodeAsString());
              blackboard.publishRemove(ac);
              myUIDs.remove(ac.getOwnerUID());
              restartComplete(addTicket.getMobileAgent(),
                              SUCCESS);
              break;
            case AgentControl.FAILURE:
              event("Restart failed:" +
                    " agent=" + addTicket.getMobileAgent() +
                    " dest=" + addTicket.getDestinationNode() +
                    " status=" + ac.getStatusCodeAsString());
              blackboard.publishRemove(ac);
              myUIDs.remove(ac.getOwnerUID());
              restartComplete(addTicket.getMobileAgent(), FAIL);
              break;
            case AgentControl.NONE:
              break;
            default:
              logger.info("Unexpected restart status" +
                          " statucCode=" + ac.getStatusCodeAsString() +
                          ", blackboard object not removed");
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
   private void restartInitiated(MessageAddress agent) {
     logger.debug("RestartInitiated: agent=" + agent);
     synchronized (listeners) {
       for (Iterator it = listeners.iterator(); it.hasNext(); ) {
         RestartListener rl = (RestartListener) it.next();
         rl.restartInitiated(agent.toString());
       }
     }
   }


  /**
   * Notify restart listeners.
   */
  private void restartComplete(MessageAddress agent, int status) {
    logger.debug("RestartComplete: agent=" + agent);
    synchronized (listeners) {
      for (Iterator it = listeners.iterator(); it.hasNext(); ) {
        RestartListener rl = (RestartListener) it.next();
        rl.restartComplete(agent.toString(), status);
      }
    }
    restartsInProcess.remove(agent);
    restartNext();
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
   * Timer used to trigger periodic check for agents to restart.
   */
  private class RestartTimer implements Alarm {
    private long expirationTime = -1;
    private boolean expired = false;
    public RestartTimer(long delay) {
      expirationTime = delay + System.currentTimeMillis();
    }

    public void expire() {
      if (!expired) {
        blackboard.openTransaction();
        restartNext();
        blackboard.closeTransaction();
      }
      alarmService.addRealTimeAlarm(new RestartTimer(TIMER_INTERVAL));
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
