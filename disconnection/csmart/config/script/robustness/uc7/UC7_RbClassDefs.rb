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
    
      def initialize(run, hostName, agentName, newValue, ucName)
        super(run) 
	@hostName = hostName
	@agentName = agentName
	@newValue = newValue
	@ucName = ucName			
	
      end
      def perform 
		result, uri = SupportClasses::EmitHTTP.now(@hostName, @agentName, @newValue, @ucName)
		#puts result if result
		#Cougaar.logger.info "#{result}" if result
		return result, uri
      end
    end
  end


require 'cougaar/communications'
require 'xmlrpc/client'

module SupportClasses
    
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

	class OpModeChanged < Cougaar::State
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
	      
	      def initialize(run, watchStrings, timeout=nil, &block)
	        @watchStrings = watchStrings
		super(run, timeout, &block)
	      end
	      
	      def process
	      
	        i=0
		puts "******** Watch Strings **********"
	        @watchStrings.each do |watch|
			puts i.to_s + ". " +watch
		end
		puts "*********************************"

		loop = true
		while loop
			event = @run.get_next_event
			puts "****New Event: "+event.data
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