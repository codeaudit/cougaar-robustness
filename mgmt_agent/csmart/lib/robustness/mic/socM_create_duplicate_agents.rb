=begin script

include_path: small_create_duplicate_agents.rb
description: Create duplicate agents in small soc

=end

CIP = ENV['CIP']

$:.unshift File.join(CIP, 'csmart', 'config', 'lib')
require 'robustness/uc1/aruc1_actions_and_states'

insert_after :during_stage_1 do
  wait_for "NodesPersistedFindProviders", "110-POL-SUPPLYCO-NODE", "REAR-D-NODE", "240-SSCO-NODE", "343-SUPPLYCO-NODE", "597-MAINTCO-NODE", "CONUS-NODE", "ConusTRANSCOM-NODE", "UA-HHC-NODE", "AVN-DET-A-NODE"
  do_action "InfoMessage", "##### Adding duplicate agents #####"
  do_action "AddAgent", "110-POL-SUPPLYCO.37-TRANSGP.21-TSC.ARMY.MIL", "900-POL-SUPPLYCO-NODE", "REAR-COMM"
  do_action "AddAgent", "21-TSC-HQ.ARMY.MIL", "597-MAINTCO-NODE", "REAR-COMM"
  do_action "AddAgent", "240-SSCO.7-CSG.5-CORPS.ARMY.MIL", "REAR-B-NODE", "REAR-COMM"
  do_action "AddAgent", "51-MAINTBN.29-SPTGP.21-TSC.ARMY.MIL", "REAR-C-NODE", "REAR-COMM"
  do_action "AddAgent", "71-MAINTBN.7-CSG.5-CORPS.ARMY.MIL", "125-ORDBN-NODE", "REAR-COMM"
  do_action "AddAgent", "DLAHQ.MIL", "OSD-NODE", "CONUS-COMM"
  do_action "AddAgent", "ConusAir.TRANSCOM.MIL", "AmmoTRANSCOM-NODE", "CONUS-COMM"
  do_action "AddAgent", "FCS-ICV-0.TACP.1-UA.ARMY.MIL", "AVN-DET-A-NODE", "1-UA-COMM"
  do_action "AddAgent", "DET-HQ.AVN-DET.1-UA.ARMY.MIL", "NLOS-A-NODE", "1-UA-COMM"
end
