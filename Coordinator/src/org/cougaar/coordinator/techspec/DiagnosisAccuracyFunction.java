/*
 * DiagnosisAccuracyFunction.java
 *
 * Created on August 5, 2003, 2:47 PM
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
