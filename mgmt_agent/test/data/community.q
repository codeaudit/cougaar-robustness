# Queries for retrieving assemblyID and updating community tables
database=${org.cougaar.configuration.database}
username=${org.cougaar.configuration.user}
password=${org.cougaar.configuration.password}

queryAssemblyID = \
 SELECT DISTINCT E.ASSEMBLY_ID \
 FROM v4_expt_experiment B, \
      v4_expt_trial_assembly E, \
      v4_asb_assembly A \
 WHERE A.ASSEMBLY_ID = E.ASSEMBLY_ID \
       AND A.ASSEMBLY_TYPE = 'COMM' \
       AND E.EXPT_ID = B.EXPT_ID \
       AND NAME = ':experimentName'

queryCommunityInfo = \
  SELECT DISTINCT ASSEMBLY_ID \
  FROM community_attribute \
  WHERE COMMUNITY_ID = ':community_id' \
        AND ATTRIBUTE_ID = 'CommunityType' \
	AND ATTRIBUTE_VALUE = ':community_type'

queryEntityInfo = \
  SELECT DISTINCT ASSEMBLY_ID \
  FROM community_entity_attribute \
  WHERE COMMUNITY_ID = ':community_id' \
        AND ENTITY_ID = ':entity_id' \
	AND ATTRIBUTE_ID = ':attribute_id' \
	AND ATTRIBUTE_VALUE = ':attribute_value'

