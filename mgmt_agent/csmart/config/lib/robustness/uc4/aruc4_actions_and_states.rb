module Cougaar

  module Actions

    class PublishThreatAlert < Cougaar::Action
      DOCUMENTATION = Cougaar.document {
        @description = "Publish threat alerts"
        @parameters = [
          {:community => "required, The community name."},
	  {:role => "required, The roles to accept the threats."},
	  {:alertLevel => "required, The level of alert, must be one of the following: maximum, high, medium, low, minimum, undefined"};
	  {:interval => "required, The lasting time of the alert."},
	  {:assets => "required, The assets of the alert. Example of assets: assets = {'node'=>'TRANS-NODE, FWD-NODE', 'host'=>'net1'}"}
        ]
        @example = "do_action 'PublishThreatAlert', '1AD-SMALL-COMM', 'HealthMonitor', 'low', 10.minutes, assets"
      }
    
      def initialize(run, community, role, alertLevel, interval, assets)
        super(run)
        @community = community
        @role = role
        @alertLevel = alertLevel
        @interval = interval
        @assets = assets
      end

      def perform
        start = Time.new.to_f
        expire = start + @interval
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
            result, uri = Cougaar::Communications::HTTP.get(node.uri+"/$"+node.name+"/alert?inputcommunity=#{@community}&inputrole=#{@role}&level=#{@alertLevel}&startFromAction=#{start}&expireFromAction=#{expire}&submit=submit&#{temp}")
            #puts "#{node.uri}/$#{node.name}/alert?community=#{@community}&role=#{@role}&level=#{@alertLevel}&startFromAction=#{start}&expireFromAction=#{expire}&#{temp}"
            return
        end
     end
   end

  end

end



