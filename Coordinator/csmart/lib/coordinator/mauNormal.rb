=begin script

include_path: mauNormal.rb
description: set the MAU weights to Normal

=end

CIP = ENV['CIP']

$:.unshift File.join(CIP, 'csmart', 'config', 'lib')
require 'coordinator/mauPolicy

insert_after parameters[:location] do
  do_action "Set_MAU_Normal", parameters[:verbose]
end
