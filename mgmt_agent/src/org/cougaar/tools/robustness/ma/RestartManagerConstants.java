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
package org.cougaar.tools.robustness.ma;

/**
 * Defines constants used by robustness restart manager.
 */
public interface RestartManagerConstants {

  public static final String PERSISTENCE_INTERVAL_ATTRIBUTE  = "PERSISTENCE_INTERVAL";
  public static final String SOLVER_MODE_ATTRIBUTE           = "SOLVER_MODE";
  public static final String AUTO_LOAD_BALANCE_ATTRIBUTE     = "AUTO_LOAD_BALANCE";
  public static final String PING_ADJUSTMENT                 = "PING_ADJUSTMENT";
  public static final String COMMUNITY_BUSY_ATTRIBUTE        = "COMMUNITY_BUSY_THRESHOLD";
  public static final String NODE_BUSY_ATTRIBUTE             = "NODE_BUSY_THRESHOLD";
  public static final String STATUS_UPDATE_ATTRIBUTE         = "STATUS_UPDATE_INTERVAL";
  public static final String CURRENT_STATUS_UPDATE_ATTRIBUTE = "CURRENT_STATUS_UPDATE_INTERVAL";
  public static final String PERSISTENCE_INTERVAL_THREATCON_HIGH_COEFFICIENT = "PERSISTENCE_INTERVAL_THREATCON_HIGH_COEFFICIENT";
  public static final String STATUS_UPDATE_INTERVAL_THREATCON_HIGH_COEFFICIENT = "STATUS_UPDATE_INTERVAL_THREATCON_HIGH_COEFFICIENT";
  public static final String PING_TIMEOUT_THREATCON_HIGH_COEFFICIENT = "PING_TIMEOUT_THREATCON_HIGH_COEFFICIENT";
  // Defines attribute to use in selection of community to monitor
  public static final String HEALTH_MONITOR_ROLE = "HealthMonitor";
  public static final String COMMUNITY_TYPE =      "Robustness";

  // Property used to define the name of a robustness community to monitor
  public static final String COMMUNITY_PROPERTY = "org.cougaar.tools.robustness.community";

  // Defines default class to use for Robustness Controller
  public static final String CONTROLLER_CLASS_PROPERTY =
      "org.cougaar.tools.robustness.controller.classname";

  public static final String DEFAULT_ROBUSTNESS_CONTROLLER_CLASSNAME =
      "org.cougaar.tools.robustness.ma.controllers.DefaultRobustnessController";

  // Property for enabling/disabling deconfliction
  public static final String DECONFLICTION = "org.cougaar.tools.robustness.restart.deconfliction";
  public static final String PROPERTY_ENABLED = "enabled";

  // Defines how often status updates are broadcast to peers
  public static final String STATUS_UPDATE_PROPERTY = "org.cougaar.tools.robustness.update.interval";
  public static final String DEFAULT_STATUS_UPDATE_INTERVAL = "30000";
  public static final int NEVER = -1;

  // Default parameter values, may be overridden by community attributes
  public static long DEFAULT_EXPIRATION = 5 * 60 * 1000;
  public static final long PING_TIMEOUT = 5 * 60 * 1000;
  public static final long MINIMUM_PING_TIMEOUT = 1 * 60 * 1000;
  public static final long HEARTBEAT_REQUEST_TIMEOUT = 2 * 60 * 1000;
  public static final long HEARTBEAT_FREQUENCY = 60 * 1000;
  public static final long HEARTBEAT_TIMEOUT = 2 * 60 * 1000;
  public static final long HEARTBEAT_PCT_OUT_OF_SPEC = 80;

  // Defines constants used to increase ping timeout based on cpu load
  // of destination.  The MAX_CPU_LOAD_FOR_ADJUSTMENT constant defines
  // the upper range of cpu loads used to calculate the scaling
  // coefficient.  The MAX_CPU_LOAD_SCALING defines the maximum
  // value of the coefficient.
  public static final double MAX_CPU_LOAD_FOR_ADJUSTMENT = 8.0;
  public static final double MAX_CPU_LOAD_SCALING = 3.0;

  // Metrics used
  public static final String LOAD_AVERAGE_METRIC = "LoadAverage";



}