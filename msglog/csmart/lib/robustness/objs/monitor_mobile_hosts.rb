=begin script

include_path: monitor_mobile_hosts.rb
description: Prints message when MobileHostsAspect detects host move (ARUC8)

=end

CIP = ENV['CIP']

$:.unshift File.join(CIP, 'csmart', 'config', 'lib')
require 'robustness/uc8/mobile_hosts'

insert_after :society_running do

  do_action "MonitorMobileHosts"

end
