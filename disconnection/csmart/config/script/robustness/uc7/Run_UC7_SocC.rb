CIP = ENV['CIP']
RULES = File.join(CIP, 'csmart','config','rules')

$:.unshift File.join(CIP, 'csmart', 'acme_scripting', 'src', 'lib')
$:.unshift File.join(CIP, 'csmart', 'acme_service', 'src', 'redist')

require 'cougaar/scripting'
require 'ultralog/scripting'
require 'UC7_RbClassDefs'


HOSTS_FILE = Ultralog::OperatorUtils::HostManager.new.get_hosts_file


Cougaar::ExperimentMonitor.enable_stdout

Cougaar.new_experiment("Soc-Base-Plan_OBJS-UC7").run(1) {

  do_action "LoadSocietyFromScript", "#{CIP}/configs/ul/FULL-1AD-TRANS-1359.rb"
  do_action "LayoutSociety", "#{CIP}/operator/1ad-layout-10_4_1.xml", HOSTS_FILE

  do_action "TransformSociety", false, 
    "#{RULES}/isat",
    "#{RULES}/logistics",
    "#{RULES}/robustness/uc7/AddMgrAgents.rule",
    "#{RULES}/robustness/uc7/UC7.rule"


 puts "Done with rules..."
    
  do_action "SaveCurrentSociety", "myUC7Society.xml"
  do_action "StartJabberCommunications"
  do_action "VerifyHosts"

  do_action "CleanupSociety"

  do_action "ConnectOperatorService"
  do_action "ClearPersistenceAndLogs"

  do_action "StartSociety"
  do_action "Sleep", 7.minutes

  wait_for  "GLSConnection"
  wait_for  "NextOPlanStage"
  do_action "Sleep", 30.seconds
  do_action "PublishNextStage"


#************************************************* UC7 code

do_action "GenericAction" do |run|

  $nodeAgent, $nodeHost = Cougaar::SupportClasses.findSomeNodeAgentAndHost(run) 
 

  $reconnectTime = 200.0
  puts "*** Going to disconnect #{$nodeAgent} at host #{$nodeHost}***"
 
 
#STEP 1.-----------------------------------------------------------Submit disconnect action
  puts "*** disconnecting #{$nodeAgent} at host #{$nodeHost}***"
  puts "*** reconnectTime = #{$reconnectTime}***"
  do_action "EffectUCState", $nodeHost, $nodeAgent, $reconnectTime.to_s, "Disconnection" 
  #-------------------------------------------------------------------Wait for expected cougaar events
  wait_for "OpModeChange1", $nodeAgent do
    do_action "StopSociety"
    do_action "ArchiveLogs"
    do_action "StopCommunications"
  end

# STEP 2. ---------------------------------------------------------Now reconnect
    $reconnectTime2 = 0.0
    puts "*** disconnecting #{$nodeAgent} at host #{$nodeHost}***"
    puts "*** reconnectTime = #{$reconnectTime2}***"
#---------------------------------------------------------------------Submit disconnect action
    do_action "EffectUCState", $nodeHost, $nodeAgent, $reconnectTime2.to_s, "Disconnection"
  wait_for "OpModeChange2", $nodeAgent do
    do_action "StopSociety"
    do_action "ArchiveLogs"
    do_action "StopCommunications"
  end

end
#***************************************************  


  wait_for  "PlanningComplete"  do
    wait_for  "Command", "shutdown"
    do_action "StopSociety"
    do_action "ArchiveLogs"
    do_action "StopCommunications"
  end

  wait_for "Command", "shutdown"
  do_action "StopSociety"
  do_action "ArchiveLogs"
  do_action "StopCommunications"
}
