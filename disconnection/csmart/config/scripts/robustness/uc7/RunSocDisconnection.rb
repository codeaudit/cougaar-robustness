CIP = ENV['CIP']
RULES = File.join(CIP, 'csmart','config','rules')

$:.unshift File.join(CIP, 'csmart', 'acme_scripting', 'src', 'lib')
$:.unshift File.join(CIP, 'csmart', 'acme_service', 'src', 'redist')
$:.unshift File.join(CIP, 'csmart', 'config', 'lib')

require 'cougaar/scripting'
require 'ultralog/scripting'
require 'ultralog/enclaves'
require 'robustness/uc7/disconnection'

HOSTS_FILE = Ultralog::OperatorUtils::HostManager.new.get_hosts_file

Cougaar::ExperimentMonitor.enable_stdout
Cougaar::ExperimentMonitor.enable_logging

Cougaar.new_experiment("Disconnection").run(1) {

  do_action "LoadSocietyFromScript", "#{CIP}/csmart/config/societies/ad/FULL-1AD-TRANS-1359.rb"
  do_action "LayoutSociety", "#{CIP}/operator/1ad-layout.xml", HOSTS_FILE

  do_action "TransformSociety", false, 
    "#{RULES}/isat",
    "#{RULES}/logistics",
    "#{RULES}/robustness/uc1/manager.rule",
    "#{RULES}/robustness/uc7/disconnection.rule",
    "#{RULES}/robustness/UC9/deconfliction.rule"

  do_action "TransformSociety", false, 
    "#{RULES}/robustness/community.rule"

  do_action "SaveCurrentSociety", "mySociety.xml"
  do_action "SaveCurrentCommunities", "myCommunities.xml" 
  do_action "StartJabberCommunications"
  do_action "DeployCommunitiesFile" 
  do_action "CleanupSociety"
  do_action "VerifyHosts"


  do_action "ConnectOperatorService"
  do_action "ClearPersistenceAndLogs"
  do_action "InstallCompletionMonitor"

  do_action "EnableNetworkInterfaces", "FWD-D"

  do_action "StartSociety"

  wait_for  "GLSConnection", true
  wait_for  "NextOPlanStage"
  do_action "Sleep", 30.seconds
  do_action "PublishNextStage"

# Disconnection
  do_action "Sleep", 30.seconds
  do_action "MonitorPlannedDisconnectExpired"
  do_action "StartPlannedDisconnect", "FWD-D", 60
  wait_for  "PlannedDisconnectStarted", "FWD-D"
  do_action "DisableNetworkInterfaces", "FWD-D"
  #do_action "Sleep", 50.seconds
  do_action "Sleep", 60.seconds
  #do_action "Sleep", 70.seconds
  do_action "EnableNetworkInterfaces", "FWD-D"
  do_action "EndPlannedDisconnect", "FWD-D"
  wait_for  "ReconnectConfirmed", "FWD-D"

  wait_for  "SocietyQuiesced"  do
    wait_for  "Command", "shutdown"
    #do_action "SaveSocietyCompletion", "completion_#{experiment.name}.xml"
    #include "inventory.inc", "RunSoc"
    do_action "StopSociety"
    do_action "ArchiveLogs"
    do_action "CleanupSociety"
    do_action "StopCommunications"
  end

  wait_for "Command", "shutdown"
  #do_action "Sleep", 30.seconds
  #do_action "SaveSocietyCompletion", "completion_#{experiment.name}.xml"
  #include "inventory.inc", "RunSoc"
  do_action "Sleep", 30.seconds
  do_action "StopSociety"
  do_action "ArchiveLogs"
  do_action "CleanupSociety"
  do_action "StopCommunications"
}
