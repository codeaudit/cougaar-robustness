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

/**
 * This Plugin requests for heartbeats (HbReq) and responds with
 * HeartbeatRequest.ACCEPTED or HeartbeatRequest.REFUSED.  
 * It should be installed in agents that might send out heartbeats.
 **/
public class HeartbeatServerPlugin extends ComponentPlugin {
  private IncrementalSubscription sub;
  private BlackboardService bb;

  private UnaryPredicate hbReqPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof HbReq);
    }
  };

  private class ProcessHeartbeatsAlarm implements Alarm {
    private long detonate = -1;
    private boolean expired = false;

    public ProcessHeartbeatsAlarm (long delay) {
      detonate = delay + System.currentTimeMillis();
    }

    public long getExpirationTime () {
      return detonate;
    }

    public void expire () {
      if (!expired) 
        processHeartbeats();
      expired = true;
    }

    public boolean hasExpired () {
      return expired;
    }

    public boolean cancel () {
      if (!expired)
        return expired = true;
      return false;
    }
  }

  private void processHeartbeats() {
    bb.openTransaction();
    execute();
    bb.closeTransaction();
  } 

  protected void setupSubscriptions() {
    bb = getBlackboardService();
    sub = (IncrementalSubscription)bb.subscribe(hbReqPred);
  }

  protected void execute() {
    long minFreq = Long.MAX_VALUE;  // milliseconds until next heartbeat should be sent
    Iterator iter = sub.getCollection().iterator();
    while (iter.hasNext()) {
      HbReq req = (HbReq)iter.next();
      System.out.println("HeartbeatServerPlugin.execute: received HbReq = " + req);
      HbReqContent content = (HbReqContent)req.getContent();
      long now = System.currentTimeMillis();
      long lastHb = content.getLastHbSent();
      long freq = content.getHbFrequency();
      // handle new request
      if (lastHb == -1) {
        if (minFreq > freq) minFreq = freq;
        content.setLastHbSent(now);
        req.updateContent(content, null);
        MessageAddress me = getBindingSite().getAgentIdentifier();
        req.updateResponse(me, new HbReqResponse(me, HeartbeatRequest.ACCEPTED));
        bb.publishChange(req);
        System.out.println("HeartbeatServerPlugin.execute: published changed HbReq = " + req);
      } else {
        // check if its time to send a heartbeat or not for this one
        long nextHb = (lastHb + freq);
        if (now >= nextHb) {   // its time for this one
          if (minFreq > freq) minFreq = freq;
          content.setLastHbSent(now);
          bb.publishChange(req);
          System.out.println("HeartbeatServerPlugin.execute: published changed HbReq = " + req);
        } else {  // its not time yet for this one
          long fromNow = nextHb - now;
          if (minFreq > fromNow) minFreq = fromNow;
        }
      }
    }
    if (minFreq != Long.MAX_VALUE)
      alarmService.addRealTimeAlarm(new ProcessHeartbeatsAlarm(minFreq));
  } 

}
