=begin experiment

name: dup-test
group: Save
description: dup-test
script: $CIP/csmart/scripts/definitions/BaselineTemplate-ExtOplan.rb
parameters:
  - run_count: 1
  - society_file: $CIP/csmart/config/societies/ua/full-tc20-avn-234a703v.plugins.rb
  - layout_file: $CIP/operator/layouts/UR-OP-layout.xml
  - archive_dir: /mnt/archive
  
  - rules:
    - $CIP/csmart/config/rules/isat
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
    - $CIP/csmart/config/rules/robustness/uc1/aruc1.rule
    - $CIP/csmart/config/rules/robustness/uc8
    - $CIP/csmart/config/rules/robustness/debug_rules/queueViewServlet.rule
    - $CIP/csmart/config/rules/robustness/debug_rules/incarnation.rule

  - community_rules:
#    - $CIP/csmart/config/rules/security/communities
    - $CIP/csmart/config/rules/robustness/communities

include_scripts:
  - script: $CIP/csmart/lib/isat/clearPnLogs.rb
  - script: $CIP/csmart/lib/isat/sms_notify.rb
  - script: $CIP/csmart/lib/isat/initialize_network.rb
  - script: $CIP/csmart/lib/isat/network_shaping.rb
#  - script: $CIP/csmart/lib/isat/klink_reporting.rb
  - script: $CIP/csmart/lib/isat/datagrabber_include.rb
  - script: $CIP/csmart/lib/robustness/mic/create_BDE_duplicate_agents.rb

  - script: $CIP/csmart/lib/isat/stop_society.rb
    parameters:
      - stop_location: before_stage_2

=end

require 'cougaar/scripting'
Cougaar::ExperimentDefinition.register(__FILE__)
