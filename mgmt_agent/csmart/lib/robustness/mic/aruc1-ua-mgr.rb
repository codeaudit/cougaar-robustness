=begin experiment

name: ARUC1-Kill-Manager-Node
description: AR UC1 - Manager Node Kill
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
    - $CIP/csmart/config/rules/robustness/manager.rule
    - $CIP/csmart/config/rules/robustness/uc1
    - $CIP/csmart/config/rules/robustness/uc9
  - community_rules:
    - $CIP/csmart/config/rules/robustness/communities/community.rule

include_scripts:
  - script: $CIP/csmart/lib/isat/clearPnLogs.rb
  - script: $CIP/csmart/lib/robustness/objs/deconfliction.rb
  - script: ua_kill_mgr_s.rb

  - script: $CIP/csmart/lib/isat/stop_society.rb
    parameters:
      - stop_location: before_stage_2

=end

require 'cougaar/scripting'
Cougaar::ExperimentDefinition.register(__FILE__)
