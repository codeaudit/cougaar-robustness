/*
 * TestDiagnosis.java
 *
 * Created on March 4, 2004, 9:50 AM
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


package org.cougaar.coordinator.test.coordination;
import org.cougaar.coordinator.*;

import org.cougaar.coordinator.techspec.*;

import org.cougaar.core.component.ServiceBroker;


/**
 *
 * @author  Administrator
 */
public class TestDiagnosis extends Diagnosis {
    
    public TestDiagnosis(String assetName, ServiceBroker serviceBroker) throws TechSpecNotFoundException 
    {
        super(assetName, serviceBroker);
    }

   

    /**
     * Creates a Diagnosis instance for Diagnoses to be performed on the specified asset. Includes initialValue 
     */
    public TestDiagnosis(String assetName, Object initialValue, ServiceBroker serviceBroker) 
    throws IllegalValueException, TechSpecNotFoundException 
    {

        super(assetName, initialValue, serviceBroker);
        
    }        

    /* Set the value of the diagnosis. Since the value is a simple string
     * no mapping is required.
     */
    public void setValue(Object newValue) throws IllegalValueException {
        super.setValue(newValue);
    }
    
    
}
