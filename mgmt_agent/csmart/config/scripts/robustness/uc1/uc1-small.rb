CIP = ENV['CIP']
RULES = File.join(CIP, 'csmart','config','rules')

$:.unshift File.join(CIP, 'csmart', 'acme_scripting', 'src', 'lib')
$:.unshift File.join(CIP, 'csmart', 'acme_service', 'src', 'redist')
$:.unshift File.join(CIP, 'csmart', 'config', 'lib')

require 'cougaar/scripting'
require 'ultralog/scripting'
require 'robustness/uc1/aruc1_actions_and_states'
require 'robustness/uc1/deconfliction'

HOSTS_FILE = Ultralog::OperatorUtils::HostManager.new.get_hosts_file

Cougaar::ExperimentMonitor.enable_stdout
Cougaar::ExperimentMonitor.enable_logging

Cougaar.new_experiment("UC1_Small_1AD_Tests").run(1) {

  do_action "LoadSocietyFromScript", "#{CIP}/csmart/config/societies/ad/SMALL-1AD-TRANS-1359.rb"
  do_action "LayoutSociety", "#{CIP}/operator/uc1-small-1ad-layout.xml", HOSTS_FILE

  do_action "TransformSociety", false,
    "#{RULES}/isat",
    "#{RULES}/logistics",
    "#{RULES}/robustness"
    "#{RULES}/robustness/uc1"

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

  # After CommunityReady event is received wait for persistence
  wait_for "CommunitiesReady", ["1AD-SMALL-COMM"]
  do_action "Sleep", 3.minutes

  # Kill node that does not contain robustness manager
  do_action "SaveHostOfNode", "1AD-SMALL-COMM", "TRANS-NODE"
  do_action "KillNodes", "TRANS-NODE"

  # Wait for restarts to complete
  wait_for "CommunitiesReady", ["1AD-SMALL-COMM"]

  # Add an empty node to community
  do_action "AddNode", "NewNode1", "1AD-SMALL-COMM"
  do_action "Sleep", 3.minutes

  # Kill node that contains robustness manager
  do_action "SaveHostOfNode", "1AD-SMALL-COMM", "FWD-NODE"
  do_action "KillNodes", "FWD-NODE"

  # Wait for restarts to complete
  wait_for "CommunitiesReady", ["1AD-SMALL-COMM"]

  # Add another empty node to community
  do_action "AddNode", "NewNode2", "1AD-SMALL-COMM"
  do_action "Sleep", 5.minutes

  # Kill last of original nodes
  do_action "SaveHostOfNode", "1AD-SMALL-COMM", "REAR-NODE"
  do_action "KillNodes", "REAR-NODE"

  # Wait for restarts to complete
  wait_for "CommunitiesReady", ["1AD-SMALL-COMM"]

  # Add another empty node to community
  do_action "AddNode", "NewNode3", "1AD-SMALL-COMM"
  do_action "Sleep", 5.minutes

  # Load balance community
  do_action "LoadBalancer", "1AD-SMALL-COMM"
  wait_for "CommunitiesReady", ["1AD-SMALL-COMM"]

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
