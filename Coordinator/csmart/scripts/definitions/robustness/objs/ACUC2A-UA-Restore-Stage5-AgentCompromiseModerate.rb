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

name: ACUC-2-AgentCompromiseModerate
description: MAU-sensitive Coordiantor response to AgentCompromise - Stop After Stage1
script: $CIP/csmart/scripts/definitions/UR-RestoreTemplate.rb
parameters:
  - run_count: 1
  - snapshot_name: $CIP/SAVE-PreStage5-ARUC7.tgz
  - archive_dir: $CIP/Logs
  - stages:
    - 5
  
include_scripts:
  - script: $CIP/csmart/lib/isat/clearPnLogs.rb
  - script: $CIP/csmart/lib/isat/initialize_network.rb
  - script: $CIP/csmart/lib/coordinator/leash_on_restart.rb
  - script: $CIP/csmart/lib/isat/stop_society.rb
    parameters:
      - stop_location: after_stage_5
  - script: $CIP/csmart/lib/logistics/al_data_compromise.rb
  - script: $CIP/csmart/lib/coordinator/mauHighSecurity.rb
    parameters:
      - location: before_stage_5
      - verbose: 2
=end

require 'cougaar/scripting'
Cougaar::ExperimentDefinition.register(__FILE__)
