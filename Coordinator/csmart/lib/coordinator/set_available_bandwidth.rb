=begin script

include_path: set_available_bandwidth.rb
description: set the AvailableBandwidthDiagnosis to Low, Moderate, High

=end

CIP = ENV['CIP']

$:.unshift File.join(CIP, 'csmart', 'config', 'lib')
require 'coordinator/acuc2b'

insert_after parameters[:location] do
  do_action "SetAvailableBandwidth", parameters[:value], parameters[:verbose]
end
