/*
 * <copyright>
 * 
 *  Copyright 2004 Object Services and Consulting, Inc.
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 *
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * </copyright>
 */


package org.cougaar.coordinator.examples.SampleDefense;

import org.cougaar.coordinator.*;
import org.cougaar.coordinator.techspec.TechSpecNotFoundException;
import org.cougaar.core.component.ServiceBroker;

public class SampleDiagnosis extends Diagnosis
{
    public SampleDiagnosis(String assetName, Object initialValue, ServiceBroker serviceBroker) 
	throws IllegalValueException, TechSpecNotFoundException {
	super(assetName, initialValue, serviceBroker);
    }

    public SampleDiagnosis(String assetName, ServiceBroker serviceBroker) 
	throws TechSpecNotFoundException {
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
            "<SampleDiagnosis:\n" +
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
