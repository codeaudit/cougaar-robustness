=begin script

include_path: leash_on_restart.rb
description: Leash All Defenses on a Full Society Restore

=end

CIP = ENV['CIP']

$:.unshift File.join(CIP, 'csmart', 'config', 'lib')
require 'coordinator/leashing'

insert_after :snapshot_restored do
  do_action "LeashOnRestart"
end
