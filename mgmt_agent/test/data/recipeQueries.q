# Find all agents including NodeAgents
recipeQueryAllAgentsAndNodeAgents=\
 SELECT C.COMPONENT_ALIB_ID \
   FROM alib_component C, asb_component_hierarchy H \
  WHERE (C.COMPONENT_TYPE='agent' OR C.COMPONENT_TYPE='node') \
    AND (H.COMPONENT_ALIB_ID = C.COMPONENT_ALIB_ID OR H.PARENT_COMPONENT_ALIB_ID = C.COMPONENT_ALIB_ID) \
    AND H.ASSEMBLY_ID :assembly_match:

# UserAdminAgent Query
recipeQueryUserAdminAgent=\
 SELECT COMPONENT_ALIB_ID FROM V4_ALIB_COMPONENT WHERE COMPONENT_TYPE = 'agent' AND COMPONENT_NAME='UserAdminAgent'


# Find all agents that are SecurityMnRManagers
# Use this when adding AdaptivityEngine stuff for security thread
# Can also use this for components that go in every such Agent
recipeQuerySecurityMnRAgents=\
 SELECT C.COMPONENT_ALIB_ID \
   FROM alib_component C, asb_component_hierarchy H \
  WHERE C.COMPONENT_TYPE='agent' AND C.COMPONENT_ALIB_ID like \
                               '%SecurityMnRManager' \
    AND (H.COMPONENT_ALIB_ID = C.COMPONENT_ALIB_ID OR H.PARENT_COMPONENT_ALIB_ID = C.COMPONENT_ALIB_ID) \
    AND H.ASSEMBLY_ID :assembly_match:

# Find all agents that are SecurityMnRManagers for an Enclave
# Use this when adding AdaptivityFilter stuff for security thread
# Can also use this for components that go in every such Agent
recipeQueryEnclaveSecurityMnRAgents=\
 SELECT C.COMPONENT_ALIB_ID \
   FROM alib_component C, asb_component_hierarchy H \
  WHERE C.COMPONENT_TYPE='agent' AND C.COMPONENT_ALIB_ID like \
                               'Enclave%SecurityMnRManager' \
    AND (H.COMPONENT_ALIB_ID = C.COMPONENT_ALIB_ID OR H.PARENT_COMPONENT_ALIB_ID = C.COMPONENT_ALIB_ID) \
    AND H.ASSEMBLY_ID :assembly_match:

# Find the Enclave1SecurityManager agent
# Use this when adding AdaptivityFilter stuff for security thread
# Can also use this for components that go in every such Agent
recipeQueryEnclave1SecurityMnRAgent=\
 SELECT C.COMPONENT_ALIB_ID \
   FROM alib_component C, asb_component_hierarchy H \
  WHERE C.COMPONENT_TYPE='agent' AND C.COMPONENT_ALIB_ID = \
                               'Enclave1SecurityMnRManager' \
    AND (H.COMPONENT_ALIB_ID = C.COMPONENT_ALIB_ID OR H.PARENT_COMPONENT_ALIB_ID = C.COMPONENT_ALIB_ID) \
    AND H.ASSEMBLY_ID :assembly_match:

# Find the Enclave2SecurityManager agent
# Use this when adding AdaptivityFilter stuff for security thread
# Can also use this for components that go in every such Agent
recipeQueryEnclave2SecurityMnRAgent=\
 SELECT C.COMPONENT_ALIB_ID \
   FROM alib_component C, asb_component_hierarchy H \
  WHERE C.COMPONENT_TYPE='agent' AND C.COMPONENT_ALIB_ID = \
                               'Enclave2SecurityMnRManager' \
    AND (H.COMPONENT_ALIB_ID = C.COMPONENT_ALIB_ID OR H.PARENT_COMPONENT_ALIB_ID = C.COMPONENT_ALIB_ID) \
    AND H.ASSEMBLY_ID :assembly_match:

# Find the Enclave3SecurityManager agent
# Use this when adding AdaptivityFilter stuff for security thread
# Can also use this for components that go in every such Agent
recipeQueryEnclave3SecurityMnRAgent=\
 SELECT C.COMPONENT_ALIB_ID \
   FROM alib_component C, asb_component_hierarchy H \
  WHERE C.COMPONENT_TYPE='agent' AND C.COMPONENT_ALIB_ID = \
                               'Enclave3SecurityMnRManager' \
    AND (H.COMPONENT_ALIB_ID = C.COMPONENT_ALIB_ID OR H.PARENT_COMPONENT_ALIB_ID = C.COMPONENT_ALIB_ID) \
    AND H.ASSEMBLY_ID :assembly_match:

# Find the Enclave4SecurityManager agent
# Use this when adding AdaptivityFilter stuff for security thread
# Can also use this for components that go in every such Agent
recipeQueryEnclave4SecurityMnRAgent=\
 SELECT C.COMPONENT_ALIB_ID \
   FROM alib_component C, asb_component_hierarchy H \
  WHERE C.COMPONENT_TYPE='agent' AND C.COMPONENT_ALIB_ID = \
                               'Enclave4SecurityMnRManager' \
    AND (H.COMPONENT_ALIB_ID = C.COMPONENT_ALIB_ID OR H.PARENT_COMPONENT_ALIB_ID = C.COMPONENT_ALIB_ID) \
    AND H.ASSEMBLY_ID :assembly_match:

# Get all Agents that are _not_ SecurityMnRAgents. IE, those that are managed
recipeQueryNOTSecurityMnRAgents=\
 SELECT C.COMPONENT_ALIB_ID \
   FROM alib_component C, asb_component_hierarchy H \
  WHERE (C.COMPONENT_TYPE='agent' OR C.COMPONENT_TYPE='node') AND C.COMPONENT_ALIB_ID not like \
                               '%SecurityMnRManager' \
    AND (H.COMPONENT_ALIB_ID = C.COMPONENT_ALIB_ID OR H.PARENT_COMPONENT_ALIB_ID = C.COMPONENT_ALIB_ID) \
    AND H.ASSEMBLY_ID :assembly_match:


#This is for LoginFailureSensor stuff
#this one is for BootStrapEventPlugin0.sql
recipeQuerySocietySecurityMnRAgent=\
SELECT C.COMPONENT_ALIB_ID \
   FROM alib_component C, asb_component_hierarchy H \
  WHERE C.COMPONENT_TYPE='agent' AND C.COMPONENT_ALIB_ID = \
                               'SocietySecurityMnRManager' \
    AND (H.COMPONENT_ALIB_ID = C.COMPONENT_ALIB_ID OR H.PARENT_COMPONENT_ALIB_ID = C.COMPONENT_ALIB_ID) \
    AND H.ASSEMBLY_ID :assembly_match:


# Find all agents that are DomainManger
recipeQueryDomainManagerAgent=\
 SELECT C.COMPONENT_ALIB_ID \
   FROM alib_component C, asb_component_hierarchy H \
  WHERE C.COMPONENT_TYPE='agent' AND C.COMPONENT_ALIB_ID = \
                               'DomainManager' AND \
    (H.COMPONENT_ALIB_ID = C.COMPONENT_ALIB_ID OR H.PARENT_COMPONENT_ALIB_ID = C.COMPONENT_ALIB_ID) \
    AND H.ASSEMBLY_ID :assembly_match:


recipeQueryRoverAgent=\
 SELECT COMPONENT_ALIB_ID FROM alib_component WHERE COMPONENT_TYPE = 'agent' AND COMPONENT_NAME='TestRover'

recipeQueryRoverControllerAgent=\
 SELECT COMPONENT_ALIB_ID FROM alib_component WHERE COMPONENT_TYPE = 'agent' AND COMPONENT_NAME='TestRoverController'


recipeQueryForUMmrmangerAgent=\
 SELECT C.COMPONENT_ALIB_ID \
   FROM V4_ALIB_COMPONENT C, V4_ASB_COMPONENT_HIERARCHY H \
  WHERE C.COMPONENT_TYPE='agent' \
    AND (H.COMPONENT_ALIB_ID = C.COMPONENT_ALIB_ID OR H.PARENT_COMPONENT_ALIB_ID = C.COMPONENT_ALIB_ID) \
    AND H.ASSEMBLY_ID :assembly_match: \
    AND C.COMPONENT_NAME = 'UMmrmanager'

# Find TestSensor agent (this agent is not needed except for testing)
recipeQueryForTestSensorAgent=\
 SELECT C.COMPONENT_ALIB_ID \
   FROM V4_ALIB_COMPONENT C, V4_ASB_COMPONENT_HIERARCHY H \
  WHERE C.COMPONENT_TYPE='agent' \
    AND (H.COMPONENT_ALIB_ID = C.COMPONENT_ALIB_ID OR H.PARENT_COMPONENT_ALIB_ID = C.COMPONENT_ALIB_ID) \
    AND H.ASSEMBLY_ID :assembly_match: \
    AND C.COMPONENT_NAME = 'TestSensorAgent'

# AGG-Agent query and NOT AGG-agent Query
recipeQueryAGGAgent=\
 SELECT COMPONENT_ALIB_ID FROM V4_ALIB_COMPONENT WHERE COMPONENT_TYPE = 'agent' AND COMPONENT_NAME='AGG-Agent'

# Both "Agg-agent" and the UofM manager are aggregators
recipeQueryNotAGGAgent=\
 SELECT COMPONENT_ALIB_ID FROM V4_ALIB_COMPONENT WHERE (COMPONENT_TYPE = 'agent' OR COMPONENT_TYPE = 'node') AND \
   COMPONENT_NAME NOT IN ('AGG-Agent', 'UMmrmanager') AND \
   COMPONENT_NAME NOT LIKE '%SecurityMnRManager'

# All Node Agents that are members of Enclave1Security-COMM
recipeQueryEnclave1SecurityCommunityNodeAgents=\
 SELECT COMPONENT_ALIB_ID FROM V4_ALIB_COMPONENT, COMMUNITY_ENTITY_ATTRIBUTE WHERE COMPONENT_TYPE = 'node' AND \
   COMPONENT_NAME = ENTITY_ID and COMMUNITY_ID = 'Enclave1Security-COMM'

# All Node Agents that are members of Enclave2Security-COMM
recipeQueryEnclave2SecurityCommunityNodeAgents=\
 SELECT COMPONENT_ALIB_ID FROM V4_ALIB_COMPONENT, COMMUNITY_ENTITY_ATTRIBUTE WHERE COMPONENT_TYPE = 'node' AND \
   COMPONENT_NAME = ENTITY_ID and COMMUNITY_ID = 'Enclave2Security-COMM'

# All Node Agents that are members of Enclave3Security-COMM
recipeQueryEnclave3SecurityCommunityNodeAgents=\
 SELECT COMPONENT_ALIB_ID FROM V4_ALIB_COMPONENT, COMMUNITY_ENTITY_ATTRIBUTE WHERE COMPONENT_TYPE = 'node' AND \
   COMPONENT_NAME = ENTITY_ID and COMMUNITY_ID = 'Enclave3Security-COMM'

# All Node Agents that are members of Enclave4Security-COMM
recipeQueryEnclave4SecurityCommunityNodeAgents=\
 SELECT COMPONENT_ALIB_ID FROM V4_ALIB_COMPONENT, COMMUNITY_ENTITY_ATTRIBUTE WHERE COMPONENT_TYPE = 'node' AND \
   COMPONENT_NAME = ENTITY_ID and COMMUNITY_ID = 'Enclave4Security-COMM'

# All SPECIFIC Node Agents that are members of Enclave0 for now belong to Security-Mgmt-COMM
recipeQueryEnclave0SecurityCommunityNodeAgents=\
 SELECT COMPONENT_ALIB_ID FROM V4_ALIB_COMPONENT WHERE COMPONENT_TYPE = 'node' AND \
   COMPONENT_NAME IN ('SOCIETY-SECURITY','USERADMIN-NODE') 

# All ManagementAgents 
recipeQueryManagementAgent=\
 SELECT COMPONENT_ALIB_ID FROM alib_component WHERE COMPONENT_TYPE = 'agent' AND COMPONENT_NAME LIKE '%RobustnessManager'

# All Enclave1ManagementAgents 
recipeQueryEnclave1ManagementAgent=\
 SELECT COMPONENT_ALIB_ID FROM alib_component WHERE COMPONENT_TYPE = 'agent' AND COMPONENT_NAME LIKE '%Enclave1SecurityRobustnessManager'

# All Enclave2ManagementAgents 
recipeQueryEnclave2ManagementAgent=\
 SELECT COMPONENT_ALIB_ID FROM alib_component WHERE COMPONENT_TYPE = 'agent' AND COMPONENT_NAME LIKE '%Enclave2SecurityRobustnessManager'


# All Not ManagementAgents 
recipeQueryNotManagementAgent=\
 SELECT COMPONENT_ALIB_ID FROM alib_component WHERE COMPONENT_TYPE = 'agent' AND COMPONENT_NAME NOT LIKE '%RobustnessManager'

# Query the PlanLogAgent agent.  This is specific to the PSU Castallen sensors.
recipeQueryPlanLogServerAgent=\
 SELECT COMPONENT_ALIB_ID FROM V4_ALIB_COMPONENT WHERE COMPONENT_TYPE = 'agent' AND COMPONENT_NAME='PlanLogAgent'
