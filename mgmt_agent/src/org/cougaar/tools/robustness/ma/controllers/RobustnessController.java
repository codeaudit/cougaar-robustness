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

import org.cougaar.tools.robustness.ma.util.MoveHelper;
import org.cougaar.tools.robustness.ma.util.HeartbeatHelper;
import org.cougaar.tools.robustness.ma.util.PingHelper;
import org.cougaar.tools.robustness.ma.util.RestartHelper;
import org.cougaar.tools.robustness.ma.util.LoadBalancer;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.mts.MessageAddress;

/**
 * Interface defining operations supported by a controller that is
 * responsible for monitoring the status of a Robustness Community and
 * restarting agents that are determined to be dead.
 */
public interface RobustnessController extends StatusChangeListener {

  public BindingSite getBindingSite();

  /**
   * Initialize controller.
   * @param agentId    Host agent
   * @param bs         Host agents BindingSite
   * @param csm        Status model to use
   */
  public void initialize(MessageAddress       agentId,
                         BindingSite          bs,
                         CommunityStatusModel csm);

  /**
   * Add a state controller.
   * @param state      State code
   * @param stateName  State name
   * @param sc         State controller
   */
  public void addController(int             state,
                            String          stateName,
                            StateController sc);

  /**
   * Get reference to controllers move helper.
   */
  public MoveHelper getMoveHelper();

  /**
   * Get reference to controllers restart helper.
   */
  public RestartHelper getRestartHelper();

  /**
   * Get reference to controllers ping helper.
   */
  public PingHelper getPingHelper();

  /**
   * Get reference to controllers load balancer.
   */
  public LoadBalancer getLoadBalancer();

  /**
   * Get reference to controllers heartbeat helper.
   */
  public HeartbeatHelper getHeartbeatHelper();

  /**
   * Receives notification of leader changes.
   */
  public void leaderChange(String priorLeader, String newLeader);

  /**
   * Receives notification of change in agent location.
   */
  public void locationChange(String name, String priorLocation, String newLocation);

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
   * Returns name of specified state.
   */
  public String stateName(int state);

}
