=begin script

include_path: community_reassignment.rb
description: Change robustness enclave for node(s)
             location: tag to insert Change action
             old_community: community node is originally in
             new_community: community node will move to

=end

CIP = ENV['CIP']

$:.unshift File.join(CIP, 'csmart', 'config', 'lib')
require 'robustness/uc1/aruc1_actions_and_states'

insert_after parameters[:location] do
  do_action "ChangeCommunityAffiliation", "AVN-CO-NODE", parameters[:old_community], parameters[:new_community], "180000"
end
