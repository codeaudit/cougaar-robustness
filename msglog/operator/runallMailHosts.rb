#! /usr/bin/ruby -W0

CIP = ENV['CIP']

$:.unshift File.join(CIP, 'csmart', 'acme_service', 'src', 'redist')
$:.unshift File.join(CIP, 'csmart', 'acme_scripting', 'src', 'lib')

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
	    puts "ssh #{host_name} '#{ARGV.join(" ")}'"
            puts `ssh #{host_name} '#{ARGV.join(" ")}'`
        end
    end
end
