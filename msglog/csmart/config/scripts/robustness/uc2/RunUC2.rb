CIP = ENV['CIP']
RULES = File.join(CIP, 'csmart','config','rules')

$:.unshift File.join(CIP, 'csmart', 'acme_scripting', 'src', 'lib')
$:.unshift File.join(CIP, 'csmart', 'acme_service', 'src', 'redist')
$:.unshift File.join(CIP, 'csmart', 'config', 'lib')

require 'cougaar/scripting'
require 'ultralog/scripting'
require 'ultralog/enclaves'
require 'robustness/uc2/msglog'
require 'robustness/uc2/flushmail'

HOSTS_FILE = Ultralog::OperatorUtils::HostManager.new.get_hosts_file

Cougaar::ExperimentMonitor.enable_stdout
Cougaar::ExperimentMonitor.enable_logging

Cougaar.new_experiment("AR-UC2").run(1) {

  do_action "LoadSocietyFromScript", "#{CIP}/csmart/config/societies/ad/FULL-1AD-TRANS-1359.rb"
  do_action "LayoutSociety", "#{CIP}/operator/1ad-layout.xml", HOSTS_FILE

  do_action "TransformSociety", false, 
    "#{RULES}/isat",
    "#{RULES}/logistics",
    "#{RULES}/robustness/uc2/base_msglog.rule",
    "#{RULES}/robustness/uc2/email.rule"
  do_action "SaveCurrentSociety", "mySociety.xml"
  do_action "StartJabberCommunications"
  do_action "CleanupSociety"
  do_action "VerifyHosts"

  do_action "ConnectOperatorService"
  do_action "ClearPersistenceAndLogs"

  # delete email leftover from previous runs
  do_action "FlushMail"

  do_action "InstallCompletionMonitor"

  # in case interfaces are still disabled from prior run
  do_action "EnableNetworkInterfaces", "FWD-B", "FWD-C"

  do_action "StartSociety"

  wait_for  "GLSConnection", true
  wait_for  "NextOPlanStage"
  do_action "Sleep", 30.seconds
  do_action "PublishNextStage"

  # AR-UC2 (Rolling Partition)
  do_action "Sleep", 1.minutes
  #do_action "GenericAction" do |run|
  #  run.comms.on_cougaar_event do |event|
  #    puts event.data
  #  end
  #end
  do_action "MonitorProtocolSelection", "FWD-B", "org.cougaar.core.mts.email.OutgoingEmailLinkProtocol"
  do_action "MonitorProtocolSelection", "FWD-C", "org.cougaar.core.mts.email.OutgoingEmailLinkProtocol"
  do_action "DisableNetworkInterfaces", "FWD-B"
  do_action "GenericAction" do |run|
    sleep 20.seconds
  end
  do_action "DisableNetworkInterfaces", "FWD-C"
  do_action "EnableNetworkInterfaces", "FWD-B"
  do_action "GenericAction" do |run|
    sleep 20.seconds
  end
  do_action "DisableNetworkInterfaces", "FWD-B"
  do_action "EnableNetworkInterfaces", "FWD-C"
  do_action "GenericAction" do |run|
    sleep 20.seconds
  end
  do_action "DisableNetworkInterfaces", "FWD-C"
  do_action "EnableNetworkInterfaces", "FWD-B"
  do_action "GenericAction" do |run|
    sleep 20.seconds
  end
  do_action "DisableNetworkInterfaces", "FWD-B"
  do_action "EnableNetworkInterfaces", "FWD-C"
  do_action "GenericAction" do |run|
    sleep 20.seconds
  end
  do_action "DisableNetworkInterfaces", "FWD-C"
  do_action "EnableNetworkInterfaces", "FWD-B"
  do_action "GenericAction" do |run|
    sleep 20.seconds
  end
  do_action "EnableNetworkInterfaces", "FWD-C"

  wait_for  "SocietyQuiesced"  do
    wait_for  "Command", "shutdown"
    do_action "SaveSocietyCompletion", "completion_#{experiment.name}.xml"
  include "inventory.inc", "RunSoc"
    do_action "EnableNetworkInterfaces", "FWD-B", "FWD-C"
    do_action "StopSociety"
    do_action "ArchiveLogs"
    do_action "StopCommunications"
  end

  wait_for "Command", "shutdown"
  do_action "Sleep", 30.seconds
  do_action "SaveSocietyCompletion", "completion_#{experiment.name}.xml"
  include "inventory.inc", "RunSoc"
  do_action "Sleep", 30.seconds
    do_action "EnableNetworkInterfaces", "FWD-B", "FWD-C"
  do_action "StopSociety"
  do_action "ArchiveLogs"
  do_action "StopCommunications"
}
