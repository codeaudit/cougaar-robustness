/*
 * RestartAgent.java
 *
 * Created on May 24, 2004, 11:17 AM
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


public class RestartAgentDefense extends Action {

    private ActionsWrapper myWrapper;

    /**
     * Creates a Diagnosis instance for Diagnoses to be performed on the specified asset. Includes initialValue 
     */
    public RestartAgentDefense(String assetName, Set initialValues, ServiceBroker serviceBroker) throws IllegalValueException, TechSpecNotFoundException 
    {

        super(assetName, initialValues, serviceBroker);
        
    }   
    
    public RestartAgentDefense(String assetName, ServiceBroker serviceBroker) throws IllegalValueException, TechSpecNotFoundException 
    {

        super(assetName, serviceBroker);
        
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
