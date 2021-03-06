=begin script

include_path: small_reaffiliation.rb
description: Change robustness enclave for node(s)

=end

CIP = ENV['CIP']

$:.unshift File.join(CIP, 'csmart', 'config', 'lib')
require 'robustness/uc1/aruc1_actions_and_states'

insert_after :during_stage_1 do
  do_action "Sleep", 1.minute
  do_action "ChangeCommunityAffiliation", "AmmoTRANSCOM-NODE", "CONUS-COMM", "1-AD-DIV-COMM", "180000"
end
