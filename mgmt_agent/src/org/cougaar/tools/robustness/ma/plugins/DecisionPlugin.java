/*
 * <copyright>
 *  Copyright 1997-2001 Mobile Intelligence Corp
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 *
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

import java.util.*;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.SimplePlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.BlackboardService;

import org.cougaar.util.UnaryPredicate;

/**
 * This plugin determines an appropriate course of action to fix agents that
 * have been placed into a HEALTH_CHECK state by the HealthMonitor plugin.  In
 * cases where the monitored agent is determined to be dead this plugin will
 * initiate a restart.
 */
public class DecisionPlugin extends SimplePlugin {

  private LoggingService log;
  private BlackboardService bbs = null;

  protected void setupSubscriptions() {

    log =  (LoggingService) getBindingSite().getServiceBroker().
      getService(this, LoggingService.class, null);

    bbs = getBlackboardService();

    // Subscribe to ManagementAgentProperties to receive parameter changes
    mgmtAgentProps =
      (IncrementalSubscription)bbs.subscribe(propertiesPredicate);

    // Subscribe to HeartbeatStatus objects to receive notification of
    // agents that have been placed into the "HEALTH_CHECK" state
    healthStatus =
      (IncrementalSubscription)bbs.subscribe(healthStatusPredicate);


  }

  public void execute() {

    // Get Parameter changes
    for (Iterator it = mgmtAgentProps.getAddedCollection().iterator();
         it.hasNext();) {
      ManagementAgentProperties props = (ManagementAgentProperties)it.next();
      //updateParams(props);
    }

     // Get HealtStatus objects
    for (Iterator it = healthStatus.getChangedCollection().iterator();
         it.hasNext();) {
      HealthStatus hs = (HealthStatus)it.next();
      log.debug("Received HEALTH_CHECK for agent " + hs.getAgentId());
      evaluate(hs);
    }
  }

  /**
   * Evaluate the stat
   * @param hs
   */
  private void evaluate(HealthStatus hs) {
    int status = hs.getStatus();
    switch (status) {
      case HealthStatus.NO_RESPONSE:
        // Agent is most likely dead.  Initiate a restart.
        hs.setState(HealthStatus.INITIAL);
        hs.setStatus(HealthStatus.UNDEFINED);
        hs.setHeartbeatRequestStatus(HealthStatus.UNDEFINED);
        System.out.println("Changing run state of agent '" +
          hs.getAgentId() + "' to INITIAL");
        bbs.publishChange(hs);
        break;
      case HealthStatus.DEGRADED:
        // Agent is alive but operating under stress.  For now just increase
        // the Heartbeat failure rate threshold.  Eventually this should
        // include logic to determine if the agent is simply busy or if there
        // is a hardware problem or external attack.
        adjustHbSensitivity(hs, 0.1f);  // Increase threshold by 10%
        hs.setState(HealthStatus.NORMAL);
        System.out.println("Changing run state of agent '" +
          hs.getAgentId() + "' to NORMAL");
        bbs.publishChange(hs);
        break;
      default:
    }
  }

  /**
   * Increase the HeartbeatFailureRateThreshold by specified value.
   * @param hs    HealthStatus object associated with monitored agent
   * @param value Adjustment value
   */
  private void adjustHbSensitivity(HealthStatus hs, float value) {
    float hbFailureRateThreshold = hs.getHbFailureRateThreshold();
    hbFailureRateThreshold = hbFailureRateThreshold * (1.0f + value);
    hs.setHbFailureRateThreshold(hbFailureRateThreshold);
  }


 /**
  * Predicate for HealthStatus objects
  */
  private IncrementalSubscription healthStatus;
  private UnaryPredicate healthStatusPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof HealthStatus) {
        HealthStatus hs = (HealthStatus)o;
        return hs.getState().equals(HealthStatus.HEALTH_CHECK);
      }
      return false;
  }};

  /**
   * Predicate for Management Agent properties
   */
  private IncrementalSubscription mgmtAgentProps;
  private UnaryPredicate propertiesPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof ManagementAgentProperties) {
        ManagementAgentProperties props = (ManagementAgentProperties)o;
        String myName = this.getClass().getName();
        String forName = props.getPluginName();
        return (myName.equals(forName) || myName.endsWith(forName));
      }
      return false;
  }};
}