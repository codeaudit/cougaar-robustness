/*
 * StateValue.java
 *
 * Created on September 8, 2003, 1:22 PM
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
import org.cougaar.core.persist.NotPersistable;

/**
 * @author Paul Pazandak, Ph.D. OBJS, Inc.
 */
public class StateValue implements NotPersistable {
    
    
    private String name;
    private int utility;
    
    /** Creates a new instance of StateValue */
    public StateValue(String name, int utility) {
        this.name = name;
        this.utility = utility;
    }
    
    /** Return the string value of this state */
    public String getName() { return name; }
    
    /** @return the utility value of being in this state, or phrased more otherwise,
     * the utility value of having the value getName() for the associated AssetStateDescriptor
     */
    public int getUtility() { return utility; }
    
    /** @return TRUE if the value returned by getName() of each obhect matches */
    public boolean equals(Object o) {     
        return ( (o instanceof StateValue) && (o != null) && ( getName().equals(((StateValue)o).getName()) ) );
    }
}
