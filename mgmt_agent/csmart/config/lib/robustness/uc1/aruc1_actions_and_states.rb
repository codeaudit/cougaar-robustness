require 'rexml/document'

module Cougaar

  KillHost_CONST = "" #the host of the most currently killed node
  Comm_CONST = Hash.new(0)

  module States

    class CommunitiesReady < Cougaar::State
      DEFAULT_TIMEOUT = 60.minutes
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
		 #"com.boeing.pw.mct.exnihilo.plugin.EN4JPlugin",
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

    class DisableDeconfliction < Cougaar::Action
      def initialize(run)
        super(run)
      end
      def perform
	  @run.society.each_node do |node|
	    node.add_parameter("-Dorg.cougaar.tools.robustness.restart.deconfliction=DISABLED")
	  end
      end
    end

    class RemoveAgents < Cougaar::Action
      PRIOR_STATES = ["SocietyLoaded"]
      DOCUMENTATION = Cougaar.document {
        @description = "Remove agents from the society"
        @parameters = [
          {:agents => "required, The agents that be removed."}
        ]
        @example = "do_action 'RemoveAgents', '1-35-ARBN, GlobalAir'"
	}

      def initialize(run, *agents)
        super(run)
	@agentNames = agents
      end
      def perform
        @agentNames.each do |myAgent|
          @run.society.each_agent do |agent|
	    if agent.name == myAgent
	      node = agent.node
	      #puts "#{node.uri}/$#{node.name}/ar?operation=kill&mobileagent=#{myAgent}&orignode=#{node.name}"
	      result, uri = Cougaar::Communications::HTTP.get("#{node.uri}/$#{node.name}/ar?operation=kill&mobileagent=#{myAgent}&orignode=#{node.name}")
	    #node.remove_agent(agent)
	      break
	    end
	  end
        end
      end
    end

    class RestartAgents < Cougaar::Action
      PRIOR_STATES = ["SocietyLoaded"]
      DOCUMENTATION = Cougaar.document {
        @description = "Peform forced restart of agents"
        @parameters = [
          {:agents => "required, The agents to be restarted."}
        ]
        @example = "do_action 'RestartAgents', '1-35-ARBN, GlobalAir'"
	}

      def initialize(run, *agents)
        super(run)
	@agentNames = agents
      end
      def perform
        @agentNames.each do |myAgent|
          @run.society.each_agent do |agent|
	    if agent.name == myAgent
	      node = agent.node
	      #puts "#{node.uri}/$#{node.name}/ar?operation=restart&mobileagent=#{myAgent}&orignode=#{node.name}"
	      result, uri = Cougaar::Communications::HTTP.get("#{node.uri}/$#{node.name}/ar?operation=restart&mobileagent=#{myAgent}&orignode=#{node.name}")
	      #node.remove_agent(agent)
	      break
	    end
	  end
        end
      end
    end

   class ModifyRobustnessParameter < Cougaar::Action
      def initialize(run, attrId, attrValue, community=nil)
        super(run)
	@myAttrId = attrId
        @myAttrValue = attrValue
        @myCommunity = community
        if @myCommunity != nil
          @myCommunity = @myCommunity.sub('-COMM','')
        end
      end
      def perform
        @run.society.each_agent do |agent|
          if agent.name =~ /.*ARManager.*/
            if @myCommunity == nil || agent.name =~ /.*#{@myCommunity}.*/
	      node = agent.node
	      result, uri = Cougaar::Communications::HTTP.get("#{node.uri}/$#{node.name}/ar?operation=modcommattr&id=#{@myAttrId}&value=#{@myAttrValue}")
	    end
          end
	end
      end
    end

   # Suspend restarts by setting PING_ADJUSTMENT attribute in all Robustness communities
   # to an arbitrarily high value
   class SuspendRestarts < Cougaar::Action
      def initialize(run)
        super(run)
      end
      def perform
        @run.society.each_agent do |agent|
          if agent.name =~ /.*ARManager.*/
	    result, uri = Cougaar::Communications::HTTP.get(agent.uri+"/ar?operation=modcommattr&id=PING_ADJUSTMENT&value=100")
	    result, uri = Cougaar::Communications::HTTP.get(agent.uri+"/ar?operation=modcommattr&id=PING_TIMEOUT_THREATCON_HIGH_COEFFICIENT&value=100")
	  end
	end
      end
    end

    #Parse nodestats.xml
    class AddNodeStatisticsToCommunity < Cougaar::Action
      DOCUMENTATION = Cougaar.document {
        @description = "Add node latency statistics to robustness community"
        @parameters = [
          {:filename => "required. The name of statistics xml file."}
        ]
        @example = "do_action 'TransformStats', 'nodestat.xml'"
	}

      def initialize(run, filename)
        super(run)
	@filename = filename
      end
      def perform
        begin
	  file = File.new(@filename)
	rescue Exception #catch file not exist exception
	  puts "#{@filename} does not exist"
	end
	doc = REXML::Document.new(file)
        @run.society.communities.each do |community|
	  isRobustness = "false"
	    community.each_attribute do |id, value|
              if id == "CommunityType" && value == "Robustness"
                 isRobustness = "true"
		 break
	      else
	         isRobustness = "false"
		 break
	      end
	    end
	  #only add statics information to robustness community
	  if isRobustness == "false"
	    next
	  end
	  mean = 0
	  stddev = 0
	  high = 0
          doc.elements.each("NodeLatencyStatistics/Item") do |element|
	    parent_community = element.attributes['community']
	    if parent_community == community.name
	      name = element.attributes['node']
	      node_mean = element.attributes['mean'].to_i
	      node_stdev = element.attributes['stdev'].to_i
	      node_high = element.attributes['high'].to_i
	      if node_high > high
	        high = node_high
	        mean = node_mean
		stddev = node_stdev
	      end
	        community.each do |entity|
	          if entity.name == name
	            entity.add_attribute("STATUS_LATENCY_MEAN", node_mean)
		    entity.add_attribute("STATUS_LATENCY_STDDEV", node_stdev)
	          end
	        end
	    end # ends if parent_community == community.name
	  end # ends doc.elements.each("NodeLatencyStatistics/Item") do |element|
	  if mean > 0
	    community.remove_attribute("DEFAULT_STATUS_LATENCY_MEAN")
	    community.add_attribute("DEFAULT_STATUS_LATENCY_MEAN", mean)
	    community.remove_attribute("DEFAULT_STATUS_LATENCY_STDDEV")
	    community.add_attribute("DEFAULT_STATUS_LATENCY_STDDEV", stddev)
	  end
	end # ends @run.society.communities.each do |community|
      end
    end

  end

end



