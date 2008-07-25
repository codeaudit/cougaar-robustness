/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: Model.java,v $
 *</NAME>
 *
 *<COPYRIGHT>
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
 *</COPYRIGHT>
 *
 *</SOURCE_HEADER>
 */

package org.cougaar.coordinator.believability;

import java.util.Observable;
import java.util.Observer;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

/**
 * Superclass for all models local to the Believability plugin.  Just
 * captures common attributes that we want to share for all the
 * models.  This includes a validity state flag and logging
 * facilities. 
 *
 * @author Tony Cassandra
 * @version $Revision: 1.1 $Date: 2008-07-25 20:47:16 $
 * 
 *
 */
abstract class Model extends Observable implements Observer
{

    // Class implmentation comments go here ...

    // For logging
    protected Logger _logger = Logging.getLogger(this.getClass().getName());

    //------------------------------------------------------------
    // public interface
    //------------------------------------------------------------

    /**
     *
     */
    Model()
    {
    } // constructor Model

    /**
     * Whether or not the contents of this model are complete and have
     * been sanity checked.
     *
     * @param
     */
    boolean isValid( )
    {
        return _valid;
    }  // method isValid

    //**************************************************************
    /** 
     * This is the routine that implements the Observer interface.
     * Not all models will need to be observers, so we provide a
     * default dummy method here for them.
     * 
     * @param  Observable The object that changed.
     * @param Object An argument about how it changed.
     */
    public void update( Observable observable, Object arg ) 
    {

        return;

    } // update

    //------------------------------------------------------------
    // protected interface
    //------------------------------------------------------------

    /**
     * Sets the validity of this model.
     *
     * @param valid Value to use for current valid status
     */
    protected void setValidity( boolean valid )
    {
        _valid = valid;
    }  // method setValidity

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    // This flag will indicate whether the contents of this model are
    // valid.  It is assumed to be invalidate until a successful
    // parsing of the the source of information.  
    //
    private boolean _valid = false;

} // class Model
