module Cougaar 

  module Actions

    class MyFirstAction < Cougaar::Action
      RESULTANT_STATE = "MyFirstActionWasRun"
      def initialize(run, name)
        super(run)
	@name = name
        @run = run
      end
      def perform
	puts "#{@name} is cool"
      end
    end
 
    class RemoveNode < Cougaar::Action
      def initialize(run, name)
        super(run)
	@nodeName = name
      end
      def perform
        @run.society.each_node do |node|
           @temp = node.name
           puts "Node: #{@temp}"
        end
        puts "Kill #{@nodeName}"
	@run.society.nodes[@nodeName].remove
        @run.society.each_node do |node|
           @temp = node.name
           puts "Node: #{@temp}"
        end
      end
    end
  end

  module States
    class MyFirstActionWasRun < Cougaar::NOOPState
    end

    class MyFirstState < Cougaar::State
      #PRIOR_STATES = ["MyFirstActionWasRun"]
      DEFAULT_TIMEOUT = 5.seconds
      def process
        #sleep 7.seconds
	puts "States are fun!"
      end
      def unhandled_timeout
	puts "My First State timed out!!!"
      end
    end

    class CommunityReady < Cougaar::State
      DEFAULT_TIMEOUT = 30.minutes
      PRIOR_STATES = ["SocietyRunning"]
      def initialize(run, timeout=nil, &block)
        @run = run
        super(run, timeout, &block)
      end
      def process
        loop = true
        while loop
          event = @run.get_next_event
          if event.event_type=="STATUS" && event.cluster_identifier=="1AD-Enclave1-RobustnessManager" && event.component=="HealthMonitorPlugin"
            puts "COMMUNITY READY"
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
end
