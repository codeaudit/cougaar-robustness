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

#DCM: change to match our paths
#$:.unshift "../src/lib"
$:.unshift "/shares/development/acme/acme_scripting/src/lib"

#$:.unshift "../../acme_service/src/redist"
$:.unshift "/shares/development/acme/acme_service/src/redist"

require 'cougaar/scripting'
require 'ultralog/scripting'

Cougaar::ExperimentMonitor.enable_stdout

Cougaar.new_experiment("MyExperiment").run {
  #do_action "LoadSocietyFromCSmart", "TINY-1AD-TRANS", "net3", "admin", "admin", "CSMART10_0"
  do_action "LoadSocietyFromXML", "aruc1_society.xml"
  do_action "StartJabberCommunications", "acme_console", "oak"

# Print out CougaarEvents as they come in 
  do_action "GenericAction" do |run|  
     run.comms.on_cougaar_event do |event|  
       puts event  
       # or print whatever  
     end  
  end  

  do_action "VerifyHosts"
  #
#  do_action "ConnectOperatorService"
#  do_action "ClearPersistenceAndLogs"
  #
  do_action "StartSociety"
  #
  wait_for  "OPlanReady"
  do_action "SendOPlan"
  wait_for  "GLSReady"
  do_action "PublishGLSRoot"
  wait_for  "PlanningComplete"
  #
  #wait_for  "Command", "shutdown"
  do_action "StopSociety"
  do_action "StopCommunications"
}
