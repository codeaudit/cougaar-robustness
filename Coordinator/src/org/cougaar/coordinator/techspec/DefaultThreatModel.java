/*
 * ThreatModel.java
 *
 * Created on July 9, 2003, 9:22 AM
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
 * This class describes a ThreatModel. Each one has a corresponding ThreatDescription,
 * or meta threat, from which these objects are created. These objects maintain a list of
 * all applicable assets that have some pobability of being impacted by this threat.
 * The list is managed by filtering assets through a vulnerability filter when they are
 * added to the system, and when their properties change.
 *
 * @author  Paul Pazandak, OBJS
 */
public class DefaultThreatModel implements ThreatModelInterface, NotPersistable  {
    
    /** the name of this treat */
    private String name;
    
    /** The revision of this interface */
    private static final String rev = "1.0";

    /** The UID of this object */
    private UID uid;

    private Object lock = new Object();
    
    /** The vector containing a list of all assets this threat is concerned about */
    private Vector assets;

    /** The vector containing a list of all newly added assets, since last time clearNewAssets() was called */
    private Vector newAssets;

    /** The vector containing a list of all newly removed assets, since last time clearRemovedAssets() was called */
    private Vector removedAssets;
    
    
    /** The ThreatType of this threat 
     * @deprecated
     */
    private ThreatType threatType;
        
    /** All of the DamageDistributions */
    private EventProbability eventProbability = null;
      
    /** The ThreatDescription that was used to create this object */
    private ThreatDescription threatDesc;
    
    /** VulnerabilityFilter */
    private ThreatVulnerabilityFilter filter;
    
    /** Static properties used to filter membership of asset list */
    private AssetType assetType;
    
    /** Logger for debugging */
    private static Logger logger;

    static {
        logger = Logging.getLogger(DefaultThreatModel.class);
    }        
    
//    DefaultThreatModel(this.name, this.assetType, uid, defaultEventLikelihoodProb, filter);
    
    /** Creates a new instance of ThreatModel. The ThreatDescription is used to filter assets. This class maintains the membership --
     *  that is, the assets that are filtered IN by the vulnerability filter defined in the ThreatDescription.
     *
     */
    public DefaultThreatModel(ThreatDescription td, UID uid) {
//                                String name, AssetType assetType, UID uid, 
//                              ThreatLikelihoodInterval[] threatLikelihoods) {
        this.assets = new Vector(20,40);
        this.name = td.getName();
        this.uid = uid;
        //this.threatType = threatType;        
        this.assetType = td.getAffectedAssetType();
        this.filter = td.getVulnerabilityFilter(); // not used here, other than to retrieve the prob. below.
        
        this.eventProbability = td.getEventProbability();
        
        this.threatDesc = td;
        
        removedAssets = new Vector(5,20);
        newAssets = new Vector(5, 20);
    }

    /** 
     * @deprecated
     * @return the ThreatType corresponding to this threat
     */
    public ThreatType getThreatType() { return threatType; }

    /**
     *@return the vector of associated damage distribution instances. This may be 
     * a dynamically generated vector.
     *@deprecated Does NOT work! Returns null.
     */
    public Vector getDamageDistributionVector() { return null;}
    
    
    /** 
     * Clear the vector of newly added assets
     *@return a clone of the vector of newly added assets
     */
    public Vector clearNewAssets() {
      synchronized(lock) {
        Vector temp = (Vector) newAssets.clone();
        newAssets.clear();
        return temp;
      }    
    }

    
    /** 
     * Clear the vector of removed assets
     *@return a clone of the vector of removed assets
     */
    public Vector clearRemovedAssets() {
      synchronized(lock) {
        Vector temp = (Vector) removedAssets.clone();
        removedAssets.clear();
        return temp;
      }        
    }
    
    /**
     * @return the list of assets that the threat cares about.
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
      synchronized(lock) {
	if (!containsAsset(asset)) {
            assets.addElement(asset);
            newAssets.add(asset);
            return true;
        }
        return false;
      }    
    }
    

    /**
     * Remove an asset
     */
    public boolean removeAsset(AssetTechSpecInterface asset) {
      synchronized(lock) {
        if (assets.removeElement(asset)) {
            removedAssets.add(asset);
            return true;
        } else {
            return false;
        }
      }
    }    
    
    
    /**
     * @return the asset type that the threat cares about.
     */
    public AssetType getAssetType() {
        return assetType;
    }    

    
    /**
     * @return the ThreatDescription techspec for this threat.
     */
    public ThreatDescription getThreatDescription() {
        return threatDesc;
    }    
    
    /**
     * Set the asset type that the threat cares about.
     */
    public void setAssetType(AssetType type) {
        assetType = type;
    }    
    

    
    /**
     *@return the threat likelihood during the given time interval
     * @deprecated - use getProbabilityOfEvent
     */
    public double getThreatLikelihood(long start_time, long end_time)  throws NegativeIntervalException
    { return 0; }

    
    /**
     * @return the probability that this threat will occur. Returns 0 if there are no probabilities defined
     * @throws NegativeIntervalException if end < start. 
     */
    public double getProbabilityOfEvent(long start, long end) throws NegativeIntervalException {

        //If no probability, then return 0
        if (eventProbability == null) { return 0; }
        
        //First get the threat probability intervals (ThreatProbabilityInterval) over this time. 
        return eventProbability.computeIntervalProbability(start, end);
        
    }
    
    /**
     * @return the name of this threat
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
     * @return TRUE if this Threat cares about this asset.
     */
    public boolean containsAsset(AssetTechSpecInterface asset) {
      synchronized(lock) {
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
    
    
/*
    // @return the likelihood that this threat will occur. Returns 0 if there are no likelihoods defined, or
    // if end < = start. 
    public double getThreatLikelihood(long start, long end) {

        return filter.getThreatLikelihood(long start, long end);
        
    }
        if (threatLikelihoods == null || threatLikelihoods.length == 0) { 
            logger.warn("__________getThreatLikelihood(): No ThreatLikelihoods, returning 0");
            return 0.0; 
        }
        if (end <= start) {
            logger.warn("__________getThreatLikelihood(): end <= start, returning 0");
            return 0.0;
        }        
        
        ThreatLikelihoodInterval tli;
        for (int i=0; i<threatLikelihoods.length; i++ ) {

            tli = threatLikelihoods[i];
            //int numIntervals = ((int)(end-start)) / tli.getIntervalLength();
            if (tli.getApplicabilityInterval() == ThreatLikelihoodInterval.ALWAYS) {
             
                logger.debug("__________getThreatLikelihood(): Computing ALWAYS prob.");
                
                //compute poisson & return value
                return tli.computeIntervalProbability(start, end); //ignores others, if "always" if found

            }
        }

        //First figure out if the time in question is during the am or pm
         Calendar calendar = new GregorianCalendar();
         Date startTime = new Date(start);
         calendar.setTime(startTime);
         boolean isDay = false;
         int hour = calendar.get(Calendar.HOUR_OF_DAY);
         if (hour > 8 && hour < 17) {
             isDay = true;
         }
         
        for (int j=0; j<threatLikelihoods.length; j++ ) {

            tli = threatLikelihoods[j];
            //int numIntervals = ((int)(end-start)) / tli.getIntervalLength();
            if (tli.getApplicabilityInterval() == ThreatLikelihoodInterval.DAY && isDay) {
                logger.debug("__________getThreatLikelihood(): Computing day time prob.");
             
                //compute poisson & return value
                return tli.computeIntervalProbability(start, end);

            } else if (tli.getApplicabilityInterval() == ThreatLikelihoodInterval.NIGHT && !isDay) {
                logger.debug("__________getThreatLikelihood(): Computing night time prob.");
             
                //compute poisson & return value
                return tli.computeIntervalProbability(start, end);
            }
        }
         
        logger.warn("__________getThreatLikelihood(): Computed NO prob.");
        
        return 0.0;
            
    }
*/    
    
    /** Structure used to store the DamageDistributions. Uses a
     *  single key based upon two values
     */
/*    class TwoKeyHash {

        private Map map = new Hashtable();
        
        
        public Object get(Object key1, Object key2) {
            return map.get(new DoubleKey(key1, key2));
        }

        public Object put(Object key1, Object key2, Object value) {
            return map.put(new DoubleKey(key1, key2), value);
        }
        
        public Collection values() { return map.values(); }
        
        
        private class DoubleKey {
            private Object key1;
            private Object key2;
            private int hashed;
            
            public DoubleKey(Object key1, Object key2) {
                this.key1 = key1;
                this.key2 = key2;
                hashed = key1.hashCode() ^ key2.hashCode();
            } 
            
            public boolean equals(Object other) {
                DoubleKey o = (DoubleKey)other;
                return (key1.equals(o.key1)) && (key2.equals(o.key2));
            } 
            
            public int hashCode() {
                return hashed;
            } 
            
        } 
    } 
*/    
    
    
}


