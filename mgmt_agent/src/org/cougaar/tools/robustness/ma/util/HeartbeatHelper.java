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
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.service.UIDService;

import org.cougaar.core.thread.Schedulable;

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

  private List myUIDs = new ArrayList();
  private LoggingService logger;
  private UIDService uidService = null;
  protected EventService eventService;
  private List listeners = new ArrayList();
  private SensorFactory sensorFactory;

  private IncrementalSubscription heartbeatRequests;
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
    heartbeatRequests =
      (IncrementalSubscription)blackboard.subscribe(heartbeatRequestPredicate);
    // Subscribe to HeartbeatHealthReports to receive notification of failed
    // heartbeats
    heartbeatHealthReports =
      (IncrementalSubscription)blackboard.subscribe(heartbeatPredicate);
  }

  public void execute() {
    // Get HeartbeatRequests
    for (Iterator it = heartbeatRequests.getChangedCollection().iterator(); it.hasNext();) {
      HeartbeatRequest hbr = (HeartbeatRequest) it.next();
      int status = hbr.getStatus();
      if (status == HeartbeatRequest.ACCEPTED) {
        heartbeatsStarted(hbr.getTarget());
      } else if (status == HeartbeatRequest.FAILED || status == HeartbeatRequest.REFUSED) {
        heartbeatFailure(hbr.getTarget());
      }
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
    ThreadService ts =
        (ThreadService) getServiceBroker().getService(this, ThreadService.class, null);
    Schedulable heartbeatThread = ts.getThread(this, new Runnable() {
      public void run() {
        logger.debug("Starting heartbeats:" +
                    " agent=" + agentName);
        MessageAddress target = SimpleMessageAddress.getMessageAddress(agentName);
        HeartbeatRequest hbr =
            sensorFactory.newHeartbeatRequest(agentId,
                                              target,
                                              hbrTimeout,
                                              hbFrequency,
                                              hbTimeout,
                                              true,
                                              pctOutOfSpec);
        myUIDs.add(hbr.getUID());
        blackboard.openTransaction();
        blackboard.publishAdd(hbr);
        blackboard.closeTransaction();
      }
    }, "HeartbeatThread");
    getServiceBroker().releaseService(this, ThreadService.class, ts);
    heartbeatThread.start();
  }

  /**
   * Add a HeartbeatListener.
   * @param hbl  HeartbeatListener to add
   */
  public void addListener(HeartbeatListener hbl) {
    synchronized (listeners) {
      if (!listeners.contains(hbl))
        listeners.add(hbl);
    }
  }

  /**
   * Remove a HeartbeatListener.
   * @param ml  HeartbeatListener to remove
   */
  public void removeListener(HeartbeatListener hbl) {
    synchronized (listeners) {
      if (listeners.contains(hbl))
        listeners.remove(hbl);
    }
  }

  /**
   * Notify HeartbeatListeners.
   */
  private void heartbeatFailure(MessageAddress agent) {
    logger.debug("Heartbeat failed: agent=" + agent);
    synchronized (listeners) {
      for (Iterator it = listeners.iterator(); it.hasNext(); ) {
        HeartbeatListener hbl = (HeartbeatListener)it.next();
        hbl.heartbeatFailure(agent.toString());
      }
    }
  }

  private void heartbeatsStarted(MessageAddress agent) {
    logger.debug("Heartbeats started: agent=" + agent);
    synchronized (listeners) {
      for (Iterator it = listeners.iterator(); it.hasNext(); ) {
        HeartbeatListener hbl = (HeartbeatListener)it.next();
        hbl.heartbeatStarted(agent.toString());
      }
    }
  }
}
