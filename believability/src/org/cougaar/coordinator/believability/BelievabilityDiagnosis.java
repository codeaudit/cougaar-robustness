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

/**
 * The class that contains a local copy of pertinent information
 * related to the diagosis.
 * @author Misty Nodine
 */
class BelievabilityDiagnosis extends DiagnosisTrigger
{

    /**
     * Constructor, from a diagnosis on the blackboard
     * @param diag The diagnosis from the blackboard
     **/
    BelievabilityDiagnosis( Diagnosis diag ) 
    {

        super( DiagnosisUtils.getAssetID( diag ) );

        // Copy relevant information from the diagnosis, as it may change
        //
        _sensor_name = diag.getClass().getName();
        _sensor_value = (String) diag.getValue();

        _sensor_state_dimension = diag.getAssetStateDimensionName();

        setTriggerTimestamp( diag.getLastAssertedTimestamp() );
    }

} // class BelievabilityDiagnosis

