module Cougaar 
  module States

    class Enclave1-Ready < Cougaar::State
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

    class Enclave2-Ready < Cougaar::State
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
          if event.event_type=="STATUS" && event.cluster_identifier=="1AD-Enclave2-RobustnessManager" && event.component=="HealthMonitorPlugin"
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

    class Enclave3-Ready < Cougaar::State
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
          if event.event_type=="STATUS" && event.cluster_identifier=="1AD-Enclave3-RobustnessManager" && event.component=="HealthMonitorPlugin"
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

    class Enclave4-Ready < Cougaar::State
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
          if event.event_type=="STATUS" && event.cluster_identifier=="1AD-Enclave4-RobustnessManager" && event.component=="HealthMonitorPlugin"
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
