module Cougaar

  module Actions

    class PlannedDisconnect < Cougaar::Action
      PRIOR_STATES = ["SocietyRunning"]
      DOCUMENTATION = Cougaar.document {
        @description = "Performs the Planned Disconnect experiment."
        @parameters = [
           {:nodes => "required, The nodes that plan to disconnect."},
           {:planned => "required, The number of seconds they plan to be disconnected."},
           {:actual => "required, The actual amount of time to actually disconnect them."},
           {:messaging => "0 for no messages (the default), 1 for only error messages, 2 for info messages, 3 for everything."}
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
        @run.info_message "Performing Planned Disconnect of nodes "+@nodes.join(", ")+" for "+@planned.to_s+" secs. Actually disconnecting for "+@actual.to_s+" secs." if @messaging >= 2
        @thread = Thread.new {
          for i in 0 ... @nodes.length
            node = @nodes[i]
            @run.info_message "Starting Planned Disconnect of node "+node+" for "+@planned.to_s+" secs" if @messaging >= 2
            nodeObj = @run.society.nodes[node]
            host = nodeObj.host
            url = "#{nodeObj.uri}/$#{node}/Disconnect?Disconnect=Disconnect&expire=#{@planned.to_s}"
            response, uri = Cougaar::Communications::HTTP.get(url)
            if !response 
              @run.error_message "Could not connect to #{url}. Planned Disconnect Experiment aborted. No disconnects will occur." if @messaging >= 1
              @run['PlannedDisconnectCompleted'] = true
	      return
	    else
              Cougaar.logger.info "#{response}" if @messaging >= 3
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
              @run.info_message event.data if @messaging >= 2
            elsif requestsSeen[node] == true
              if event.component == "DisconnectServlet" && event.data.include?("Failed to Disconnect")
                results[node] = 'failed'
                @run.error_message "Node "+node+" "+event.data if @messaging >= 1
              elsif event.component == "DisconnectNodePlugin" && event.data.include?("Not allowed to Disconnect ManagementAgent")
                results[node] = 'failed'
                @run.error_message event.data if @messaging >= 1
              elsif event.component == "DisconnectNodePlugin" && event.data.include?("has received permission to Disconnect")
                results[node] = 'enabled'
                @run.info_message event.data if @messaging >= 2
              elsif event.component == "DisconnectNodePlugin" && event.data.include?("is denied permission to Disconnect")
                results[node] = 'disabled'
                @run.error_message event.data if @messaging >= 1
              end
            end
          end
          @result = true
          results.each_value do |value|
            if value != 'enabled'
              @run.error_message "Permission to Disconnect was denied for some requesting Nodes. Experiment aborted. No disconnects will occur." if @messaging >= 1
              @run['PlannedDisconnectCompleted'] = true
              return
            end
          end
          @nodes.each do |node|
            cougaar_node = @run.society.nodes[node]
            cougaar_host = cougaar_node.host
            node_names = cougaar_host.nodes.collect { |node| node.name }
            @run.info_message "Taking down network for host #{cougaar_host.name} that has nodes #{node_names.join(', ')}" if @messaging >= 2
            if cougaar_node
              @run.comms.new_message(cougaar_node.host).set_body("command[nic]trigger").send
            else
              @run.error_message "Failed to drop nic to node #{cougaar_node.name}. Node not known. Experiment aborted." if @messaging >= 1
              @run['PlannedDisconnectCompleted'] = true
	      return
            end
          end
          sleep @actual 
          @nodes.each do |node|
            cougaar_node = @run.society.nodes[node]
            cougaar_host = cougaar_node.host
            node_names = cougaar_host.nodes.collect { |node| node.name }
            @run.info_message "Bringing up network for host #{cougaar_host.name} that has nodes #{node_names.join(', ')}" if @messaging >= 2
            if cougaar_node
              @run.comms.new_message(cougaar_node.host).set_body("command[nic]reset").send
            else
              @run.error_message "Failed to bring up nic to node #{cougaar_node.name}. Node not known. Experiment aborted." if @messaging >= 1
              @run['PlannedDisconnectCompleted'] = true
              return
            end
          end
          for i in 0 ... @nodes.length
            node = @nodes[i]
            @run.info_message "Node #{node} Requesting Permission to Reconnect" if @messaging >= 2
            nodeObj = @run.society.nodes[node]
            host = nodeObj.host
            url = "#{nodeObj.uri}/$#{node}/Disconnect?Reconnect=Reconnect"
            @run.info_message "EndPlannedDisconnect url="+url  if @messaging >= 3
            response, uri = Cougaar::Communications::HTTP.get(url)
            if response
              @run.info_message "response="+response if @messaging >= 3
            else
              @run.error_message "Could not connect to #{url}." if @messaging >= 1
            end
          end
          node_count = @nodes.length
          seen = {}
          results = {}
          while results.length < node_count
            event = @run.get_next_event
            node = event.node
            if event.component == "DisconnectNodePlugin" && event.data.include?("Requesting to Connect Node: ")
              seen[node] = true
              @run.info_message event.data if @messaging >= 2
            elsif event.component == "DisconnectManagerPlugin" && event.data.include?("is Tardy")
              start_pos = event.data.index(' ') + 1
              end_pos = event.data.index(':' , start)  
              the_node = event.data.slice(start_pos, end_pos - start_pos)
              seen[the_node] = true
              results[the_node] = 'tardy'
              @run.error_message event.data if @messaging >= 1
            elsif seen[node] == true
              if event.component == "DisconnectServlet" && event.data.include?("Failed to Connect")
                results[node] = 'failed'
                @run.error_message "Node "+node+" "+event.data if @messaging >= 1
              elsif event.component == "DisconnectNodePlugin" && event.data.include?("has received permission to Reconnect")
                results[node] = 'enabled'
                @run.info_message event.data if @messaging >= 2
              elsif event.component == "DisconnectNodePlugin" && event.data.include?("is denied permission to Reconnect")
                results[node] = 'disabled'
                @run.error_message event.data if @messaging >= 2
              end
            end
          end
          @result = true
          results.each_value do |value|
            if value != 'enabled'
              @run.error_message "Permission to Reconnect was denied to some Nodes." if @messaging >= 1
              @run.error_message "Planned Disconnect Experiment Complete." if @messaging >= 1
              @run['PlannedDisconnectCompleted'] = true
              return
            end
          end
          @run.info_message "All Nodes were Permitted to Reconnect." if @messaging >= 2
          @run.info_message "Planned Disconnect Experiment Complete." if @messaging >= 1
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
           {:messaging => "0 for no messages (the default), 1 for only error messages, 2 for info messages, 3 for everything."},
           {:block => "The timeout handler (unhandled: StopSociety, StopCommunications)"}
        ]
        @example = "wait_for 'PlannedDisconnectCompleted', ['FWD-C','FWD-D'], 2.minutes, 3.minutes, 1"
      }
      def initialize(run, timeout=20.minutes, messaging=0, &block)
        super(run, timeout, &block)
        @messaging = messaging
      end
      def process
        @run.info_message "Waiting " + @timeout.to_s + " seconds for Completion of the Planned Disconnect Experiment." if @messaging >= 2
	while @run['PlannedDisconnectCompleted'] == false 
        end
      end
      def unhandled_timeout
        @run.error_message "Timed out after "+@timeout.to_s+" seconds waiting for Completion of the Planned Disconnect Experiment." if @messaging >= 1
        @run.do_action "StopSociety"
        @run.do_action "StopCommunications"
      end
    end
  end

end

