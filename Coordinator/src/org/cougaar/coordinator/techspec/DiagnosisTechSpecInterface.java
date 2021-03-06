/*
 * DiagnosisTechSpec.java
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
import java.util.Set;
import org.cougaar.core.util.UID;


/**
 *
 * @author  Paul Pazandak, Ph.D. OBJS, Inc.
 */
public interface DiagnosisTechSpecInterface extends TechSpecRootInterface {

    static final int SNAPSHOT = 1;
    static final int ALWAYS_CURRENT = 2;
    
    
    /** @return a user-readable name for this TechSpec
     *
     */
    public String getName();
    
    
    /** @return a revision string for this TechSpec
     *
     */
    public String getRevision();
    
    
    /** @return a unique cougaar level name for this TechSpec
     *
     */
    public UID getUID();

  
    /** @return the State Dimension that the sensor is watching.
     */
    public AssetStateDimension getStateDimension();

    /** @return the latency -- the time it takes this sensor to diagnose/notice & report a problem, in milliseconds.
     */
    public int getLatency();    
    
    /** @return the reportingPolicy -- either SNAPSHOT | ALWAYS_CURRENT
     */
    public int getReportingPolicy();
    
    /** @return the vector of monitoring levels that this sensor supports
     *
     */
//    public void addMonitoringLevel() {
//        levels.add(null);
//    }
    
    /** 
     * Set the possible values that Diagnoses generated by this Sensor can return via getValue()
     */
    public void addPossibleValue(String v);
   
    /** @return the default value that Diagnoses generated by this Sensor should default to when initialized     
     */
    public String getDefaultValue();
    
    /** @return a revision string for this TechSpec
     *
     */
    public void setRevision(String r);
    
    /** @return the ThreatTypes associated with this Defense
     */
//    public void addThreatType() {
//        threatTypes.add(null);
//    }
    
    /**
     * @return the DiagnosisProbabilities
     *
     */
    public Vector getDiagnosisProbabilities();


    /**
     * @return the CrossDiagnosisProbabilities
     *
     */
    public Vector getCrossDiagnosisProbabilities();
    
    /**
     * add a CrossDiagnosisProbability
     *
     */
    public void addCrossDiagnosisProbability(CrossDiagnosis cd);
    

    
    
    /**
     * Add a DiagnosisProbability
     *
     */
    public void addDiagnosisProbability(DiagnosisProbability dp);
    
    
    /**
     * @return the possible values that Diagnoses generated by this Sensor can return via getValue()
     */
    public Set getPossibleValues();
    
    /**
     * @return the asset type that the threat cares about.
     */
    public AssetType getAssetType();
    
    /**
     * @return the vector of monitoring levels that this sensor supports
     *
     */
    public Vector getMonitoringLevels();
    
     /**
      * @return the ThreatTypes associated with this Defense
      */
     public Vector getThreatTypes();
   

}
