=begin script

include_path: test_disconnect.rb
description: Test Disconnect of two nodes on a single host

=end

CIP = ENV['CIP']

$:.unshift File.join(CIP, 'csmart', 'config', 'lib')
require 'robustness/uc7/test_disconnect'
#require 'coordinator/leashingNew'

verb = parameters[:verbose]

#insert_before parameters[:wait_location] do
#  timeout = eval(parameters[:timeout].to_s)
#  wait_for "PlannedDisconnectCompleted", timeout, verb
#end

insert_after parameters[:location] do
  actual = eval(parameters[:actual_disconnect].to_s)
  do_action "TestDisconnect", parameters[:nodes], actual, verb
end

#insert_after parameters[:location] do
#  do_action "Unleash", 1
##  do_action "UnleashOnSubsequentRestarts", 1
#end

insert_after parameters[:location] do
 if( parameters[:start_delay] != nil && parameters[:start_delay] > 0)
    do_action "SleepFrom", parameters[:location], parameters[:start_delay]
  end
end

insert_after parameters[:location] do
  if (parameters[:location] == "during_stage_1")
    wait_for "NodesPersistedFindProviders", *parameters[:nodes]
  end
end

#insert_after :society_running do
#  do_action "MonitorPlannedDisconnect", verb
#end