#!/bin/sh
ACMEDIR='/shares/development/acme'
WRKDIR=`pwd`

java -cp ../lib/Robustness_mic_mgmt_agent.jar org.cougaar.tools.robustness.ma.util.ACMEXmlTransformer $1 $2 tmp.xml ../aruc.xsl

cd $ACMEDIR/acme_scripting/bin
ruby transform_society.rb -i $WRKDIR/tmp.xml -r $WRKDIR/transform_rules
mv new-tmp.xml $WRKDIR/tmp1.xml
cd $WRKDIR
cat tmp1.xml | sed "s/localhost/$3/" > aruc1_society.xml
rm tmp.xml tmp1.xml
