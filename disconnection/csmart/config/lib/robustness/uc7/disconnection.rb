module Cougaar

  module Actions

    class MonitorPlannedDisconnectEvents < Cougaar::Action
      PRIOR_STATES = ["SocietyLoaded"]
      DOCUMENTATION = Cougaar.document {
        @description = "Print event when Planned Disconnect emits events of interest."
        @parameters = [
          {:nodes => "The nodes that have will be monitored."},
          {:messaging => "0 is no messages (the default), 1 is normal messages, 2 is verbose."}
        ]
        @example = "do_action 'MonitorPlannedDisconnectEvents', ['FWD-C', 'FWD-D']"
        }
      def initialize(run, nodes=nil, messaging=0)
        super(run)
        @nodes = nodes
        @messaging = messaging
      end
      def perform
        @run.comms.on_cougaar_event do |event|
          if( ( (event.component == "DisconnectNodePlugin") &&
                (event.data.include?("has received permission to Reconnect") ||
                 event.data.include?("is denied permission to Reconnect") ||
                 event.data.include?("Requesting to Connect") ||
                 event.data.include?("Not allowed to Disconnect ManagementAgent"))) ||
              (event.component == "DisconnectServlet" && event.data.include?("Failed to Connect")) ||
              (event.component == "DisconnectManagerPlugin" && event.data.include?("is Tardy")))
            if @nodes == nil
              @run.info_message event.data if @messaging >= 1
            else
              for i in 0 ... @nodes.length
                node = @nodes[i]
                if (event.node == node) || event.data.include?(node)
                  @run.info_message event.data if @messaging >= 1
                end
              end
            end
          end
        end
      end
    end

    class PlannedDisconnect < Cougaar::Action
      PRIOR_STATES = ["SocietyRunning"]
      DOCUMENTATION = Cougaar.document {
        @description = "Performs the Planned Disconnect experiment."
        @parameters = [
           {:nodes => "required, The nodes that plan to disconnect."},
           {:planned => "required, The number of seconds they plan to be disconnected."},
           {:actual => "required, The actual amount of time to actually disconnect them."},
           {:messaging => "0 is no messages (the default), 1 is normal messages, 2 is verbose."}
        ]
        @example = "do_action 'PlannedDisconnect', ['FWD-C','FWD-D'], 8.minutes, 12.minutes, 1"
      }
              
      def initialize(run, nodes, planned, actual, messaging=0)
        super(run)
        @nodes = nodes
        @planned = planned
        @actual = actual
        @messaging = messaging
        @result = false
        @thread = nil
      end
     
      def perform
        @run['PlannedDisconnectCompleted'] = false 
        @run.info_message "Performing Planned Disconnect of nodes "+@nodes.join(", ")+" for "+@planned.to_s+" secs. Actually disconnecting for "+@actual.to_s+" secs." if @messaging >= 1
        @thread = Thread.new {
          for i in 0 ... @nodes.length
            node = @nodes[i]
            @run.info_message "Starting Planned Disconnect of node "+node+" for "+@planned.to_s+" secs" if @messaging >= 1
            nodeObj = @run.society.nodes[node]
            host = nodeObj.host
            url = "#{nodeObj.uri}/$#{node}/Disconnect?Disconnect=Disconnect&expire=#{@planned.to_s}"
            response, uri = Cougaar::Communications::HTTP.get(url)
            if !response 
              raise "Could not connect to #{url}" 
              @run.info_message "Could not connect to #{url}. Planned Disconnect Experiment aborted. No disconnects will occur." if @messaging >= 1
              @run['PlannedDisconnectCompleted'] = true
	      return
	    else
              Cougaar.logger.info "#{response}" if response
            end
          end
          node_count = @nodes.length
          requestsSeen = {}
          results = {}
          while results.length < node_count
            event = @run.get_next_event
            node = event.node
            if event.component == "DisconnectNodePlugin" && event.data.include?("Requesting to Disconnect Node: ")
              requestsSeen[node] = true
              @run.info_message event.data if @messaging >= 1
            elsif requestsSeen[node] == true
              if event.component == "DisconnectServlet" && event.data.include?("Failed to Disconnect")
                results[node] = 'failed'
                @run.error_message "Node "+node+" "+event.data if @messaging >= 0
              elsif event.component == "DisconnectNodePlugin" && 
                    event.data.include?("has received permission to Disconnect")
                results[node] = 'enabled'
                @run.info_message event.data if @messaging >= 1
              elsif event.component == "DisconnectNodePlugin" && event.data.include?("is denied permission to Disconnect")
                results[node] = 'disabled'
                @run.info_message event.data if @messaging >= 1
              end
            end
          end
          @result = true
          results.each_value do |value|
            if value != 'enabled'
              @run.info_message "Permission to Disconnect was denied for some requesting Nodes. Experiment aborted. No disconnects will occur." if @messaging >= 1
              @run['PlannedDisconnectCompleted'] = true
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
          for i in 0 ... @nodes.length
            node = @nodes[i]
            @run.info_message "Ending Planned Disconnect of node "+node if @messaging >= 1
            nodeObj = @run.society.nodes[node]
            host = nodeObj.host
            url = "#{nodeObj.uri}/$#{node}/Disconnect?Reconnect=Reconnect"
            @run.info_message "EndPlannedDisconnect url="+url  if @messaging >= 2
            response, uri = Cougaar::Communications::HTTP.get(url)
            if (response && @messaging >= 2) 
                @run.info_message "response="+response
            end
            raise "Could not connect to #{url}" unless response
            Cougaar.logger.info "#{response}" if response
          end
          @run['PlannedDisconnectCompleted'] = true
        }
      end

    end
  
  end

  module States
    class PlannedDisconnectCompleted < Cougaar::State
      DEFAULT_TIMEOUT = 20.minutes
      PRIOR_STATES = ["SocietyPlanning"]
      DOCUMENTATION = Cougaar.document {
        @description = "Waits for Completion of Planned Disconnect Experiment."
        @parameters = [
           {:timeout => "default=nil, Amount of time to wait in seconds."},
           {:messaging => "0 is no messages (the default), 1 is normal messages, 2 is verbose."},
           {:block => "The timeout handler (unhandled: StopSociety, StopCommunications)"}
        ]
        @example = "wait_for 'PlannedDisconnectCompleted', ['FWD-C','FWD-D'], 2.minutes, 3.minutes, 1"
      }
      def initialize(run, timeout=20.minutes, messaging=0, &block)
        super(run, timeout, &block)
        @messaging = messaging
      end
      def process
        @run.info_message "Waiting " + @timeout.to_s + " seconds for Completion of the Planned Disconnect Experiment." if @messaging >= 1
	while @run['PlannedDisconnectCompleted'] == false 
        end
        @run.info_message "Planned Disconnect Experiment Completed." if @messaging >= 1        
      end
      def unhandled_timeout
        @run.info_message "Timed out after "+@timeout.to_s+" seconds waiting for Completion of the Planned Disconnect Experiment."
        @run.do_action "StopSociety"
        @run.do_action "StopCommunications"
      end
    end
  end

end

