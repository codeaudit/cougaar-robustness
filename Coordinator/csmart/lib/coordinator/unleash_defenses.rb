=begin script

include_path: unleash_defenses.rb
description: enable the coordinator to set actions

=end

CIP = ENV['CIP']

$:.unshift File.join(CIP, 'csmart', 'config', 'lib')
require 'coordinator/leashing'

insert_after parameters[:location] do
 if( parameters[:start_delay] != nil && parameters[:start_delay] > 0)
    do_action "SleepFrom", parameters[:location], parameters[:start_delay]
  else
    do_action "SleepFrom", parameters[:location], 60
  end
  do_action "Unleash", parameters[:verbose]
  do_action "UnleashOnSubsequentRestarts", parameters[:verbose]
end
