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
import org.cougaar.tools.robustness.ma.controllers.*;
import org.cougaar.tools.robustness.ma.util.HeartbeatListener;
import org.cougaar.tools.robustness.ma.util.MoveHelper;
import org.cougaar.tools.robustness.ma.util.MoveListener;
import org.cougaar.tools.robustness.ma.util.RestartHelper;
import org.cougaar.tools.robustness.ma.util.RestartListener;
import org.cougaar.tools.robustness.ma.util.RestartDestinationLocator;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.service.LoggingService;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import java.util.*;

public class DefaultRobustnessController extends RobustnessControllerBase {

  public static final int INITIAL        = 0;
  public static final int ACTIVE         = 1;
  public static final int HEALTH_CHECK   = 2;
  public static final int DEAD           = 3;
  public static final int RESTART        = 4;
  public static final int FAILED_RESTART = 5;
  public static final int MOVE           = 6;

  public static long DEFAULT_EXPIRATION = 1 * 60 * 1000;

  // Default parameter values, may be overridden by community/entity attributes
  public static final long PING_TIMEOUT              =  10 * 1000;
  public static final long HEARTBEAT_REQUEST_TIMEOUT =  30 * 1000;
  public static final long HEARTBEAT_FREQUENCY       =  30 * 1000;
  public static final long HEARTBEAT_TIMEOUT         =  60 * 1000;
  public static final long HEARTBEAT_PCT_OUT_OF_SPEC =  50;

  /**
   * State Controller: INITIAL
   * <pre>
   *   Entry: Agent started/restarted
   *   Actions performed: Start heartbeats
   *   Next state:  HEALTH_CHECK if status expired
   * </pre>
   */
  class InitialStateController extends    StateControllerBase
                               implements HeartbeatListener {
    { addHeartbeatListener(this); }
    public void enter(String name) {
      logger.debug("INITIAL: agent=" + name + " loc=" + getLocation(name));
      if (isLocal(name)) {
        if (isAgent(name)) {
          startHeartbeats(name);
          newState(name, DefaultRobustnessController.ACTIVE);
        } else { // is node
          model.setCurrentState(name, DefaultRobustnessController.ACTIVE, NEVER);
        }
      }
    }
    public void expired(String name) {
      if (isLocal(name)) newState(name, HEALTH_CHECK);
    }
    public void heartbeatStarted(String name) {
      if (isLocal(name)) {
        logger.info("Heartbeats started: agent=" + name);
        newState(name, DefaultRobustnessController.ACTIVE);
      }
    }
    public void heartbeatStopped(String name) {
      if (isLocal(name)) {
        logger.info("Heartbeats stopped: agent=" + name);
      }
    }
    public void heartbeatFailure(String name) {
      if (getState(name) == DefaultRobustnessController.ACTIVE &&
          isLocal(name)) {
        logger.info("Heartbeat failure: agent=" + name);
        newState(name, HEALTH_CHECK);
      }
    }
  }

  boolean communityReady = false;

  /**
   * State Controller: ACTIVE
   * <pre>
   *   Actions performed: None
   *   Next state:  HEALTH_CHECK on expired status
   * </pre>
   */
  class ActiveStateController extends StateControllerBase {
    public void enter(String name) {

      if (isLeader()) {
        if (communityReady == false && liveAgents() == expectedAgents()) {
          communityReady = true;
          event("Community " + model.getCommunityName() + " Ready");
          RestartDestinationLocator.clearRestarts();
        }
      }

      //logger.info("enter: state=ACTIVE agent=" + name);
      if (isLocal(name)) {
        setExpiration(name, NEVER);  // Set expiration to never, let
                                     // heartbeatListener maintain state status
      }
    }
    public void expired(String name) {
      if (isLocal(name) || isLeader()) {
        logger.info("expired: state=ACTIVE agent=" + name);
      }
      newState(name, HEALTH_CHECK);
    }
  }

  /**
   * State Controller: HEALTH_CHECK
   * <pre>
   *   Actions performed: Ping agent
   *   Next state:  ACTIVE on successful ping
   *                DEAD on failed ping or expired status
   * </pre>
   */
  class HealthCheckStateController extends StateControllerBase {
    public void enter(String name) {
      if(isLocal(name) || isNode(name)) {
        doPing(name, DefaultRobustnessController.ACTIVE, DEAD);
      }
    }
    public void expired(String name) {
      if(isLeader() || isNode(name)) {
        newState(name, DEAD);
      }
    }
  }

  /**
   * State Controller: DEAD
   * <pre>
   *   Actions performed: Notify deconflictor
   *   Next state: RESTART when OK'd by deconflictor
   * </pre>
   */
  class DeadStateController extends StateControllerBase {
    public void enter(String name) {
      if (isLeader() && isAgent(name)){
        communityReady = false;  // For ACME Community Ready Events
          // Interface point for Deconfliction
          // If Ok'd by Deconflictor set state to RESTART
        newState(name, RESTART);
        setLocation(name, "");
      } else if (isNode(name)) {
        setExpiration(name, NEVER);  // Never expires
      } else if (isLocal(name)) {
        setLocation(name, null);
      }
    }
  }

  /**
   * State Controller: RESTART
   * <pre>
   *   Actions performed: Determine restart location
   *                      Restart agent
   *   Next state:  ACTIVE on successful restart
   *                FAILED_RESTART on failed restart or expired status
   * </pre>
   */
  class RestartStateController extends StateControllerBase implements RestartListener {
    { addRestartListener(this); }
    public void enter(String name) {
      if (isLeader() && isAgent(name)) {
        String dest = getRestartLocation();
        restartAgent(name, dest);
        RestartDestinationLocator.restartOneAgent(dest);
      }
    }

    public void expired(String name) {
      if (isLeader()) {
        newState(name, FAILED_RESTART);
      }
    }

    public void restartInitiated(String name, String dest) {
      event("Restarting agent=" + name + " dest=" + dest);
    }

    public void restartComplete(String name, String dest, int status) {
      if (status == RestartHelper.SUCCESS) {
        event("Restart complete: agent=" + name + " location=" + dest);
        //updateLocationAndSetState(name, INITIAL);
        //setLocation(name, dest);
        //newState(name, INITIAL);
      } else {
        event("Restart failed: agent=" + name + " location=" + dest);
        newState(name, FAILED_RESTART);
      }
    }
  }

  /**
   * State Controller: FAILED_RESTART
   * <pre>
   *   Actions performed: None
   *   Next state: HEALTH_CHECK
   * </pre>
   */
  class FailedRestartStateController extends StateControllerBase {

    public void enter(String name) {
      if (isLeader() && isAgent(name)) {
        //newState(name, HEALTH_CHECK);
      }
    }
    public void expired(String name) {
      if (isLeader() && isAgent(name)) {
        //newState(name, HEALTH_CHECK);
      }
    }
  }

  /**
   * State Controller: MOVE
   * <pre>
   *   Actions performed: None
   *   Next state: HEALTH_CHECK
   * </pre>
   */
  class MoveStateController extends StateControllerBase implements MoveListener {
    { addMoveListener(this); }
    public void enter(String name) {
    }
    public void expired(String name) {
      if (isLeader() && isAgent(name)) {
        newState(name, HEALTH_CHECK);
      }
    }

    public void moveInitiated(String name, String orig, String dest) {
      event("Move initiated:" +
            " agent=" + name +
            " orig=" + orig +
            " dest=" + dest);
      stopHeartbeats(name);
      newState(name, MOVE);
    }

    public void moveComplete(String name, String orig, String dest, int status) {
      event("Move complete:" +
            " agent=" + name +
            " orig=" + orig +
            " dest=" + dest +
            " status=" + (status == MoveHelper.SUCCESS ? "SUCCESS" : "FAIL"));
      if (status == MoveHelper.FAIL) {
        newState(name, HEALTH_CHECK);
      }
    }

  }


  /**
   * Constructor initializes services and loads state controller classes.
   */
  public DefaultRobustnessController(final String thisAgent,
                                     final BindingSite bs,
                                     final CommunityStatusModel csm) {
    super(thisAgent, bs, csm);
    addController(INITIAL,        "INITIAL", new InitialStateController());
    addController(DefaultRobustnessController.ACTIVE, "ACTIVE",  new ActiveStateController());
    addController(HEALTH_CHECK,   "HEALTH_CHECK",  new HealthCheckStateController());
    addController(DEAD,           "DEAD",  new DeadStateController());
    addController(RESTART,        "RESTART", new RestartStateController());
    addController(FAILED_RESTART, "FAILED_RESTART", new FailedRestartStateController());
    addController(MOVE,           "MOVE", new MoveStateController());
    RestartDestinationLocator.setCommunityStatusModel(csm);
    RestartDestinationLocator.setLoggingService(logger);
  }

  /**
   * Identifies state considered "Normal" by this controller.
   */
  public int getNormalState() { return DefaultRobustnessController.ACTIVE; }

  /**
   * State to trigger leader re-election.
   */
  public int getLeaderElectionTriggerState() { return DEAD; }

  /**
   * Gets default period for a state expiration.
   */
  public long getDefaultStateExpiration() {
    return DEFAULT_EXPIRATION;
  }

  public long liveAgents() {
    int allAgents = model.listEntries(model.AGENT).length;
    int deadAgents = model.listEntries(model.AGENT, DEAD).length;
    return allAgents - deadAgents;
  }

  public long expectedAgents() {
    return getLongProperty("NumberOfAgents", -1);  }

  /**
   * Selects destination node for agent to be restarted.
   */
  protected String getRestartLocation() {
    /*String candidateNodes[] =
        model.listEntries(model.NODE, DefaultRobustnessController.ACTIVE);
    int numAgents = 0;
    String selectedNode = null;
    for (int i = 0; i < candidateNodes.length; i++) {
      int agentsOnNode = model.agentsOnNode(candidateNodes[i]).length;
      if (selectedNode == null || agentsOnNode < numAgents) {
        selectedNode = candidateNodes[i];
        numAgents = agentsOnNode;
      }
    }*/
    String selectedNode = RestartDestinationLocator.getRestartLocation();
    logger.debug("get restart destination node:: " + selectedNode);
    return selectedNode;
  }

}
