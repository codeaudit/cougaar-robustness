module Cougaar

  module Actions

    class PublishThreatAlert < Cougaar::Action
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
        @assets.each do |key, value|
          str << "type#{i}=#{key}&id#{i}=#{value}&"
          i = i + 1
        end
        temp = str.chop
        @run.society.each_node do |node|
          result, uri = Cougaar::Communications::HTTP.get(node.uri+"/$"+node.name+"/alert?class=#{@classname}&community=#{@community}&role=#{@role}&level=#{@alertLevel}&startFromAction=#{start}&expireFromAction=#{expire}&submit=submit&#{temp}")
          # puts "#{node.uri}/$#{node.name}/alert?community=#{@community}&role=#{@role}&level=#{@alertLevel}&startFromAction=#{start}&expireFromAction=#{expire}&#{temp}"
          return
        end
      end
    end

  end

end



