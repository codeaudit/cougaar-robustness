=begin script

include_path: small_reaffiliation.rb
description: Change robustness enclave for node(s)

=end

CIP = ENV['CIP']
 
$:.unshift File.join(CIP, 'csmart', 'config', 'lib')
require 'robustness/uc4/aruc4_actions_and_states'
 
insert_after :during_stage_1 do
  do_action "Sleep", 2.minute
  assets = {"node"=>"FWD-NODE"}
  do_action "PublishThreatAlert",
            "org.cougaar.tools.robustness.ma.ReaffiliationNotification",
            "1-35-ARBN.2-BDE.1-AD.ARMY.MIL",
            "medium",
            10.hours,
            assets,
            "RobustnessManager"
end
