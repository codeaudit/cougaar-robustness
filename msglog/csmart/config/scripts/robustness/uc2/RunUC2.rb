##
#  <copyright>
#  Copyright 2003 Object Services and Consulting, Inc.
#  Copyright 2002 InfoEther, LLC
#  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
#
#  This program is free software; you can redistribute it and/or modify
#  it under the terms of the Cougaar Open Source License as published by
#  DARPA on the Cougaar Open Source Website (www.cougaar.org).
#
#  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
#  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
#  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
#  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
#  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
#  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
#  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
#  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
#  PERFORMANCE OF THE COUGAAR SOFTWARE.
# </copyright>
#

$:.unshift "../../../../acme_scripting/src/lib"
$:.unshift "../../../../acme_service/src/redist"
$:.unshift "../../../lib"

require 'cougaar/scripting'
require 'ultralog/scripting'
require 'ultralog/stressors'
require 'msglog'

Cougaar::ExperimentMonitor.enable_stdout

Cougaar.new_experiment("scUC2Experiment").run(1) {

#for multi runs add parens and a number see below
#Cougaar.new_experiment("scARTExperiment").run(30) {

  #do_action "LoadSocietyFromCSmart", "SC-1AD-NEW-AL-STRIPPED", "u173", "society_config", "s0c0nfig", "csmart102"
  #do_action "LoadSocietyFromXML","SC-1AD-TRANS.xml"
  do_action "LoadSocietyFromScript","SC-1AD-NEW-AL-RULES-msglog-socC.xml.rb"
  do_action "StartJabberCommunications", "acme_console", "acmef"
  do_action "VerifyHosts"
  #
  do_action "ConnectOperatorService", "sc022"
  do_action "ClearPersistenceAndLogs"
  #do_action "ClearLogs"
  #do_action "ClearPersistence"
  #
  do_action "StartSociety"
  #
  #do_action "GenericAction" do |run|
  #run.comms.on_cougaar_event do |event|
  #  puts event.to_s
  # end   
  #end
  
  wait_for  "OPlanReady" do
    do_action "StopSociety"
    do_action "ArchiveLogs"
    do_action "StopCommunications"
  end

  do_action "SendOPlan"
  wait_for  "GLSReady" do
    do_action "StopSociety"
    do_action "ArchiveLogs"
    do_action "StopCommunications"
  end

  #wait_for  "Command", "OK"
  #do_action "GenericAction" do |run|
  #sleep 3.minutes
  #end

  do_action "PublishGLSRoot"

  #do_action "GenericAction" do |run|
  #sleep 10.seconds
  #end
  
  # AR UC#2 Rolling Partition Defense
  #do_action "GenericAction" do |run|
  #  run.comms.on_cougaar_event do |event|
  #    puts event.data
  #  end
  #end
  do_action "MonitorProtocolSelection", "FWD-A", "org.cougaar.core.mts.email.OutgoingEmailLinkProtocol"
  do_action "MonitorProtocolSelection", "FWD-F", "org.cougaar.core.mts.email.OutgoingEmailLinkProtocol"
  do_action "DisableNetworkInterfaces", "FWD-A"
  do_action "GenericAction" do |run|
    sleep 20.seconds
  end
  do_action "DisableNetworkInterfaces", "FWD-F"
  do_action "EnableNetworkInterfaces", "FWD-A"
  do_action "GenericAction" do |run|
    sleep 20.seconds
  end
  do_action "DisableNetworkInterfaces", "FWD-A"
  do_action "EnableNetworkInterfaces", "FWD-F"
  do_action "GenericAction" do |run|
    sleep 20.seconds
  end
  do_action "DisableNetworkInterfaces", "FWD-F"
  do_action "EnableNetworkInterfaces", "FWD-A"
  do_action "GenericAction" do |run|
    sleep 20.seconds
  end
  do_action "DisableNetworkInterfaces", "FWD-A"
  do_action "EnableNetworkInterfaces", "FWD-F"
  do_action "GenericAction" do |run|
    sleep 20.seconds
  end
  do_action "DisableNetworkInterfaces", "FWD-F"
  do_action "EnableNetworkInterfaces", "FWD-A"
  do_action "GenericAction" do |run|
    sleep 20.seconds
  end
  do_action "EnableNetworkInterfaces", "FWD-F"

  wait_for  "PlanningComplete" do
    #wait_for  "Command", "shutdown"
    do_action "StopSociety"
    do_action "ArchiveLogs"
    do_action "StopCommunications"
  end
  #
  wait_for  "Command", "shutdown"
  
  do_action "EnableNetworkInterfaces", "FWD-A", "FWD-F"

  do_action "StopSociety"
  do_action "ArchiveLogs"
  do_action "StopCommunications"
}
