 #  Copyright 2003-2004 Object Services and Consulting, Inc.
 #  under sponsorship of the Defense Advanced Research Projects
 #  Agency (DARPA).
 #
 #  You can redistribute this software and/or modify it under the
 #  terms of the Cougaar Open Source License as published on the
 #  Cougaar Open Source Website (www.cougaar.org).
 #
 #  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 #  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 #  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 #  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 #  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 #  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 #  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 #  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 #  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 #  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 #  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

module Cougaar

  module Actions

    class MonitorPlannedDisconnect < Cougaar::Action
      DOCUMENTATION = Cougaar.document {
        @description = "Prints out some interesting Planned Disconnect Events."
        @parameters = [
           {:messaging => "0 for no messages (the default), 1 for only error messages, 2 for info messages, 3 for everything."}
        ]
        @example = "do_action 'MonitorPlannedDisconnect', 1"
      }
              
      def initialize(run, messaging=0)
        super(run)
        @messaging = messaging
      end

      def perform
        @run.comms.on_cougaar_event do |event|
          if event.component == "DisconnectNodePlugin" &&
            (event.data.include?("Requesting to Disconnect Node") ||
	     event.data.include?("Requesting to Connect Node") ||
             event.data.include?("has received permission to Reconnect") ||
             event.data.include?("is denied permission to Reconnect"))
            @run.info_message event.data if @messaging >= 2
          elsif event.component == "DisconnectManagerPlugin" && 
                event.data.include?("is Tardy")
            @run.info_message event.data if @messaging >= 2
          elsif event.component == "DisconnectServlet" && 
                event.data.include?("Failed to Connect")
            @run.info_message event.data if @messaging >= 2
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
        @thread = nil
        @thread2 = nil
      end
     
      def perform
        @run['PlannedDisconnectCompleted'] = false 
        @run.info_message "Performing Planned Disconnect of nodes "+@nodes.join(", ")+" for "+@planned.to_s+" secs. Actually disconnecting for "+@actual.to_s+" secs." if @messaging >= 2

        # instigate request to disconnect via servlet  
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

        # handle response to disconnect request
        @run['PlannedDisconnectResults'] = {}
        listener = @run.comms.on_cougaar_event do |event|
          node = event.node
          if event.component == "DisconnectServlet" && event.data.include?("Failed to Disconnect")
            @run['PlannedDisconnectResults'][node] = 'failed'
            @run.error_message "Node "+node+" "+event.data if @messaging >= 1
          elsif event.component == "DisconnectNodePlugin" && event.data.include?("Not allowed to Disconnect ManagementAgent")
            @run['PlannedDisconnectResults'][node] = 'failed'
            @run.error_message event.data if @messaging >= 1
          elsif event.component == "DisconnectNodePlugin" && event.data.include?("has received permission to Disconnect")
            @run['PlannedDisconnectResults'][node] = 'enabled'
            @run.info_message event.data if @messaging >= 2
          elsif event.component == "DisconnectNodePlugin" && event.data.include?("is denied permission to Disconnect")
            @run['PlannedDisconnectResults'][node] = 'disabled'
            @run.info_message event.data if @messaging >= 2
          end
          if @run['PlannedDisconnectResults'].length >= @nodes.length 
	    @run.comms.remove_on_cougaar_event(listener)
          end
        end
        
        # wait for results
        while @run['PlannedDisconnectResults'].length < @nodes.length 
          next
        end

        # make sure all nodes got permission before doing the disconnect
        @run['PlannedDisconnectResults'].each_value do |value|
          if value != 'enabled'
            @run.info_message "Permission to Disconnect was denied for some requesting Nodes. "+
                              "Experiment aborted. No disconnects will occur." if @messaging >= 2
            @run['PlannedDisconnectCompleted'] = true
            return
          end
        end

        # disconnect nics
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

        @thread = Thread.new {

          # sleep for period of actual disconnect
          sleep @actual 
  
          # reconnect nics
          @nodes.each do |node|
            cougaar_node = @run.society.nodes[node]
            cougaar_host = cougaar_node.host
            node_names = cougaar_host.nodes.collect { |node| node.name }
            @run.info_message "Bringing up network for host #{cougaar_host.name} that has nodes #{node_names.join(', ')}" if @messaging >= 2
            if cougaar_node
              @run.comms.new_message(cougaar_node.host).set_body("command[nic]reset").send
            else
              @run.error_message "Failed to bring up nic to node #{cougaar_node.name}. Node not known." if @messaging >= 1
              @run['PlannedDisconnectCompleted'] = true
              return
            end
          end

          # instigate request to reconnect via servlet  
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
  
          # wait for success or failure back from each node
          @run['PlannedDisconnectResults'] = {}
          listener = @run.comms.on_cougaar_event do |event|
            node = event.node
#           if event.component == "DisconnectManagerPlugin" && event.data.include?("is Tardy")
#             start_pos = event.data.index(' ') + 1
#             end_pos = event.data.index(':' , start_pos)  
#             the_node = event.data.slice(start_pos, end_pos - start_pos)
#             @run['PlannedDisconnectResults'][the_node] = 'tardy'
#             @run.info_message event.data if @messaging >= 2
            if event.component == "DisconnectServlet" && event.data.include?("Failed to Connect")
              @run['PlannedDisconnectResults'][node] = 'failed'
#             @run.error_message "Node "+node+" "+event.data if @messaging >= 1
            elsif event.component == "DisconnectNodePlugin" && event.data.include?("has received permission to Reconnect")
              @run['PlannedDisconnectResults'][node] = 'enabled'
#             @run.info_message event.data if @messaging >= 2
            elsif event.component == "DisconnectNodePlugin" && event.data.include?("is denied permission to Reconnect")
              @run['PlannedDisconnectResults'][node] = 'disabled'
#             @run.info_message event.data if @messaging >= 2
            end
	    if @run['PlannedDisconnectResults'].length >= @nodes.length
              @run.comms.remove_on_cougaar_event(listener)
            end
          end
     
          # wait for results
          while @run['PlannedDisconnectResults'].length < @nodes.length 
            next
          end

          # report status
          @run['PlannedDisconnectResults'].each_value do |value|
            if value != 'enabled'
              @run.info_message "Not all Nodes Reconnected." if @messaging >= 2
              @run.info_message "Planned Disconnect Experiment Complete." if @messaging >= 2
              @run['PlannedDisconnectCompleted'] = true
              return
            end
          end
          @run.info_message "All Nodes were Permitted to Reconnect." if @messaging >= 2
          @run.info_message "Planned Disconnect Experiment Complete." if @messaging >= 2
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
