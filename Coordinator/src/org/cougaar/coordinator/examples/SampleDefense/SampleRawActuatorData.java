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
import org.cougaar.core.persist.NotPersistable;
import java.io.Serializable;

class SampleRawActuatorData implements NotPersistable, Serializable
{
    String assetName;
    Set possibleValues;
    Set valuesOffered;
    Set permittedValues;
    int command;
    Object actionValue;
    String completionCode;

    static final int SET_PERMITTED_VALUES = 0,
	             SET_VALUES_OFFERED = 1,
	             START = 2,
	             STOP = 3;

    SampleRawActuatorData(String assetName, Set possibleValues, Set valuesOffered, 
			  Set permittedValues, Object actionValue) {
	this.assetName = assetName;
        this.possibleValues = possibleValues;
        this.valuesOffered = valuesOffered;
        this.permittedValues = permittedValues;
        this.actionValue = actionValue;
    }

    String getAssetName() { return assetName; }	

    Set getPossibleValues() { return possibleValues; }	

    Set getValuesOffered() { return valuesOffered; }	

    void setValuesOffered(Set valuesOffered) { this.valuesOffered = valuesOffered; }	

    Set getPermittedValues() { return permittedValues;  }	

    void setPermittedValues(Set permittedValues) { this.permittedValues = permittedValues; }	

    int getCommand() { return command; }	

    void setCommand(int command) { this.command = command; }	

    Object getActionValue() { return actionValue; }	

    void setActionValue(Object value) {	actionValue = value; }	

    String getCompletionCode() { return completionCode; }	

    void setCompletionCode(String code) { completionCode = code; }	

}
