=begin experiment

name: ARUC1-Kill-Multiple-Nodes-With-Manager
description: AR UC1 - Multiple Node Kill with Manager
script: BaselineTemplateStage1.rb
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
  - script: $CIP/csmart/scripts/definitions/clearPnLogs.rb
  - script: $CIP/csmart/scripts/definitions/setup_robustness.rb
  - script: ua_kill_w_mgr.rb

=end

#CIP = ENV['CIP']
#$:.unshift File.join(CIP, 'csmart', 'acme_scripting', 'src', 'lib')
#$:.unshift File.join(CIP, 'csmart', 'acme_service', 'src', 'redist')

require 'cougaar/scripting'
Cougaar::ExperimentDefinition.register(__FILE__)
