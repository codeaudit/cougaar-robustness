/*
 * <copyright>
 * Copyright 2002 Mobile Intelligence Corporation
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
package org.cougaar.tools.robustness;

import org.cougaar.core.mts.MessageAddress;

/**
 * HeartbeatRequest implementation:
 **/
public class HeartbeatRequestImpl extends HeartbeatRequestAdapter {

  private String myText = null;
  private String myToString = null;

  private long interval;

  /**
   * HeartbeatRequestImpl - Constructor
   *
   * @param reportText String specifying the text associated with the
   * HealthReport
   **/
  public HeartbeatRequestImpl(long interval) {
    super();
    this.interval = interval;
  }

  public long getInterval() {
    return this.interval;
  }

  public void setInterval(long interval) {
    this.interval = interval;
  }

  /**
   * toString -  Returns a string representation of the HeartbeatRequest
   *
   * @return String - a string representation of the HeartbeatRequest.
   **/
  public String toString() {
    if (myToString == null) {
      myToString = "HeartbeatRequest: " + getInterval();
      myToString = myToString.intern();
    }

    return myToString;
  }
}