/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: IntervalAlarm.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/IntervalAlarm.java,v $
 * $Revision: 1.3 $
 * $Date: 2004-07-15 20:19:42 $
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
 * Base class for all alarms that are based on the passing of some
 * interval of time.
 *
 * @author Tony Cassandra
 * @version $Revision: 1.3 $Date: 2004-07-15 20:19:42 $
 *
 */
public class IntervalAlarm extends Loggable implements Alarm
{

    // Class implmentation comments go here ...

    //------------------------------------------------------------
      // public interface
    //------------------------------------------------------------

    /**
     * Constructor documentation comments go here ...
     *
     * @param
     */
    public IntervalAlarm( long duration,
                          AlarmExpirationHandler handler )
    {
        this._handler = handler;
        _start_time = System.currentTimeMillis();
        _duration = duration;
        _expired = false;
        _cancelled = false;
        
    }  // constructor IntervalAlarm

    //------------------------------------------------------------
    // Alarm interface
    //------------------------------------------------------------

    //************************************************************
    /**
     * Can be called by a client to cancel the alarm. May or may not
     * remove the alarm from the queue, but should prevent expire from
     * doing anything.
     *
     * @return false IF the the alarm has already expired or was
     * already canceled.
     */
    public boolean cancel()
    {
        boolean was_cancelled = _cancelled;

        _cancelled = true;

        return _expired || was_cancelled;

    } // method cancel

    //************************************************************
    /**
     * Called by the cluster clock when clock-time >=
     * getExpirationTime(). The system will attempt to Expire the
     * Alarm as soon as possible on or after the ExpirationTime, but
     * cannot guarantee any specific maximum latency. NOTE: this will
     * be called in the thread of the cluster clock. Implementations
     * should make certain that this code does not block for a
     * significant length of time. If the alarm has been canceled,
     * this should be a no-op.
     */
    public void expire()
    {
        if ( _expired || _cancelled )
            return;

        _expired = true;

        try
        {
            _handler.handleAlarmExpired( this );
        }
        catch (BelievabilityException be)
        {
            logDebug( "Problem handling alarm expire: " + be.getMessage() );
        }

    } // method expire

    //************************************************************
    /**
     * @return absolute time (in milliseconds) that the Alarm should
     * go off. This value must be implemented as a fixed value.
     */
    public long getExpirationTime()
    {
        return _start_time + _duration;
    } // method getExpirationTime

    //************************************************************
    /**
     * @return true IFF the alarm has rung (expired) or was canceled.
     */
    public boolean hasExpired()
    {
        return _expired || _cancelled;
    } // method hasExpired

    //************************************************************
    /**
     * Typical way to give some useful representation of this object.
     *
     */
    public String toString( )
    {
        StringBuffer buff = new StringBuffer();

        buff.append( "IntervalAlarm: start=" + _start_time
                     + ", duration=" + _duration );

        if ( _expired )
            buff.append( " [expired]" );

        if ( _cancelled )
            buff.append( " [cancelled]" );

        return buff.toString();
    } // method toString
    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    private AlarmExpirationHandler _handler;
    private long _start_time;
    private long _duration;
    private boolean _expired;
    private boolean _cancelled;

} // class IntervalAlarm
