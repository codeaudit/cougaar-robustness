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

$:.unshift "/shares/development/cougaar/csmart/acme_scripting/src/lib"
$:.unshift "/shares/development/cougaar/csmart/acme_service/src/redist"

require 'cougaar/scripting'
require 'ultralog/scripting'
require 'aruc1_actions_and_states'

Cougaar::ExperimentMonitor.enable_stdout

Cougaar.new_experiment("MyExperiment").run {
  do_action "LoadSocietyFromCSmart", "TINY-1AD-TRANS-AR", "dell8200", "admin", "admin", "CSMART10_2"
  #do_action "LoadSocietyFromXML", "TINY-1AD-TRANS-AR.xml"
  do_action "StartJabberCommunications", "acme_console", "dell8200"

# Print out CougaarEvents as they come in 
  do_action "GenericAction" do |run|  
     run.comms.on_cougaar_event do |event|  
       if event.cluster_identifier=="1AD-Enclave1-RobustnessManager"
         puts event  
       end
     end  
  end  

  do_action "VerifyHosts"
  #
#  do_action "ConnectOperatorService"
#  do_action "ClearPersistenceAndLogs"
  #
  do_action "StartSociety"
  #

  wait_for  "Enclave1-Ready"
  do_action "KillNodes", "TINY-1AD-3"
  wait_for  "Enclave1-Ready"

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
