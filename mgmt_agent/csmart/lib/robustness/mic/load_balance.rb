=begin script

include_path: load_balance.rb
description: Invoke Load Balancer

=end

CIP = ENV['CIP']

$:.unshift File.join(CIP, 'csmart', 'config', 'lib')
require 'robustness/uc1/aruc1_actions_and_states'

insert_after :during_stage_1 do
  wait_for "NodesPersistedFindProviders", "110-POL-SUPPLYCO-NODE", "125-ORDBN-NODE", "191-ORDBN-NODE", "240-SSCO-NODE", "343-SUPPLYCO-NODE", "565-RPRPTCO-NODE", "597-MAINTCO-NODE", "900-POL-SUPPLYCO-NODE", "AVN-CO-NODE", "RCA-REAR-NODE", "REAR-A-NODE", "REAR-B-NODE", "REAR-C-NODE", "REAR-CA-NODE", "REAR-D-NODE", "REAR-ROB-MGMT-NODE", "REAR-SEC-MGMT-NODE", "REAR-WP1-NODE", "REAR-XNODE-1", "REAR-XNODE-2", "REAR-XNODE-3", "REAR-XNODE-4", "RPM-REAR-NODE"
  do_action "LoadBalancer", "REAR-COMM"

  wait_for "NodesPersistedFindProviders", "1-UA-CA-NODE", "1-UA-ROB-MGMT-NODE", "1-UA-SEC-MGMT-NODE", "1-UA-WP1-NODE", "1-UA-XNODE-1", "1-UA-XNODE-2", "1-UA-XNODE-3", "1-UA-XNODE-4", "1-UA-XNODE-5", "1-UA-XNODE-6", "AVN-DET-A-NODE", "AVN-DET-B-NODE", "FSB-CO-HQ-CIC-NODE", "FSB-DISTRO-FWD-EVAC-NODE", "FSB-DRY-CARGO-SECTION-NODE", "FSB-FUEL-WATER-SECTION-A-NODE", "FSB-FUEL-WATER-SECTION-B-NODE", "FSB-HOLDING-DET-MED-TREATMENT-NODE", "FSB-MAINT-PLT-NODE", "FSB-SURG-STAFF-CELL-NODE", "FSB-SUSTAIN-CO-HQ-NODE", "NLOS-A-NODE", "NLOS-B-NODE", "NLOS-BTY-A-NODE", "NLOS-BTY-B-NODE", "NLOS-BTY-C-NODE", "RCA-UA-NODE", "RPM-UA-NODE", "UA-BIC-NODE", "UA-HHC-NODE"
  do_action "LoadBalancer", "1-UA-COMM"
end
