package org.cougaar.tools.robustness.ma.util;

import org.cougaar.tools.robustness.ma.CommunityStatusModel;
import org.cougaar.tools.robustness.ma.controllers.RobustnessController;
import org.cougaar.tools.robustness.threatalert.*;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.component.BindingSite;

import java.util.*;

/**
 */

public class ThreatAlertHandler implements ThreatAlertListener {

  private String thisAgent;
  private CommunityStatusModel model;
  private RobustnessController controller;
  private MoveHelper moveHelper;
  private LoggingService logger;

  public ThreatAlertHandler(String               thisAgent,
                            RobustnessController controller,
                            CommunityStatusModel model) {
    this.thisAgent = thisAgent;
    this.model = model;
    this.controller = controller;
    this.moveHelper = controller.getMoveHelper();
    BindingSite bs = controller.getBindingSite();
    logger =
      (LoggingService)bs.getServiceBroker().getService(this, LoggingService.class, null);
    logger = org.cougaar.core.logging.LoggingServiceWithPrefix.add(logger, thisAgent + ": ");
  }

  public void newAlert(ThreatAlert ta) {
    if (thisAgent.equals(preferredLeader())) {
      logger.info("Received ThreatAlert: " + ta);
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
    moveHelper.addListener(new MoveListener() {
      public void moveInitiated(String agentName, String origNode, String destNode) {}

      public void moveComplete(String agentName, String origNode,
                               String destNode, int status) {
        agentsToMove.remove(agentName);
        if (agentsToMove.isEmpty()) {
          logger.info("Vacate nodes complete: nodes=" + nodesToVacate);
          moveHelper.removeListener(this);
        }
      }
    });
    List moveList = new ArrayList(agentsToMove);
    for (Iterator it1 = moveList.iterator(); it1.hasNext(); ) {
      String agentToMove = (String) it1.next();
      String dest =
          RestartDestinationLocator.getRestartLocation(agentToMove,
          nodesToVacate);
      moveHelper.moveAgent(agentToMove, model.getLocation(agentToMove), dest,
                           model.getCommunityName());
    }
  }

  public String preferredLeader() {
    return model.getStringAttribute(model.MANAGER_ATTR);
  }

}