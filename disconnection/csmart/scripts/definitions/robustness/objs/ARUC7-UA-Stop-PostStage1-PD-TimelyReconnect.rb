=begin experiment

name: ARUC7-TimelyReturn 
description: Adaptive Robustness Use Case #7 (Planned Disconnect) (UA) Stop After Stage1
script: $CIP/csmart/scripts/definitions/UR-BaselineTemplate-ExtOplan.rb
parameters:
  - run_count: 1
  - society_file: $CIP/csmart/config/societies/ua/full-tc20-avn-162a208v.plugins.rb
  - layout_file: $CIP/operator/layouts/UR-557-layout-1.xml
  - archive_dir: $CIP/Logs
  
  - rules:
    - $CIP/csmart/config/rules/isat
    - $CIP/csmart/config/rules/yp
    - $CIP/csmart/config/rules/logistics
    - $CIP/csmart/config/rules/robustness
    - $CIP/csmart/config/rules/robustness/common
    - $CIP/csmart/config/rules/coordinator
#    - $CIP/csmart/config/rules/coordinator/examples/sample_defense
    - $CIP/csmart/config/rules/coordinator/test
    - $CIP/csmart/config/rules/robustness/uc1
#    - $CIP/csmart/config/rules/robustness/uc2
    - $CIP/csmart/config/rules/robustness/uc7
    - $CIP/csmart/config/rules/metrics/basic
    - $CIP/csmart/config/rules/metrics/sensors
    - $CIP/csmart/config/rules/metrics/serialization/metrics-only-serialization.rule
    - $CIP/csmart/config/rules/metrics/rss/tic
  - community_rules:
    - $CIP/csmart/config/rules/robustness/communities

include_scripts:
  - script: $CIP/csmart/lib/isat/clearPnLogs.rb
  - script: $CIP/csmart/lib/isat/initialize_network.rb
#  - script: $CIP/csmart/lib/isat/save_snapshot.rb
#    parameters:
#      - snapshot_name: $CIP/SAVE-Small-PreStage2-AR.tgz
#      - snapshot_location: before_stage_2
  - script: $CIP/csmart/lib/isat/stop_society.rb
    parameters:
      - stop_location: after_stage_1
#  - script: $CIP/csmart/lib/isat/wait_for_ok.rb
#    parameters:
#      - wait_for_location: after_stage_1
  - script: $CIP/csmart/lib/robustness/objs/planned_disconnect.rb
    parameters:
      - location: during_stage_1
      - start_delay: 60
      - wait_location: after_stage_1
      - nodes: ["UA-FSB-A-NODE", "UA-FSB-C-NODE"]
      - planned_disconnect: 12.minutes
      - actual_disconnect: 8.minutes
      - timeout: 30.minutes
      - verbose: 2
  - script: $CIP/csmart/lib/coordinator/unleash_defenses.rb 

=end

require 'cougaar/scripting'
Cougaar::ExperimentDefinition.register(__FILE__)
