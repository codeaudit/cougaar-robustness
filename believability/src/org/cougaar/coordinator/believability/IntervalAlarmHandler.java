/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: IntervalAlarmHandler.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/IntervalAlarmHandler.java,v $
 * $Revision: 1.4 $
 * $Date: 2004-10-20 16:48:21 $
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

import java.util.Iterator;
import java.util.Vector;

import org.cougaar.core.service.AlarmService;
import org.cougaar.core.service.BlackboardService;

import org.cougaar.core.agent.service.alarm.Alarm;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

/**
 * This is the class that will handle the starting and expiration of
 * various interval alarms.  The necessity of this class comes from
 * the fact that in cougaar we should do very little work in the
 * thread that runs the alarm expire() method, and we should also not
 * publish anything from the blackboard.  Because of this, the
 * interval alarms expire() methods will result in the alarm simply
 * being queued. It then singals the Cougaar system that we have work
 * to do, which result in the execute() method of the plugin being
 * called.  It is in the execute() method that we will do all the work
 * of handling the alarm expiration. 
 *
 * All IntervalAlarm's should be created with this as the immediate
 * expiration handler.
 * 
 * @author Tony Cassandra
 * @version $Revision: 1.4 $Date: 2004-10-20 16:48:21 $
 *
 */
public class IntervalAlarmHandler extends Object 
{

    // Class implmentation comments go here ...

    // For logging
    protected Logger _logger = Logging.getLogger(this.getClass().getName());

    //------------------------------------------------------------
    // public interface
    //------------------------------------------------------------

    /**
     * Needs the cougaar alarm service to construct this so alarms can
     * be added (this class serves as a relay for adding alarms).
     *
     * @param plugin This is plugin that is associated with this alarm
     * handler.
     */
    public IntervalAlarmHandler( BelievabilityPlugin plugin )
    {
        this._plugin = plugin;

    }  // constructor IntervalAlarmHandler

    //************************************************************
    /**
     * Add a new interval alarm to the Cougaar system.
     *
     * @param alarm The alarm to be added.
     */
    void addAlarm( IntervalAlarm alarm )
    {
        _plugin.addRealTimeAlarm( alarm );

    } // method addAlarm
 
    //************************************************************
    /**
     * This method should be called from the plugin's execute() method
     * so that any queued-up expired alarms can have their work
     * commence.  We cannot do the real work inside the alarm
     * expire(), so we just queue them up until this method is
     * called..
     *
     */
    void execute( )
    {
        // Just called the deferred handler on all alarms.
        //
        synchronized( _expired_alarm_queue )
        {
            Iterator iter = _expired_alarm_queue.iterator();
            while ( iter.hasNext() ) 
            {
                IntervalAlarm alarm = (IntervalAlarm) iter.next();

                if ( _logger.isDetailEnabled() )
                    _logger.detail( "Handling deferred expire for alarm: " 
                                    + alarm.toString()  );

                alarm.handleDeferredExpiration();

            } // while iter

            _expired_alarm_queue.clear();

        } // synchronized( _expired_alarm_queue )


    } // method execute

   //************************************************************
    /**
     * This will be called when the alarm expires, and if it had not
     * been cancelled previously.
     *
     * @param alarm the alarm object that has expired.
     */
    public void queueExpiredAlarm( Alarm alarm )
    {
        // All we do is queue the alarm and then signal client
        // activity to Cougaar.

        synchronized( _expired_alarm_queue )
        {
            if ( _logger.isDetailEnabled() )
                _logger.detail( "Queuing expired alarm: "
                                + alarm.toString()  );
            
            _expired_alarm_queue.add( alarm );
            
        } // synchronized( _expired_alarm_queue )
 
        // With something in the queue, we do not want to wait too
        // long before we handle it.  Calling this will result in the
        // plugin's execute() method being called.
        //
        _plugin.signalClientActivity();

    } // method queueExpiredAlarm
 
    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    private BelievabilityPlugin _plugin;

    private Vector _expired_alarm_queue = new Vector();

} // class IntervalAlarmHandler
