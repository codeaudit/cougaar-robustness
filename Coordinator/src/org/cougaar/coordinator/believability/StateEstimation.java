/*
 * StateEstimation.java
 *
 * Created on July 8, 2003, 4:17 PM
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

package org.cougaar.coordinator.believability;

import org.cougaar.coordinator.DefenseApplicabilityConditionSnapshot;

import org.cougaar.core.persist.NotPersistable;

import java.util.Hashtable;
import java.util.Vector;

/**
 * Temporarily this object will use a hashtable to store the data
 * required by consumers of it.
 *
 * The hashtable is specific to a given asset. For each StateDescriptor 
 * (key) in the hashtable, it stores the AssetBeliefState for that asset 
 * with respect to the StateDescriptor.
 * 
 */
public class StateEstimation extends Hashtable implements NotPersistable 
{
    
    
    /** Creates a new instance of StateEstimation */
    public StateEstimation() {
    }
    

    /** Creates a new instance of StateEstimation */
    public StateEstimation( String extended_asset_name ) {
     _extended_asset_name = extended_asset_name;
    }

    /** Accessors */
    public String getAssetName() { return _extended_asset_name; }

    /**
     * Accessor for the list of defense conditions that were used in
     * the belief update calculations.
     *
     * @return The Vector of DefenseApplicabilityConditionSnapshots
     * used in the belief update computation.
     */
    public Vector getDefenseConditions() { return _defense_cond_list; }

    /**
     * Adds a defense condition.
     */
    public void addDefenseCondition( DefenseApplicabilityConditionSnapshot dc )
    {
        this._defense_cond_list.addElement( dc ); 
    }

    /*
     * Accessing whether or not there was an error encountered while
     * trying to create this state estimation object.
     *
     * @return true if an error was logged, and false if everything
     * went ok.
     */
    public boolean hasError() { return this._error; }

    /*
     * Returns the accumulated error messages when an error exists.
     *
     * @return The list of error messages, or an empty String if there
     * were no errors.
     */
    public String getErrorMessage() { return _error_msg_buff.toString(); }

    /**
     * Sets the error condition of the object and adds a message.
     *
     * @param err_msg The message to append to the fulle S.E. error
     * message.  
     */
    public void logError( String err_msg )
    {
        this._error = true;
        _error_msg_buff.append( err_msg );
    }

    //************************************************************
    // Asset that this state estimation concerns
    //
    private String _extended_asset_name = null;

    // It is useful to be able to access the individual defenbse
    // conditions downstream.  Since they are the same for all
    // AssetBeliefState, we store them also in the StateEstimation
    // object.
    private Vector _defense_cond_list = new Vector();

    // Useful to track errors so we can pass them along.
    private boolean _error = false;
    private StringBuffer _error_msg_buff = new StringBuffer();

}
