module Cougaar

  module States

    class CommunitiesReady < Cougaar::State
      DEFAULT_TIMEOUT = 30.minutes
      PRIOR_STATES = ["SocietyRunning"]
      def initialize(run, community_names, timeout=nil, &block)
        super(run, timeout, &block)
        @community_names = community_names
      end
      def process
        loop = true
        while loop
          event = @run.get_next_event
          @community_names.delete_if { |name| event.data=="Community #{name} Ready" }
          if @community_names.size==0
            loop = false
          end
        end
      end
      def unhandled_timeout
        @run.do_action "StopSociety"
        @run.do_action "StopCommunications"
      end
    end
  end

  module Actions

    class AddNode < Cougaar::Action
      def initialize(run, nodeName, communityName, hostName=KillHost_CONST)
        super(run)
	@community = communityName
        @hostName = hostName
	@nodeName = nodeName
      end
      def perform
        @run.society.each_host do |host|
          if host.name == @hostName
             newNode = host.add_node(@nodeName)
             newNode.classname = "org.cougaar.bootstrap.Bootstrapper"
             newNode.add_prog_parameter("org.cougaar.core.node.Node")
             components = ["org.cougaar.tools.robustness.ma.plugins.NodeHealthMonitorPlugin", 
                "org.cougaar.tools.robustness.sensors.PingServerPlugin", 
                 "org.cougaar.tools.robustness.sensors.PingRequesterPlugin",
                 "org.cougaar.tools.robustness.sensors.HeartbeatRequesterPlugin",
                 "org.cougaar.community.CommunityPlugin",
		 "org.cougaar.community.util.CommunityViewerServlet",
                 "org.cougaar.tools.robustness.ma.ui.ARServlet",
		 "com.boeing.pw.mct.exnihilo.plugin.EN4JPlugin",
                 "org.cougaar.core.mobility.service.RedirectMovePlugin",
                 "org.cougaar.core.mobility.service.RootMobilityPlugin"]
             newNode.add_components(components)
             sensorDomain = Cougaar::Model::Component.new("org.cougaar.tools.robustness.sensors.SensorDomain")
             sensorDomain.agent = @nodeName
	     sensorDomain.classname = "org.cougaar.tools.robustness.sensors.SensorDomain"
             sensorDomain.insertionpoint = "Node.AgentManager.Agent.DomainManager.Domain"
             sensorDomain.add_argument("sensors")
             newNode.add_component(sensorDomain)
             @parameters = []
 	     @run.society.each_node do |node|
                if @parameters.length == 0
                   @parameters = node.parameters
                   break
                   #node.each_component do |component|
                    # newNode.add_component(component)
		   #end
                end
             end
             newNode.add_parameter(@parameters)
             newNode.override_parameter("-Dorg.cougaar.node.name", @nodeName)
             newNode.add_parameter("-Dorg.cougaar.tools.robustness.community=#{@community}")

             post_node_xml(newNode)
             msg_body = @nodeName + ".rb"
             result = @run.comms.new_message(host).set_body("command[start_xml_node]#{msg_body}").request(120)
	     if result.nil?
               puts "Could not start node #{@nodeName} on host #{@hostName}"
             else
               #puts "start node #{@nodeName} successfully on host #{@hostName}: #{result}"
	       ORIGAGENTS_CONST[@nodeName] = 0
               #ORIGAGENTS_CONST.each {|key, value| print key, " is ", value, "\n"}
               ALLNODES_CONST[ALLNODES_CONST.length] = @nodeName
               PORTS_CONST[@nodeName] = @hostName
             end
          end
        end
      end
      def post_node_xml(node)
        node_society = Cougaar::Model::Society.new( "society-for-#{node.name}" ) do |society|
          society.add_host( node.host.name ) do |host|
            host.add_node( node.clone(host) )
          end
        end
        node_society.remove_all_facets
	result = Cougaar::Communications::HTTP.post("http://#{node.host.host_name}:9444/xmlnode/#{node.name}.rb", node_society.to_ruby, "x-application/ruby")
        puts result if @debug
      end
    end

    class LoadBalancer < Cougaar::Action
      DEFAULT_TIMEOUT = 30.minutes
      def initialize(run, community)
        super(run)
        @community = community
      end
      def perform
        @run.society.each_node do |node|
          result, uri = Cougaar::Communications::HTTP.get(node.uri+"/communityViewer")
          if result and result.include?("community=#{@community}>")
            Cougaar::Communications::HTTP.get(node.uri+"/ar?loadBalance=Load+Balance")
            return
          end
        end
      end
    end

  end

end



