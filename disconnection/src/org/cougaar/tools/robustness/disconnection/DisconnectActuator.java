/*
 * DisconnectActuator.java
 *
 * Created on July 18, 2004, 2:33 PM
 */

package org.cougaar.tools.robustness.disconnection;

/**
 *
 * @author  David Wells - OBJS
 * @version 
 */

import java.util.Set;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.coordinator.*;
import org.cougaar.coordinator.techspec.TechSpecNotFoundException;
import org.cougaar.core.component.ServiceBroker;

public class DisconnectActuator extends Action {

    /** Creates new DisconnectActuator */
    public DisconnectActuator(String assetName, ServiceBroker serviceBroker)
	throws TechSpecNotFoundException {
        super(assetName, serviceBroker);
    }
    
    public DisconnectActuator(String assetName, Set initialValuesOffered, ServiceBroker serviceBroker)
	throws IllegalValueException, TechSpecNotFoundException {
	super(assetName, initialValuesOffered, serviceBroker);
    }


    public static final UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof DisconnectActuator ) {
                    return true ;
                }
                return false ;
            }
        };


    protected void setValuesOffered(Set values) throws IllegalValueException {
	super.setValuesOffered(values);
    }

    protected void start(Object actionValue) throws IllegalValueException {  
	super.start(actionValue);
    }

    protected void stop() throws NoStartedActionException {  
	super.stop();
    }

    protected void stop(CompletionCode completionCode) 
	throws IllegalValueException, NoStartedActionException  {
	super.stop(completionCode);
    }

}
