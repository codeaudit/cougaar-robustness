=begin experiment

name: MOAS-II-ARUC7-LateReturn-Stop-PostStage1
group: Save
description: Adaptive Robustness Use Case #7 (Planned Disconnect) (UA) Stop After Stage1
script: $CIP/csmart/scripts/definitions/UR-BaselineTemplate-ExtOplan.rb
parameters:
  - run_count: 1
  - society_file: $CIP/csmart/config/societies/ua/full-tc20-avn-234a703v.plugins.rb
  - layout_file: $CIP/operator/layouts/UR-OP-layout.xml
  - archive_dir: /mnt/archive
  
  - rules:
    - $CIP/csmart/config/rules/isat
#    - $CIP/csmart/config/rules/isat/debug/suicideDump.rule
#    - $CIP/csmart/config/rules/isat/debug/mts-bigMessage.rule
    - $CIP/csmart/config/rules/yp
    - $CIP/csmart/config/rules/logistics
    - $CIP/csmart/config/rules/logistics-predictors
    - $CIP/csmart/config/rules/assessment
    - $CIP/csmart/config/rules/metrics/basic
    - $CIP/csmart/config/rules/metrics/sensors
    - $CIP/csmart/config/rules/metrics/serialization/metrics-only-serialization.rule
    - $CIP/csmart/config/rules/metrics/rss/tic
    - $CIP/csmart/config/rules/robustness/manager.rule
    - $CIP/csmart/config/rules/robustness/common
    - $CIP/csmart/config/rules/coordinator
#    - $CIP/csmart/config/rules/coordinator/test
    - $CIP/csmart/config/rules/robustness/uc1
    - $CIP/csmart/config/rules/robustness/UC3
    - $CIP/csmart/config/rules/robustness/uc7
#    - $CIP/csmart/config/rules/isat/uc3_nosec
    - $CIP/csmart/config/rules/robustness/uc8
    - $CIP/csmart/config/rules/robustness/debug_rules/queueViewServlet.rule
############################################################################
##   SECURITY RULES
    - $CIP/csmart/config/rules/security
#    - $CIP/csmart/config/rules/security/testCollectData/MessageReaderAspect.rule
#    - $CIP/csmart/config/rules/security/testCollectData/ServiceContractPlugin.rule

#    - $CIP/csmart/config/rules/security/mts/loopback_protocol.rule
#    - $CIP/csmart/config/rules/security/mts/http_mts.rule
#    - $CIP/csmart/config/rules/security/mts/https_mts.rule
#    - $CIP/csmart/config/rules/security/mts/sslRMI.rule
#    - $CIP/csmart/config/rules/security/naming


#    - $CIP/csmart/config/rules/security/ruleset/base
#    - $CIP/csmart/config/rules/security/ruleset/crypto
#    - $CIP/csmart/config/rules/security/ruleset/jaas
#    - $CIP/csmart/config/rules/security/ruleset/accesscontrol
#    - $CIP/csmart/config/rules/security/ruleset/misc
#    - $CIP/csmart/config/rules/security/ruleset/monitoring
#    - $CIP/csmart/config/rules/security/ruleset/debug
#    - $CIP/csmart/config/rules/security/ruleset/signConfig

    - $CIP/csmart/lib/security/rules

#    - $CIP/csmart/config/rules/security/mop
#    - $CIP/csmart/config/rules/security/testCollectData
   # ###
   # Redundant CA and persistence managers
#    - $CIP/csmart/config/rules/security/redundancy
#    - $CIP/csmart/config/rules/security/robustness
    - $CIP/csmart/config/rules/security/robustness
   # Run with only redundant PM
#    - $CIP/csmart/config/rules/security/redundancy/add_redundant_pm_facet.rule
#    - $CIP/csmart/config/rules/security/redundancy/adjust_memory.rule
#    - $CIP/csmart/config/rules/security/robustness/redundant_persistence_mgrs.rule
#####################################################################################

  - community_rules:
    - $CIP/csmart/config/rules/security/communities
    - $CIP/csmart/config/rules/robustness/communities

include_scripts:
  - script: $CIP/csmart/lib/isat/clearPnLogs.rb
#  - script: $CIP/csmart/lib/coordinator/unleash_defenses.rb
  - script: $CIP/csmart/lib/isat/sms_notify.rb
  - script: $CIP/csmart/lib/isat/initialize_network.rb
  - script: $CIP/csmart/lib/isat/network_shaping.rb
#  - script: $CIP/csmart/lib/isat/klink_reporting.rb
  - script: $CIP/csmart/lib/isat/datagrabber_include.rb

  - script: $CIP/csmart/lib/robustness/bbn/scripting.rb
  - script: $CIP/csmart/lib/robustness/bbn/make-rss-files.rb

################################################################
## SECURITY SCRIPTS
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
#################################################################

  - script: $CIP/csmart/lib/isat/stop_society.rb
    parameters:
      - stop_location: after_stage_1

  - script: $CIP/csmart/lib/robustness/objs/planned_disconnect.rb
    parameters:
      - location: during_stage_1
      - wait_location: after_stage_1
      - nodes: ["FSB-CO-HQ-CIC-NODE", "FSB-DISTRO-FWD-EVAC-NODE"]
      - planned_disconnect: 4.minutes
      - actual_disconnect: 8.minutes
      - timeout: 30.minutes
      - verbose: 2

  - script: $CIP/csmart/lib/coordinator/unleash_defenses.rb 
    parameters:
      - location: during_stage_1
      - verbose: 1

  - script: $CIP/csmart/lib/coordinator/nodes_persisted_find_providers.rb 
    parameters:
      - location: during_stage_1
      - start_delay: 60
      - nodes: ["FSB-CO-HQ-CIC-NODE", "FSB-DISTRO-FWD-EVAC-NODE"]

=end

require 'cougaar/scripting'
Cougaar::ExperimentDefinition.register(__FILE__)
