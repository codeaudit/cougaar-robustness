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
package org.cougaar.tools.robustness.ma.plugins;

import org.cougaar.tools.robustness.threatalert.*;
import org.cougaar.tools.robustness.ma.SecurityAlert;
import org.cougaar.tools.robustness.ma.util.CommunityFinder;

import org.cougaar.core.adaptivity.InterAgentOperatingMode;
import org.cougaar.core.adaptivity.OMCRangeList;

import org.cougaar.core.security.constants.AdaptiveMnROperatingModes;

import org.cougaar.core.plugin.ComponentPlugin;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceAvailableListener;
import org.cougaar.core.component.ServiceAvailableEvent;

import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.community.CommunityService;

import org.cougaar.core.blackboard.IncrementalSubscription;

import org.cougaar.util.UnaryPredicate;

import java.util.Collection;
import java.util.Iterator;

/**
 * Generates a SecurityAlert upon detection of a security generated
 * InterAgentOperatingMode containing a mode of
 * AdaptiveMnROperatingModes.PREVENTIVE_MEASURE_POLICY.
 */
public class SecurityAlertPlugin extends ComponentPlugin {

  private LoggingService logger;
  private CommunityService commService;
  private CommunityFinder finder;
  private ThreatAlertService tas;
  private SecurityAlert securityAlert;

  public void setCommunityService (CommunityService cs) {
    commService = cs;
  }

  protected String getCommunity() {
    return finder.getCommunityName();
  }

  public void setupSubscriptions() {
    logger =
      (LoggingService)getBindingSite().getServiceBroker().getService(this, LoggingService.class, null);
    logger = org.cougaar.core.logging.LoggingServiceWithPrefix.add(logger, agentId + ": ");
    finder = new CommunityFinder.ForManager(commService, agentId);

    ServiceBroker sb = getBindingSite().getServiceBroker();
    if (sb.hasService(ThreatAlertService.class)) {
      tas =
          (ThreatAlertService) sb.getService(this, ThreatAlertService.class, null);
    } else {
      sb.addServiceListener(new ServiceAvailableListener() {
        public void serviceAvailable(ServiceAvailableEvent sae) {
          if (sae.getService().equals(ThreatAlertService.class)) {
            tas =
                (ThreatAlertService) getBindingSite().getServiceBroker().getService(this, ThreatAlertService.class, null);
          }
        }
      });
    }

    // Subscribe to InterAgentOperatingMode objects
    opModes =
        (IncrementalSubscription)blackboard.subscribe(_threatConOM);
  }

  public void execute() {
    Collection requests = opModes.getAddedCollection();
    requests.addAll(opModes.getAddedCollection());
    for (Iterator it = requests.iterator(); it.hasNext();) {
      generateSecurityAlert((InterAgentOperatingMode)it.next());
    }
  }

  /**
   * Generate a SecurityAlert if an
   * AdaptiveMnROperatingModes.PREVENTIVE_MEASURE_POLICY == HIGH.
   * @param iaom
   */
  private void generateSecurityAlert(InterAgentOperatingMode iaom) {
    Comparable level = iaom.getValue();
    OMCRangeList allowedValues = iaom.getAllowedValues();
    if (level == allowedValues.getMax()) {
      if (securityAlert == null) {
        securityAlert = new SecurityAlert(SecurityAlert.HIGH_SEVERITY);
        tas.sendAlert(securityAlert, getCommunity(), "HealthMonitor");
      }
    } else {
      if (securityAlert != null) {
        tas.cancelAlert(securityAlert);
        securityAlert = null;
      }
    }
  }

  private IncrementalSubscription opModes;
  UnaryPredicate _threatConOM = new UnaryPredicate() {
    public boolean execute(Object o) {
      if(o == null || !(o instanceof InterAgentOperatingMode)) {
        return false;
      }
      InterAgentOperatingMode mode = (InterAgentOperatingMode)o;
      return mode.getName().equals(AdaptiveMnROperatingModes.PREVENTIVE_MEASURE_POLICY);
    }
  };

}