/*
 * <copyright>
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

import java.util.*;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.community.CommunityService;
import org.cougaar.core.blackboard.Publishable;
import org.cougaar.tools.robustness.sensors.HeartbeatRequest;
import org.cougaar.tools.robustness.sensors.HeartbeatEntry;
import org.cougaar.tools.robustness.sensors.PingRequest;
import org.cougaar.util.log.*;

import javax.naming.directory.*;

/**
 * Class for tracking status of a monitored agent.
 */

public class HealthStatus implements
  Publishable, org.cougaar.core.persist.NotPersistable {

  private Logger log =
    Logging.getLogger(org.cougaar.tools.robustness.ma.plugins.HealthStatus.class.getName());
  public static final int UNDEFINED   = -1;

  // Run States
  public static final String INITIAL          = "INITIAL";
  public static final String NORMAL           = "NORMAL";
  public static final String HEALTH_CHECK     = "HEALTH_CHECK";
  public static final String RESTART          = "RESTART";
  public static final String RESTART_COMPLETE = "RESTART_COMPLETE";
  public static final String FAILED_RESTART   = "FAILED_RESTART";
  public static final String MOVE             = "MOVE";
  public static final String FAILED_MOVE      = "FAILED_MOVE";

  // Heartbeat status codes
  public static final int HB_NORMAL  = 0;
  public static final int HB_TIMEOUT = 1;

  // Health Status codes
  public static final int OK          = 0;
  public static final int DEGRADED    = 1;
  public static final int NO_RESPONSE = 2;
  public static final int DEAD        = 3;
  public static final int RESTARTED   = 4;
  public static final int MOVED       = 5;


  private String currentState = INITIAL;
  private String priorState   = INITIAL;
  private int currentStatus   = UNDEFINED;

  private int heartbeatRequestStatus = UNDEFINED;
  private Date heartbeatRequestTime = null;
  private int heartbeatStatus = UNDEFINED;
  private HeartbeatEntry heartbeatEntry = null;

  private boolean pingRequested = false;
  private int pingStatus = UNDEFINED;
  private Date pingTimestamp;

  private Date lastRestartAttempt;

  private long hbReqTimeout;  // Defines the timeout period (in milliseconds)
                              // for a Heartbeat Request,
  private long hbReqRetries;  // Defines the number of times to retry a
                              // HeartbeatRequest when a failure is encountered
  private long hbFreq;        // Defines the frequency (in milliseconds) at
                              // which the monitored agents are to send
                              // heartbeats to the management agent.
  private long hbTimeout;     // Defines the timeout period (in milliseconds)
                              // for heartbeats.
  private float hbPctLate;    // Defines a tolerance for late heartbeats before
                              // they are reported in a HeartbeatHealthReport
                              // from sensor plugins.  The value is defined as a
                              // Float number representing a percentage of the
                              // hbTimeout.
  private long hbWindow;      // Defines the interval (in milliseconds) over
                              // which a Heartbeat failure rate is calculated.
                              // The heartbeat failure rate is the percentage of
                              // late hearbeats vs expected heartbeats.
  private float hbFailRate;   // Defines the heartbeat failure rate threshold.
                              // When the calculated heartbeat failure rate
                              // exceeds this threshold the monitored agent is
                              // placed in a "HEALTH-CHECK" state for further
                              // evaluation by the Management Agent.  This value
                              // is defined as a Float number that represents
                              // the maximum acceptable heartbeat failure rate.
  private long pingTimeout;   // Defines the ping timeout period
                              // (in milliseconds).
  private long pingRetries;   // Defines the number of times to retry a ping


  private List heartbeatTimeouts = new Vector();
  private Date statusTimestamp;

  private MessageAddress agentId;
  private String communityName;

  private HeartbeatRequest hbr;
  private int hbReqRetryCtr = 0;

  private PingRequest pr;
  private int pingRetryCtr = 0;

  private CommunityService commSvc = null;


  /**
   * HealthStatus constructor.
   * @param agentId        MessageAddress of monitored agent.
   * @param communityName  Name of agents Robustness community.
   * @param sb             Reference to ManagementAgents ServiceBroker
   * @param hbReqTimeout   Timeout period for Heartbeat Requests
   * @param hbReqRetries   Number of times to retry a Heartbeat request
   * @param hbFreq         Rate at which heartbeats are received
   * @param hbTimeout      Heartbeat timeout period
   * @param hbPctLate      Period at which a late Heartbeat is reported
   * @param hbWindow       Lookback period for calculating heartbeat failure rate
   * @param hbFailRate     Threshold for heartbeat failures
   * @param pingTimeout    Timeout period for pings
   * @param pingRetries    Number of times to retry a ping
   */
  protected HealthStatus(MessageAddress agentId,
                         String communityName,
                         ServiceBroker sb,
                         long hbReqTimeout,
                         long hbReqRetries,
                         long hbFreq,
                         long hbTimeout,
                         float hbPctLate,
                         long hbWindow,
                         float hbFailRate,
                         long pingTimeout,
                         long pingRetries) {
    this.agentId = agentId;
    this.communityName = communityName;
    this.commSvc =
      (CommunityService)sb.getService(this, CommunityService.class, null);
    this.hbReqTimeout = hbReqTimeout;
    this.hbReqRetries = hbReqRetries;
    this.hbFreq = hbFreq;
    this.hbTimeout = hbTimeout;
    this.hbPctLate = hbPctLate;
    this.hbWindow = hbWindow;
    this.hbFailRate = hbFailRate;
    this.pingTimeout = pingTimeout;
    this.pingRetries = pingRetries;
    setState(currentState);
  }

  /**
   * Returns current Heartbeat frequency.
   * @return Heartbeat frequency
   */
  protected long getHbFrequency() {
    return hbFreq;
  }

  /**
   * Sets agents Heartbeat frequency.
   * @param freq New frequency
   */
  protected void setHbFrequency(long freq) {
    this.hbFreq = freq;
  }

  /**
   * Returns Heartbeat Request timeout.
   * @return Heartbeat request timeout
   */
  protected long getHbReqTimeout() {
    return hbReqTimeout;
  }

  /**
   * Sets agents Heartbeat request timeout.
   * @param timeout New timeout
   */
  protected void setHbReqTimeout(long timeout) {
    this.hbReqTimeout = timeout;
  }

  /**
   * Returns Heartbeat Request retries.
   * @return Heartbeat request retries
   */
  protected long getHbReqRetries() {
    return hbReqRetries;
  }

  /**
   * Sets agents Heartbeat request retries.
   * @param retries Number of retries to perform
   */
  protected void setHbReqRetries(long retries) {
    this.hbReqRetries = retries;
  }

  /**
   * Returns current Heartbeat failure rate threshold.
   * @return Heartbeat failure rate threshold
   */
  protected float getHbFailRate() {
    return hbFailRate;
  }

  /**
   * Sets agents Heartbeat failure rate threshold.
   * @param rate New rate.
   */
  protected void setHbFailRate(float rate) {
    this.hbFailRate = rate;
  }

  /**
   * Gets current size of lookback period that is used to calculate the
   * Heartbeat failure rate.
   * @return Duration of failure rate period (in milliseconds)
   */
  protected float getHbWindow() {
    return hbWindow;
  }

  /**
   * Gets current size of lookback period that is used to calculate the
   * Heartbeat failure rate.
   * @param duration Duration in milliseconds.
   */
  protected void setHbWindow(long duration) {
    this.hbWindow = duration;
  }

  /**
   * Returns Heartbeat timeout value.
   * @return Heartbeat timeout
   */
  protected long getHbTimeout() {
    return hbTimeout;
  }

  /**
   * Sets agents Heartbeat timeout value
   * @param timeout Heartbeat timeout
   */
  protected void setHbTimeout(long timeout) {
    this.hbTimeout = timeout;
  }

  /**
   * Returns Heartbeat pct late value.
   * @return Heartbeat pct late
   */
  protected float getHbPctLate() {
    return hbPctLate;
  }

  /**
   * Sets agents Heartbeat pct late value
   * @param pct  Pct late tolerance
   */
  protected void setHbPctLate(float pct) {
    this.hbPctLate = pct;
  }

  /**
   * Returns ping timeout.
   * @return Ping timeout
   */
  protected long getPingTimeout() {
    return pingTimeout;
  }

  /**
   * Sets agents ping timeout.
   * @param timeout New timeout
   */
  protected void setPingTimeout(long timeout) {
    this.pingTimeout = timeout;
  }

  /**
   * Returns Ping retries.
   * @return Ping retries
   */
  protected long getPingRetries() {
    return pingRetries;
  }

  /**
   * Sets agents Ping retries.
   * @param retries Number of retries to perform
   */
  protected void setPingRetries(long retries) {
    this.pingRetries = retries;
  }

  /**
   * Determines if current failure rate exceeds threshold.
   * Heartbeat failure rate.
   * @return True if rate is above limit established by
   *         HeartbeatFailureRateThreshold
   */
  protected boolean hbFailureRateInSpec() {
    return getFailureRate() < hbFailRate;
  }

  /**
   * Returns monitored agents current status.
   * @return  Status code
   */
  protected int getStatus() {
    return currentStatus;
  }


  /**
   * Returns monitored agents prior state.
   * @return  State string
   */
  protected String getPriorState() {
    return priorState;
  }


  /**
   * Sets monitored agents prior state.
   * @param  status State string
   */
  protected void setPriorState(String state) {
    this.priorState = state;
  }


  /**
   * Sets monitored agents status.
   * @param status Status code
   */
  protected void setStatus(int status) {
    this.currentStatus = status;
    statusTimestamp = new Date();
  }

  /**
   * Returns current status of HeartbeatRequest associated with monitored agent.
   * @return HeartbeatRequest status
   * (refer to org.cougaar.tools.robustness.sensors.HeartbeatRequest for values)
   */
  protected int getHeartbeatRequestStatus() {
    return heartbeatRequestStatus;
  }

  /**
   * Sets current status of HeartbeatRequest associated with monitored agent.
   * @param status HeartbeatRequest status
   * (refer to org.cougaar.tools.robustness.sensors.HeartbeatRequest for values)
   */
  protected void setHeartbeatRequestStatus(int status) {
    this.heartbeatRequestStatus = status;
  }

  /**
   * Returns the time that the HeartbeatRequest was published.
   * @return Time that the HeartbeatRequest was issued
   */
  protected Date getHeartbeatRequestTime() {
    return heartbeatRequestTime;
  }

  /**
   * Records the time that the HeartbeatRequest was published.
   * @param time Time that the HeartbeatRequest was issued
   */
  protected void setHeartbeatRequestTime(Date time) {
    this.heartbeatRequestTime = time;
  }

  /**
   * Returns current Heartbeat status.
   * @return Status code associated with heartbeat status.
   * (refer to org.cougaar.tools.robustness.sensors.HeartbeatEntry for values)
   */
  protected int getHeartbeatStatus() {
    return heartbeatStatus;
  }

  /**
   * Sets current Heartbeat status.
   * @param status Status code associated with heartbeat status.
   * (refer to org.cougaar.tools.robustness.sensors.HeartbeatEntry for values)
   */
  protected void setHeartbeatStatus(int status) {
    this.heartbeatStatus = status;
  }


  /**
   * Returns the HeartbeatEntry object associated with this Agent.
   * @return HeartbeatEntry for this agent.
   */
  protected HeartbeatEntry getHeartbeatEntry() {
    return heartbeatEntry;
  }

  /**
   * Sets HeartbeatEntry.
   * @param hbe HeartbeatEntry object associated with this agent.
   */
  protected void setHeartbeatEntry(HeartbeatEntry hbe) {
    this.heartbeatEntry = hbe;
  }


  /**
   * Returns HeartbeatRequest associated with this agent.
   * @return HeartbeatRequest
   */
  protected HeartbeatRequest getHeartbeatRequest() {
    return this.hbr;
  }

  /**
   * Sets HeartbeatRequest
   * @param hbr HeartbeatRequest associated with this agent
   */
  protected void setHeartbeatRequest(HeartbeatRequest hbr) {
    this.hbr = hbr;
  }


  /**
   * Returns PingRequest associated with this agent.
   * @return PingRequest
   */
  protected PingRequest getPingRequest() {
    return this.pr;
  }

  /**
   * Sets PingRequest
   * @param pr PingRequest associated with this agent
   */
  protected void setPingRequest(PingRequest pr) {
    this.pr = pr;
  }


  /**
   * Returns the ID (MessageAddress) associated with this monitored agent.
   * @return Agents MessageAddress
   */
  protected MessageAddress getAgentId() {
    return agentId;
  }

  /**
   * Get a list of the current Heartbeat timeouts.
   * @return Current Heartbeat timeouts
   */
  private List getHeartbeatTimeouts() {
    return heartbeatTimeouts;
  }

  /**
   * Returns time of last restart attempt.
   * @return Date Time of last attempted restart
   */
  protected Date getLastRestartAttempt() {
    return lastRestartAttempt;
  }

  /**
   * Sets time of last restart attempt.
   * @param restartAttempted Time of last attempted restart
   */
  protected Date setLastRestartAttempt(Date restartAttempted) {
    return lastRestartAttempt = restartAttempted;
  }

  /**
   * Adds a new heartbeat timeout to List.
   * @param timeout  Time of detected timeout.
   */
  protected void addHeartbeatTimeout(Date timeout) {
    long tolerance = hbFreq/2;
    pruneTimeoutList();
    long to = timeout.getTime();
    for (Iterator it = heartbeatTimeouts.iterator(); it.hasNext();) {
      long item = ((Date)it.next()).getTime();
      if (to < item + tolerance &&
          to > item - tolerance) return;
    }
    heartbeatTimeouts.add(timeout);
  }

  /**
   * Removes Heartbeat timeouts from list if they fall outside of the Heartbeat
   * Failure calculation window.
   */
  private void pruneTimeoutList() {
    Date timeout;
    long now = new Date().getTime();
    Date cutoff = new Date(now - hbWindow);
    for (Iterator it = heartbeatTimeouts.iterator(); it.hasNext();) {
      timeout = (Date)it.next();
      if (timeout.before(cutoff)) {
        it.remove();
      }
    }
  }

  /**
   * Calculates the current heartbeat failure rate.  The rate is the percentage
   * of late heartbeats vs. expected heartbeats within lookback period.
   * @return  Percent of late heartbeats in evaluation period
   */
  protected float getFailureRate() {
    pruneTimeoutList();
    float lateHeartbeats = heartbeatTimeouts.size();
    if (lateHeartbeats == 0) return 0.0f;
    float totalHeartbeats = hbWindow/hbFreq;
    float rate = lateHeartbeats/totalHeartbeats;
    return (rate > 1.0f ? 1.0f : rate);
  }


  protected String failureRateData() {
    pruneTimeoutList();
    float lateHeartbeats = heartbeatTimeouts.size();
    float totalHeartbeats = hbWindow/hbFreq;
    float rate = lateHeartbeats/totalHeartbeats;
    return "HbFailureRateData: agent=" + getAgentId() +
      ", lateHeartbeats=" + lateHeartbeats +
      ", totalHeartbeats=" + totalHeartbeats +
      ", rate=" + rate;
  }


  /**
   * Sets flag indicating that a ping has been sent to monitored agent.
   * @param b  Set to true if a ping has been requested.
   */
  protected void setPingRequested(boolean b) {
    this.pingRequested = b;
  }

  /**
   * Indicates whether there is a pending ping request.
   * @return  True if a ping request has been issued.
   */
  protected boolean pingRequested() {
    return this.pingRequested;
  }

  /**
   * Sets HeartbeatRequestRetries counter
   * @param retries  Number of retries to attempt
   */
  protected void setHbReqRetryCtr(int retries) {
    this.hbReqRetryCtr = retries;
  }

  /**
   * Gets HeartbeatRequestRetries counter
   * @return  Number of times HeartbeatRequests have been resent
   */
  protected int getHbReqRetryCtr() {
    return this.hbReqRetryCtr;
  }

  /**
   * Sets PingRetries counter
   * @param retries  Number of retries to attempt
   */
  protected void setPingRetryCtr(int retries) {
    this.pingRetryCtr = retries;
  }

  /**
   * Gets PingRetries counter
   * @return  Number of times Pings have been resent
   */
  protected int getPingRetryCtr() {
    return this.pingRetryCtr;
  }

  /**
   * Sets current run state for monitored agent.  At startup the agent is put
   * in the INITIAL state until its health can be verified.  Once contact
   * is made the agent is placed in the NORMAL state.  When runtime anomalies
   * are detected (such as no heartbeats) the agents state is changed to
   * HEALTH_CHECK which triggers analysis by a ManagementAgent Decision plugin.
   * Based on its analysis the Decision plugin will either put the agent into
   * the NORMAL or RESTART state.
   * @param state Current run state of monitored agent
   */
  protected void setState(String state) {
    log.debug("SetState: agent=" + agentId + " state=" + state);
    currentState = state;
    ModificationItem mods[] = new ModificationItem[1];
    mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
      new BasicAttribute("RunState", currentState));
    commSvc.modifyEntityAttributes(communityName, agentId.toString(), mods);
    //Attributes attrs = new BasicAttributes("RunState", currentState);
    //commSvc.setEntityAttributes(communityName, agentId.toString(), attrs);
  }

  /**
   * Returns monitored agents current run state.
   * @return Current run state for agent.
   */
  protected String getState() {
    //return currentState;


    try {
      Attributes attrs =
        commSvc.getEntityAttributes(communityName, agentId.toString());
      Attribute stateAttr = attrs.get("RunState");
      if (stateAttr != null) {
        String state = (String)stateAttr.get();
        if (state == null || state.trim().length() == 0) {
          setState(currentState);
          log.debug("GetState: agent=" + agentId +
            " state is null/empty, setting state to " + currentState);
          return currentState;
        } else {
          log.debug("GetState: agent=" + agentId + " state=" + state);
          return state;
        }
      } else {
        log.debug("GetState: agent=" + agentId +
          " state attribute not defined, setting state to " + currentState);
        setState(currentState);
        return currentState;
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      return new String();
    }


  }


  /**
   * Records time that a ping was requested.
   * @param time  Time of ping request
   */
  protected void setPingTimestamp(Date time) {
    this.pingTimestamp = time;
  }

  /**
   * Returns the time of a ping request.
   * @return  Time of ping request
   */
  protected Date getPingTimestamp() {
    return this.pingTimestamp;
  }

  /**
   * Sets status of a pending Ping.
   * @param status Status code associated with ping status.
   * (refer to org.cougaar.tools.robustness.sensors.PingRequest for values)
   */
  protected void setPingStatus(int pingStatus) {
    this.pingStatus = pingStatus;
  }

  /**
   * Returns status of a pending Ping.
   * @return status Status code associated with ping status.
   * (refer to org.cougaar.tools.robustness.sensors.PingRequest for values)
   */
  protected int getPingStatus() {
    return this.pingStatus;
  }

  public boolean isPersistable() {
    return false;
  }
}