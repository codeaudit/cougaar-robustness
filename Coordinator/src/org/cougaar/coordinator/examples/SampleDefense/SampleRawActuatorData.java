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
