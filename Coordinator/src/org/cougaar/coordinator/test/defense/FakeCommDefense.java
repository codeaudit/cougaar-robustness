/*
 * Class.java
 *
 * Created on May 24, 2004, 12:13 PM
 *
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


package org.cougaar.coordinator.test.defense;

/**
 *
 * @author  David Wells - OBJS
 * @version 
 */
import org.cougaar.coordinator.*;
import org.cougaar.coordinator.ActionsWrapper;
import org.cougaar.coordinator.techspec.*;
import org.cougaar.core.component.ServiceBroker;
import java.util.Set;


public class FakeCommDefense extends Action {

    private ActionsWrapper myWrapper;

    /**
     * Creates a Diagnosis instance for Diagnoses to be performed on the specified asset. Includes initialValue 
     */
    public FakeCommDefense(String assetName, Set initialValues, ServiceBroker serviceBroker) throws IllegalValueException, TechSpecNotFoundException 
    {

        super(assetName, initialValues, serviceBroker);
        
    }   
    
    public FakeCommDefense(String assetName, ServiceBroker serviceBroker) throws IllegalValueException, TechSpecNotFoundException 
    {

        super(assetName, serviceBroker);
        
    }    

    public FakeCommDefense(FakeCommDefense action) {
	super(action);
    }    

    /* Set the value of the diagnosis. Since the value is a simple string
     * no mapping is required.
     */
    //Note - this is public only so it can be set by the test servlet - nornmally it would be protected so only the Sensor could set it
    public void setValuesOffered(Set newValues) throws IllegalValueException {
        super.setValuesOffered(newValues);
    }

    public void setWrapper(ActionsWrapper aw) { myWrapper = aw; }

    public ActionsWrapper getWrapper()  { return myWrapper; }

}
