#!/bin/sh
# Script for creating a new ACME XML society by applying a new host/node/agent
# config to an existing XML society.  The script takes 3 inputs.  The first
# is the path to the existing society config file, the second is the path to
# the config file containing the new host/node/agent mapping, and the third
# parameter is the name of the host designated as the name server.

ACMEDIR="/shares/development/acme/csmart"
#ACMEDIR="/shares/development/acme/install_set/current/csmart"
RULESDIR="../../../rules/robustness/uc1"

java -cp $COUGAAR_INSTALL_PATH/lib/ar_mic.jar org.cougaar.tools.robustness.ma.util.ACMEXmlTransformer $1 $2 tmp.xml aruc1.xsl
ruby $ACMEDIR/config/bin/transform_society.rb -i tmp.xml -r $RULESDIR
cat new-tmp.xml | sed "s/localhost/$3/" | sed 's/jar=/jar/' > aruc1_society.xml
rm new-tmp.xml 
