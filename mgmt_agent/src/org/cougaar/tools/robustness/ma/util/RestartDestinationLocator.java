package org.cougaar.tools.robustness.ma.util;

import org.cougaar.util.log.Logger;
import org.cougaar.util.log.LoggerFactory;

import org.cougaar.tools.robustness.ma.CommunityStatusModel;
import org.cougaar.tools.robustness.ma.controllers.DefaultRobustnessController;

import java.util.Map;
import java.util.Collections;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

public class RestartDestinationLocator {
  protected static Logger logger =
      LoggerFactory.getInstance().createLogger(RestartDestinationLocator.class);
  CommunityStatusModel model;
  Hashtable restartAgents = new Hashtable();
  Hashtable selectedNodes = new Hashtable();
  Map preferredRestartLocations = Collections.synchronizedMap(new HashMap());


  public RestartDestinationLocator(CommunityStatusModel model) {
    this.model = model;
  }

  /**
   * Defines the preferred location for a future agent restart.
   * @param preferredLocations  Map of agent names (key) and node name of
   *                            preferred restart destination
   */
  public void setPreferredRestartLocations(Map preferredLocations) {
    preferredRestartLocations.putAll(preferredLocations);
  }

  public String getRestartLocation(String agent, Set excludedNodes) {
    if (logger.isDebugEnabled()) {
      logger.debug("getRestartLocation: agent=" + agent +
                   " preselectedNodes=" + selectedNodes.get(agent) +
                   " excludedNodes=" + excludedNodes);
    }
    if (selectedNodes.containsKey(agent)) {
      LinkedList nodes = (LinkedList)selectedNodes.get(agent);
      nodes.removeAll(excludedNodes);
      if (logger.isDebugEnabled()) {
        logger.debug("getRestartLocation: agent=" + agent + " candidates=" + nodes);
      }
      if (nodes.size() > 0) {
        return (String) nodes.removeFirst();
      } else {
        selectedNodes.remove(agent);
        return null;
      }
    }

    String activeNodes[] =
        model.listEntries(CommunityStatusModel.NODE, DefaultRobustnessController.ACTIVE);
    Set candidateNodes = new HashSet();
    for (int i = 0; i < activeNodes.length; i++) {
      if (!excludedNodes.contains(activeNodes[i])) {
        candidateNodes.add(activeNodes[i]);
      }
    }
    if (logger.isDebugEnabled()) {
      logger.debug("getRestartLocation: agent=" + agent + " candidates=" +
                   candidateNodes);
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
    if (preferredRestartLocations.containsKey(agent) &&
        !excludedNodes.contains((String)preferredRestartLocations.get(agent))) {
      selectedNode = (String)preferredRestartLocations.remove(agent);
      if (logger.isDebugEnabled()) {
        logger.debug("Using preferredRestartLocation: agent=" + agent + " dest=" + selectedNode);
      }
    }
    if (selectedNode != null) temp.remove(selectedNode); //this one is already used this time, don't count it.

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

  public void restartOneAgent(String nodeName) {
    if(restartAgents.containsKey(nodeName)) {
      int restarts = Integer.parseInt((String)(restartAgents.get(nodeName)));
      restarts += 1;
      restartAgents.put(nodeName, Integer.toString(restarts));
    }
    else {
      restartAgents.put(nodeName, "1");
    }
  }

  public void clearRestarts() {
    restartAgents.clear();
  }

  public void restartSuccess(String agentName) {
    if(selectedNodes.containsKey(agentName))
      selectedNodes.remove(agentName);
  }

}
