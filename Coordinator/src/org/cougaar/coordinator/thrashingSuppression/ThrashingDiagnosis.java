/*
 * ThrashingSuppressionApplicabilityCondition.java
 *
 * Created on September 29, 2003, 2:36 PM
 */

package org.cougaar.coordinator.thrashingSuppression;

import org.cougaar.coordinator.Diagnosis;
import org.cougaar.coordinator.DiagnosesWrapper;
import org.cougaar.coordinator.IllegalValueException;
import org.cougaar.coordinator.techspec.*;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.core.component.ServiceBroker;
/**
 * @date 6/1/04
 * @author  David Wells - OBJS 
 * @version 
 */
public class ThrashingDiagnosis extends Diagnosis { 

    private DiagnosesWrapper myWrapper;

    /** Creates new ThrashingSuppressionApplicabilityCondition */
    public ThrashingDiagnosis(String assetName, Object initialValue, ServiceBroker serviceBroker) throws IllegalValueException, TechSpecNotFoundException 
    {

        super(assetName, initialValue, serviceBroker);
        
    }   
    
    public ThrashingDiagnosis(String assetName, ServiceBroker serviceBroker) throws IllegalValueException, TechSpecNotFoundException 
    {

        super(assetName, serviceBroker);
        
    }        

    /* Set the value of the diagnosis. Since the value is a simple string
     * no mapping is required.
     */
    //Note - this is public only so it can be set by the test servlet - nornmally it would be protected so only the Sensor could set it
    public void setValue(Object newValue) throws IllegalValueException {
        super.setValue(newValue);
    }

    public void setWrapper(DiagnosesWrapper dw) { myWrapper = dw; }

    public DiagnosesWrapper getWrapper()  { return myWrapper; }

    //predicate for ThrashingDiagnosis objects
    public static UnaryPredicate pred = new UnaryPredicate() {
        public boolean execute(Object o) {  
            return 
                (o instanceof ThrashingDiagnosis);
        }
    };
} 