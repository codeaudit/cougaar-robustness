=begin experiment

name: ARUC1-Kill-Multiple-Nodes
description: AR UC1 - Multiple Node Kill
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

    # Robustness
    - $CIP/csmart/config/rules/robustness/manager.rule
    - $CIP/csmart/config/rules/robustness/uc1
    - $CIP/csmart/config/rules/robustness/uc9
    - $CIP/csmart/config/rules/security

    # Security
    - $CIP/csmart/lib/security/rules
    - $CIP/csmart/config/rules/security/mop
    - $CIP/csmart/config/rules/security/testCollectData/ServiceContractPlugin.rule
    - $CIP/csmart/config/rules/security/robustness
    - $CIP/csmart/config/rules/security/redundancy

  - community_rules:
    - $CIP/csmart/config/rules/robustness/communities/community.rule
    - $CIP/csmart/config/rules/security/communities

include_scripts:
  - script: $CIP/csmart/lib/isat/clearPnLogs.rb
  - script: $CIP/csmart/lib/robustness/objs/deconfliction.rb
  - script: ua_kill_s.rb

  # Security scripts
  - script: $CIP/csmart/lib/security/scripts/setup_scripting.rb
  - script: $CIP/csmart/lib/security/scripts/setup_userManagementSAVE.rb
  - script: $CIP/csmart/lib/security/scripts/log_node_process_info.rb
  - script: $CIP/csmart/lib/security/scripts/check_wp.rb
  - script: $CIP/csmart/lib/security/scripts/parseResults.rb
  - script: $CIP/csmart/lib/security/scripts/saveAcmeEvents.rb
  - script: $CIP/csmart/lib/security/scripts/security_archives.rb
  - script: $CIP/csmart/lib/security/scripts/cleanup_society.rb
                                                                                
  - script: $CIP/csmart/lib/isat/stop_society.rb
    parameters:
      - stop_location: before_stage_2

=end

require 'cougaar/scripting'
Cougaar::ExperimentDefinition.register(__FILE__)
