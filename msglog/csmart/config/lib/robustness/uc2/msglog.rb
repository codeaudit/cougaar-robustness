##
#  <copyright>
#  Copyright 2003 Object Services and Consulting, Inc.
#  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
#
#  This program is free software; you can redistribute it and/or modify
#  it under the terms of the Cougaar Open Source License as published by
#  DARPA on the Cougaar Open Source Website (www.cougaar.org).
#
#  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
#  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
#  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
#  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
#  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
#  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
#  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
#  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
#  PERFORMANCE OF THE COUGAAR SOFTWARE.
# </copyright>
#

module Cougaar

  module Actions
    class MonitorProtocolSelection < Cougaar::Action
      def initialize(run, node=nil, protocol=nil)
        super(run)
        @node = node
        @protocol= protocol
      end
      def perform
        @run.comms.on_cougaar_event do |event|
          #puts event.data
          if (event.component=="AdaptiveLinkSelectionPolicy") && (@protocol==nil || event.data.include?(" to " + @protocol)) && (@node==nil || event.data.include?(" to " + @node))
            loop = false
            puts event.data
          end
        end
      end
    end
  end

  module States

    class SwitchProtocols < Cougaar::State
      DEFAULT_TIMEOUT = 1.minutes
      PRIOR_STATES = ["SocietyRunning"]
      def initialize(run, node, protocol, timeout=nil, &block)
        super(run, timeout, &block)
        @node=node
        @protocol=protocol
      end
      
      def process
        puts "Waiting " + @timeout.to_s + " seconds for switch to " + @protocol + " for messages to " + @node
        loop = true
        while loop
          event = @run.get_next_event
          #puts event.data
          if event.component=="AdaptiveLinkSelectionPolicy" && event.data.include?("Switch from ") && event.data.include?(" to " + @protocol) && event.data.include?(" to " + @node)
            loop = false
            puts event.data
          end
        end
      end
      
      def unhandled_timeout
        puts "Timed out after " + @timeout.to_s + " seconds waiting for switch to " + @protocol + " for messages to " + @node
        @run.do_action "StopSociety" 
        @run.do_action "StopCommunications"
      end
    end

  end

end
