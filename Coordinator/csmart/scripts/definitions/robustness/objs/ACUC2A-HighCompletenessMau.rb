=begin experiment

name: ACUC2A-HighCompletenessyMau
group: Stress
description: ACUC2A
script: $CIP/csmart/scripts/definitions/UR-RestoreTemplate.rb
parameters:
  - run_count: 1
  - snapshot_name: $CIP/SAVE-PreStage4.tgz
  - archive_dir: /mnt/archive
  - stages:
    - 4
  
include_scripts:
  - script: $CIP/csmart/lib/isat/clearLogs.rb
  - script: $CIP/csmart/lib/isat/sms_notify.rb
  - script: $CIP/csmart/assessment/assess/setup_scripting.rb
  - script: $CIP/csmart/assessment/assess/asmt_init_network.rb
  - script: $CIP/csmart/lib/isat/initialize_network.rb
  - script: $CIP/csmart/lib/isat/network_shaping.rb
#  - script: $CIP/csmart/lib/isat/klink_reporting.rb
  - script: $CIP/csmart/lib/isat/datagrabber_include.rb
  - script: $CIP/csmart/assessment/assess/inbound_aggagent_parallel_include.rb
  - script: $CIP/csmart/assessment/assess/outofbound_aggagent_include.rb
  - script: $CIP/csmart/lib/coordinator/leash_on_restart.rb
  - script: $CIP/csmart/lib/robustness/mic/freeze.rb

# ############################################################
# Security scripts
  - script: $CIP/csmart/lib/security/scripts/setup_scripting.rb
  - script: $CIP/csmart/lib/security/scripts/build_config_jarfiles.rb
  - script: $CIP/csmart/lib/security/scripts/build_policies.rb
#  - script: $CIP/csmart/lib/security/scripts/setup_userManagement.rb
  - script: $CIP/csmart/lib/security/scripts/security_archives.rb
  - script: $CIP/csmart/lib/security/scripts/saveAcmeEvents.rb
  - script: $CIP/csmart/lib/security/scripts/log_node_process_info.rb
#  - script: $CIP/csmart/lib/security/scripts/setup_society_1000_ua.rb
  - script: $CIP/csmart/lib/security/scripts/check_wp.rb
  - script: $CIP/csmart/lib/security/scripts/check_report_chain_ready.rb
  - script: $CIP/csmart/lib/security/scripts/cleanup_society.rb

   # Agent Compromise
  - script: $CIP/csmart/lib/logistics/al_data_compromise.rb
  - script: $CIP/csmart/lib/coordinator/mauHighCompleteness.rb
    parameters:
      - location: during_stage_5
      - verbose: 2
  - script: $CIP/csmart/lib/coordinator/unleash_defenses.rb 
    parameters:
      - location: starting_stage
      - verbose: 1

=end

require 'cougaar/scripting'
Cougaar::ExperimentDefinition.register(__FILE__)
