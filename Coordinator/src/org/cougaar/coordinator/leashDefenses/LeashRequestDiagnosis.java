/*
 * ThrashingDiagnosis.java
 *
 * Created on September 29, 2003, 2:36 PM
 */

package org.cougaar.coordinator.leashDefenses;

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
public class LeashRequestDiagnosis extends Diagnosis { 

    // Values that the diagnosis can take.
    public static final String LEASH = "WantsToLeash";
    public static final String UNLEASH = "WantsToUnleash";

    private DiagnosesWrapper myWrapper;

    /** Creates new ThrashingDiagnosis */
    public LeashRequestDiagnosis(String assetName, Object initialValue, ServiceBroker serviceBroker) throws IllegalValueException, TechSpecNotFoundException 
    {

        super(assetName, initialValue, serviceBroker);
        
    }   
    
    public LeashRequestDiagnosis(String assetName, ServiceBroker serviceBroker) throws IllegalValueException, TechSpecNotFoundException 
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
                (o instanceof LeashRequestDiagnosis);
        }
    };

    /**
     * Return whether or not the diagnosis says the system is thrashing
     **/
    public boolean areDefensesLeashed() {
	String diagnosis_value = (String) this.getValue();
	return (diagnosis_value.equalsIgnoreCase( LEASH ) );
    }

} 
