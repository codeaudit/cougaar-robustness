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

import org.cougaar.core.mts.MessageAddress;
import java.util.Date;

/**
 * An Entry in the list of Heartbeats in the HeartbeatHealthReport.
 **/
public final class HeartbeatEntry implements java.io.Serializable {
  private MessageAddress source;
  private Date timeReceived;
  private float percentLate;

  /**
   * @param source MessageAddress of the agent which sent the Heartbeat.
   * @param timeReceived Time the heartbeat was received
   * @param percentLate Percentage late the next heartbeat is.
   */
  public HeartbeatEntry(MessageAddress source, 
                        Date timeReceived, 
                        float percentLate) {
    this.source = source;
    this.timeReceived = timeReceived;
    this.percentLate = percentLate;
  }

  /**
  * Get the address of the agent that sent the heartbeat.
  */
  public MessageAddress getSource() { return source; }

  /**
  * Get the time the heartbeat was received
  */
  public Date getTimeReceived() { return timeReceived; }

  /**
  * Get the time the heartbeat was received
  */
  public float getPercentLate() { return percentLate; }

  /**
  * Returns a String represention for this object.
  */
  public String toString() {
    return "(HeartbeatEntry: source = " + source + 
              ", timeReceived = " + timeReceived + 
              ", percentLate = " + percentLate + ")";
  }

}