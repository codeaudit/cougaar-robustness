/*
 * Class.java
 *
 * Created on May 24, 2004, 12:13 PM
 */

package org.cougaar.coordinator.test.defense;

/**
 *
 * @author  David Wells - OBJS
 * @version 
 */
import org.cougaar.coordinator.*;
import org.cougaar.coordinator.ActionsWrapper;
import org.cougaar.coordinator.techspec.*;
import org.cougaar.core.component.ServiceBroker;
import java.util.Set;


public class FakeCommDefense extends Action {

    private ActionsWrapper myWrapper;

    /**
     * Creates a Diagnosis instance for Diagnoses to be performed on the specified asset. Includes initialValue 
     */
    public FakeCommDefense(String assetName, Set initialValues, ServiceBroker serviceBroker) throws IllegalValueException, TechSpecNotFoundException 
    {

        super(assetName, initialValues, serviceBroker);
        
    }   
    
    public FakeCommDefense(String assetName, ServiceBroker serviceBroker) throws IllegalValueException, TechSpecNotFoundException 
    {

        super(assetName, serviceBroker);
        
    }    

    public FakeCommDefense(FakeCommDefense action) {
	super(action);
    }    

    /* Set the value of the diagnosis. Since the value is a simple string
     * no mapping is required.
     */
    //Note - this is public only so it can be set by the test servlet - nornmally it would be protected so only the Sensor could set it
    public void setValuesOffered(Set newValues) throws IllegalValueException {
        super.setValuesOffered(newValues);
    }

    public void setWrapper(ActionsWrapper aw) { myWrapper = aw; }

    public ActionsWrapper getWrapper()  { return myWrapper; }

}
