=begin script

include_path: deconfliction.rb
description: initialization for Deconfliction

=end

CIP = ENV['CIP']

$:.unshift File.join(CIP, 'csmart', 'config', 'lib')
require 'robustness/uc9/deconfliction'

insert_after :during_stage_1 do
  do_action "MonitorUnleashConfirmed", 1
  do_action "UnleashDefenses"
end
