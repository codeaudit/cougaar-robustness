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

import org.cougaar.coordinator.*;
import org.cougaar.coordinator.techspec.TechSpecNotFoundException;
import org.cougaar.core.component.ServiceBroker;

public class AgentLivenessDiagnosis extends Diagnosis
{
    public AgentLivenessDiagnosis(String assetName, Object initialValue, ServiceBroker serviceBroker)
        throws IllegalValueException, TechSpecNotFoundException {
        super(assetName, initialValue, serviceBroker);
    }

    public AgentLivenessDiagnosis(String assetName, ServiceBroker serviceBroker)
        throws TechSpecNotFoundException {
        super(assetName, serviceBroker);
    }

    public void setValue(Object value) throws IllegalValueException {
        super.setValue(value);
    }

    public String toString() {
      StringBuffer sb = new StringBuffer("AgentLivenessDiagnosis:");
      sb.append(" name=" + getAssetName());
      sb.append(" value=" + getValue());
      sb.append(" posibleValues=" + getPossibleValues());
      return sb.toString();
    }

    /**
     * Returns a verbose XML representation for a AgentLivenessDiagnosis.
     */
    public String dump() {
        return "\n" +
            "<AgentLivenessDiagnosis:\n" +
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
