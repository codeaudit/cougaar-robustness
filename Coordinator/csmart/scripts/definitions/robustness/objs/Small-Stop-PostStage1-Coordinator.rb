 #  Copyright 2004 Object Services and Consulting, Inc.
 #  under sponsorship of the Defense Advanced Research Projects
 #  Agency (DARPA).
 #
 #  You can redistribute this software and/or modify it under the
 #  terms of the Cougaar Open Source License as published on the
 #  Cougaar Open Source Website (www.cougaar.org).
 #
 #  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 #  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 #  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 #  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 #  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 #  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 #  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 #  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 #  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 #  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 #  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


=begin experiment

name: Stop-Small-postStage1-Coordinator 
description: Stop Small postStage1 Coordinator
script: $CIP/csmart/scripts/definitions/BaselineTemplate.rb
parameters:
  - run_count: 1
  - society_file: $CIP/csmart/config/societies/ad/SMALL-1AD-TC20.rb
  - layout_file: $CIP/operator/layouts/SMALL-1AD-TC20-MGRS-layout.xml
  - archive_dir: $CIP/Logs
  
  - rules:
    - $CIP/csmart/config/rules/isat
    - $CIP/csmart/config/rules/yp
    - $CIP/csmart/config/rules/logistics
    - $CIP/csmart/config/rules/robustness
    - $CIP/csmart/config/rules/coordinator
    - $CIP/csmart/config/rules/coordinator/examples/sample_defense
    - $CIP/csmart/config/rules/coordinator/test
    - $CIP/csmart/config/rules/robustness/uc1
#    - $CIP/csmart/config/rules/robustness/uc7
    - $CIP/csmart/config/rules/robustness/uc9
    - $CIP/csmart/config/rules/metrics/basic
    - $CIP/csmart/config/rules/metrics/sensors
    - $CIP/csmart/config/rules/metrics/serialization/metrics-only-serialization.rule
    - $CIP/csmart/config/rules/metrics/rss/tic
  - community_rules:
    - $CIP/csmart/config/rules/robustness/communities

include_scripts:
  - script: $CIP/csmart/lib/isat/clearPnLogs.rb
  - script: $CIP/csmart/lib/robustness/objs/deconfliction.rb 
  - script: $CIP/csmart/lib/coordinator/unleash_defenses.rb 
#  - script: $CIP/csmart/lib/robustness/objs/disconnect.rb
#    parameters:
#      - location: during_stage_1
#      - nodes: ["FWD-NODE"]
#      - planned_disconnect: 12.minutes
#      - actual_disconnect: 8.minutes
#      - verbose: 1
#  - script: $CIP/csmart/lib/isat/save_snapshot.rb
#    parameters:
#      - snapshot_name: $CIP/SAVE-Small-PreStage2-AR.tgz
#      - snapshot_location: before_stage_2
  - script: $CIP/csmart/lib/isat/stop_society.rb
    parameters:
      - stop_location: after_stage_1
  - script: $CIP/csmart/lib/isat/wait_for_ok.rb
    parameters:
      - wait_for_location: after_stage_1

=end

require 'cougaar/scripting'
Cougaar::ExperimentDefinition.register(__FILE__)
