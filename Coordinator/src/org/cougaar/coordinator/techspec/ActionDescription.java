/*
 * ActionDescription.java
 *
 * Created on April 5, 2004, 3:31 PM
 * <copyright>  
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

package org.cougaar.coordinator.techspec;

import org.cougaar.core.plugin.ComponentPlugin;

import org.cougaar.core.service.LoggingService;

import org.cougaar.core.service.UIDService;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;
import org.cougaar.core.util.UID;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.ArrayList;

import org.w3c.dom.*;

/**
 *
 * @author  Administrator
 */
public class ActionDescription {
    
    String name;
    String desc;
    AssetType affectsAssetType;
    AssetStateDimension affectsStateDimension;
    Vector transitions;
    
    AssetTransitionWithCost wildcardTransition = null;
    
    /** Creates a new instance of ActionDescription */
    public ActionDescription(String name, AssetType affectsAssetType, AssetStateDimension affectsStateDimension) {
        this.name = name;
        this.affectsAssetType = affectsAssetType;
        this.affectsStateDimension = affectsStateDimension;
        
        transitions = new Vector(5,5);
    }
        
    /** @return action name */
    public String name() { return name;}
    
    /** @return action description */
    public String description() { return desc;}
        
    /** Set the description for this action */
    public void setDescription(String d) {
        this.desc = d;
    }

    /**
     * @return the asset type that this event will affect
     */
    public AssetType getAffectedAssetType() { return affectsAssetType; }

    /**
     * @return the asset state dimension this event will affect
     */
    public AssetStateDimension getAffectedStateDimension() { return affectsStateDimension; }
    
    
    
    /* Called by XML Loaders when parsed */
    public void addTransition(AssetTransitionWithCost atwc) { 
        
        this.transitions.add(atwc); 
        if (atwc.start == AssetState.ANY) {
            this.wildcardTransition = atwc;
        } 
    }
    
    /**
     * @return the vector of AssetTransitionWithCost objects that may occur should this event happen
     */
    public Vector getTransitionVector() {
        return transitions;
    }

   /**
     * Locates a transition from the supplied starting AssetState. If one is found the ending state is
     * returned, otherwise null.  
     */
   /**
     * Locates a transition from the supplied starting AssetState. If one is found the ending state is
     * returned, otherwise null.  
     */
    public AssetTransitionWithCost getTransitionForState(AssetState as) {
    
        if (this.wildcardTransition != null) { //this will apply to all requests
 
            return this.wildcardTransition;
 
        } else { //see if we have a transition with the specified starting state in the transitions
            
            AssetTransitionWithCost at;
            for (Iterator i=transitions.iterator(); i.hasNext(); ) {
                
               at = (AssetTransitionWithCost)i.next();
               if ( at.getStartValue().equals(as)) {
                    return at; // return the AssetTransitionWithCost with the state we'd transition to if this event occurs given the (as) starting state.
                }
            }
            return null; // didn't find one
        }
    }



   /**
     * Locates a transition from the supplied starting AssetState. If one is found the ending state is
     * returned, otherwise null.  
     */
    public AssetTransitionWithCost getTransitionForState(String s) {
    
        if (this.wildcardTransition != null) { //this will apply to all requests

            return this.wildcardTransition;

        } else { //see if we have a transition with the specified starting state in the transitions
            
            AssetTransitionWithCost at;
            for (Iterator i=transitions.iterator(); i.hasNext(); ) {
                
                at = (AssetTransitionWithCost)i.next();
                if ( at.getStartValue().getName().equals(s)) {
                    return at; // return the AssetTransitionWithCost with the state we'd transition to if this event occurs given the (as) starting state.
                }
            }
            return null; // didn't find one
        }
    }
    
    
    
    public String toString() {
        
        String s = "    Action ["+this.name()+"] -------------\n";
        s += "    Desc = "+ this.description() + "\n";
        
        AssetTransitionWithCost at;
        s += "\n    Transitions:";
        for (Iterator i=transitions.iterator(); i.hasNext(); ) {

            at = (AssetTransitionWithCost)i.next();
            s += "\n        "+at.toString();
        }
/*
        s += "    WhenStateIs="+this.getWhenStateIs()+"  EndStateWillBe="+this.getEndStateWillBe()+"\n";
        if (this.getOneTimeCost() != null) {
            s += "    One Time Transition Costs:\n"+ this.getOneTimeCost();
        }
        if (this.getContinuingCost() != null) {
            s += "    Continuing Transition Costs:\n"+ this.getContinuingCost();
        }
 */
        return s;
    }
    
}
