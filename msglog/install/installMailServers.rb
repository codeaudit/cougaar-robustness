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
	    puts "ssh #{host_name} 'mkdir /usr/local/OBJS'"
	    puts `ssh #{host_name} 'mkdir /usr/local/OBJS'`
	    puts "ssh #{host_name} 'unzip #{CIP}/msglog/james_for_B11_2.zip -d /usr/local/OBJS'"
	    puts `ssh #{host_name} 'unzip #{CIP}/msglog/james_for_B11_2.zip -d /usr/local/OBJS'`
	    puts "ssh #{host_name} 'cp #{CIP}/msglog/runjames /etc/rc.d'"
	    puts `ssh #{host_name} 'cp #{CIP}/msglog/runjames /etc/rc.d'`
	    puts "ssh #{host_name} 'cat #{CIP}/msglog/rc >> /etc/rc.d/rc.local'"
	    puts `ssh #{host_name} 'cat #{CIP}/msglog/rc >> /etc/rc.d/rc.local'`
        end
    end
end
