/*
 * TransitiveEffectModel.java
 *
 * Created on May 6, 2004, 3:24 PM
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
// ALERT: potential source of CMEs
        return assets;
    }    

    /**
     * Add an asset, if not already added.
     * @return TRUE if asset was not already there
     */
    public boolean addAsset(AssetTechSpecInterface asset) {
      synchronized(assets) {
        if (!containsAsset(asset)) {
            assets.addElement(asset);
            return true;
        }
        return false;
      }
    }    
    

    /**
     * Remove an asset
     */
    public boolean removeAsset(AssetTechSpecInterface asset) {
      synchronized(assets) {
        return assets.removeElement(asset);
      }
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
      synchronized(assets) {
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
    
}


