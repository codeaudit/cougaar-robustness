=begin script

include_path: freeze.rb
description: Disable restarts

=end

$:.unshift File.join(CIP, 'csmart', 'config', 'lib')
require 'robustness/uc1/aruc1_actions_and_states'

insert_after :during_stage_1 do
  do_action "DisableRestarts"
end
