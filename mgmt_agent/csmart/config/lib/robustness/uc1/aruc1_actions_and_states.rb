module Cougaar

  ORIGAGENTS_CONST = Hash.new(0) #records all nodes and child agents number before node restarting
  CURRENTAGENTS_CONST = Hash.new(0) #records all nodes and child agents number after node restarting
  PORTS_CONST = Hash.new(0) #record each node and it's port address
  ALLNODES_CONST = [] #record names of all current running nodes
  KILLNODE_CONST = "" #which one is being killed right now?
  ShiftFlag_CONST = "false" #do we need remove the first node in all node list?
  Times_CONST = [] # record time before and after restarting a node
  KillHost_CONST = "" #the host of the most currently killed node
  LoadBalancer_CONST = "false"
  Comm_CONST = Hash.new(0)

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
          @community_names.delete_if { |name| event.data.include?("Community #{name} Ready") }
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

    class SaveHostOfNode < Cougaar::Action
      def initialize(run, communityName, *nodes)
        super(run)
	@commName = communityName
	@nodes = nodes
      end
      def perform
	@nodes.each do |mynode|
	@run.society.each_node do |node|
          if node.name == mynode
			   #KillHost_CONST[0..KillHost_CONST.length] = node.host.name
	    host = node.host.name
	    if Comm_CONST.has_key?(@commName)
	      hosts = Comm_CONST[@commName]
	      hosts.push(host)
	      break
	    end
	    hosts = []
	    hosts.push(host)
	    Comm_CONST[@commName] = hosts
	  end
        end
        end
       #Comm_CONST.each {|key, value| puts "#{key} is #{value}"}
      end
    end

    class AddNode < Cougaar::Action
      def initialize(run, nodeName, communityName, hostName=nil)
        super(run)
	@community = communityName
        @hostName = hostName
	@nodeName = nodeName
      end
      def perform
        if @hostName == nil
	  @hostName = getHost(@community)
	  #puts "find host: #{@hostName}"
	end
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
	     cip = getCIP()
	     newNode.override_parameter("-Dorg.cougaar.core.logging.log4j.appender.SECURITY.File","#{cip}/workspace/log4jlogs/#{@nodeName}.log")
	     
             post_node_xml(newNode)
             msg_body = @nodeName + ".rb"
             result = @run.comms.new_message(host).set_body("command[start_xml_node]#{msg_body}").request(120)
	     if result.nil?
               puts "Could not start node #{@nodeName} on host #{@hostName}"
             else
               puts "start node #{@nodeName} successfully on host #{@hostName}"
	       #ORIGAGENTS_CONST[@nodeName] = 0
               #ORIGAGENTS_CONST.each {|key, value| print key, " is ", value, "\n"}
               #ALLNODES_CONST[ALLNODES_CONST.length] = @nodeName
               #PORTS_CONST[@nodeName] = @hostName
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
      def getCIP()
        @run.society.each_node do |node|
	  node.each_parameter do |parameter|
	    if parameter.include? "-Dorg.cougaar.install.path"
	      index = parameter.index('=')
	      cip = parameter[index+1..parameter.length-1]
	      return cip
	    end
	  end
	end
      end
      def getHost(community)
        if Comm_CONST.has_key?(community)
	  hosts = Comm_CONST[community]
	  host = hosts.shift
	  if hosts.length == 0
	    Comm_CONST.delete(community)
	  end
	  return host
	end
	puts "No host avaliable for #{community}"
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
          result, uri = Cougaar::Communications::HTTP.get(node.uri+"/$"+node.name+"/communityViewer")
          if result and result.include?("community=#{@community}>")
			 print node.uri, "/$", node.name, "/ar?loadBalance=Load+Balance", "\n"
            Cougaar::Communications::HTTP.get(node.uri+"/$"+node.name+"/ar?loadBalance=Load+Balance")
            return
          end
        end
      end
    end
    
    class AddTestNode < Cougaar::Action
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
                 "org.cougaar.core.mobility.service.RedirectMovePlugin"]
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
	     #newNode.override_parameter("-Dorg.cougaar.core.logging.log4j.appender.SECURITY.File","/shares/development/cougaar-b/workspace/log4jlogs/#{@nodeName}.log")

	     
             post_node_xml(newNode)
             msg_body = @nodeName + ".rb"
             result = @run.comms.new_message(host).set_body("command[start_xml_node]#{msg_body}").request(120)
	     if result.nil?
               puts "Could not start node #{@nodeName} on host #{@hostName}"
             else
               #puts "start node #{@nodeName} successfully on host #{@hostName}: #{result}"
	       ORIGAGENTS_CONST[@nodeName] = 0
               #ORIGAGENTS_CONST.each {|key, value| print key, " is ", value, "\n"}
               #ALLNODES_CONST[ALLNODES_CONST.length] = @nodeName
               #PORTS_CONST[@nodeName] = @hostName
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

	 class PublishThreatAlert < Cougaar::Action
	   def initialize(run, community, role, alertLevel, interval, assets)
		  super(run)
		  @community = community
		  @role = role
		  @alertLevel = alertLevel
		  @interval = interval
		  @assets = assets
		end
		def perform
		  start = Time.new.to_f
		  expire = start + @interval
		  str = ""
		  i = 1
		  @assets.each do |key, value|
		    str << "type#{i}=#{key}&id#{i}=#{value}&"
			 i = i + 1
		  end
		  temp = str.chop
		  @run.society.each_node do |node|
		    result, uri = Cougaar::Communications::HTTP.get(node.uri+"/$"+node.name+"/alert?community=#{@community}&role=#{@role}&level=#{@alertLevel}&startFromAction=#{start}&expireFromAction=#{expire}&submit=submit&#{temp}")
		   # puts "#{node.uri}/$#{node.name}/alert?community=#{@community}&role=#{@role}&level=#{@alertLevel}&startFromAction=#{start}&expireFromAction=#{expire}&#{temp}"
			 return
		  end
		end
	 end

  end

end



