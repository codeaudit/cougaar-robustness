CIP = ENV['CIP']
RULES = File.join(CIP, 'csmart','config','rules')

$:.unshift File.join(CIP, 'csmart', 'acme_scripting', 'src', 'lib')
$:.unshift File.join(CIP, 'csmart', 'acme_service', 'src', 'redist')
$:.unshift File.join(CIP, 'csmart', 'config', 'lib')

require 'cougaar/scripting'
require 'ultralog/scripting'
require 'robustness/uc7/disconnection'
require 'robustness/uc9/deconfliction'

HOSTS_FILE = Ultralog::OperatorUtils::HostManager.new.get_hosts_file

Cougaar::ExperimentMonitor.enable_stdout
Cougaar::ExperimentMonitor.enable_logging

Cougaar.new_experiment("Disconnection").run(1) {

  do_action "LoadSocietyFromScript", "#{CIP}/csmart/config/societies/ad/SMALL-1AD-TC20.rb"
# do_action "LayoutSociety", "#{CIP}/operator/layouts/SMALL-1AD-TC20-layout.xml", HOSTS_FILE
  do_action "LayoutSociety", "#{CIP}/operator/uc1-small-1ad-layout.xml", HOSTS_FILE

  do_action "TransformSociety", false, 
    "#{RULES}/isat",
    "#{RULES}/logistics",
    "#{RULES}/robustness/manager.rule",
    "#{RULES}/robustness/uc7",
    "#{RULES}/robustness/uc9"

  do_action "TransformSociety", false, "#{RULES}/robustness/communities"

  do_action "SaveCurrentSociety", "mySociety.xml"
  do_action "SaveCurrentCommunities", "myCommunities.xml" 
  do_action "StartJabberCommunications"
  do_action "DeployCommunitiesFile" 
  do_action "CleanupSociety"
  do_action "VerifyHosts"
  do_action "ConnectOperatorService"
  do_action "ClearPersistenceAndLogs"

  do_action "InstallCompletionMonitor"
  do_action "KeepSocietySynchronized"

#  do_action "GenericAction" do |run|
#    run.comms.on_cougaar_event do |event|
#      if (!event.component.include?("QuiescenceReportServiceProvider"))
#        puts event
#      end
#    end
#  end

  # in case interface is still disabled from prior run
  do_action "EnableNetworkInterfaces", "FWD-NODE"

  do_action "StartSociety"

  wait_for  "GLSConnection", true
  wait_for  "NextOPlanStage"
  do_action "Sleep", 30.seconds
  do_action "PublishNextStage"

# Unleash Suppressed Defenses
  do_action "MonitorUnleashConfirmed", 1
  do_action "UnleashDefenses", 1

# Deconfliction start
  do_action "Sleep", 30.seconds
  do_action "MonitorPlannedDisconnectExpired", "FWD-NODE", 1
  do_action "MonitorReconnectConfirmed", "FWD-NODE", 1
  do_action "StartPlannedDisconnect", "FWD-NODE", 60, 1
  wait_for  "PlannedDisconnectStarted", "FWD-NODE", 2.minutes, 1 do
    wait_for  "Command", "shutdown"
    do_action "StopSociety"
    do_action "EnableNetworkInterfaces", "FWD-NODE"
    do_action "ArchiveLogs"
    do_action "StopCommunications"
  end
  do_action "DisableNetworkInterfaces", "FWD-NODE"
  do_action "Sleep", 40.seconds
  #do_action "Sleep", 60.seconds
  #do_action "Sleep", 70.seconds
  do_action "EnableNetworkInterfaces", "FWD-NODE"
  do_action "EndPlannedDisconnect", "FWD-NODE", 1
# Deconfliction end

  wait_for  "SocietyQuiesced"  do
    wait_for  "Command", "shutdown"
    do_action "SaveSocietyCompletion", "completion_#{experiment.name}.xml"
    do_action "StopSociety"
    do_action "EnableNetworkInterfaces", "FWD-NODE"
    do_action "ArchiveLogs"
    do_action "StopCommunications"
  end

  wait_for "Command", "shutdown"
  do_action "Sleep", 30.seconds
  do_action "SaveSocietyCompletion", "completion_#{experiment.name}.xml"
  do_action "StopSociety"
  do_action "EnableNetworkInterfaces", "FWD-NODE"
  do_action "ArchiveLogs"
  do_action "StopCommunications"
}
