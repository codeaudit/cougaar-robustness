=begin script

include_path: small_create_duplicate_agents.rb
description: Create duplicate agents in small soc

=end

CIP = ENV['CIP']

$:.unshift File.join(CIP, 'csmart', 'config', 'lib')
require 'robustness/uc1/aruc1_actions_and_states'

insert_after :during_stage_1 do
  wait_for "NodesPersistedFindProviders", "1-35-ARBN-NODE", "1-6-INFBN-NODE", "40-ENGBN-NODE", "4-27-FABN-NODE", "2-6-INFBN-NODE", "1-13-ARBN-NODE", "2-70-ARBN-NODE", "70-ENGBN-NODE", "4-1-FABN-NODE", "1-41-INFBN-NODE"
  do_action "InfoMessage", "##### Adding duplicate agents #####"
  do_action "AddAgent", "1-35-ARBN.2-BDE.1-AD.ARMY.MIL", "RPM-BDE-NODE", "1-AD-BDE-COMM"
  do_action "AddAgent", "1-6-INFBN.2-BDE.1-AD.ARMY.MIL", "RPM-BDE-NODE", "1-AD-BDE-COMM"
  do_action "AddAgent", "40-ENGBN.2-BDE.1-AD.ARMY.MIL", "RPM-BDE-NODE", "1-AD-BDE-COMM"
  do_action "AddAgent", "4-27-FABN.2-BDE.1-AD.ARMY.MIL", "RPM-BDE-NODE", "1-AD-BDE-COMM"
  do_action "AddAgent", "2-6-INFBN.2-BDE.1-AD.ARMY.MIL", "RPM-BDE-NODE", "1-AD-BDE-COMM"
  do_action "AddAgent", "1-13-ARBN.3-BDE.1-AD.ARMY.MIL", "RCA-BDE-NODE", "1-AD-BDE-COMM"
  do_action "AddAgent", "2-70-ARBN.3-BDE.1-AD.ARMY.MIL", "RCA-BDE-NODE", "1-AD-BDE-COMM"
  do_action "AddAgent", "70-ENGBN.3-BDE.1-AD.ARMY.MIL", "RCA-BDE-NODE", "1-AD-BDE-COMM"
  do_action "AddAgent", "4-1-FABN.3-BDE.1-AD.ARMY.MIL", "RCA-BDE-NODE", "1-AD-BDE-COMM"
  do_action "AddAgent", "1-41-INFBN.3-BDE.1-AD.ARMY.MIL", "RCA-BDE-NODE", "1-AD-BDE-COMM"
end
