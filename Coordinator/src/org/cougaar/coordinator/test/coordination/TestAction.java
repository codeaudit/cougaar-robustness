/*
 * TestAction.java
 *
 * Created on March 4, 2004, 10:15 AM
 */

package org.cougaar.coordinator.test.coordination;
import org.cougaar.coordinator.*;
import org.cougaar.coordinator.techspec.*;

import org.cougaar.core.component.ServiceBroker;
import java.util.Set;

/**
 *
 * @author  Administrator
 */
public class TestAction extends Action {
    
    /**
     * Creates an action instance for actions to be performed on the specified asset
     */
    public TestAction(String assetName, ServiceBroker serviceBroker) throws TechSpecNotFoundException
    {
        super(assetName, serviceBroker);
    }


    /**
     * Creates an action instance for actions to be performed on the specified asset. Includes initialiValuesOffered to
     * override the defaultValuesOffered specified in the techSpecs.
     */
    public TestAction(String assetName, Set initialValuesOffered, ServiceBroker serviceBroker) 
    throws IllegalValueException, TechSpecNotFoundException 
    {
        
        super(assetName, initialValuesOffered, serviceBroker);
    }        
    
}
