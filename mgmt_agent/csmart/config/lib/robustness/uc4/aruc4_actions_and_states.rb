module Cougaar

  module Actions

    class PublishThreatAlert < Cougaar::Action
      DOCUMENTATION = Cougaar.document {
        @description = "Publish threat alerts"
        @parameters = [
          {:classname => "required, ThreatAlert classname."},
          {:community => "required, name of destination community."},
	  {:role => "required, community member role to receive alerts."},
	  {:alertLevel => "required, alert level, must be one of the following: maximum, high, medium, low, minimum, undefined"},
	  {:duration => "required, alert duration."},
	  {:assets => "required, affected assets. Example of assets: assets = {'node'=>'TRANS-NODE, FWD-NODE', 'host'=>'net1'}"}
        ]
        @example = "do_action 'org.cougaar.tools.robustness.ma.HostLossThreatAlert', 'PublishThreatAlert', '1AD-SMALL-COMM', 'HealthMonitor', 'low', 10.minutes, assets"
      }

      def initialize(run, classname, community, role, alertLevel, duration, assets)
        super(run)
        @classname = classname
        @community = community
        @role = role
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
            result, uri = Cougaar::Communications::HTTP.get(node.uri+"/$"+node.name+"/alert?class=#{@classname}&inputcommunity=#{@community}&inputrole=#{@role}&level=#{@alertLevel}&startFromAction=#{start}&expireFromAction=#{expire}&submit=submit&#{temp}")
            #puts "#{node.uri}/$#{node.name}/alert?class=#{@classname}&inputcommunity=#{@community}&inputrole=#{@role}&level=#{@alertLevel}&startFromAction=#{start}&expireFromAction=#{expire}&#{temp}"
            return
        end
     end
   end

  end

end



