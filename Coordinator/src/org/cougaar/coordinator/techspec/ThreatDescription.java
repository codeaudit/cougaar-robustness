/*
 * ThreatDescription.java
 *
 * Created on March 26, 2004, 3:06 PM
 * 
 * <copyright>
 * 
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
public class ThreatDescription implements NotPersistable {
    
    private String name;
    private AssetType affectsAssetType;
    private String causesEventNamed;
    private EventDescription causesEvent;
    private float defaultEventLikelihoodProb;
    //<Threat name="Bomb" affectsAssetType="Host" causesEvent="HostDeath" defaultEventLikelihoodProb="NONE" />
    private EventProbability eventProbability = null;
    
    /** Threat filter */
    private ThreatVulnerabilityFilter filter = null;
    
    /** Creates a new instance of ThreatDescription  -- it has no filter other than asset type */
    public ThreatDescription(String name, AssetType affectsAssetType, String causesEvent, float defaultEventLikelihoodProb, int probInterval) {
        
        this.name = name;
        this.affectsAssetType = affectsAssetType;
        this.causesEventNamed = causesEvent;
        this.defaultEventLikelihoodProb = defaultEventLikelihoodProb;        
        
        //Create an ALWAYS interval if the defaultEventLikelihoodProb > 0
        if (defaultEventLikelihoodProb > 0.0) {
            eventProbability = new EventProbability();
            eventProbability.addInterval(new EventProbabilityInterval(defaultEventLikelihoodProb, probInterval));
        } 
        this.filter = null;
    }

    /** Creates a new instance of ThreatDescription from a root ThreatDescription */
    public ThreatDescription(ThreatDescription rootTD, ThreatVulnerabilityFilter vf) {
        
        this.name = rootTD.name;
        this.affectsAssetType = rootTD.affectsAssetType;
        this.causesEventNamed = rootTD.causesEventNamed;
        this.defaultEventLikelihoodProb = rootTD.defaultEventLikelihoodProb;
      
        //if vf's ep is null create new probability using default, o.w. use one from vf
        if (vf != null && vf.getProbability() != null) {
            eventProbability = vf.getProbability();
        } else {
            //use probability of the root TD
            eventProbability = rootTD.getEventProbability();
        }
        
        this.filter = vf;
    }
            
    /**
     * @return the name of this threat
     */
    public String getName() { return name; }

    /**
     * @return the asset type that this threat pertains to
     */
    public AssetType getAffectedAssetType() { return affectsAssetType; }

    /**
     * Set the event that this threat causes
     */
    public void setEventThreatCauses(EventDescription event) { causesEvent = event; }
    
    /**
     * @return the event that this threat would cause
     */
    public EventDescription getEventThreatCauses() { return causesEvent; }

    /**
     * @return the name of the event that this threat would cause
     */
    public String getEventNameThreatCauses() { return causesEventNamed; }

    /**
     * @return the event probability. 
     */
    public EventProbability getEventProbability() { return eventProbability; }
        
    /**
     * Get vulnerability filter
     */
    protected ThreatVulnerabilityFilter getVulnerabilityFilter() {
        return filter;
    }
    
    /**
     * @return true if an asset qualifies -- if the threat's filter doesn't exclude the asset.
     * Will return <b>false</b> if the asset type of the asset and the affectAssetType aren't equal.
     * If this is a ThreatDescription without a filter, it will return true if the asset type of the
     * asset equals the affectedAssetType of the threat.
     */
    public boolean qualifies(ThreatModelManagerPlugin mgr, AssetTechSpecInterface asset) {

        //If the asset type & affectAssetType don't equal, then this threat doesn't apply.
        if (! (asset.getAssetType().equals(this.getAffectedAssetType())) ) {
            return false;
        }
        
        if (filter == null) {
            return true;
        } else { // check filter
            return filter.qualifies(mgr, asset);
        }        
    }
    
    public String toString() {
     
        String s = "Threat ["+this.getName()+"], affects asset type="+this.getAffectedAssetType()+", causes event="+this.getEventNameThreatCauses()+"\n";
        if (filter == null) {
            if (this.getEventProbability() == null ) {
                s += "[Probability = 0.0] -- this is a root threat with no direct impact. See embedded threats.\n";
            } else {        
                s += "[Probability = "+this.getEventProbability()+"]\n";
            }
        } else {
             s = s + filter + "\n";
        } 
        return s;
    }
    

    /** A ptr to an instantiation of this meta model */
    private DefaultThreatModel instantiation = null;
    
    //ThreatModelManagerPlugin uses the following methods
    public DefaultThreatModel getInstantiation() { return instantiation; }
    /** Create a new DefaultThreatModel */
    public DefaultThreatModel instantiate(UID uid) { 
        instantiation = new DefaultThreatModel(this, uid);
        return instantiation; 
    }
    
}
