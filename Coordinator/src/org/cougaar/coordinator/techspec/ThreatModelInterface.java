/*
 * ThreatModelInterface.java
 *
 * Created on July 9, 2003, 9:22 AM
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
 * //@deprecated Likely deprecated in 2004.
 * @see ThreatDescription
 * @author Paul Pazandak, Ph.D. OBJS, Inc.
 */
public interface ThreatModelInterface extends TechSpecRootInterface {
    

    /**
     *@return the vector of asset tech specs that this threat model affects.
     * That is, the set of assets that might be affected by this threat.
     *
     */
    public Vector getAssetList();
    
    /**
     *@return the threat likelihood during the given time interval
     * @deprecated - use getProbabilityOfEvent
     */
    public double getThreatLikelihood(long start_time, long end_time)  throws NegativeIntervalException;

    /**
     * @return the probability that this threat will occur. Returns 0 if there are no probabilities defined
     * @throws NegativeIntervalException if end < start. 
     */
    public double getProbabilityOfEvent(long start, long end) throws NegativeIntervalException;
    
    /**
     *@return the vector of associated damage distribution instances. This may be 
     * a dynamically generated vector.
     *@deprecated
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
      *@deprecated
      */
     public ThreatType getThreatType();
     
    /** 
     * @return TRUE if this Threat cares about this asset.
     */
    public boolean containsAsset(AssetTechSpecInterface asset);

    /**
     * @return the ThreatDescription techspec for this threat.
     */
    public ThreatDescription getThreatDescription();
    
    
}


