package org.cougaar.tools.robustness.ma.util;

import org.cougaar.core.service.LoggingService;

import org.cougaar.tools.robustness.ma.StatusChangeListener;
import org.cougaar.tools.robustness.ma.CommunityStatusChangeEvent;
import org.cougaar.tools.robustness.ma.CommunityStatusModel;
import org.cougaar.tools.robustness.ma.controllers.DefaultRobustnessController;

import java.util.Hashtable;


public class RestartDestinationLocator {
  static CommunityStatusModel model;
  static LoggingService logger;
  static Hashtable restartAgents = new Hashtable();


  public RestartDestinationLocator() {
  }

  public static void setCommunityStatusModel(CommunityStatusModel cmodel) {
    model = cmodel;
  }

  public static void setLoggingService(LoggingService ls) {
    logger = ls;
  }

  public static String getRestartLocation() {
    String candidateNodes[] =
        model.listEntries(model.NODE, DefaultRobustnessController.ACTIVE);
    int numAgents = 0;
    String selectedNode = null;
    for (int i = 0; i < candidateNodes.length; i++) {
      int agentsOnNode = model.agentsOnNode(candidateNodes[i]).length;
      logger.debug("agents on node: " + candidateNodes[i] + "  " + agentsOnNode);
      if(restartAgents.containsKey(candidateNodes[i])){
        int restarts = Integer.parseInt((String)(restartAgents.get(candidateNodes[i])));
        agentsOnNode += restarts;
      }
      if (selectedNode == null || agentsOnNode < numAgents) {
        selectedNode = candidateNodes[i];
        numAgents = agentsOnNode;
      }
    }
    return selectedNode;
  }

  public static void restartOneAgent(String nodeName) {
    if(restartAgents.containsKey(nodeName)) {
      int restarts = Integer.parseInt((String)(restartAgents.get(nodeName)));
      restarts += 1;
      restartAgents.put(nodeName, Integer.toString(restarts));
    }
    else {
      restartAgents.put(nodeName, "1");
    }
  }

  public static void clearRestarts() {
    restartAgents.clear();
  }

}