=begin script

include_path: sleep.rb
description: sleep

=end

CIP = ENV['CIP']

$:.unshift File.join(CIP, 'csmart', 'config', 'lib')

insert_after parameters[:location] do
  do_action "Sleep", parameters[:period]
end
