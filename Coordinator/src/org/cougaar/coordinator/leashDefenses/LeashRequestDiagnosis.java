/*
 * ThrashingDiagnosis.java
 *
 * Created on September 29, 2003, 2:36 PM
 *
 *
 * <copyright>
 * 
 *  Copyright 2003-2004 Object Services and Consulting, Inc.
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


package org.cougaar.coordinator.leashDefenses;

import org.cougaar.coordinator.Diagnosis;
import org.cougaar.coordinator.DiagnosesWrapper;
import org.cougaar.coordinator.IllegalValueException;
import org.cougaar.coordinator.techspec.*;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.core.component.ServiceBroker;
/**
 * @date 6/1/04
 * @author  David Wells - OBJS 
 * @version 
 */
public class LeashRequestDiagnosis extends Diagnosis { 

    // Values that the diagnosis can take.
    public static final String LEASH = "LeashDefenses";
    public static final String UNLEASH = "UnleashDefenses";

    private DiagnosesWrapper myWrapper;

    /** Creates new ThrashingDiagnosis */
    public LeashRequestDiagnosis(String assetName, Object initialValue, ServiceBroker serviceBroker) throws IllegalValueException, TechSpecNotFoundException 
    {

        super(assetName, initialValue, serviceBroker);
        
    }   
    
    public LeashRequestDiagnosis(String assetName, ServiceBroker serviceBroker) throws IllegalValueException, TechSpecNotFoundException 
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

    //predicate for ThrashingDiagnosis objects
    public static UnaryPredicate pred = new UnaryPredicate() {
        public boolean execute(Object o) {  
            return 
                (o instanceof LeashRequestDiagnosis);
        }
    };

    /**
     * Return whether or not the diagnosis says the system is thrashing
     **/
    public boolean areDefensesLeashed() {
	String diagnosis_value = (String) this.getValue();
        System.out.println("areDefensesLeashed() returns: " + diagnosis_value.equalsIgnoreCase( LEASH ) + " for: " + diagnosis_value );
	return (diagnosis_value.equalsIgnoreCase( LEASH ) );
    }

} 
