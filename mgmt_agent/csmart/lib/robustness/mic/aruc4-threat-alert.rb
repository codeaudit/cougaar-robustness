=begin script

include_path: aruc4-threat-alert.rb
description: Generate Threat Alert

=end

CIP = ENV['CIP']

$:.unshift File.join(CIP, 'csmart', 'config', 'lib')
require 'robustness/uc4/aruc4_actions_and_states'

insert_after :during_stage_1 do
wait_for "NodesPersistedFindProviders", "REAR-A-NODE", "REAR-B-NODE"
assets = {'node' => 'REAR-A-NODE, REAR-B-NODE'}
do_action "InfoMessage", "########  PublishThreatAlert - MAXIMUM  #########"
do_action "PublishThreatAlert",
          "org.cougaar.tools.robustness.ma.HostLossThreatAlert",
          "community",
          "REAR-COMM",
          "maximum",
          5.minutes,
          assets,
          "HealthMonitor"
end
