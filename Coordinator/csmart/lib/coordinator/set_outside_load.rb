=begin script

include_path: set_outside_load.rb
description: set the OutsideLoadDiagnosis to None, Moderate, or High

=end

CIP = ENV['CIP']

$:.unshift File.join(CIP, 'csmart', 'config', 'lib')
require 'coordinator/acuc2b'

insert_after parameters[:location] do
  do_action "SetOutsideLoad", parameters[:enclave], parameters[:value], parameters[:verbose]
end
