CIP = ENV['CIP']
RULES = File.join(CIP, 'csmart','config','rules')

$:.unshift File.join(CIP, 'csmart', 'acme_scripting', 'src', 'lib')
$:.unshift File.join(CIP, 'csmart', 'acme_service', 'src', 'redist')
$:.unshift File.join(CIP, 'csmart', 'config', 'lib')

require 'cougaar/scripting'
require 'ultralog/scripting'
require 'robustness/uc1/aruc1_actions_and_states'
require 'robustness/uc4/aruc4_actions_and_states'
require 'robustness/uc1/deconfliction'

HOSTS_FILE = Ultralog::OperatorUtils::HostManager.new.get_hosts_file

Cougaar::ExperimentMonitor.enable_stdout
Cougaar::ExperimentMonitor.enable_logging

Cougaar.new_experiment("ARUC4_ThreatCon").run(1) {

  do_action "LoadSocietyFromScript", "#{CIP}/csmart/config/societies/ua/full-160a237v.plugins.rb"
  do_action "LayoutSociety", "#{CIP}/operator/Full-UA-21H51N537A-NoG-layout.xml", HOSTS_FILE

  do_action "TransformSociety", false,
    "#{RULES}/isat",
    "#{RULES}/logistics",
    "#{RULES}/robustness",
    "#{RULES}/robustness/uc1",
    "#{RULES}/robustness/uc4",
    "#{RULES}/metrics/basic",
    "#{RULES}/metrics/sensors"

  do_action "TransformSociety", false, "#{RULES}/robustness/communities"

  # for debugging
  #do_action "SaveCurrentSociety", "mySociety.xml"
  #do_action "SaveCurrentCommunities", "myCommunities.xml"

  do_action "StartJabberCommunications"
  do_action "VerifyHosts"

  do_action "DeployCommunitiesFile"

  do_action "DisableDeconfliction"

  do_action "CleanupSociety"

  do_action "ConnectOperatorService"
  do_action "ClearPersistenceAndLogs"
  do_action "StartSociety"

  ## Print events from Robustness Controller
  do_action "GenericAction" do |run|
    run.comms.on_cougaar_event do |event|
      if event.component.include?("RobustnessController")
        puts event
      end
    end
  end

  wait_for "CommunitiesReady", ["CONUS-REAR-COMM"]

  do_action "PublishInterAgentOperatingMode",
            "CONUS-REAR-ARManager",
            "HIGH"

  do_action "Sleep", 1.minutes

  do_action "PublishInterAgentOperatingMode",
            "CONUS-REAR-ARManager",
            "LOW"

  wait_for  "GLSConnection", true
  wait_for  "NextOPlanStage"
  do_action "Sleep", 30.seconds
  do_action "PublishNextStage"

  wait_for  "SocietyQuiesced"  do
    wait_for  "Command", "shutdown"
    do_action "SaveSocietyCompletion", "completion_#{experiment.name}.xml"
    #include "inventory.inc", "RunSoc"
    do_action "StopSociety"
    do_action "ArchiveLogs"
    do_action "StopCommunications"
  end

  wait_for "Command", "shutdown"
  do_action "Sleep", 30.seconds
  do_action "SaveSocietyCompletion", "completion_#{experiment.name}.xml"
  #include "inventory.inc", "RunSoc"
  do_action "Sleep", 30.seconds
  do_action "StopSociety"
  do_action "ArchiveLogs"
  do_action "StopCommunications"

}
