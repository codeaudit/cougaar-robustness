/*
 * DefenseTimeout.java
 *
 * Created on October 17, 2003, 3:14 PM
 */

package org.cougaar.coordinator.monitoring;

import org.cougaar.core.persist.NotPersistable;

/**
 *
 * @author  administrator
 * @version 
 */

import org.cougaar.coordinator.DefenseApplicabilityConditionSnapshot;


public class DefenseTimeoutCondition implements NotPersistable {
    
    private DefenseApplicabilityConditionSnapshot dac;

    /** Creates new DefenseTimeout */
    public DefenseTimeoutCondition(DefenseApplicabilityConditionSnapshot dac) {
        this.dac = dac;
    }
    
    public DefenseApplicabilityConditionSnapshot getCondition() { return dac; }
    
    public DefenseApplicabilityConditionSnapshot getDefenseCondition() { return dac; }

}
