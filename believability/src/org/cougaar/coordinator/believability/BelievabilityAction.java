/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: BelievabilityAction.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/BelievabilityAction.java,v $
 * $Revision: 1.1 $
 * $Date: 2004-06-09 18:00:22 $
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

import org.cougaar.coordinator.Action;

/**
 * The class that contains a local copy of pertinent information
 * related to the action.
 * 
 * @author Tony cassandra
 */
class BelievabilityAction extends BeliefUpdateTrigger 
{

    //---------------------------------------------------------------
    // public interface
    //---------------------------------------------------------------

    /**
     * Return the believability action as a string
     */
    public String toString() 
    {
        StringBuffer buff = new StringBuffer();

        // FIXME: Change this when body of this class is filled out.

        buff.append( "BelievabilityAction: ??? " );

        return buff.toString();
    } // method toString

    //---------------------------------------------------------------
    // package interface
    //---------------------------------------------------------------

    /**
     * Constructor, using an Action from the blackboard (via an
     * ActionsWrapper)
     *
     * @param action The action from the blackboard
     */
    BelievabilityAction( Action action ) 
    {
        super( null, null );

        _blackboard_action = action;

        // FIXME: 
        logError( "BelievabilitAction class is not implemented." );

    } // constructor BelievabilityAction
    
    /**
     * Return the last time the sensor asserted a value different from
     * the previous value
     * @return the timestamp
     **/
    long getTriggerTimestamp() 
    { 
        // FIXME: 
        logError( "BelievabilitAction class is not implemented." );

        return 0;

    } // method getTriggerTimestamp

    /**
     * This routine should return the asset statew dimension name that
     * this trigger pertains to.
     */
    String getStateDimensionName()
    {
        // FIXME: 
        logError( "BelievabilitAction class is not implemented." );

        return null;

    } // method getStateDimensionName

    //---------------------------------------------------------------
    // private interface
    //---------------------------------------------------------------

    // The object where the action information came from
    //
    private Action _blackboard_action;

} // class BelievabilityAction

