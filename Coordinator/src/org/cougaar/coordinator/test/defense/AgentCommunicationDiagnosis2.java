/*
 * AgentCommunication.java
 *
 * Created on May 20, 2004, 9:27 AM
 */

package org.cougaar.coordinator.test.defense;

/**
 *
 * @author  Administrator
 * @version 
 */
import org.cougaar.coordinator.*;

import org.cougaar.coordinator.techspec.*;

import org.cougaar.core.component.ServiceBroker;

public class AgentCommunicationDiagnosis2 extends Diagnosis {

    private DiagnosesWrapper  myWrapper;
    /**
     * Creates a Diagnosis instance for Diagnoses to be performed on the specified asset. Includes initialValue 
     */
    public AgentCommunicationDiagnosis2(String assetName, Object initialValue, ServiceBroker serviceBroker) throws IllegalValueException, TechSpecNotFoundException 
    {

        super(assetName, initialValue, serviceBroker);
        
    }   
    
    public AgentCommunicationDiagnosis2(String assetName, ServiceBroker serviceBroker) throws IllegalValueException, TechSpecNotFoundException 
    {

        super(assetName, serviceBroker);
        
    }        

    /* Set the value of the diagnosis. Since the value is a simple string
     * no mapping is required.
     */
    //Note - this is public only so it can be set by the test servlet - nornmally it would be protected so only the Sensor could set it
    public void setValue(Object newValue) throws IllegalValueException {
        super.setValue(newValue);
    }

    public void setWrapper(DiagnosesWrapper dw) { myWrapper = dw; }

    public DiagnosesWrapper getWrapper()  { return myWrapper; }
}
