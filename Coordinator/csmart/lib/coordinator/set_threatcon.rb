=begin script

include_path: set_threatcon.rb
description: set the Threatcon to Low or High

=end

CIP = ENV['CIP']

$:.unshift File.join(CIP, 'csmart', 'config', 'lib')
require 'coordinator/acuc2b'

insert_after parameters[:location] do
  do_action "SetThreatcon", parameters[:value], parameters[:verbose]
end
