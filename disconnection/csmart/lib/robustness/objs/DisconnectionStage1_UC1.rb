=begin experiment

name: DisconnectionStage1wUC1
description: Disconnection (ARUC7) with UC1 loaded through Stage1
script: $CIP/csmart/lib/robustness/objs/BaselineTemplateStage1.rb
parameters:
  - run_count: 1
  - society_file: $CIP/csmart/config/societies/ua/full-tc20-232a703v.plugins.rb
  - layout_file: $CIP/operator/layouts/FULL-UA-MNGR-33H63N-layout.xml
  - archive_dir: $CIP/Logs
  
  - rules:
    - $CIP/csmart/config/rules/isat
    - $CIP/csmart/config/rules/yp
    - $CIP/csmart/config/rules/logistics
    - $CIP/csmart/config/rules/robustness/manager.rule
    - $CIP/csmart/config/rules/robustness/uc1/manager.rule
    - $CIP/csmart/config/rules/robustness/uc1/aruc1.rule
    - $CIP/csmart/config/rules/robustness/uc1/mic.rule
    - $CIP/csmart/config/rules/robustness/uc7
    - $CIP/csmart/config/rules/robustness/uc9
    - $CIP/csmart/config/rules/metrics/basic
    - $CIP/csmart/config/rules/metrics/sensors
  - community_rules:
    - $CIP/csmart/config/rules/robustness/communities/community.rule

include_scripts:
  - script: $CIP/csmart/scripts/definitions/clearPnLogs.rb
  - script: $CIP/csmart/lib/robustness/objs/disconnection.rb
    parameters:
      - location: during_stage_1
      - node: "UA-FSB-A-NODE"
      - planned_disconnect: 60.seconds
      - actual_disconnect: 50.seconds
      - verbose: 1
  - script: $CIP/csmart/lib/robustness/objs/deconfliction.rb
  - script: $CIP/csmart/lib/robustness/objs/printActions.rb

=end

require 'cougaar/scripting'
Cougaar::ExperimentDefinition.register(__FILE__)
