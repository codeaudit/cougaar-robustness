=begin experiment

name: TestLongDisconnects
description: A test for WP and MTS recovery from long disconnects
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
#    - $CIP/csmart/config/rules/robustness/common
#    - $CIP/csmart/config/rules/robustness/uc8
    - $CIP/csmart/config/rules/metrics/basic
    - $CIP/csmart/config/rules/metrics/sensors
    - $CIP/csmart/config/rules/metrics/serialization/metrics-only-serialization.rule
    - $CIP/csmart/config/rules/metrics/rss/tic
    - $CIP/csmart/config/rules/robustness/debug_rules/messageTrace.rule

include_scripts:
  - script: $CIP/csmart/lib/isat/clearPnLogs.rb
  - script: $CIP/csmart/lib/isat/initialize_network.rb
  - script: $CIP/csmart/lib/robustness/objs/test_disconnect.rb
    parameters:
      - location: during_stage_1
      - start_delay: 60
      - nodes: ["UA-FSB-A-NODE", "UA-FSB-C-NODE"]
      - actual_disconnect: 8.minutes
      - timeout: 30.minutes
      - verbose: 3
  - script: $CIP/csmart/lib/isat/stop_society.rb
    parameters:
      - stop_location: after_stage_1
  - script: $CIP/csmart/lib/isat/wait_for_ok.rb
    parameters:
      - wait_for_location: after_stage_1

=end

require 'cougaar/scripting'
Cougaar::ExperimentDefinition.register(__FILE__)
