-- MySQL dump 8.21
--
-- Host: localhost    Database: tempcopy
---------------------------------------------------------
-- Server version	3.23.49-nt

--
-- Dumping data for table 'alib_component'
--


LOCK TABLES alib_component WRITE;
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave1-RobustnessManager','1AD-Enclave1-RobustnessManager','1AD-Enclave1-RobustnessManager','agent',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('RobustnessManagementAgents-cpy','RobustnessManagementAgents-cpy','recipe|##RECIPE_CLASS##','recipe',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave1-RobustnessManager|org.cougaar.community.CommunityPlugin','1AD-Enclave1-RobustnessManager|org.cougaar.community.CommunityPlugin','plugin|org.cougaar.community.CommunityPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave1-RobustnessManager|org.cougaar.core.mobility.service.RedirectMovePlugin','1AD-Enclave1-RobustnessManager|org.cougaar.core.mobility.service.RedirectMovePlugin','plugin|org.cougaar.core.mobility.service.RedirectMovePlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave1-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.DecisionPlugin','1AD-Enclave1-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.DecisionPlugin','plugin|org.cougaar.tools.robustness.ma.plugins.DecisionPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave1-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.HealthMonitorPlugin','1AD-Enclave1-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.HealthMonitorPlugin','plugin|org.cougaar.tools.robustness.ma.plugins.HealthMonitorPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave1-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.RestartLocatorPlugin','1AD-Enclave1-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.RestartLocatorPlugin','plugin|org.cougaar.tools.robustness.ma.plugins.RestartLocatorPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave1-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.VacatePlugin','1AD-Enclave1-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.VacatePlugin','plugin|org.cougaar.tools.robustness.ma.plugins.VacatePlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave1-RobustnessManager|org.cougaar.tools.robustness.ma.ui.ParameterModifierServlet','1AD-Enclave1-RobustnessManager|org.cougaar.tools.robustness.ma.ui.ParameterModifierServlet','plugin|org.cougaar.tools.robustness.ma.ui.ParameterModifierServlet','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave1-RobustnessManager|org.cougaar.tools.robustness.ma.ui.RobustnessServlet','1AD-Enclave1-RobustnessManager|org.cougaar.tools.robustness.ma.ui.RobustnessServlet','plugin|org.cougaar.tools.robustness.ma.ui.RobustnessServlet','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave1-RobustnessManager|org.cougaar.tools.robustness.sensors.HeartbeatRequesterPlugin','1AD-Enclave1-RobustnessManager|org.cougaar.tools.robustness.sensors.HeartbeatRequesterPlugin','plugin|org.cougaar.tools.robustness.sensors.HeartbeatRequesterPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave1-RobustnessManager|org.cougaar.tools.robustness.sensors.HeartbeatServerPlugin','1AD-Enclave1-RobustnessManager|org.cougaar.tools.robustness.sensors.HeartbeatServerPlugin','plugin|org.cougaar.tools.robustness.sensors.HeartbeatServerPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave1-RobustnessManager|org.cougaar.tools.robustness.sensors.PingRequesterPlugin','1AD-Enclave1-RobustnessManager|org.cougaar.tools.robustness.sensors.PingRequesterPlugin','plugin|org.cougaar.tools.robustness.sensors.PingRequesterPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave1-RobustnessManager|org.cougaar.tools.robustness.sensors.PingServerPlugin','1AD-Enclave1-RobustnessManager|org.cougaar.tools.robustness.sensors.PingServerPlugin','plugin|org.cougaar.tools.robustness.sensors.PingServerPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave2-RobustnessManager','1AD-Enclave2-RobustnessManager','1AD-Enclave2-RobustnessManager','agent',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave2-RobustnessManager|org.cougaar.community.CommunityPlugin','1AD-Enclave2-RobustnessManager|org.cougaar.community.CommunityPlugin','plugin|org.cougaar.community.CommunityPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave2-RobustnessManager|org.cougaar.core.mobility.service.RedirectMovePlugin','1AD-Enclave2-RobustnessManager|org.cougaar.core.mobility.service.RedirectMovePlugin','plugin|org.cougaar.core.mobility.service.RedirectMovePlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave2-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.DecisionPlugin','1AD-Enclave2-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.DecisionPlugin','plugin|org.cougaar.tools.robustness.ma.plugins.DecisionPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave2-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.HealthMonitorPlugin','1AD-Enclave2-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.HealthMonitorPlugin','plugin|org.cougaar.tools.robustness.ma.plugins.HealthMonitorPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave2-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.RestartLocatorPlugin','1AD-Enclave2-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.RestartLocatorPlugin','plugin|org.cougaar.tools.robustness.ma.plugins.RestartLocatorPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave2-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.VacatePlugin','1AD-Enclave2-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.VacatePlugin','plugin|org.cougaar.tools.robustness.ma.plugins.VacatePlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave2-RobustnessManager|org.cougaar.tools.robustness.ma.ui.ParameterModifierServlet','1AD-Enclave2-RobustnessManager|org.cougaar.tools.robustness.ma.ui.ParameterModifierServlet','plugin|org.cougaar.tools.robustness.ma.ui.ParameterModifierServlet','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave2-RobustnessManager|org.cougaar.tools.robustness.ma.ui.RobustnessServlet','1AD-Enclave2-RobustnessManager|org.cougaar.tools.robustness.ma.ui.RobustnessServlet','plugin|org.cougaar.tools.robustness.ma.ui.RobustnessServlet','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave2-RobustnessManager|org.cougaar.tools.robustness.sensors.HeartbeatRequesterPlugin','1AD-Enclave2-RobustnessManager|org.cougaar.tools.robustness.sensors.HeartbeatRequesterPlugin','plugin|org.cougaar.tools.robustness.sensors.HeartbeatRequesterPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave2-RobustnessManager|org.cougaar.tools.robustness.sensors.HeartbeatServerPlugin','1AD-Enclave2-RobustnessManager|org.cougaar.tools.robustness.sensors.HeartbeatServerPlugin','plugin|org.cougaar.tools.robustness.sensors.HeartbeatServerPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave2-RobustnessManager|org.cougaar.tools.robustness.sensors.PingRequesterPlugin','1AD-Enclave2-RobustnessManager|org.cougaar.tools.robustness.sensors.PingRequesterPlugin','plugin|org.cougaar.tools.robustness.sensors.PingRequesterPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave2-RobustnessManager|org.cougaar.tools.robustness.sensors.PingServerPlugin','1AD-Enclave2-RobustnessManager|org.cougaar.tools.robustness.sensors.PingServerPlugin','plugin|org.cougaar.tools.robustness.sensors.PingServerPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave3-RobustnessManager','1AD-Enclave3-RobustnessManager','1AD-Enclave3-RobustnessManager','agent',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave3-RobustnessManager|org.cougaar.community.CommunityPlugin','1AD-Enclave3-RobustnessManager|org.cougaar.community.CommunityPlugin','plugin|org.cougaar.community.CommunityPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave3-RobustnessManager|org.cougaar.core.mobility.service.RedirectMovePlugin','1AD-Enclave3-RobustnessManager|org.cougaar.core.mobility.service.RedirectMovePlugin','plugin|org.cougaar.core.mobility.service.RedirectMovePlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave3-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.DecisionPlugin','1AD-Enclave3-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.DecisionPlugin','plugin|org.cougaar.tools.robustness.ma.plugins.DecisionPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave3-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.HealthMonitorPlugin','1AD-Enclave3-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.HealthMonitorPlugin','plugin|org.cougaar.tools.robustness.ma.plugins.HealthMonitorPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave3-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.RestartLocatorPlugin','1AD-Enclave3-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.RestartLocatorPlugin','plugin|org.cougaar.tools.robustness.ma.plugins.RestartLocatorPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave3-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.VacatePlugin','1AD-Enclave3-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.VacatePlugin','plugin|org.cougaar.tools.robustness.ma.plugins.VacatePlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave3-RobustnessManager|org.cougaar.tools.robustness.ma.ui.ParameterModifierServlet','1AD-Enclave3-RobustnessManager|org.cougaar.tools.robustness.ma.ui.ParameterModifierServlet','plugin|org.cougaar.tools.robustness.ma.ui.ParameterModifierServlet','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave3-RobustnessManager|org.cougaar.tools.robustness.ma.ui.RobustnessServlet','1AD-Enclave3-RobustnessManager|org.cougaar.tools.robustness.ma.ui.RobustnessServlet','plugin|org.cougaar.tools.robustness.ma.ui.RobustnessServlet','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave3-RobustnessManager|org.cougaar.tools.robustness.sensors.HeartbeatRequesterPlugin','1AD-Enclave3-RobustnessManager|org.cougaar.tools.robustness.sensors.HeartbeatRequesterPlugin','plugin|org.cougaar.tools.robustness.sensors.HeartbeatRequesterPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave3-RobustnessManager|org.cougaar.tools.robustness.sensors.HeartbeatServerPlugin','1AD-Enclave3-RobustnessManager|org.cougaar.tools.robustness.sensors.HeartbeatServerPlugin','plugin|org.cougaar.tools.robustness.sensors.HeartbeatServerPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave3-RobustnessManager|org.cougaar.tools.robustness.sensors.PingRequesterPlugin','1AD-Enclave3-RobustnessManager|org.cougaar.tools.robustness.sensors.PingRequesterPlugin','plugin|org.cougaar.tools.robustness.sensors.PingRequesterPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave3-RobustnessManager|org.cougaar.tools.robustness.sensors.PingServerPlugin','1AD-Enclave3-RobustnessManager|org.cougaar.tools.robustness.sensors.PingServerPlugin','plugin|org.cougaar.tools.robustness.sensors.PingServerPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave4-RobustnessManager','1AD-Enclave4-RobustnessManager','1AD-Enclave4-RobustnessManager','agent',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave4-RobustnessManager|org.cougaar.community.CommunityPlugin','1AD-Enclave4-RobustnessManager|org.cougaar.community.CommunityPlugin','plugin|org.cougaar.community.CommunityPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave4-RobustnessManager|org.cougaar.core.mobility.service.RedirectMovePlugin','1AD-Enclave4-RobustnessManager|org.cougaar.core.mobility.service.RedirectMovePlugin','plugin|org.cougaar.core.mobility.service.RedirectMovePlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave4-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.DecisionPlugin','1AD-Enclave4-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.DecisionPlugin','plugin|org.cougaar.tools.robustness.ma.plugins.DecisionPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave4-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.HealthMonitorPlugin','1AD-Enclave4-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.HealthMonitorPlugin','plugin|org.cougaar.tools.robustness.ma.plugins.HealthMonitorPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave4-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.RestartLocatorPlugin','1AD-Enclave4-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.RestartLocatorPlugin','plugin|org.cougaar.tools.robustness.ma.plugins.RestartLocatorPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave4-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.VacatePlugin','1AD-Enclave4-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.VacatePlugin','plugin|org.cougaar.tools.robustness.ma.plugins.VacatePlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave4-RobustnessManager|org.cougaar.tools.robustness.ma.ui.ParameterModifierServlet','1AD-Enclave4-RobustnessManager|org.cougaar.tools.robustness.ma.ui.ParameterModifierServlet','plugin|org.cougaar.tools.robustness.ma.ui.ParameterModifierServlet','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave4-RobustnessManager|org.cougaar.tools.robustness.ma.ui.RobustnessServlet','1AD-Enclave4-RobustnessManager|org.cougaar.tools.robustness.ma.ui.RobustnessServlet','plugin|org.cougaar.tools.robustness.ma.ui.RobustnessServlet','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave4-RobustnessManager|org.cougaar.tools.robustness.sensors.HeartbeatRequesterPlugin','1AD-Enclave4-RobustnessManager|org.cougaar.tools.robustness.sensors.HeartbeatRequesterPlugin','plugin|org.cougaar.tools.robustness.sensors.HeartbeatRequesterPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave4-RobustnessManager|org.cougaar.tools.robustness.sensors.HeartbeatServerPlugin','1AD-Enclave4-RobustnessManager|org.cougaar.tools.robustness.sensors.HeartbeatServerPlugin','plugin|org.cougaar.tools.robustness.sensors.HeartbeatServerPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave4-RobustnessManager|org.cougaar.tools.robustness.sensors.PingRequesterPlugin','1AD-Enclave4-RobustnessManager|org.cougaar.tools.robustness.sensors.PingRequesterPlugin','plugin|org.cougaar.tools.robustness.sensors.PingRequesterPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('1AD-Enclave4-RobustnessManager|org.cougaar.tools.robustness.sensors.PingServerPlugin','1AD-Enclave4-RobustnessManager|org.cougaar.tools.robustness.sensors.PingServerPlugin','plugin|org.cougaar.tools.robustness.sensors.PingServerPlugin','plugin',0.000000000000000000000000000000);
UNLOCK TABLES;

--
-- Dumping data for table 'asb_agent'
--


LOCK TABLES asb_agent WRITE;
INSERT INTO asb_agent (ASSEMBLY_ID, COMPONENT_ALIB_ID, COMPONENT_LIB_ID, CLONE_SET_ID, COMPONENT_NAME) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave1-RobustnessManager','1AD-Enclave1-RobustnessManager',0.000000000000000000000000000000,'1AD-Enclave1-RobustnessManager');
INSERT INTO asb_agent (ASSEMBLY_ID, COMPONENT_ALIB_ID, COMPONENT_LIB_ID, CLONE_SET_ID, COMPONENT_NAME) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave2-RobustnessManager','1AD-Enclave2-RobustnessManager',0.000000000000000000000000000000,'1AD-Enclave2-RobustnessManager');
INSERT INTO asb_agent (ASSEMBLY_ID, COMPONENT_ALIB_ID, COMPONENT_LIB_ID, CLONE_SET_ID, COMPONENT_NAME) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave3-RobustnessManager','1AD-Enclave3-RobustnessManager',0.000000000000000000000000000000,'1AD-Enclave3-RobustnessManager');
INSERT INTO asb_agent (ASSEMBLY_ID, COMPONENT_ALIB_ID, COMPONENT_LIB_ID, CLONE_SET_ID, COMPONENT_NAME) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave4-RobustnessManager','1AD-Enclave4-RobustnessManager',0.000000000000000000000000000000,'1AD-Enclave4-RobustnessManager');
UNLOCK TABLES;

--
-- Dumping data for table 'asb_agent_pg_attr'
--


LOCK TABLES asb_agent_pg_attr WRITE;
UNLOCK TABLES;

--
-- Dumping data for table 'asb_agent_relation'
--


LOCK TABLES asb_agent_relation WRITE;
UNLOCK TABLES;

--
-- Dumping data for table 'asb_assembly'
--


LOCK TABLES asb_assembly WRITE;
INSERT INTO asb_assembly (ASSEMBLY_ID, ASSEMBLY_TYPE, DESCRIPTION) VALUES ('RCP-0006-RobustnessManagementAgents','RCP','RobustnessManagementAgents-cpy');
UNLOCK TABLES;

--
-- Dumping data for table 'asb_component_arg'
--


LOCK TABLES asb_component_arg WRITE;
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave1-RobustnessManager','1AD-Enclave1-RobustnessManager',1.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave2-RobustnessManager','1AD-Enclave2-RobustnessManager',1.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave3-RobustnessManager','1AD-Enclave3-RobustnessManager',1.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave4-RobustnessManager','1AD-Enclave4-RobustnessManager',1.000000000000000000000000000000);
UNLOCK TABLES;

--
-- Dumping data for table 'asb_component_hierarchy'
--


LOCK TABLES asb_component_hierarchy WRITE;
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave1-RobustnessManager','RobustnessManagementAgents-cpy','COMPONENT',0.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave1-RobustnessManager|org.cougaar.community.CommunityPlugin','1AD-Enclave1-RobustnessManager','COMPONENT',0.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave1-RobustnessManager|org.cougaar.core.mobility.service.RedirectMovePlugin','1AD-Enclave1-RobustnessManager','COMPONENT',1.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave1-RobustnessManager|org.cougaar.tools.robustness.sensors.PingRequesterPlugin','1AD-Enclave1-RobustnessManager','COMPONENT',2.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave1-RobustnessManager|org.cougaar.tools.robustness.sensors.PingServerPlugin','1AD-Enclave1-RobustnessManager','COMPONENT',3.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave1-RobustnessManager|org.cougaar.tools.robustness.sensors.HeartbeatRequesterPlugin','1AD-Enclave1-RobustnessManager','COMPONENT',4.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave1-RobustnessManager|org.cougaar.tools.robustness.sensors.HeartbeatServerPlugin','1AD-Enclave1-RobustnessManager','COMPONENT',5.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave1-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.DecisionPlugin','1AD-Enclave1-RobustnessManager','COMPONENT',6.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave1-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.HealthMonitorPlugin','1AD-Enclave1-RobustnessManager','COMPONENT',7.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave1-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.RestartLocatorPlugin','1AD-Enclave1-RobustnessManager','COMPONENT',8.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave1-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.VacatePlugin','1AD-Enclave1-RobustnessManager','COMPONENT',9.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave1-RobustnessManager|org.cougaar.tools.robustness.ma.ui.ParameterModifierServlet','1AD-Enclave1-RobustnessManager','COMPONENT',10.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave1-RobustnessManager|org.cougaar.tools.robustness.ma.ui.RobustnessServlet','1AD-Enclave1-RobustnessManager','COMPONENT',11.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave2-RobustnessManager','RobustnessManagementAgents-cpy','COMPONENT',1.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave2-RobustnessManager|org.cougaar.community.CommunityPlugin','1AD-Enclave2-RobustnessManager','COMPONENT',0.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave2-RobustnessManager|org.cougaar.core.mobility.service.RedirectMovePlugin','1AD-Enclave2-RobustnessManager','COMPONENT',1.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave2-RobustnessManager|org.cougaar.tools.robustness.sensors.PingRequesterPlugin','1AD-Enclave2-RobustnessManager','COMPONENT',2.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave2-RobustnessManager|org.cougaar.tools.robustness.sensors.PingServerPlugin','1AD-Enclave2-RobustnessManager','COMPONENT',3.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave2-RobustnessManager|org.cougaar.tools.robustness.sensors.HeartbeatRequesterPlugin','1AD-Enclave2-RobustnessManager','COMPONENT',4.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave2-RobustnessManager|org.cougaar.tools.robustness.sensors.HeartbeatServerPlugin','1AD-Enclave2-RobustnessManager','COMPONENT',5.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave2-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.DecisionPlugin','1AD-Enclave2-RobustnessManager','COMPONENT',6.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave2-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.HealthMonitorPlugin','1AD-Enclave2-RobustnessManager','COMPONENT',7.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave2-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.RestartLocatorPlugin','1AD-Enclave2-RobustnessManager','COMPONENT',8.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave2-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.VacatePlugin','1AD-Enclave2-RobustnessManager','COMPONENT',9.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave2-RobustnessManager|org.cougaar.tools.robustness.ma.ui.ParameterModifierServlet','1AD-Enclave2-RobustnessManager','COMPONENT',10.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave2-RobustnessManager|org.cougaar.tools.robustness.ma.ui.RobustnessServlet','1AD-Enclave2-RobustnessManager','COMPONENT',11.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave3-RobustnessManager','RobustnessManagementAgents-cpy','COMPONENT',2.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave3-RobustnessManager|org.cougaar.community.CommunityPlugin','1AD-Enclave3-RobustnessManager','COMPONENT',0.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave3-RobustnessManager|org.cougaar.core.mobility.service.RedirectMovePlugin','1AD-Enclave3-RobustnessManager','COMPONENT',1.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave3-RobustnessManager|org.cougaar.tools.robustness.sensors.PingRequesterPlugin','1AD-Enclave3-RobustnessManager','COMPONENT',2.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave3-RobustnessManager|org.cougaar.tools.robustness.sensors.PingServerPlugin','1AD-Enclave3-RobustnessManager','COMPONENT',3.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave3-RobustnessManager|org.cougaar.tools.robustness.sensors.HeartbeatRequesterPlugin','1AD-Enclave3-RobustnessManager','COMPONENT',4.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave3-RobustnessManager|org.cougaar.tools.robustness.sensors.HeartbeatServerPlugin','1AD-Enclave3-RobustnessManager','COMPONENT',5.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave3-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.DecisionPlugin','1AD-Enclave3-RobustnessManager','COMPONENT',6.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave3-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.HealthMonitorPlugin','1AD-Enclave3-RobustnessManager','COMPONENT',7.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave3-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.RestartLocatorPlugin','1AD-Enclave3-RobustnessManager','COMPONENT',8.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave3-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.VacatePlugin','1AD-Enclave3-RobustnessManager','COMPONENT',9.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave3-RobustnessManager|org.cougaar.tools.robustness.ma.ui.ParameterModifierServlet','1AD-Enclave3-RobustnessManager','COMPONENT',10.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave3-RobustnessManager|org.cougaar.tools.robustness.ma.ui.RobustnessServlet','1AD-Enclave3-RobustnessManager','COMPONENT',11.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave4-RobustnessManager','RobustnessManagementAgents-cpy','COMPONENT',3.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave4-RobustnessManager|org.cougaar.community.CommunityPlugin','1AD-Enclave4-RobustnessManager','COMPONENT',0.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave4-RobustnessManager|org.cougaar.core.mobility.service.RedirectMovePlugin','1AD-Enclave4-RobustnessManager','COMPONENT',1.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave4-RobustnessManager|org.cougaar.tools.robustness.sensors.PingRequesterPlugin','1AD-Enclave4-RobustnessManager','COMPONENT',2.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave4-RobustnessManager|org.cougaar.tools.robustness.sensors.PingServerPlugin','1AD-Enclave4-RobustnessManager','COMPONENT',3.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave4-RobustnessManager|org.cougaar.tools.robustness.sensors.HeartbeatRequesterPlugin','1AD-Enclave4-RobustnessManager','COMPONENT',4.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave4-RobustnessManager|org.cougaar.tools.robustness.sensors.HeartbeatServerPlugin','1AD-Enclave4-RobustnessManager','COMPONENT',5.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave4-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.DecisionPlugin','1AD-Enclave4-RobustnessManager','COMPONENT',6.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave4-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.HealthMonitorPlugin','1AD-Enclave4-RobustnessManager','COMPONENT',7.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave4-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.RestartLocatorPlugin','1AD-Enclave4-RobustnessManager','COMPONENT',8.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave4-RobustnessManager|org.cougaar.tools.robustness.ma.plugins.VacatePlugin','1AD-Enclave4-RobustnessManager','COMPONENT',9.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave4-RobustnessManager|org.cougaar.tools.robustness.ma.ui.ParameterModifierServlet','1AD-Enclave4-RobustnessManager','COMPONENT',10.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0006-RobustnessManagementAgents','1AD-Enclave4-RobustnessManager|org.cougaar.tools.robustness.ma.ui.RobustnessServlet','1AD-Enclave4-RobustnessManager','COMPONENT',11.000000000000000000000000000000);
UNLOCK TABLES;

--
-- Dumping data for table 'asb_oplan'
--


LOCK TABLES asb_oplan WRITE;
UNLOCK TABLES;

--
-- Dumping data for table 'asb_oplan_agent_attr'
--


LOCK TABLES asb_oplan_agent_attr WRITE;
UNLOCK TABLES;

--
-- Dumping data for table 'community_attribute'
--


LOCK TABLES community_attribute WRITE;
UNLOCK TABLES;

--
-- Dumping data for table 'community_entity_attribute'
--


LOCK TABLES community_entity_attribute WRITE;
UNLOCK TABLES;

--
-- Dumping data for table 'lib_agent_org'
--


LOCK TABLES lib_agent_org WRITE;
REPLACE INTO lib_agent_org (COMPONENT_LIB_ID, AGENT_LIB_NAME, AGENT_ORG_CLASS) VALUES ('1AD-Enclave1-RobustnessManager','1AD-Enclave1-RobustnessManager','MilitaryOrganization');
REPLACE INTO lib_agent_org (COMPONENT_LIB_ID, AGENT_LIB_NAME, AGENT_ORG_CLASS) VALUES ('1AD-Enclave2-RobustnessManager','1AD-Enclave2-RobustnessManager','MilitaryOrganization');
REPLACE INTO lib_agent_org (COMPONENT_LIB_ID, AGENT_LIB_NAME, AGENT_ORG_CLASS) VALUES ('1AD-Enclave3-RobustnessManager','1AD-Enclave3-RobustnessManager','MilitaryOrganization');
REPLACE INTO lib_agent_org (COMPONENT_LIB_ID, AGENT_LIB_NAME, AGENT_ORG_CLASS) VALUES ('1AD-Enclave4-RobustnessManager','1AD-Enclave4-RobustnessManager','MilitaryOrganization');
UNLOCK TABLES;

--
-- Dumping data for table 'lib_component'
--


LOCK TABLES lib_component WRITE;
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('1AD-Enclave1-RobustnessManager','agent','org.cougaar.core.agent.ClusterImpl','Node.AgentManager.Agent','Added agent');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('recipe|##RECIPE_CLASS##','recipe','##RECIPE_CLASS##','recipe','Added recipe');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.community.CommunityPlugin','plugin','org.cougaar.community.CommunityPlugin','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.core.mobility.service.RedirectMovePlugin','plugin','org.cougaar.core.mobility.service.RedirectMovePlugin','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.tools.robustness.ma.plugins.DecisionPlugin','plugin','org.cougaar.tools.robustness.ma.plugins.DecisionPlugin','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.tools.robustness.ma.plugins.HealthMonitorPlugin','plugin','org.cougaar.tools.robustness.ma.plugins.HealthMonitorPlugin','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.tools.robustness.ma.plugins.RestartLocatorPlugin','plugin','org.cougaar.tools.robustness.ma.plugins.RestartLocatorPlugin','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.tools.robustness.ma.plugins.VacatePlugin','plugin','org.cougaar.tools.robustness.ma.plugins.VacatePlugin','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.tools.robustness.ma.ui.ParameterModifierServlet','plugin','org.cougaar.tools.robustness.ma.ui.ParameterModifierServlet','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.tools.robustness.ma.ui.RobustnessServlet','plugin','org.cougaar.tools.robustness.ma.ui.RobustnessServlet','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.tools.robustness.sensors.HeartbeatRequesterPlugin','plugin','org.cougaar.tools.robustness.sensors.HeartbeatRequesterPlugin','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.tools.robustness.sensors.HeartbeatServerPlugin','plugin','org.cougaar.tools.robustness.sensors.HeartbeatServerPlugin','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.tools.robustness.sensors.PingRequesterPlugin','plugin','org.cougaar.tools.robustness.sensors.PingRequesterPlugin','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.tools.robustness.sensors.PingServerPlugin','plugin','org.cougaar.tools.robustness.sensors.PingServerPlugin','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('1AD-Enclave2-RobustnessManager','agent','org.cougaar.core.agent.ClusterImpl','Node.AgentManager.Agent','Added agent');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('1AD-Enclave3-RobustnessManager','agent','org.cougaar.core.agent.ClusterImpl','Node.AgentManager.Agent','Added agent');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('1AD-Enclave4-RobustnessManager','agent','org.cougaar.core.agent.ClusterImpl','Node.AgentManager.Agent','Added agent');
UNLOCK TABLES;

--
-- Dumping data for table 'lib_mod_recipe'
--


LOCK TABLES lib_mod_recipe WRITE;
REPLACE INTO lib_mod_recipe (MOD_RECIPE_LIB_ID, NAME, JAVA_CLASS, DESCRIPTION) VALUES ('RECIPE-0023RobustnessManagementAgents','RobustnessManagementAgents-cpy','org.cougaar.tools.csmart.recipe.CompleteAgentRecipe','No description available');
UNLOCK TABLES;

--
-- Dumping data for table 'lib_mod_recipe_arg'
--


LOCK TABLES lib_mod_recipe_arg WRITE;
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0023RobustnessManagementAgents','Assembly Id',0.000000000000000000000000000000,'RCP-0006-RobustnessManagementAgents');
UNLOCK TABLES;

--
-- Dumping data for table 'lib_pg_attribute'
--


LOCK TABLES lib_pg_attribute WRITE;
UNLOCK TABLES;

