=begin script

include_path: disconnect.rb
description: Planned Disconnect (ARUC7) of Multiple Nodes

=end

CIP = ENV['CIP']

$:.unshift File.join(CIP, 'csmart', 'config', 'lib')
require 'robustness/uc7/disconnection'

insert_after parameters[:location] do

  #nodes = parameters[:nodes]
  planned = eval(parameters[:planned_disconnect].to_s)
  actual = eval(parameters[:actual_disconnect].to_s)
  verb = parameters[:verbose]

  do_action "Sleep", 30.seconds

  do_action "MonitorPlannedDisconnectExpired", parameters[:nodes], verb
  do_action "MonitorReconnectConfirmed", parameters[:nodes], verb

  do_action "StartPlannedDisconnect", parameters[:nodes], planned, verb

  wait_for "ActualDisconnectCompleted", parameters[:nodes], actual, 30.minutes, verb

  do_action "EndPlannedDisconnect", parameters[:nodes], verb

end
