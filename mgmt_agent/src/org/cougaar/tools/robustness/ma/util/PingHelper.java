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
import org.cougaar.tools.robustness.sensors.PingRequest;

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

import org.cougaar.core.service.AlarmService;

import org.cougaar.core.util.UID;

import org.cougaar.util.UnaryPredicate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Helper class for sending ping to remote agent.
 */

public class PingHelper extends BlackboardClientComponent {

  public static final long MAX_CONCURRENT_PINGS = -1;

  //private Map myUIDs = new HashMap();

  private List pingQueue = Collections.synchronizedList(new ArrayList());
  private Map pingsInProcess = Collections.synchronizedMap(new HashMap());

  private LoggingService logger;
  private UIDService uidService = null;
  protected EventService eventService;
  private List listeners = new ArrayList();
  private SensorFactory sensorFactory;

  private IncrementalSubscription pingRequests;
  private UnaryPredicate pingRequestPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof PingRequest) {
        PingRequest pr = (PingRequest)o;
        return (pingsInProcess.keySet().contains(pr.getUID()));
      }
      return false;
  }};

  public PingHelper(BindingSite bs) {
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
      if (logger.isErrorEnabled()) {
        logger.error("Unable to get 'sensors' domain");
      }
    }
  }

  public void setupSubscriptions() {
    // Subscribe to PingRequests to receive ping results
    pingRequests =
      (IncrementalSubscription)blackboard.subscribe(pingRequestPredicate);
  }

  public void execute() {
    sendPings();

    // Get PingRequests
    for (Iterator it = pingRequests.getChangedCollection().iterator(); it.hasNext();) {
      PingRequest pr = (PingRequest) it.next();
      int status = pr.getStatus();
      switch (status) {
        case PingRequest.RECEIVED:
          pingComplete(pr.getUID(), pr.getTarget(), PingResult.SUCCESS, pr.getRoundTripTime());
          blackboard.publishRemove(pr);
          break;
        case PingRequest.FAILED:
          pingComplete(pr.getUID(), pr.getTarget(), PingResult.FAIL, pr.getRoundTripTime());
          break;
      }
    }
  }

  public void ping(String[] agentNames, final long timeout, final PingListener pl) {
    if (logger.isDebugEnabled()) {
      Set agentsToPing = new HashSet();
      for (int i = 0; i < agentNames.length; i++) agentsToPing.add(agentNames[i]);
      logger.debug("ping: agents=" + agentsToPing + " timeout=" + timeout);
    }
    queuePing(new QueueEntry(agentNames, timeout, pl));
  }

  private void pingComplete(UID uid, MessageAddress agent, int status, long roundTripTime) {
    QueueEntry qe = (QueueEntry)pingsInProcess.remove(uid);
    qe.pingResults.add(new PingResult(agent.toString(), status, roundTripTime));
    qe.agentsToPing.remove(agent);
    if (qe.agentsToPing.isEmpty()) {
      if (logger.isDebugEnabled()) {
        logger.debug("pingsComplete:" + qe.pingResults);
      }
      qe.listener.pingComplete((PingResult[])qe.pingResults.toArray(new PingResult[0]));
    }
  }

  public boolean pingInProcess(String agentName) {
    List activePings = new ArrayList();
    synchronized (pingsInProcess) {
      activePings.addAll(pingsInProcess.values());
    }
    for (Iterator it = activePings.iterator(); it.hasNext();) {
      QueueEntry activePing = (QueueEntry)it.next();
      if (activePing.agentsToPing.contains(MessageAddress.getMessageAddress(agentName))) return true;
    }
    return false;
  }

  protected void queuePing(QueueEntry qe) {
    synchronized (pingQueue) {
      pingQueue.add(qe);
    }
    if (blackboard != null) {
      blackboard.signalClientActivity();
    }
  }

  private void sendPings() {
    List l = new ArrayList();
    while ((MAX_CONCURRENT_PINGS == -1 ||
           pingsInProcess.size() < MAX_CONCURRENT_PINGS) &&
           !pingQueue.isEmpty()) {
      QueueEntry qe = (QueueEntry) pingQueue.remove(0);
      MessageAddress agentsToPing[] = (MessageAddress[]) qe.agentsToPing.
          toArray(new MessageAddress[0]);
      for (int i = 0; i < agentsToPing.length; i++) {
        PingRequest pr = sensorFactory.newPingRequest(agentId,
            agentsToPing[i],
            qe.timeout);
        pingsInProcess.put(pr.getUID(), qe);
        if (agentsToPing[i].equals(agentId)) { // Can't ping self, report success
          pingComplete(pr.getUID(), agentsToPing[i], PingResult.SUCCESS, 0l);
        } else {
          blackboard.publishAdd(pr);
        }
      }
    }
  }


  static class QueueEntry {
    long timeout;
    PingResult pr[];
    PingListener listener;
    Set agentsToPing = new HashSet();
    List pingResults = new ArrayList();
    QueueEntry(String agentNames[], long to, PingListener l) {
      for (int i = 0; i < agentNames.length; i++) {
        agentsToPing.add(MessageAddress.getMessageAddress(agentNames[i]));
      }
      this.timeout = to;
      this.listener = l;
    }
  }

}
