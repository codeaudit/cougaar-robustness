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
import org.cougaar.core.service.BlackboardService;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.LoggingService;

/**
 * This Plugin requests for heartbeats (HbReq) and responds with
 * HeartbeatRequest.ACCEPTED or HeartbeatRequest.REFUSED.  
 * It should be installed in agents that might send out heartbeats.
 **/
public class HeartbeatServerPlugin extends ComponentPlugin {
  private IncrementalSubscription sub;
  private BlackboardService bb;
  private LoggingService log;

  private UnaryPredicate hbReqPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof HbReq);
    }
  };

  private class ProcessHeartbeatsAlarm implements Alarm {
    private long detonate = -1;
    private boolean expired = false;

    /**
     * Create an Alarm to go off in the milliseconds specified,
     * to send out heartbeats on schedule.
     **/
    public ProcessHeartbeatsAlarm (long delay) {
      detonate = delay + System.currentTimeMillis();
    }

    /** @return absolute time (in milliseconds) that the Alarm should
     * go off.  
     * This value must be implemented as a fixed value.
     **/
    public long getExpirationTime () {
      return detonate;
    }

    /** 
     * Called by the cluster clock when clock-time >= getExpirationTime().
     **/
    public void expire () {
      if (!expired) {
        bb.openTransaction();
        processHeartbeats();
        bb.closeTransaction();
        expired = true;
      }
    }

    /** @return true IFF the alarm has expired or was canceled. **/
    public boolean hasExpired () {
      return expired;
    }

    /** can be called by a client to cancel the alarm.  May or may not remove
     * the alarm from the queue, but should prevent expire from doing anything.
     * @return false IF the the alarm has already expired or was already canceled.
     **/
    public boolean cancel () {
      if (!expired)
        return expired = true;
      return false;
    }
  }

  private void processHeartbeats() {
    long minFreq = Long.MAX_VALUE;  // milliseconds until next heartbeat should be sent
    Iterator iter = sub.getCollection().iterator();
    while (iter.hasNext()) {
      HbReq req = (HbReq)iter.next();
      if (log.isDebugEnabled()) 
        log.debug("\nHeartbeatServerPlugin.processHeartbeats: processing HbReq = " + req);
      HbReqContent content = (HbReqContent)req.getContent();
      HbReqResponse response = (HbReqResponse)req.getResponse();
      long now = System.currentTimeMillis();
      long lastHb = response.getLastHbSent();
      long freq = content.getHbFrequency();
      // ignore new requests here - execute will handle
      if (lastHb != -1) {
        // check if its time to send a heartbeat or not for this one
        long nextHb = (lastHb + freq);
        if (now >= nextHb) {   // its time for this one
          if (minFreq > freq) minFreq = freq;
          MessageAddress me = getBindingSite().getAgentIdentifier();
          response.setResponder(me);
          response.setStatus(HbReqResponse.HEARTBEAT);
          response.setLastHbSent(now);
          req.updateResponse(me, response);
          bb.publishChange(req);
          if (log.isDebugEnabled()) 
            log.debug("\nHeartbeatServerPlugin.processHeartbeats: published changed HbReq = " + req);
        } else {  // its not time yet for this one
          long fromNow = nextHb - now;
          if (minFreq > fromNow) minFreq = fromNow;
        }
      }
    }
    if (minFreq != Long.MAX_VALUE)
      alarmService.addRealTimeAlarm(new ProcessHeartbeatsAlarm(minFreq));
  } 

  protected void setupSubscriptions() {
    log =  (LoggingService) getBindingSite().getServiceBroker().
      getService(this, LoggingService.class, null);
    bb = getBlackboardService();
    sub = (IncrementalSubscription)bb.subscribe(hbReqPred);
    processHeartbeats();
  }

  protected void execute() {
    long minFreq = Long.MAX_VALUE;  // milliseconds until next heartbeat should be sent
    Iterator iter = sub.getAddedCollection().iterator();
    while (iter.hasNext()) {
      HbReq req = (HbReq)iter.next();
      if (log.isDebugEnabled()) 
        log.debug("\nHeartbeatServerPlugin.execute: received new HbReq = " + req);
      HbReqContent content = (HbReqContent)req.getContent();
      long now = System.currentTimeMillis();
      long freq = content.getHbFrequency();
      if (minFreq > freq) minFreq = freq;
      MessageAddress me = getBindingSite().getAgentIdentifier();
      req.updateResponse(me, new HbReqResponse(me, HeartbeatRequest.ACCEPTED, now));
      bb.publishChange(req);
      if (log.isDebugEnabled()) 
        log.debug("\nHeartbeatServerPlugin.execute: published changed HbReq = " + req);
    } 
    if (minFreq != Long.MAX_VALUE)
      alarmService.addRealTimeAlarm(new ProcessHeartbeatsAlarm(minFreq));
  } 

}
