package org.cougaar.tools.robustness.disconnection;

import org.cougaar.core.adaptivity.OMCRangeList;

import org.cougaar.tools.robustness.deconfliction.*;

public class PlannedDisconnectApplicabilityCondition extends org.cougaar.tools.robustness.deconfliction.DefenseApplicabilityCondition
{
	public PlannedDisconnectApplicabilityCondition(String name) 
	{
		super(name, DefenseConstants.BOOL_RANGELIST, DefenseConstants.BOOL_FALSE.toString());
	}
    public void setValue(Comparable value) {
        super.setValue(value);      
  }
	
}