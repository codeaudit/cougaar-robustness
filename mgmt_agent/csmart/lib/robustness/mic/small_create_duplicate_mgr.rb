=begin script

include_path: small_create_duplicate_mgr.rb
description: Add duplicate manager in small soc

=end

CIP = ENV['CIP']

$:.unshift File.join(CIP, 'csmart', 'config', 'lib')
require 'robustness/uc1/aruc1_actions_and_states'

insert_after :during_stage_1 do
  # Give the manager time to persist before creating clone
  do_action "Sleep", 3.minutes
  do_action "InfoMessage", "##### Adding duplicate manager to Node EuroTRANSCOM-NODE #####"
  do_action "AddAgent", "SMALL-ARManager", "EuroTRANSCOM-NODE", "SMALL-COMM"
end
