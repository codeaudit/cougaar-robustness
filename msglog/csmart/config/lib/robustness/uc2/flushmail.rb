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

    # fashioned after ClearPersistenceAndLogs in $CIP/csmart/acme_scripting/src/lib/ultralog/operator.rb
    class FlushMail < Cougaar::Action
      PRIOR_STATES = ['OperatorServiceConnected','CommunicationsRunning']
      DOCUMENTATION = Cougaar.document {
        @description = "Flushes the inboxes for all Nodes in this society."
        @example = "do_action 'FlushMail'"
      }
      def initialize(run)
        super(run)
      end
      def perform
        operator = @run['operator']
        cip = operator.test.strip
        op_host = @run.society.get_service_host('operator') 
        @run.society.each_node do |node|
          #puts "flushing email to " + node.name
          inbox_param = "-Dorg.cougaar.message.protocol.email.inboxes." + node.name + "="
          #puts "looking for inbox_param = " + inbox_param
          node.parameters.each do |param|
            #puts "comparing param = " + param
            if param[0...(inbox_param.size)]==inbox_param
              #puts "found param = " + param
              uri = param[inbox_param.size..-1]
              puts "flushing email at " + uri
              @run.comms.new_message(op_host).set_body("command[rexec]#{cip}/operator/flushmail.csh #{cip} #{uri}").request(30) 
            end
          end
        end
      end
    end

  end

end
