/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: AlarmExpirationHandler.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/AlarmExpirationHandler.java,v $
 * $Revision: 1.3 $
 * $Date: 2004-07-15 20:19:41 $
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

import org.cougaar.core.agent.service.alarm.Alarm;

/**
 * Simple interface to help deal with handling Cougaar alarm
 * expirations.
 *
 * @author Tony Cassandra
 * @version $Revision: 1.3 $Date: 2004-07-15 20:19:41 $
 *
 */
public interface AlarmExpirationHandler
{

    // Class implmentation comments go here ...

    //------------------------------------------------------------
    // public interface
    //------------------------------------------------------------

    /**
     * This will be called when the alarm expires, if it has not been
     * cancelled previously.  
     *
     * @param alarm the alarm object that has expired.
     */
    public void handleAlarmExpired( Alarm alarm )
            throws BelievabilityException;

} // interface AlarmExpirationHandler
