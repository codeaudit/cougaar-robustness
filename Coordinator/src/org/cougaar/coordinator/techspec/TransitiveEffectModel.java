/*
 * TransitiveEffectModel.java
 *
 * Created on May 6, 2004, 3:24 PM
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
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Hashtable;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Date;

import org.cougaar.core.util.UID;
import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

import org.cougaar.core.persist.NotPersistable;


/**
 * This class describes a TransitiveEffectModel. Each one has a corresponding TransitiveEffectDescription,
 * or meta transitiveEffect, from which these objects are created. These objects maintain a list of
 * all applicable assets that have some pobability of being impacted by this transitiveEffect.
 * The list is managed by filtering assets through a vulnerability filter when they are
 * added to the system, and when their properties change.
 *
 * @author  Paul Pazandak, OBJS
 */
public class TransitiveEffectModel implements NotPersistable {    
    
    /** the name of this treat */
    private String name;
    
    /** The revision of this interface */
    private static final String rev = "1.0";

    /** The UID of this object */
    private UID uid;
    
    /** The vector containing a list of all assets this transitiveEffect is concerned about */
    private Vector assets;
        
    /** Probability */
    private float eventProbability = 0;
      
    /** TransitiveEffectVulnerabilityFilter */
    private TransitiveEffectVulnerabilityFilter filter;
    
    /** Static properties used to filter membership of asset list */
    private AssetType assetType;
    
    /** Logger for debugging */
    private static Logger logger;

    private TransitiveEffectDescription  td;
    
    static {
        logger = Logging.getLogger(TransitiveEffectModel.class);
    }        
    
//    TransitiveEffectModel(this.name, this.assetType, uid, defaultEventLikelihoodProb, filter);
    
    /** Creates a new instance of TransitiveEffectModel. The TransitiveEffectDescription is used to filter assets. This class maintains the membership --
     *  that is, the assets that are filtered IN by the vulnerability filter defined in the TransitiveEffectDescription.
     *
     */
    public TransitiveEffectModel(TransitiveEffectDescription td, UID uid) {

        this.td = td;
        
        this.assets = new Vector(20,40);
        this.name = td.getTransitiveEventName();
        this.uid = uid;
        //this.threatType = threatType;        
        this.assetType = td.getTransitiveAssetType();
        this.filter = td.getTransitiveVulnerabilityFilter(); // not used here, other than to retrieve the prob. below.
                
        if (filter != null) {
            this.eventProbability = td.getTransitiveVulnerabilityFilter().getProbability();
        }
    }

    /** 
     * @return the TransitiveEffectType corresponding to this transitiveEffect
     */
    //public TransitiveEffectType getTransitiveEffectType() { return threatType; }
       

    public TransitiveEffectDescription getTransitiveEffectDescription() { return td; }
    
    /**
     * @return the list of assets that the transitiveEffect cares about.
     */
    public java.util.Vector getAssetList() {
        return assets;
    }    

    /**
     * Add an asset, if not already added.
     * @return TRUE if asset was not already there
     */
    public boolean addAsset(AssetTechSpecInterface asset) {
        if (!containsAsset(asset)) {
            assets.addElement(asset);
            return true;
        }
        return false;
    }    
    

    /**
     * Remove an asset
     */
    public boolean removeAsset(AssetTechSpecInterface asset) {
        return assets.removeElement(asset);
    }    
    
    
    /**
     * @return the asset type that the transitiveEffect cares about.
     */
    public AssetType getAssetType() {
        return assetType;
    }    

    
    /**
     * @return the TransitiveEffectMembershipFilters that the transitiveEffect cares about.
     */
    //public TransitiveEffectMembershipFilter[] getMembershipFilters() {
    //    return filters;
    //}    
    
    /**
     * Set the asset type that the transitiveEffect cares about.
     */
    public void setAssetType(AssetType type) {
        assetType = type;
    }    
    
    
    /**
     * @return the likelihood that this transitiveEffect will occur. Returns 0 if there are no likelihoods defined
     */
    public float getTransitiveEffectLikelihood() {

        //First get the transitiveEffect probability. 
        return eventProbability;
        
    }
    
    /**
     * @return the name of this transitiveEffect
     */
    public String getName() {
        return name;
    }
    
    /**
     * @return the revision of this interface
     */
    public String getRevision() {
        return rev;
    }
    
    /**
     * @return the UID of this object
     */
    public org.cougaar.core.util.UID getUID() {
        return uid;
    }
    
    
    /** 
     * @return TRUE if this TransitiveEffect cares about this asset.
     */
    public boolean containsAsset(AssetTechSpecInterface asset) {

        AssetTechSpecInterface atsi;
        Iterator iter = assets.iterator();
        while (iter.hasNext()) {
            atsi = (AssetTechSpecInterface)iter.next();
            if (atsi.getAssetID().equals(asset.getAssetID())) {
                return true;
            }
        }
        return false;
        
    }
    
}


