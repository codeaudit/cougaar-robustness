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

import org.cougaar.tools.robustness.ma.controllers.RobustnessController;

import org.cougaar.tools.robustness.ma.ldm.HealthMonitorRequest;

import org.cougaar.core.blackboard.BlackboardClientComponent;
import org.cougaar.core.blackboard.IncrementalSubscription;

import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.AlarmService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.EventService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.SchedulerService;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ServiceBroker;

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
 * Provides interface for initiating load balancing across community nodes.
 */
public class LoadBalancer extends BlackboardClientComponent {

  private LoggingService logger;
  protected EventService eventService;
  private MoveHelper moveHelper;
  private RobustnessController controller;

  // Subscription to HealthMonitorRequests for load balancing
  private IncrementalSubscription healthMonitorRequests;
  private UnaryPredicate healthMonitorRequestPredicate = new UnaryPredicate() {
    public boolean execute (Object o) {
      if (o instanceof HealthMonitorRequest) {
        HealthMonitorRequest hmr = (HealthMonitorRequest)o;
        return (hmr.getRequestType() == hmr.LOAD_BALANCE);
      }
      return false;
  }};

/**
 * Constructor requires BindingSite to initialize needed services.
 * @param bs
 */
  public LoadBalancer(BindingSite bs, RobustnessController controller) {
    this.setBindingSite(bs);
    this.controller = controller;
    this.moveHelper = controller.getMover();
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
    super.load();
  }

  public void start() {
    super.start();
  }

  /**
   * Subscribe to mobility AgentControl objects and remote HealthMonitorRequests.
   */
  public void setupSubscriptions() {
    healthMonitorRequests =
        (IncrementalSubscription)blackboard.subscribe(healthMonitorRequestPredicate);
  }

  public void execute() {
    for (Iterator it = healthMonitorRequests.getAddedCollection().iterator(); it.hasNext(); ) {
      HealthMonitorRequest hsm = (HealthMonitorRequest) it.next();
      logger.debug("Received HealthMonitorRequest:" + hsm);
      if (hsm.getRequestType() == HealthMonitorRequest.LOAD_BALANCE) {
        doLoadBalance();
      }
    }
  }

  /**
   * Submit request to EN for new community laydown and perform required moves.
   */
  public void doLoadBalance() {
    // TODO: submit request to EN plugin and send move requests to moveHelper
    //       upon receipt of EN response
    //controller.getCompleteStatus()
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

}
