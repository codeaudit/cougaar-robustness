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
package org.cougaar.tools.robustness.ma;

import org.cougaar.tools.robustness.ma.CommunityStatusModel;
import org.cougaar.tools.robustness.ma.util.PersistenceHelper;

import org.cougaar.tools.robustness.ma.controllers.RobustnessController;
import org.cougaar.tools.robustness.threatalert.*;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.mts.MessageAddress;

/**
 * ThreatAlert handler to respond to changes in Security posture.
 */
public class SecurityAlertHandler extends RobustnessThreatAlertHandlerBase
   implements RestartManagerConstants {

  private PersistenceHelper persistenceHelper;

  public SecurityAlertHandler(BindingSite          bs,
                              MessageAddress       agentId,
                              RobustnessController controller,
                              CommunityStatusModel model) {
    super(bs, agentId, controller, model);
    persistenceHelper = new PersistenceHelper(bs);
  }

  public void newAlert(ThreatAlert ta) {
    if (ta instanceof SecurityAlert) {
      SecurityAlert sa = (SecurityAlert)ta;
      if (logger.isInfoEnabled()) {
        logger.info("Received new SecurityThreatAlert: " + sa);
      }
      if (agentId.toString().equals(preferredLeader())) {
        adjustRobustnessParameters(sa);
      }
    }
  }

  public void changedAlert(ThreatAlert ta) {
    if (ta instanceof SecurityAlert) {
      SecurityAlert sa = (SecurityAlert) ta;
      if (logger.isInfoEnabled()) {
        logger.info("SecurityThreatAlert changed: " + sa);
      }
      if (agentId.toString().equals(preferredLeader())) {
        adjustRobustnessParameters(sa);
      }
    }
  }

  public void removedAlert(ThreatAlert ta) {
    if (ta instanceof SecurityAlert) {
      SecurityAlert sa = (SecurityAlert) ta;
      if (logger.isInfoEnabled()) {
        logger.info("SecurityThreatAlert removed: " + sa);
      }
      if (agentId.toString().equals(preferredLeader())) {
        adjustRobustnessParameters(sa, true);  // Reset to default
      }
    }
  }

  /**
   * Adjust key robustness parameters based on new security level.
   * @param ta
   */
  protected void adjustRobustnessParameters(SecurityAlert sa) {
    adjustRobustnessParameters(sa, false);
  }

  protected void adjustRobustnessParameters(SecurityAlert sa, boolean resetToDefault) {
    if (logger.isInfoEnabled()) {
      logger.info("Adjusting robustness parameters: securityLevel=" +
                  sa.getSeverityLevelAsString() +
                  " reset=" + resetToDefault);
    }
    // TODO: Adjust robustness parameter values, if any
  }

  protected double getLongAttribute(String id, long defaultValue) {
    long value = model.getLongAttribute(id);
    return value != Long.MIN_VALUE ? value : defaultValue;
  }

  protected double getDoubleAttribute(String id, double defaultValue) {
    double value = model.getDoubleAttribute(id);
    return value != Double.NaN ? value : defaultValue;
  }
}
