package org.cougaar.tools.robustness.disconnection;

import org.cougaar.tools.robustness.deconfliction.*;

import org.cougaar.core.adaptivity.SensorCondition;
import org.cougaar.core.adaptivity.OMCThruRange;
import org.cougaar.core.adaptivity.OMCRangeList;
import org.cougaar.core.adaptivity.OMCPoint;

import org.cougaar.core.util.UID;


public class DefenseTimeCondition extends DefenseCondition
{
    public static final Double MINTIME  = new Double(0.0);
    public static final Double MAXTIME = new Double(9223372036854775807.0);

    protected static OMCRangeList allowedValues = new OMCRangeList(new OMCThruRange (MINTIME, MAXTIME));

     /* Do not use. Only use the constructors of the subclasses.
     */
    public DefenseTimeCondition(String name) {
        super(name, allowedValues, new Double(0.0));
    }
       
    protected void setValue(String newValue) {
        super.setValue(newValue);
    }

    /** tiny helper class for VTH Operating Modes */
    protected static class OMCStrPoint extends OMCPoint {
        public OMCStrPoint (String a) { super (a); }
    }
}