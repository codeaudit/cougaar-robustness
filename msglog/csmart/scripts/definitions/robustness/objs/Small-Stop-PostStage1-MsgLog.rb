=begin experiment

name: Stop-Small-postStage1-MsgLog 
description: Stop Small postStage1 MsgLog
script: $CIP/csmart/scripts/definitions/BaselineTemplate.rb
parameters:
  - run_count: 1
  - society_file: $CIP/csmart/config/societies/ad/SMALL-1AD-TC20.rb
  - layout_file: $CIP/operator/layouts/SMALL-1AD-TC20-MGRS-layout.xml
  - archive_dir: $CIP/Logs
  
  - rules:
    - $CIP/csmart/config/rules/isat
    - $CIP/csmart/config/rules/yp
    - $CIP/csmart/config/rules/logistics
    - $CIP/csmart/config/rules/robustness
#    - $CIP/csmart/config/rules/coordinator
#    - $CIP/csmart/config/rules/coordinator/examples/sample_defense
#    - $CIP/csmart/config/rules/coordinator/test
#    - $CIP/csmart/config/rules/robustness/uc1
    - $CIP/csmart/config/rules/robustness/uc2/base_msglog.rule
    - $CIP/csmart/config/rules/robustness/uc2/email.rule
#    - $CIP/csmart/config/rules/robustness/uc7
    - $CIP/csmart/config/rules/robustness/uc9
    - $CIP/csmart/config/rules/metrics/basic
    - $CIP/csmart/config/rules/metrics/sensors
    - $CIP/csmart/config/rules/metrics/serialization/metrics-only-serialization.rule
    - $CIP/csmart/config/rules/metrics/rss/tic
  - community_rules:
    - $CIP/csmart/config/rules/robustness/communities

include_scripts:
  - script: $CIP/csmart/lib/isat/clearPnLogs.rb
  - script: $CIP/csmart/lib/robustness/objs/deconfliction.rb 
#  - script: $CIP/csmart/lib/robustness/objs/disconnect.rb
#    parameters:
#      - location: during_stage_1
#      - nodes: ["FWD-NODE"]
#      - planned_disconnect: 12.minutes
#      - actual_disconnect: 8.minutes
#      - verbose: 1
#  - script: $CIP/csmart/lib/isat/save_snapshot.rb
#    parameters:
#      - snapshot_name: $CIP/SAVE-Small-PreStage2-AR.tgz
#      - snapshot_location: before_stage_2
  - script: $CIP/csmart/lib/isat/stop_society.rb
    parameters:
      - stop_location: after_stage_1
  - script: $CIP/csmart/lib/isat/wait_for_ok.rb
    parameters:
      - wait_for_location: after_stage_1

=end

require 'cougaar/scripting'
Cougaar::ExperimentDefinition.register(__FILE__)
