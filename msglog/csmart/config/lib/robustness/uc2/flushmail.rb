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

module UltraLog

  class Operator
  
    # fashioned after clear_pnlogs in $CIP/csmart/acme_scripting/src/lib/ultralog/operator.rb
    def flush_mail
      send_command('flush_mail', 10)
    end

  end

end

# fashioned after clear_pnlogs in $CIP/csmart/acme_service/src/plugins/acme_tic_operator/operator.rb
#register_command("flush_mail", "Flush inboxes") do |message, command|
  #result = "\n"
  #result += call_cmd('cd $CIP/operator; ./flushmail.csh')
  #message.reply.set_body(result).send
#end

module Cougaar

  module Actions

    # fashioned after ClearPersistenceAndLogs in $CIP/csmart/acme_scripting/src/lib/ultralog/operator.rb
    class FlushMail < Cougaar::Action
      PRIOR_STATES = ['OperatorServiceConnected']
      DOCUMENTATION = Cougaar.document {
        @description = "Flushes the inboxes for all Nodes in this society."
        @example = "do_action 'FlushMail'"
      }
      def initialize(run)
        super(run)
      end
      def perform
        @run.society.each_node do |node|
          puts "flushing email to " + node.name
          inbox_param = "-Dorg.cougaar.message.protocol.email.inboxes." + node.name + "="
          puts "looking for inbox_param = " + inbox_param
          node.parameters.each do |param|
            puts "comparing param = " + param
            if param[0...(inbox_param.size)]==inbox_param
              puts "found param = " + param
              uri = param[inbox_param.size..-1]
              puts "flushing email at " + uri
              operator = @run['operator']
              operator.flush_mail
              #puts "result = " + result
            end
          end
        end
      end
    end

  end

end
