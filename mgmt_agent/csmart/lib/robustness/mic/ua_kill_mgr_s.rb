=begin script
                                                                                              
include_path: ua_kill_mgr_s.rb
description: Kill manager node
                                                                                              
=end
                                                                                              
insert_after :during_stage_1 do
  wait_for "NodesPersistedFindProviders", "UA-CA-ROB-MGMT-NODE", "1-CA-BN-A-NODE", "REAR-A-NODE"
  do_action "Sleep", 2.minutes
  do_action "KillNodes", "UA-CA-ROB-MGMT-NODE", "1-CA-BN-A-NODE", "REAR-A-NODE"
end
