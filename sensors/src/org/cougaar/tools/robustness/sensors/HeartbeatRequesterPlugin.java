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
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.util.UID;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.blackboard.UniqueObjectSet;
import java.util.Date;

/**
 * This Plugin receives HeartbeatRequests from the local Blackboard and
 * sends HbReqs to the target agent's HeartbeatServerPlugin. It should 
 * be installed in the Agent that is originating the HeartbeatRequests.
 **/
public class HeartbeatRequesterPlugin extends ComponentPlugin {
  private IncrementalSubscription heartbeatRequestSub;
  private IncrementalSubscription hbReqSub;
  private BlackboardService bb;
  private UniqueObjectSet UIDtable;

  private UnaryPredicate HeartbeatRequestPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof HeartbeatRequest);
    }
  };

  private UnaryPredicate hbReqPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof HbReq);
    }
  };

  private void sendHbReq (HeartbeatRequest req) {
    MessageAddress source = getBindingSite().getAgentIdentifier();
    MessageAddress target = req.getTarget();
    UID reqUID = req.getUID();
    UIDtable.add(req);
    req.setStatus(HeartbeatRequest.SENT);
    req.setTimeSent(new Date());
    HbReqContent content = new HbReqContent(reqUID, req.getReqTimeout(), req.getHbFrequency(), req.getHbTimeout());
    HbReq hbReq = new HbReq(getUIDService().nextUID(), source, target, content, null);
    bb.publishAdd(hbReq);
    bb.publishChange(req);
    System.out.println("HeartbeatRequesterPlugin.sendHbReq: published new HbReq = " + hbReq);
    System.out.println("HeartbeatRequesterPlugin.sendHbReq: published changed HeartbeatRequest = " + req);
  }

  private void updateHeartbeatRequest (HbReq hbReq) {
    HbReqContent content = (HbReqContent)hbReq.getContent();
    UID reqUID = content.getHeartbeatRequestUID();
    HeartbeatRequest req = (HeartbeatRequest)UIDtable.findUniqueObject(reqUID);
    Date timeReceived = new Date();
    req.setTimeReceived(timeReceived);
    HbReqResponse response = (HbReqResponse)hbReq.getResponse();
    req.setStatus(response.getStatus());
    req.setRoundTripTime(timeReceived.getTime() - req.getTimeSent().getTime());
    bb.publishChange(req);
    bb.publishRemove(hbReq);
    System.out.println("HeartbeatRequesterPlugin.updateHeartbeatRequest: published changed HeartbeatRequest = " + req);
    System.out.println("HeartbeatRequesterPlugin.updateHeartbeatRequest: removed HbReq = " + hbReq);
  }

  protected void setupSubscriptions() {
    UIDtable = new UniqueObjectSet();
    bb = getBlackboardService();
    heartbeatRequestSub = (IncrementalSubscription)bb.subscribe(HeartbeatRequestPred);
    hbReqSub = (IncrementalSubscription)bb.subscribe(hbReqPred);
  }

  protected void execute() {
    // check for new HeartbeatRequests
    Iterator iter = heartbeatRequestSub.getAddedCollection().iterator();
    while (iter.hasNext()) {
      HeartbeatRequest req = (HeartbeatRequest)iter.next();
      System.out.println("HeartbeatRequesterPlugin.execute: new HeartbeatRequest received = " + req);
      MessageAddress myAddr = getBindingSite().getAgentIdentifier();
      if (req.getStatus() == HeartbeatRequest.NEW) {   
        sendHbReq(req);
      }
    }
    // check for responses from HeartbeatServerPlugin
    iter = hbReqSub.getChangedCollection().iterator();
    while (iter.hasNext()) {
      HbReq hbReq = (HbReq)iter.next();
      System.out.println("HeartbeatRequesterPlugin.execute: changed HbReq received = " + hbReq);
      MessageAddress myAddr = getBindingSite().getAgentIdentifier();
      if (hbReq.getSource().equals(myAddr)) {
        updateHeartbeatRequest(hbReq);
      }
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
