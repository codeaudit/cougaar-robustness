module Cougaar

  module Actions

    class PublishThreatAlert < Cougaar::Action
      DOCUMENTATION = Cougaar.document {
        @description = "Publish threat alerts"
        @parameters = [
          {:classname => "required, ThreatAlert classname."},
	  {:targetType => "required, type of target, must be one of the following: community, agent"},
          {:targetName => "required, name of destination."},
	  {:role => "required if target is a community, community member role to receive alerts."},
	  {:alertLevel => "required, alert level, must be one of the following: maximum, high, medium, low, minimum, undefined"},
	  {:duration => "required,alert duration."},
	  {:assets => "required, affected assets. Example of assets: assets={'node'=>'TRANS-NODE, FWD-NODE', 'host'=>'net1'}"}        ]
        @example = "do_action 'PublishThreatAlert', 'org.cougaar.tools.robustness.ma.HostLossThreatAlert', 'community', '1AD-SMALL-COMM', 'HealthMonitor', 'low', 10.minutes, assets"      }
      def initialize(run, classname, type, target, alertLevel, duration, assets, role=nil)
        super(run)
        @classname = classname
	@type = type
	if @type == "agent"
	  @agent = target
	else
          @community = target
          @role = role
	end
        @alertLevel = alertLevel
        @duration = duration
        @assets = assets
      end

      def perform
        start = Time.new.to_f
        expire = start + @duration
        str = ""
        i = 1
	temp = ""
        @assets.each do |key, value|
           if value.include? ","
             temp = ""
             value.each(',') {|s|
               s = s.chomp(",")
               s = s.strip
               str << "type#{i}=#{key}&id#{i}=#{s}&"
               i = i + 1
             }
          else
            str << "type#{i}=#{key}&id#{i}=#{value}&"
            i = i + 1
          end
          temp = str.chop
	end
        @run.society.each_node do |node|
	   if @type=="agent"
            result, uri = Cougaar::Communications::HTTP.get(node.uri+"/$"+node.name+"/alert?class=#{@classname}&inputagent=#{@agent}&level=#{@alertLevel}&startFromAction=#{start}&expireFromAction=#{expire}&submit=submit&#{temp}")
            return
	   else
	    result, uri = Cougaar::Communications::HTTP.get(node.uri+"/$"+node.name+"/alert?class=#{@classname}&inputcommunity=#{@community}&inputrole=#{@role}&level=#{@alertLevel}&startFromAction=#{start}&expireFromAction=#{expire}&submit=submit&#{temp}")
            return
	   end
 #puts "#{node.uri}/$#{node.name}/alert?class=#{@classname}&inputcommunity=#{@community}&inputrole=#{@role}&level=#{@alertLevel}&startFromAction=#{start}&expireFromAction=#{expire}&#{temp}"
        end
      end
   end

   class PublishInterAgentOperatingMode < Cougaar::Action
     def initialize(run, agent, level)
      super(run)
	@myAgent = agent
	@myLevel = level
      end
      def perform
        @run.society.each_agent do |agent|
	  if agent.name == @myAgent
	    result, uri = Cougaar::Communications::HTTP.get(agent.uri+"/iaom?target=#{@myAgent}&level=#{@myLevel}")
	  end
	end
      end
    end

  end

end



