/* 
 * <copyright>
 * Copyright 2002 BBNT Solutions, LLC
 * Copyright 2002 Object Services and Consulting, Inc.
 * under sponsorship of the Defense Advanced Research Projects Agency (DARPA).

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).

 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.tools.robustness.sensors;

import org.cougaar.core.util.UID;

/**
 * An object to be passed along with a HbReq object between 
 * HeartbeatRequesterPlugin to HeartbeatServerPlugin containing
 * additional content.
 **/
public final class HbReqContent implements java.io.Serializable {
  private UID heartbeatRequestUID;
  private long reqTimeout;
  private long hbFrequency;
  private long hbTimeout;

  /**
   * @param heartbeatRequestUID UID of the HeartbeatRequest object. Used
   * by HeartbeatRequesterPlugin to match up HbReq replies with HeartbeatRequests.
   * @param reqTimeout HbReq timeout in milliseconds
   * @param hbFrequency Heartbeat frequency in milliseconds
   * @param hbTimeout Heartbeat timeout in milliseconds
   */
  public HbReqContent(UID heartbeatRequestUID, 
                      long reqTimeout, 
                      long hbFrequency, 
                      long hbTimeout) {
    this.heartbeatRequestUID = heartbeatRequestUID;
    this.reqTimeout = reqTimeout;
    this.hbFrequency = hbFrequency;
    this.hbTimeout = hbTimeout;
  }

  /**
  * Get the UID of the HeartbeatRequest object that spawned this request.
  */
  public UID getHeartbeatRequestUID() { return heartbeatRequestUID; }

  /**
  * Get the UID of the HeartbeatRequest object that spawned this request.
  */
  public void setHeartbeatRequestUID(UID heartbeatRequestUID) { 
    this.heartbeatRequestUID = heartbeatRequestUID; 
  }

  /**
  * Get the number of milliseconds that source agent should wait
  *   before giving up sending a request for a heartbeat or target
  *   agent should wait before giving up sending his response.
  */
  public long getReqTimeout() { return reqTimeout; }

  /**
  * Set the number of milliseconds that source agent should wait
  *   before giving up sending a request for a heartbeat or target
  *   agent should wait before giving up sending his response.
  */
  public void setReqTimeout(long reqTimeout) { 
    this.reqTimeout = reqTimeout; 
  }

  /**
  * Get the number of milliseconds between heartbeats to be sent by 
  * target agent.
  */
  public long getHbFrequency() { return hbFrequency; }

  /**
  * Set the number of milliseconds between heartbeats to be sent by 
  * target agent.
  */
  public void setHbFrequency(long hbFrequency) { 
    this.hbFrequency = hbFrequency; 
  }

  /**
  * Get the number of milliseconds that target agent should wait before 
  * giving up sending a Heartbeat.
  */
  public long getHbTimeout() { return hbTimeout; }

  /**
  * Set the number of milliseconds that target agent should wait before 
  * giving up sending a Heartbeat.
  */
  public void setHbTimeout(long hbTimeout) { 
    this.hbTimeout = hbTimeout; 
  }

  public String toString() {
    return "(HbReqContent:\n" +
           "    heartbeatRequestUID = " + heartbeatRequestUID + "\n" +
           "    reqTimeout = " + reqTimeout + "\n" +
           "    hbFrequency = " + hbFrequency + "\n" +
           "    hbTimeout = " + hbTimeout + "\n" + ")";
  }

}