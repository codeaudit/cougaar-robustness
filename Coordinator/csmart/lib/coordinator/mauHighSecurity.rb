=begin script

include_path: mauHighSecurity.rb
description: set the MAU weights to HighSecurity

=end

CIP = ENV['CIP']

$:.unshift File.join(CIP, 'csmart', 'config', 'lib')
require 'coordinator/mauPolicy'

insert_after parameters[:location] do
  do_action "Set_MAU_HighSecurity", parameters[:verbose]
end
