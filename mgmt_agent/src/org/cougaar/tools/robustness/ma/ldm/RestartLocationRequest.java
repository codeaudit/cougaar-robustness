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
package org.cougaar.tools.robustness.ma.ldm;

import java.util.*;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.community.CommunityService;
import org.cougaar.core.blackboard.Publishable;
import org.cougaar.core.util.UID;

/**
 * This class is used in the Management Agent to request the hostname of
 * a suitable location to restart one or more named agents.
 */
public class RestartLocationRequest implements java.io.Serializable {

  // Request codes
  public static final int LOCATE_NODE = 0;
  public static final int LOCATE_HOST = 1;

  // Request status codes
  public static final int NEW        = 0;
  public static final int IN_PROCESS = 1;
  public static final int SUCCESS    = 2;
  public static final int FAIL       = 3;

  // Request type
  private int requestType = LOCATE_NODE;

  // Owners Unique ID
  UID ownerUID;

  // Agents to be restarted/relocated
  private Set agents = new HashSet();

  // Collection of nodes/hosts that should not be
  // considered for restart/move destionation
  private Collection excludedNodes = new Vector();
  private Collection excludedHosts = new Vector();

  // Current status
  private int status = NEW;

  // Name of destination host
  private String hostName = null;

  // Name of destination node
  private String nodeName = null;

  public RestartLocationRequest(int requestType, UID ownerUID) {
    this.requestType = requestType;
    this.ownerUID = ownerUID;
  }

  /**
   * Returns request type.
   * @return Request type
   */
  public int getRequestType() {
    return this.requestType;
  }

  /**
   * Returns UID associated with this request.
   * @return UID
   */
  public UID getOwnerUID() {
    return this.ownerUID;
  }

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
   * @return Status of request
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
   * @return  Name of selected restart host
   */
  public String getHost() {
    return this.hostName;
  }

  /**
   * Sets name of mode selected for restarting/relocating agents.
   * @param nodeName
   */
  public void setNode(String nodeName) {
    this.nodeName = nodeName;
  }

  /**
   * Gets name of node selected for restarting/relocating agents.
   * @return  Name of selected restart node
   */
  public String getNode() {
    return this.nodeName;
  }

  /**
   * Sets name of hosts that should not be considered as a possible destination.
   * @param hostNames Collection of host names
   */
  public void setExcludedHosts(Collection hostNames) {
    this.excludedHosts = hostNames;
  }

  /**
   * Gets Collection of host names that sould be excluded from consideration
   * as a restart/move destination.
   * @return  Collection of host names
   */
  public Collection getExcludedHosts() {
    return this.excludedHosts;
  }

  /**
   * Sets name of nodes that should not be considered as a possible destination.
   * @param nodeNames Collection of host names
   */
  public void setExcludedNodes(Collection nodeNames) {
    this.excludedNodes = nodeNames;
  }

  /**
   * Gets Collection of node names that sould be excluded from consideration
   * as a restart/move destination.
   * @return  Collection of node names
   */
  public Collection getExcludedNodes() {
    return this.excludedNodes;
  }

}