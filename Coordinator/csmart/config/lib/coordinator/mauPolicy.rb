 #  Copyright 2004 Object Services and Consulting, Inc.
 #  under sponsorship of the Defense Advanced Research Projects
 #  Agency (DARPA).
 #
 #  You can redistribute this software and/or modify it under the
 #  terms of the Cougaar Open Source License as published on the
 #  Cougaar Open Source Website (www.cougaar.org).
 #
 #  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 #  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 #  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 #  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 #  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 #  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 #  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 #  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 #  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 #  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 #  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


module Cougaar

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
            @run.info_message "Requested that "+agent.name+" set MAU Policy to HighCompleteness." if @messaging >= 1
            Cougaar.logger.info "MAU Policy set to HighCompleteness at #{agent.name}"
	  end
        end
      end

    end


  end




end


