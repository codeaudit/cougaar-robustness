/*
 * AssetTransition.java
 *
 * Created on September 22, 2003, 3:53 PM
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
import org.cougaar.core.persist.NotPersistable;

/**
 * This class defines the possible transitions that an Asset make take from one AssetState to another
 * in a given AssetStateDimension.
 *
 * Reused/reengineered for 2004
 *
 * @author Paul Pazandak, Ph.D. OBJS, Inc.
 */
public class AssetTransition implements NotPersistable {
    
    protected String start = null;
    protected String end = null;
    protected String stateDim = null;
    
    protected AssetState startState = null;
    protected AssetState endState = null;

    protected AssetType assetType;
    
    /** Creates a new instance of AssetTransition. It takes Strings as the AssetStates may not have been loaded 
     *  into the system yet -- lazy evaluation is used
     */
    public AssetTransition(AssetType assetType, String stateDim, String start, String end) {
        
        this.start = start;
        this.end = end;
        this.assetType = assetType;
        this.stateDim = stateDim;
    }

    /** @return the starting state value */
    public AssetState getStartValue() { 
        if (startState != null) { return startState; }
        //Otherwise, look it up
        AssetStateDimension asd = assetType.findStateDimension(stateDim);
        if (asd != null) {
            startState = asd.findAssetState(start);           
        }
        return startState; //even if null
    }
    
    /** @return the ending state value */
    public AssetState getEndValue() { 
        if (endState != null) { return endState; }
        //Otherwise, look it up
        AssetStateDimension asd = assetType.findStateDimension(stateDim);
        if (asd != null) {
            endState = asd.findAssetState(end);           
        }
        return endState;  //even if null
    }
}
