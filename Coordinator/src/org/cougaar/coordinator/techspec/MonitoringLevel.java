 /*
 * MonitoringLevel.java
 *
 * Created on August 5, 2003, 11:39 AM
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
     public double getProbability(AssetStateDimension assetState, StateValue stateValue, StateValue diagnosisStateValue) {
         
          Iterator iter = diagnoses.iterator();
          while (iter.hasNext()) {
              DiagnosisAccuracyFunction daf = (DiagnosisAccuracyFunction) iter.next();
              if (daf.getAssetState().equals(assetState) && daf.getAssetStateValue().equals(stateValue) &&
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
