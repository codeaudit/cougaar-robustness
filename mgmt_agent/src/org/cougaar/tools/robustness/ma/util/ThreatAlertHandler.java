package org.cougaar.tools.robustness.ma.util;

import org.cougaar.tools.robustness.ma.CommunityStatusModel;
import org.cougaar.tools.robustness.ma.StatusChangeListener;
import org.cougaar.tools.robustness.ma.CommunityStatusChangeEvent;

import org.cougaar.tools.robustness.ma.controllers.RobustnessController;
import org.cougaar.tools.robustness.threatalert.*;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.mts.MessageAddress;

import java.util.*;

/**
 */

public class ThreatAlertHandler extends ThreatAlertHandlerBase {

  private CommunityStatusModel model;
  private RobustnessController controller;
  private MoveHelper moveHelper;

  public ThreatAlertHandler(BindingSite          bs,
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
    logger.info("Received ThreatAlert: " + ta);
    if (agentId.toString().equals(preferredLeader())) {
      Set nodes = new HashSet();
      if (ta.getSeverityLevel() >= ThreatAlert.MEDIUM_SEVERITY) {
        Asset affectedAssets[] = ta.getAffectedAssets();
        for (int i = 0; i < affectedAssets.length; i++) {
          String type = affectedAssets[i].getAssetType();
          String id = affectedAssets[i].getAssetIdentifier();
          if (type != null && type.equalsIgnoreCase("Node") &&
              model.contains(id)) {
            nodes.add(id);
          }
        }
        if (!nodes.isEmpty()) {
          vacateNodes(nodes);
        }
      }
    }
  }

  private void vacateNodes(final Set nodesToVacate) {
    final Set agentsToMove = Collections.synchronizedSet(new HashSet());
    logger.info("Vacating nodes: " + nodesToVacate);
    for (Iterator it = nodesToVacate.iterator(); it.hasNext(); ) {
      String nodeName = (String) it.next();
      String agentsOnNode[] = model.agentsOnNode(nodeName);
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

  public String preferredLeader() {
    return model.getStringAttribute(model.MANAGER_ATTR);
  }

}