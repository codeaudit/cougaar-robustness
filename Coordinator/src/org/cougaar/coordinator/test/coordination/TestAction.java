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
import java.util.HashSet;

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
        setOffered(this.getPossibleValues());
    }


    /**
     * Creates an action instance for actions to be performed on the specified asset. Includes initialiValuesOffered to
     * override the defaultValuesOffered specified in the techSpecs.
     */
    public TestAction(String assetName, Set initialValuesOffered, ServiceBroker serviceBroker) 
    throws IllegalValueException, TechSpecNotFoundException 
    {
        
        super(assetName, initialValuesOffered, serviceBroker);
        setOffered(this.getPossibleValues());
    }        
    
    /** Sets the values Offered to the possible values */
    private void setOffered(Set s) {
        try {
            this.setValuesOffered(s);
        } catch (Exception e) {}
    }        

    protected void start(Object o) throws IllegalValueException {
        super.start(o);
    }

    protected void stop(Action.CompletionCode cc) throws NoStartedActionException, IllegalValueException  {
        super.stop(cc);
    }
}
