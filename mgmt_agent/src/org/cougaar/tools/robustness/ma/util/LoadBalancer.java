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
import com.boeing.pw.mct.exnihilo2002.CSParser;
import com.boeing.pw.mct.exnihilo.plugin.LoadBalanceRequest;
import com.boeing.pw.mct.exnihilo.plugin.LoadBalanceRequestPredicate;

import java.util.ArrayList;
import java.util.Date;
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

  private LoggingService logger;
  protected EventService eventService;
  private MoveHelper moveHelper;
  private RobustnessController controller;
  //private HashMap origSocietys = new HashMap();
  private CougaarSociety origSociety = null;

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
  public LoadBalancer(BindingSite bs, RobustnessController controller) {
    this.setBindingSite(bs);
    this.controller = controller;
    this.moveHelper = controller.getMoveHelper();
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
    eventService = (EventService) getServiceBroker().getService(this, EventService.class, null);
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
    for (Iterator it = healthMonitorRequests.getAddedCollection().iterator(); it.hasNext(); ) {
      HealthMonitorRequest hsm = (HealthMonitorRequest) it.next();
      if (hsm.getRequestType() == HealthMonitorRequest.LOAD_BALANCE) {
        origSociety = doLoadBalance();
      }
    }

    for (Iterator it = loadBalanceRequests.getChangedCollection().iterator(); it.hasNext(); ){
      LoadBalanceRequest lbr = (LoadBalanceRequest)it.next();
      if(lbr.isResult()) {
        CougaarSociety society = lbr.getCougaarSociety();
        if(society == null) continue;
        logger.debug("result society: \n" + society.toXML());
        moveAgents(origSociety, society);
      }
    }
  }

  /**
   * Submit request to EN for new community laydown and perform required moves.
   */
  public CougaarSociety doLoadBalance() {
    // TODO: submit request to EN plugin and send move requests to moveHelper
    //       upon receipt of EN response
    logger.info("doLoadBalance");
    String society = controller.getCompleteStatus();
    String enSociety = getXmlForEN(society);
    if(enSociety == null) return null;
    logger.debug("get society xml before load balance: \n" + enSociety);
    CougaarSociety cs = loadSocietyFromXML(enSociety);
    LoadBalanceRequest loadBalReq = new LoadBalanceRequest(cs);
    logger.debug("publishing LoadBalanceRequest");
    blackboard.publishAdd(loadBalReq);
    return cs;
  }

  private void moveAgents(CougaarSociety origSociety, CougaarSociety newSociety){
    String society = controller.getCompleteStatus();
    int index = society.indexOf("<community name=");
    index = society.indexOf("\"", index);
    String comm = society.substring(index + 1, society.indexOf("\"", index+1));
    for(Iterator it = newSociety.getNodes(); it.hasNext(); ) {
      CougaarNode node = (CougaarNode)it.next();
      for(Iterator ait = node.getAgents(); ait.hasNext();) {
        CougaarAgent agent = (CougaarAgent)ait.next();
        String name = agent.getName();
        CougaarAgent oldAgent = origSociety.getAgent(name);
        String oldNode = oldAgent.getNode().getName();
        if(!(agent.getNode().getName().equals(oldNode))) {
          logger.debug("move agent " + name + " from " + oldNode + " to " + node.getName() + " in " + comm);
          moveHelper.moveAgent(name, oldNode, node.getName(), comm);
        }
      }
    }
  }

  /**
   * Get the society xml file especially for EN.
   * @param society original society xml
   * @return
   */
  private String getXmlForEN(String society) {
    StringBuffer sb = new StringBuffer();
    sb.append("<?xml version=\"1.0\"?>\n" +
        "<CougaarSociety identifier=\"baseline\">\n");
    int index = society.indexOf("<nodes count=");
    index = society.indexOf("\"", index);
    int nodesCount = Integer.parseInt(society.substring(index+1, society.indexOf("\"", index+1)));
    index = society.indexOf("<agents count=");
    index = society.indexOf("\"", index);
    int agentsCount = Integer.parseInt(society.substring(index+1, society.indexOf("\"", index+1)));
    for(int i=0; i<nodesCount; i++) {
      index = society.indexOf("<node name=");
      index = society.indexOf("\"", index);
      society = society.substring(index);
      String nodeName = society.substring(1, society.indexOf("\"", 1));
      int stateIndex1 = society.indexOf("state=\"");
      int stateIndex2 = society.indexOf("\"", stateIndex1+8);
      String state = society.substring(stateIndex1+7, stateIndex2);
      if(state.equals("DEAD")){
        continue;
      }
      sb.append("  <CougaarNode name=\"" + nodeName + "\">\n");
      sb.append("    <Attribute name=\"Memory\" value=\"nodeMemory\"/>\n");
      sb.append("    <Attribute name=\"ProbabilityOfFailure\" value=\"0.2\"/>\n");
      sb.append("    <Attribute name=\"CPU\" value=\"nodeCPU\"/>\n");
      sb.append("    <Attribute name=\"OperatingSystem\" value=\"LINUX\"/>\n");
      String tmp = society;
      int childAgents = 0;
      for(int j=0; j<agentsCount; j++) {
        index = tmp.indexOf("<agent name=");
        index = tmp.indexOf("\"", index);
        tmp = tmp.substring(index);
        String agentName = tmp.substring(1, tmp.indexOf("\"", 1));
        index = tmp.indexOf("current=");
        index = tmp.indexOf("\"", index);
        tmp = tmp.substring(index);
        String parent = tmp.substring(1, tmp.indexOf("\"", 1));
        if(parent.equals(nodeName)) {
          sb.append("    <CougaarAgent name=\"" + agentName + "\">\n");
          sb.append("      <Requirement name=\"CPU\" value=\"50\"/>\n");
          sb.append("      <Requirement name=\"Memory\" value=\"30\"/>\n");
          sb.append("      <Requirement name=\"OperatingSystem\" value=\"LINUX\"/>\n");
          sb.append("      <Requirement name=\"BandwidthReceived_" + agentName + "\" value=\"3\"/>\n");
          sb.append("      <Requirement name=\"BandwidthSent_" + agentName + "\" value=\"3\"/>\n");
          sb.append("    </CougaarAgent>\n");
          childAgents ++;
        }
      }
      int nodeCPU = childAgents * 50 + 200;
      index = sb.lastIndexOf("nodeCPU");
      sb.replace(index, index+7, Integer.toString(nodeCPU));
      int nodeMemory = childAgents * 30 + 200;
      index = sb.lastIndexOf("nodeMemory");
      sb.replace(index, index+10, Integer.toString(nodeMemory));
      sb.append("  </CougaarNode>\n");
    }
    sb.append("</CougaarSociety>");
    //origSocietys.put(comm, sb.toString());
    return sb.toString();
  }

  /**
   * Use CSParser to load the cougaar society from given xml file.
   * @param xml
   * @return
   */
  private CougaarSociety loadSocietyFromXML(String xml) {
    CSParser csp = new CSParser();
    csp.echoStruct_ = false;    // be quiet
    CougaarSociety society  = null;
    try {
        BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(xml.getBytes()));
        society = csp.parseCSXML(bis);
        bis.close();
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    return society;
  } // loadSocietyFromXML


  /**
   * Sends Cougaar event via EventService.
   */
  protected void event(String message) {
    if (eventService != null && eventService.isEventEnabled())
      eventService.event(message);
  }

  private static String targetsToString(Collection targets) {
    StringBuffer sb = new StringBuffer("[");
    for (Iterator it = targets.iterator(); it.hasNext();) {
      sb.append(it.next());
      if (it.hasNext()) sb.append(",");
    }
    sb.append("]");
    return sb.toString();
  }

}
