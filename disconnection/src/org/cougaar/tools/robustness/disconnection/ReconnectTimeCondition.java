package org.cougaar.tools.robustness.disconnection;

import java.util.Date;

public class ReconnectTimeCondition extends DefenseTimeCondition
{
    public ReconnectTimeCondition(String name) {
        super(name);
    }
    
    protected void setValue(Double newValue) {
        super.setValue(newValue);
    }
}