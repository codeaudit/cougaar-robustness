=begin script

include_path: planned_disconnect_completed.rb
description: Prevent end of run or advance to next stage until Planned Disconnect experiment completes or times out. 

=end

CIP = ENV['CIP']

$:.unshift File.join(CIP, 'csmart', 'config', 'lib')
require 'robustness/uc7/disconnection'

verb = parameters[:verbose]

insert_before parameters[:wait_location] do
  timeout = eval(parameters[:timeout].to_s)
  wait_for "PlannedDisconnectCompleted", timeout, verb
end
