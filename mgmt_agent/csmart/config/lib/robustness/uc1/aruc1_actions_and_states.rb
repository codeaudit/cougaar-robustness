module Cougaar
  
  ORIGAGENTS_CONST = Hash.new(0) #records all nodes and child agents number before node restarting
  CURRENTAGENTS_CONST = Hash.new(0) #records all nodes and child agents number after node restarting
  PORTS_CONST = Hash.new(0) #record each node and it's port address
  ALLNODES_CONST = [] #record names of all current running nodes
  KILLNODE_CONST = "" #which one is being killed right now?
  ShiftFlag_CONST = "false" #do we need remove the first node in all node list?
  Times_CONST = [] # record time before and after restarting a node  
  KillHost_CONST = "" #the host of the most currently killed node

  class CommunityController
    def initialize(run, communityName, agentsCount)
      @run = run
      @community = communityName
      @agentsCount = agentsCount
    end
    #Get all addresses of active nodes, the address will be in format host:port. this method
    #is only run once after the society is started.
    def getPorts
      @run.society.each_node do |node|
       temp = node.name
       ALLNODES_CONST[ALLNODES_CONST.length] = temp if not ALLNODES_CONST.include?(temp)
       hostName = node.host.name
       if hostName[0..4] == "host|"
          hostName[0..4] = ""
       end
       address = nil
       port = 8800
       while address == nil
         result,uri = Cougaar::Communications::HTTP.get("http://#{hostName}:#{port}/agents?format=text")
         begin
           result.each_line do |line| 
	     if line.strip == temp
               address = "#{hostName}:#{port}"
               break
             end
           end
         rescue NameError
           next
         end
         port += 1
       end
       PORTS_CONST[temp] = address
     end
     if PORTS_CONST.length != ALLNODES_CONST.length
       sleep 2.seconds
       getPorts 
     end
     ALLNODES_CONST.delete("RobustnessManagerNode") if ALLNODES_CONST.include?("RobustnessManagerNode")
    end

    #This method checked communityViewer servlet of every agent to test if the agents in given
    #community equals the given number. Yes means the community is ready.
    def isCommunityReady
      @currentAgents = 0
      if KILLNODE_CONST.length != 0
        if PORTS_CONST.has_key?(KILLNODE_CONST)
          PORTS_CONST.delete(KILLNODE_CONST) 
          puts "delete #{KILLNODE_CONST} from PORTS_CONST"
        end
      end
      PORTS_CONST.each do |key, value|
        if !(value.include?":") #this is a new node, try to get it's port address
          puts "meet new node: #{key}"
          sleep 1.minutes
          address = nil
          port = "8800"
          while address == nil
            temp = "#{value}:#{port}"
            if PORTS_CONST.has_value?(temp)
              tport = port.to_i + 1
              port = tport.to_s
              address = nil
	    else
              begin 
                result,uri = Cougaar::Communications::HTTP.get("http://#{temp}/agents?format=text")
                address = temp
                PORTS_CONST[key] = address
                value = address
                break
              rescue
                puts "try to get port of #{key} failed"
                sleep 5.seconds
              end
            end
          end
        end # end if !(value.include?":")
        result,uri = Cougaar::Communications::HTTP.get("http://#{value}/agents?format=text")
        begin
          result.each_line do |line|
	    temp = line.strip
	    if temp != key
              result2,uri2 = Cougaar::Communications::HTTP.get("http://#{value}/$#{temp}/communityViewer")            
	      result2.each_line do |line2|
		html = line2.strip
                if html.include?"community="
		  index = html.index('community=')
                  html[0, index+10] = ""
                  index = html.index('>')
                  html[index, html.length] = ""
                  if html == @community
		    @currentAgents += 1
                    if @currentAgents >= @agentsCount
                      if ShiftFlag_CONST == "true"
  			one = ALLNODES_CONST.shift
                        if one == "NAME_SERVER"
                           ALLNODES_CONST.shift
                        end
                        ShiftFlag_CONST[0..ShiftFlag_CONST.length] = "false"
		      end
                      #KILLNODE_CONST[0..KILLNODE_CONST.length] = ALLNODES_CONST[0]
		      return true
		    end
		  end
		end
	      end
            end
          end
        rescue NameError
          next
        end
      end
      return false
    end
  end
  

  module States
    class REAR_Community_Ready < Cougaar::State
      DEFAULT_TIMEOUT = 30.minutes
      PRIOR_STATES = ["SocietyRunning"]
      def initialize(run, timeout=nil, &block)
        super(run, timeout, &block)
      end
      def process
        controller = ::Cougaar::CommunityController.new(run, "1AD-REAR-COMM", 18)
        if PORTS_CONST.length == 0
           sleep 3.minutes
           controller.getPorts
        end
        #result = controller.isCommunityReady
        while !(controller.isCommunityReady)
          sleep 5.seconds
        end
        #str = ALLNODES_CONST.shift
      end
      def unhandled_timeout
        @run.do_action "StopSociety" 
        @run.do_action "StopCommunications"
      end
    end

    class FWD_Community_Ready < Cougaar::State
      DEFAULT_TIMEOUT = 30.minutes
      PRIOR_STATES = ["SocietyRunning"]
      def initialize(run, timeout=nil, &block)
        super(run, timeout, &block)
      end
      def process
        controller = ::Cougaar::CommunityController.new(run, "1AD-FWD-COMM", 14)
        if PORTS_CONST.length == 0
           sleep 2.minutes
           controller.getPorts
        end
        #result = controller.isCommunityReady
        while !(controller.isCommunityReady)
          sleep 5.seconds
        end
        #str = ALLNODES_CONST.shift
      end
      def unhandled_timeout
        @run.do_action "StopSociety" 
        @run.do_action "StopCommunications"
      end
    end

    class CONUS_Community_Ready < Cougaar::State
      DEFAULT_TIMEOUT = 30.minutes
      PRIOR_STATES = ["SocietyRunning"]
      def initialize(run, timeout=nil, &block)
        super(run, timeout, &block)
      end
      def process
        controller = ::Cougaar::CommunityController.new(run, "1AD-CONUS-COMM", 3)
        if PORTS_CONST.length == 0
           sleep 2.minutes
           controller.getPorts
        end
        #result = controller.isCommunityReady
        while !(controller.isCommunityReady)
          sleep 5.seconds
        end
        #str = ALLNODES_CONST.shift
      end
      def unhandled_timeout
        @run.do_action "StopSociety" 
        @run.do_action "StopCommunications"
      end
    end

    class TRANS_Community_Ready < Cougaar::State
      DEFAULT_TIMEOUT = 30.minutes
      PRIOR_STATES = ["SocietyRunning"]
      def initialize(run, timeout=nil, &block)
        super(run, timeout, &block)
      end
      def process
        controller = ::Cougaar::CommunityController.new(run, "1AD-TRANS-COMM", 7)
        if PORTS_CONST.length == 0
           sleep 2.minutes
           controller.getPorts
        end
        #result = controller.isCommunityReady
        while !(controller.isCommunityReady)
          sleep 5.seconds
        end
        #str = ALLNODES_CONST.shift
      end
      def unhandled_timeout
        @run.do_action "StopSociety" 
        @run.do_action "StopCommunications"
      end
    end

    class Robustness_Community_Ready < Cougaar::State
      DEFAULT_TIMEOUT = 30.minutes
      PRIOR_STATES = ["SocietyRunning"]
      def initialize(run, timeout=nil, &block)
        super(run, timeout, &block)
      end
      def process
        controller = ::Cougaar::CommunityController.new(run, "1AD-ROBUSTNESS-COMM", 42)
        if PORTS_CONST.length == 0
           sleep 3.minutes
           controller.getPorts
        end
        #result = controller.isCommunityReady
        while !(controller.isCommunityReady)
          sleep 5.seconds
        end
        #str = ALLNODES_CONST.shift
      end
      def unhandled_timeout
        @run.do_action "StopSociety" 
        @run.do_action "StopCommunications"
      end
    end
  end

  module Actions
    class ShowResult < Cougaar::Action
      PRIOR_STATES = ["SocietyRunning"]
      def initialize(run, *nodes)
        super(run)
        @run = run
        @nodes = nodes
        $end = Time.new
        #print "end time: ", $end.to_i, "\n"
      end
      def perform 
        Times_CONST[1] = Time.new 
        ShiftFlag_CONST[0..ShiftFlag_CONST.length] = "true"
        puts "------------------------------------------"
        if Times_CONST[0] != nil && Times_CONST[1] != nil
          print "  Restart total time: ", Times_CONST[1].to_i - Times_CONST[0].to_i, " seconds\n"
        end
        countOrigNodes if ORIGAGENTS_CONST.length == 0  
        countCurrentNodes
        @origtotal = 0
        ORIGAGENTS_CONST.each_value {|value| @origtotal += value}
#ORIGAGENTS_CONST.each {|key, value| print key, " is ", value, "\n"}
#puts "original total is: #{@origtotal}"
        @currenttotal = 0
        CURRENTAGENTS_CONST.each_value {|value| @currenttotal += value}
#CURRENTAGENTS_CONST.each {|key, value| print key, " is ", value, "\n"}
#puts "current total is: #{@currenttotal}"
        @removeAgentsCount = 0
        @nodes.each do |removenode|
#ORIGAGENTS_CONST.each {|key, value| print key, " is ", value, "\n"}
#puts "remove: #{removenode}"
          @removeAgentsCount += ORIGAGENTS_CONST[removenode]
	end
        restartFails = @origtotal - @currenttotal
        restartFails = 0 if restartFails < 0
        restartSucceeds = @removeAgentsCount - restartFails
        print "  Restarts performed: ", restartSucceeds, "\n"
        print "  Restarts failed:    ", restartFails, "\n"
        puts "  Agents count by node:"
        PORTS_CONST.each do |key, value|
          current = CURRENTAGENTS_CONST[key]
          print "    ", key, "   ", ORIGAGENTS_CONST[key], "(original)", "   ", current, "(after restart)\n"
        end
        puts "------------------------------------------"
        ORIGAGENTS_CONST.clear
        CURRENTAGENTS_CONST.each {|key, value| ORIGAGENTS_CONST[key] = value}
        if ALLNODES_CONST[0] == "NAME_SERVER"
          ALLNODES_CONST.shift
        end
        KILLNODE_CONST[0..KILLNODE_CONST.length] = ALLNODES_CONST[0]
	temp = PORTS_CONST[KILLNODE_CONST]
        index = temp.index(':')
        KillHost_CONST[0..KillHost_CONST.length] = temp[0..index-1]
        Times_CONST[0] = Time.new
      end

      def countOrigNodes
        @run.society.each_node do |node|
	  temp = node.name
          ORIGAGENTS_CONST[temp] = node.agents.length
        end
      end

      def countCurrentNodes
        CURRENTAGENTS_CONST.clear
        PORTS_CONST.each do |key, value|
          begin
            result,uri = Cougaar::Communications::HTTP.get("http://#{value}/agents?format=text")
          rescue
	    print "http://#{value}/agents?format=text", " can't be reached.\n"
	    next
	  end
          @agentCount = 0
          begin
            result.each_line do |line| 
	      temp = line.strip
              if temp != key
                @agentCount += 1
 	      end
            end
          rescue NameError
	    next
	  end
          CURRENTAGENTS_CONST[key] = @agentCount
        end
      end
    end


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
                 "org.cougaar.tools.robustness.ma.ui.ARServlet"]
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

    class DiscoverKillNode < Cougaar::Action
      def initialize(run, communityName)
        super(run)
        @community = communityName
      end
      def perform
        flag = "false"
        PORTS_CONST.each do |key, value|
          if !(value.include?":")
            next
          end
          result,uri = Cougaar::Communications::HTTP.get("http://#{value}/agents?format=text")
         begin
          result.each_line do |line|
	    temp = line.strip
	    if temp == key
              result2,uri2 = Cougaar::Communications::HTTP.get("http://#{value}/$#{temp}/communityViewer")            
	      result2.each_line do |line2|
		html = line2.strip
                if html.include?"community="
		  index = html.index('community=')
                  html[0, index+10] = ""
                  index = html.index('>')
                  html[index, html.length] = ""
                  if html == @community
                    KILLNODE_CONST[0..KILLNODE_CONST.length] = key
                    ALLNODES_CONST.delete(key)
                    flag = "true"
                    break
		  end
		end
	      end
              break if flag == "true"
            end
          end
         rescue NameError
          next
         end
         break if flag == "true"
       end
      end
    end

    class NewClearPersistenceAndLog < Cougaar::Action
      DEFAULT_TIMEOUT = 30.minutes
      def initialize(run, host="")
        super(run)
        @host = host
      end
      def perform
        @tmp = @host + "/"
        if @host == nil
          @tmp = ""
        end
        log = "$COUGAAR_INSTALL_PATH/workspace/" + @tmp + "log4jlogs/*"
        rmLog = %Q[rm -rf #{log}]
        persistence = "$COUGAAR_INSTALL_PATH/workspace/" + @tmp + "P/*"
        rmPersistence = %Q[rm -rf #{persistence}]
        `#{rmLog}`
        `#{rmPersistence}`
      end
    end
    
  end
end



