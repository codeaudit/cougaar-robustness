=begin script

include_path: planned_disconnect.rb
description: Planned Disconnect (ARUC7) of Multiple Nodes

=end

CIP = ENV['CIP']

$:.unshift File.join(CIP, 'csmart', 'config', 'lib')
require 'robustness/uc7/disconnection'

verb = parameters[:verbose]

insert_after :society_running do
  do_action "MonitorPlannedDisconnect", verb
end

insert_after parameters[:location] do
  if (parameters[:location] == "during_stage_1")
    wait_for "NodesPersistedFindProviders", *parameters[:nodes]
  end
 if( parameters[:start_delay] != nil && parameters[:start_delay] > 0)
    do_action "SleepFrom", parameters[:location], parameters[:start_delay]
  end
  planned = eval(parameters[:planned_disconnect].to_s)
  actual = eval(parameters[:actual_disconnect].to_s)
  do_action "PlannedDisconnect", parameters[:nodes], planned, actual, verb
end

insert_before parameters[:wait_location] do
  timeout = eval(parameters[:timeout].to_s)
  wait_for "PlannedDisconnectCompleted", timeout, verb
end
