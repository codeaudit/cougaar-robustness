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
package org.cougaar.tools.robustness.ma.util;

import org.cougaar.tools.robustness.ma.CommunityStatusModel;
import org.cougaar.tools.robustness.ma.RestartManagerConstants;

import org.cougaar.tools.robustness.ma.ldm.HealthMonitorRequest;
import org.cougaar.tools.robustness.ma.controllers.RobustnessController;
import org.cougaar.tools.robustness.ma.controllers.DefaultRobustnessController;

import org.cougaar.core.blackboard.BlackboardClientComponent;
import org.cougaar.core.blackboard.IncrementalSubscription;

import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.AlarmService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.EventService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.SchedulerService;
import org.cougaar.core.service.UIDService;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ServiceBroker;

import org.cougaar.core.util.UID;

import org.cougaar.util.UnaryPredicate;

import org.cougaar.scalability.util.CougaarSociety;
import org.cougaar.scalability.util.CougaarNode;
import org.cougaar.scalability.util.CougaarAgent;
import org.cougaar.robustness.exnihilo.plugin.LoadBalanceRequest;
import org.cougaar.robustness.exnihilo.plugin.LoadBalanceRequestPredicate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

/**
 * Provides interface for initiating load balancing across community nodes.
 */
public class LoadBalancer
    extends BlackboardClientComponent implements RestartManagerConstants {

  public static final boolean DEFAULT_HAMMING = true;

  private LoggingService logger;
  private MoveHelper moveHelper;
  private RobustnessController controller;
  private CommunityStatusModel model;
  private UIDService uidService;
  private List lbReqQueue = Collections.synchronizedList(new ArrayList());
  private Map myRequests = Collections.synchronizedMap(new HashMap());

  private List newNodes = Collections.synchronizedList(new ArrayList()); //record all new nodes in the community model
  private List killedNodes = Collections.synchronizedList(new ArrayList()); //record all killed nodes in the community model

  // Subscription to HealthMonitorRequests for load balancing
  private IncrementalSubscription healthMonitorRequests;
  private UnaryPredicate healthMonitorRequestPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof HealthMonitorRequest) {
        HealthMonitorRequest hmr = (HealthMonitorRequest) o;
        return (hmr.getRequestType() == hmr.LOAD_BALANCE);
      }
      return false;
    }
  };

  //Subscription to LoadBalanceRequests for load balancing results
  private IncrementalSubscription loadBalanceRequests;

  /**
   * Constructor requires BindingSite to initialize needed services.
   * @param bs
   */
  public LoadBalancer(BindingSite bs, RobustnessController controller,
                      CommunityStatusModel model) {
    this.setBindingSite(bs);
    this.controller = controller;
    this.moveHelper = controller.getMoveHelper();
    this.model = model;
    model.addNodeChangeListener(new NodeChangeListener() {
      public void addNode(String nodeName) {
        if (!newNodes.contains(nodeName)) {
          newNodes.add(nodeName);
        }
      }

      public void removeNode(String nodeName) {
        if (!killedNodes.contains(nodeName)) {
          killedNodes.add(nodeName);
        }
      }
    });
    initialize();
    load();
    start();
  }

  /**
   * Load requires services.
   */
  public void load() {
    ServiceBroker sb = getBindingSite().getServiceBroker();
    setAgentIdentificationService(
        (AgentIdentificationService) sb.getService(this,
        AgentIdentificationService.class, null));
    setAlarmService(
        (AlarmService) sb.getService(this, AlarmService.class, null));
    setSchedulerService(
        (SchedulerService) sb.getService(this, SchedulerService.class, null));
    setBlackboardService(
        (BlackboardService) sb.getService(this, BlackboardService.class, null));
    logger = (LoggingService) sb.getService(this, LoggingService.class, null);
    logger = org.cougaar.core.logging.LoggingServiceWithPrefix.add(logger,
        agentId + ": ");
    uidService = (UIDService) sb.getService(this, UIDService.class, null);
    super.load();
  }

  public void start() {
    super.start();
  }

  /**
   * Subscribe to mobility AgentControl objects and remote HealthMonitorRequests.
   */
  public void setupSubscriptions() {
    healthMonitorRequests =
        (IncrementalSubscription) blackboard.subscribe(
        healthMonitorRequestPredicate);
    loadBalanceRequests =
        (IncrementalSubscription) blackboard.subscribe(new
        LoadBalanceRequestPredicate(true));
  }

  public void execute() {
    // Publish queued LB requests
    pubishLoadBalancerRequests();

    for (Iterator it = healthMonitorRequests.getAddedCollection().iterator();
         it.hasNext(); ) {
      HealthMonitorRequest hsm = (HealthMonitorRequest) it.next();
      if (hsm.getRequestType() == HealthMonitorRequest.LOAD_BALANCE) {
        doLoadBalance();
      }
    }

    for (Iterator it = loadBalanceRequests.getChangedCollection().iterator();
         it.hasNext(); ) {
      LoadBalanceRequest lbr = (LoadBalanceRequest) it.next();
      if (lbr.isResult()) {
        CougaarSociety society = lbr.getCougaarSociety();
        if (society != null) {
          if (logger.isDebugEnabled()) {
            logger.debug("LoadBalancer result: \n" + society.toXML());
          }
          Map layout = layoutFromSociety(society);
          if (lbr instanceof UniqueLoadBalanceRequest) {
            UniqueLoadBalanceRequest ulbr = (UniqueLoadBalanceRequest) lbr;
            LoadBalancerListener listener =
                (LoadBalancerListener) myRequests.remove(ulbr.getUID());
            if (listener != null) {
              listener.layoutReady(layout);
            }
          }
        }
      }
    }
  }

  /**
   * Get recommended layout from EN4J and execute the listener callback with
   * results.
   * @param useHamming
   * @param newNodes
   * @param killedNodes
   * @param leaveAsIsNodes
   * @param listener
   */
  public void doLayout(int                  solverMode,
                       int                  annealTime,
                       boolean              useHamming,
                       List                 newNodes,
                       List                 killedNodes,
                       List                 leaveAsIsNodes,
                       LoadBalancerListener listener) {
    UID uid = uidService.nextUID();
    if (logger.isInfoEnabled()) {
      logger.info("getLayout:" +
                  " annealTime=" + annealTime +
                  " solverMode=" + solverMode +
                  " useHamming=" + useHamming +
                  " newNodes=" + newNodes +
                  " killedNodes=" + killedNodes +
                  " leaveAsIsNodes=" + leaveAsIsNodes +
                  " uid=" + uid);
    }
    LoadBalanceRequest loadBalReq =
        new UniqueLoadBalanceRequest(annealTime,
                                     solverMode,
                                     useHamming,
                                     newNodes,
                                     killedNodes,
                                     leaveAsIsNodes,
                                     uid);
    myRequests.put(uid, listener);
    if (logger.isDebugEnabled()) {
      logger.debug("publishing LoadBalanceRequest");
    }
    fireLater(loadBalReq);
  }

  /**
   * Submit request to EN for new community laydown and perform required moves.
   */
  public void doLoadBalance() {
    long annealTime = getLongAttribute(ANNEAL_TIME_ATTRIBUTE,
                                       ANNEAL_TIME);
    long solverMode = getLongAttribute(LOAD_BALANCER_MODE_ATTRIBUTE,
                                       DEFAULT_LOAD_BALANCER_MODE);
    doLoadBalance((int)solverMode,
                  (int)annealTime,
                  DEFAULT_HAMMING,
                  getNewNodes(),
                  getKilledNodes(),
                  getExcludedNodes());
  }

  protected boolean isVacantNode(String name) {
    return model.entitiesAtLocation(name, model.AGENT).length == 0;
  }

  public void doLoadBalance(int     solverMode,
                            int     annealTime,
                            boolean useHamming,
                            List    newNodes,
                            List    killedNodes,
                            List    leaveAsIsNodes) {
    // submit request to EN plugin and send move requests to moveHelper
    //       upon receipt of EN response
    if (logger.isInfoEnabled()) {
      logger.info("doLoadBalance");
    }
    doLayout(solverMode, annealTime, useHamming, newNodes, killedNodes,
             leaveAsIsNodes, new LoadBalancerListener() {
      public void layoutReady(Map layout) {
        if (logger.isInfoEnabled()) {
          logger.info("layout from EN4J: " + layout);
        }
        moveAgents(layout);
      }
    });
  }

  protected List getExcludedNodes() {
    return model.search("(UseForRestarts=False)");
  }

  /**
   * Get all killed nodes in the community model.
   * @return List
   */
  protected List getKilledNodes() {
    for (Iterator it = killedNodes.iterator(); it.hasNext(); ) {
      String nodeName = (String) it.next();
      if (model.contains(nodeName) &&
          model.getCurrentState(nodeName) != DefaultRobustnessController.DEAD) {
        it.remove();
      }
    }
    return killedNodes;
  }

  /**
   * Get all new nodes in the community model.
   * @return List
   */
  protected List getNewNodes() {
    for (Iterator it = newNodes.iterator(); it.hasNext(); ) {
      String nodeName = (String) it.next();
      if (model.entitiesAtLocation(nodeName).length > 0 ||
          model.getCurrentState(nodeName) != DefaultRobustnessController.ACTIVE) {
        it.remove();
      }
    }
    return newNodes;
  }

  protected void fireLater(LoadBalanceRequest lbr) {
    synchronized (lbReqQueue) {
      lbReqQueue.add(lbr);
    }
    if (blackboard != null) {
      blackboard.signalClientActivity();
    }
  }

  private void pubishLoadBalancerRequests() {
    int n;
    List l;
    synchronized (lbReqQueue) {
      n = lbReqQueue.size();
      if (n <= 0) {
        return;
      }
      l = new ArrayList(lbReqQueue);
      lbReqQueue.clear();
    }
    for (int i = 0; i < n; i++) {
      blackboard.publishAdd( (LoadBalanceRequest) l.get(i));
    }
  }

  private Map layoutFromSociety(CougaarSociety newSociety) {
    Map layout = new HashMap();
    String society = controller.getCompleteStatus();
    int index = society.indexOf("<community name=");
    index = society.indexOf("\"", index);
    String comm = society.substring(index + 1, society.indexOf("\"", index + 1));
    for (Iterator it = newSociety.getNodes(); it.hasNext(); ) {
      CougaarNode node = (CougaarNode) it.next();
      for (Iterator ait = node.getAgents(); ait.hasNext(); ) {
        CougaarAgent agent = (CougaarAgent) ait.next();
        layout.put(agent.getName(), node.getName());
      }
    }
    return layout;
  }

  public void moveAgents(Map layout) {

    String communityName = model.getCommunityName();

    Map oldnodes = new HashMap(); //save all nodes and the number of agents in each current node
    Map newnodes = new HashMap(); //save all nodes and the number of agents in each node after balancing

    Map temp = new HashMap();
    synchronized (layout) {
      temp.putAll(layout);
    }

    //remove all agents who don't need move.
    for (Iterator it = temp.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry me = (Map.Entry) it.next();
      String agent = (String) me.getKey();
      String newNode = (String) me.getValue();
      increaseCounts(newnodes, newNode);
      String currentNode = model.getLocation(agent);
      if (model.getType(agent) == model.AGENT && currentNode != null) {
        increaseCounts(oldnodes, currentNode);
        if (newNode.equals(currentNode)) {
          it.remove();
        }
      }
      else {
        it.remove();
      }
    }

    boolean needChange = true; //if the result of compare of two sets need be changed?
    String lastChangeNode = ""; //what is the last result of the comparison?
    int checkDiffCount = 0; //how many times we get a "useless" result?
    int nodeSize = oldnodes.size() > newnodes.size() ? oldnodes.size() : newnodes.size();
    /**
     * This loop does the following jobs to keep node balancing during agents moving:
     * 1. Find the node who has the most number of agents need to be moved out.
     * 2. Move five agents from the node to destinations.
     * 3. Repeat step1 to step2 until all agents get destinations.
     */
    while (!temp.isEmpty()) {
      String node = getBiggestDifference(oldnodes, newnodes, needChange, lastChangeNode);
      if(needChange)
        checkDiffCount = checkDiffCount + 1;
      else
        checkDiffCount = 0;
      int count = 0;
      lastChangeNode = node;
      needChange = true;
      for (Iterator it = temp.keySet().iterator(); it.hasNext(); ) {
        if (count == 5) {
          break;
        }
        String agent = (String) it.next();
        String newNode = (String) temp.get(agent);
        String currentNode = model.getLocation(agent);
        if ((currentNode.equals(node) && ! (newNode.equals(currentNode))) ||
            node.equals("") || checkDiffCount >= nodeSize) {
          if (logger.isInfoEnabled()) {
            logger.info("move agent " + agent + " from " + currentNode +
                         " to " +
                         newNode + " in community " + communityName);
          }
          moveHelper.moveAgent(agent, currentNode, newNode, communityName);
          count++;
          needChange = false;
          int num = ( (Integer) oldnodes.get(currentNode)).intValue();
          oldnodes.put(currentNode, new Integer(num - 1));
          if (oldnodes.containsKey(newNode)) {
            num = ( (Integer) oldnodes.get(newNode)).intValue();
            oldnodes.put(newNode, new Integer(num + 1));
          } else {
            oldnodes.put(newNode, new Integer(1));
          }
          it.remove();
        }
      }
    }
    if (logger.isInfoEnabled()) {
      logger.info("LoadBalance finished.");
    }
  }

  /**
   * Fetch the element who has the most number of difference between the two given maps.
   * @param a map to be compared
   * @param b map to be compared
   * @param needChange If the result returned same as last time and no agent is moved based on this
   *                   result, the function will fall in one endless loop. To avoid this, set this
   *                   boolean value to indicate if the result need to differ with the one got from
   *                   last time.
   * @param lastChangeNode the result got from last comparation.
   */
  private String getBiggestDifference(Map a, Map b, boolean needChange, String lastChangeNode) {
    int diff = 0;
    String result = "";
    for (Iterator it = a.keySet().iterator(); it.hasNext(); ) {
      String node = (String) it.next();
      if(needChange && node.equals(lastChangeNode))
        continue;
      int oldnum = 0;
      if(a.containsKey(node))
        oldnum = ( (Integer) a.get(node)).intValue(); //how many agents on current node?
      int newnum = 0;
      if(b.containsKey(node))
        newnum = ( (Integer) b.get(node)).intValue(); //how many agents on this node after balancing?
      int x = oldnum - newnum; //get the difference.
      if ( ((diff == 0 && x != 0) || x > diff)){
        diff = x;
        result = node;
      }
    }
    return result;
  }

  private void increaseCounts(Map map, String name) {
    if (map.containsKey(name)) {
      int num = ( (Integer) map.get(name)).intValue();
      map.put(name, new Integer(num + 1));
    }
    else {
      map.put(name, new Integer(1));
    }
  }

  protected long getLongAttribute(String id, long defaultValue) {
    if (attributeDefined(id)) {
      return model.getLongAttribute(id);
    } else {
      return defaultValue;
    }
  }

  protected boolean attributeDefined(String id) {
    return model.hasAttribute(id);
  }

}
