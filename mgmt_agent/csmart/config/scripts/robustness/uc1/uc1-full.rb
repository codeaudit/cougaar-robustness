CIP = ENV['CIP']
RULES = File.join(CIP, 'csmart','config','rules')

$:.unshift File.join(CIP, 'csmart', 'acme_scripting', 'src', 'lib')
$:.unshift File.join(CIP, 'csmart', 'acme_service', 'src', 'redist')
$:.unshift File.join(CIP, 'csmart', 'config', 'lib')

require 'cougaar/scripting'
require 'ultralog/scripting'
require 'robustness/uc1/aruc1_actions_and_states'

HOSTS_FILE = Ultralog::OperatorUtils::HostManager.new.get_hosts_file

Cougaar::ExperimentMonitor.enable_stdout
Cougaar::ExperimentMonitor.enable_logging

Cougaar.new_experiment("UC1_Small_1AD_Tests").run(1) {

  do_action "LoadSocietyFromScript", "#{CIP}/configs/ul/FULL-1AD-TRANS-1359.rb"
  do_action "LayoutSociety", "#{CIP}/operator/1ad-layout-10_4_1.xml", HOSTS_FILE

  do_action "TransformSociety", false,
    "#{RULES}/isat",
    "#{RULES}/logistics",
    "#{RULES}/robustness/uc1"
  do_action "SaveCurrentSociety", "mySociety.xml"
  do_action "StartJabberCommunications"
  do_action "VerifyHosts"

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
  wait_for "CommunitiesReady", ["1AD-FWD-COMM", "1AD-REAR-COMM"]
  do_action "Sleep", 5.minutes

  # Kill node
  do_action "SaveHostOfNode", "FWD-A"
  do_action "KillNodes", "FWD-A"
  # Wait for restarts to complete
  wait_for "CommunitiesReady", ["1AD-FWD-COMM"]
  # Add an empty node to community
  do_action "AddNode", "FWD-NEW-1", "1AD-FWD-COMM"
  do_action "Sleep", 3.minutes

  # Kill node
  do_action "SaveHostOfNode", "REAR-A"
  do_action "KillNodes", "REAR-A"
  # Wait for restarts to complete
  wait_for "CommunitiesReady", ["1AD-REAR-COMM"]
  # Add an empty node to community
  do_action "AddNode", "REAR-NEW-1", "1AD-REAR-COMM"
  do_action "Sleep", 3.minutes

  # Kill node that contains robustness manager
  do_action "SaveHostOfNode", "FWD-MGMT-NODE"
  do_action "KillNodes", "FWD-MGMT-NODE"
  wait_for "CommunitiesReady", ["1AD-FWD-COMM"]

  # Add another empty node to community
  do_action "AddNode", "FWD-NEW-2", "1AD-FWD-COMM"
  do_action "Sleep", 3.minutes

  # Load balance community
  do_action "LoadBalancer", "1AD-FWD-COMM"
  wait_for "CommunitiesReady", ["1AD-FWD-COMM"]

  #wait_for "Command", "ok"

  wait_for  "GLSConnection", false
  wait_for  "NextOPlanStage"
  do_action "Sleep", 30.seconds
  do_action "PublishNextStage"

  wait_for  "PlanningComplete"  do
    wait_for  "Command", "shutdown"
    #do_action "SaveSocietyCompletion", "completion_#{name}.xml"
    do_action "StopSociety"
    do_action "ArchiveLogs"
    do_action "StopCommunications"
  end

#  do_action "Sleep", 1.minutes
  #do_action "SaveSocietyCompletion", "completion_#{name}.xml"

#  do_action "Sleep", 2.minutes

=begin
  # Get some inventory charts
  do_action "SampleInventory", "1-35-ARBN", "120MM APFSDS-T M829A1:DODIC/C380", "#{name}-1-35-ARBN-DODIC-C380.xml"
  do_action "SampleInventory", "1-35-ARBN", "120MM HEAT-MP-T M830:DODIC/C787", "#{name}-1-35-ARBN-DODIC-C787.xml"
  do_action "SampleInventory", "1-35-ARBN", "MEAL READY-TO-EAT  :NSN/8970001491094", "#{name}-1-35-ARBN-MRE.xml"
  do_action "SampleInventory", "1-35-ARBN", "FRESH FRUITS :NSN/891501F768439", "#{name}-1-35-ARBN-FRUIT.xml"
  do_action "SampleInventory", "1-35-ARBN", "UNITIZED GROUP RATION - HEAT AND SERVE BREAKFAST :NSN/897UGRHSBRKXX", "#{name}-1-35-ARBN-BREAKFAST.xml"

  # 47-FSB inventories
  do_action "SampleInventory", "47-FSB", "120MM APFSDS-T M829A1:DODIC/C380", "#{name}-47-FSB-DODIC-C380.xml"
  do_action "SampleInventory", "47-FSB", "120MM HEAT-MP-T M830:DODIC/C787", "#{name}-47-FSB-DODIC-C787.xml"

  do_action "SampleInventory", "47-FSB", "MEAL READY-TO-EAT  :NSN/8970001491094", "#{name}-47-FSB-MRE.xml"
  do_action "SampleInventory", "47-FSB", "FRESH FRUITS  :NSN/891501F768439", "#{name}-47-FSB-FRUIT.xml"
  do_action "SampleInventory", "47-FSB", "UNITIZED GROUP RATION - HEAT AND SERVE BREAKFAST :NSN/897UGRHSBRKXX", "#{name}-47-FSB-BREAKFAST.xml"
  do_action "SampleInventory", "47-FSB", "DF2:NSN/9140002865294", "#{name}-47-FSB-DF2.xml"
  do_action "SampleInventory", "47-FSB", "JP8:NSN/9130010315816", "#{name}-47-FSB-JP8.xml"
  do_action "SampleInventory", "47-FSB", "GREASE,GENERAL PURP:NSN/9150001806383", "#{name}-47-FSB-GREASE.xml"
  do_action "SampleInventory", "47-FSB", "PETROLATUM,TECHNICA:NSN/9150002500926", "#{name}-47-FSB-PETROLATUM.xml"
  do_action "SampleInventory", "47-FSB", "GLOW PLUG:NSN/2920011883863", "#{name}-47-FSB-GLOWPLUG.xml"
  do_action "SampleInventory", "47-FSB", "BELT,VEHICULAR SAFE:NSN/2540013529175", "#{name}-47-FSB-BELT.xml"
  do_action "SampleInventory", "47-FSB", "BRAKE SHOE:NSN/2530013549427", "#{name}-47-FSB-BRAKESHOE.xml"

  # 123-MSB inventories
  do_action "SampleInventory", "123-MSB", "120MM APFSDS-T M829A1:DODIC/C380", "#{name}-123-MSB-DODIC-C380.xml"
  do_action "SampleInventory", "123-MSB", "120MM HEAT-MP-T M830:DODIC/C787", "#{name}-123-MSB-DODIC-C787.xml"
  do_action "SampleInventory", "123-MSB", "MEAL READY-TO-EAT  :NSN/8970001491094", "#{name}-123-MSB-MRE.xml"
  do_action "SampleInventory", "123-MSB", "FRESH FRUITS  :NSN/891501F768439", "#{name}-123-MSB-FRUIT.xml"
  do_action "SampleInventory", "123-MSB", "UNITIZED GROUP RATION - HEAT AND SERVE BREAKFAST :NSN/897UGRHSBRKXX", "#{name}-123-MSB-BREAKFAST.xml"
  do_action "SampleInventory", "123-MSB", "DF2:NSN/9140002865294", "#{name}-123-MSB-DF2.xml"
  do_action "SampleInventory", "123-MSB", "JP8:NSN/9130010315816", "#{name}-123-MSB-JP8.xml"
  do_action "SampleInventory", "123-MSB", "GREASE,GENERAL PURP:NSN/9150001806383", "#{name}-123-MSB-GREASE.xml"
  do_action "SampleInventory", "123-MSB", "PETROLATUM,TECHNICA:NSN/9150002500926", "#{name}-123-MSB-PETROLATUM.xml"
  do_action "SampleInventory", "123-MSB", "GLOW PLUG:NSN/2920011883863", "#{name}-123-MSB-GLOWPLUG.xml"
  do_action "SampleInventory", "123-MSB", "BELT,VEHICULAR SAFE:NSN/2540013529175", "#{name}-123-MSB-BELT.xml"
  do_action "SampleInventory", "123-MSB", "BRAKE SHOE:NSN/2530013549427", "#{name}-123-MSB-BRAKESHOE.xml"

  # 191-ORDBN Inventory
  do_action "SampleInventory", "191-ORDBN", "120MM APFSDS-T M829A1:DODIC/C380", "#{name}-191-ORDBN-DODIC-C380.xml"
  do_action "SampleInventory", "191-ORDBN", "120MM HEAT-MP-T M830:DODIC/C787", "#{name}-191-ORDBN-DODIC-C787.xml"

  # 343-SUPPLYCO Inventory
  do_action "SampleInventory", "343-SUPPLYCO", "MEAL READY-TO-EAT  :NSN/8970001491094", "#{name}-343-SUPPLYCO-MRE.xml"
  do_action "SampleInventory", "343-SUPPLYCO", "FRESH FRUITS  :NSN/891501F768439", "#{name}-343-SUPPLYCO-FRUIT.xml"
  do_action "SampleInventory", "343-SUPPLYCO", "UNITIZED GROUP RATION - HEAT AND SERVE BREAKFAST :NSN/897UGRHSBRKXX", "#{name}-343-SUPPLYCO-BREAKFAST.xml"

  # 110-POL-SUPPLYCO Inventory
  do_action "SampleInventory", "110-POL-SUPPLYCO", "DF2:NSN/9140002865294", "#{name}-110-POL-SUPPLYCO-DF2.xml"
  do_action "SampleInventory", "110-POL-SUPPLYCO", "JP8:NSN/9130010315816", "#{name}-110-POL-SUPPLYCO-JP8.xml"
  do_action "SampleInventory", "110-POL-SUPPLYCO", "GREASE,GENERAL PURP:NSN/9150001806383", "#{name}-110-POL-SUPPLYCO-GREASE.xml"
  do_action "SampleInventory", "110-POL-SUPPLYCO", "PETROLATUM,TECHNICA:NSN/9150002500926", "#{name}-110-POL-SUPPLYCO-PETROLATUM.xml"

  # 565-RPRPTCO Inventory
  do_action "SampleInventory", "565-RPRPTCO", "GLOW PLUG:NSN/2920011883863", "#{name}-565-RPRPTCO-GLOWPLUG.xml"
  do_action "SampleInventory", "565-RPRPTCO", "BELT,VEHICULAR SAFE:NSN/2540013529175", "#{name}-565-RPRPTCO-BELT.xml"
  do_action "SampleInventory", "565-RPRPTCO", "BRAKE SHOE:NSN/2530013549427", "#{name}-565-RPRPTCO-BRAKESHOE.xml"
=end

#stop to "P" in case you want to rehydrate"
  do_action "Sleep", 5.minutes
  wait_for "Command", "shutdown"
  do_action "StopSociety"
  do_action "ArchiveLogs"
  do_action "StopCommunications"
}
