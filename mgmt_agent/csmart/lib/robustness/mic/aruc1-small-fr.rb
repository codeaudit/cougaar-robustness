=begin experiment

name: ARUC1-Forced-Restart
description: AR UC1 Forced Restart
script: BaselineTemplateStage1.rb
parameters:
  - run_count: 1
  - society_file: $CIP/csmart/config/societies/ad/SMALL-1AD-TC20.rb
  - layout_file: $CIP/operator/uc1-small-1ad-layout.xml
  - archive_dir: $CIP/Logs

  - rules:
    - $CIP/csmart/config/rules/isat
    - $CIP/csmart/config/rules/yp
    - $CIP/csmart/config/rules/logistics
    - $CIP/csmart/config/rules/robustness/manager.rule
    - $CIP/csmart/config/rules/robustness/uc1
    - $CIP/csmart/config/rules/robustness/uc1/debug
    - $CIP/csmart/config/rules/robustness/uc1/tuning
    - $CIP/csmart/config/rules/robustness/uc9
  - community_rules:
    - $CIP/csmart/config/rules/robustness/communities/community.rule

include_scripts:
  - script: $CIP/csmart/scripts/definitions/clearPnLogs.rb
  - script: $CIP/csmart/scripts/definitions/setup_robustness.rb
  - script: forced_restart.rb

=end

require 'cougaar/scripting'
Cougaar::ExperimentDefinition.register(__FILE__)
