
module Cougaar

  Mgrs = Hash.new(0)

  module Actions

    class Set_MAU_Normal < Cougaar::Action
      PRIOR_STATES = []
      DOCUMENTATION = Cougaar.document {
        @description = "Set MAU weights to Normal."
        @parameters = [
	    {:messaging => "0 is no messages (the default), 1 is normal messages, 2 is verbose."}
        ]
        @example = "do_action 'Set_MAU_Normal', 1" 
	}
    
      def initialize(run, messaging=0)
        super(run)
        @messaging = messaging
      end

      def perform 
        @run.society.each_agent do |agent|
          if agent.name =~ /.*ARManager.*/
	    @run.info_message "MAUPolicy found #{agent.name}" if @messaging >= 2
            url = "#{agent.uri}/MAUPolicy?Normal=Normal"
	    response, uri = Cougaar::Communications::HTTP.get(url)
            raise "Could not connect to #{url}" unless response
            Mgrs[agent.name] = agent
            @run.info_message "Requested that "+agent.name+" set MAU Policy to Normal." if @messaging >= 1
            Cougaar.logger.info "MAU Policy set to Normal at #{agent.name}"
	  end
        end
      end

    end

    class Set_MAU_HighSecurity < Cougaar::Action
      PRIOR_STATES = []
      DOCUMENTATION = Cougaar.document {
        @description = "Set MAU weights to HighSecurity."
        @parameters = [
	    {:messaging => "0 is no messages (the default), 1 is normal messages, 2 is verbose."}
        ]
        @example = "do_action 'Set_MAU_HighSecurity', 1" 
	}
    
      def initialize(run, messaging=0)
        super(run)
        @messaging = messaging
      end

      def perform 
        @run.society.each_agent do |agent|
          if agent.name =~ /.*ARManager.*/
	    @run.info_message "MAUPolicy found #{agent.name}" if @messaging >= 2
            url = "#{agent.uri}/MAUPolicy?HighSecurity=HighSecurity"
	    response, uri = Cougaar::Communications::HTTP.get(url)
            raise "Could not connect to #{url}" unless response
            Mgrs[agent.name] = agent
            @run.info_message "Requested that "+agent.name+" set MAU Policy to HighSecurity." if @messaging >= 1
            Cougaar.logger.info "MAU Policy set to HighSecurity at #{agent.name}"
	  end
        end
      end

    end


    class Set_MAU_HighCompleteness < Cougaar::Action
      PRIOR_STATES = []
      DOCUMENTATION = Cougaar.document {
        @description = "Set MAU weights to HighCompleteness."
        @parameters = [
	    {:messaging => "0 is no messages (the default), 1 is normal messages, 2 is verbose."}
        ]
        @example = "do_action 'Set_MAU_HighCompleteness', 1" 
	}
    
      def initialize(run, messaging=0)
        super(run)
        @messaging = messaging
      end

      def perform 
        @run.society.each_agent do |agent|
          if agent.name =~ /.*ARManager.*/
	    @run.info_message "MAUPolicy found #{agent.name}" if @messaging >= 2
            url = "#{agent.uri}/MAUPolicy?HighCompleteness=HighCompleteness"
	    response, uri = Cougaar::Communications::HTTP.get(url)
            raise "Could not connect to #{url}" unless response
            Mgrs[agent.name] = agent
            @run.info_message "Requested that "+agent.name+" set MAU Policy to HighCompleteness." if @messaging >= 1
            Cougaar.logger.info "MAU Policy set to HighCompleteness at #{agent.name}"
	  end
        end
      end

    end


  end




end


