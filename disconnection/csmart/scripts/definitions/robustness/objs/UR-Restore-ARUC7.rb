=begin experiment

name: UR-Stage-4-ARUC7
group: Base
description: UR-Stage-4 + ARUC7
script: $CIP/csmart/scripts/definitions/UR-RestoreTemplate.rb
parameters:
  - run_count: 1
  - snapshot_name: $CIP/SAVE-PreStage4.tgz
  - archive_dir: $CIP/Logs
  - stages:
    - 4
  
include_scripts:
  - script: $CIP/csmart/lib/isat/clearLogs.rb
  - script: $CIP/csmart/lib/isat/initialize_network.rb

  - script: $CIP/csmart/lib/robustness/objs/planned_disconnect.rb
    parameters:
      - location: during_stage_4
      - wait_location: after_stage_4
      - nodes: ["UA-FSB-A-NODE", "UA-FSB-C-NODE"]
      - planned_disconnect: 12.minutes
      - actual_disconnect: 8.minutes
      - timeout: 30.minutes
      - verbose: 2
  - script: $CIP/csmart/lib/coordinator/leash_on_restart.rb 

=end

require 'cougaar/scripting'
Cougaar::ExperimentDefinition.register(__FILE__)
