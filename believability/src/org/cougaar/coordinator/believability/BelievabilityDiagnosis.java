/**
 * BelievabilityDiagnosis.java
 *
 * Created on April 28, 2004
 * <copyright>
 *  Copyright 2004 Telcordia Technoligies, Inc.
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

package org.cougaar.coordinator.believability;

import org.cougaar.coordinator.Diagnosis;
import org.cougaar.coordinator.DiagnosisUtils;

import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.techspec.AssetType;
import org.cougaar.coordinator.techspec.DiagnosisTechSpecInterface;
import org.cougaar.coordinator.techspec.DiagnosisTechSpecService;

/**
 * The class that contains a local copy of pertinent information
 * related to the diagosis.
 * @author Misty Nodine
 */
public class BelievabilityDiagnosis extends BeliefUpdateTrigger
{

    /**
     * Constructor, from a diagnosis on the blackboard
     * @param diag The diagnosis from the blackboard
     **/
    public BelievabilityDiagnosis( Diagnosis diag ) {

        super( DiagnosisUtils.getAssetID( diag ) );

     _blackboard_diagnosis = diag;
     
     // Copy relevant information from the diagnosis, as it may change
     _diagnosis_value = (String) _blackboard_diagnosis.getValue();
     _sensor_name = _blackboard_diagnosis.getClass().getName();
     _diagnosis_state_dimension = 
         _blackboard_diagnosis.getAssetStateDimensionName();

     _last_asserted_timestamp = 
         _blackboard_diagnosis.getLastAssertedTimestamp();
     _last_changed_timestamp =
         _blackboard_diagnosis.getLastChangedTimestamp();
    }


    /**
     * Return the value of the diagnosis, as published by the sensor
     * @return the diagnosis value
     **/
    String getDiagnosisValue() { return _diagnosis_value; }


    /**
     * Return the last time the sensor asserted a value
     * @return the timestamp
     **/
    long getLastAssertedTimestamp() { 
     return _last_asserted_timestamp; 
    }


    /**
     * Return the last time the sensor asserted a value different from
     * the previous value
     * @return the timestamp
     **/
    long getTriggerTimestamp() { 
     return _last_changed_timestamp; 
    }


    /**
     * This routine should return the asset statew dimension name that
     * this trigger pertains to.
     */
    String getStateDimensionName()
    {
        return _diagnosis_state_dimension;
    } // method getStateDimensionName


    /**
     * Diagnoses do not require immediate publication
     **/
    public boolean requiresImmediateForwarding() {
        return false;
    }


    /**
     * Return the name of the sensor that made this diagnosis
     * @return 
     **/
    public String getSensorName() { 
     return _sensor_name;
    }


    /**
     * Return the believability diagnosis as a string
     **/
    public String toString() {
     StringBuffer sb = new StringBuffer();
     sb.append( "BelievabilityDiagnosis: asset " );
     sb.append( this.getAssetID().toString() );
     sb.append( " with sensor " );
     sb.append( _sensor_name );
     sb.append( " asserted diagnosis " );
     sb.append( _diagnosis_value );
     sb.append( " at time " + _last_asserted_timestamp );
     return sb.toString();
    }


    //---------------------------------------------------------------
    // Private interface
    //---------------------------------------------------------------

    // The place the diagnosis information came from
    private Diagnosis _blackboard_diagnosis;

    // The value of the diagnosis, in the terms that the sensor uses
    private String _diagnosis_value;

    // The name of the sensor
    private String _sensor_name;

    // The state dimension that the diagnosis concerns
    private String _diagnosis_state_dimension;

    // The last time the sensor asserted a value
    private long _last_asserted_timestamp;

    // The last time the sensor asserted a value that was different from
    // the previous value
    private long _last_changed_timestamp;

} // class BelievabilityDiagnosis

