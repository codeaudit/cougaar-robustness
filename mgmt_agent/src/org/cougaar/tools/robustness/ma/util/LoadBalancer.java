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

import org.cougaar.tools.robustness.ma.ldm.HealthMonitorRequest;
import org.cougaar.tools.robustness.ma.controllers.RobustnessController;

import org.cougaar.core.blackboard.BlackboardClientComponent;
import org.cougaar.core.blackboard.IncrementalSubscription;

import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.AlarmService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.EventService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.SchedulerService;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ServiceBroker;

import org.cougaar.core.util.UID;

import org.cougaar.util.UnaryPredicate;

import org.cougaar.scalability.util.CougaarSociety;
import org.cougaar.scalability.util.CougaarNode;
import org.cougaar.scalability.util.CougaarAgent;
import org.cougaar.robustness.exnihilo.CSParser;
import org.cougaar.robustness.exnihilo.plugin.LoadBalanceRequest;
import org.cougaar.robustness.exnihilo.plugin.LoadBalanceRequestPredicate;

import java.util.ArrayList;
import java.util.Date;
import java.util.Collections;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;

/**
 * Provides interface for initiating load balancing across community nodes.
 */
public class LoadBalancer extends BlackboardClientComponent {

  public static final int DEFAULT_SOLVER_MODE = LoadBalanceRequest.SOLVER_MODE_BLEND_PFAIL_LOAD_BALANCE;
  public static final int DEFAULT_ANNEAL_TIME = -1;
  public static final boolean DEFAULT_HAMMING = true;

  private LoggingService logger;
  private MoveHelper moveHelper;
  private RobustnessController controller;
  private CommunityStatusModel model;
  private List lbReqQueue = Collections.synchronizedList(new ArrayList());

  // Subscription to HealthMonitorRequests for load balancing
  private IncrementalSubscription healthMonitorRequests;
  private UnaryPredicate healthMonitorRequestPredicate = new UnaryPredicate() {
    public boolean execute (Object o) {
      if (o instanceof HealthMonitorRequest) {
        HealthMonitorRequest hmr = (HealthMonitorRequest)o;
        return (hmr.getRequestType() == hmr.LOAD_BALANCE);
      }
      return false;
  }};

  //Subscription to LoadBalanceRequests for load balancing results
  private IncrementalSubscription loadBalanceRequests;

/**
 * Constructor requires BindingSite to initialize needed services.
 * @param bs
 */
  public LoadBalancer(BindingSite bs, RobustnessController controller, CommunityStatusModel model) {
    this.setBindingSite(bs);
    this.controller = controller;
    this.moveHelper = controller.getMoveHelper();
    this.model = model;
    initialize();
    load();
    start();
  }

  /**
   * Load requires services.
   */
  public void load() {
    setAgentIdentificationService(
      (AgentIdentificationService)getServiceBroker().getService(this, AgentIdentificationService.class, null));
    setAlarmService(
      (AlarmService)getServiceBroker().getService(this, AlarmService.class, null));
    setSchedulerService(
      (SchedulerService)getServiceBroker().getService(this, SchedulerService.class, null));
    setBlackboardService(
      (BlackboardService)getServiceBroker().getService(this, BlackboardService.class, null));
    logger =
      (LoggingService)getBindingSite().getServiceBroker().getService(this, LoggingService.class, null);
    logger = org.cougaar.core.logging.LoggingServiceWithPrefix.add(logger, agentId + ": ");
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
        (IncrementalSubscription)blackboard.subscribe(healthMonitorRequestPredicate);
    loadBalanceRequests =
     (IncrementalSubscription) blackboard.subscribe(new LoadBalanceRequestPredicate(true));
  }

  public void execute() {
    // Publish queued LB requests
    pubishLoadBalancerRequests();

    for (Iterator it = healthMonitorRequests.getAddedCollection().iterator(); it.hasNext(); ) {
      HealthMonitorRequest hsm = (HealthMonitorRequest) it.next();
      if (hsm.getRequestType() == HealthMonitorRequest.LOAD_BALANCE) {
        doLoadBalance(true,
                      Collections.EMPTY_LIST,
                      Collections.EMPTY_LIST,
                      getExcludedNodes());
      }
    }

    for (Iterator it = loadBalanceRequests.getChangedCollection().iterator(); it.hasNext(); ){
      LoadBalanceRequest lbr = (LoadBalanceRequest)it.next();
      if(lbr.isResult()) {
        CougaarSociety society = lbr.getCougaarSociety();
        if(society == null) continue;
        logger.debug("result society: \n" + society.toXML());
        moveAgents(society);
      }
    }
  }

  /**
   * Submit request to EN for new community laydown and perform required moves.
   */
  public void doLoadBalance() {
    // submit request to EN plugin and send move requests to moveHelper
    //       upon receipt of EN response
    logger.info("doLoadBalance");
    List newNodes = Collections.EMPTY_LIST;
    List killedNodes = Collections.EMPTY_LIST;
    List leaveAsIsNodes = getExcludedNodes();
    doLoadBalance(DEFAULT_HAMMING, newNodes, killedNodes, leaveAsIsNodes);
    logger.debug("publishing LoadBalanceRequest");
  }

  public void doLoadBalance(boolean useHamming, List newNodes, List killedNodes, List leaveAsIsNodes) {
    int solverMode = DEFAULT_SOLVER_MODE;
    logger.info("doLoadBalance:" +
                " solverMode=" + solverMode +
                " useHamming=" + useHamming +
                " newNodes=" + newNodes +
                " killedNodes=" + killedNodes +
                " leaveAsIsNodes=" + leaveAsIsNodes);
    LoadBalanceRequest loadBalReq =
        new LoadBalanceRequest(DEFAULT_ANNEAL_TIME,
                               DEFAULT_SOLVER_MODE,
                               useHamming,
                               newNodes,
                               killedNodes,
                               leaveAsIsNodes);
    logger.debug("publishing LoadBalanceRequest");
    fireLater(loadBalReq);
  }

  protected List getExcludedNodes() {
    return model.search("(UseForRestarts=False)");
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
      blackboard.publishAdd((LoadBalanceRequest) l.get(i));
    }
  }

  private void moveAgents(CougaarSociety newSociety){
    String society = controller.getCompleteStatus();
    int index = society.indexOf("<community name=");
    index = society.indexOf("\"", index);
    String comm = society.substring(index + 1, society.indexOf("\"", index+1));
    for(Iterator it = newSociety.getNodes(); it.hasNext(); ) {
      CougaarNode node = (CougaarNode)it.next();
      for(Iterator ait = node.getAgents(); ait.hasNext();) {
        CougaarAgent agent = (CougaarAgent)ait.next();
        String name = agent.getName();
        String oldNode = model.getLocation(name);
        if(!(agent.getNode().getName().equals(oldNode))) {
          logger.debug("move agent " + name + " from " + oldNode + " to " + node.getName() + " in " + comm);
          moveHelper.moveAgent(name, oldNode, node.getName(), comm);
        }
      }
    }
  }

}
