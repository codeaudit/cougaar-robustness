=begin experiment

name: ACUC7-TimelyReturn-Restore-Stage2
group: Base
description: UR-Stage-2 Restore + ARUC7
script: $CIP/csmart/scripts/definitions/UR-RestoreTemplate.rb
parameters:
  - run_count: 1
  - snapshot_name: $CIP/SAVE-PreStage2-ARUC7.tgz
  - archive_dir: $CIP/Logs
  - stages:
    - 2
  
include_scripts:
  - script: $CIP/csmart/lib/isat/clearLogs.rb
  - script: $CIP/csmart/lib/isat/initialize_network.rb
  - script: $CIP/csmart/lib/coordinator/leash_on_restart.rb 
  - script: $CIP/csmart/lib/robustness/objs/planned_disconnect.rb
    parameters:
      - location: during_stage_2
      - wait_location: after_stage_2
      - nodes: ["UA-FSB-A-NODE", "UA-FSB-C-NODE"]
      - planned_disconnect: 12.minutes
      - actual_disconnect: 8.minutes
      - timeout: 30.minutes
      - verbose: 2
  - script: $CIP/csmart/lib/coordinator/unleash_defenses.rb 
    parameters:
      - location: during_stage_2
      - verbose: 1
  - script: $CIP/csmart/lib/coordinator/nodes_persisted_find_providers.rb 
    parameters:
      - location: during_stage_2
      - start_delay: 60
      - nodes: ["UA-FSB-A-NODE", "UA-FSB-C-NODE"]

=end

require 'cougaar/scripting'
Cougaar::ExperimentDefinition.register(__FILE__)
