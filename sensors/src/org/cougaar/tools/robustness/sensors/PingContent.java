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
 * An object to be passed along with a Ping object between PingRequesterPlugin 
 *   to PingServerPlugin containing additional content.
 **/
public final class PingContent implements java.io.Serializable {
  private UID pingReqUID;
  private long timeout;

  /**
   * @param pingReqUID UID of the PingRequest object.  
   *   Used by PingRequesterPlugin to match up Ping replies with PingRequests
   * @param timeout milliseconds to wait before timing out ping
   */
  public PingContent(UID pingReqUID, long timeout) {
    this.pingReqUID = pingReqUID;
    this.timeout = timeout;
  }

  public UID getPingReqUID() { return pingReqUID; }
  public long getTimeout() { return timeout; }

  public String toString() {
    return "(PingContent: " + pingReqUID + ", " + timeout + ")";
  }

}