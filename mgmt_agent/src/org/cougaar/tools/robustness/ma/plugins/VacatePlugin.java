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
package org.cougaar.tools.robustness.ma.plugins;

import org.cougaar.tools.robustness.ma.ldm.VacateRequest;
import org.cougaar.robustness.restart.plugin.*;
import org.cougaar.tools.robustness.ma.ldm.RestartLocationRequest;
import java.util.*;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.SimplePlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.TopologyReaderService;
import org.cougaar.core.mts.MessageAddress;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.robustness.restart.plugin.NodeMove;

/**
 * This plugin moves all community members from a specified node or host to
 * another node/host when it receives a VacateRequest.
 */
public class VacatePlugin extends SimplePlugin {

  private LoggingService log;
  private TopologyReaderService trs;
  private BlackboardService bbs = null;

  // Defines default values for configurable parameters.
  private static String defaultParams[][] = new String[0][0];

  ManagementAgentProperties vacateProps =
    ManagementAgentProperties.makeProps(this.getClass().getName(), defaultParams);

  protected void setupSubscriptions() {

    log =  (LoggingService) getBindingSite().getServiceBroker().
      getService(this, LoggingService.class, null);

    trs = (TopologyReaderService)getBindingSite().getServiceBroker().
      getService(this, TopologyReaderService.class, null);

    bbs = getBlackboardService();

    // Initialize configurable paramaeters from defaults and plugin arguments.
    updateParams(vacateProps);
    bbs.publishAdd(vacateProps);

    // Subscribe to ManagementAgentProperties to receive parameter changes
    mgmtAgentProps =
      (IncrementalSubscription)bbs.subscribe(propertiesPredicate);

    // Subscribe to VacateRequest objects
    vacateRequests =
      (IncrementalSubscription)bbs.subscribe(vacateRequestPredicate);

    restartLocationRequests =
      (IncrementalSubscription)bbs.subscribe(restartLocationRequestPredicate);

    // Print informational message defining current parameters
    StringBuffer startMsg = new StringBuffer();
    startMsg.append("VacatePlugin started: ");
    startMsg.append(" " + paramsToString());
    log.info(startMsg.toString());
  }

  public void execute() {

    // Get Parameter changes
    for (Iterator it = mgmtAgentProps.getChangedCollection().iterator();
         it.hasNext();) {
      ManagementAgentProperties props = (ManagementAgentProperties)it.next();
      updateParams(props);
      log.info("Parameters modified: " + paramsToString());
    }

     // Get VacateRequest objects
    for (Iterator it = vacateRequests.getAddedCollection().iterator();
         it.hasNext();) {
      VacateRequest vr = (VacateRequest)it.next();
      //log.debug("Received VacateRequest: host=" + vr.getHost() +
        //" node=" + vr.getNode());
      System.out.println("Received VacateRequest: host=" + vr.getHost() +
        " node=" + vr.getNode());
      if(vr.getRequestType() == VacateRequest.VACATE_HOST)
      {
        RestartLocationRequest rlr = new RestartLocationRequest(RestartLocationRequest.LOCATE_HOST);
        Set memberNodes = trs.getChildrenOnParent(TopologyReaderService.NODE,
            TopologyReaderService.HOST, vr.getHost());
        rlr.setExcludedNodes(memberNodes);
        for(Iterator iter = memberNodes.iterator(); iter.hasNext();)
        {
          Set memberAgents = trs.getChildrenOnParent(TopologyReaderService.AGENT,
            TopologyReaderService.NODE, (String)iter.next());
          for(Iterator ait = memberAgents.iterator(); ait.hasNext();)
            rlr.addAgent(new MessageAddress((String)ait.next()));
        }
        Collection hosts = new ArrayList();
        hosts.add(vr.getHost());
        rlr.setExcludedHosts(hosts);
        rlr.setStatus(RestartLocationRequest.NEW);
        bbs.publishAdd(rlr);
      }
    }

    //get updated RestartLocationRequest objects, publish a NodeMove object to the
    //blackboard for each node in the vacate host.
    for(Iterator it = restartLocationRequests.getChangedCollection().iterator(); it.hasNext();)
    {
      RestartLocationRequest rlr = (RestartLocationRequest)it.next();
      if(rlr.getStatus() == RestartLocationRequest.SUCCESS)
      {
        String destHost = rlr.getHost();
        for(Iterator iter = rlr.getExcludedHosts().iterator(); iter.hasNext();)
        {
          String hostName = (String)iter.next();
          Set memberNodes = trs.getChildrenOnParent(TopologyReaderService.NODE,
            TopologyReaderService.HOST, hostName);
          for(Iterator nit = memberNodes.iterator(); nit.hasNext();)
          {
            String sourceNode = (String)nit.next();
            NodeMove nm = new NodeMove(hostName, destHost, sourceNode);
            bbs.publishAdd(nm);
          }
        }
      }
    }
  }


  /**
   * Obtains plugin parameters
   * @param obj List of "name=value" parameters
   */
  public void setParameter(Object obj) {
    List args = (List)obj;
    for (Iterator it = args.iterator(); it.hasNext();) {
      String arg = (String)it.next();
      String name = arg.substring(0,arg.indexOf("="));
      String value = arg.substring(arg.indexOf('=')+1);
      vacateProps.setProperty(name, value);
    }
  }

  /**
   * Creates a printable representation of current parameters.
   * @return  Text string of current parameters
   */
  private String paramsToString() {
    StringBuffer sb = new StringBuffer();
    for (Enumeration enum = vacateProps.propertyNames(); enum.hasMoreElements();) {
      String propName = (String)enum.nextElement();
      sb.append(propName + "=" +
        vacateProps.getProperty(propName) + " ");
    }
    return sb.toString();
  }

  /**
   * Sets externally configurable parameters using supplied Properties object.
   * @param props Propertie object defining paramater names and values.
   */
  private void updateParams(Properties props) {
     // None for now
  }

 /**
  * Predicate for VacateRequest objects
  */
  private IncrementalSubscription vacateRequests;
  private UnaryPredicate vacateRequestPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof VacateRequest);
  }};

  /**
   * Predicate for RestartLocationRequest objects
   */
  private IncrementalSubscription restartLocationRequests;
  private UnaryPredicate restartLocationRequestPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof RestartLocationRequest);
  }};


  /**
   * Predicate for Management Agent properties
   */
  String myPluginName = getClass().getName();
  private IncrementalSubscription mgmtAgentProps;
  private UnaryPredicate propertiesPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof ManagementAgentProperties) {
        ManagementAgentProperties props = (ManagementAgentProperties)o;
        String forName = props.getPluginName();
        return (myPluginName.equals(forName) || myPluginName.endsWith(forName));
      }
      return false;
  }};
}