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

import javax.naming.directory.*;

/**
 * Class for tracking status of a monitored agent.
 */

public class HealthStatus implements
  Publishable, org.cougaar.core.persist.NotPersistable {

  public static final int UNDEFINED   = -1;

  // Run States
  public static final String INITIAL          = "INITIAL";
  public static final String NORMAL           = "NORMAL";
  public static final String HEALTH_CHECK     = "HEALTH_CHECK";
  public static final String RESTART          = "RESTART";

  // Heartbeat status codes
  public static final int HB_NORMAL  = 0;
  public static final int HB_TIMEOUT = 1;

  // Health Status codes
  public static final int OK          = 0;
  public static final int DEGRADED    = 1;
  public static final int NO_RESPONSE = 2;
  public static final int DEAD        = 3;


  private String currentState = INITIAL;
  private int currentStatus   = UNDEFINED;

  private int heartbeatRequestStatus = UNDEFINED;
  private int heartbeatStatus = UNDEFINED;
  private Date heartbeatRequestTime = null;

  private boolean pingRequested = false;
  private int pingStatus = UNDEFINED;
  private Date pingTimestamp;

  private long  hbFrequency;
  private long  hbWindow;
  private float hbFailureRateThreshold;

  private List heartbeatTimeouts = new Vector();
  private Date statusTimestamp;

  private MessageAddress agentId;
  private String communityName;

  private CommunityService commSvc = null;


  /**
   * HealthStatus constructor.
   * @param agentId        MessageAddress of monitored agent.
   * @param communityName  Name of agents Robustness community.
   * @param sb             Reference to ManagementAgents ServiceBroker
   * @param hbFrequency    Rate at which heartbeats are received
   * @param hbWindow       Lookback period for calculating heartbeat failure rate
   * @param hbFailureRateThreshold  Threshold for heartbeat failures
   */
  protected HealthStatus(MessageAddress agentId,
                         String communityName,
                         ServiceBroker sb,
                         long hbFrequency,
                         long hbWindow,
                         float hbFailureRateThreshold) {
    this.agentId = agentId;
    this.communityName = communityName;
    this.commSvc =
      (CommunityService)sb.getService(this, CommunityService.class, null);
    this.hbFrequency = hbFrequency;
    this.hbWindow = hbWindow;
    this.hbFailureRateThreshold = hbFailureRateThreshold;
    setState(currentState);
  }

  /**
   * Returns current Heartbeat failure rate threshold.
   * @return Heartbeat failure rate threshold
   */
  protected float getHbFailureRateThreshold() {
    return hbFailureRateThreshold;
  }

  /**
   * Sets agents Heartbeat failure rate threshold.
   * @param rate New rate.
   */
  protected void setHbFailureRateThreshold(float rate) {
    this.hbFailureRateThreshold = rate;
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
   * Determines if current failure rate exceeds threshold.
   * Heartbeat failure rate.
   * @return True if rate is above limit established by
   *         HeartbeatFailureRateThreshold
   */
  protected boolean hbFailureRateInSpec() {
    System.out.println(failureRateData());
    return getFailureRate() < hbFailureRateThreshold;
  }

  /**
   * Returns monitored agents last known status.
   * @return  Status code
   */
  protected int getStatus() {
    return currentStatus;
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
   * sets current Heartbeat status.
   * @param status Status code associated with heartbeat status.
   * (refer to org.cougaar.tools.robustness.sensors.HeartbeatEntry for values)
   */
  protected void setHeartbeatStatus(int status) {
    this.heartbeatStatus = status;
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
   * Adds a new heartbeat timeout to List.
   * @param timeout  Time of detected timeout.
   */
  protected void addHeartbeatTimeout(Date timeout) {
    long tolerance = hbFrequency/2;
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
        //System.out.println("Removing timeout: agent=" + getAgentId());
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
    float totalHeartbeats = hbWindow/hbFrequency;
    float rate = lateHeartbeats/totalHeartbeats;
    //System.out.println("HBFailureRate Calc: lateHeartbeats=" + lateHeartbeats +
    //  ", totalHeartbeats=" + totalHeartbeats +
    //  ", rate=" + rate);
    return (rate > 1.0f ? 1.0f : rate);
  }


  protected String failureRateData() {
    pruneTimeoutList();
    float lateHeartbeats = heartbeatTimeouts.size();
    float totalHeartbeats = hbWindow/hbFrequency;
    float rate = lateHeartbeats/totalHeartbeats;
    return "HbFailureRateData: agent=" + getAgentId() +
      ", lateHeartbeats=" + lateHeartbeats +
      ", totalHeartbeats=" + totalHeartbeats +
      ", rate=" + rate;
  }


  /**
   * Sets flat indicating that a ping has been sent to monitored agent.
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
    currentState = state;
    //Attributes attrs = new BasicAttributes("RunState", currentState);
    //commSvc.setEntityAttributes(communityName, agentId.toString(), attrs);
  }

  /**
   * Returns monitored agents current run state.
   * @return
   */
  protected String getState() {
    return currentState;
    /*
    try {
      Attributes attrs =
        commSvc.getEntityAttributes(communityName, agentId.toString());
      return (String)attrs.get("RunState").get();
    } catch (Exception ex) {
      return new String();
    }
    */
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