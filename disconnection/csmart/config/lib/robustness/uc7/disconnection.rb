module Cougaar

  module Actions

    class StartPlannedDisconnect < Cougaar::Action
      PRIOR_STATES = ["SocietyLoaded"]
      DOCUMENTATION = Cougaar.document {
        @description = "Start a Planned Disconnect"
        @parameters = [
          {:nodes => "required, The nodes that plan to be disconnected."},
          {:seconds => "required, The number of seconds the nodes plan to be disconnected."},
	    {:messaging => "0 is no messages (the default), 1 is normal messages, 2 is verbose."}
        ]
        @example = "do_action 'StartPlannedDisconnect', ['FWD-C', 'FWD-D'] 60, 1" 
	}

	@@nodes = nil

      def initialize(run, nodes, seconds, messaging=0)
        super(run) 
	@nodes = nodes
	@seconds = seconds
        @messaging = messaging
      end

      def perform 
        for i in 0 ... @nodes.length
          node = @nodes[i]
	  nodeObj = @run.society.nodes[node]
          host = nodeObj.host
          url = "#{nodeObj.uri}/$#{node}/Disconnect?Disconnect=Disconnect&expire=#{@seconds.to_s}"
	  response, uri = Cougaar::Communications::HTTP.get(url)
          raise "Could not connect to #{url}" unless response
          Cougaar.logger.info "#{response}" if response
	end
      end
    end

    class EndPlannedDisconnect < Cougaar::Action
      PRIOR_STATES = ["SocietyLoaded"]
      DOCUMENTATION = Cougaar.document {
        @description = "End a Planned Disconnect"
        @parameters = [
          {:nodes => "required, The nodes that have reconnected."},
	  {:messaging => "0 is no messages (the default), 1 is normal messages, 2 is verbose."}
        ]
        @example = "do_action 'EndPlannedDisconnect', ['FWD-C','FWD-D'], 1"
	}

	@@nodes = nil

      def initialize(run, nodes, messaging=0)
        super(run) 
	@nodes = nodes
        @messaging = messaging
      end

      def perform 
        for i in 0 ... @nodes.length
          node = @nodes[i]
 	  nodeObj = @run.society.nodes[node]
          host = nodeObj.host
          url = "#{nodeObj.uri}/$#{node}/Disconnect?Reconnect=Reconnect"
          @run.info_message "EndPlannedDisconnect url="+url  if @messaging >= 1
          response, uri = Cougaar::Communications::HTTP.get(url)
          if (response && @messaging >= 2) 
              @run.info_message "response="+response
          end
          raise "Could not connect to #{url}" unless response
          Cougaar.logger.info "#{response}" if response
	end
      end
    end

    class MonitorPlannedDisconnectExpired < Cougaar::Action
      PRIOR_STATES = ["SocietyLoaded"]
      DOCUMENTATION = Cougaar.document {
        @description = "Print event when disconnected nodes should have reconnected."
        @parameters = [
          {:nodes => "The disconnected nodes that should have reconnected."},
	    {:messaging => "0 is no messages (the default), 1 is normal messages, 2 is verbose."}
        ]
        @example = "do_action 'MonitorPlannedDisconnectExpired', ['FWD-C', 'FWD-D'], 1"
	}
      def initialize(run, nodes=nil, messaging=0)
        super(run)
        @nodes = nodes
        @messaging = messaging
      end
      def perform
        @run.comms.on_cougaar_event do |event|
          for i in 0 ... @nodes.length
            node = @nodes[i]
            if (event.component=="DisconnectManagerPlugin") && (node==nil || event.data.include?(node+" no longer legitimately Disconnected"))
              @run.info_message event.data if @messaging >= 1
            end
          end
        end
      end
    end    

    class MonitorReconnectConfirmed < Cougaar::Action
      PRIOR_STATES = ["SocietyLoaded"]
      DOCUMENTATION = Cougaar.document {
        @description = "Print event when Reconnected Node receives confirmation of Reconnection from Robustness Manager."
        @parameters = [
          {:nodes => "The nodes that have reconnected."},
	  {:messaging => "0 is no messages (the default), 1 is normal messages, 2 is verbose."}
        ]
        @example = "do_action 'ReconnectConfirmed', ['FWD-C', 'FWD-D']"
	}
      def initialize(run, nodes=nil, messaging=0)
        super(run)
        @nodes = nodes
        @messaging = messaging
      end
      def perform
        @run.comms.on_cougaar_event do |event|
	  for i in 0 ... @nodes.length
            node = @nodes[i]
            if (event.component=="DisconnectNodePlugin") && (node==nil || event.data.include?("Planned Disconnect DISABLED for "+node))
              @run.info_message event.data if @messaging >= 1
            end
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
            @run.info_message event.data if @messaging >= 1
          end
        end
      end
    end	

  end

  module States
    class ActualDisconnectCompleted < Cougaar::State
      DEFAULT_TIMEOUT = 2.minutes
      PRIOR_STATES = ["SocietyPlanning"] #,"PlannedDisconnectSignalled"]
      DOCUMENTATION = Cougaar.document {
        @description = "Waits for all Disconnecting Nodes to receive permission from Coordinator."
        @parameters = [
           {:nodes => "required, The nodes that plan to disconnect."},
           {:actual => "required, The actual amount of time to disconnect."},
           {:timeout => "default=nil, Amount of time to wait in seconds."},
           {:messaging => "0 is no messages (the default), 1 is normal messages, 2 is verbose."},
           {:block => "The timeout handler (unhandled: StopSociety, StopCommunications)"}
        ]
        @example = "wait_for 'ActualDisconnectCompleted', ['FWD-C','FWD-D'], 2.minutes, 3.minutes, 1"
      }
	      
      def initialize(run, nodes, actual, timeout=2.minutes, messaging=0, &block)
        super(run, timeout, &block)
        @nodes = nodes
        @actual = actual
        @messaging = messaging
        @result = false
      end
     
      def process
        @run.info_message "Waiting " + @timeout.to_s + " seconds for Coordinator's permission to disconnect node "+@nodes.join(", ")+"." if @messaging >= 1
        loop = true
	node_count = @nodes.length
        requestsSeen = {}
        results = {}
        while results.length < node_count
          event = @run.get_next_event
          node = event.node
          if event.component=="DisconnectNodePlugin" && event.data.include?("Requesting to Disconnect Node: ")
            requestsSeen[node] = true
            @run.info_message event.data if @messaging >= 1
          elsif requestsSeen[node] == true
            if event.component=="DisconnectServlet" && event.data.include?("Defense not initialized")
	      results[node] = 'failed'
              @run.error_message "Node "+node+" not allowed to Disconnect - Manager Not Ready" if @messaging >= 0
            elsif event.component=="DisconnectNodePlugin" && event.data.include?("Planned Disconnect ENABLED for ")
	      results[node] = 'enabled'
              @run.info_message event.data if @messaging >= 1
            elsif event.component=="DisconnectNodePlugin" && event.data.include?("Planned Disconnect DISABLED for ")
              results[node] = 'disabled'
              @run.info_message "Not allowed to Disconnect - Permission Denied" if @messaging >= 1
            end
          end
        end
	@result = true
        results.each_value do |value|
	  if value != 'enabled'
            @run.info_message "Planned Disconnect was NOT ENABLED for some requesting Nodes." if @messaging >= 1
            return
          end
        end
        @nodes.each do |node|
          cougaar_node = @run.society.nodes[node]
          cougaar_host = cougaar_node.host
          node_names = cougaar_host.nodes.collect { |node| node.name }
          @run.info_message "Taking down network for host #{cougaar_host.name} that has nodes #{node_names.join(', ')}"
          if cougaar_node
            @run.comms.new_message(cougaar_node.host).set_body("command[nic]trigger").send
          else
            raise_failure "Cannot disable nic on node #{node}, node unknown."
          end
        end
        sleep @actual 
        @nodes.each do |node|
          cougaar_node = @run.society.nodes[node]
          cougaar_host = cougaar_node.host
          node_names = cougaar_host.nodes.collect { |node| node.name }
          @run.info_message "Bringing up network for host #{cougaar_host.name} that has nodes #{node_names.join(', ')}"    
          if cougaar_node
            @run.comms.new_message(cougaar_node.host).set_body("command[nic]reset").send
          else
            raise_failure "Cannot enable nic on node #{node}, node unknown."
          end
        end
      end
      
      def unhandled_timeout
        @run.info_message "Timed out after "+@timeout.to_s+" seconds waiting for Coordinator's permission to disconnect nodes "+@nodes.join(", ")+"."
      end
    end

    class PlannedDisconnectExpired < Cougaar::State
      DEFAULT_TIMEOUT = 2.minutes
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
        @run.info_message "Waiting " + @timeout.to_s + " seconds for node "+@node+"'s PlannedDisconnect to expire." if @messaging >= 1
        loop = true
        while loop
          event = @run.get_next_event
          #@run.info_message event.data id @messaging >= 2
          if event.component=="DisconnectManagerPlugin" && event.data.include?(@node+" no longer legitimately Disconnected")
            loop = false
            @run.info_message event.data
          end
        end
      end
      def unhandled_timeout
        @run.info_message "Timed out after "+@timeout.to_s+" seconds waiting for nodes "+@nodes+"'s PlannedDisconnect to expire." if @messaging >= 1
      end
    end

  end

end

