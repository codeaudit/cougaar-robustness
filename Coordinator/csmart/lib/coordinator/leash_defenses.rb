=begin script

include_path: leash_defenses.rb
description: stop the coordinator from permitting defense actions

=end

CIP = ENV['CIP']

$:.unshift File.join(CIP, 'csmart', 'config', 'lib')
require 'coordinator/leashing'

insert_after parameters[:location] do
  do_action "Leash", parameters[:verbose]
end
