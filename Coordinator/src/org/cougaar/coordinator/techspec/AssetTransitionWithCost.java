/*
 * AssetTransition.java
 *
 * Created on September 22, 2003, 3:53 PM
 * 
 * <copyright>
 * 
 *  Copyright 2003-2004 Object Services and Consulting, Inc.
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
