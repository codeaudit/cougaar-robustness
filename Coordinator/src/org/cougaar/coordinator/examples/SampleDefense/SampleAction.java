/*
 * <copyright>
 *  Copyright 2004 Object Services and Consulting, Inc.
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA)
 *  and the Defense Logistics Agency (DLA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
*/

package org.cougaar.coordinator.examples.SampleDefense;

import java.util.Set;
import org.cougaar.coordinator.*;
import org.cougaar.coordinator.techspec.TechSpecNotFoundException;
import org.cougaar.core.component.ServiceBroker;

public class SampleAction extends Action
{
    public SampleAction(String assetName, ServiceBroker serviceBroker)
	throws TechSpecNotFoundException {
        super(assetName, serviceBroker);
    }
    
    public SampleAction(String assetName, Set initialValuesOffered, ServiceBroker serviceBroker)
	throws IllegalValueException, TechSpecNotFoundException {
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
     * Returns a verbose pretty-printed representation for a SampleAction.
     */
    public String dump() {
	return "\n" +
            "<SampleAction:\n" +
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
	    "   techSpec = " + getTechSpec() + "\n" +
	    "   uid = " + getUID() + "\n" +
	    "   value = " + getValue() + "\n" +
	    "   valuesOffered = " + getValuesOffered() + ">";
    }
}
