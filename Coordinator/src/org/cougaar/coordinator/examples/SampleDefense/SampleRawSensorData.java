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
import org.cougaar.core.persist.NotPersistable;
import java.io.Serializable;

public class SampleRawSensorData implements NotPersistable, Serializable
{
    String assetName;
    Set possibleValues;
    Object value;

    public SampleRawSensorData(String assetName, Set possibleValues, Object initialValue) {
	this.assetName = assetName;
	this.possibleValues = possibleValues;
	this.value = initialValue;
    }

    String getAssetName() {
	return assetName;
    }

    Set getPossibleValues() {
	return possibleValues;
    }

    Object getValue() {
	return value;
    }	

    void setValue(Object value) {
	this.value = value;
    }	
    
}
