/*
 * AgentDisconnectAction.java
 *
 * Created on June 30, 2004, 9:33 AM
 */

package org.cougaar.tools.robustness.disconnection;

/**
 *
 * @author  David Wells - OBJS
 * @version 
 */

import java.util.Set;
import org.cougaar.coordinator.*;
import org.cougaar.coordinator.techspec.TechSpecNotFoundException;
import org.cougaar.core.component.ServiceBroker;

public class AgentDisconnectAction extends DisconnectAction
{

    public AgentDisconnectAction(String assetName, ServiceBroker serviceBroker) throws TechSpecNotFoundException {
        super(assetName, serviceBroker);
    }
    
    public AgentDisconnectAction(String assetName, Set initialValuesOffered, ServiceBroker serviceBroker) throws IllegalValueException, TechSpecNotFoundException {
	super(assetName, initialValuesOffered, serviceBroker);
    }


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

    /**
     * Returns a verbose pretty-printed representation for a AgentDisconnectAction.
     */
    public String dump() {
	return "\n" +
            "<AgentDisconnectAction:\n" +
	    "   assetID = " + getAssetID() + "\n" +
	    "   assetName = " + getAssetName() + "\n" +
	    "   assetStateDimensionName = " + getAssetStateDimensionName() + "\n" +
	    "   content = " + getContent() + "\n" +
	    "   permittedValues = " + getPermittedValues() + "\n" +
	    "   possibleValues = " + getPossibleValues() + "\n" +
	    "   previousValue = " + getPreviousValue() + "\n" +
	    "   response = " + getResponse() + "\n" +
	    "   source = " + getSource() + "\n" +
	    "   targets = " + getTargets() + "\n" +
	    "   uid = " + getUID() + "\n" +
	    "   value = " + getValue() + "\n" +
	    "   valuesOffered = " + getValuesOffered() + "\n" +
            "   wrapper = " + getWrapper() + ">";

    }
}
