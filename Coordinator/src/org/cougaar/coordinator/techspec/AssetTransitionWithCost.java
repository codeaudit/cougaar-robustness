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
 * in a given AssetStateDimension. This class extends AssetTransition with information on the costs
 * associated with those transitions should they occur.
 *
 * @author Paul Pazandak, Ph.D. OBJS, Inc.
 */
public class AssetTransitionWithCost extends AssetTransition implements NotPersistable {
    
    ActionCost oneTimeCost = null;
    ActionCost continuingCost = null;
    
    AssetState intermediateState = null;
    
    /** Creates a new instance of AssetTransition. It takes Strings as the AssetStates may not have been loaded 
     *  into the system yet -- lazy evaluation is used
     */
    public AssetTransitionWithCost(AssetType assetType, AssetStateDimension stateDim, AssetState start, AssetState end, AssetState intermediateState) {

        super(assetType, stateDim, start, end);
        this.intermediateState = intermediateState;
    }

    
    /** 
     *  Set the Action Cost. Set <b>isOneTimeCost</> to TRUE if
     *  this is the one time cost. Set to false if it is the 
     *  continuing cost.
     */
    public void setActionCost(ActionCost ac, boolean isOneTimeCost) {
     
        if (isOneTimeCost) { 
            oneTimeCost = ac; 
        } else {
            continuingCost = ac;
        }
    }        
    
    /** @return get one-time cost */
    public ActionCost getOneTimeCost() { return oneTimeCost; }

    /** @return get continuing cost */
    public ActionCost getContinuingCost() { return continuingCost; }
    
    
    /** @return the intermediate state value */
    public AssetState getIntermediateValue() { 
        return intermediateState;  //even if null
    }
    
    public String toString() {
     
        return "WhenStateIs="+this.start+" , IntermediateState="+this.intermediateState+" , EndStateWillBe="+this.end+"\n        OneTimeCost=\n"+this.oneTimeCost+"\n        ContinuingCost=\n"+this.continuingCost;
    }
    
}
