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
import org.cougaar.core.service.BlackboardService;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.blackboard.UniqueObjectSet;
import org.cougaar.core.util.UID;
import org.cougaar.core.service.UIDService;

/**
 * This Plugin requests for heartbeats (HbReq) and responds with
 * HeartbeatRequest.ACCEPTED or HeartbeatRequest.REFUSED.  
 * It should be installed in agents that might send out heartbeats.
 **/
public class HeartbeatServerPlugin extends ComponentPlugin {
  private IncrementalSubscription sub;
  private BlackboardService bb;
  private UniqueObjectSet heartbeatTable;

  private UnaryPredicate hbReqPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof HbReq);
    }
  };

  protected void setupSubscriptions() {
    heartbeatTable = new UniqueObjectSet();
    bb = getBlackboardService();
    sub = (IncrementalSubscription)bb.subscribe(hbReqPred);
  }

  protected void execute() {
    Iterator iter = sub.getAddedCollection().iterator();
    while (iter.hasNext()) {
      HbReq req = (HbReq)iter.next();

      System.out.println("HeartbeatServerPlugin.execute: received HbReq = " + req);
      req.updateResponse(null, new HbReqResponse(HeartbeatRequest.ACCEPTED));
// TODO: add check for dup hb in table
      MessageAddress myAddr = getBindingSite().getAgentIdentifier();
      MessageAddress target = req.getSource();
      Heartbeat hb = new Heartbeat(getUIDService().nextUID(), myAddr, target, null, null);
      bb.publishChange(req);
      System.out.println("HeartbeatServerPlugin.execute: published changed HbReq = " + req);
      heartbeatTable.add(req);   
      bb.publishChange(heartbeatTable);
    }
  }

  private UIDService UIDService;
   
  public UIDService getUIDService() {
      return this.UIDService;
  }
   
  public void setUIDService(UIDService UIDService) {
      this.UIDService = UIDService;
  }

}
