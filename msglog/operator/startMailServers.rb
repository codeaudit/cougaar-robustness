#! /usr/bin/ruby -W0

$:.unshift File.join(ENV['CIP'], 'csmart', 'acme_service', 'src', 'redist')
$:.unshift File.join(ENV['CIP'], 'csmart', 'acme_scripting', 'src', 'lib')

require 'cougaar/scripting'
require 'ultralog/scripting'

Ultralog::OperatorUtils::HostManager.new.load_society.each_service_host("acme") do |host|
    host.each_facet(:service) do |facet|
        if (facet[:service]=='smtp' || facet[:service]=='SMTP')
            host_name = nil
            if host.has_facet?("uriname")
	        host_name = host.get_facet("uriname")
            else
	        host_name = host.host_name
            end
	    puts "ssh #{host_name} 'at -f /etc/rc.d/runjames now + 1 minute'"
	    puts `ssh #{host_name} 'at -f /etc/rc.d/runjames now + 1 minute'`
        end
    end
end

