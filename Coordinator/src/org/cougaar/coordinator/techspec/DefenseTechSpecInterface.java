/*
 * DefenseTechSpec.java
 *
 * Created on July 9, 2003, 9:24 AM
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
 *
 * @author  Paul Pazandak, Ph.D. OBJS, Inc.
 */
public interface DefenseTechSpecInterface extends TechSpecRootInterface {

    
    /**
     * @return the vector of states that the defense described by this 
     * tech spec can be in
     */
    public Vector getStates();
    
    /**
     * @return a boolean indicating if this defense implements an idempotent action or not.
     *
     */
    public boolean isIdempotent();
    
    /**
     * @return a boolean indicating if this defense implements a reversible action or not.
     *
     */
    public boolean isReversible();

    
    /**
     * @return the asset type that the threat cares about.
     */
    public AssetType getAssetType();
    
    /**
     * @return the vector of monitoring levels that this defense supports
     *
     */
    public Vector getMonitoringLevels();
    
    /**
     * @return the Defense Finite State Machine object for this defense
     *
     */
    public DefenseFSM getDefenseFSM();

    
     /**
      * @return the ThreatTypes associated with this Defense
      */
     public Vector getThreatTypes();
   
     
    /** 
      * @return The asset state descriptor that this defense affects / diagnoses 
      */
    public AssetStateDimension getAffectedAssetState();

        /**
      * @return the cost of executing this defense
      */
     public double t_getCost();

     /**
      * @return the benefit of executing this defense
      */
     public double t_getBenefit();

}
