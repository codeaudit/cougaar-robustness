#!/bin/tcsh
rm SMALL-1AD-TRANS-Small1_MASTER_withMgrNode.xml
rm SMALL-1AD-TRANS-Small1-UC7.xml.rb
cp SMALL-1AD-TRANS-Small1_MASTER_withMgrNode.ORIG.xml SMALL-1AD-TRANS-Small1_MASTER_withMgrNode.xml
ruby edit.rb SMALL-1AD-TRANS-Small1_MASTER_withMgrNode.xml
ruby ../../config/bin/transform_society.rb -i SMALL-1AD-TRANS-Small1_MASTER_withMgrNode.xml.rb -r . -o SMALL-1AD-TRANS-Small1-UC7.xml.rb

