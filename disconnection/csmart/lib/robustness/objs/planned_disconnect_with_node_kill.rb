=begin script

include_path: planned_disconnect_with_node_kill.rb
description: Planned Disconnect (ARUC7) of Multiple Nodes with Nodes killed while disconnected

include_path: standard_kill_nodes.rb
=end

CIP = ENV['CIP']

$:.unshift File.join(CIP, 'csmart', 'config', 'lib')
require 'robustness/uc7/disconnection'

insert_after parameters[:location] do
  planned = eval(parameters[:planned_disconnect].to_s)
  actual = eval(parameters[:actual_disconnect].to_s)
  verb = parameters[:verbose]
  #do_action "Sleep", 2.minutes
  do_action "PlannedDisconnect", parameters[:nodes], planned, actual, verb
  do_action "Sleep", 1.minutes
  do_action "InfoMessage", "##### Killing Agents #{parameters[:nodes_to_kill]} #####"
  do_action "KillNodes", *parameters[:nodes_to_kill]
end

insert_before parameters[:wait_location] do
  timeout = eval(parameters[:timeout].to_s)
  verb = parameters[:verbose]
  wait_for "PlannedDisconnectCompleted", timeout, verb
end
