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

import org.cougaar.coordinator.DiagnosesWrapper;
import org.cougaar.coordinator.Diagnosis;
import org.cougaar.coordinator.DiagnosisUtils;

import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.techspec.AssetType;
import org.cougaar.coordinator.techspec.DiagnosisTechSpecInterface;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;


/**
 * The class that contains a local copy of pertinent information
 * related to the diagosis.
 * @author Misty Nodine
 */
public class BelievabilityDiagnosis
{

    /**
     * Constructor, from a diagnosis on the blackboard
     * @param bb_diagnosis The DiagnosesWrapper from the blackboard
     **/
    public BelievabilityDiagnosis( DiagnosesWrapper bb_diagnosis ) {
	
	// Pull the diagnosis from the blackboard
	_blackboard_diagnosis = bb_diagnosis.getDiagnosis();

	// Copy relevant information from the diagnosis, as it may change
	_diagnosis_value = (String) _blackboard_diagnosis.getValue();
	_diagnosis_TS = _blackboard_diagnosis.getTechSpec();

	//****** Note that in the tech spec, the method
	//    getDiagnosisProbabilities returns a Vector of DiagnosisProbability objects
	//    getCrossDiagnosisProbabilities returns a Vector of CrossDiagnosis objects

	_last_asserted_timestamp = 
	    _blackboard_diagnosis.getLastAssertedTimestamp();
        _last_changed_timestamp =
	    _blackboard_diagnosis.getLastChangedTimestamp();
        _asset_id = DiagnosisUtils.getAssetID( _blackboard_diagnosis );
        _asset_type = DiagnosisUtils.getAssetType( _blackboard_diagnosis );
    }


    /**
     * Return the value of the diagnosis, as published by the sensor
     * @return the diagnosis value
     **/
    public String getDiagnosisValue() { return _diagnosis_value; }


    /**
     * Return the last time the sensor asserted a value
     * @return the timestamp
     **/
    public long getLastAssertedTimestamp() { 
	return _last_asserted_timestamp; 
    }


    /**
     * Return the last time the sensor asserted a value different from
     * the previous value
     * @return the timestamp
     **/
    public long getLastChangedTimestamp() { 
	return _last_changed_timestamp; 
    }


    /**
     * Return the identifier of the affected asset
     * @return the AssetID for the affected asset
     **/
    public AssetID getAssetID() { 
	return _asset_id; 
    }


    /**
     * Return the type of the affected asset
     * @return the AssetType of the affected asset
     **/
    public AssetType getAssetType() { 
	return _asset_type; 
    }


    /**
     * Return the believability diagnosis as a string
     **/
    public String toString() {
	StringBuffer sb = new StringBuffer();
	sb.append( "BelievabilityDiagnosis: asset " );
	sb.append( _asset_id.toString() );
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

    // The interface to the diagnosis tech spec
    private DiagnosisTechSpecInterface _diagnosis_TS;

    // The last time the sensor asserted a value
    private long _last_asserted_timestamp;

    // The last time the sensor asserted a value that was different from
    // the previous value
    private long _last_changed_timestamp;

    // The AssetID of the affected asset
    private AssetID _asset_id;

    // The type of the affected asset
    private AssetType _asset_type;

    // Logger for error messages
    private Logger _logger;

} // class BeliefUpdate
