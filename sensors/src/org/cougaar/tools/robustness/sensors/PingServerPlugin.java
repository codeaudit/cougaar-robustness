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

import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.blackboard.IncrementalSubscription;
import java.util.Iterator;
import java.util.Set;
import java.util.Collections;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.LoggingService;

/**
 * This Plugin receives and echoes Pings that originate 
 * from the PingRequesterPlugin.  
 * It should be installed in agents that might be pinged.
 **/
public class PingServerPlugin extends ComponentPlugin {
  private IncrementalSubscription sub;
  private BlackboardService bb;
  private LoggingService log;

  private UnaryPredicate pingPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof Ping);
    }
  };

  protected void setupSubscriptions() {
    log =  (LoggingService) getBindingSite().getServiceBroker().
      getService(this, LoggingService.class, null);
    bb = getBlackboardService();
    sub = (IncrementalSubscription)bb.subscribe(pingPred);
  }

  protected void execute() {
    Iterator iter = sub.getAddedCollection().iterator();
    while (iter.hasNext()) {
      Ping ping = (Ping)iter.next();
      if (ping.getTargets() == Collections.EMPTY_SET) {  // make sure I'm the target, not the source
        MessageAddress me = getBindingSite().getAgentIdentifier();
        if (log.isDebugEnabled()) 
          log.debug("PingServerPlugin.execute: received Ping = " + ping);
        ping.updateResponse(me, "Got it!");
        bb.publishChange(ping);
        if (log.isDebugEnabled()) 
          log.debug("PingServerPlugin.execute: published changed Ping = " + ping);
      }
    }
  }

}
