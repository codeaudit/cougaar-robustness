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
import org.cougaar.tools.robustness.ma.StatusChangeListener;
import org.cougaar.tools.robustness.ma.CommunityStatusChangeEvent;
import org.cougaar.tools.robustness.ma.util.MoveHelper;
import org.cougaar.tools.robustness.ma.util.PersistenceHelper;
import org.cougaar.tools.robustness.ma.util.RestartDestinationLocator;

import org.cougaar.tools.robustness.ma.controllers.RobustnessController;
import org.cougaar.tools.robustness.threatalert.*;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.mts.MessageAddress;

import java.util.*;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;

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
      logger.info("Received new SecurityThreatAlert: " + sa);
      if (agentId.toString().equals(preferredLeader())) {
        adjustRobustnessParameters(sa);
      }
    }
  }

  public void changedAlert(ThreatAlert ta) {
    if (ta instanceof SecurityAlert) {
      SecurityAlert sa = (SecurityAlert) ta;
      logger.info("SecurityThreatAlert changed: " + sa);
      if (agentId.toString().equals(preferredLeader())) {
        adjustRobustnessParameters(sa);
      }
    }
  }

  public void removedAlert(ThreatAlert ta) {
    if (ta instanceof SecurityAlert) {
      SecurityAlert sa = (SecurityAlert) ta;
      logger.info("SecurityThreatAlert removed: " + sa);
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
    logger.info("Adjusting robustness parameters: securityLevel=" + sa.getSeverityLevelAsString() +
                " reset=" + resetToDefault);
    long persistenceInterval = model.getLongAttribute(PERSISTENCE_INTERVAL_ATTRIBUTE);
    double persistenceAdjustmentCoefficient = 1.0;
    if (!resetToDefault && sa.getSeverityLevel() > sa.MEDIUM_SEVERITY) {
      persistenceAdjustmentCoefficient = model.getDoubleAttribute(PERSISTENCE_INTERVAL_THREATCON_HIGH_COEFFICIENT);
    }
    persistenceInterval = (long)((double)persistenceInterval * persistenceAdjustmentCoefficient);
    Properties controls = new Properties();
    controls.setProperty("lazyInterval", Long.toString(persistenceInterval));
    persistenceHelper.controlPersistence(model.listEntries(model.AGENT), true, controls);

    long statusUpdateInterval = model.getLongAttribute(STATUS_UPDATE_INTERVAL_ATTRIBUTE);
    double pingThreatconCoefficient = 1.0;
    double statusUpdateAdjustmentCoefficient = 1.0;
    if (!resetToDefault && sa.getSeverityLevel() > sa.MEDIUM_SEVERITY) {
      statusUpdateAdjustmentCoefficient = getDoubleAttribute(STATUS_UPDATE_INTERVAL_THREATCON_HIGH_COEFFICIENT, statusUpdateAdjustmentCoefficient);
      pingThreatconCoefficient = getDoubleAttribute(PING_TIMEOUT_THREATCON_HIGH_COEFFICIENT, pingThreatconCoefficient);
    }
    statusUpdateInterval = (long)((double)statusUpdateInterval * statusUpdateAdjustmentCoefficient);
    Attribute mods[] =
        new Attribute[] {new BasicAttribute(STATUS_UPDATE_INTERVAL_ATTRIBUTE, Long.toString(statusUpdateInterval)),
                         new BasicAttribute(PING_ADJUSTMENT_ATTRIBUTE, Double.toString(pingThreatconCoefficient))};
    changeAttributes(model.getCommunityName(), null, mods);
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
