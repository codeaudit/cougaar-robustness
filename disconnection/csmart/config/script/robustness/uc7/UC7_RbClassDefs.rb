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


module Cougaar
  module Actions
    class EffectUCState < Cougaar::Action      #RESULTANT_STATE = "ConditionChanged"
      #DOCUMENTATION = Cougaar.document {
       # @description = "Set A Deconfliction/Disconnection Condition to a supplied value via HTTP"
       # @parameters = [
       #   {:desc => "required, The run description."}
       # ]
       # @example = "do_action 'EffectUCState', 'u115', 'MANAGEMENT_NODE', 1200, 'Deconfliction' " 
	#}

	@@node = nil

      def initialize(run, agentHost, agentName, newValue, ucName)
        super(run) 
	@agentHost = agentHost
	@agentName = agentName
	@newValue = newValue
	@ucName = ucName			

      end
      def perform 
     
		result, uri = SupportClasses::EmitHTTP.now(@agentHost, @agentName, @newValue, @ucName)
		#puts result if result
		#Cougaar.logger.info "#{result}" if result
		return result, uri
      end
    end
  end


require 'cougaar/communications'
require 'xmlrpc/client'

module SupportClasses
    
    def SupportClasses.findSomeNodeAgentAndHost(run)
	    puts "findNodeAgentAndHost called ******************"
	        nodeAgent = nil
		nodeHost = nil
		run.society.each_node do |node|
		  if !(node.get_facet("role") =~"Management")
			#node.each_agent do |agent|
			#    if !(agent.name =~ /.*ARManager.*/ )        
			       nodeAgent = node.name
			       nodeHost = node.host.name
			       break
			#    end
			#end
		  end
		end
		puts "uc7:findSomeNodeAgentAndHost found: " + nodeAgent + ":"+ nodeHost
		return nodeAgent, nodeHost
    end
  
    class EmitHTTP
    
	    def self.now(hostName, agentName, newValue, ucName)
	    #do stuff
	        url = "http://#{hostName}:8800/$#{agentName}/#{ucName}?reconnectTime=#{newValue}"
		result, uri = Cougaar::Communications::HTTP.get(url)
		  puts "URL -> " + url
		  puts "HTTP Result -> " + result if result
		  puts "HTTP URI -> " + uri if uri
		  
	#            Cougaar.logger.info "#{result}" if result
		return result, uri
	    end
    end
    
end


  module States

	class OpModeChange1 < Cougaar::State
	      DEFAULT_TIMEOUT = 10.minutes
	      #PRIOR_STATES = ["SocietyPlanning"]
	      #DOCUMENTATION = Cougaar.document {
		#@description = "Waits for the Op Modes to change after EffectUCState is called."
		#@parameters = [
		 # {:watchStrings => "array of watch strings that need to be seen."},
		 # {:timeout => "default=nil, Amount of time to wait in seconds."},
		 # {:block => "The timeout handler (unhandled: StopSociety, StopCommunications)"}
		#]
		#@example = "wait_for 'OpModeChanged', 3.minutes do puts 'Did not see the Op Modes change!!!' do_action 'StopSociety' do_action 'StopCommunications' end "
	      #}
	      
	      def initialize(run, node, timeout=nil, &block)
	        $node = node
		#-------------------------------------------------------------------Set up substring constants
		  $disconnectStr = "DefenseOperatingMode: PlannedDisconnect.UnscheduledDisconnect.Node." + $node 
		  $reconnectTimeStr = "DefenseOperatingMode: PlannedDisconnect.UnscheduledReconnectTime.Node." + $node  
		  $nodeDefenseStr = "DefenseOperatingMode: PlannedDisconnect.NodeDefense.Node." + $node
		
		  $applicableStr = "DefenseOperatingMode: PlannedDisconnect.Applicable.Node." + $node
		  $defenseStr = "DefenseOperatingMode: PlannedDisconnect.Defense.Node." + $node
		  $monitoringStr = "DefenseOperatingMode: PlannedDisconnect.Monitoring.Node." + $node
		  $mgrMonitoringStr = "DefenseOperatingMode: PlannedDisconnect.ManagerMonitoring.Node." + $node
		#-------------------------------------------------------------------Set up expected cougaar event values
		  $watchStr1 = $disconnectStr + "=TRUE"
		  $watchStr2 = $reconnectTimeStr + '=' + $reconnectTime.to_s
		  $watchStr3 = $nodeDefenseStr + '=' + "ENABLED"			
		  $watchStr4 = $applicableStr + '=' +"TRUE"
		  $watchStr5 = $defenseStr + '=' +"ENABLED"
		  $watchStr6 = $monitoringStr + '=' +"ENABLED"
		  $watchStr7 = $mgrMonitoringStr + '=' +"ENABLED"
	        @watchStrings = [ $watchStr1, $watchStr2, $watchStr3, $watchStr4, $watchStr5, $watchStr6, $watchStr7 ]
		super(run, timeout, &block)
	      end
	      
	      def process
	      
	        i=0
		puts "******** Watch Strings **********"
	        @watchStrings.each do |watch|
			puts i.to_s + ". " +watch
			i=i+1
		end
		puts "*********************************"

		loop = true
		while loop
			event = @run.get_next_event
			#puts "****New Event: "+event.data
			index = 0
			# watch for the specifid strings
			@watchStrings.each do |watch|
				if watch
					if event.data.include?(watch) 
					    puts "*** Found Match At index = "+index.to_s
					    @watchStrings[index] = nil #set found string to nil
					end
				end
				index += 1
			end
			#now check & see if all watch strings are nil
			done = true
			@watchStrings.each do |watch|
			    done = false if watch
			end
			loop = false if done
		end
	      end
	      
	      def unhandled_timeout
		@run.do_action "StopSociety"
		@run.do_action "StopCommunications"
	      end
	end	
	class OpModeChange2 < Cougaar::State
	      DEFAULT_TIMEOUT = 10.minutes
	      #PRIOR_STATES = ["SocietyPlanning"]
	      #DOCUMENTATION = Cougaar.document {
		#@description = "Waits for the Op Modes to change after EffectUCState is called."
		#@parameters = [
		 # {:watchStrings => "array of watch strings that need to be seen."},
		 # {:timeout => "default=nil, Amount of time to wait in seconds."},
		 # {:block => "The timeout handler (unhandled: StopSociety, StopCommunications)"}
		#]
		#@example = "wait_for 'OpModeChanged', 3.minutes do puts 'Did not see the Op Modes change!!!' do_action 'StopSociety' do_action 'StopCommunications' end "
	      #}
	      
	      def initialize(run, node, timeout=nil, &block)
	        $node = node
		#-------------------------------------------------------------------Set up substring constants
		  $disconnectStr = "DefenseOperatingMode: PlannedDisconnect.UnscheduledDisconnect.Node." + $node 
		  $reconnectTimeStr = "DefenseOperatingMode: PlannedDisconnect.UnscheduledReconnectTime.Node." + $node  
		  $nodeDefenseStr = "DefenseOperatingMode: PlannedDisconnect.NodeDefense.Node." + $node
		
		  $applicableStr = "DefenseOperatingMode: PlannedDisconnect.Applicable.Node." + $node
		  $defenseStr = "DefenseOperatingMode: PlannedDisconnect.Defense.Node." + $node
		  $monitoringStr = "DefenseOperatingMode: PlannedDisconnect.Monitoring.Node." + $node
		  $mgrMonitoringStr = "DefenseOperatingMode: PlannedDisconnect.ManagerMonitoring.Node." + $node
		#-------------------------------------------------------------------Set up expected cougaar event values
		  $watchStr11 = $disconnectStr + "=FALSE"
		  $watchStr12 = $reconnectTimeStr + '=' + $reconnectTime2.to_s
		  $watchStr13 = $nodeDefenseStr + '=' +"DISABLED"		  
		  $watchStr14 = $applicableStr + '=' +"FALSE"
		  $watchStr15 = $defenseStr + '=' +"DISABLED"
		  $watchStr16 = $monitoringStr + '=' +"DISABLED"
		  $watchStr17 = $mgrMonitoringStr + '=' +"DISABLED"
	        @watchStrings = [ $watchStr11, $watchStr12, $watchStr13, $watchStr14, $watchStr15, $watchStr16, $watchStr17 ]
		super(run, timeout, &block)
	      end
	      
	      def process
	      
	        i=0
		puts "******** Watch Strings **********"
	        @watchStrings.each do |watch|
			puts i.to_s + ". " +watch
			i=i+1
		end
		puts "*********************************"

		loop = true
		while loop
			event = @run.get_next_event
			#puts "****New Event: "+event.data
			index = 0
			# watch for the specifid strings
			@watchStrings.each do |watch|
				if watch
					if event.data.include?(watch) 
					    puts "*** Found Match At index = "+index.to_s
					    @watchStrings[index] = nil #set found string to nil
					end
				end
				index += 1
			end
			#now check & see if all watch strings are nil
			done = true
			@watchStrings.each do |watch|
			    done = false if watch
			end
			loop = false if done
		end
	      end
	      
	      def unhandled_timeout
		@run.do_action "StopSociety"
		@run.do_action "StopCommunications"
	      end
	end	
   end
end