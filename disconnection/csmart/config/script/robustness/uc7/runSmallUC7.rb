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
  

$nodeAgent=""
#-------------------------------------------------------------------Define variables
#Find a node to disconnection
 do_action "GenericAction" do |run|
	run.society.each_node_agent do |agent|
		if agent.name =~ /.*MANAGEMENT_NODE.*/        
		else # grab last node_agent & use that to disconnect (could use any but Mgmt_Node)
			$nodeAgent = agent.name
			$nodeHost = agent.host.name
		end
	end

  $node = $nodeAgent
  $reconnectTime = 200.0
  puts "*** Going to disconnect #{$nodeAgent} at host #{$nodeHost}***"
  

  $reconnectTime2 = 0.0

#-------------------------------------------------------------------Set up substring constants
  $disconnectStr = "DefenseOperatingMode: PlannedDisconnect.UnscheduledDisconnect.Node." + $node 
  $reconnectTimeStr = "DefenseOperatingMode: PlannedDisconnect.UnscheduledReconnectTime.Node." + $node  
  $nodeDefenseStr = "DefenseOperatingMode: PlannedDisconnect.NodeDefense.Node." + $node
#  $nodeMonitoringStr = "DefenseOperatingMode: PlannedDisconnect.NodeMonitoring.Node." + $node

  $applicableStr = "DefenseOperatingMode: PlannedDisconnect.Applicable.Node." + $node
  $defenseStr = "DefenseOperatingMode: PlannedDisconnect.Defense.Node." + $node
  $monitoringStr = "DefenseOperatingMode: PlannedDisconnect.Monitoring.Node." + $node
#  $mgrDefenseStr = "DefenseOperatingMode: PlannedDisconnect.ManagerDefense.Node." + $node
  $mgrMonitoringStr = "DefenseOperatingMode: PlannedDisconnect.ManagerMonitoring.Node." + $node
#-------------------------------------------------------------------Set up expected cougaar event values
  $watchStr1 = $disconnectStr + "=TRUE"
  $watchStr2 = $reconnectTimeStr + '=' + $reconnectTime.to_s
  $watchStr3 = $nodeDefenseStr + '=' + "ENABLED"
#  $watchStr8 = $nodeMonitoringStr + '=' +"DISABLED"
		
  $watchStr4 = $applicableStr + '=' +"TRUE"
  $watchStr5 = $defenseStr + '=' +"ENABLED"
  $watchStr6 = $monitoringStr + '=' +"ENABLED"
#  $watchStr7 = $mgrDefenseStr + '=' +"DISABLED"
  $watchStr9 = $mgrMonitoringStr + '=' +"ENABLED"

  $watchStr11 = $disconnectStr + "=FALSE"
  $watchStr12 = $reconnectTimeStr + '=' + $reconnectTime2.to_s
  $watchStr13 = $nodeDefenseStr + '=' +"DISABLED"
#  $watchStr18 = $nodeMonitoringStr + '=' +"ENABLED"
  
  $watchStr14 = $applicableStr + '=' +"FALSE"
  $watchStr15 = $defenseStr + '=' +"DISABLED"
  $watchStr16 = $monitoringStr + '=' +"DISABLED"
#  $watchStr17 = $mgrDefenseStr + '=' +"ENABLED"
  $watchStr19 = $mgrMonitoringStr + '=' +"DISABLED"

  

#---------------------------------------------------------------------Submit disconnect action
  puts "*** disconnecting #{$nodeAgent} at host #{$nodeHost}***"
  puts "*** reconnectTime = #{$reconnectTime}***"
  do_action "EffectUCState", $nodeHost, $node, $reconnectTime.to_s, "Disconnection" 
  #-------------------------------------------------------------------Wait for expected cougaar events
#  watchList = [ $watchStr1, $watchStr2, $watchStr3, $watchStr4, $watchStr5, $watchStr6, $watchStr7, $watchStr8 ]
  watchList = [ $watchStr1, $watchStr2, $watchStr3, $watchStr4, $watchStr5, $watchStr6, $watchStr9 ]
  wait_for "OpModeChanged", watchList do
    do_action "StopSociety"
    do_action "ArchiveLogs"
    do_action "StopCommunications"
  end

# -------------------------------------------------------------------Now reconnect
#-------------------------------------------------------------------Set up expected cougaar event values
puts "*** disconnecting #{$nodeAgent} at host #{$nodeHost}***"
puts "*** reconnectTime = #{$reconnectTime2}***"
#---------------------------------------------------------------------Submit disconnect action
    do_action "EffectUCState", $nodeHost, $node, $reconnectTime2.to_s, "Disconnection"
#  watchList = [ $watchStr11, $watchStr12, $watchStr13, $watchStr14, $watchStr15, $watchStr16, $watchStr17, $watchStr18 ]
  watchList = [ $watchStr11, $watchStr12, $watchStr13, $watchStr14, $watchStr15, $watchStr16, $watchStr19 ]
  wait_for "OpModeChanged", watchList do
    do_action "StopSociety"
    do_action "ArchiveLogs"
    do_action "StopCommunications"
  end



  
  wait_for  "PlanningComplete" do
    #wait_for  "Command", "shutdown"
    do_action "StopSociety"
    do_action "ArchiveLogs"
    do_action "StopCommunications"
  end
  #
end

#wait_for  "Command", "shutdown"
  
  do_action "StopSociety"
  do_action "ArchiveLogs"
  do_action "StopCommunications"
}
