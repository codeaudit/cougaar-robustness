=begin experiment

name: BaselineStage1
description: Baseline through Stage1
script: $CIP/csmart/scripts/definitions/BaselineTemplateStage1.rb
parameters:
  - run_count: 1
  - society_file: $CIP/csmart/config/societies/ua/full-tc20-232a703v.plugins.rb
  - layout_file: $CIP/operator/layouts/FULL-UA-TC20-35H41N-layout.xml
  - archive_dir: $CIP/Logs
  
  - rules:
    - $CIP/csmart/config/rules/isat
    - $CIP/csmart/config/rules/yp
    - $CIP/csmart/config/rules/logistics

include_scripts:
  - script: $CIP/csmart/scripts/definitions/clearPnLogs.rb

=end

require 'cougaar/scripting'
Cougaar::ExperimentDefinition.register(__FILE__)
