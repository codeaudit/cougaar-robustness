/*
 * NodeDisconnectDiagnosis.java
 *
 * Created on June 29, 2004, 11:45 AM
 */

package org.cougaar.tools.robustness.disconnection;

/**
 *
 * @author  David Wells - OBJS
 * @version 
 */

import org.cougaar.coordinator.*;
import org.cougaar.coordinator.techspec.TechSpecNotFoundException;
import org.cougaar.core.component.ServiceBroker;

public class RequestToDisconnectNodeSensor extends org.cougaar.coordinator.Diagnosis {

    // Legal Diagnosis Values
    public final static String DISCONNECT_REQUEST = "Disconnect_Request";
    public final static String CONNECT_REQUEST = "Connect_Request";
    public final static String TARDY = "Tardy";

    /** Creates new DisconnectDiagnosis */
    public RequestToDisconnectNodeSensor(String assetName, Object initialValue, ServiceBroker serviceBroker) throws IllegalValueException, TechSpecNotFoundException {
	super(assetName, initialValue, serviceBroker);
    }

    public RequestToDisconnectNodeSensor(String assetName, ServiceBroker serviceBroker) throws TechSpecNotFoundException {
        super(assetName, serviceBroker);
    }

    public void setValue(Object value) throws IllegalValueException {
	super.setValue(value);
    }

    /**
     * Returns a verbose pretty-printed representation for a SampleDiagnosis.
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
