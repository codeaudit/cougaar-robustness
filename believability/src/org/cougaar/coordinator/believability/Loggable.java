/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: Loggable.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/Loggable.java,v $
 * $Revision: 1.1 $
 * $Date: 2004-05-20 21:39:49 $
 *</RCS_KEYWORD>
 *
 *<COPYRIGHT>
 * The following source code is protected under all standard copyright
 * laws.
 *</COPYRIGHT>
 *
 *</SOURCE_HEADER>
 */

package org.cougaar.coordinator.believability;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

/**
 * Superclass for all models local to the Believability plugin.  Just
 * captures common attributes that we want to share for all the
 * models.  This includes a validity state flag and logging
 * facilities. 
 *
 * @author Tony Cassandra
 * @version $Revision: 1.1 $Date: 2004-05-20 21:39:49 $
 * 
 *
 */
abstract class Loggable
{

    // Class implmentation comments go here ...

    //------------------------------------------------------------
    // public interface
    //------------------------------------------------------------

    /**
     *
     */
    Loggable()
    {
        _logger = Logging.getLogger(this.getClass().getName());
    } // constructor Loggable

    //------------------------------------------------------------
    // protected interface
    //------------------------------------------------------------

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
     * Log an error message.
     *
     * @param msg The error message to log.
     */
    protected void logError( String msg )
    {
        _logger.error( msg );
    } // method logError

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    // For logging debug, warning and error messages to the cougaar
    // logging system
    //
    Logger _logger;

} // class Loggable
