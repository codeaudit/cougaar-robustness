/*
 * RequestToDisconnectAgentDiagnosis.java
 *
 * Created on July 14, 2004, 9:50 AM
 */

package org.cougaar.tools.robustness.disconnection;

import org.cougaar.coordinator.*;
import org.cougaar.coordinator.techspec.TechSpecNotFoundException;
import org.cougaar.core.component.ServiceBroker;
import java.io.Serializable;

public class RequestToDisconnectAgentDiagnosis extends org.cougaar.coordinator.Diagnosis implements Serializable {

    /** Creates new DisconnectDiagnosis */
    public RequestToDisconnectAgentDiagnosis(String assetName, Object initialValue, ServiceBroker serviceBroker) throws IllegalValueException, TechSpecNotFoundException {
	super(assetName, initialValue, serviceBroker);
    }

    public RequestToDisconnectAgentDiagnosis(String assetName, ServiceBroker serviceBroker) throws TechSpecNotFoundException {
        super(assetName, serviceBroker);
    }

    public void setValue(Object value) throws IllegalValueException {
	super.setValue(value);
    }

    /**
     * Returns a verbose pretty-printed representation for a RequestToDisconnectAgentDiagnosis.
     */
    public String dump() {
	return "\n" +
            "<DisconnectDiagnosis:\n" +
	    "   assetID = " + getAssetID() + "\n" +
	    "   assetName = " + getAssetName() + "\n" +
	    "   assetStateDimensionName = " + getAssetStateDimensionName() + "\n" +
	    "   content = " + getContent() + "\n" +
	    "   lastAssertedTimestamp = " + getLastAssertedTimestamp() + "\n" +
	    "   lastChangedTimestamp = " + getLastChangedTimestamp() + "\n" +
	    "   possibleValues = " + getPossibleValues() + "\n" +
	    "   response = " + getResponse() + "\n" +
	    "   source = " + getSource() + "\n" +
	    "   targets = " + getTargets() + "\n" +
	    "   uid = " + getUID() + "\n" +
	    "   value = " + getValue() + ">";
    }

}
