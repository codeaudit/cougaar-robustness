=begin experiment

name: ARUC8
description: Mobile Hosts (ARUC8) Test
script: $CIP/csmart/scripts/definitions/UR-BaselineTemplate-ExtOplan.rb
parameters:
  - run_count: 1
  - society_file: $CIP/csmart/config/societies/ua/full-tc20-avn-162a208v.plugins.rb
  - layout_file: $CIP/operator/layouts/UR-557-layout-1.xml
  - archive_dir: $CIP/Logs
  
  - rules:
    - $CIP/csmart/config/rules/isat
    - $CIP/csmart/config/rules/yp
    - $CIP/csmart/config/rules/logistics
    - $CIP/csmart/config/rules/robustness/uc8

include_scripts:
  - script: $CIP/csmart/lib/isat/clearPnLogs.rb
#  - script: $CIP/csmart/lib/isat/datagrabber_include.rb
  - script: $CIP/csmart/lib/isat/initialize_network.rb
  - script: $CIP/csmart/lib/robustness/objs/monitor_mobile_hosts.rb
  - script: $CIP/csmart/lib/isat/migrate.rb
    parameters:
    - migrate_location: during_stage_1
    - node_name: AVN-CO-NODE
    - target_network: 1-UA
  - script: $CIP/csmart/lib/isat/stop_society.rb
    parameters:
      - stop_location: after_stage_1
  - script: $CIP/csmart/lib/isat/wait_for_ok.rb
    parameters:
    - wait_for_location: after_stage_1

=end

require 'cougaar/scripting'
Cougaar::ExperimentDefinition.register(__FILE__)
