/*
 * TestDiagnosis.java
 *
 * Created on March 4, 2004, 9:50 AM
 */

package org.cougaar.coordinator.test.coordination;
import org.cougaar.coordinator.*;

import org.cougaar.coordinator.techspec.*;

import org.cougaar.core.component.ServiceBroker;


/**
 *
 * @author  Administrator
 */
public class TestDiagnosis extends Diagnosis {
    
    public TestDiagnosis(String assetName, ServiceBroker serviceBroker) throws TechSpecNotFoundException 
    {
        super(assetName, serviceBroker);
    }

   

    /**
     * Creates a Diagnosis instance for Diagnoses to be performed on the specified asset. Includes initialValue 
     */
    public TestDiagnosis(String assetName, Object initialValue, ServiceBroker serviceBroker) 
    throws IllegalValueException, TechSpecNotFoundException 
    {

        super(assetName, initialValue, serviceBroker);
        
    }        

    
    
}
