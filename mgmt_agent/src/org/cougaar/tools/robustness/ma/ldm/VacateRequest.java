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

/**
 * This class is used in the Management Agent to request that all agents/nodes
 * on a specified node/host be moved to a different node/host.
 */
public class VacateRequest implements java.io.Serializable {

  // Request codes
  public static final int VACATE_NODE = 0;
  public static final int VACATE_HOST = 1;

  // Request status codes
  public static final int NEW        = 0;
  public static final int IN_PROCESS = 1;
  public static final int SUCCESS    = 2;
  public static final int FAIL       = 3;

  // Request type
  private int requestType = VACATE_HOST;

  // Current status
  private int status = NEW;

  // Name of host to be vacated
  private String hostName = null;

  // Name of node to be vacated
  private String nodeName = null;

  public VacateRequest(int requestType) {
    this.requestType = requestType;
  }

  /**
   * Returns request type.
   * @return Request type
   */
  public int getRequestType() {
    return this.requestType;
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
   * Sets name of host to be vacated.
   * @param hostName
   */
  public void setHost(String hostName) {
    this.hostName = hostName;
  }

  /**
   * Gets name of host to be vacated.
   * @return  Name of host
   */
  public String getHost() {
    return this.hostName;
  }

  /**
   * Sets name of node to be vacated.
   * @param nodeName
   */
  public void setNode(String nodeName) {
    this.nodeName = nodeName;
  }

  /**
   * Gets name of node to be vacated.
   * @return  Name of node
   */
  public String getNode() {
    return this.nodeName;
  }

}