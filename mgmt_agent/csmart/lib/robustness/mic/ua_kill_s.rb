=begin script

include_path: ua_kill_s.rb
description: Kill 5 nodes (0 wps), 96 agents

=end

insert_after :during_stage_1 do
  wait_for "NodesPersistedFindProviders", "ConusTRANSCOM-NODE", "REAR-A-NODE", "UA-B-NODE", "3-CA-BN-C-NODE", "123-MSB-NODE"
  do_action "Sleep", 3.minutes
  do_action "InfoMessage", "##### Killing 5 nodes (0 wps) and 96 agents: ConusTRANSCOM-NODE, REAR-A-NODE, UA-B-NODE, 3-CA-BN-C-NODE, 123-MSB-NODE #####"
  do_action "KillNodes", "ConusTRANSCOM-NODE", "REAR-A-NODE", "UA-B-NODE", "3-CA-BN-C-NODE", "123-MSB-NODE"
end
