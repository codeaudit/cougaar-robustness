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
 * Helper class for sending ping to remote agent.
 */

public class PingHelper extends BlackboardClientComponent {

  public static final int SUCCESS = 0;
  public static final int FAIL = 1;

  private Map myUIDs = new HashMap();
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
        return (myUIDs.keySet().contains(pr.getUID()));
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
      logger.error("Unable to get 'sensors' domain");
    }
  }

  public void setupSubscriptions() {
    // Subscribe to PingRequests to receive ping results
    pingRequests =
      (IncrementalSubscription)blackboard.subscribe(pingRequestPredicate);
  }

  public void execute() {
    // Get PingRequests
    for (Iterator it = pingRequests.getChangedCollection().iterator(); it.hasNext();) {
      PingRequest pr = (PingRequest) it.next();
      int status = pr.getStatus();
      if (status == PingRequest.RECEIVED || status == PingRequest.FAILED) {
        PingListener pl = (PingListener)myUIDs.remove(pr.getUID());
        logger.debug("PingAgent:" +
                    " agent=" + pr.getTarget().toString() +
                    " status=" + (status == PingRequest.RECEIVED
                                    ? "SUCCESS"
                                    : "FAIL"));
        pl.pingComplete(pr.getTarget().toString(),
                        (status == PingRequest.RECEIVED
                             ? SUCCESS
                             : FAIL));
        blackboard.publishRemove(pr);
      }
    }
  }

  public void ping(final String agentName, final long timeout, final PingListener pl) {
    ThreadService ts =
        (ThreadService) getServiceBroker().getService(this, ThreadService.class, null);
    Schedulable pingThread = ts.getThread(this, new Runnable() {
      public void run() {
        logger.debug("PingAgent:" +
                    " agent=" + agentName);
        PingRequest pr = sensorFactory.newPingRequest(agentId,
                            SimpleMessageAddress.getMessageAddress(agentName),
                            timeout);
        myUIDs.put(pr.getUID(), pl);
        blackboard.openTransaction();
        blackboard.publishAdd(pr);
        blackboard.closeTransaction();
      }
    }, "PingThread");
    getServiceBroker().releaseService(this, ThreadService.class, ts);
    pingThread.start();
  }
}