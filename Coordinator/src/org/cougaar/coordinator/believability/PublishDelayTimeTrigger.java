/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: PublishDelayTimeTrigger.java,v $
 *</NAME>
 *
 * <copyright>
 *  Copyright 2004 Telcordia Technologies, Inc.
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
 * This class serves for concrete instances of belief update triggers
 * that are based solely on time (no diagnosis or action.) 
 * 
 * @author Tony Cassandra
 */
class PublishDelayTimeTrigger extends TimeUpdateTrigger 
{

    //---------------------------------------------------------------
    // package interface
    //---------------------------------------------------------------

    /**
     * Main constructor
     *
     * @param asset_id The asset ID
     * @param time The time that the update happened or was triggered
     */
    PublishDelayTimeTrigger( AssetID asset_id, long time ) 
    {
        super( asset_id, time );

    } // constructor PublishDelayTimeTrigger


} // class PublishDelayTimeTrigger

