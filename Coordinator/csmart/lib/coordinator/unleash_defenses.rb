 #  Copyright 2004 Object Services and Consulting, Inc.
 #  under sponsorship of the Defense Advanced Research Projects
 #  Agency (DARPA).
 #
 #  You can redistribute this software and/or modify it under the
 #  terms of the Cougaar Open Source License as published on the
 #  Cougaar Open Source Website (www.cougaar.org).
 #
 #  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 #  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 #  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 #  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 #  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 #  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 #  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 #  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 #  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 #  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 #  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


=begin script

include_path: unleash_defenses.rb
description: enable the coordinator to set actions

=end

CIP = ENV['CIP']

$:.unshift File.join(CIP, 'csmart', 'config', 'lib')
require 'coordinator/leashing'

insert_after parameters[:location] do
 if( parameters[:start_delay] != nil && parameters[:start_delay] > 0)
    do_action "SleepFrom", parameters[:location], parameters[:start_delay]
  else
    do_action "SleepFrom", parameters[:location], 60
  end
  do_action "Unleash", parameters[:verbose]
  do_action "UnleashOnSubsequentRestarts", parameters[:verbose]
end
