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

import org.cougaar.core.plugin.*;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.blackboard.IncrementalSubscription;
import java.util.Iterator;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.mts.MessageAddress;

/**
 * This Plugin tests PingRequester and PingServerPlugin.
 * It is an example of how a Plugin would ping remote agents.
 *
 * See sensors/doc/readme.txt for more information.
 **/
public class PingTesterPlugin extends ComponentPlugin {
  private IncrementalSubscription sub;
  private BlackboardService bb;
  private SensorFactory sensorFactory;
  private LoggingService log;

  private UnaryPredicate pred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof PingRequest) {
        return true;
      } else {
        return false;
      }
    }
  };

  protected void setupSubscriptions() {
    ServiceBroker sb = getServiceBroker();
    log =  (LoggingService) sb.getService(this, LoggingService.class, null);
    DomainService domainService = (DomainService) sb.getService(this, DomainService.class, null);
    sensorFactory = (SensorFactory)domainService.getFactory("sensors");
    bb = getBlackboardService();
    sub = (IncrementalSubscription)bb.subscribe(pred);
    MessageAddress source = getAgentIdentifier();
    // target is first parameter (from .ini)
    MessageAddress target = null;
    Iterator iter = this.getParameters().iterator();  
    if (iter.hasNext()) {
      target = new ClusterIdentifier((String)iter.next());
    }
    long timeout = 0;
    if (iter.hasNext()) {
      timeout = Long.parseLong((String)iter.next());
    }
    PingRequest req = sensorFactory.newPingRequest(source, target, timeout);
    bb.publishAdd(req);
    if (log.isInfoEnabled()) 
      log.info("PingTesterPlugin.setupSubscriptions: added PingRequest = " + req);
  }

  protected void execute () {
    Iterator iter = sub.getChangedCollection().iterator();
    while (iter.hasNext()) {
      PingRequest req = (PingRequest)iter.next();
      if (log.isInfoEnabled()) 
        log.info("PingTesterPlugin.execute: received changed PingRequest = " + req);
      MessageAddress myAddr = getAgentIdentifier();
      if (req.getSource().equals(myAddr)) {
        int status = req.getStatus();
	  switch (status) {
          case PingRequest.NEW:
            if (log.isInfoEnabled()) 
              log.info("PingTesterPlugin.execute: status = NEW, ignored.");
            break;
          case PingRequest.SENT:
            if (log.isInfoEnabled()) 
              log.info("PingTesterPlugin.execute: status = SENT, ignored.");
            break;
          case PingRequest.RECEIVED:
            if (log.isInfoEnabled()) {
              log.info("PingTesterPlugin.execute: status = RECEIVED.");
              log.info("PingTesterPlugin.execute: timeSent = " + req.getTimeSent());
              log.info("PingTesterPlugin.execute: timeReceived = " + req.getTimeReceived());
              log.info("PingTesterPlugin.execute: roundTripTime = " + req.getRoundTripTime());
              log.info("PingTesterPlugin.execute: timeSent = " + req.getTimeSent());
            }
            bb.publishRemove(req); 
            break;
          case PingRequest.FAILED:
            if (log.isInfoEnabled()) {
              log.info("PingTesterPlugin.execute: status = FAILED.");
              log.info("PingTesterPlugin.execute: timeSent = " + req.getTimeSent());
              log.info("PingTesterPlugin.execute: timeReceived = " + req.getTimeReceived());
              log.info("PingTesterPlugin.execute: roundTripTime = " + req.getRoundTripTime());
              log.info("PingTesterPlugin.execute: timeSent = " + req.getTimeSent());
            }
            bb.publishRemove(req); 
            break;
          default:
            if (log.isInfoEnabled()) 
              log.info("PingTesterPlugin.execute: illegal status = " + req.getStatus());
            bb.publishRemove(req); 
        }
      }
    }
  }

}


