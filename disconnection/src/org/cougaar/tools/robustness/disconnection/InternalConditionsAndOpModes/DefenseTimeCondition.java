/*
 * DefenseOperatingMode.java
 *
 * @author David Wells - OBJS
 *
 * <copyright>
 *  Copyright 2003 Object Services and Consulting, Inc.
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

package org.cougaar.tools.robustness.disconnection.InternalConditionsAndOpModes;


import org.cougaar.core.adaptivity.SensorCondition;
import org.cougaar.core.adaptivity.OMCThruRange;
import org.cougaar.core.adaptivity.OMCRangeList;
import org.cougaar.core.adaptivity.OMCPoint;

import org.cougaar.core.util.UID;


public class DefenseTimeCondition extends DefenseCondition
{
    public static final Double MINTIME  = new Double(-1.0);
    public static final Double MAXTIME = new Double(9223372036854775807.0);

    protected static OMCRangeList allowedValues = new OMCRangeList(new OMCThruRange (MINTIME, MAXTIME));

     /* Do not use. Only use the constructors of the subclasses.
     */
    public DefenseTimeCondition(String assetType, String assetName, String managerID) {
        super(assetType, assetName, managerID, allowedValues, new Double(0.0));
    }
       
    protected void setValue(String newValue) {
        super.setValue(newValue);
    }

    /** tiny helper class for VTH Operating Modes */
    protected static class OMCStrPoint extends OMCPoint {
        public OMCStrPoint (String a) { super (a); }
    }

    public boolean compareSignature(String type, String id, String defenseName) {
    return ((this.assetType.equalsIgnoreCase(type)) &&
          (this.assetName.equals(id)) &&
          (this.defenseName.equals(defenseName)));
}
  

}