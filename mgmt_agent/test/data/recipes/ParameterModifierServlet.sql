# MySQL dump 8.16
#
# Host: localhost    Database: tempcopy
#--------------------------------------------------------
# Server version	3.23.44-nt

#
# Dumping data for table 'alib_component'
#

LOCK TABLES alib_component WRITE;
UNLOCK TABLES;

#
# Dumping data for table 'asb_agent'
#

LOCK TABLES asb_agent WRITE;
UNLOCK TABLES;

#
# Dumping data for table 'asb_agent_pg_attr'
#

LOCK TABLES asb_agent_pg_attr WRITE;
UNLOCK TABLES;

#
# Dumping data for table 'asb_agent_relation'
#

LOCK TABLES asb_agent_relation WRITE;
UNLOCK TABLES;

#
# Dumping data for table 'asb_assembly'
#

LOCK TABLES asb_assembly WRITE;
UNLOCK TABLES;

#
# Dumping data for table 'asb_component_arg'
#

LOCK TABLES asb_component_arg WRITE;
UNLOCK TABLES;

#
# Dumping data for table 'asb_component_hierarchy'
#

LOCK TABLES asb_component_hierarchy WRITE;
UNLOCK TABLES;

#
# Dumping data for table 'asb_oplan'
#

LOCK TABLES asb_oplan WRITE;
UNLOCK TABLES;

#
# Dumping data for table 'asb_oplan_agent_attr'
#

LOCK TABLES asb_oplan_agent_attr WRITE;
UNLOCK TABLES;

#
# Dumping data for table 'community_attribute'
#

LOCK TABLES community_attribute WRITE;
UNLOCK TABLES;

#
# Dumping data for table 'community_entity_attribute'
#

LOCK TABLES community_entity_attribute WRITE;
UNLOCK TABLES;

#
# Dumping data for table 'lib_agent_org'
#

LOCK TABLES lib_agent_org WRITE;
UNLOCK TABLES;

#
# Dumping data for table 'lib_component'
#

LOCK TABLES lib_component WRITE;
UNLOCK TABLES;

#
# Dumping data for table 'lib_mod_recipe'
#

LOCK TABLES lib_mod_recipe WRITE;
REPLACE INTO lib_mod_recipe (MOD_RECIPE_LIB_ID, NAME, JAVA_CLASS, DESCRIPTION) VALUES ('RECIPE-0012ParameterModifierServletInsertion','ParameterModifierServletInsertion','org.cougaar.tools.csmart.recipe.SpecificInsertionRecipe','No description available');
UNLOCK TABLES;

#
# Dumping data for table 'lib_mod_recipe_arg'
#

LOCK TABLES lib_mod_recipe_arg WRITE;
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0012ParameterModifierServletInsertion','Class Name',2.000000000000000000000000000000,'org.cougaar.tools.robustness.ma.ui.ParameterModifierServlet');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0012ParameterModifierServletInsertion','Component Name',4.000000000000000000000000000000,'org.cougaar.tools.robustness.ma.ui.ParameterModifierServlet');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0012ParameterModifierServletInsertion','Component Priority',0.000000000000000000000000000000,'COMPONENT');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0012ParameterModifierServletInsertion','Number of Arguments',1.000000000000000000000000000000,'0');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0012ParameterModifierServletInsertion','Target Component Selection Query',3.000000000000000000000000000000,'recipeQueryManagementAgent');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0012ParameterModifierServletInsertion','Type of Insertion',5.000000000000000000000000000000,'plugin');
UNLOCK TABLES;

#
# Dumping data for table 'lib_pg_attribute'
#

LOCK TABLES lib_pg_attribute WRITE;
UNLOCK TABLES;

