/*
 * ThreatDescription.java
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

/**
 *
 * @author  Administrator
 */
public class ThreatDescription implements NotPersistable {
    
    private String name;
    private AssetType affectsAssetType;
    private String causesEvent;
    private float defaultEventLikelihoodProb;
    //<Threat name="Bomb" affectsAssetType="Host" causesEvent="HostDeath" defaultEventLikelihoodProb="NONE" />

    private Vector filters;
    
    /** Creates a new instance of ThreatDescription */
    public ThreatDescription(String name, AssetType affectsAssetType, String causesEvent, float defaultEventLikelihoodProb) {
        
        this.name = name;
        this.affectsAssetType = affectsAssetType;
        this.causesEvent = causesEvent;
        this.defaultEventLikelihoodProb = defaultEventLikelihoodProb;
        
        filters = new Vector();
        
    }
    
    /**
     * @return the name of this threat
     */
    public String getName() { return name; }

    /**
     * @return the name of this threat
     */
    public AssetType getAffectedAssetType() { return affectsAssetType; }
    /**
     * @return the name of this threat
     */
    public String getEventThreatCauses() { return causesEvent; }
    /**
     * @return the name of this threat
     */
    public float getDefaultProbability() { return defaultEventLikelihoodProb; }
    
    /**
     * Add a vulnerability
     */
    public void addVulnerabilityFilter(VulnerabilityFilter vf) {
        filters.add(vf);
    }
    
    /**
     * Get vulnerability filters
     */
    public Vector getVulnerabilityFilters() {
        return filters;
    }
    
    public String toString() {
     
        String s = "Threat ["+this.getName()+"], affects asset type="+this.getAffectedAssetType()+", causes event="+this.getEventThreatCauses()+"\n";
        s += "[Default Probability = "+this.getDefaultProbability()+"]\n";
        Iterator i = this.getVulnerabilityFilters().iterator();
        while (i.hasNext()) {
             VulnerabilityFilter vf = (VulnerabilityFilter)i.next();
             s = s+ vf + "\n";
        }        
        return s;
    }
}
