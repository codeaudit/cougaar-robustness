
module Cougaar

  module Actions
    class UnleashDefenses < Cougaar::Action
      PRIOR_STATES = []
      DOCUMENTATION = Cougaar.document {
        @description = "Allow Defenses to run normally."
        @parameters = [
	    {:messaging => "0 is no messages (the default), 1 is normal messages, 2 is verbose."}
        ]
        @example = "do_action 'UnleashDefenses'" 
	}
    
      def initialize(run, messaging=0)
        super(run)
        @messaging = messaging
      end

      def perform 
        @run.society.each_agent do |agent|
          #@run.info_message "Agent found #{agent.name}"
          if agent.name =~ /.*ARManager.*/
	    @run.info_message "UnleashDefenses found #{agent.name}" if @messaging >= 2
            url = "#{agent.uri}/LeashDefenses?UnleashDefenses=UnleashDefenses"
	    response, uri = Cougaar::Communications::HTTP.get(url)
            raise "Could not connect to #{url}" unless response
            Cougaar.logger.info "UnleashedDefenses by calling #{agent.name}"
	  end
        end
      end

    end

    class MonitorUnleashConfirmed < Cougaar::Action
      PRIOR_STATES = ["SocietyLoaded"]
      DOCUMENTATION = Cougaar.document {
        @description = "Print event when Defenses have been Unleashed by Robustness Manager."
        @parameters = [
	    {:messaging => "0 is no messages (the default), 1 is normal messages, 2 is verbose."}
        ]
        @example = "do_action 'MonitorUnleashConfirmed'"
	}
      def initialize(run, messaging=0)
        super(run)
        @messaging = messaging
      end

      def perform
        @run.comms.on_cougaar_event do |event|
          #@run.info_message event.data
          if event.data.include?("Defenses Unleashed")
            @run.info_message event.data + " - Ready to go" if @messaging >= 1
          end
        end
      end
    end

    class NotifyRestart <  Cougaar::Action
      DOCUMENTATION = Cougaar.document {
        @description = "Leash defenses when restarting, intended to be used when restarting a full society from persistence."
        @parameters = [
        ]
        @example = "do_action 'NotifyRestart'"
      }

      def initialize(run)
        super(run)
      end

      def perform()
        @run.society.each_node do |node|
          if node.has_facet?("role")
            node.each_facet("role") do |facet|
              if facet["role"] == "AR-Management"
                node.replace_parameter(/Dorg.cougaar.tools.robustness.deconfliction.leashOnRestart/, 
                                       "-Dorg.cougaar.tools.robustness.deconfliction.leashOnRestart=true")
              end
            end
          end
        end
      end
      
    end

  end


end


