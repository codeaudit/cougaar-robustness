 /*
 * MonitoringLevel.java
 *
 * Created on August 5, 2003, 11:39 AM
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
import java.util.Iterator;
import org.cougaar.core.persist.NotPersistable;

/**
 * @author Paul Pazandak, Ph.D. OBJS, Inc.
 */
public class MonitoringLevel implements NotPersistable {
    
    /** Name of the monitoring level */
    private String name;
    
    /** Holds DiagnosisAccuracyFunction instances */
    private Vector diagnoses;
    
    /** diagnosis latency */
    private long latency;
    
    /** cost */
    private double cost;

    /** Creates a new instance of MonitoringLevel */
    public MonitoringLevel(String name, long latency, double cost) {
        this.name = name;
        this.cost = cost;
        diagnoses = new Vector();
        setDiagnosisLatency(latency);
    }
    
    /**
     * @return a user-readable name for this Monitoring Level
     *
     */
    public String getName() {return name; }

    
    /**
     * @return the cost value for the period specified
     *
     */
    public int getCost(long startTime, long endTime) {        
        //fill function in here.
        return -1;        
    }
    
    /**
     *@return the vector of DiagnosisAccuracyFunction instances
     *
     */
    public Vector getDiagnosisAccuracy() {
      
        //should we return a clone of the vector so the caller cannot change it?
        return diagnoses;
    }
    
    /**
     * @return the transition probability for the given states. Returns 0 if a
     * corresponding function is not found.
     */
     public double getProbability(AssetStateDimension assetStateDim, AssetState stateValue, AssetState diagnosisStateValue) {
         
          Iterator iter = diagnoses.iterator();
          while (iter.hasNext()) {
              DiagnosisAccuracyFunction daf = (DiagnosisAccuracyFunction) iter.next();
              if (daf.getAssetState().equals(assetStateDim) && daf.getAssetStateValue().equals(stateValue) &&
                  daf.getDiagnosisStateValue().equals(diagnosisStateValue))
                     return daf.getProbability();
          }
          return 0; // 0 probability
     }
     
    
     /**
      * Add a diagnosisAccuracyFunction instance
      */
     public void addDiagnosis(DiagnosisAccuracyFunction daf) {
         diagnoses.addElement(daf);
     }
     
     /**
      * Remove a diagnosisAccuracyFunction instance
      */
     public void removeDiagnosis(DiagnosisAccuracyFunction daf) {
         diagnoses.removeElement(daf);
     }
     
     /**
      * @return the diagnosis latency associated with this monitoring level
      * (the amount of time it may take for this defense to determine that
      * it is applicable).
      */
     public long getDiagnosisLatency() { return latency; }

     /**
      * Set the diagnosis latency associated with this monitoring level
      * (the amount of time it may take for this defense to determine that
      * it is applicable).
      */
     public void setDiagnosisLatency(long latency) { this.latency = latency; }
     
}
