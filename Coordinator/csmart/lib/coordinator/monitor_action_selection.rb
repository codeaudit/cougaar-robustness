=begin script

include_path: monitor_action_selection.rb
description: prints a message everytime the coordinator sets an Action value

=end

CIP = ENV['CIP']

$:.unshift File.join(CIP, 'csmart', 'config', 'lib')
require 'coordinator/acuc2b'

insert_after parameters[:location] do
  do_action "MonitorActionSelection", parameters[:verbose]
end
