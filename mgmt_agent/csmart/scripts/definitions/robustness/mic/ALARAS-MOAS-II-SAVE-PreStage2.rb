=begin experiment

name: ALARAS_MOAS-II-SAVE
description: ALARAS-MOAS-II SAVE pre-Stage2
script: $CIP/csmart/scripts/definitions/BaselineTemplate-ExtOplan.rb
parameters:
  - run_count: 1
  - society_file: $CIP/csmart/config/societies/ua/full-tc20-232a703v.plugins.rb
  - layout_file: $CIP/operator/layouts/FULL-UA-MNGR-33H63N-layout.xml
  - archive_dir: $CIP/Logs

  - rules:
    - $CIP/csmart/config/rules/isat
    - $CIP/csmart/config/rules/yp
    - $CIP/csmart/config/rules/logistics
    - $CIP/csmart/config/rules/assessment

    - $CIP/csmart/config/rules/robustness/manager.rule
    - $CIP/csmart/config/rules/coordinator
    - $CIP/csmart/config/rules/robustness/uc1
    - $CIP/csmart/config/rules/robustness/uc4
    - $CIP/csmart/config/rules/robustness/uc7
    #- $CIP/csmart/config/rules/robustness/uc9
    - $CIP/csmart/config/rules/robustness/UC3
    - $CIP/csmart/config/rules/metrics/basic
    - $CIP/csmart/config/rules/metrics/sensors
    - $CIP/csmart/config/rules/metrics/serialization/metrics-only-serialization.rule
    - $CIP/csmart/config/rules/metrics/rss/tic

# Security rules
    - $CIP/csmart/config/rules/security
    - $CIP/csmart/lib/security/rules
    - $CIP/csmart/config/rules/security/mop
    - $CIP/csmart/config/rules/security/testCollectData/ServiceContractPlugin.rule
    - $CIP/csmart/config/rules/security/robustness

  - community_rules:
    - $CIP/csmart/config/rules/security/communities
    - $CIP/csmart/config/rules/robustness/communities

include_scripts:
  - script: $CIP/csmart/lib/isat/clearPnLogs.rb
  -script: $CIP/csmart/lib/isat/initialize_network.rb

  # Security scripts
  - script: $CIP/csmart/lib/security/scripts/setup_scripting.rb
  - script: $CIP/csmart/lib/security/scripts/setup_userManagement.rb
    parameters:
      - user_mgr_label: wait_for_initialization
  - script: $CIP/csmart/lib/security/scripts/parseResults.rb
  - script: $CIP/csmart/lib/security/scripts/security_archives.rb
  - script: $CIP/csmart/lib/security/scripts/cleanup_society.rb
    parameters:
      - cleanup_label: transformed_society

  # Robustness scripts
  #- script: $CIP/csmart/lib/robustness/objs/deconfliction.rb
  - script: $CIP/csmart/lib/coordinator/unleash_defenses.rb

  - script: $CIP/csmart/lib/isat/save_snapshot.rb
    parameters:
      - snapshot_name: $CIP/SAVE-PreStage2.tgz
      - snapshot_location: before_stage_2

=end

require 'cougaar/scripting'
Cougaar::ExperimentDefinition.register(__FILE__)
