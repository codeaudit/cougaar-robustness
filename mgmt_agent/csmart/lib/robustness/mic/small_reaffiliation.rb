=begin script

include_path: small_reaffiliation.rb
description: Change robustness enclave for node(s)

=end

CIP = ENV['CIP']

$:.unshift File.join(CIP, 'csmart', 'config', 'lib')
require 'robustness/uc1/aruc1_actions_and_states'

insert_after :during_stage_1 do
  do_action "Sleep", 1.minute
  do_action "ReaffiliationNotification", "FWD-NODE", "SMALL1-COMM", "SMALL2-COMM"
end
