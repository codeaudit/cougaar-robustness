=begin script

include_path: small_kill_mgr.rb
description: Kill mgr node in small soc

=end

insert_after :during_stage_1 do
  do_action "Sleep", 3.minutes
  do_action "InfoMessage", "##### Killing Node SMALL-MGMT-NODE #####"
  do_action "KillNodes", "SMALL-MGMT-NODE"
end
