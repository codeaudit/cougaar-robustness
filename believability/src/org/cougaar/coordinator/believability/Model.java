/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: Model.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/Model.java,v $
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
 * @version $Revision: 1.1 $Date: 2004-05-20 21:39:49 $
 * 
 *
 */
abstract class Model extends Observable implements Observer
{

    // Class implmentation comments go here ...

    //------------------------------------------------------------
    // public interface
    //------------------------------------------------------------

    /**
     *
     */
    Model()
    {
        _logger = Logging.getLogger(this.getClass().getName());
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

    // This flag will indicate whether the contents of this model are
    // valid.  It is assumed to be invalidate until a successful
    // parsing of the the source of information.  
    //
    private boolean _valid = false;

    // For logging debug, warning and error messages to the cougaar
    // logging system
    //
    Logger _logger;

} // class Model
