=begin script

include_path: small_create_duplicate_agents.rb
description: Create duplicate agents in small soc

=end

CIP = ENV['CIP']

$:.unshift File.join(CIP, 'csmart', 'config', 'lib')
require 'robustness/uc1/aruc1_actions_and_states'

insert_after :during_stage_1 do
  wait_for "NodesPersistedFindProviders", "REAR-NODE", "ConusTRANSCOM-NODE"
  do_action "InfoMessage", "##### Adding duplicate agents #####"
  do_action "AddAgent", "110-POL-SUPPLYCO.37-TRANSGP.21-TSC.ARMY.MIL", "EuroTRANSCOM-NODE", "SMALL-COMM"
  do_action "AddAgent", "21-TSC-HQ.ARMY.MIL", "SMALL-MGMT-NODE", "SMALL-COMM"
  do_action "AddAgent", "240-SSCO.7-CSG.5-CORPS.ARMY.MIL", "SMALL-MGMT-NODE", "SMALL-COMM"
  do_action "AddAgent", "51-MAINTBN.29-SPTGP.21-TSC.ARMY.MIL", "SMALL-MGMT-NODE", "SMALL-COMM"
  do_action "AddAgent", "71-MAINTBN.7-CSG.5-CORPS.ARMY.MIL", "SMALL-MGMT-NODE", "SMALL-COMM"
  do_action "AddAgent", "DLAHQ.MIL", "SMALL-MGMT-NODE", "SMALL-COMM"
  do_action "AddAgent", "ConusAir.TRANSCOM.MIL", "FWD-NODE", "SMALL-COMM"
end
