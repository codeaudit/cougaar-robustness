=begin script

include_path: unleash_defenses.rb
description: enable the coordinator to set actions

=end

CIP = ENV['CIP']

$:.unshift File.join(CIP, 'csmart', 'config', 'lib')
require 'coordinator/leashing'

insert_after :society_running do
  do_action "UnleashDefenses"
end
