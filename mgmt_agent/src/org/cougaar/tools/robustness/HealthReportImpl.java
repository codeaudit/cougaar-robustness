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
package org.cougaar.tools.robustness;

import java.util.Date;
import org.cougaar.core.mts.MessageAddress;

/**
 * Implementation of HealthReport interface.
 **/
public class HealthReportImpl extends HealthReportAdapter {

  private MessageAddress agentId = null;
  private int categoryCode = HealthReport.UNDEFINED;
  private int statusCode   = HealthReport.UNDEFINED;

  private String myToString = null;
  private Date timestamp;

  /**
   * HealthReportImpl - no arg Constructor
   **/
  public HealthReportImpl() {
    super();
    updateTimestamp();
  }


  /**
   * Identifies the agent that is the topic of this HealthReport.
   *
   * @param agentId Agents MessageAddress
   */
  public void setAgentId(MessageAddress agentId) {
    this.agentId = agentId;
  }


  /**
   * Returns the MessageAddress of the agent that is the topic of this
   * HealthReport.
   *
   * @return Agents MessageAddress
   */
  public MessageAddress getAgentId() {
    return this.agentId;
  }


  /**
   * Defines the overall category of this HealthReport.
   *
   * @param category HealthReport category code
   */
  public void setCategory(int categoryCode) {
    this.categoryCode = categoryCode;
  }


  /**
   * Gets the overall category of this HealthReport.
   *
   * @return HealthReport category code
   */
  public int getCategory() {
    return this.categoryCode;
  }


  /**
   * Defines the HealthReport status code.
   *
   * @param int HealthReport status code
   */
  public void setStatus(int statusCode) {
    this.statusCode = statusCode;
  }


  /**
   * Gets the HealthReport status code.
   *
   * @return  HealthReport status code
   */
  public int getStatus() {
    return this.statusCode;
  }

  /**
   * Gets the time the HealthReport was created/updated.
   *
   * @return  Date of HealthReport creation/update
   */
  public Date getTimestamp() {
    return this.timestamp;
  }

  public void updateTimestamp() {
    this.timestamp = new Date();
  }

  private String categoryToString(int categoryCode) {
    switch(categoryCode) {
      case HealthReport.UNDEFINED:
        return "UNDEFINED";
      case HealthReport.HEARTBEAT:
        return "HEARTBEAT";
      case HealthReport.AUTHORITATIVE:
        return "AUTHORITATIVE";
      default:
        return "INVALID";
    }
  }

  private String statusToString(int statusCode) {
    switch(statusCode) {
      case HealthReport.UNDEFINED:
        return "UNDEFINED";
      case HealthReport.DEAD:
        return "DEAD";
      case HealthReport.ALIVE:
        return "ALIVE";
      case HealthReport.NO_RESPONSE:
        return "NO_RESPONSE";
      default:
        return "INVALID";
    }
  }

  /**
   * Returns a string representation of the HealthReport
   *
   * @return String - a string representation of the HealthReport.
   **/
  public String toString() {
    if (myToString == null) {
      myToString = "HealthReport: " +
        "agentId=" + getAgentId() +
        ", source=" + getSource().toString() +
        ", category=" + categoryToString(getCategory()) +
        ", status=" + statusToString(getStatus());
      myToString = myToString.intern();
    }

    return myToString;
  }
}