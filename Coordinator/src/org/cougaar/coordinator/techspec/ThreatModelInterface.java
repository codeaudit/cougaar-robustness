/*
 * ThreatModelInterface.java
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

/**
 * This is NOT a tech spec... in the cougaar sense & hence its name does not indicate this.
 * However, for purposes of implementation it does inherit from the TechSpecRootInterface.
 *
 * This interface is primarily for external consumers to code to.
 * As such it focuses on the API that is expected such a consumer will
 * require. A more complex API will be found in the implementation,
 * providing much greater functionality -- but primarily oriented toward
 * intra-package use.
 *
 * @author Paul Pazandak, Ph.D. OBJS, Inc.
 */
public interface ThreatModelInterface extends TechSpecRootInterface {
    

    /**
     *@return the vector of asset tech specs that this threat model pertains to.
     *
     */
    public Vector getAssetList();
    
    /**
     *@return the threat likelihood during the given time interval
     *
     */
    public double getThreatLikelihood(long start_time, long end_time);
    
    /**
     *@return the vector of associated damage distribution instances. This may be 
     * a dynamically generated vector.
     *
     */
    public Vector getDamageDistributionVector();

    
    /**
     * This is particularly useful for dynamically generated values. If a vector already
     * exists, then just do the search thru the vector.
     *
     * @return the damage distribution probability for the given asset & state. Returns
     * <i>0</i> if a distribution probability is not available.
     */
     //public double getDamageProbability(AssetTechSpecInterface asset, AssetStateDescriptor assetState, StateValue startVal, StateValue endVal) throws NoSuchAssetException;
    

    /**
     * @return the asset type that the threat cares about.
     */
    public AssetType getAssetType();
    
    /**
     * @return The damage distribution for the specified asset and asset state
     */
    //public DamageDistribution getDamageDistribution(AssetTechSpecInterface asset, AssetStateDescriptor assetState) throws NoSuchAssetException;
     
     /**
      * @return the ThreatType associated with this ThreatModel
      */
     public ThreatType getThreatType();
     
    /** 
     * @return TRUE if this Threat cares about this asset.
     */
    public boolean containsAsset(AssetTechSpecInterface asset);
     
}


