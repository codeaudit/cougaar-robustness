/*
 * ThrashingSuppressionApplicabilityCondition.java
 *
 * Created on September 29, 2003, 2:36 PM
 */

package org.cougaar.coordinator.thrashingSuppression;

import org.cougaar.coordinator.*;

/**
 *
 * @author  David Wells - OBJS 
 * @version 
 */
public class ThrashingSuppressionApplicabilityCondition extends DefenseCondition {

    /** Creates new ThrashingSuppressionApplicabilityCondition */
    public ThrashingSuppressionApplicabilityCondition() {
        super(null, null, "ThrashingSuppression", DefenseConstants.BOOL_RANGELIST);
    }
    
    public void setValue(DefenseConstants.OMCStrBoolPoint newValue) {
      super.setValue(newValue.toString());
    }
} 