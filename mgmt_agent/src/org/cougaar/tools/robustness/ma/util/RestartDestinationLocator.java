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
  static Map preferredRestartLocations = Collections.synchronizedMap(new HashMap());


  public RestartDestinationLocator() {
  }

  public static void setCommunityStatusModel(CommunityStatusModel cmodel) {
    model = cmodel;
  }

  public static void setLoggingService(LoggingService ls) {
    logger = ls;
  }

  /**
   * Defines the preferred location for a future agent restart.
   * @param preferredLocations  Map of agent names (key) and node name of
   *                            preferred restart destination
   */
  public static void setPreferredRestartLocations(Map preferredLocations) {
    preferredRestartLocations.putAll(preferredLocations);
  }

  public static String getRestartLocation(String agent, Set excludedNodes) {
    if(selectedNodes.containsKey(agent)) {
      LinkedList nodes = (LinkedList)selectedNodes.get(agent);
      if(nodes.size() > 0) {
        return (String) nodes.removeFirst();
      } else {
        selectedNodes.remove(agent);
        return null;
      }
    }

    String activeNodes[] =
        model.listEntries(model.NODE, DefaultRobustnessController.ACTIVE);
    Set candidateNodes = new HashSet();
    for (int i = 0; i < activeNodes.length; i++) {
      if (!excludedNodes.contains(activeNodes[i])) {
          candidateNodes.add(activeNodes[i]);
      }
    }
    int numAgents = 0;
    String selectedNode = null;
    Hashtable temp = new Hashtable();
    for (Iterator it = candidateNodes.iterator(); it.hasNext();) {
      String candidate = (String)it.next();
      int agentsOnNode = model.entitiesAtLocation(candidate).length;
      if(logger.isDebugEnabled())
        logger.debug("agents on node: " + candidate + "  " + agentsOnNode);
      if(restartAgents.containsKey(candidate)){
        int restarts = Integer.parseInt((String)(restartAgents.get(candidate)));
        agentsOnNode += restarts;
      }
      temp.put(candidate, Integer.toString(agentsOnNode));
      if (selectedNode == null || agentsOnNode < numAgents) {
        selectedNode = candidate;
        numAgents = agentsOnNode;
      }
    }
    if (preferredRestartLocations.containsKey(agent)) {
      selectedNode = (String)preferredRestartLocations.remove(agent);
      logger.info("Using preferredRestartLocation: agent=" + agent + " dest=" + selectedNode);
    }
    temp.remove(selectedNode); //this one is already used this time, don't count it.

    LinkedList list = new LinkedList();
    for(int i=0; i<temp.size(); i++) {
      String next = getNextNode(temp);
      list.addLast(next);
      temp.remove(next);
    }
    if(logger.isDebugEnabled())
      logger.debug("other available restart nodes: " + list);
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