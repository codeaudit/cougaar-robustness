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

  public static final int DEFAULT_TIMEOUT                      = 5;
  public static final int DEFAULT_RESTART_AGGRESSIVENESS       = 3;

  public static final int DEFAULT_HEARTBEAT_FREQUENCY          = 1;
  public static final int DEFAULT_HEARTBEAT_PCT_OUT_OF_SPEC    = 80;
  public static final long MINIMUM_AUTOTUNING_SAMPLES          = 10;

  public static final int DEFAULT_STATUS_UPDATE_INTERVAL       = 1;

  public static final String DEFAULT_LOAD_BALANCER             = "Internal";
  public static final String DEFAULT_LOAD_BALANCER_MODE        = "6";
  public static final String DEFAULT_AUTO_LOAD_BALANCE_ENABLED = "False";

  public static final String RESTART_AGGRESSIVENESS_ATTRIBUTE  = "RestartAggressiveness";
  public static final String DEFAULT_TIMEOUT_ATTRIBUTE         = "DefaultTimeout";
  public static final String STATUS_UPDATE_INTERVAL_ATTRIBUTE  = "StatusUpdateInterval";

  public static final String LOAD_BALANCER_ATTRIBUTE           = "LoadBalancer";
  public static final String LOAD_BALANCER_MODE_ATTRIBUTE      = "LoadBalancerMode";
  public static final String AUTO_LOAD_BALANCE_ATTRIBUTE       = "AutoLoadBalance";
  public static final String MINIMUM_ANNEAL_TIME_ATTRIBUTE     = "MinimumAnnealTime";
  // Minimum value for EN4J annealTime parameter
  public static final int MINIMUM_ANNEAL_TIME                  = 10;

  public static final String USE_FOR_RESTARTS_ATTRIBUTE        = "UseForRestarts";
  public static final String EXPECTED_AGENTS_ATTRIBUTE         = "NumberOfAgents";

  public static final String PERSISTENCE_INTERVAL_ATTRIBUTE    = "PersistenceInterval";

  // Defines attribute values to use in selection of community to monitor
  public static final String ROLE_ATTRIBUTE                    = "Role";
  public static final String ENTITY_TYPE_ATTRIBUTE             = "EntityType";
  public static final String HEALTH_MONITOR_ROLE               = "HealthMonitor";
  public static final String ROBUSTNESS_MANAGER_ATTRIBUTE      = "RobustnessManager";
  public static final String COMMUNITY_TYPE_ATTRIBUTE          = "CommunityType";
  public static final String ROBUSTNESS_COMMUNITY_TYPE         = "Robustness";

  public static final String ESSENTIAL_RESTART_SERVICE_ATTRIBUTE = "EssentialRestartService";

  public static final String HEARTBEAT_FREQUENCY_ATTRIBUTE       = "HeartbeatFrequency";
  public static final String HEARTBEAT_PCT_OUT_OF_SPEC_ATTRIBUTE = "HeartbeatOfOfSpecPct";

  // Property used to define the name of a robustness community to monitor
  public static final String COMMUNITY_PROPERTY = "org.cougaar.tools.robustness.community";

  // Defines class to use for Robustness Controller
  public static final String CONTROLLER_CLASS_PROPERTY = "org.cougaar.tools.robustness.controller.classname";
  public static final String DEFAULT_CONTROLLER_CLASSNAME = "org.cougaar.tools.robustness.ma.controllers.DefaultRobustnessController";

  // Defines class to use for Defense Coordinator
  public static final String COORDINATOR_CLASS_PROPERTY = "org.cougaar.tools.robustness.coordinator.classname";
  public static final String DEFAULT_COORDINATOR_CLASSNAME = "org.cougaar.tools.robustness.ma.util.CoordinatorHelperImpl";
  // Old coordinator impl = "org.cougaar.tools.robustness.ma.util.DeconflictionHelper";

  // System properties
  public static final String ENABLE_DECONFLICTION_PROPERTY = "org.cougaar.tools.robustness.restart.deconfliction";
  public static final String ENABLE_AUTOTUNING_PROPERTY = "org.cougaar.tools.robustness.autotuning";
  public static final String LEASH_DEFENSES_ON_RESTART_PROPERTY = "org.cougaar.tools.robustness.deconfliction.leashOnRestart";
  public static final String MIN_HOSTS_FOR_MGR_RESTART_PROPERTY = "org.cougaar.tools.robustness.minHostsForMgrRestart";

  // Some propety values
  public static final String ENABLED = "enabled";
  public static final String DISABLED = "disabled";

  public static final int NEVER = -1;
  public static final int MS_PER_MIN = 60000;

}
