/*
 * Loggable.java
 *
 * Created on May 24, 2004
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

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

/**
 * Superclass for all cougaar classes that want to use the cougaar
 * logging mechanism. 
 *
 * @author Tony Cassandra
 * @version $Revision: 1.28 $Date: 2004-08-09 20:46:41 $
 * 
 *
 */
abstract class Loggable
{

    // Class implmentation comments go here ...

    //------------------------------------------------------------
    // package interface
    //------------------------------------------------------------

    /**
     * Main constructor
     */
    Loggable()
    {
        _logger = Logging.getLogger(this.getClass().getName());
    } // constructor Loggable

    //------------------------------------------------------------
    // protected interface
    //------------------------------------------------------------

    /**
     * Log an error message.
     *
     * @param msg The error message to log.
     */
    protected void logError( String msg )
    {
        _logger.error( msg );
    } // method logError


    /**
     * Log a warning
     *
     * @param msg The error message to log.
     */
    protected void logWarning( String msg )
    {
         _logger.warn( msg );
    } // method logWarning


    /**
     * Log a debugging message if debugging is enabled.
     *
     * @param msg The message to log if debugging is enabled.
     */
    protected void logDebug( String msg )
    {
        if ( _logger.isDebugEnabled() )
            _logger.debug( msg );

    } // method logDebug


    /**
     * Log some information
     *
     * @param msg The error message to log.
     */
    protected void logInfo( String msg )
    {
        if ( _logger.isInfoEnabled() )
         _logger.info( msg );
    } // method logInfo

    /**
     * Log some detail information
     *
     * @param msg The detail message to log.
     */
    protected void logDetail( String msg )
    {
        if ( _logger.isDetailEnabled() )
         _logger.detail( msg );
    } // method logDetail

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    // For logging debug, warning and error messages to the cougaar
    // logging system
    //
    Logger _logger;

} // class Loggable
