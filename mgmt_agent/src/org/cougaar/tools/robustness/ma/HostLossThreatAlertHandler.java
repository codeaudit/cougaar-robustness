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

import org.cougaar.tools.robustness.ma.RestartManagerConstants;
import org.cougaar.tools.robustness.ma.CommunityStatusModel;
import org.cougaar.tools.robustness.ma.StatusChangeListener;
import org.cougaar.tools.robustness.ma.CommunityStatusChangeEvent;
import org.cougaar.tools.robustness.ma.util.LoadBalancer;
import org.cougaar.tools.robustness.ma.util.LoadBalancerListener;

import org.cougaar.tools.robustness.ma.controllers.RobustnessController;
import org.cougaar.tools.robustness.threatalert.*;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.mts.MessageAddress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * ThreatAlert handler to respond to threats of imminent loss of a host
 * computer.
 */
public class HostLossThreatAlertHandler extends RobustnessThreatAlertHandlerBase
  implements RestartManagerConstants {

  public static final int ALERT_LEVEL_FOR_VACATE = ThreatAlert.HIGH_SEVERITY;

  public HostLossThreatAlertHandler(BindingSite          bs,
                                    MessageAddress       agentId,
                                    RobustnessController controller,
                                    CommunityStatusModel model) {
    super(bs, agentId, controller, model);
  }

  public void newAlert(ThreatAlert ta) {
    if (ta instanceof HostLossThreatAlert) {
      if (logger.isInfoEnabled()) {
        logger.info("Received new HostLossThreatAlert: " + ta);
      }
      if (agentId.toString().equals(preferredLeader()) && ta.isActive()) {
        Set affectedNodes = getAffectedNodes(ta.getAffectedAssets());
        if (ta.getSeverityLevel() >= ALERT_LEVEL_FOR_VACATE) {
          vacate(affectedNodes);
        } else {
          adjustRobustnessParameters(ta, affectedAgents(affectedNodes));
        }
      }
    }
  }

  public void changedAlert(ThreatAlert ta) {
    if (ta instanceof HostLossThreatAlert) {
      if (logger.isInfoEnabled()) {
        logger.info("Received changed HostLossThreatAlert: " + ta);
      }
      if (agentId.toString().equals(preferredLeader()) && ta.isActive()) {
        Set affectedNodes = getAffectedNodes(ta.getAffectedAssets());
        if (ta.getSeverityLevel() >= ALERT_LEVEL_FOR_VACATE) {
          vacate(affectedNodes);
        } else {
          adjustRobustnessParameters(ta, affectedAgents(affectedNodes));
        }
      }
    }
  }

  public void removedAlert(ThreatAlert ta) {
    if (ta instanceof HostLossThreatAlert) {
      if (logger.isInfoEnabled()) {
        logger.info("Received removed HostLossThreatAlert: " + ta);
      }
      if (agentId.toString().equals(preferredLeader()) && ta.isExpired()) {
        Set affectedNodes = getAffectedNodes(ta.getAffectedAssets());
        if (ta.getSeverityLevel() >= ALERT_LEVEL_FOR_VACATE) {
          LoadBalancerListener lbl = new LoadBalancerListener() {
            public void layoutReady(Map layout) {
              controller.getLoadBalancer().moveAgents(layout);
            }
          };
          controller.getLoadBalancer().doLayout(RestartManagerConstants.DEFAULT_LOAD_BALANCER_MODE,
                                                RestartManagerConstants.ANNEAL_TIME,
                                                LoadBalancer.DEFAULT_HAMMING,
                                                new ArrayList(affectedNodes),
                                                Collections.EMPTY_LIST,
                                                new ArrayList(getExcludedNodes()),
                                                lbl);
        } else {
          adjustRobustnessParameters(ta, affectedAgents(affectedNodes));
        }
      }
    }
  }

  /**
   * Move specified agents to new locations.
   * @param agentsToMove
   * @param nodesToVacate
   */
  private void vacate(final Set nodesToVacate) {
    final Set agentsToMove = affectedAgents(nodesToVacate);
    if (logger.isInfoEnabled()) {
      logger.info("Vacating nodes: " + nodesToVacate);
    }
    model.addChangeListener(new StatusChangeListener() {
      public void statusChanged(CommunityStatusChangeEvent[] csce) {
        for (int i = 0; i < csce.length; i++) {
          if (csce[i].locationChanged() &&
              agentsToMove.contains(csce[i].getName()) &&
              !nodesToVacate.contains(csce[i].getCurrentLocation())) {
            agentsToMove.remove(csce[i].getName());
            if (agentsToMove.isEmpty()) {
              if (logger.isInfoEnabled()) {
                logger.info("Vacate nodes complete: nodes=" + nodesToVacate);
              }
              model.removeChangeListener(this);
            }
          }
        }
      }
    });
    LoadBalancerListener lbl = new LoadBalancerListener() {
      public void layoutReady(Map layout) {
        controller.getLoadBalancer().moveAgents(layout);
      }
    };
    controller.getLoadBalancer().doLayout(RestartManagerConstants.DEFAULT_LOAD_BALANCER_MODE,
                                          RestartManagerConstants.ANNEAL_TIME,
                                          LoadBalancer.DEFAULT_HAMMING,
                                          Collections.EMPTY_LIST,
                                          new ArrayList(nodesToVacate),
                                          new ArrayList(getExcludedNodes()),
                                          lbl);
  }

  /**
   * Adjust key robustness parameters based on new threat level.
   * @param ta
   */
  protected void adjustRobustnessParameters(ThreatAlert ta, Set affectedAgents) {
    if (logger.isInfoEnabled()) {
      logger.info("Adjusting robustness parameters: threatLevel=" +
                  ta.getSeverityLevelAsString());
    }
    // TODO: Adjust parameters, if any
  }

  protected Set getAffectedNodes(Asset[] affectedAssets) {
    Set nodesAndHosts = new HashSet();
    for (int i = 0; i < affectedAssets.length; i++) {
      String type = affectedAssets[i].getAssetType();
      String id = affectedAssets[i].getAssetIdentifier();
      if (type != null &&
          (type.equalsIgnoreCase("Node") || type.equalsIgnoreCase("Host"))) {
        nodesAndHosts.add(id);
      }
    }
    if (!nodesAndHosts.isEmpty()) {
      return resolveNodes(nodesAndHosts);
    } else {
      return Collections.EMPTY_SET;
    }
  }

  protected Set getExcludedNodes() {
    Set excludedNodes = new HashSet();
    String allNodes[] = model.listEntries(CommunityStatusModel.NODE);
    for (int i = 0; i < allNodes.length; i++) {
      if (model.hasAttribute(model.getAttributes(allNodes[i]), "UseForRestarts", "False")) {
        excludedNodes.add(allNodes[i]);
      }
    }
    return excludedNodes;
  }

}
