=begin script

include_path: small_kill.rb
description: Kill 1 node in small soc

=end

insert_after :during_stage_1 do
  wait_for "NodesPersistedFindProviders", "ConusTRANSCOM-NODE"
  do_action "InfoMessage", "##### Killing Node ConusTRANSCOM-NODE #####"
  do_action "KillNodes", "ConusTRANSCOM-NODE"
end
