/*
 * TransProbability.java
 *
 * Created on September 9, 2003, 9:26 AM
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

package org.cougaar.coordinator.techspec;

import org.cougaar.core.persist.NotPersistable;

/**
 * @deprecated
 * This class describes a transition probability.
 * @author Paul Pazandak, Ph.D. OBJS, Inc.
 */
public class TransProbability implements NotPersistable {
    
    
    /** Starting value of this state transition */
    private AssetState start;

    /** Ending value of this state transition */
    private AssetState end;

    /** Probability of this state transition */
    private double probability;
    
    /** Creates a new instance of TransProbability */
    public TransProbability(AssetState start, AssetState end, double probability) {        
        this.start = start;
        this.end = end;
        this.probability = probability;
    }
    
    public AssetState getStartValue() { return start; }

    public AssetState getEndValue() { return end; }

    public double getProbability() { return probability; }
}
