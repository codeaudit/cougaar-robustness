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
package org.cougaar.tools.robustness.ma.controllers;

import org.cougaar.tools.robustness.ma.CommunityStatusModel;
import org.cougaar.tools.robustness.ma.StatusChangeListener;
import org.cougaar.tools.robustness.ma.CommunityStatusChangeEvent;

import org.cougaar.tools.robustness.ma.util.MoveHelper;
import org.cougaar.tools.robustness.ma.util.HeartbeatHelper;
import org.cougaar.tools.robustness.ma.util.PingHelper;
import org.cougaar.tools.robustness.ma.util.RestartHelper;
import org.cougaar.tools.robustness.ma.util.LoadBalancer;

import org.cougaar.core.blackboard.BlackboardClientComponent;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ServiceBroker;

import org.cougaar.core.service.LoggingService;

import java.util.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;


public interface RobustnessController extends StatusChangeListener {

  public void addController(int             state,
                            String          stateName,
                            StateController sc);


  /**
   * Get reference to controllers move helper.
   */
  public MoveHelper getMover();

  /**
   * Get reference to controllers restart helper.
   */
  public RestartHelper getRestarter();

  /**
   * Get reference to controllers ping helper.
   */
  public PingHelper getPinger();

  /**
   * Get reference to controllers load balancer interface.
   */
  public LoadBalancer getLoadBalancer();

  /**
   * Get reference to controllers heartbeat helper.
   */
  public HeartbeatHelper getHeartbeater();

  /**
   * Returns a String containing top-level health status of monitored community.
   */
  public String statusSummary();

  /**
   * Returns XML formatted dump of all community status information.
   * @return XML formatted status
   */
  public String getCompleteStatus();

  /**
   * State considered "Normal" by this controller
   */
  public int getNormalState();

  /**
   * State to trigger leader re-election.
   */
  public int getLeaderElectionTriggerState();

  /**
   * Gets default period for a state expiration.
   */
  public long getDefaultStateExpiration();

  /**
   * Returns name of specified state.
   */
  public String stateName(int state);

}
