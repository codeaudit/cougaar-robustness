module Cougaar
  module Actions
    class DisableDeconfliction < Cougaar::Action
      def initialize(run)
        super(run)
      end
      def perform
	  @run.society.each_node do |node|
	    node.add_parameter("-Dorg.cougaar.tools.robustness.restart.deconfliction=DISABLED")
	  end
      end
    end
  end
end