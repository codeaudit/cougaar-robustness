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

import org.cougaar.tools.robustness.sensors.SensorFactory;
import org.cougaar.tools.robustness.sensors.HeartbeatRequest;
import org.cougaar.tools.robustness.sensors.HeartbeatHealthReport;
import org.cougaar.tools.robustness.sensors.HeartbeatEntry;

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
import org.cougaar.core.component.ServiceBroker;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.SimpleMessageAddress;

import org.cougaar.core.service.AlarmService;
import org.cougaar.core.agent.service.alarm.Alarm;

import org.cougaar.core.util.UID;

import org.cougaar.util.UnaryPredicate;

import java.util.ArrayList;
import java.util.Date;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Helper class for sending HeartbeatRequests and receiving heartbeat status.
 */

public class HeartbeatHelper extends BlackboardClientComponent {

  public static final int SUCCESS = 0;
  public static final int FAIL = 1;
  public static final int START = 0;
  public static final int STOP = 1;

  private List heartbeatRequestQueue = new ArrayList();
  private Map heartbeatRequests = Collections.synchronizedMap(new HashMap());
  private List myUIDs = new ArrayList();
  private LoggingService logger;
  private UIDService uidService = null;
  protected EventService eventService;
  private List listeners = new ArrayList();
  private SensorFactory sensorFactory;

  private IncrementalSubscription heartbeatRequestSub;
  private UnaryPredicate heartbeatRequestPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof HeartbeatRequest) {
        HeartbeatRequest hbr = (HeartbeatRequest)o;
        return (myUIDs.contains(hbr.getUID()));
      }
      return false;
  }};

  /**
   * Predicate for Heartbeats.
    */
  private IncrementalSubscription heartbeatHealthReports;
  private UnaryPredicate heartbeatPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return(o instanceof HeartbeatHealthReport);
    }
  };

  public HeartbeatHelper(BindingSite bs) {
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
    uidService = (UIDService) getServiceBroker().getService(this, UIDService.class, null);
    super.load();
  }

  public void start() {
    super.start();
    DomainService domainService =
      (DomainService) getBindingSite().getServiceBroker().
      getService(this, DomainService.class, null);
    sensorFactory =
      ((SensorFactory) domainService.getFactory("sensors"));
    if (sensorFactory == null) {
      logger.error("Unable to get 'sensors' domain");
    }
  }

  public void setupSubscriptions() {
    // Subscribe to HeartbeatRequests to receive heartbeat status
    heartbeatRequestSub =
      (IncrementalSubscription)blackboard.subscribe(heartbeatRequestPredicate);
    // Subscribe to HeartbeatHealthReports to receive notification of failed
    // heartbeats
    heartbeatHealthReports =
      (IncrementalSubscription)blackboard.subscribe(heartbeatPredicate);
  }

  public void execute() {

    fireAll();

    // Get HeartbeatRequests
    for (Iterator it = heartbeatRequestSub.getChangedCollection().iterator(); it.hasNext();) {
      HeartbeatRequest hbr = (HeartbeatRequest) it.next();
      int status = hbr.getStatus();
      MessageAddress target = getTarget(hbr);
      if (status == HeartbeatRequest.ACCEPTED) {
        heartbeatRequests.put(target, hbr);
        heartbeatsStarted(target);
      } else if (status == HeartbeatRequest.FAILED || status == HeartbeatRequest.REFUSED) {
        logger.debug("Heartbeat request failed: agent=" + target +
                    " status=" + hbr.statusToString(hbr.getStatus()));
        heartbeatFailure(target);
      }
    }

    for (Iterator it = heartbeatRequestSub.getRemovedCollection().iterator(); it.hasNext();) {
      HeartbeatRequest hbr = (HeartbeatRequest) it.next();
      MessageAddress target = getTarget(hbr);
      heartbeatRequests.remove(target);
      heartbeatsStopped(target);
    }

    // Get HeartbeatHealthReports
    for(Iterator it = heartbeatHealthReports.getAddedCollection().iterator();
        it.hasNext(); ) {
      HeartbeatHealthReport hbhr = (HeartbeatHealthReport)it.next();
      HeartbeatEntry hbe[] = hbhr.getHeartbeats();
      for(int i = 0; i < hbe.length; i++) {
        hbe[i].getSource();
        logger.debug("HeartbeatTimeout: agent=" + hbe[i].getSource() +
                     ", heartbeatEntry=" + hbe[i].toString());
        heartbeatFailure(hbe[i].getSource());
      }
      blackboard.publishRemove(hbhr);
    }
  }

  public void startHeartbeats(final String agentName,
                              final long hbrTimeout,
                              final long hbFrequency,
                              final long hbTimeout,
                              final long pctOutOfSpec) {
    fireLater(new QueueEntry(START,
                             MessageAddress.getMessageAddress(agentName),
                             hbrTimeout,
                             hbFrequency,
                             hbTimeout,
                             pctOutOfSpec));
  }

  public void stopHeartbeats(final String agentName) {
    fireLater(new QueueEntry(STOP,
                             MessageAddress.getMessageAddress(agentName)));
  }

  /**
   * Add a HeartbeatListener.
   * @param hbl  HeartbeatListener to add
   */
  public void addListener(HeartbeatListener hbl) {
      if (!listeners.contains(hbl))
        listeners.add(hbl);
  }

  /**
   * Remove a HeartbeatListener.
   * @param ml  HeartbeatListener to remove
   */
  public void removeListener(HeartbeatListener hbl) {
      if (listeners.contains(hbl))
        listeners.remove(hbl);
  }

  /**
   * Notify HeartbeatListeners.
   */
  private void heartbeatFailure(MessageAddress agent) {
    logger.debug("Heartbeat failed: agent=" + agent);
      for (Iterator it = listeners.iterator(); it.hasNext(); ) {
        HeartbeatListener hbl = (HeartbeatListener)it.next();
        hbl.heartbeatFailure(agent.toString());
      }
  }

  private void heartbeatsStarted(MessageAddress agent) {
    logger.debug("Heartbeats started: agent=" + agent);
      for (Iterator it = listeners.iterator(); it.hasNext(); ) {
        HeartbeatListener hbl = (HeartbeatListener)it.next();
        hbl.heartbeatStarted(agent.toString());
      }
  }

  private void heartbeatsStopped(MessageAddress agent) {
    logger.debug("Heartbeats stopped: agent=" + agent);
      for (Iterator it = listeners.iterator(); it.hasNext(); ) {
        HeartbeatListener hbl = (HeartbeatListener)it.next();
        hbl.heartbeatStopped(agent.toString());
      }
  }

  private MessageAddress getTarget(HeartbeatRequest hbr) {
    Set targets = hbr.getTargets();
    return !targets.isEmpty()
               ? ((MessageAddress[])targets.toArray(new MessageAddress[0]))[0]
               : null;
  }

  protected void fireLater(QueueEntry qe) {
    synchronized (heartbeatRequestQueue) {
      heartbeatRequestQueue.add(qe);
    }
    if (blackboard != null) {
      blackboard.signalClientActivity();
    }
  }

  private void fireAll() {
    int n;
    List l;
    synchronized (heartbeatRequestQueue) {
      n = heartbeatRequestQueue.size();
      if (n <= 0) {
        return;
      }
      l = new ArrayList(heartbeatRequestQueue);
      heartbeatRequestQueue.clear();
    }
    for (int i = 0; i < n; i++) {
      QueueEntry qe = (QueueEntry) l.get(i);
      if (qe.action == START) {
        HeartbeatRequest hbr =
            sensorFactory.newHeartbeatRequest(agentId,
                                              Collections.singleton(qe.agent),
                                              qe.hbrTimeout,
                                              qe.hbFrequency,
                                              qe.hbTimeout,
                                              true,
                                              qe.pctOutOfSpec);
        myUIDs.add(hbr.getUID());
        blackboard.publishAdd(hbr);
      } else {  // Stop heartbeats
        HeartbeatRequest hbr = (HeartbeatRequest)heartbeatRequests.get(qe.agent);
        if (hbr != null) {
          blackboard.publishRemove(hbr);
        }
      }
    }
  }

  static class QueueEntry {
    int action;
    MessageAddress agent;
    long hbrTimeout;
    long hbFrequency;
    long hbTimeout;
    long pctOutOfSpec;
    QueueEntry(int action,
               MessageAddress agent) {
      this.action = action;
      this.agent = agent;
    }
    QueueEntry(int action,
               MessageAddress agent,
               long hbrTimeout,
               long hbFrequency,
               long hbTimeout,
               long pctOutOfSpec) {
      this.action = action;
      this.agent = agent;
      this.hbrTimeout = hbrTimeout;
      this.hbFrequency = hbFrequency;
      this.hbTimeout = hbTimeout;
      this.pctOutOfSpec = pctOutOfSpec;
    }
  }
}
