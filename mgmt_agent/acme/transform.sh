#!/bin/sh
ACMEDIR='/shares/development/acme'
WRKDIR=`pwd`

cd $ACMEDIR/acme_scripting/bin
ruby transform_society.rb -i $WRKDIR/$1 -r $WRKDIR/transform_rules
mv new-$1 $WRKDIR/aruc1_society.xml

cd $WRKDIR
