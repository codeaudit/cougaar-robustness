/*
 * EventDescription.java
 *
 * Created on March 26, 2004, 3:06 PM
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

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.core.util.UID;


/**
 *
 * @author  Administrator
 */
public class EventDescription implements NotPersistable {
    
    private String name;
    private AssetType affectsAssetType;
    private String affectsStateDimension;
    //<Threat name="Bomb" affectsAssetType="Host" causesEvent="HostDeath" defaultEventLikelihoodProb="NONE" />
        
    private String wildcardEnd = null; //e.g. whenActualStateIs="*", EndStateWillBe="..."
    private AssetState wildcardEndState = null; //e.g. whenActualStateIs="*", EndStateWillBe="..."
    private Vector directEffectTransitions;

    //** In 2004 we're only supporting one transitive effect. 
    //Future versions could support several.
    private TransitiveEffectDescription transEffect;
    
    /** Creates a new instance of EventDescription */
    public EventDescription(String name, AssetType affectsAssetType, String affectsStateDimension) {
        
        this.name = name;
        this.affectsAssetType = affectsAssetType;
        this.affectsStateDimension = affectsStateDimension;
        directEffectTransitions = new Vector();
    }

    public String toString() {
        String s = "   EventDescription name="+name+" [affects assets of type="+affectsAssetType+", state dimension="+affectsStateDimension+"\n";
        Iterator i = directEffectTransitions.iterator();
        s += "      Direct Transitions:\n";
        if (i.hasNext()) {
            while (i.hasNext()) {
                AssetTransition at = (AssetTransition)i.next();
                s+= "        WhenActualStateIs="+at.start+" EndStateWillBe="+at.end+"\n";
            }
        } else {
            if (wildcardEnd != null) { 
                s += "        Wildcard Start state, EndStateWillBe="+this.wildcardEnd+"\n";
            } else {
                s += "        NONE.\n";
            }
        }

        s += "      Transitive Effect:";
        if (transEffect != null) {
            s += "Causes Event = "+transEffect.getTransitiveEventName() + " on asset type = "+ transEffect.getTransitiveAssetType()+"\n";
            s += transEffect.getTransitiveVulnerabilityFilter().toString();
        } else {
            s += "        NONE.";
        }
        return s;
    }
    
    /**
     * @return the name of this event
     */
    public String getName() { return name; }

    /**
     * @return the asset type that this event will affect
     */
    public AssetType getAffectedAssetType() { return affectsAssetType; }

    /**
     * @return the asset state dimension this event will affect
     */
    public String getAffectedStateDimension() { return affectsStateDimension; }
    

    /* Called by XML Loaders when parsed */
    public void addDirectEffectTransition(String start, String end) { 
        
        if (start.equals("*")) {
            this.wildcardEnd = end;
        } else {  
            this.directEffectTransitions.add(new AssetTransition(affectsAssetType, affectsStateDimension, start, end)); 
        }
    }
    
    /**
     * @return the vector of direct transitions that may occur should this event happen
     */
    public Vector getDirectEffectTransitionVector() {
        return directEffectTransitions;
    }

   /**
     * Locates a transition from the supplied starting AssetState. If one is found the ending state is
     * returned, otherwise null.  
     */
    public AssetState getDirectEffectTransitionForState(AssetState as) {
    
        if (this.wildcardEnd != null) {
            if (this.wildcardEndState != null) {
                return this.wildcardEndState;
            } else { //lazily get it.
                AssetStateDimension asd = affectsAssetType.findStateDimension(affectsStateDimension);
                if (asd != null) {
                    wildcardEndState = asd.findAssetState(wildcardEnd);           
                }
                return wildcardEndState;  //even if null
            }
        } else { //see if we have a transition with the specified starting state in the directEffectTransitions
            
            AssetTransition at;
            for (Iterator i=directEffectTransitions.iterator(); i.hasNext(); ) {
                
                at = (AssetTransition)i.next();
                if ( at.getStartValue().equals(as)) {
                    return at.getEndValue(); // return the state we'd transition to if this event occurs given the (as) starting state.
                }
            }
            return null; // didn't find one
        }
    }
    
    
    public void setTransitiveEffect(TransitiveEffectDescription transEffect) { this.transEffect = transEffect; }
    public TransitiveEffectDescription getTransitiveEffect() { return transEffect; }

    
    
}