CIP = ENV['CIP']

require 'cougaar/scripting'
require 'ultralog/scripting'

include Cougaar

HOSTS_FILE = Ultralog::OperatorUtils::HostManager.new.get_hosts_file

Cougaar::ExperimentMonitor.enable_stdout
Cougaar::ExperimentMonitor.enable_logging

Cougaar.new_experiment().run(parameters[:run_count]) {
  set_archive_path parameters[:archive_dir]

  do_action "LoadSocietyFromScript", parameters[:society_file]
  do_action "LayoutSociety", parameters[:layout_file], HOSTS_FILE

  do_action "TransformSociety", false, *parameters[:rules]
  if (!parameters[:community_rules].nil?)
    do_action "TransformSociety", false, *parameters[:community_rules]
  end

at :transformed_society

  do_action "SaveCurrentSociety", "mySociety.xml"
  do_action 'SaveCurrentCommunities', 'myCommunity.xml'

  do_action "StartCommunications"

  do_action "CleanupSociety"
  do_action "Sleep", 10.seconds

  do_action "VerifyHosts"

  do_action "DeployCommunitiesFile"
  do_action "InstallCompletionMonitor"
  do_action "WatchAgentPersists"
  do_action "KeepSocietySynchronized"
  do_action "MarkForArchive", "#{CIP}/workspace/log4jlogs", "*log", "Log4j node log"
  do_action "MarkForArchive", "#{CIP}/configs/nodes", "*xml", "XML node config files"

  do_action "InstallReportChainWatcher"

at :setup_run

  do_action "StartSociety"

at :wait_for_initialization

  wait_for  "ReportChainReady", 30.minutes
  
at :society_running
  
  wait_for  "GLSConnection", false
  do_action "Sleep", 30.seconds
  wait_for  "NextOPlanStage", 10.minutes
  do_action "PublishNextStage"
  do_action "InfoMessage", "########  Starting Initial Planning Phase  Stage-1#########"

at :during_stage_1

  wait_for  "SocietyQuiesced", 2.hours
  include "#{CIP}/csmart/scripts/definitions/post_stage_data.inc", "Stage1"
  
at :end_of_run

  do_action "FreezeSociety"

at :society_frozen

  do_action "Sleep", 30.seconds
  do_action "StopSociety"
  
at :society_stopped

  do_action "CleanupSociety"
  do_action "StopCommunications"
}
