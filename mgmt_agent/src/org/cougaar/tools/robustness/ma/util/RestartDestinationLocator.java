package org.cougaar.tools.robustness.ma.util;

import org.cougaar.core.service.LoggingService;

import org.cougaar.tools.robustness.ma.StatusChangeListener;
import org.cougaar.tools.robustness.ma.CommunityStatusChangeEvent;
import org.cougaar.tools.robustness.ma.CommunityStatusModel;
import org.cougaar.tools.robustness.ma.controllers.DefaultRobustnessController;

import java.util.*;


public class RestartDestinationLocator {
  static CommunityStatusModel model;
  static LoggingService logger;
  static Hashtable restartAgents = new Hashtable();
  static Hashtable selectedNodes = new Hashtable();


  public RestartDestinationLocator() {
  }

  public static void setCommunityStatusModel(CommunityStatusModel cmodel) {
    model = cmodel;
  }

  public static void setLoggingService(LoggingService ls) {
    logger = ls;
  }

  public static String getRestartLocation(String agent) {
    if(selectedNodes.containsKey(agent)) {
      LinkedList nodes = (LinkedList)selectedNodes.get(agent);
      if(nodes.size() > 0) {
        return (String) nodes.removeFirst();
      }
      else {
        selectedNodes.remove(agent);
        return null;
      }
    }

    String candidateNodes[] =
        model.listEntries(model.NODE, DefaultRobustnessController.ACTIVE);
    int numAgents = 0;
    String selectedNode = null;
    Hashtable temp = new Hashtable();
    for (int i = 0; i < candidateNodes.length; i++) {
      int agentsOnNode = model.agentsOnNode(candidateNodes[i]).length;
      if(logger.isDebugEnabled())
        logger.debug("agents on node: " + candidateNodes[i] + "  " + agentsOnNode);
      if(restartAgents.containsKey(candidateNodes[i])){
        int restarts = Integer.parseInt((String)(restartAgents.get(candidateNodes[i])));
        agentsOnNode += restarts;
      }
      temp.put(candidateNodes[i], Integer.toString(agentsOnNode));
      if (selectedNode == null || agentsOnNode < numAgents) {
        selectedNode = candidateNodes[i];
        numAgents = agentsOnNode;
      }
    }
    temp.remove(selectedNode); //this one is already used this time, don't count it.

    LinkedList list = new LinkedList();
    for(int i=0; i<temp.size(); i++) {
      String next = getNextNode(temp);
      list.addLast(next);
      temp.remove(next);
    }
    if(logger.isDebugEnabled())
      logger.debug("other avaliable restart nodes: " + list);
    selectedNodes.put(agent, list);

    return selectedNode;
  }

  private static String getNextNode(Hashtable nodes) {
    String next = "";
    int number = 0;
    for(Iterator it = nodes.keySet().iterator(); it.hasNext();) {
      String key = (String)it.next();
      int agents = Integer.parseInt((String)nodes.get(key));
      if(number == 0 || agents < number) {
        number = agents;
        next = key;
      }
    }
    return next;
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

  public static void restartSuccess(String agentName) {
    if(selectedNodes.containsKey(agentName))
      selectedNodes.remove(agentName);
  }

}