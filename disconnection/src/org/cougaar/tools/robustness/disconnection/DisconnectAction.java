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
import java.io.Serializable;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.coordinator.*;
import org.cougaar.coordinator.techspec.TechSpecNotFoundException;
import org.cougaar.core.component.ServiceBroker;

public class DisconnectAction extends Action implements Serializable {

    /** Creates new DisconnectActuator */
    public DisconnectAction(String assetName, ServiceBroker serviceBroker) throws TechSpecNotFoundException {
        super(assetName, serviceBroker);
    }
    
    public DisconnectAction(String assetName, Set initialValuesOffered, ServiceBroker serviceBroker) throws IllegalValueException, TechSpecNotFoundException {
	super(assetName, initialValuesOffered, serviceBroker);
    }

    public static final UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof DisconnectAction ) {
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
