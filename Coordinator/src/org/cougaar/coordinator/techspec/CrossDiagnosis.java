/*
 * CrossDiagnosis.java
 *
 * Created on March 25, 2004, 3:09 PM
 * <copyright>
 *  Copyright 2003 Object Services and Consulting, Inc.
 *  Copyright 2001-2003 Mobile Intelligence Corp
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

package org.cougaar.coordinator.techspec;

import java.util.Vector;

/**
 *
 * @author  Administrator
 */
public class CrossDiagnosis {
    
    private String sensorName;
    private String isAffectedByStateDimension;
    private Vector probs;
    
    /** Creates a new instance of CrossDiagnosis */
    public CrossDiagnosis(String name, String affectedState) {
        
        sensorName = name;
        isAffectedByStateDimension = affectedState;
        probs = new Vector();
    }

    /** @return the cross diagnosis sensor type */
    public String getSensorName() { return sensorName; }
    
    /** @return the cross diagnosis affected dimension */
    public String getAffectedDimension() { return isAffectedByStateDimension; }
    
    /** Add a cross diagnosis probability */
    public void addProbability(DiagnosisProbability dp) {
     
        probs.add(dp);
    }

    /** @return the cross diagnosis probabilities */
    public Vector getProbabilities() {
     
        return probs;
    }
    
}