/*
 * <copyright>
 *  Copyright 1997-2001 Mobile Intelligence Corp
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
package org.cougaar.tools.robustness.ma.plugins;

import java.util.Date;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.tools.robustness.HealthReport;

/**
 * Object for tracking status of a monitored agent.  Records time last Heartbeat
 * was received.
 */

public class HealthStatus {

  private int currentStatus = HealthReport.UNDEFINED;
  private Date statusTimestamp;

  private MessageAddress agentId;

  //private Date lastHeartbeatTime;
  //private boolean heartbeatTimedout = false;

  //private boolean pingRequested = false;
  private Date pingTimestamp;
  private int pingStatus = -1;

  protected HealthStatus(MessageAddress agentId) {
    this.agentId = agentId;
    setTimestamp(new Date());
  }

  protected int getStatus() {
    return currentStatus;
  }

  protected void setStatus(int status) {
    this.currentStatus = status;
    statusTimestamp = new Date();
  }

  protected Date getTimestamp() {
    return statusTimestamp;
  }

  protected void setTimestamp(Date timestamp) {
    this.statusTimestamp = timestamp;
  }

  protected MessageAddress getAgentId() {
    return agentId;
  }

  protected void heartbeatReceived(Date heartbeatTimestamp) {
    setStatus(HealthReport.ALIVE);
    setTimestamp(heartbeatTimestamp);
  }

  //protected void setLastHeartbeatTime() {
  //  lastHeartbeatTime = new Date();
  //}

  //protected Date lastHeartbeat() {
  //  return lastHeartbeatTime;
  //}

  //protected void setHeartbeatTimedout(boolean timedout) {
  //  this.heartbeatTimedout = timedout ;
  //}

  //protected boolean isHeartbeatTimedout() {
  //  return this.heartbeatTimedout;
  //}

  //protected void setPingRequested(boolean b) {
  //  this.pingRequested = b;
  //}

  protected void setPingTimestamp(Date time) {
    this.pingTimestamp = time;
  }

  protected Date getPingTimestamp() {
    return this.pingTimestamp;
  }

  //protected boolean isPingRequested() {
  //  return this.pingRequested;
  //}

  protected void setPingStatus(int pingStatus) {
    this.pingStatus = pingStatus;
  }

  protected int getPingStatus() {
    return this.pingStatus;
  }

}