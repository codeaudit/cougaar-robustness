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
import org.cougaar.core.mts.MessageAddress;
import java.util.Date;

/**
 * The Response returned by the HeartbeatServerPlugin, containing the status.
 **/
public final class HbReqResponse implements java.io.Serializable {
  private int status;
  MessageAddress responder;
  private long firstHbSent; //only set and used on the target side
  private long lastHbSent;  //only set and used on the target side

  /**
  * This status is for heartbeats, and isn't propagated to the HeartbeatRequest.
  */
  public static final int HEARTBEAT = -1;

  /**
   * @param responder the MessageAddress of the responding agent.
   * @param status The status of the HeartbeatRequest 
   * (i.e. HeartbeatRequest.ACCEPTED, HeartbeatRequest.REFUSED)
   * @param firstHbSent the time when the first heartbeat was sent
   * @param lastHbSent the time when the last heartbeat was sent
   */
  public HbReqResponse(MessageAddress responder, int status, long firstHbSent, long lastHbSent) {
    this.responder = responder;
    this.firstHbSent = firstHbSent;
    this.lastHbSent = lastHbSent;
    switch (status) {
      // only these two values can be set by server
      case HeartbeatRequest.ACCEPTED:
      case HeartbeatRequest.REFUSED:
      case HEARTBEAT:
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
      case HEARTBEAT:
        this.status = status;
        return;
      default:
        throw new RuntimeException("illegal status = " + status);
    }
  }

  /**
  * Get the MessageAddress of the responding agent.
  */
  public MessageAddress getResponder() { return responder; }

  /**
  * Set the MessageAddress of the responding agent.
  */
  public void setResponder(MessageAddress responder) { 
    this.responder = responder; 
  }

  /**
  * Get the time when the first heartbeat was sent.
  */
  public long getFirstHbSent() { return firstHbSent; }

  /**
  * Set the first send time for a heartbeat.
  */
  public void setFirstHbSent(long firstHbSent) { 
    this.firstHbSent = firstHbSent;        
  }

  /**
  * Get the time when the last heartbeat was sent.
  */
  public long getLastHbSent() { return lastHbSent; }

  /**
  * Set the last send time for a heartbeat.
  */
  public void setLastHbSent(long lastHbSent) { 
    this.lastHbSent = lastHbSent;        
  }

  /**
  * Returns true if this object equals the argument.
  */
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof HbReqResponse)) {
      return false;
    } else {
      HbReqResponse r = (HbReqResponse)o;
      if (this.status == r.getStatus() &&
          this.responder.equals(r.getResponder()) &&
          this.firstHbSent == r.getFirstHbSent() &&
          this.lastHbSent == r.getLastHbSent()) 
        return true;
    }
    return false;
  }

  /**
  * Convert the status code to a human-readable String.
  */
  public static String statusToString(int status) {
    if (status == HEARTBEAT) {
      return "HEARTBEAT";
    } else {
      return HeartbeatRequest.statusToString(status);
    }
  }

  /**
  * Returns a String represention for a HbReqResponse.
  */
  public String toString() {
    return "\n" +
           "    (HbReqResponse:\n" +
           "       responder = " + responder + "\n" +
           "       status = " + statusToString(status) + "\n" +
           "       firstsHbSent = " + new Date(firstHbSent) + "\n" +
           "       lastHbSent = " + new Date(lastHbSent) + ")";
  } 

}