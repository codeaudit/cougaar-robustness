/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
 *  Copyright 2002 Object Services and Consulting, Inc.
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 *
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

package org.cougaar.tools.robustness.sensors;

import java.util.Iterator;
import java.util.Collection;
import org.cougaar.core.plugin.*;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.component.ServiceBroker;

/**
 * This Plugin tests HeartbeatRequester and HeartbeatServerPlugin.
 * It is an example of how a Plugin would issue HeartbeatRequests
 * and receive HeartbeatHealthReports.
 * 
 * This particular class is set up the way MIC uses Heartbeats, in
 * that it issues individual requests for each monitored agent.
 *
 * See sensors/doc/readme.txt for more information.
 **/
public class HeartbeatTesterMICPlugin extends ComponentPlugin {
  private IncrementalSubscription reqSub;
  private IncrementalSubscription reportSub;
  private BlackboardService bb;
  private SensorFactory sensorFactory;
  private LoggingService log;
  private Collection parms;

  private UnaryPredicate reqPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof HeartbeatRequest) {
        return true;
      } else {
        return false;
      }
    }
  };

  private UnaryPredicate reportPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof HeartbeatHealthReport) {
        return true;
      } else {
        return false;
      }
    }
  };

  private void processParms() {
    Iterator iter = parms.iterator(); 
    long requestTimeout = Long.parseLong((String)iter.next());
    long heartbeatFrequency = Long.parseLong((String)iter.next());
    long heartbeatTimeout = Long.parseLong((String)iter.next());
    boolean onlyOutOfSpec = Boolean.valueOf((String)iter.next()).booleanValue();
    float percentOutOfSpec = Float.parseFloat((String)iter.next());
    // the rest of the parameters are targets
    MessageAddress source = getAgentIdentifier();
    MessageAddress target = null;
    while (iter.hasNext()) {
      target = new ClusterIdentifier((String)iter.next());
      HeartbeatRequest req = sensorFactory.newHeartbeatRequest(source, 
                                                               target, 
                                                               requestTimeout, 
                                                               heartbeatFrequency,      
                                                               heartbeatTimeout,   
                                                               onlyOutOfSpec,
                                                               percentOutOfSpec);
      if (log.isInfoEnabled()) 
        log.info("processParms: publishAdd HeartbeatRequest = " + req);
      bb.publishAdd(req);
    }
  }

  protected void setupSubscriptions() {
    ServiceBroker sb = getServiceBroker();
    log =  (LoggingService) sb.getService(this, LoggingService.class, null);
    DomainService domainService = (DomainService) sb.getService(this, DomainService.class, null);
    sensorFactory = (SensorFactory)domainService.getFactory("sensors");
    bb = getBlackboardService();
    reqSub = (IncrementalSubscription)bb.subscribe(reqPred);
    reportSub = (IncrementalSubscription)bb.subscribe(reportPred);
    parms = this.getParameters(); //from .ini
    processParms();
  }

  protected void execute () {
    // process changed HeartbeatRequests
    Iterator iter = reqSub.getChangedCollection().iterator();
    while (iter.hasNext()) {
      HeartbeatRequest req = (HeartbeatRequest)iter.next();
      MessageAddress myAddr = getAgentIdentifier();
      if (req.getSource().equals(myAddr)) {
        if (log.isInfoEnabled()) 
          log.info("execute: received changed HeartbeatRequest = " + req);
        int status = req.getStatus();
	  switch (status) {
          case HeartbeatRequest.NEW:
            if (log.isInfoEnabled()) 
              log.info("execute: status = NEW, ignored.");
            break;
          case HeartbeatRequest.SENT:
            if (log.isInfoEnabled()) 
              log.info("execute: status = SENT, ignored.");
            break;
          case HeartbeatRequest.ACCEPTED:
            if (log.isInfoEnabled()) 
              log.info("execute: status = ACCEPTED.");
            break;
          case HeartbeatRequest.REFUSED:
            if (log.isInfoEnabled()) 
              log.info("execute: status = REFUSED, removed.");
            bb.publishRemove(req); 
            break;
          case HeartbeatRequest.FAILED:
            if (log.isInfoEnabled()) 
              log.info("execute: status = FAILED, removed.");
            bb.publishRemove(req); 
            break;
          default:
            if (log.isInfoEnabled()) 
              log.info("execute: illegal status = " + req.getStatus() + ", removed.");
            bb.publishRemove(req); 
        }
      }
    }
    // process new HeartbeatHealthReports
    iter = reportSub.getAddedCollection().iterator();
    while (iter.hasNext()) {
      HeartbeatHealthReport rpt = (HeartbeatHealthReport)iter.next();
      if (log.isInfoEnabled()) {
        log.info("execute: received added HeartbeatHealthReport = " + rpt);
        log.info("execute: publishRemove HeartbeatHealthReport = " + rpt); }
      bb.publishRemove(rpt);
    }
  }

}


