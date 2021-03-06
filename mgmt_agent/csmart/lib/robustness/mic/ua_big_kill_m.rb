=begin script

include_path: ua_big_kill_m.rb
description: Kill multiple nodes to approximate 40% infrastructure loss.

=end

insert_after :during_stage_1 do
  wait_for "NodesPersistedFindProviders", "ConusTRANSCOM-NODE", "REAR-A-NODE", "3-CA-BN-INF-CO-A-NODE", "3-BDE-1-AD-NODE", "NLOS-A-NODE", "123-MSB-HQ-NODE", "343-SUPPLYCO-NODE", "106-TCBN-NODE", "UA-HHC-NODE", "AVN-DET-A-NODE", "NLOS-BTY-B-NODE", "FSB-DRY-CARGO-SECTION-NODE", "FSB-STAFF-CELL-NODE", "FSB-TREATMENT-PLT-NODE", "1-CA-BN-HHC-NODE", "1-CA-BN-RECON-DET-NODE", "1-CA-BN-INF-CO-A-NODE", "1-CA-BN-INF-CO-B-NODE", "2-CA-BN-SUPPORT-SECTION-NODE", "2-CA-BN-MORTAR-BTY-NODE", "2-CA-BN-MCS-CO-A-NODE", "3-CA-BN-SUPPORT-SECTION-NODE", "3-CA-BN-MCS-CO-A-NODE", "3-CA-BN-MCS-CO-B-NODE"
  do_action "Sleep", 3.minutes
  do_action "InfoMessage", "##### Killing 24 nodes and 484 agents #####"
  do_action "KillNodes", "ConusTRANSCOM-NODE", "REAR-A-NODE", "3-CA-BN-INF-CO-A-NODE", "3-BDE-1-AD-NODE", "NLOS-A-NODE", "123-MSB-HQ-NODE", "343-SUPPLYCO-NODE", "106-TCBN-NODE", "UA-HHC-NODE", "AVN-DET-A-NODE", "NLOS-BTY-B-NODE", "FSB-DRY-CARGO-SECTION-NODE", "FSB-STAFF-CELL-NODE", "FSB-TREATMENT-PLT-NODE", "1-CA-BN-HHC-NODE", "1-CA-BN-RECON-DET-NODE", "1-CA-BN-INF-CO-A-NODE", "1-CA-BN-INF-CO-B-NODE", "2-CA-BN-SUPPORT-SECTION-NODE", "2-CA-BN-MORTAR-BTY-NODE", "2-CA-BN-MCS-CO-A-NODE", "3-CA-BN-SUPPORT-SECTION-NODE", "3-CA-BN-MCS-CO-A-NODE", "3-CA-BN-MCS-CO-B-NODE"
end
