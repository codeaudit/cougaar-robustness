=begin experiment

name: MOAS-II-RESTORE-Stressed-KillsOnly-Stage2
group: nogroup
type: baseline
description: MOAS-II-Stage2-RESTORE_Kills
script: $CIP/csmart/scripts/definitions/RestoreTemplate.rb
parameters:
  - run_count: 1
  - snapshot_name: $CIP/SAVE-PreStage2.tgz
  - archive_dir: $CIP/Logs
  - stages:
    - 2

include_scripts:
  - script: $CIP/csmart/lib/isat/clearLogs.rb
  - script: $CIP/csmart/lib/isat/initialize_network.rb

  # Security scripts
  - script: $CIP/csmart/lib/security/scripts/setup_scripting.rb
  - script: $CIP/csmart/lib/security/scripts/setup_userManagement.rb
    parameters:
      - user_mgr_label: society_running
  - script: $CIP/csmart/lib/security/scripts/log_node_process_info.rb
  - script: $CIP/csmart/lib/security/scripts/parseResults.rb
  - script: $CIP/csmart/lib/security/scripts/security_archives.rb
  - script: $CIP/csmart/lib/security/scripts/cleanup_society.rb
    parameters:
      - cleanup_label: snapshot_restored

  # Robustness
  - script: $CIP/csmart/lib/robustness/mic/prepare_kills.rb
  - script: $CIP/csmart/lib/robustness/objs/deconfliction.rb
  - script: $CIP/csmart/lib/coordinator/unleash_defenses.rb
  - script: $CIP/csmart/lib/isat/standard_kill_nodes.rb
    parameters:
      - start_tag: starting_stage
      - start_delay: 60
      - nodes_to_kill:
        - ConusTRANSCOM-NODE
        - REAR-ROB-MGMT-NODE
        - REAR-B-NODE
        - 1-AD-BDES-NODE
        - UA-B-NODE
        - AVN-DET-B-NODE
        - NLOS-BN-A-NODE
        - 1-CA-BN-A-NODE
        - 1-CA-BN-C-NODE
        - 1-CA-BN-E-NODE
        - 2-CA-BN-A-NODE
        - 2-CA-BN-E-NODE
        - 3-CA-BN-D-NODE
        - 123-MSB-NODE

=end

require 'cougaar/scripting'
Cougaar::ExperimentDefinition.register(__FILE__)
