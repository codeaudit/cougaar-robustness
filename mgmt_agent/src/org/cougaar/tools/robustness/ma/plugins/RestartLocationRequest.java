/*
 * <copyright>
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 *
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.tools.robustness.ma.plugins;

import java.util.*;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.community.CommunityService;
import org.cougaar.core.blackboard.Publishable;

/**
 * This class is used in the Management Agent to request the hostname of
 * a suitable location to restart one or more named agents.
 */
public class RestartLocationRequest implements java.io.Serializable {

  // Request status codes
  public static final int NEW        = 0;
  public static final int IN_PROCESS = 1;
  public static final int SUCCESS    = 2;
  public static final int FAIL       = 3;

  // Agents to be restarted/relocated
  private Set agents = new HashSet();

  // Current status
  private int status = NEW;

  // Name of destination host
  private String hostName = null;

  /**
   * Adds an agent to set of agents to be relocated/restarted.
   * @param agent
   */
  public void addAgent(MessageAddress agent) {
    agents.add(agent);
  }

  /**
   * Returns set of agents to be relocated/restarted.
   * @return Set of MessageAddresses for agents to be relocated/restarted.
   */
  public Set getAgents() {
    return this.agents;
  }

  /**
   * Set status of request.
   * @param status
   */
  public void setStatus(int status) {
    this.status = status;
  }

  /**
   * Returns current status of this request.
   * @return
   */
  public int getStatus() {
    return this.status;
  }

  /**
   * Sets name of host selected for restarting/relocating agents.
   * @param hostName
   */
  public void setHost(String hostName) {
    this.hostName = hostName;
  }

  /**
   * Gets name of host selected for restarting/relocating agents.
   * @return
   */
  public String getHost() {
    return this.hostName;
  }
}