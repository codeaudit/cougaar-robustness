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
import java.util.HashSet;
import org.cougaar.core.plugin.*;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.mts.MessageAddress;

/**
 * This Plugin tests HeartbeatRequester and HeartbeatServerPlugin.
 * It is an example of how a Plugin would issue HeartbeatRequests
 * and receive HeartbeatHealthReports.
 *
 * See sensors/doc/readme.txt for more information.
 **/
public class HeartbeatTesterPlugin extends ComponentPlugin {
  private IncrementalSubscription reqSub;
  private IncrementalSubscription reportSub;
  private BlackboardService bb;
  private SensorFactory sensorFactory;

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

  protected void setupSubscriptions() {
    DomainService domainService = 
      (DomainService) getBindingSite().getServiceBroker().getService(this, DomainService.class, null);
    sensorFactory = (SensorFactory)domainService.getFactory("sensors");
    bb = getBlackboardService();
    reqSub = (IncrementalSubscription)bb.subscribe(reqPred);
    reportSub = (IncrementalSubscription)bb.subscribe(reportPred);
    MessageAddress source = getBindingSite().getAgentIdentifier();

    Iterator iter = this.getParameters().iterator(); //from .ini
    long requestTimeout = 0;
    if (iter.hasNext()) {
      requestTimeout = Long.parseLong((String)iter.next());
    }
    long heartbeatFrequency = 0;
    if (iter.hasNext()) {
      heartbeatFrequency = Long.parseLong((String)iter.next());
    }
    long heartbeatTimeout = 0;
    if (iter.hasNext()) {
      heartbeatTimeout = Long.parseLong((String)iter.next());
    }
    boolean onlyOutOfSpec = false;
    if (iter.hasNext()) {
      onlyOutOfSpec = Boolean.valueOf((String)iter.next()).booleanValue();
    }
    float percentOutOfSpec = 0.0f;
    if (iter.hasNext()) {
      percentOutOfSpec = Float.parseFloat((String)iter.next());
    }
    // the rest of the parameters are targets
    HashSet targets = new HashSet();
    MessageAddress target = null;
    while (iter.hasNext()) {
      targets.add(new ClusterIdentifier((String)iter.next()));
    }
    HeartbeatRequest req = sensorFactory.newHeartbeatRequest(source, 
                                                             targets, 
                                                             requestTimeout, 
                                                             heartbeatFrequency,      
                                                             heartbeatTimeout,   
                                                             onlyOutOfSpec,
                                                             percentOutOfSpec);
    bb.publishAdd(req);
    System.out.println("\nHeartbeatTesterPlugin.setupSubscriptions: added HeartbeatRequest = " + req);
  }

  protected void execute () {
    // process changed HeartbeatRequests
    Iterator iter = reqSub.getChangedCollection().iterator();
    while (iter.hasNext()) {
      HeartbeatRequest req = (HeartbeatRequest)iter.next();
      MessageAddress myAddr = getBindingSite().getAgentIdentifier();
      if (req.getSource().equals(myAddr)) {
        System.out.println("\nHeartbeatTesterPlugin.execute: received changed HeartbeatRequest = " + req);
        int status = req.getStatus();
	  switch (status) {
          case HeartbeatRequest.NEW:
            System.out.println("HeartbeatTesterPlugin.execute: status = NEW, ignored.");
            break;
          case HeartbeatRequest.SENT:
            System.out.println("HeartbeatTesterPlugin.execute: status = SENT, ignored.");
            break;
          case HeartbeatRequest.ACCEPTED:
            System.out.println("HeartbeatTesterPlugin.execute: status = ACCEPTED.");
            break;
          case HeartbeatRequest.REFUSED:
            System.out.println("HeartbeatTesterPlugin.execute: status = REFUSED, removed.");
            bb.publishRemove(req); 
            break;
          case HeartbeatRequest.FAILED:
            System.out.println("HeartbeatTesterPlugin.execute: status = FAILED, removed.");
            bb.publishRemove(req); 
            break;
          default:
            System.out.println("HeartbeatTesterPlugin.execute: illegal status = " + req.getStatus() + ", removed.");
            bb.publishRemove(req); 
        }
      }
    }
    // process new HeartbeatHealthReports
    iter = reportSub.getAddedCollection().iterator();
    while (iter.hasNext()) {
      HeartbeatHealthReport rpt = (HeartbeatHealthReport)iter.next();
      System.out.println("\nHeartbeatTesterPlugin.execute: received HeartbeatHealthReport = " + rpt);
      bb.publishRemove(rpt);
    }
  }

}


