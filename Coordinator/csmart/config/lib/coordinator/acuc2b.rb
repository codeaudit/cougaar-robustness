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

    class SetOutsideLoad < Cougaar::Action
      PRIOR_STATES = []
      DOCUMENTATION = Cougaar.document {
        @description = "Set OutsideLoadDiagnosis to None, Moderate, or High in a specific enclave."
        @parameters = [
	    {:enclave => "Name of the Enclave (e.g. 1-UA)"},
	    {:value => 	"None, Moderate, or High."},
            {:messaging => "0 is no messages (the default), 1 is normal messages, 2 is verbose."}
        ]
        @example = "do_action 'SetOutsideLoad', '1-UA', 'High', 1" 
	}
    
      def initialize(run, enclave, value, messaging=0)
        super(run)
        @enclave = enclave
        @value = value
        @messaging = messaging
      end

      def perform 
        @run.info_message "Setting OutsideLoadDiagnosis to "+@value+" for "+@enclave+" enclave" if @messaging >= 1
        armgr = @enclave+"-ARManager"
        @run.society.each_agent do |agent|
          if agent.name == armgr
            url = "#{agent.uri}/OutsideLoadServlet?"+@value+"="+@value
	    @run.info_message "Calling "+url if @messaging >= 2
	    response, uri = Cougaar::Communications::HTTP.get(url)
            raise "Could not connect to #{url}" unless response
            Cougaar.logger.info "OutsideLoadDiagnosis set to "+@value+" at "+armgr
	  end
        end
      end
    end

    class SetAvailableBandwidth < Cougaar::Action
      PRIOR_STATES = []
      DOCUMENTATION = Cougaar.document {
        @description = "Set AvailableBandwidthDiagnosis to Low, Moderate, or High in a specific enclave."
        @parameters = [
	    {:enclave => "Name of the Enclave (e.g. 1-UA)"},
	    {:value => 	"Low, Moderate, or High."},
            {:messaging => "0 is no messages (the default), 1 is normal messages, 2 is verbose."}
        ]
        @example = "do_action 'SetAvailableBandwidth', '1-UA', 'Low', 1" 
	}
    
      def initialize(run, enclave, value, messaging=0)
        super(run)
	@enclave = enclave
        @value = value
        @messaging = messaging
      end

      def perform 
        @run.info_message "Setting AvailableBandwidthDiagnosis to "+@value+" for "+@enclave+" enclave" if @messaging >= 1
        armgr = @enclave+"-ARManager"
        @run.society.each_agent do |agent|
          if agent.name == armgr
            url = "#{agent.uri}/AvailableBandwidthServlet?"+@value+"="+@value
	    @run.info_message "Calling "+url if @messaging >= 2
	    response, uri = Cougaar::Communications::HTTP.get(url)
            raise "Could not connect to #{url}" unless response
            Cougaar.logger.info "AvailableBandwidthDiagnosis set to "+@value+" at "+armgr 
	  end
        end
      end
    end

    class SetThreatcon < Cougaar::Action
      PRIOR_STATES = []
      DOCUMENTATION = Cougaar.document {
        @description = "Set Threatcon level to Low or High in a specific enclave."
        @parameters = [
	    {:enclave => "Name of the Enclave (e.g. 1-UA)"},
	    {:value => 	"Low or High."},
            {:messaging => "0 is no messages (the default), 1 is normal messages, 2 is verbose."}
        ]
        @example = "do_action 'SetThreatcon', '1-UA', 'High', 1" 
	}
    
      def initialize(run, enclave, value, messaging=0)
        super(run)
	@enclave = enclave
        @value = value
        @messaging = messaging
      end

      def perform 
        @run.info_message "Setting Threatcon to "+@value+" for "+@enclave+" enclave" if @messaging >= 1
        mnr_mgr = @enclave.capitalize+"MonitoringManager"
        modeName = "org.cougaar.core.security.monitoring.LOGIN_FAILURE_RATE"
        modeValue = "100000"
        if @value == "Low"
            modeValue = "1"
        end
        @run.society.each_agent do |agent|
          if agent.name == mnr_mgr
            url = "#{agent.uri}/sendOperatingModeServlet?agent="+mnr_mgr+",modeName="+modeName+",modeValue="+modeValue
	    @run.info_message "Calling "+url if @messaging >= 2
	    response, uri = Cougaar::Communications::HTTP.get(url)
            raise "Could not connect to #{url}" unless response
            Cougaar.logger.info "SetThreatcon set to "+@value+" at "+mnr_mgr
	  end
        end
      end
    end

    class MonitorActionSelection < Cougaar::Action
      DOCUMENTATION = Cougaar.document {
        @description = "Prints out a new Action value is selected."
        @parameters = [
           {:messaging => "0 for no messages (the default), 1 for only error messages, 2 for info messages, 3 for everything."}
        ]
        @example = "do_action 'MonitorActionSelection', 1"
      }
              
      def initialize(run, messaging=0)
        super(run)
        @messaging = messaging
      end

      def perform
        @run.comms.on_cougaar_event do |event|
          if event.component == "ActionSelectionPlugin"
            @run.info_message event.data if @messaging >= 1
          end   
        end
      end
    end

  end
end


