=begin script

include_path: small_create_duplicate_agents.rb
description: Create duplicate agents in small soc

=end

CIP = ENV['CIP']

$:.unshift File.join(CIP, 'csmart', 'config', 'lib')
require 'robustness/uc1/aruc1_actions_and_states'

insert_after :during_stage_1 do
  wait_for "NodesPersistedFindProviders", "REAR-NODE"
  do_action "InfoMessage", "##### Adding duplicate agent to Node EuroTRANSCOM-NODE #####"
  do_action "AddAgent", "110-POL-SUPPLYCO.37-TRANSGP.21-TSC.ARMY.MIL", "EuroTRANSCOM-NODE", "SMALL-COMM"
end
