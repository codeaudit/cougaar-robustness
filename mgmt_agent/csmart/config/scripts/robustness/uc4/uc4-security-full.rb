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

Cougaar.new_experiment("UC4_Small_1AD_Tests").run(1) {

  do_action "LoadSocietyFromScript", "#{CIP}/csmart/config/societies/ad/SMALL-1AD-TRANS-1359.rb"
  do_action "LayoutSociety", "#{CIP}/operator/uc1-small-1ad-layout.xml", HOSTS_FILE

  do_action "TransformSociety", false,
    "#{RULES}/isat",
    "#{RULES}/logistics",
    "#{RULES}/robustness/uc1",
    "#{RULES}/robustness/uc4"

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

  wait_for "CommunitiesReady", ["1AD-REAR-COMM"]

  # Action "PublishThreatAlert" has 6 parameters:
  #   alert classname,
  #   community name,
  #   role in the community,
  #   alert level(select from: maximum, high, medium, low, minimum, undefined),
  #   alert duration, and
  #   assets
  assets = Hash[]
  do_action "PublishThreatAlert",
            "org.cougaar.tools.robustness.ma.SecurityAlert",
            "1AD-SMALL-COMM",
            "HealthMonitor",
            "medium",
            10.minutes,
            assets

  wait_for "CommunitiesReady", ["1AD-REAR-COMM"]

  #wait_for "Command", "ok"

  wait_for  "GLSConnection", false
  wait_for  "NextOPlanStage"
  do_action "Sleep", 30.seconds
  do_action "PublishNextStage"

  #wait_for  "PlanningComplete"  do
    wait_for  "Command", "shutdown"
    do_action "StopSociety"
    do_action "ArchiveLogs"
    do_action "StopCommunications"
  #end

}
