=begin script

include_path: planned_disconnect.rb
description: Planned Disconnect (ARUC7) of Multiple Nodes

=end

CIP = ENV['CIP']

$:.unshift File.join(CIP, 'csmart', 'config', 'lib')
require 'robustness/uc7/disconnection'

insert_after parameters[:location] do
  planned = eval(parameters[:planned_disconnect].to_s)
  actual = eval(parameters[:actual_disconnect].to_s)
  verb = parameters[:verbose]
  do_action "Sleep", 2.minutes
  do_action "MonitorPlannedDisconnectEvents", parameters[:nodes], verb
  do_action "PlannedDisconnect", parameters[:nodes], planned, actual, verb
end

insert_before parameters[:wait_location] do
  timeout = eval(parameters[:timeout].to_s)
  verb = parameters[:verbose]
  wait_for "PlannedDisconnectCompleted", timeout, verb
end
