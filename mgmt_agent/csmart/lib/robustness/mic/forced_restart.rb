=begin script

include_path: forced_restart.rb
description: Kill and restart 1 agent

=end

insert_after :during_stage_1 do
  wait_for "NodesPersistedFindProviders", "FWD-NODE"
  do_action "InfoMessage", "##### Force restart of 1-35-ARBN.2-BDE.1-AD.ARMY.MIL #####"
  do_action "RestartAgents", "1-35-ARBN.2-BDE.1-AD.ARMY.MIL"
end
