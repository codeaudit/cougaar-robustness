=begin experiment

name: ACUC7-Save-preStage2
group: Save
description: UR save Stage2 + ARUC7
script: $CIP/csmart/scripts/definitions/UR-BaselineTemplate-ExtOplan.rb
parameters:
  - run_count: 1
  - society_file: $CIP/csmart/config/societies/ua/full-tc20-avn-162a208v.plugins.rb
  - layout_file: $CIP/operator/layouts/UR-557-layout-1.xml
  - archive_dir: /mnt/archive
  
  - rules:
    - $CIP/csmart/config/rules/isat
    - $CIP/csmart/config/rules/yp
    - $CIP/csmart/config/rules/logistics
    - $CIP/csmart/config/rules/robustness
    - $CIP/csmart/config/rules/robustness/common
    - $CIP/csmart/config/rules/robustness/uc8
    - $CIP/csmart/config/rules/coordinator
#    - $CIP/csmart/config/rules/coordinator/examples/sample_defense
    - $CIP/csmart/config/rules/coordinator/test
    - $CIP/csmart/config/rules/robustness/uc1
#    - $CIP/csmart/config/rules/robustness/uc2
    - $CIP/csmart/config/rules/robustness/uc7
    - $CIP/csmart/config/rules/metrics/basic
    - $CIP/csmart/config/rules/metrics/sensors
    - $CIP/csmart/config/rules/metrics/serialization/metrics-only-serialization.rule
    - $CIP/csmart/config/rules/metrics/rss/tic
  - community_rules:
    - $CIP/csmart/config/rules/robustness/communities


include_scripts:
  - script: $CIP/csmart/lib/isat/clearPnLogs.rb
  - script: $CIP/csmart/lib/isat/initialize_network.rb
  - script: $CIP/csmart/lib/isat/save_snapshot.rb
    parameters:
      - snapshot_name: $CIP/SAVE-PreStage2-ARUC7.tgz
      - snapshot_location: before_stage_2
  - script: $CIP/csmart/lib/coordinator/leash_defenses.rb
    parameters:
      - location: before_stage_2
      - verbose: 1
  - script: $CIP/csmart/lib/coordinator/unleash_defenses.rb 
=end

require 'cougaar/scripting'
Cougaar::ExperimentDefinition.register(__FILE__)
