module Cougaar

  module Actions

    class TestDisconnect < Cougaar::Action
      {}
              
      def initialize(run, nodes, actual, messaging=0)
        super(run)
        @nodes = nodes
        @actual = actual
        @messaging = messaging
        @thread = nil
      end
     
      def perform

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
        }
      end

    end
  
  end

end
