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

import org.cougaar.core.relay.Relay;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.core.util.UID;

/**
 * HealthReports report some aspect of element health.
 * Extends Relay so that messages can be sent to a role based
 * AttributeBasedAddress.
 **/
public interface HealthReport extends Relay.Source, Relay.Target, UniqueObject {

  public static final int UNDEFINED     = -1;

  // HealthReport categories
  public static final int HEARTBEAT     = 0;
  public static final int AUTHORITATIVE = 1;
  public static final int OTHER         = 2;

  // Health status codes
  public static final int ALIVE         = 0;
  public static final int NO_RESPONSE   = 1;
  public static final int DEAD          = 2;


  /**
   * Identifies the agent that is the topic of this HealthReport.
   *
   * @param agentId Agents MessageAddress
   */
  public void setAgentId(MessageAddress agentId);


  /**
   * Returns the MessageAddress of the agent that is the topic of this
   * HealthReport.
   *
   * @return Agents MessageAddress
   */
  public MessageAddress getAgentId();


  /**
   * Defines the overall category of this HealthReport.
   *
   * @param category HealthReport category code
   */
  public void setCategory(int catetory);


  /**
   * Gets the overall category of this HealthReport.
   *
   * @return HealthReport category code
   */
  public int getCategory();


  /**
   * Defines the HealthReport status code.
   *
   * @param int HealthReport status code
   */
  public void setStatus(int statusCode);


  /**
   * Gets the HealthReport status code.
   *
   * @return  HealthReport status code
   */
  public int getStatus();


  /**
   * Gets the time the HealthReport was created/updated.
   *
   * @return  Date of HealthReport creation/update
   */
  public Date getTimestamp();


  /**
   * Add a target message address.
   * @param target the address of the target agent.
   **/
  public void addTarget(MessageAddress target);

  /**
   * Remove a target message address.
   * @param target the address of the target agent to be removed.
   **/
  public void removeTarget(MessageAddress target);

}
