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

  /////////////////////////////////////////////////////////////////////////////
  // Community attributes used
  /////////////////////////////////////////////////////////////////////////////
  public static final String SOLVER_MODE_ATTRIBUTE            = "SOLVER_MODE";
  public static final String AUTO_LOAD_BALANCE_ATTRIBUTE      = "AUTO_LOAD_BALANCE";

  public static final String PING_TIMEOUT_ATTRIBUTE           = "PING_TIMEOUT";
  public static final String MINIMUM_PING_TIMEOUT_ATTRIBUTE   = "MINIMUM_PING_TIMEOUT";

  public static final String HEARTBEAT_REQUEST_TIMEOUT_ATTRIBUTE = "HEARTBEAT_REQUEST_TIMEOUT";
  public static final String HEARTBEAT_FREQUENCY_ATTRIBUTE       = "HEARTBEAT_FREQUENCY";
  public static final String HEARTBEAT_TIMEOUT_ATTRIBUTE         = "HEARTBEAT_TIMEOUT";
  public static final String HEARTBEAT_PCT_OUT_OF_SPEC_ATTRIBUTE = "HEARTBEAT_PCT_OUT_OF_SPEC";

  public static final String STATUS_UPDATE_INTERVAL_ATTRIBUTE        = "STATUS_UPDATE_INTERVAL";
  public static final String DEFAULT_STATUS_LATENCY_MEAN_ATTRIBUTE   = "DEFAULT_STATUS_LATENCY_MEAN";
  public static final String DEFAULT_STATUS_LATENCY_STDDEV_ATTRIBUTE = "DEFAULT_STATUS_LATENCY_STDDEV";
  public static final String RESTART_CONFIDENCE_ATTRIBUTE            = "RESTART_CONFIDENCE";

  public static final String CURRENT_STATUS_UPDATE_ATTRIBUTE = "CURRENT_STATUS_UPDATE_INTERVAL";
  public static final String PERSISTENCE_INTERVAL_THREATCON_HIGH_COEFFICIENT = "PERSISTENCE_INTERVAL_THREATCON_HIGH_COEFFICIENT";
  public static final String STATUS_UPDATE_INTERVAL_THREATCON_HIGH_COEFFICIENT = "STATUS_UPDATE_INTERVAL_THREATCON_HIGH_COEFFICIENT";
  public static final String PING_TIMEOUT_THREATCON_HIGH_COEFFICIENT = "PING_TIMEOUT_THREATCON_HIGH_COEFFICIENT";

  public static final String USE_FOR_RESTARTS_ATTRIBUTE      = "USE_FOR_RESTARTS";
  public static final String EXPECTED_AGENTS_ATTRIBUTE       = "NumberOfAgents";
  public static final String PERSISTENCE_INTERVAL_ATTRIBUTE  = "PERSISTENCE_INTERVAL";
  public static final String PING_ADJUSTMENT_ATTRIBUTE       = "PING_ADJUSTMENT";

  // Defines attribute to use in selection of community to monitor
  public static final String HEALTH_MONITOR_ROLE = "HealthMonitor";
  public static final String COMMUNITY_TYPE =      "Robustness";

  // Property used to define the name of a robustness community to monitor
  public static final String COMMUNITY_PROPERTY = "org.cougaar.tools.robustness.community";

  // Defines class to use for Robustness Controller
  public static final String CONTROLLER_CLASS_PROPERTY = "org.cougaar.tools.robustness.controller.classname";
  public static final String DEFAULT_ROBUSTNESS_CONTROLLER_CLASSNAME =
      "org.cougaar.tools.robustness.ma.controllers.DefaultRobustnessController";

  // Property for enabling/disabling deconfliction
  public static final String DECONFLICTION = "org.cougaar.tools.robustness.restart.deconfliction";
  public static final String PROPERTY_ENABLED = "enabled";

  // Defines how often status updates are broadcast to peers
  public static final String STATUS_UPDATE_PROPERTY = "org.cougaar.tools.robustness.update.interval";

  public static final int NEVER = -1;

  // Default parameter values, may be overridden by applicable community attributes
  public static final long DEFAULT_EXPIRATION =   5 * 60 * 1000;
  public static final long DEFAULT_PING_TIMEOUT = 5 * 60 * 1000;
  public static final long DEFAULT_MINIMUM_PING_TIMEOUT = 1 * 60 * 1000;
  public static final long DEFAULT_HEARTBEAT_REQUEST_TIMEOUT = 1 * 60 * 1000;
  public static final long DEFAULT_HEARTBEAT_FREQUENCY = 60 * 1000;
  public static final long DEFAULT_HEARTBEAT_TIMEOUT = 2 * 60 * 1000;
  public static final long DEFAULT_HEARTBEAT_PCT_OUT_OF_SPEC = 80;

  public static final String COLLECT_NODE_STATS_PROPERTY = "org.cougaar.tools.robustness.collect.stats";
  public static final long MIN_SAMPLES = 10;
  public static final long MIN_SAMPLE_VALUE = 10000;

  /*
    Restart time = DEFAULT_STATUS_UPDATE_INTERVAL +
                   DEFAULT_STATUS_LATENCY_MEAN +
                   (DEFAULT_STATUS_LATENCY_STDDEV * DEFAULT_RESTART_CONFIDENCE)
  */
  public static final long DEFAULT_STATUS_UPDATE_INTERVAL = 30000;
  public static final long DEFAULT_STATUS_LATENCY_MEAN =   150000;
  public static final long DEFAULT_STATUS_LATENCY_STDDEV =  25000;
  public static final long DEFAULT_RESTART_CONFIDENCE = 4;

  // Minimum value for EN4J annealTime parameter
  public static final int MINIMUM_ANNEAL_TIME = 10;

}
