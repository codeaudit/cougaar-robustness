#!/bin/tcsh
rm SC-1AD-NEW-AL-RULES-$1.xml.rb
ruby edit.rb SC-1AD-NEW-AL-STRIPPED.xml
ruby ../../../../config/bin/transform_society.rb -i SC-1AD-NEW-AL-RULES.xml.rb -r ../../../rules/robustness/uc2/$1.rule -o SC-1AD-NEW-AL-RULES-$1.xml.rb
