=begin script

include_path: small_kill_agents.rb
description: Kill agents in small soc

=end

CIP = ENV['CIP']

$:.unshift File.join(CIP, 'csmart', 'config', 'lib')
require 'robustness/uc1/aruc1_actions_and_states'

insert_after :during_stage_1 do
  wait_for "NodesPersistedFindProviders", "REAR-NODE", "ConusTRANSCOM-NODE"
  do_action "InfoMessage", "##### Killing agents #####"
  do_action "RemoveAgents", "110-POL-SUPPLYCO.37-TRANSGP.21-TSC.ARMY.MIL", "21-TSC-HQ.ARMY.MIL", "240-SSCO.7-CSG.5-CORPS.ARMY.MIL", "51-MAINTBN.29-SPTGP.21-TSC.ARMY.MIL", "71-MAINTBN.7-CSG.5-CORPS.ARMY.MIL", "DLAHQ.MIL", "ConusAir.TRANSCOM.MIL"
end
