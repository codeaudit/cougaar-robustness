#!/bin/sh
#ACMEDIR='/usr/local/acme'
ACMEDIR="$COUGAAR_INSTALL_PATH/csmart"
WRKDIR=`pwd`
RULESDIR="$WRKDIR/../../../rules/robustness/uc1"

java -cp $COUGAAR_INSTALL_PATH/lib/ar_mic.jar org.cougaar.tools.robustness.ma.util.ACMEXmlTransformer $1 $2 tmp.xml aruc1.xsl

cd $ACMEDIR/acme_scripting/bin
ruby transform_society.rb -i $WRKDIR/tmp.xml -r $RULESDIR
mv new-tmp.xml $WRKDIR/tmp1.xml
cd $WRKDIR
cat tmp1.xml | sed "s/localhost/$3/" > aruc1_society.xml
rm tmp.xml tmp1.xml
