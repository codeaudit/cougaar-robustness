
module Cougaar

  Mgrs = Hash.new(0)

  module States

    class Unleashed < Cougaar::State
      DEFAULT_TIMEOUT = 3.minutes
      PRIOR_STATES = ["SocietyRunning"]
      DOCUMENTATION = Cougaar.document {
        @description = "Wait until all the Robustness Managers have Unleashed Defenses."
        @parameters = [
	    {:messaging => "0 is no messages (the default), 1 is normal messages, 2 is verbose."}
        ]
        @example = "wait_for 'Unleashed'"
	}
      def initialize(run, messaging=0, timeout=3.minutes, &block)
        super(run, timeout, &block)
	@messaging = messaging
      end
      def process
        loop = true
        while loop
          event = @run.get_next_event
          Mgrs.delete_if { |mgr_name,mgr| event.data.include?("#{mgr_name}: Finished Handling Unleashing") }
          if Mgrs.size==0
            loop = false
          end
        end
        @run.info_message "All Robustness Managers have Unleashed Defenses." if @messaging >= 1
      end
    end

  end

  module Actions
    class Unleash < Cougaar::Action
      PRIOR_STATES = []
      DOCUMENTATION = Cougaar.document {
        @description = "Allow Defenses to run normally."
        @parameters = [
	    {:messaging => "0 is no messages (the default), 1 is normal messages, 2 is verbose."}
        ]
        @example = "do_action 'Unleash'" 
	}
    
      def initialize(run, messaging=0)
        super(run)
        @messaging = messaging
      end

      def perform 
	started = 0
	@run['FinishedUnleashed'] = {}
        @run.society.each_agent do |agent|
          if agent.name =~ /.*ARManager.*/
	    @run.info_message "Unleash found #{agent.name} on #{agent.node.name}" if @messaging >= 2
            started = started + 1
	  end
        end
        @run.comms.on_cougaar_event do |event|
          if event.data.include?("Finished Handling Unleashing")
            node = event.node
            @run['FinishedUnleashed'][node] = node
            @run.info_message "Defenses Unleashed on "+node if @messaging >= 1
          end
        end
        @run.society.each_agent do |agent|
          if agent.name =~ /.*ARManager.*/
	    @run.info_message "Unleash found #{agent.name}" if @messaging >= 2
            url = "#{agent.uri}/LeashDefenses?UnleashDefenses=UnleashDefenses"
	    response, uri = Cougaar::Communications::HTTP.get(url)
            raise "Could not connect to #{url}" unless response
            @run.info_message "Requested that "+agent.name+" on "+agent.node.name+" Unleash Defenses." if @messaging >= 1
            Cougaar.logger.info "Unleash requested at #{agent.name}"
	  end
        end
        while @run['FinishedUnleashed'].length < started
          next
        end
        @run.info_message "All Robustness Managers have Unleashed Defenses." if @messaging >= 1
      end

    end

    class Leash < Cougaar::Action
      PRIOR_STATES = []
      DOCUMENTATION = Cougaar.document {
        @description = "Stop Permitting Defense Actions.  This Action is reversed by Unleash."
        @parameters = [
	    {:messaging => "0 is no messages (the default), 1 is normal messages, 2 is verbose."}
        ]
        @example = "do_action 'Leash', 1" 
	}
    
      def initialize(run, messaging=0)
        super(run)
        @messaging = messaging
      end

      def perform 
        @run.society.each_agent do |agent|
          if agent.name =~ /.*ARManager.*/
	    @run.info_message "Leash found #{agent.name}" if @messaging >= 2
            url = "#{agent.uri}/LeashDefenses?LeashDefenses=LeashDefenses"
	    response, uri = Cougaar::Communications::HTTP.get(url)
            raise "Could not connect to #{url}" unless response
            Mgrs[agent.name] = agent
            @run.info_message "Requested that "+agent.name+" Leash Defenses." if @messaging >= 1
            Cougaar.logger.info "Leash requested at #{agent.name}"
	  end
        end
      end

    end

    class MonitorUnleash < Cougaar::Action
      PRIOR_STATES = ["SocietyLoaded"]
      DOCUMENTATION = Cougaar.document {
        @description = "Print event when Defenses have been Unleashed by Robustness Manager."
        @parameters = [
	    {:messaging => "0 is no messages (the default), 1 is normal messages, 2 is verbose."}
        ]
        @example = "do_action 'MonitorUnleash'"
	}
      def initialize(run, messaging=0)
        super(run)
        @messaging = messaging
      end

      def perform
        @run.comms.on_cougaar_event do |event|
          #@run.info_message event.data
          if event.data.include?("Finished Handling Unleashing")
            @run.info_message event.node+": Defenses Unleashed." if @messaging >= 1
          end
        end
      end
    end

    class LeashOnRestart <  Cougaar::Action
      DOCUMENTATION = Cougaar.document {
        @description = "Leash defenses when restarting, intended to be used when restarting a full society from persistence."
        @parameters = [
        ]
        @example = "do_action 'LeashOnRestart'"
      }

      def initialize(run)
        super(run)
      end

      def perform()
        @run.society.each_node do |node|
          if node.has_facet?("role")
            node.each_facet("role") do |facet|
              if facet["role"] == "AR-Management"
                node.replace_parameter(/Dorg.cougaar.coordinator.leashOnRestart/, 
                                       "-Dorg.cougaar.coordinator.leashOnRestart=true")
              end
            end
          end
        end
      end
      
    end

    class UnleashOnSubsequentRestarts < Cougaar::Action
      PRIOR_STATES = []
      DOCUMENTATION = Cougaar.document {
        @description = "Unleash Defenses on subsequent Restarts. Intended to be called after Unleash."
        @parameters = [
	    {:messaging => "0 is no messages (the default), 1 is normal messages, 2 is verbose."}
        ]
        @example = "do_action 'UnleashOnSubsequentRestarts'" 
	}
    
      def initialize(run, messaging=0)
        super(run)
        @messaging = messaging
      end

      def perform 
        @run.society.each_node do |node|
	    @run.info_message "UnleashOnSubsequentRestarts found #{node.name}" if @messaging >= 2
            url = "#{node.uri}/LeashOnRestart?UnleashOnRestart=UnleashOnRestart"
	    response, uri = Cougaar::Communications::HTTP.get(url)
            raise "Could not connect to #{url}" unless response
            @run.info_message "Requested that "+node.name+" Unleash Defenses on Subsequent Restarts." if @messaging >= 2
            Cougaar.logger.info "UnleashOnSubsequentRestarts requested at #{node.name}"
        end
      end

    end



  end




end


