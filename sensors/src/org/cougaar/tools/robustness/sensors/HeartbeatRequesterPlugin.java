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
import java.util.Hashtable;
import org.cougaar.core.agent.service.alarm.Alarm;

/**
 * This Plugin receives HeartbeatRequests from the local Blackboard and
 * sends HbReqs to the target agent's HeartbeatServerPlugin. It should 
 * be installed in the Agent that is originating the HeartbeatRequests.
 **/
public class HeartbeatRequesterPlugin extends ComponentPlugin {
  private IncrementalSubscription heartbeatRequestSub;
  private IncrementalSubscription hbReqSub;
  private BlackboardService bb;
  private UniqueObjectSet reqTable;
  private Hashtable hbTable;
  private Hashtable reportTable;

  private class SendHeathReportsAlarm implements Alarm {
    private long detonate = -1;
    private boolean expired = false;

    public SendHeathReportsAlarm (long delay) {
      detonate = delay + System.currentTimeMillis();
    }

    public long getExpirationTime () {
      return detonate;
    }

    public void expire () {
      if (!expired) {
        bb.openTransaction();
        prepareHealthReports();
        bb.closeTransaction();
        expired = true;
      }
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
    reqTable.add(req);
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
    HeartbeatRequest req = (HeartbeatRequest)reqTable.findUniqueObject(reqUID);
    Date timeReceived = new Date();
    req.setTimeReceived(timeReceived);
    HbReqResponse response = (HbReqResponse)hbReq.getResponse();
    req.setStatus(response.getStatus());
    req.setRoundTripTime(timeReceived.getTime() - req.getTimeSent().getTime());
    bb.publishChange(req);
    //bb.publishRemove(hbReq);
    System.out.println("HeartbeatRequesterPlugin.updateHeartbeatRequest: published changed HeartbeatRequest = " + req);
    //System.out.println("HeartbeatRequesterPlugin.updateHeartbeatRequest: removed HbReq = " + hbReq);
  }

  private void prepareHealthReports() {
    // process NEW and ACCEPTED HeartbeatRequests
    long minFreq = Long.MAX_VALUE;  // milliseconds until next HeartbeatHealthReport should be sent
    Iterator iter = heartbeatRequestSub.getCollection().iterator();
    while (iter.hasNext()) {
      HeartbeatRequest req = (HeartbeatRequest)iter.next();
      int status = req.getStatus();
      if (status == HeartbeatRequest.NEW) {
        System.out.println("HeartbeatRequesterPlugin.execute: new HeartbeatRequest received = " + req);
        sendHbReq(req);
      } else if (status == HeartbeatRequest.ACCEPTED) {
        // Is it time yet to send a health report for this request?
        long freq = req.getHbFrequency();
        long now = System.currentTimeMillis();
        // Get time last report was sent from reportTable?
        UID uid = req.getUID();
        long lastReportTime = ((Date)reportTable.get(uid)).getTime();
        long nextReportTime = (lastReportTime + freq);
        if (now < nextReportTime) {  // not time yet, so just set the next wakeup time 
          long timeUntilNextReport = nextReportTime - now;    
          if (minFreq > timeUntilNextReport) minFreq = timeUntilNextReport;
        } else {
          // now is the time
          if (minFreq > freq) minFreq = freq;
          MessageAddress target = req.getTarget();  // change here for multiple targets
          Date lastHbDate = (Date)hbTable.get(target);
          long nextHbTime = lastHbDate.getTime() + freq;
          long outOfSpec = now - nextHbTime;
          float percentOutOfSpec = (float)outOfSpec / (float)freq;
          // send report if requester wants report whether in or out or spec
          // or if the heartbeat is enough out of spec
          if ( req.getOnlyOutOfSpec() == false ||  
               percentOutOfSpec > req.getPercentOutOfSpec() ) {
            HeartbeatEntry [] entries = new HeartbeatEntry[1];
            entries[0] = new HeartbeatEntry(target, lastHbDate, percentOutOfSpec);
            HeartbeatHealthReport report = new HeartbeatHealthReport(entries);
            bb.publishAdd(report);
          }   
        }                                                  
      }
    }
    if (minFreq != Long.MAX_VALUE)
      alarmService.addRealTimeAlarm(new SendHeathReportsAlarm(minFreq));
  }

  protected void setupSubscriptions() {
    reqTable = new UniqueObjectSet();
    hbTable = new Hashtable();
    reportTable = new Hashtable();
    bb = getBlackboardService();
    heartbeatRequestSub = (IncrementalSubscription)bb.subscribe(HeartbeatRequestPred);
    hbReqSub = (IncrementalSubscription)bb.subscribe(hbReqPred);
  }

  protected void execute() {
    // check for responses from HeartbeatServerPlugin
    // the first response will cause the status in the HeartbeatRequest to be updated
    // all responses except REFUSED are considered heartbeats and are entered in hbTable
    // Eventually, heartbeats won't show up this way, as they will be filtered in MTS,
    // and fed into the Metrics Service and we will get them there.
    Iterator iter = hbReqSub.getChangedCollection().iterator();
    while (iter.hasNext()) {
      HbReq hbReq = (HbReq)iter.next();
      System.out.println("HeartbeatRequesterPlugin.execute: changed HbReq received = " + hbReq);
      MessageAddress myAddr = getBindingSite().getAgentIdentifier();
      if (hbReq.getSource().equals(myAddr)) {
        HbReqResponse response = (HbReqResponse)hbReq.getResponse();
        int status = response.getStatus();
        Date now = new Date();
        switch (status) {
          case HeartbeatRequest.ACCEPTED:
            hbTable.put(response.getResponder(), now); // falls through
            reportTable.put(((HbReqContent)hbReq.getContent()).getHeartbeatRequestUID(), now); // falls through
          case HeartbeatRequest.REFUSED:
            updateHeartbeatRequest(hbReq);
            break;
          case HbReqResponse.HEARTBEAT:
            hbTable.put(response.getResponder(), now);
          default:
            throw new RuntimeException("illegal status = " + status);
        }
      }
    }
    prepareHealthReports();
  }

  private UIDService UIDService;
   
  public UIDService getUIDService() {
      return this.UIDService;
  }
   
  public void setUIDService(UIDService UIDService) {
      this.UIDService = UIDService;
  }

}
