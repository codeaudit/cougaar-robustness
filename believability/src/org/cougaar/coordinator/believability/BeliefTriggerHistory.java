/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: BeliefTriggerHistory.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/BeliefTriggerHistory.java,v $
 * $Revision: 1.11 $
 * $Date: 2004-08-05 17:14:19 $
 *</RCS_KEYWORD>
 *
 *<COPYRIGHT>
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
 *</COPYRIGHT>
 *
 *</SOURCE_HEADER>
 */

package org.cougaar.coordinator.believability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.cougaar.coordinator.techspec.AssetID;

import org.cougaar.core.service.AlarmService;

import org.cougaar.core.agent.service.alarm.Alarm;

/**
 * This is a container class for holding all information about the
 * history of belief update triggers for a particular asset. It does
 * not keep a full history, but rather just the necessary information
 * to satisfy the requirements:
 *
 * 1) Ensure that sensor latency is accounted for before calculating
 *    the belief state resulting from a diagnosis.
 *
 * 2) Ensure that successful actions result in an immediate belief
 *    calculation. (see below for question about this)
 *
 * 3) Ensure that a belief calculation does not wait for the sensor
 *    latency period when a diagnosis has been received from all
 *    sensors.
 *
 * 4) When producing a belief state, if some "delta1" amount of time has
 *    passed since we last heard from a sensor, and the sensor should
 *    be viewed as having "implicit" diagnoses, then factor in the last
 *    diagnosis value from this sensor into the belief update
 *    calculation.  (Note that 'delta1' is currently defined to be the
 *    sensor latency length.)  
 *
 * 5) If no state estimates have been published for an asset after
 *    'delta2' amount of time, then force the calculation, accounting
 *    for threats and any implicit diagnoses. (The interval delta2 will
 *    eventually be policy and/or resource based.)
 *
 * 6) For items #1, #3 and #4, only publish a state estimation of the
 *    belief calculation if there is a "significant" utility change.
 *
 * Some terminology:
 *
 *   o Sensor Latency Window - The time window maintained to account
 *                  for the delay that sensors may have in reporting.
 *
 *   o Publish Delay Window - The time window maintained to account
 *             stop all immediate publications.  This helps with
 *             simultaneous and near-simultaneous events.
 *
 *   o Publish Window - The time window maintained to ensure that we do
 *          not go too long without producing a belief update for an
 *          asset. 
 *
 * At a given point in time, there could be three timers associated with
 * an instance of this class: one for each of these.
 *
 * @author Tony Cassandra
 * @version $Revision: 1.11 $Date: 2004-08-05 17:14:19 $
 * @see BeliefTriggerManager
 */
class BeliefTriggerHistory 
        extends Loggable implements AlarmExpirationHandler
{

    // Aside from history information, this object maintains the
    // context of the current processing of belief triggers.  In
    // particular, it will maintain the diagnosis triggers during the
    // time window of the maximum sensor latency.
    //

    //------------------------------------------------------------
    // package interface
    //------------------------------------------------------------

    /**
     * Main constructor
     *
     * @param asset_id The Asset ID for which this is the trigger
     * history for.
     * @param model_manager The source of all model-related data
     */
    BeliefTriggerHistory( AssetID asset_id, 
                          ModelManagerInterface model_manager,
                          BeliefConsumer consumer,
                          AlarmService alarm_service )
    {
        super();

        logDetail( "Creating trigger history for: " + asset_id );

        this._asset_id = asset_id;
        this._model_manager = model_manager;
        this._consumer = consumer;
        this._alarm_service = alarm_service;

    }  // constructor BeliefTriggerHistory

    //************************************************************
    /**
     * Main method for handling the arrival of a belief update
     * trigger.
     *
     * @param trigger The newly arriving trigger t be handled.
     */
    synchronized void handleBeliefTrigger( BeliefUpdateTrigger trigger )
            throws BelievabilityException
    {
        // Method implementation comments go here ...

        logDetail( "Handling trigger: " + trigger.toString() );

        // There are two events that can happen which impact the
        // details about how to deal with triggers: rehydration and
        // unleashing.  In both cases, these effect what we use for
        // the initial belief state (the first belief update after
        // these events.)  The model manager has the status of whether
        // or not rehydration or unleashing is happening, so we use
        // this opportunity to check those and arrange our internal
        // variables so that the right thing will happen when we do a
        // belief update. Note that because we always delay publishing
        // a little bit, the belief updating will happen after these
        // events are complete, even though what we do needs to
        // account for this.  COnversely, this handleBeliefTrigger()
        // *is* called while these events are occurring, so now is the
        // time to handle them in a way that will impact the
        // subsequent belief state.
        //
        handleSpecialEvents();

        // First step is to add the trigger (no matter what type of
        // trigger this is, we add it).
        //
        if ( ! add( trigger ))
        {
            logDebug( "Trigger discarded for "
                      + _asset_id + " (happened in the past?)" );
            return;
        }

        // Now we run through the possible conditions under which we
        // may want to compute a new belief state, predominantly based
        // upon the type of trigger we are seeing..

        // We only publish in response to a timer/alarm going off.
        // However, there are three types of possible alarms that
        // could trigger publishing: PublishDelayTimeTrigger,
        // PublishIntervalTimeTrigger and SensorLatencyTimeTrigger.
        //
        if ( trigger instanceof TimeUpdateTrigger )
        {
            logDetail( "Time-out for alarm type "
                       + trigger.getClass().getName()
                       + ". Updating and publishing for asset "
                       + _asset_id );

            updateBeliefState( );
            publishLatestBelief( );
            return;

        } // if an alarm trigger type occurs

        // On actions, we always update the belief and publish it,
        // though only after a short delay.
        //
        if ( trigger instanceof BelievabilityAction )
        {
            logDetail( "Action trigger. Starting publish delay timer for: "
                       + _asset_id );
            
            // Note that if there is an existing delay alarm, this
            // routine will do nothing.
            //
            startPublishDelayTimer();
            return;
        } // if action trigger
        

        // If we have seen a diagnosis from all sensors, then we
        // should not wait until the end of the max sensor latency
        // period. Note that if there is only one sensor, this wil
        // return true on the first diagnosis addition and thus cause
        // an immediate belief computation.  This uses a small delay
        // for publishing in case the diagnosis arrives at the same
        // time as an action.
        //
        if ( seenAllSensors() )
        {
            logDetail( "Seen all sensors. Starting publish delay timer for: "
                       + _asset_id );
          
            startPublishDelayTimer();
            return;
         
        } // if seen all sensors

        // If this is the first sensor we are seeing (and there are
        // more than one for this asset), then we need to start a
        // timer to wait for the maximum latency time before actually
        // generating a new belie state. Note that if there is only
        // one known sensor, that the seenAllSensors() case above will
        // be satisfied and we should not get to this condition.
        //
        if ( trigger instanceof BelievabilityDiagnosis )
        {
            if ( _current_triggers.size() == 1 )
            {
                logDetail( "First, diagnosis, deferring belief update for: "
                           + _asset_id );
                
                startSensorLatencyTimer();
                return;
            }
            else
            {
                logDetail( "Deferring belief update (diagnosis) for: "
                           + _asset_id );
                return;
            }
        } // if BelievabilityDiagnosis

        else
        {
            logDebug( "unknown trigger for: "
                      + _asset_id );
       }
       
    } // method handleBeliefTrigger

    //************************************************************
    /**
     * Add a trigger to this current history.
     *
     * @param trigger The new trigger to be added.
     * @return False if the trigger is not added.  This happens if the
     * timestamp of the trigger precedes the last computed belief
     * state timestamp.
     */
    boolean add( BeliefUpdateTrigger trigger )
    {
        // Method implementation comments go here ...

        // I am pretty sure having an inequality here is better.  I
        // originally had "<=", which caused some problems for
        // simultaneous actions/diagnosis.  Anyway, if you are
        // debugging a problem and happen to look here, this might be
        // useful information for you.
        //
        if (( _last_computed_belief != null )
            && ( trigger.getTriggerTimestamp() 
                 < _last_computed_belief.getTimestamp() ))
        {
            return false;
        }

        // This will simply append it, though we will sort it by
        // timestamp later.
        //
        _current_triggers.add( trigger );
        
        // BelievabilityDiagnosis objects require some special
        // handling as far as ensuring that our data structures stay
        // current.
        //
        if ( trigger instanceof BelievabilityDiagnosis )
        {
            BelievabilityDiagnosis diag =
                    (BelievabilityDiagnosis) trigger;

            // Must track the last explicit diagnosis for sensors in
            // case we find that we need to add implicit diagnoses for
            // them.
            //
            _last_explicit_diagnosis.put( diag.getSensorName(),
                                          trigger );

            // An explict diagnosis should clear out the last implict
            // diagosis if there was any.
            //
            _last_implicit_diagnosis_time.remove( diag.getSensorName() );

            // Also need to track all the sensor names, so that we can
            // handle the case of having received diagnoses from all
            // sensors before the sensor latency window alarm expires.
            //
            _current_sensors.add( diag.getSensorName() );

        } // If this is a diagnosis trigger

        return true;

    } // method add

    //************************************************************
    /**
     * This is the main crank to turn on this history.  It will factor
     * in everything it knows about the current triggers and produce a
     * new belief state.
     *
     */
    void updateBeliefState( ) throws BelievabilityException
    {
        // Iterate over the collection of triggers and compute a
        // series of belief states until we get to the end of the
        // list.  This final belief state is the one we want.
        //

        // This 'no trigger' case should not really hapen, since we
        // should call this only after adding at least one trigger.
        // However, better to explicitly check for this and deal with
        // it gracefully then have the system bomb when the
        // assumptions are violated.  Here we simply do not update the
        // belief state.
        //
        if ( _current_triggers.size() < 1 )
        {
            logInfo( "Ignoring belief update due to zero triggers." );
            return;
        }

        // We should first sort this trigger collection, as
        // everything else here will require it to be sorted.
        //
        Collections.sort( _current_triggers, new TriggerComparator() );
        
        logDetail( "Explicit trigger count is " + _current_triggers.size()
                   + " for " + _asset_id );

        // Before updating the belief state, we need to check whether
        // or not we will need to add any implicit diagnoses.  Some
        // sensors do not report infomration when it is the same for
        // efficiency reasons, so we need to make sure we treat this
        // case correctly.  This works by simply adding extra triggers
        // to the list at the end. 
        //
        addImplicitDiagnoses( getLatestCurrentTriggerTime() );

        logDetail( "Updating belief based on " + _current_triggers.size()
                   + " triggers for " + _asset_id );

        // Handle case where we might be computing the first belief
        // state.
        //
        if ( _last_computed_belief == null )
        {
            // Some special events might explcitly define what we need
            // to use for the initial belief state when we have no
            // record of a last computed belief.  If this happens, we
            // use that explicitly set value, otherwise, we get the
            // default techspec-defined a priori belief from the model
            // manager.
            //
            if ( _default_belief != null )
                _last_computed_belief = _default_belief;
            else
                _last_computed_belief
                        = _model_manager.getInitialBeliefState( _asset_id );

            _last_computed_belief.setTimestamp
                    ( getEarliestCurrentTriggerTime() );
        }

        BeliefState latest_belief = _last_computed_belief;

        logDetail( "Initial belief before trigger list: "
                   + latest_belief.toString() );

        // This iterator will return items in ascending order (by
        // trigger time).
        //
        Iterator trigger_iter = _current_triggers.iterator();
        while ( trigger_iter.hasNext() )
        {
            BeliefUpdateTrigger trigger 
                    = (BeliefUpdateTrigger) trigger_iter.next();

            logDetail( "Processing trigger: " + trigger.toString() );
            
            latest_belief = _model_manager.updateBeliefState( latest_belief,
                                                              trigger );
            
            logDetail( "Belief after trigger: " + latest_belief.toString() );
            
        } // while trigger_iter

        // Once we have incorporated all the triggers into the
        // computed belief state, we should clear them out, along with
        // anything else that is associated with this latency window
        // we have maintained.
        //
        clearTriggerHistory();
        
        _last_computed_belief = latest_belief;

        // Note that we choose not to worry about what to do with this
        // belief state here.  The decision whether to publish or not
        // should be made by whatever routine is actually calling this
        // update method.
        //

    } // method updateBeliefState

    //************************************************************
    /**
     * Part of handleBeliefTrigger() that deals with special events
     * that can happen that will impact the belief update (e.g.,
     * rehydration, unleashing)
     *
     */
    void handleSpecialEvents( )
            throws BelievabilityException
    {

        // If we are handling a trigger while rehydration is in
        // progress, then this measn that this is an asset that
        // existed before rehydration, and thus we want to make sure
        // we use the uniform distribution for the initial belief
        // state.
        //
        if ( _model_manager.isRehydrationHappening() )
        {

            // If we are rehydrating the plugin, then using the tech spec
            // initial belief will not make sense since we have no idea
            // what the state of the asset was prior to rehydrating.
            // Thus, the safest thing to use is the uniform distribution
            // for the initial state whch effectively assumes we have no a
            // priori information about the current state of the asset.
            //
            _default_belief 
                    = _model_manager.getUniformBeliefState( _asset_id );
        }

        // If we are processing a trigger during the course of
        // unleashing, then we essentially want to treat this like a
        // reset.  Thus, we will use the techspec-defined initial
        // belief state.  Note that it is possible to be unleashed
        // after rehydration, so we might well be overriding the
        // uniform distribution set earlier from a rehydration.  This
        // is the desired behavior.
        //
        else if ( _model_manager.isUnleashingHappening() )
        {
            _default_belief 
                    = _model_manager.getInitialBeliefState( _asset_id );

            // When unleashing, we always want to act as if the first
            // post-leash diagnosis is the first things we know about
            // an asset, thus we "forget" about any previously
            // computed belief state we might have.
            //
            _last_computed_belief = null;
        }

    } // method handleSpecialEvents

    //************************************************************
    /**
     * This should be called right after a new belief state is
     * computed to clear out the current history of triggers that we
     * might have been accumulating.
     *
     */
    void clearTriggerHistory( )
    {
        // Cancel any alarms that might be on-going.
        //
        if ( _latency_alarm != null )
        {
            _latency_alarm.cancel();
            _latency_alarm = null;
        } // if have a latency alarm

        if ( _delay_alarm != null )
        {
            _delay_alarm.cancel();
            _delay_alarm = null;
        } // if have a publish delay alarm

        _current_triggers.clear();
        _current_sensors.clear();

    } // method clearTriggerHistory

    //************************************************************
    /**
     * Looks in the current set of triggers for the trigger with the
     * latest (most recent) trigger time.  Assumes that the trigger
     * collection has been sorted and are currently stored in
     * ascending trigger timestamp sorted order. Latest time will be
     * the last one in the list.
     *
     */
    long getLatestCurrentTriggerTime( )
    {
        // Method implementation comments go here ...
        
        BeliefUpdateTrigger last_trigger;

        try
        {
            last_trigger = (BeliefUpdateTrigger) 
                    _current_triggers.get( _current_triggers.size() - 1 );
        }
        catch (IndexOutOfBoundsException ioobe)
        {
            logInfo( "Cannot get latest trigger time from empty list." );
            return System.currentTimeMillis();
        }

        return last_trigger.getTriggerTimestamp();

    } // method getLatestCurrentTriggerTime

    //************************************************************
    /**
     * Looks in the current set of triggers for the trigger with the
     * earliest (least recent) trigger time.  Assumes that the trigger
     * collection has been sorted and are currently stored in
     * ascending trigger timestamp sorted order. Earliest time will be
     * the first one in the list.
     *
     */
    long getEarliestCurrentTriggerTime( )
    {
        // Method implementation comments go here ...
        
        BeliefUpdateTrigger first_trigger;

        try
        {
            first_trigger = (BeliefUpdateTrigger) _current_triggers.get( 0 );
        }
        catch (IndexOutOfBoundsException ioobe)
        {
            logInfo( "Cannot get earliest trigger time from empty list." );
            return System.currentTimeMillis();
        }

        return first_trigger.getTriggerTimestamp();

    } // method getLatestCurrentTriggerTime

    //************************************************************
    /**
     * Some sensors do not report even when they have made a
     * diagnosis.  This happens for efficiency reasons when the sensor
     * has not detected any change.  Not reporting a diagnosis is
     * different from not having a diagnosis, so we make the
     * assumption that for certain sensors, "no report" should be
     * taken to be the same as not reporting (and not 'no
     * information').
     *
     * @param time The time to use for any added implicit diagnoses.
     */
    void addImplicitDiagnoses( long time )
             throws BelievabilityException
    {
        // Method implementation comments go here ...

        // loop over all sensor names we have ever seen;

        Enumeration sensor_name_enum = _last_explicit_diagnosis.keys();
        while ( sensor_name_enum.hasMoreElements() )
        {
            String sensor_name = (String) sensor_name_enum.nextElement();

            // First, if we have actually gotten an explicit value for
            // this sensor, then we do not need to consider it.
            //
            if ( _current_sensors.contains( sensor_name ))
                continue;

            // Next, we need to see whether or not this is a sensor
            // for which we need to add an implicit diagnosis at all.
            //
            if ( ! usesImplicitDiagnoses( sensor_name ))
                continue;

            // Last check is to see when the last explicit or implict
            // diagnosis was made for this sensor.
            //
            if ( ! needImplicitDiagnosis( sensor_name, time ))
                continue;

            // If we get here, then we know we need to add an implicit
            // diagnosis for this sensor.
            //
            ImplicitDiagnosisTrigger diag 
                    = createImplictDiagnosis( sensor_name, time );

            // One bad apple don't spoil the whole bunch girl.
            //
            if ( diag == null )
                continue;

            logDetail( "Adding implict diagnosis: "
                       + diag.toString() );

            add( diag );

            _last_implicit_diagnosis_time.put( sensor_name,
                                               new Long( time ));
            
        } // while sensor_name_enum

    } // method addImplicitDiagnoses

    //************************************************************
    /**
     * Determines if an implicit diagnosis is needed for the current
     * sensor based on the time sent in, in relation to the last explicit
     * diagnosis time as well as the last implicit diagnosis time.
     * This assumes that the sensor name is one for which the model
     * says we will need to add implicit diagnoses for.
     *
     * @param sensor_name The name of the sensor we are checking
     * @param time The time at which we are to consider whether we
     * need an implicit sensor reading.
     * @return True if it is time to add an implicit diagnosis for the
     * sensor.
     */
    boolean needImplicitDiagnosis( String sensor_name, long time )
            throws BelievabilityException
    {

        long last_time;

        BelievabilityDiagnosis last_explicit_diag
                = (BelievabilityDiagnosis) _last_explicit_diagnosis.get
                ( sensor_name );

        // This really should not happen, but we should do something
        // semi-reasonable if it does.
        //
        if ( last_explicit_diag == null )
        {
            logDebug( "Didn't find last explict diagnosis for: "
                      + sensor_name );
            return false;
        }

        Long last_implict
                = (Long) _last_implicit_diagnosis_time.get( sensor_name );

        if ( last_implict == null )
            last_time = last_explicit_diag.getTriggerTimestamp();
        else
            last_time = last_implict.longValue();

        // This really should not happen, but we should do something
        // semi-reasonable if it does.
        //
        if ( last_time > time )
        {
            logDebug( "Implicit diag. time less than last diag. time for: "
                      + sensor_name 
                      + " ( " + last_time + " > " + time + ")" );
            return false;
        }

        // The sensor latency is defined to be the period at which a
        // sensor generates diagnoses (as per David Wells on
        // 8/4/2004).
        //
        long sensor_latency
                = _model_manager.getSensorLatency( _asset_id.getType(),
                                                   sensor_name );

        // This is the main check that happens in this method.  The
        // minimum time between adding implicit diagnoses is a
        // policy/model decision so we always go to the model manager
        // for this information.
        //
        if ( (time - last_time) >= sensor_latency )
            return true;

        return false;

    } // method needImplicitDiagnosis
    //************************************************************
    /**
     * Checks to see if the given sensor name requires us to consider
     * using implicit diagnoses in our belief update calculations.
     *
     * @param sensor_name The name of the sensor to see if we need to
     * consider adding implicit diagnoses.
     */
    boolean usesImplicitDiagnoses( String sensor_name )
            throws BelievabilityException
    {
        // Calling into the model manager to determine this is a
        // little expensive, since it must look over all state
        // dimensions.  As a simple optimization, we cache the
        // information we get so we only need to make this call once
        // per sensor name.

        Boolean cache_result 
                = (Boolean) _uses_implict_diagnoses.get( sensor_name );

        // If we have cached result, then use it.
        if ( cache_result != null )
            return cache_result.booleanValue();

        // Ask model for the information, and cache it.
        
        boolean result = _model_manager.usesImplicitDiagnoses
                ( _asset_id.getType(), sensor_name );

        _uses_implict_diagnoses.put( sensor_name, 
                                     new Boolean( result ));

        return result;

    } // method usesImplicitDiagnoses
    
    //************************************************************
    /**
     * Creates an implicit diagnosis trigger object for the given
     * sensor by looking at the last actual (explict) diagnosis
     * received.

     */
    ImplicitDiagnosisTrigger createImplictDiagnosis( String sensor_name, 
                                                     long time )
            throws BelievabilityException
    {
        BelievabilityDiagnosis last_diag
                = (BelievabilityDiagnosis) 
                _last_explicit_diagnosis.get( sensor_name );

        if ( last_diag == null )
        {
            logDebug( "Cannot find last explicit diagnosis."
                      + "No implicit diagnosis for :" + _asset_id );
            return null;
        }

        // Try to make as close a copy as we can to the original
        // diagnosis, only with a different time.
        //
        return new ImplicitDiagnosisTrigger( _asset_id,
                                             last_diag.getSensorName(),
                                             last_diag.getSensorValue(),
                                             last_diag.getStateDimensionName(),
                                             time );
    } // method createImplictDiagnosis

    //************************************************************
    /**
     * Looks at the current latency window to determine if we have
     * received an explicit diagnosis from all known sensors.  This is
     * useful for knowing that we no longer need to wait until the end of
     * the latency window to update our belief.
     *
     */
    boolean seenAllSensors( )
            throws BelievabilityException
    {
        // Here we make the assumption that sensor names are unique,
        // so that the _current_sensors, which is a set of sensor
        // names, just needs to be the same size as the total number
        // of sensors for this asset type.  i.e., we *do not* iterate
        // through all known sensor names and check that they all
        // appear in our _curent_sesnors set.
        //
        return ( _current_sensors.size() 
                 >= _last_explicit_diagnosis.size() );

        // FIXME: This is the original version that has a problem
        // since not all sensors will defined on all agents of a given
        // type.  I am leaving this line here in case we need it later.
        //
        // return ( _current_sensors.size() 
        //         == _model_manager.getNumberOfSensors
        //         ( _asset_id.getType() ));
        
    } // method seenAllSensors

    //************************************************************
    /**
     * This method is called when publication of the latest belief
     * state should only be done if there has been a meaningful change
     * (based on utility) since the last time we published the belief
     * state.
     *
     * @return True if the last computed belief state is significantly
     * different enough from the last published belief state.
     */
    boolean utilityHasChangedEnough( )
            throws BelievabilityException
    {

        // Easiest case is when we have never published before.  Here
        // we always want to publish the first things we see.
        //
        if ( _last_published_belief == null )
            return true;

        // This really should not happen, but we should check anyway.
        //
        if ( _last_computed_belief == null )
        {
            logInfo( "Did not find last computed belief in utility calc.");
            return false;
        }

        double previous_utility = 0.0;
        double current_utility = 0.0;

        Enumeration prev_dim_enum 
                = _last_published_belief.getAllBeliefStateDimensions
                ().elements();

        // The utilitiies are just the sum of the individual weighted
        // utlities of each possible state value for each possible
        // state dimension.
        //
        // We make some strong assumptions in this loop as far as the
        // number and names of the state dimensions/values.  If they
        // do not all agree (which they should), this loop will likely
        // bomb miserably, or compute the wrong values.
        //
        while ( prev_dim_enum.hasMoreElements() )
        {
            BeliefStateDimension prev_bsd =
                    (BeliefStateDimension) prev_dim_enum.nextElement();
            
            BeliefStateDimension cur_bsd =
                    _last_computed_belief.getBeliefStateDimension
                    ( prev_bsd.getName() );

            // Get the utility weights 
            double[] mau_weighted_utilities = 
                    _model_manager.getWeightedAssetUtilities
                    ( _asset_id.getType(), 
                      prev_bsd.getAssetStateDimension() );
            
            double[] prev_probs = prev_bsd.getProbabilityArray();
            for ( int i = 0; i < prev_probs.length; i++ ) 
                previous_utility += ( prev_probs[i] 
                                      * mau_weighted_utilities[i] );

            double[] cur_probs = cur_bsd.getProbabilityArray();
            for ( int i = 0; i < cur_probs.length; i++ ) 
                current_utility += ( cur_probs[i] 
                                     * mau_weighted_utilities[i] );
        } // while prev_dim_enum

        // The threshold for whether the utilities has changed enough
        // is a policy/model decision, so we defer this to the modfel
        // manager.
        //
        if ( Math.abs( current_utility - previous_utility )
             > _model_manager.getBeliefUtilityChangeThreshold() )
            return true;

        return false;

    } // method utilityHasChangedEnough

    //************************************************************
    /**
     * Responsible for setting an alarm to be triggered after a brief
     * period after certain triggers are processed.  This is used to
     * ensure that simultaneous and near simultaneous trigger events
     * are grouped into the same belief update.  Note that if there is
     * already an active delay alarm, this routine does nothing.
     */
    void startPublishDelayTimer( )
            throws BelievabilityException
    {
        // If there is an outstanding delay alarm, then we do
        // nothing. 
        //
        if (( _delay_alarm != null )
            && ( ! _delay_alarm.hasExpired() ))
            return;

        // Note that we *always* want to wait the publish delay time,
        // even if an existing latency timer will expire sooner.  The
        // idea is that the publish delay should be very short (less
        // than a second) and that its purpose is to make sure
        // simultaneous and near-simultaneous events get grouped
        // together into the same belief update. The same goes for
        // any existing publication alarm.
        //
        if ( _latency_alarm != null )
        {
            _latency_alarm.cancel();
            _latency_alarm = null;
        }

        if ( _publish_alarm != null )
        {
            _publish_alarm.cancel();
            _publish_alarm = null;
        }

        _delay_alarm = new IntervalAlarm
                ( _model_manager.getPublishDelayInterval(), this );
        
        _alarm_service.addRealTimeAlarm( _delay_alarm );

        logDetail( "Publish delay timer started for " + _asset_id
                   + ". Alarm:" + _delay_alarm.toString() );

    } // method startPublishDelayTimer

    //************************************************************
    /**
     * Responsible for setting an alarm to be triggered when the
     * maximum sensor latency time has been reached, starting from the
     * current time.
     */
    void startSensorLatencyTimer( )
            throws BelievabilityException
    {
        // If there is an existing, unexpired publish delay timer,
        // then that should take precedence over the sensor latency.
        // The idea here is that the delay serves and important
        // function (group simultaneous and near-simultaneous trigger
        // events) and that it should be a relative short interval
        // (less than a second).  Also, the delay alarm existing means
        // we want to publish a belief state sonner rather than later.
        // Thus, we do not create a latency alarm if there is an
        // active delay alarm.
        //
        if (( _delay_alarm != null )
            && ( ! _delay_alarm.hasExpired() ))
        {
            logDetail( "Latency timer not started for " + _asset_id
                       + ". Existing publish delay alarm exists." );
            return;
        }

        // If there is an existing publication interval timer, then
        // we cancel that, as it is more important to wait for the
        // sensors to report than the somewhat arbitrary
        // inter-publication interval.
        //
        if ( _publish_alarm != null )
        {
            _publish_alarm.cancel();
            _publish_alarm = null;
        }
        
        // Just for good luck, make sure any existing alarm gets
        // cancelled. 
        //
        if ( _latency_alarm != null )
            _latency_alarm.cancel();

        _latency_alarm = new IntervalAlarm
                ( _model_manager.getMaxSensorLatency
                  ( _asset_id.getType() ), 
                  this );
        
        _alarm_service.addRealTimeAlarm( _latency_alarm );

        logDetail( "Latency timer started for " + _asset_id
                   + ". Alarm:" + _latency_alarm.toString() );

    } // method startSensorLatencyTimer

    //************************************************************
    /**
     * Responsible for setting an alarm to be triggered when the
     * maximum inter-belief publish interval has been reached,
     * starting from the current time.
     */
    void startPublishIntervalTimer( )
            throws BelievabilityException
    {
        // Note that:
        //
        //    o we should only be starting this alarm immediately after
        //      publishing, and 
        //
        //    o we only publish is response to some timer going off.
        //
        //    o we should only have one active alarm at a time.
        //
        // Therefore, we do not check for the status of the other
        // alarms here.

        // Just for good luck, make sure any existing alarm gets
        // cancelled. 
        //
        if ( _publish_alarm != null )
            _publish_alarm.cancel();
        
        _publish_alarm = new IntervalAlarm
                ( _model_manager.getMaxPublishInterval
                  ( _asset_id.getType() ),
                  this );
        
        _alarm_service.addRealTimeAlarm( _publish_alarm );

        logDetail( "Publish timer started for " + _asset_id
                   + ". Alarm:" + _publish_alarm.toString() );

    } // method startPublishIntervalTimer

    //************************************************************
    /**
     * This handles dispatching the belief state (publishes to the
     * consumer, not necessarily ther cougaar blackboard).  The
     * _consume object has the detail of what should be done.  Note
     * that it is very important that the _consume object not publish
     * somethin gto the blackboard directly, since this publish method
     * will often be * called within an alarm expiration.
     *
     */
    void publishLatestBelief( )
            throws BelievabilityException
    {

        logDetail( "Publish belief for" + _asset_id );

        _consumer.consumeBeliefState( _last_computed_belief );

        _last_published_belief = _last_computed_belief;

        _last_publish_time = System.currentTimeMillis();

        // Cancel an outstanding timer (if any) for notifying us when
        // we have not published for a while.
        //
        if ( _publish_alarm != null )
            _publish_alarm.cancel();

        // Set a timer to make sure we publish a belief state afer a
        // given amount of time has elpased.
        //
        startPublishIntervalTimer();

    } // method publishLatestBelief

    //------------------------------------------------------------
    // AlarmExpirationHandler interface
    //------------------------------------------------------------

    //************************************************************
    /**
     * This will be called when the alarm expires, and if it had not
     * been cancelled previously.
     *
     * @param alarm the alarm object that has expired.
     */
    public void handleAlarmExpired( Alarm alarm )
            throws BelievabilityException
    {
        // FIXME: Because multiple alarms can expire at the same time,
        // you can run into synchronization problems.  Any instance of
        // this object should only have one active alarm, so this
        // really shouldn't be a problem. Alas, we have seen some
        // problems.  Thus, a better way to do this would be to have
        // the alarm expire() (or this method) simply queue itself up,
        // and then call the plugin's signalClientActivity() method to
        // force its execute() method to be invoked. executes() do not
        // happen in parallel.
        //

        if ( alarm == _delay_alarm )
        {
            logDetail( "Handling delay alarm expiration for: "
                       + _asset_id );

            handleBeliefTrigger
                    (  new PublishDelayTimeTrigger
                       (  _asset_id, alarm.getExpirationTime() ));

        } // if publish alarm

        else if ( alarm == _latency_alarm )
        {
            logDetail( "Handling latency alarm expiration for: "
                       + _asset_id );

            handleBeliefTrigger
                    (  new SensorLatencyTimeTrigger
                       (  _asset_id, alarm.getExpirationTime() ));

        } // if latency alarm
        
        else if ( alarm == _publish_alarm )
        {
            logDetail( "Handling publish alarm expiration for: "
                       + _asset_id );

            handleBeliefTrigger
                    (  new PublishIntervalTimeTrigger
                       (  _asset_id, alarm.getExpirationTime() ));

        } // if publish alarm

        else
        {
            logDebug( "Unknown alarm expiration: " 
                      + alarm.getClass().getName() + " for "
                      + _asset_id );
        }

    } // method handleAlarmExpired

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    // This identifies which asset this trigger history is for.
    //
    private AssetID _asset_id;

    // This is the source for al model-related infomration that we
    // need.
    //
    private ModelManagerInterface _model_manager;

    // We need sometrhing that can actually do the publishing when
    // this class determines that a belief sttae is ready for
    // publishing.  We publish the belief to this consumer and let it
    // do its thing.
    //
    private BeliefConsumer _consumer;

    // This is needed to properly set and mamage alarms in the cougaar
    // system. 
    //
    private AlarmService _alarm_service;

    // For the current latency window, we need to track which sensors
    // we have heard from during the window. The reason for this is
    // that if we happen to hear from them all before the end of the
    // window, then we do not want to wait to update the belief
    // state.  i.e., the only reason we wait for the latecy period is
    // in case a sensor diagnosis is delayed.  If we heard from them
    // all then we have no reason to wait any further.
    //
    private HashSet _current_sensors = new HashSet();

    // Some sensors will not report a diagnosis unless its value has
    // changed. This is different from the case of "no information",
    // since in this case, the sensor really is monitoring the asset
    // and has the information, it is just an optimization that it
    // chooses not to relay what it knows. Thus, when we decide a new
    // belief state calculation is in order, we will need to factor
    // this in. To do this requires remebering the last diagnosis
    // value we received for each sensor.  This Hashtable has this
    // information: key is the sensor name, value is a
    // BelievabilityDiagnosis object for the last reported value.
    //
    private Hashtable _last_explicit_diagnosis = new Hashtable();

    // When we are looking at adding the implicit sensor diagnoses,
    // aside from tracking the last explicit sensor value, we also
    // need to track the last time we added an implicit diagnosis for
    // each sensor, since it is the interval between this time and the
    // current time that really matters as far as whether we should
    // add another explicit sensor.  Key here is the sensor name,
    // values are Long objects.
    //
    private Hashtable _last_implicit_diagnosis_time = new Hashtable();

    // This is a cahce of the results that the model mamager tells us
    // about which sensors we need to consider implicit diagnoses for.
    // Key is the sensor name (String), values are Boolean objects.
    //
    private Hashtable _uses_implict_diagnoses = new Hashtable();

    // During the latency window, we need to keep track of all the
    // BeliefUpdateTrigger elements that occur.  As things are added,
    // these are order by the times in which we receive them, but will
    // will ensure they are sorted by timestamp order before
    // processing them when updating the belief state.
    //
    private ArrayList _current_triggers = new ArrayList( );

    // This is used as the intial belief state on the first belief
    // update when the _last_computed_belief is null.  This will
    // either be the techspec derived initial a priori belief state,
    // or a belief state that has a uniform distribution for each
    // state dimension.  The former is the normal case, while the
    // latter is used after a rehydration.
    //
    private BeliefState _default_belief = null;

    // Always need to track the last computed belief state, since this
    // is the starting point for the next computed belief state.  An
    // important part of this object is the timestamp of when the
    // BeliefState was produced.
    //
    private BeliefState _last_computed_belief;

    // The last computed belief state and the last published belief
    // state will not always be the same (though they could be).  They
    // differ when the utility calculation determines that the belief
    // sttae change has not been significant enough to warrant a
    // publication.  We need to keep track of the last published
    // belief, because it is this one that we need to always compare
    // against to see if the current belief has changed wnough.
    //
    private BeliefState _last_published_belief;

    // Also keep track of the time that a belief was last published.
    //
    private long _last_publish_time;

    // A handle to the current alarm (if any) associated with the
    // sensor latency.
    //
    private IntervalAlarm _latency_alarm;

    // A handle to the current alarm associated with ensuring that we
    // publish a belief state after a period of time even if we have
    // not received any diagnoses.
    //
    private IntervalAlarm _publish_alarm;

    // A handle to the current alarm associated with ensuring that we
    // publish a belief state after a short period of time after
    // receiving the trigger that is suposed to start a belief state
    // publication.
    //
    private IntervalAlarm _delay_alarm;

    //------------------------------------------------------------
    // Inner Classes
    //------------------------------------------------------------

    //************************************************************
    /**
     * Inner class used to compare BeliefUpdateTrigger objects
     */
    class TriggerComparator implements Comparator
    {
        public int compare(Object o1, Object o2) 
        {
            if ( ! ( o1 instanceof BeliefUpdateTrigger )
                 || ! (o2 instanceof BeliefUpdateTrigger ))
                throw new ClassCastException
                        ( "Cannot compare objects. Not BeliefUpdateTriggers.");

            BeliefUpdateTrigger t1 = (BeliefUpdateTrigger) o1;
            BeliefUpdateTrigger t2 = (BeliefUpdateTrigger) o2;
            
            if ( t1.getTriggerTimestamp() < t2.getTriggerTimestamp() )
                return -1;

            if ( t1.getTriggerTimestamp() > t2.getTriggerTimestamp() )
                return 1;

            // If the timestamps are the same, then we define an action to
            // preceed a diagnosis.
            //
            if (( o1 instanceof BelievabilityAction )
                && ( o2 instanceof DiagnosisTrigger ))
                return -1;
            
            // Equality case is zero return value.
            //
            return 0;
            
        } // method compare

    } // inner class TriggerComparator

} // class BeliefTriggerHistory
