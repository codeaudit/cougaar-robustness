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
 * The Response returned by the HeartbeatServerPlugin, containing the status.
 **/
public final class HbReqResponse implements java.io.Serializable {
  private int status;

  /**
   * @param status The status of the HeartbeatRequest 
   * (i.e. HeartbeatRequest.ACCEPTED, HeartbeatRequest.REFUSED)
   */
  public HbReqResponse(int status) {
    switch (status) {
      // only these two values can be set by server
      case HeartbeatRequest.ACCEPTED:
      case HeartbeatRequest.REFUSED:
        this.status = status;
        return;
      default:
        throw new RuntimeException("illegal status = " + status);
    }
  }

  /**
  * Get the status.
  */
  public int getStatus() { return status; }

  /**
  * Set the status.
  * @param status Change status to HeartbeatRequest.ACCEPTED or HeartbeatRequest.REFUSED.
  */
  public void setStatus(int status) { 
    switch (status) {
      // only these two values can be set by server
      case HeartbeatRequest.ACCEPTED:
      case HeartbeatRequest.REFUSED:
        this.status = status;
        return;
      default:
        throw new RuntimeException("illegal status = " + status);
    }
  }

  /**
  * Returns a String represention for a HbReqResponse.
  */
  public String toString() {
    return "(HbReqResponse: status = " + HeartbeatRequest.statusToString(status);
  }

}