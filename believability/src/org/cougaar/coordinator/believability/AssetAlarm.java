/**
 * AssetAlarm.java
 *
 * Created on April 24, 2004
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
 **/

package org.cougaar.coordinator.believability;

import org.cougaar.coordinator.techspec.AssetID;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.util.log.Logger;


/**
 * This class implements an asset-specific alarm to be used to time
 * diagnosis periods.
 **/

public class AssetAlarm implements Alarm {
        
    /**
     * Constructor. Starts the alarm
     * @param asset_model the model for the associated asset.
     * @param alarm_period the length of the alarm.
     * @param callback the object to call back to when the alarm expires
     **/
    public AssetAlarm ( AssetID asset_id, long alarm_period, StateEstimationPublisher callback ) {
	_asset_id = asset_id;
	_callback = callback;
	_expired = false;
	_start_time = System.currentTimeMillis();
	_expiration_time = _start_time + alarm_period;

	if (_logger.isInfoEnabled())
	    _logger.info("Asset alarm set for asset " 
			 + _asset_id.toString() 
			 + " for time "
			 + _expiration_time );
    }

        
    /** 
     * Get the expiration time
     * @return The expiration time, as a long.
     **/
    public long getExpirationTime () {
	return _expiration_time;
    }
        

    /**
     * Expire the alarm
     **/
    public void expire () {

	if ( _expired ) return;
	_expired = true;
	if (_logger.isInfoEnabled()) 
	    _logger.info("Alarm expired for: " 
			 + _asset_id.toString());

	_callback.timerCallback( _asset_id, true );
    }



    /**
     * Check to see if the alarm has expired.
     **/
    public boolean hasExpired () {
	return _expired;
    }


    /**
     * Cancel the alarm. 
     * @returns true if the alarm is successfully canceled, false if it
     *               expired before it was canceled or was already canceled
     **/
    public boolean cancel () {

	// Make sure the canceled flag is set properly
	if ( _canceled ) return false;

	// Check if you have already expired or not.
	if ( hasExpired() ) return false;

	// Otherwise cancel everything.
	_canceled = true;
	if (_logger.isInfoEnabled()) 
	    _logger.info("Alarm canceled for: " 
			 + _asset_id.toString());

	_callback.timerCallback( _asset_id, false );
	return true;
    }

 
    // The asset id for the asset that this alarm is attached to
    private AssetID _asset_id;

    // The object to call back to
    private StateEstimationPublisher _callback;

    // The start time of the alarm
    private long _start_time;

    // The expiration time of the alarm
    private long _expiration_time;

    // An indicator that the alarm has been canceled.
    private boolean _canceled = false;

    // An indicator that the alarm has expired.
    private boolean _expired = false;

    // Private logger for error messages
    private Logger _logger;
} // end class AssetAlarm

