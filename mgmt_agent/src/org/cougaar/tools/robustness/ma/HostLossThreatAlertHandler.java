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
 * ThreatAlert handler to respond to threats of imminent loss of a host
 * computer.
 */
public class HostLossThreatAlertHandler extends ThreatAlertHandlerBase {

  private CommunityStatusModel model;
  private RobustnessController controller;
  private MoveHelper moveHelper;

  public HostLossThreatAlertHandler(BindingSite          bs,
                                    MessageAddress       agentId,
                                    RobustnessController controller,
                                    CommunityStatusModel model) {
    super(bs, agentId);
    this.model = model;
    this.controller = controller;
    this.moveHelper = controller.getMoveHelper();
  }

  private void addChangeListener() {
  }

  public void newAlert(ThreatAlert ta) {
    if (ta instanceof HostLossThreatAlert) {
      logger.info("Received HostLossThreatAlert: " + ta);
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
        if (ta.getSeverityLevel() >= ThreatAlert.MEDIUM_SEVERITY) {
          vacate(affectedAgents, affectedNodes);
        }
        adjustParameters(ta, affectedAgents);
      }
    }
  }

  /**
   * Returns a Set of agent names residing on specified nodes.
   * @param locations  Node names
   * @return           Set of agent names
   */
  protected Set affectedAgents(Set nodes) {
    Set affectedAgents = new HashSet();
    for (Iterator it = nodes.iterator(); it.hasNext();) {
      String node = (String)it.next();
      affectedAgents.addAll(agentsOnNode(node));
    }
    return affectedAgents;
  }

  /**
   * Returns a Set of node names from a set of node and/or host names.
   * @param locations  Set of host and/or node names
   * @return           Node names
   */
  private Set resolveNodes(Set locations) {
    Set nodes = new HashSet();
    for (Iterator it = locations.iterator(); it.hasNext();) {
      String location = (String)it.next();
      if (model.getType(location) == model.NODE) {
        nodes.add(location);
      } else {  // Host
        String nodeNames[] = model.entitiesAtLocation(location);
        for (int i = 0; i < nodeNames.length; i++) {
          nodes.add(nodeNames[i]);
        }
      }
    }
    return nodes;
  }

  /**
   * Returns a Set of all agents on specified node.
   * @param nodeName
   * @return   Set of agent names
   */
  private Set agentsOnNode(String nodeName) {
    Set agents = new HashSet();
    String[] agentsOnNode = model.entitiesAtLocation(nodeName);
    for (int i = 0; i < agentsOnNode.length; i++) {
      agents.add(agentsOnNode[i]);
    }
    return agents;
  }

  /**
   * Move specified agents to new locations.
   * @param agentsToMove
   * @param nodesToVacate
   */
  private void vacate(final Set agentsToMove, final Set nodesToVacate) {
    logger.info("Vacating nodes: " + nodesToVacate);
    for (Iterator it = nodesToVacate.iterator(); it.hasNext(); ) {
      String nodeName = (String) it.next();
      String agentsOnNode[] = model.entitiesAtLocation(nodeName);
      for (int i = 0; i < agentsOnNode.length; i++) {
        agentsToMove.add(agentsOnNode[i]);
      }
    }
    model.addChangeListener(new StatusChangeListener() {
      public void statusChanged(CommunityStatusChangeEvent[] csce) {
        for (int i = 0; i < csce.length; i++) {
          if (csce[i].locationChanged() &&
              agentsToMove.contains(csce[i].getName()) &&
              !nodesToVacate.contains(csce[i].getCurrentLocation())) {
            agentsToMove.remove(csce[i].getName());
            if (agentsToMove.isEmpty()) {
              logger.info("Vacate nodes complete: nodes=" + nodesToVacate);
              model.removeChangeListener(this);
              logger.info("Starting Load Balancer");
              controller.getLoadBalancer().doLoadBalance();
            }
          }
        }
      }
    });
    List moveList = new ArrayList(agentsToMove);
    for (Iterator it1 = moveList.iterator(); it1.hasNext(); ) {
      String agentToMove = (String) it1.next();
      String dest =
          RestartDestinationLocator.getRestartLocation(agentToMove,
          nodesToVacate);
      logger.debug("Move agent: agent=" + agentToMove + " dest=" + dest);
      moveHelper.moveAgent(agentToMove, model.getLocation(agentToMove), dest,
                           model.getCommunityName());
    }
  }

  /**
   * Adjust key robustness parameters based on new threat level.
   * @param ta
   */
  protected void adjustParameters(ThreatAlert ta, Set affectedAgents) {
    logger.info("Adjusting robustness parameters: threatLevel=" + ta.getSeverityLevelAsString());
    CommunityService cs =
        (CommunityService) bindingSite.getServiceBroker().getService(this, CommunityService.class, null);
    Attributes attrs = cs.getCommunityAttributes(model.getCommunityName());
    Attribute intervalAttr = attrs.get("UPDATE_INTERVAL");
    if (intervalAttr == null) {
      attrs.remove("UPDATE_INTERVAL");
    }
    attrs.put(new BasicAttribute("UPDATE_INTERVAL", "15000"));
    cs.setCommunityAttributes(model.getCommunityName(), attrs);
  }

  protected String preferredLeader() {
    return model.getStringAttribute(model.MANAGER_ATTR);
  }

}