=begin script

include_path: ua_kill.rb
description: Kill REAR-A-NODE, 1-CA-BN-C-NODE, 123-MSB-NODE, 2-CA-BN-A-NODE, 123-MSB-NODE.

=end

insert_after :during_stage_1 do
  wait_for "NodesPersistedFindProviders", "REAR-A-NODE", "1-CA-BN-C-NODE", "2-CA-BN-A-NODE", "123-MSB-NODE"
  do_action "InfoMessage", "##### Killing Nodes REAR-A-NODE, 1-CA-BN-C-NODE, 2-CA-BN-A-NODE, 123-MSB-NODE #####"
  do_action "KillNodes", "REAR-A-NODE", "1-CA-BN-C-NODE", "2-CA-BN-A-NODE", "123-MSB-NODE"
end
