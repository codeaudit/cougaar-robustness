CIP = ENV['CIP']
RULES = File.join(CIP, 'csmart','config','rules')

$:.unshift File.join(CIP, 'csmart', 'acme_scripting', 'src', 'lib')
$:.unshift File.join(CIP, 'csmart', 'acme_service', 'src', 'redist')
$:.unshift File.join(CIP, 'csmart', 'config', 'lib')

require 'cougaar/scripting'
require 'ultralog/scripting'
require 'robustness/uc1/aruc1_actions_and_states'
require 'robustness/uc2/msglog'
require 'robustness/uc2/flushmail'

HOSTS_FILE = Ultralog::OperatorUtils::HostManager.new.get_hosts_file

Cougaar::ExperimentMonitor.enable_stdout
Cougaar::ExperimentMonitor.enable_logging

Cougaar.new_experiment("UC1-Steve").run(1) {

  do_action "LoadSocietyFromScript", "#{CIP}/configs/ul/FULL-1AD-TRANS-1359.rb"
  do_action "LayoutSociety", "#{CIP}/operator/1ad-layout-10_4_1.xml", HOSTS_FILE

  do_action "TransformSociety", false, 
    "#{RULES}/isat",
    "#{RULES}/logistics",
    "#{RULES}/robustness/uc1",
    "#{RULES}/robustness/uc2/msglog.rule"
  do_action "SaveCurrentSociety", "mySociety.xml"
  do_action "StartJabberCommunications"
  do_action "VerifyHosts"

  do_action "CleanupSociety"

  do_action "ConnectOperatorService"
  do_action "ClearPersistenceAndLogs"

  do_action "FlushMail" 

  do_action "KeepSocietySynchronized" 
  do_action "StartSociety"

  wait_for  "GLSConnection", true
  wait_for  "NextOPlanStage"
  do_action "Sleep", 30.seconds
  do_action "PublishNextStage"

  do_action "Sleep", 8.minutes
  #do_action "KillNodes", "REAR-D"
  do_action "KillNodes", "FWD-B"

  wait_for  "PlanningComplete"  do
    wait_for  "Command", "shutdown"
    do_action "SaveSocietyCompletion", "completion_#{experiment.name}.xml"
    do_action "StopSociety"
    do_action "ArchiveLogs"
    do_action "StopCommunications"
  end

  wait_for "Command", "shutdown"
  do_action "Sleep", 30.seconds
  do_action "SaveSocietyCompletion", "completion_#{experiment.name}.xml"
#stop to "P" in case you want to rehydrate"
  #do_action "Sleep", 10.minutes
  do_action "StopSociety"
  do_action "ArchiveLogs"
  do_action "StopCommunications"
}
