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
import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.service.LoggingService;

/**
 * This Plugin receives PingRequests from the local Blackboard and
 * sends Pings to the target agent's PingServerPlugin.
 * It should be installed in the Agent that is originating the PingRequests.
 **/
public class PingRequesterPlugin extends ComponentPlugin {
  private IncrementalSubscription pingReqSub;
  private IncrementalSubscription pingSub;
  private BlackboardService bb;
  private LoggingService log;
  private UniqueObjectSet UIDtable;

  private UnaryPredicate pingReqPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof PingRequest);
    }
  };

  private UnaryPredicate pingPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof Ping);
    }
  };

  private void sendPing (PingRequest req) {
    MessageAddress source = getBindingSite().getAgentIdentifier();
    MessageAddress target = req.getTarget();
    UID reqUID = req.getUID();
    UIDtable.add(req);
    req.setStatus(PingRequest.SENT);
    req.setTimeSent(new Date());
    PingContent content = new PingContent(reqUID, req.getTimeout());
    Ping ping = new Ping(getUIDService().nextUID(), source, target, content, null);
    bb.publishAdd(ping);
    bb.publishChange(req);
    if (log.isDebugEnabled()) {
      log.debug("PingRequesterPlugin.sendPing: published new Ping = " + ping);
      log.debug("PingRequesterPlugin.sendPing: published changed PingRequest = " + req);
    }
    // temp hack to timeout a request
    alarmService.addRealTimeAlarm(new PingRequestTimeout(req.getTimeout(),reqUID));
  }

  private void updatePingRequest (Ping ping) {
    PingContent content = (PingContent)ping.getContent();
    UID reqUID = content.getPingReqUID();
    PingRequest req = (PingRequest)UIDtable.findUniqueObject(reqUID);
    Date timeReceived = new Date();
    req.setTimeReceived(timeReceived);
    req.setStatus(PingRequest.RECEIVED);
    req.setRoundTripTime(timeReceived.getTime() - req.getTimeSent().getTime());
    bb.publishChange(req);
    bb.publishRemove(ping);
    if (log.isDebugEnabled()) {
      log.debug("PingRequesterPlugin.updatePingRequest: published changed PingRequest = " + req);
      log.debug("PingRequesterPlugin.updatePingRequest: removed Ping = " + ping);
    }
  }

  protected void setupSubscriptions() {
    UIDtable = new UniqueObjectSet();
    bb = getBlackboardService();
    pingReqSub = (IncrementalSubscription)bb.subscribe(pingReqPred);
    pingSub = (IncrementalSubscription)bb.subscribe(pingPred);
  }

  protected void execute() {
    // check for new PingRequests
    Iterator iter = pingReqSub.getAddedCollection().iterator();
    while (iter.hasNext()) {
      PingRequest req = (PingRequest)iter.next();
      if (log.isDebugEnabled()) 
        log.debug("PingRequesterPlugin.execute: new PingRequest received = " + req);
      MessageAddress myAddr = getBindingSite().getAgentIdentifier();
      if (req.getStatus() == PingRequest.NEW) {   
        sendPing(req);
      }
    }
    // check for changed Pings
    iter = pingSub.getChangedCollection().iterator();
    while (iter.hasNext()) {
      Ping ping = (Ping)iter.next();
      if (log.isDebugEnabled()) 
        log.debug("PingRequesterPlugin.execute: changed Ping received = " + ping);
      MessageAddress myAddr = getBindingSite().getAgentIdentifier();
      if (ping.getSource().equals(myAddr)) {
        updatePingRequest(ping);
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

  // temporary implementation of PingRequest timeout.
  // will be replaced by MTS-based implementation.
  private class PingRequestTimeout implements Alarm {
    private long detonate = -1;
    private boolean expired = false;
    private UID reqUID;

    public PingRequestTimeout (long timeout, UID reqUID) {
      detonate = timeout + System.currentTimeMillis();
      this.reqUID = reqUID;
    }
    public long getExpirationTime () {return detonate;
    }
    public void expire () {
      if (!expired) {
        fail(reqUID);
        expired = true;}
    }
    public boolean hasExpired () {return expired;
    }
    public boolean cancel () {
      if (!expired)
        return expired = true;
      return false;}
  }

  private void fail(UID reqUID) {
    PingRequest req = (PingRequest)UIDtable.findUniqueObject(reqUID);
    if (req.getStatus() == PingRequest.SENT) {
      bb.openTransaction();
      req.setStatus(PingRequest.FAILED);
      bb.publishChange(req);
      bb.closeTransaction();
      if (log.isDebugEnabled()) 
        log.debug("\nPingRequesterPlugin.fail: published changed PingRequest = " + req);
    }
  }


}
