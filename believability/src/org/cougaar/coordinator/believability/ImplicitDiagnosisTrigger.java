/**
 * ImplicitDiagnosisTrigger.java
 *
 * Created on July 14, 2004
 *
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

import org.cougaar.coordinator.techspec.AssetID;

/**
 * The class that contains a local copy of pertinent information
 * related to the diagosis.
 *
 * @author Tony Cassandra
 */
class ImplicitDiagnosisTrigger extends DiagnosisTrigger
{

    //************************************************************
    /**
     * Main constructor
     *
     * @param asset_id The asset ID this diagnosis is on
     * @param sensor_name  The sensor name that generated the diagnosis
     * @param sensor_value The diagnosis value itself
     * @param state_dim_name The state dimension of the asset that was
     * diagnosed
     * @param time The time to set for the timestamps on this trigger.
     **/
    ImplicitDiagnosisTrigger( AssetID asset_id,
                              String sensor_name,
                              String sensor_value,
                              String state_dim_name, 
                              long time ) {
        
        super( asset_id );
        
        _sensor_name = sensor_name;
        _sensor_value = sensor_value;
        _sensor_state_dimension = state_dim_name;
        
        setTriggerTimestamp( time );

    } // constructor

} // class ImplicitDiagnosisTrigger

