=begin experiment

name: ACUC-2-AgentCompromiseModerate
description: MAU-sensitive Coordiantor response to AgentCompromise - Stop After Stage1
script: $CIP/csmart/scripts/definitions/UR-RestoreTemplate.rb
parameters:
  - run_count: 1
  - snapshot_name: $CIP/SAVE-PreStage5-ARUC7.tgz
  - archive_dir: $CIP/Logs
  - stages:
    - 5
  
include_scripts:
  - script: $CIP/csmart/lib/isat/clearPnLogs.rb
  - script: $CIP/csmart/lib/isat/initialize_network.rb
  - script: $CIP/csmart/lib/coordinator/leash_on_restart.rb
  - script: $CIP/csmart/lib/isat/stop_society.rb
    parameters:
      - stop_location: after_stage_5
  - script: $CIP/csmart/lib/logistics/al_data_compromise.rb
  - script: $CIP/csmart/lib/coordinator/mauHighSecurity.rb
    parameters:
      - location: before_stage_5
      - verbose: 2
=end

require 'cougaar/scripting'
Cougaar::ExperimentDefinition.register(__FILE__)
