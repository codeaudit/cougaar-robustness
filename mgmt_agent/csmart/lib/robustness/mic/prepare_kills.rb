=begin script

include_path: prepare_kills.rb
description: Do action NotifyRestarts on restore runs to re-supress defenses

=end

$:.unshift File.join(CIP, 'csmart', 'config', 'lib')
require 'robustness/uc9/deconfliction'

unless (sequence.index_of(:snapshot_restored).nil?) then
  insert_after :snapshot_restored do
    do_action "NotifyRestart"
  end
end
