=begin script

include_path: ua_big_kill.rb
description: Kill 14 nodes 401 agents

=end

insert_after :during_stage_1 do
  wait_for "NodesPersistedFindProviders", "ConusTRANSCOM-NODE", "REAR-ROB-MGMT-NODE", "REAR-B-NODE", "1-AD-NODE", "1-AD-BDES-NODE", "UA-B-NODE", "AVN-DET-B-NODE", "NLOS-BN-A-NODE", "1-CA-BN-A-NODE", "1-CA-BN-C-NODE", "1-CA-BN-E-NODE", "2-CA-BN-A-NODE", "2-CA-BN-E-NODE", "3-CA-BN-D-NODE", "123-MSB-NODE"
  do_action "Sleep", 3.minutes
  do_action "InfoMessage", "##### Killing 14 nodes, 401 agents #####"
  #do_action "KillNodes", "ConusTRANSCOM-NODE", "REAR-ROB-MGMT-NODE", "REAR-B-NODE", "1-AD-NODE", "1-AD-BDES-NODE", "UA-B-NODE", "AVN-DET-B-NODE", "NLOS-BN-A-NODE", "1-CA-BN-A-NODE", "1-CA-BN-C-NODE", "1-CA-BN-E-NODE", "2-CA-BN-A-NODE", "2-CA-BN-E-NODE", "3-CA-BN-D-NODE", "123-MSB-NODE"
  do_action "KillNodes", "ConusTRANSCOM-NODE", "REAR-ROB-MGMT-NODE", "REAR-B-NODE", "1-AD-BDES-NODE", "UA-B-NODE", "AVN-DET-B-NODE", "NLOS-BN-A-NODE", "1-CA-BN-A-NODE", "1-CA-BN-C-NODE", "1-CA-BN-E-NODE", "2-CA-BN-A-NODE", "2-CA-BN-E-NODE", "3-CA-BN-D-NODE", "123-MSB-NODE"
end
