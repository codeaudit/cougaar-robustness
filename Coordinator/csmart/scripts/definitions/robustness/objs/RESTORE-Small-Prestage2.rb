=begin experiment

name: Restore-Small-preStage2
description: Restore Small preStage2
script: RestoreTemplate.rb
parameters:
  - run_count: 1
  - snapshot_name: $CIP/SAVE-Small-PreStage2.tgz
  - archive_dir: $CIP/Logs
  - stages:
    - 2
  
include_scripts:
  - script: $CIP/csmart/lib/isat/clearLogs.rb
    parameters:
      - run_type: base
      - description: Stage 56 Baseline
  - script: $CIP/csmart/assessment/assess/analysis_baseline_cmds.rb
    parameters:
      - only_analyze: "moe1"
      - baseline_name: base2

=end

require 'cougaar/scripting'
Cougaar::ExperimentDefinition.register(__FILE__)
