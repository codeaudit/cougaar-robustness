=begin script

include_path: unleash_defenses.rb
description: enable the coordinator to set actions

=end

CIP = ENV['CIP']

$:.unshift File.join(CIP, 'csmart', 'config', 'lib')
require 'coordinator/leashing'

insert_after :during_stage_1 do
  do_action "MonitorUnleash", 1
  do_action "Unleash"
end
