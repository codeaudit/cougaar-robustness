 #  Copyright 2003-2004 Object Services and Consulting, Inc.
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

name: ACUC7-TimelyReturn-Restore-Stage2
group: Base
description: UR-Stage-2 Restore + ARUC7
script: $CIP/csmart/scripts/definitions/UR-RestoreTemplate.rb
parameters:
  - run_count: 1
  - snapshot_name: $CIP/SAVE-PreStage2-ARUC7.tgz
  - archive_dir: $CIP/Logs
  - stages:
    - 2
  
include_scripts:
  - script: $CIP/csmart/lib/isat/clearLogs.rb
  - script: $CIP/csmart/lib/isat/initialize_network.rb
  - script: $CIP/csmart/lib/coordinator/leash_on_restart.rb 
  - script: $CIP/csmart/lib/robustness/objs/planned_disconnect.rb
    parameters:
      - location: during_stage_2
      - wait_location: after_stage_2
      - nodes: ["UA-FSB-A-NODE", "UA-FSB-C-NODE"]
      - planned_disconnect: 12.minutes
      - actual_disconnect: 8.minutes
      - timeout: 30.minutes
      - verbose: 2
  - script: $CIP/csmart/lib/coordinator/unleash_defenses.rb 
    parameters:
      - location: during_stage_2
      - verbose: 1
  - script: $CIP/csmart/lib/coordinator/nodes_persisted_find_providers.rb 
    parameters:
      - location: during_stage_2
      - start_delay: 60
      - nodes: ["UA-FSB-A-NODE", "UA-FSB-C-NODE"]

=end

require 'cougaar/scripting'
Cougaar::ExperimentDefinition.register(__FILE__)
