/* 
 * <copyright>
 * Copyright 2002 BBNT Solutions, LLC
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
import org.cougaar.tools.manager.ldm.report.HealthReportAdapter;

/**
 * HealthReport with an associated list of 
 * monitored agents, time last msg was received, and whether that
 * was in-spec or not, according to current HeartbeatRequests.
 **/
public class HeartbeatHealthReport implements java.io.Serializable {
  private HeartbeatEntry [] heartbeats;

  /**
   * Default constructor.
   **/
  public HeartbeatHealthReport() {
  }

  /**
   * @param heartbeats Array of heartbeats
   **/
  public HeartbeatHealthReport(HeartbeatEntry [] heartbeats) {
    super();
    setHeartbeats(heartbeats);
  }

  /**
   * Get array of requested heartbeats.
   **/
  public HeartbeatEntry [] getHeartbeats() {
    return heartbeats;
  }

  /**
   * Set array of requested heartbeats.
   **/
  public void setHeartbeats(HeartbeatEntry [] heartbeats) {
    this.heartbeats = heartbeats;
  }

  /**
   * Returns a String representation of the HeartbeatHealthReport
   *
   * @return String - a string representation of the HeartbeatHealthReport.
   **/
  public String toString() {
      return "HeartbeatHealthReport: " + heartbeats;
  }

}








