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
import org.cougaar.tools.robustness.ma.util.RestartDestinationLocator;

import org.cougaar.tools.robustness.ma.controllers.RobustnessController;
import org.cougaar.tools.robustness.threatalert.*;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.mts.MessageAddress;

import org.cougaar.core.service.community.CommunityService;

import java.util.*;
import javax.naming.directory.Attributes;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;

/**
 * ThreatAlert handler to respond to changes in Security posture.
 */
public class SecurityAlertHandler extends RobustnessThreatAlertHandlerBase {

  public SecurityAlertHandler(BindingSite          bs,
                              MessageAddress       agentId,
                              RobustnessController controller,
                              CommunityStatusModel model) {
    super(bs, agentId, controller, model);
  }

  public void newAlert(ThreatAlert ta) {
    if (ta instanceof SecurityAlert) {
      SecurityAlert sa = (SecurityAlert)ta;
      logger.info("Received SecurityThreatAlert: " + sa);
      if (agentId.toString().equals(preferredLeader())) {
        Set affectedNodes = new HashSet();
        Set affectedAgents = new HashSet();
        Set nodesAndHosts = new HashSet();
        Asset affectedAssets[] = ta.getAffectedAssets();
        for (int i = 0; i < affectedAssets.length; i++) {
          String type = affectedAssets[i].getAssetType();
          String id = affectedAssets[i].getAssetIdentifier();
          if (type != null &&
              (type.equalsIgnoreCase("Node") || type.equalsIgnoreCase("Node"))) {
            nodesAndHosts.add(id);
          }
         if (!nodesAndHosts.isEmpty()) {
            affectedNodes.addAll(resolveNodes(nodesAndHosts));
            affectedAgents.addAll(affectedAgents(affectedNodes));
          }
        }
        adjustRobustnessParameters(sa, affectedAgents);
      }
    }
  }

  /**
   * Adjust key robustness parameters based on new security level.
   * @param ta
   */
  protected void adjustRobustnessParameters(SecurityAlert sa, Set affectedAgents) {
    logger.info("Adjusting robustness parameters: securityLevel=" + sa.getSeverityLevelAsString());
    CommunityService cs =
        (CommunityService) bindingSite.getServiceBroker().getService(this, CommunityService.class, null);
    Attributes attrs = cs.getCommunityAttributes(model.getCommunityName());
    changeAttribute(attrs, "UPDATE_INTERVAL", "15000");
    cs.setCommunityAttributes(model.getCommunityName(), attrs);
  }

}