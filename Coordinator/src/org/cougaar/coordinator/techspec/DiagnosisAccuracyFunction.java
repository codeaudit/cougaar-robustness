/*
 * DiagnosisAccuracyFunction.java
 *
 * Created on August 5, 2003, 2:47 PM
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
import org.cougaar.core.persist.NotPersistable;

/**
 * @author Paul Pazandak, Ph.D. OBJS, Inc.
 */
public class DiagnosisAccuracyFunction implements NotPersistable {
    
    /**
     * The state of the asset
     */
    private AssetStateDimension assetState;

    /**
     * the state value of the asset
     */
    private AssetState stateValue;
    
    /**
     * This AssetState must be one that is associated with the AssetStateDimension defined for this defense.
     */
    private AssetState diagnosisState;

    /**
     * if the asset is in assetState:stateValue, the Defense will say the asset is in diagnosisState with this probability. 
     */
    private double probability;
    
    /** Creates a new instance of DiagnosisAccuracyFunction 
     *
     *
     * if the asset is in assetState:stateValue, the Defense will say the asset is in diagnosisState with p=probability. E.g. 
     *
     *   <p><b>Example 1: </b>
     *   diagnosisState="Dead" assetStateDescriptor="EXECUTION" assetStateValue="Dead" probability="0.7" <p> <b>is interpreted as: </b><p>
     *   if the asset is "Dead", the Defense will say the asset is "Dead" with p=0.7 (a correct diagnosis) and implying the Defense will say "OK" with p=0.3, (a misdiagnosis)
     *<p><p>
     *   <p><b>Example 2: </b>
     *  diagnosisState="Dead" assetStateDescriptor="COMMUNICATION" assetStateValue="NotCommunicating" probability="0.5" <p> <b>is interpreted as: </b><p>
     *  if the asset is "NotCommunicating", the Defense will say the asset is "Dead" with p=0.5 (a misdiagnosis) and implying the Defense will say "OK" with p=0.5 (also a misdiagnosis)
     *<p>
     *
     * <i>Note: The diagnosisState must be one that is associated with the AssetStateDimension defined for this defense.</i>
     *
     */
    public DiagnosisAccuracyFunction(AssetStateDimension assetState, AssetState stateValue, AssetState diagnosisState, double probability) {
    
        this.assetState = assetState;
        this.stateValue = stateValue;
        this.diagnosisState = diagnosisState;
        this.probability = probability;    
    }
    
    /**
     *@return the AssetStateDimension
     */
    public AssetStateDimension getAssetState() {return assetState;}

    /**
     *@return the diagnosis state
     */
    public AssetState getAssetStateValue() {return stateValue;}
    
    /**
     *@return the diagnosis state value - This AssetState must be one that is associated with the AssetStateDimension 
     *          defined for this defense.
     */
    public AssetState getDiagnosisStateValue() {return diagnosisState;}
    
    /**
     *@return the probability
     */
    public double getProbability() { return probability; }
}
