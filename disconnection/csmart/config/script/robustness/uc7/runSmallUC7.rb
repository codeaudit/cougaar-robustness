##
#  <copyright>
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

#$:.unshift "../../acme_scripting/src/lib"
#$:.unshift "../../acme_service/src/redist"
$:.unshift "/usr/local/acme/redist"
$:.unshift "."



require 'cougaar/scripting'
require 'ultralog/scripting'
require 'UC7_RbClassDefs'

Cougaar::ExperimentMonitor.enable_stdout

Cougaar.new_experiment("Paul_SmallUC7").run(1) {


#for multi runs add parens and a number see below
#Cougaar.new_experiment("scARTExperiment").run(30) {

  do_action "LoadSocietyFromScript","SMALL-1AD-TRANS-Small1-UC7.xml.rb"
  do_action "StartJabberCommunications", "acme_console", "acme"
  do_action "VerifyHosts"
  #
  do_action "ConnectOperatorService", "u113"
  do_action "ClearPersistenceAndLogs"
  #do_action "ClearLogs"
  #do_action "ClearPersistence" 
  #
  do_action "StartSociety", 120
  #
  


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


  do_action "PublishGLSRoot"
  
#******************************************* UC7 Specific code ********************************
#-------------------------------------------------------------------Define variables
#Find a node to disconnect
# do_action "GenericAction" do |run|
#	run.society.each_node_agent do |agent|#
#		if agent.name =~ /.*MANAGEMENT_NODE.*/        
#		else # grab last node_agent & use that to disconnect (could use any but Mgmt_Node)
#			$nodeAgent = agent.name
#			$nodeHost = agent.host.name
#		end
#	end
#Find a node to disconnect
 do_action "GenericAction" do |run|
  $nodeAgent, $nodeHost = Cougaar::SupportClasses.findSomeAgentAndHost(run) 
 

  $reconnectTime = 200.0
  puts "*** Going to disconnect #{$nodeAgent} at host #{$nodeHost}***"
 
 
#STEP 1.-----------------------------------------------------------Submit disconnect action
  puts "*** disconnecting #{$nodeAgent} at host #{$nodeHost}***"
  puts "*** reconnectTime = #{$reconnectTime}***"
  do_action "EffectUCState", $nodeHost, $nodeAgent, $reconnectTime.to_s, "Disconnection" 
  #-------------------------------------------------------------------Wait for expected cougaar events
  wait_for "OpModeChange1", $nodeAgent do
    do_action "StopSociety"
    do_action "ArchiveLogs"
    do_action "StopCommunications"
  end

# STEP 2. ---------------------------------------------------------Now reconnect
    $reconnectTime2 = 0.0
    puts "*** disconnecting #{$nodeAgent} at host #{$nodeHost}***"
    puts "*** reconnectTime = #{$reconnectTime2}***"
#---------------------------------------------------------------------Submit disconnect action
    do_action "EffectUCState", $nodeHost, $nodeAgent, $reconnectTime2.to_s, "Disconnection"
  wait_for "OpModeChange2", $nodeAgent do
    do_action "StopSociety"
    do_action "ArchiveLogs"
    do_action "StopCommunications"
  end

end #of generic action
#**********************************************************************************************

  
  wait_for  "PlanningComplete" do
    #wait_for  "Command", "shutdown"
    do_action "StopSociety"
    do_action "ArchiveLogs"
    do_action "StopCommunications"
  end
  #


#wait_for  "Command", "shutdown"
  
  do_action "StopSociety"
  do_action "ArchiveLogs"
  do_action "StopCommunications"
}
