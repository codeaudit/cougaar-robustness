/*
 * <copyright>
 *  Copyright 2001-2003 Mobile Intelligence Corp
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 *
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

import org.cougaar.core.relay.Relay;
import org.cougaar.core.util.UniqueObject;

/**
 * Requests action to be performed by restart manager.
 **/
public interface HealthMonitorRequest
  extends Relay.Target, UniqueObject {

  public static final int UNDEFINED                = -1;

  // Actions that can be performed by restart manager
  public static final int RESTART                  = 0;
  public static final int FORCED_RESTART           = 2;
  public static final int KILL                     = 3;
  public static final int MOVE                     = 4;
  public static final int GET_STATUS               = 5;
  public static final int LOAD_BALANCE             = 6;
  public static final int ADD                      = 7;

  /**
   * Get name of Robustness Community.
   * @return String Name of Robustness Community
   */
  public String getCommunityName();

  /**
   * Get name of agents that will be target of requested action.
   * @return String[]  Name of affected agents
   */
  public String[] getAgents();

  /**
   * Get name of node that will be used as a destination for MOVE, RESTART,
   * or FORCED_RESTART.
   * @return String  Name of destination node
   */
  public String getDestinationNode();

  /**
   * Get name of node that currently contains affected agents.
   * @return String  Name of original node
   */
  public String getOriginNode();

  /**
   * Get RequestType.
   * @return int  Request Type (RESTART, MOVE, etc).
   */
  public int getRequestType();

  /**
   * Get RequestType as a text String.
   * @return String  Request Type ("RESTART", "MOVE", etc).
   */
  public String getRequestTypeAsString();

  /**
   * Set response for request.
   * @param resp HealthMonitorResponse  Response
   */
  public void setResponse(HealthMonitorResponse resp);

}
