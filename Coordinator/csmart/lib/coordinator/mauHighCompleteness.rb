=begin script

include_path: mauHighCompleteness.rb
description: set the MAU weights to HighCompleteness

=end

CIP = ENV['CIP']

$:.unshift File.join(CIP, 'csmart', 'config', 'lib')
require 'coordinator/mauPolicy

insert_after parameters[:location] do
  do_action "Set_MAU_HighCompleteness", parameters[:verbose]
end
