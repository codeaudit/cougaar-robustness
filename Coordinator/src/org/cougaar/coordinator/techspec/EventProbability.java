/*
 * EventProbability.java
 *
 * Created on March 26, 2004, 4:22 PM
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

import java.util.Vector;
import java.util.Iterator;

/**
 *
 * @author  Administrator
 */
public class EventProbability {
    
    Vector intervals;
    
    /** Creates a new instance of EventProbability */
    public EventProbability() {
        intervals = new Vector();
    }
    
    /**
     * Add a EventProbabilityInterval
     */
    public void addInterval(EventProbabilityInterval epi) {
        intervals.add(epi);
    }
    
    public String toString() {
     
        String s = "";
        Iterator i = this.intervals.iterator();
        while (i.hasNext()) {
             EventProbabilityInterval ep = (EventProbabilityInterval)i.next();
             s = s+ ep + "\n      ";
        }        
        return s;
    }
    
}