=begin script

include_path: small_kill_w_mgr.rb
description: Kill 1 node in small soc

=end

insert_after :during_stage_1 do
  wait_for "NodesPersistedFindProviders", "SMALL-MGMT-NODE", "ConusTRANSCOM-NODE"
  do_action "InfoMessage", "##### Killing Nodes SMALL-MGMT-NODE ConusTRANSCOM-NODE #####"
  do_action "KillNodes", "SMALL-MGMT-NODE", "ConusTRANSCOM-NODE"
end
