/*
 * TestAction.java
 *
 * Created on March 4, 2004, 10:15 AM
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
import java.util.Set;
import java.util.HashSet;

/**
 *
 * @author  Administrator
 */
public class TestAction extends Action {
    
    /**
     * Creates an action instance for actions to be performed on the specified asset
     */
    public TestAction(String assetName, ServiceBroker serviceBroker) throws TechSpecNotFoundException
    {
        super(assetName, serviceBroker);
        setOffered(this.getPossibleValues());
    }


    /**
     * Creates an action instance for actions to be performed on the specified asset. Includes initialiValuesOffered to
     * override the defaultValuesOffered specified in the techSpecs.
     */
    public TestAction(String assetName, Set initialValuesOffered, ServiceBroker serviceBroker) 
    throws IllegalValueException, TechSpecNotFoundException 
    {
        
        super(assetName, initialValuesOffered, serviceBroker);
        setOffered(this.getPossibleValues());
    }        

    public TestAction(TestAction action) {
	super(action);
    }
    
    /** Sets the values Offered to the possible values */
    private void setOffered(Set s) {
        try {
            this.setValuesOffered(s);
        } catch (Exception e) {}
    }        

    public void start(Object o) throws IllegalValueException {
        super.start(o);
    }

    public void stop(Action.CompletionCode cc) throws NoStartedActionException, IllegalValueException  {
        super.stop(cc);
    }
}
