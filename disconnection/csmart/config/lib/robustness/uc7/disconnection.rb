
module Cougaar

  module Actions
    class StartPlannedDisconnect < Cougaar::Action
      PRIOR_STATES = ["SocietyLoaded"]
      #RESULTANT_STATE = "PlannedDisconnectSignalled"
      DOCUMENTATION = Cougaar.document {
        @description = "Start a Planned Disconnect"
        @parameters = [
          {:node => "required, The node that plans to disconnect."},
          {:seconds => "required, The number of seconds the node plans to be disconnected."},
	    {:messaging => "0 is no messages (the default), 1 is normal messages, 2 is verbose."}
        ]
        @example = "do_action 'StartPlannedDisconnect', 'FWD-C', 60, 0" 
	}

	@@node = nil

      def initialize(run, node, seconds, messaging=0)
        super(run) 
	@node = node
	@seconds = seconds
      @messaging = messaging
      end

      def perform 
	nodeObj = @run.society.nodes[@node]
        host = nodeObj.host
        url = "#{nodeObj.uri}/$#{@node}/Disconnect?Disconnect=Disconnect&expire=#{@seconds.to_s}"
	response, uri = Cougaar::Communications::HTTP.get(url)
        raise "Could not connect to #{url}" unless response
        Cougaar.logger.info "#{response}" if response
        return response, uri
      end
    end

    class EndPlannedDisconnect < Cougaar::Action
      PRIOR_STATES = ["SocietyLoaded"]
      #RESULTANT_STATE = "PlannedDisconnectSignalled"
      DOCUMENTATION = Cougaar.document {
        @description = "End a Planned Disconnect"
        @parameters = [
          {:node => "required, The node that has reconnected."},
	    {:messaging => "0 is no messages (the default), 1 is normal messages, 2 is verbose."}
        ]
        @example = "do_action 'EndPlannedDisconnect', 'FWD-C'"
	}

	@@node = nil

      def initialize(run, node, messaging=0)
        super(run) 
	@node = node
      @messaging = messaging
      end

      def perform 
	nodeObj = @run.society.nodes[@node]
        host = nodeObj.host
        url = "#{nodeObj.uri}/$#{@node}/Disconnect?Reconnect=Reconnect"
        puts "EndPlannedDisconnect url="+url  if @messaging >= 1
        response, uri = Cougaar::Communications::HTTP.get(url)
        puts "response="+response if @messaging >= 2
        raise "Could not connect to #{url}" unless response
        Cougaar.logger.info "#{response}" if response
        return response, uri
      end
    end

    class MonitorPlannedDisconnectExpired < Cougaar::Action
      PRIOR_STATES = ["SocietyLoaded"]
      DOCUMENTATION = Cougaar.document {
        @description = "Print event when a disconnected node should have reconnected."
        @parameters = [
          {:node => "The disconnected node that should have reconnected."},
	    {:messaging => "0 is no messages (the default), 1 is normal messages, 2 is verbose."}
        ]
        @example = "do_action 'MonitorPlannedDisconnectExpired', 'FWD-C'"
	}
      def initialize(run, node=nil, messaging=0)
        super(run)
        @node = node
        @messaging = messaging
      end
      def perform
        @run.comms.on_cougaar_event do |event|
          if (event.component=="DisconnectManagerPlugin") && (@node==nil || event.data.include?(@node+" no longer legitimately Disconnected"))
            puts event.data if @messaging >= 1
          end
        end
      end
    end    

    class MonitorReconnectConfirmed < Cougaar::Action
      PRIOR_STATES = ["SocietyLoaded"]
      DOCUMENTATION = Cougaar.document {
        @description = "Print event when Reconnected Node receives confirmation of Reconnection from Robustness Manager."
        @parameters = [
          {:node => "The node that has reconnected."},
	    {:messaging => "0 is no messages (the default), 1 is normal messages, 2 is verbose."}
        ]
        @example = "do_action 'ReconnectConfirmed', 'FWD-C'"
	}
      def initialize(run, node=nil, messaging=0)
        super(run)
        @node = node
        @messaging = messaging
      end
      def perform
        @run.comms.on_cougaar_event do |event|
          if (event.component=="DisconnectNodePlugin") && (@node==nil || event.data.include?(@node+" has Reconnected"))
            puts event.data if @messaging >= 1
          end
        end
      end
    end

    class MonitorDisconnectionEvents < Cougaar::Action
      PRIOR_STATES = ["SocietyLoaded"]
      DOCUMENTATION = Cougaar.document {
        @description = "Print all events emitted by Disconnection."
        @parameters = [
	    {:messaging => "0 is no messages (the default), 1 is normal messages, 2 is verbose."}
        ]
        @example = "do_action 'MonitorDisconnectionEvents'"
	}
      def initialize(run, messaging=0)
        super(run)
        @messaging = messaging
      end
      def perform
        @run.comms.on_cougaar_event do |event|
          if (event.component=="DisconnectNodePlugin") || (event.component=="DisconnectManagerPlugin")
            puts event.data if @messaging >= 1
          end
        end
      end
    end	

  end

  module States
    class PlannedDisconnectStarted < Cougaar::State
      DEFAULT_TIMEOUT = 2.minutes
      PRIOR_STATES = ["SocietyPlanning"] #,"PlannedDisconnectSignalled"]
      #RESULTANT_STATE = "PlannedDisconnectStarted"
      DOCUMENTATION = Cougaar.document {
        @description = "Waits for Disconnecting Node to receive permission from Coordinator."
        @parameters = [
           {:node => "required, The node that plans to disconnect."},
           {:timeout => "default=nil, Amount of time to wait in seconds."},
	     {:messaging => "0 is no messages (the default), 1 is normal messages, 2 is verbose."},
           {:block => "The timeout handler (unhandled: StopSociety, StopCommunications)"}
        ]
        @example = "wait_for 'PlannedDisconnectStarted', 'FWD-C', 3.minutes"
     }
	      
     def initialize(run, node, timeout=2.minutes, messaging=0, &block)
       super(run, timeout, &block)
       @node = node
       @messaging = messaging
     end
     
     def process
        puts "Waiting " + @timeout.to_s + " seconds for Coordinator's permission to disconnect node "+@node+"." if @messaging >= 1
        loop = true
        requestSeen = false
        while loop
          event = @run.get_next_event
          if event.component=="DisconnectNodePlugin" && event.data.include?("Requesting to Disconnect Node: "+@node)
            requestSeen = true
            puts event.data if @messaging >= 1
          end
          if requestSeen == true
            if event.component=="DisconnectNodePlugin" && event.data.include?(@node+" plans to Disconnect")
              loop = false
              puts event.data if @messaging >= 1
            end
	      if event.component=="DisconnectNodePlugin" && event.data.include?(@node+" has Reconnected")
              loop = false
              puts "Not allowed to Disconnect - Permission Denied" if @messaging >= 1
            end
	      if event.component=="DisconnectServlet" && event.data.include?("Defense not initialized")
              loop = false
              puts "Not allowed to Disconnect - Manager Not Ready" if @messaging >= 1
            end
          end
        end
      end
      
      def unhandled_timeout
        puts "Timed out after " + @timeout.to_s + " seconds waiting for Coordinator's permission to disconnect node " + @node
        @run.do_action "StopSociety" 
        @run.do_action "StopCommunications"
      end
    end

    class PlannedDisconnectExpired < Cougaar::State
      DEFAULT_TIMEOUT = 2.minutes
      #PRIOR_STATES = ["SocietyPlanning","PlannedDisconnectStarted"]
      DOCUMENTATION = Cougaar.document {
        @description = "Waits for Planned Disconnect to expire."
        @parameters = [
           {:node => "required, The node that is disconnected."},
           {:timeout => "default=nil, Amount of time to wait in seconds."},
	     {:messaging => "0 is no messages (the default), 1 is normal messages, 2 is verbose."},
           {:block => "The timeout handler (unhandled: StopSociety, StopCommunications)"}
        ]
        @example = "wait_for 'PlannedDisconnectExpired', 'FWD-C', 3.minutes"
     }
     def initialize(run, node, timeout=2.minutes, messaging=0, &block)
       super(run, timeout, messaging, &block)
       @node = node
       @messaging = messaging
     end
     def process
        puts "Waiting " + @timeout.to_s + " seconds for node "+@node+"'s PlannedDisconnect to expire." if @messaging >= 1
        loop = true
        while loop
          event = @run.get_next_event
          #puts event.data id @messaging >= 2
          if event.component=="DisconnectManagerPlugin" && event.data.include?(@node+" no longer legitimately Disconnected")
            loop = false
            puts event.data
          end
        end
      end
      def unhandled_timeout
        puts "Timed out after "+@timeout.to_s+" seconds waiting for node "+@node+"'s PlannedDisconnect to expire." if @messaging >= 1
        @run.do_action "StopSociety" 
        @run.do_action "StopCommunications"
      end
    end

  end

end

