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

import java.util.Set;
import java.util.Collections;
import org.cougaar.core.relay.*;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.XMLizable;
import org.cougaar.core.util.XMLize;
import org.cougaar.core.util.UniqueObject;
import java.util.Date;

/**
 * A Plugin developer requests a Heartbeat by publishing a 
 * HeartbeatRequest on the blackboard of a local agent with 
 * HeartbeatRequesterPlugin loaded.
 **/
public class HeartbeatRequest implements UniqueObject
{
  private UID uid;
  private MessageAddress source;
  private MessageAddress target;
  private long reqTimeout;  // milliseconds
  private long hbFrequency;  // milliseconds
  private long hbTimeout;  // milliseconds
  private boolean onlyOutOfSpec; 
  private float percentOutOfSpec; 
  private int status;
  private Date timeSent;
  private Date timeReceived;
  private long roundTripTime; //milliseconds

  /**
  * A new HeartbeatRequest that has not yet resulted in a request being
  * sent to target agent.
  */
  public static final int NEW = 0;

  /**
  * A HeartbeatRequest that has resulted in a request being sent to 
  * target agent.
  */
  public static final int SENT = 1;

  /**
  * A HeartbeatRequest which has been accepted by target agent.
  */
  public static final int ACCEPTED = 2;

  /**
  * A HeartbeatRequest which has been denied by target agent.
  */
  public static final int REFUSED = 3;

  /**
  * A HeartbeatRequest which had not been accepted/denied before timing
  * out or failing in some other way.
  */
  public static final int FAILED = 4;

  /**
  * Convert the status code to a human-readable String.
  */
  public static String statusToString(int status) {
    switch (status) {
      case NEW:
        return "NEW";
      case SENT:
        return "SENT";
      case ACCEPTED:
        return "ACCEPTED";
      case REFUSED:
        return "REFUSED";
      case FAILED:
        return "FAILED";
      default:
        throw new RuntimeException("illegal status = " + status);
    }
  }

  /**
   * @param uid UID of this request
   * @param source MessageAddress of agent requesting the Heartbeat
   * @param target MessageAddress of agent providing the Heartbeat
   * @param reqTimeout Request timeout in milliseconds
   * @param hbFrequency Heartbeat frequency in milliseconds
   * @param hbTimeout Heartbeat timeout in milliseconds
   * @param onlyOutOfSpec only report if heartbeat is late,
   * as specified by hbFrequency
   * @param percentOutOfSpec only report when heartbeat is 
   * this much later than specified by hbFrequency
   */
  public HeartbeatRequest(UID uid, 
                          MessageAddress source, 
                          MessageAddress target, 
                          long reqTimeout, 
                          long hbFrequency, 
                          long hbTimeout,
                          boolean onlyOutOfSpec,
                          float percentOutOfSpec) {
    this.uid = uid;
    this.source = source;
    this.target = target;
    this.reqTimeout = reqTimeout;
    this.hbFrequency = hbFrequency;
    this.hbTimeout = hbTimeout;
    this.onlyOutOfSpec = onlyOutOfSpec;
    this.percentOutOfSpec = percentOutOfSpec;
    this.status = NEW; 
  }

  // UniqueObject implementation

  /**
  * Get the UID of this request.
  */
  public UID getUID () { return uid; } 

  /**
  * UIDs are not permitted to be set except by constructor, so this 
  * method throws an exception.
  *
  * @throws java.lang.RuntimeException
  **/ 
  public void setUID(UID uid) { 
    throw new RuntimeException("Attempt to change UID"); 
  }

  /**
  * Get the address of the Agent that sent this request.
  */
  public MessageAddress getSource() { return source; }

  /**
  * Set the address of the Agent that sent this request.
  */
  public void setSource(MessageAddress addr) { source = addr; }

  /**
  * Get the address of the Agent to which this request is sent.
  */
  public MessageAddress getTarget() { return target; }
  
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

  /**
  * Returns true when only late heartbeats, as 
  * specified by hbFrequency, should be reported.
  */
  public boolean getOnlyOutOfSpec() { return onlyOutOfSpec; }

  /**
  * Set to true if only late heartbeats, as 
  * specified by hbFrequency, should be reported.
  * Else, all heartbeats will be reported.
  */
  public void setOnlyOutOfSpec(boolean onlyOutOfSpec) {
    this.onlyOutOfSpec = onlyOutOfSpec; 
  }

  /**
  * Get percentage late, as specified by hbFrequency, 
  * heartbeats can be before they are reported.
  */
  public float getPercentOutOfSpec() { return percentOutOfSpec; }

  /**
  * Set percentage late, as specified by hbFrequency, 
  * heartbeats can be before they are reported.
  */
  public void setPercentOutOfSpec(float percentOutOfSpec) {
    this.percentOutOfSpec = percentOutOfSpec; 
  }

  /**
  * Get the status of the request 
  */
  public int getStatus() { return status; }

  /**
  * Set the status of the request 
  * (i.e. NEW, SENT, ACCEPTED, REFUSED, FAILED).
  */
  public void setStatus(int status) { 
    switch (status) {
      case NEW:
      case SENT:
      case ACCEPTED:
      case REFUSED:
      case FAILED:
        this.status = status;
        break;
      default:
        throw new RuntimeException("illegal status = " + status);
    }
  }

  /**
  * Get the time that the request was sent to the target Agent.
  */
  public Date getTimeSent() { return timeSent; }

  /**
  * Set the time that the Heartbeat was sent to the target Agent.
  */
  public void setTimeSent(Date timeSent) { this.timeSent = timeSent; }

  /**
  * Get the time that the response was received from the target Agent.
  */
  public Date getTimeReceived() { return timeReceived; }

  /**
  * Set the time that the response was received from the target Agent.
  */
  public void setTimeReceived(Date timeReceived) { 
    this.timeReceived = timeReceived; 
  }

  /**
  * Get the number of milliseconds between sending the request and 
  * receiving a response.
  */
  public long getRoundTripTime() { return roundTripTime; }

  /**
  * Set the number of milliseconds between sending the request and 
  * receiving a response.
  */
  public void setRoundTripTime(long roundTripTime) { 
    this.roundTripTime = roundTripTime; 
  }

  /**
  * Returns a pretty-printed representation for a HeartbeatRequest.
  */
  public String toString() {
    return "\n" +
           "(HeartbeatRequest:\n" +
           "   uid = " + uid + "\n" +
           "   source = " + source + "\n" +
           "   target = " + target + "\n" +
           "   reqTimeout = " + reqTimeout + "\n" +
           "   hbFrequency = " + hbFrequency + "\n" +
           "   hbTimeout = " + hbTimeout + "\n" +
           "   onlyOutOfSpec = " + onlyOutOfSpec + "\n" +
           "   percentOutOfSpec = " + percentOutOfSpec + "\n" +
           "   status = " + statusToString(status) + "\n" +
           "   timeSent = " + timeSent + "\n" +
           "   timeReceived = " + timeReceived + "\n" +
           "   roundTripTime = " + roundTripTime + ")";
  }

}
