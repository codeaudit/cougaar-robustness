/*
 * <copyright>
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

package org.cougaar.tools.robustness.ma.util;

import java.util.Set;
import org.cougaar.coordinator.*;
import org.cougaar.coordinator.techspec.TechSpecNotFoundException;
import org.cougaar.core.component.ServiceBroker;

public class AgentRestartAction extends Action
{
    public AgentRestartAction(String assetName, ServiceBroker serviceBroker)
        throws TechSpecNotFoundException {
        super(assetName, serviceBroker);
    }

    public AgentRestartAction(String assetName, Set initialValuesOffered, ServiceBroker serviceBroker)
        throws IllegalValueException, TechSpecNotFoundException {
        super(assetName, initialValuesOffered, serviceBroker);
    }

    public AgentRestartAction(AgentRestartAction action) {
        super(action);
    }

    public void setValuesOffered(Set values) throws IllegalValueException {
        super.setValuesOffered(values);
    }

    public void start(Object actionValue) throws IllegalValueException {
        super.start(actionValue);
    }

    public void stop() throws NoStartedActionException {
        super.stop();
    }

    public void stop(CompletionCode completionCode)
        throws IllegalValueException, NoStartedActionException  {
        super.stop(completionCode);
    }

}
