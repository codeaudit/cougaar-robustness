/**
 * BeliefUpdate.java
 *
 * Created on April 21, 2004
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

import org.cougaar.coordinator.techspec.AssetStateDimension;
import org.cougaar.coordinator.techspec.StateValue;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;


/**
 * The class representing the belief state of an asset.
 * There is one BeliefUpdate object for each asset (or each 
 * AssetTechSpecInterface).
 *
 * @author Misty Nodine and Tony Cassandra
 */
public class BeliefUpdate extends Object
{
    // Convenience to allow finer grain control over debug logging.
    //
    public static final int LOCAL_DEBUG_LEVEL = 1;

    // FIXME: There are circumstances where the diagnosis time and the
    // initial time we set for the start of the interval result in
    // negative times.  This needs to be resolved in a better way, but
    // for now, we will check this condition and reset the times to
    // use this interval instead.  This is in milliseconds.
    //
    //    public static long DEFAULT_LAST_BELIEF_INTERVAL = 60000;
    //   public static long DEFAULT_DIAGNOSIS_DELAY_INTERVAL = 5000;

    //------------------------------------------------------------
    // public interface
    //------------------------------------------------------------

    //*************************************************************
    /*
     * Each BeliefUpdate has a pointer to the POMDP model that it
     * works with. It maintains the beliefs about the state of a
     * particular asset.
     *
     * @param asset_model The AssetModel for the asset
     * @param pomdp_model The POMDPModelInterface for the believability model
     *
     */
    public BeliefUpdate( AssetModel asset_model,
                         POMDPModelInterface pomdp_model ) {

	this._asset_model = asset_model;
	this._pomdp_model = pomdp_model;

	// Need a logger to help with troubleshooting an debugging.
	this._logger = Logging.getLogger( this.getClass().getName() );

	// Important to keep last diagnosis times when doing the belief
	// update (see transformBeliefState() method for more
	// explanation.
	//	initializeLastTimes();
    } // constructor BeliefUpdate

    //
    //    /**
    //     * Sets up the last diagnosis times so that belief update
    //     * compuation always has some valid time to refer to when
    //     * determining the amoutn of time that a threat has been acting on an
    //     * asset. 
    //     *
    //     */
    //    private void initializeLastTimes()
    //    {
    //        // Need to initialize the last diagnosis times.  Here we choose
    //        // to simply initialize them to be the current time at whichj
    //        // this constructor is called.
    //        Long cur_time = new Long( System.currentTimeMillis());
    //
    //        Enumeration sd_enum = 
    //                this._asset_model.getAssetStateDimensions().elements();
    //
    //        while ( sd_enum.hasMoreElements() ) {
    //            AssetStateDimension asd = 
    //                    (AssetStateDimension) sd_enum.nextElement();
    //            String asd_name = asd.getStateName();
    //
    //            this._last_diag_times.put( asd_name, cur_time );
    //            this._last_belief_times.put( asd_name, cur_time );
    //            
    //        } // while sd_enum
    //
    //    } // method initializeLastDiagnosisTimes
    //
    //
    //    /**
    //     * Sets the a priori belief distribution for the given asset sate
    //     * dimension
    //     *
    //     * @param asset_name The name of the asset
    //     * @param asd The asset state dimension for this belief state
    //     * @return A StateDimensionEstimation that is the a priori probabilities
    //     *         for the asset state dimension being in the various states.
    //     */
    //    public StateDimensionEstimation getAprioriBelief( String asset_name,
    //						      AssetStateDimension asd )
    //            throws BelievabilityException
    //    {
    //        String asd_name = asd.getStateName();
    //         
    //        // Get a new StateDimensionEstimation
    //        Vector aspv = asd.getPossibleValues();
    //        StateDimensionEstimation apriori_state_estimation = 
    //                new StateDimensionEstimation( asset_name,
    //					      asd_name, 
    //					      aspv.size() );
    //        
    //        // iterate through state descriptor values
    //        Enumeration aspv_enum = aspv.elements();
    //        while ( aspv_enum.hasMoreElements() ) 
    //        {
    //	    StateValue sv = (StateValue) aspv_enum.nextElement();
    //            String state_name = sv.getName();
    //
    //            // Set the probability in the StateDimensionEstimation
    //            // to the apriori probability
    //            // May throw BelievabilityException
    //            apriori_belief_state.set
    //                    ( state_name,
    //                      _pomdp_model.getAprioriProbability( asset_name,
    //                                                          asd_name,
    //                                                          state_name ) );
    //        } // while aspv_enum
    //
    //        return apriori_belief_state;
    //       
    //    } // method getAprioriBelief
    //
    //
    //    /**
    //     * When doing the belief update, we need to have some starting
    //     * belief to be updated.  This routine fetches that belief sate
    //     * for a given asset state descriptor.
    //     *
    //     * @param asset_name The name of the asset
    //     * @param asd The asset state dimension
    //     * @return A belief state over the given asset state descriptor
    //     */
    //    public AssetBeliefState getInitialBelief( String asset_name,
    //                                              AssetStateDimension asd )
    //            throws BelievabilityException
    //    {
    //        // For now, we assume we always start from scratch with the a
    //        // priori probabilities. In some future version, it might be
    //        // desirable to track the belief states over time and return
    //        // the last belief state computed.
    //
    //        return getAprioriBelief( asset_name, asd );
    //
    //    } // getInitialBelief
    //
    //
    //    /**
    //     * Update the belief state for the asset, given this new set
    //     * of diagnoses.
    //     *
    //     * @param asd The asset state dimension that the defense is monitoring
    //     * @param diagnosis The diagnosis being processed
    //     * @return A StateDimensionEstimation object with the new estimates
    //     *            of the state of the asset state dimension
    //     * @exception BelievabilityException if there is a problem finding needed
    //     *            informaton in the state dimension
    //     **/
    //    public AssetBeliefState update( AssetStateDimension asd,
    //                                    BelievabilityDiagnosis diagnosis ) 
    //            throws BelievabilityException 
    //    {
    //        
    //	System.out.println ("***BeliefUpdate.update: need to implement still");
    //  // Get the last diagnosis time.
    //	//        Long last_diag_t = (Long) _last_diag_times.get(asd.getStateName());
	//        Long last_belief_t = (Long) _last_belief_times.get(asd.getStateName());

	//        if ( last_diag_t == null )
	//            throw new BelievabilityException
	//                    ( "Did not find last diagnosis time." );
	//        if ( last_belief_t == null )
	//            throw new BelievabilityException
	//                    ( "Did not find last belief time." );
	//
	//        long last_diag_time = last_diag_t.longValue();
	//        long last_belief_time = last_belief_t.longValue();
	//        long diag_time = composite_diagnosis.getTimestamp();
	//        long cur_time = System.currentTimeMillis();
	//
	//        StateDimensionEstimation prev_belief
	//                = getInitialBelief( _asset_model.getAssetName(), asd );
	//
	//        if (this._logger.isDebugEnabled()) 
	//            this._logger.debug("    Previous belief: "
	//                               + prev_belief.toString()
	//                               + "\n" );
	//
	//        // Here we do all the work.
	//        //
	//        StateDimensionEstimation new_belief 
	//                =  transformBeliefState( prev_belief,
	//                                         composite_diagnosis,
	//                                         last_diag_time,
	//                                         last_belief_time,
	//                                         diag_time,
	//                                         cur_time );
	//
	//        new_belief.setCompositeDiagnosis( composite_diagnosis );
	//        
	//        if (this._logger.isDebugEnabled()) 
	//            this._logger.debug("    Next belief: "
	//                               + new_belief.toString()
	//                               + "\n" );
	//
	//        // Before returning, update the last diagnosis time for the
	//        // next belief computation.
	//        //
	//        this._last_diag_times.put( asd.getStateName(),
	//                                   new Long( diag_time ));
	//        this._last_belief_times.put( asd.getStateName(),
	//                                     new Long( cur_time ));
	//
    //        return ???

    // } // update
    //************************************************************


    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    //************************************************************
    /**
     *  This is the method that will do the belief update computation
     * using the model parameters and the general POMDP update equations
     * (essentially a Bayesian update rule).
     *
     * @param prev_belief Previous belief state: the starting point
     * for updating the belief state 
     * @param defense_name The name of the defsne that made the
     * diagnosis 
     * @param monitoring_level The name of the monitoring level
     * (translates into the POMDP model action)
     * @param diagnosis_name The diagnosis made by the defense
     * (translates into the POMDP observation)
     * @param last_diag_time The diagnosis time for the previous
     * belief state update computation
     * @param diag_time The time at which the current diagnosis was
     * made 
     * @param cur_time The current system time
     * @throws BelievabilityException If anything goes wrong in the
     * computation. 
     * @return An AssetBeliefState object that represents a
     * probability distribution over the state values for the given
     * asset state descriptor.
     */
	//    public AssetBeliefState transformBeliefState
	//            ( AssetBeliefState prev_belief,
	//              CompositeDiagnosis composite_diagnosis,
	//              long last_diag_time,
	//              long last_belief_time,
	//              long diag_time,
	//              long cur_time ) throws BelievabilityException
	//    {
	//
	//        // There are the following time points that are potentially of
	//        // importance here: 
	//        // 
	//        //   - t0 : the previous last diagnosis received
        //   - t1 : the last time we computed the belief state
        //   - t2 : the time at which the current diagnosis was made
        //   - t3 : the current time (the time at which we will
        //          compute the current belief state)
        //
        // As per a telcon between OBJS and Telcordia (10/8/2003), the
        // desire is to compute the asset belief state as of the
        // current time (t3) rather than the belief state for the time
        // at which the diagnosis was made (t2).  Because of this, we
        // would need to account for the threats acting on the asset
        // both before (t2 - t1) and after (t3 - t2) the diagnosis was
        // made. (Note that the threats acting during (t1 - t0) will
        // have been accounted for during the belief upadte at time
        // t1. 
        //
        // If we were to be carrying over the belief states from one
        // update computation to another, then all of these times need
        // to be considered in order to properly apply the update
        // procedure.
        //
        // However, since for now we will always be resetting the
        // belief state back to the a priori belief sttae for each
        // belief update calculation, not all these times matter.
        // However, we do need to consider some time period, since the
        // threat model state transition probabilkities are
        // time-dependent. Although somewhat arbitrary about where we
        // assume this a priori belief state comes into existence, two
        // logical possibilities would be either at the time time of
        // our previous diagnosis (t0) or at the time of our last
        // belief update (t1).   
        //
        // Note that time t2 does not make sense, because this assume
        // that the defense made its diagnosis *before* any threat
        // acted on the asset.  The whole purpose of having the
        // threats and diagnoses is to assume we need to diagnosis
        // after some threat may have had a chance to act on the 
        //
        // Now the choice is whether t0 or t1 makes the most sense.
        // Although the threats during the interval from t0 to t1 is
        // accounted for in the previous belief update at t1, that is
        // just an estimate based on the model parameters.  Since we
        // discard the belief when we start with the a priori belief
        // all the time, we will lose this information.  Therefore, it
        // seems to make the most sense to start the clock on threats
        // affecting assets at the point where the last diagnosis was
        // made (t0).  
        //
        // On the other hand, if the assumption is that at the time of
        // the last belief calculation, we did something to correct
        // the problem, thus returning the world to the good a priori
        // states, then the time t1 makes the most sense.  Also, if we
        // did choose to not start over with the a priori belief state
        // and carry over our belief computations, then the time t1 is
        // the proper time to use, since this the previous belief will
        // have already factored in the t1-t0 time interval.  We
        // choose to use time t1, but we keep track of both t0 and t12
        // t1 in case we change our minds..
        //
        // Therefore, the belief update consists of three phases:
        //
        //   1. Threat acting on asset during the interval t2 - t1
        //   2. Diagnosis (instantaneous) from defense at point t2
        //   3. Threat acting on the asset during the interval (t3-t2).
        //
        // Because the timed diagnosis does not contain historical
        // information, we will need to cache the last diagnosis times
        // (t0) for each asset state descriptor and also the last time
        // at which we computed the belief state.
        //
        // Relating these time to this method parameters:
        //
        //     t0 = last_diag_time
        //     t1 = last_belief_time 
        //     t2 = diag_time  
        //     t3 = cur_time

	//        String asset_name = prev_belief.getAssetName();
	//        String asd_name = prev_belief.getAssetStateDescName();
	//        int num_states = prev_belief.getNumStates();
	//
	//        // Initialize local array froim belief state
	//        double[] init_belief = prev_belief.toArray( _pomdp_model );
	//
	//        if (this._logger.isDebugEnabled()) 
	//        {
	//            this._logger.debug( "    Initial belief : " 
	//                                + array2string( init_belief ) );
	//
	//            this._logger.debug( "    Times:"
	//                                + "\n      LastDiag: " + last_diag_time
	//                                + "\n      LastBelief: " + last_belief_time
	//                                + "\n      DiagTime: " + diag_time
	//                                + "\n      CurTime: " + cur_time );
	//        }
	//
	//        // FIXME: Hack around bogus time intervals.
	//        if ( last_belief_time >= diag_time )
	//            last_belief_time = diag_time - DEFAULT_LAST_BELIEF_INTERVAL;
	//
	//        // This first belief update covers the belief update
	//        // considering the threat acting during the interval between
	//        // the last belief was computed and the current diagnosis
	//        // time.
	//	//        //
	//        double[] b_t1_t2 = transformUsingTransitions( init_belief,
	//                                                      asset_name,
	//                                                      asd_name,
	//                                                      last_belief_time,
	//                                                      diag_time );
	//
	//        if (this._logger.isDebugEnabled()) 
	//            this._logger.debug( "    After threat-1 belief : " 
	//                                + array2string( b_t1_t2 ) );
	//        
	//        double[] b_t2 = transformUsingObservations( b_t1_t2,
	//                                                    asset_name,
	//                                                    asd_name,
	//                                                    composite_diagnosis );
	//
	//        if (this._logger.isDebugEnabled()) 
	//            this._logger.debug( "    After diagnosis belief : " 
	//                                + array2string( b_t2 ) );
	//
	//
	//        // FIXME: Hack around bogus time intervals.
	//        if ( diag_time >= cur_time )
	//            diag_time = cur_time - DEFAULT_DIAGNOSIS_DELAY_INTERVAL;
	//
	//        double[] b_t2_t3 = transformUsingTransitions( b_t2,
	//                                            asset_name,
	//                                             asd_name,
	//                                                      diag_time,
	//                                                      cur_time );
	//
	//        if (this._logger.isDebugEnabled()) 
	//            this._logger.debug( "    Final belief : " 
	//                                + array2string( b_t2_t3 ) );
	//
	//        AssetBeliefState belief = new AssetBeliefState( asset_name,
	//                                                        asd_name,
	//                                                        num_states );
	//
	//        // Initialize belief values from local array
	//        belief.set( b_t2_t3, _pomdp_model );
	//     
	//        return belief;
	//     
	//    }  // method transformBeliefState

    /**
     * This method will take a belief state and return an updated
     * belief state that only factors in the state transition
     * probabilites for the given time interval
     *
     * @param 
     * @throws 
     * @return 
     * @see 
     */
	//    private double[] transformUsingTransitions
	//            ( double[] prev_belief,
	//              String asset_name,
	//              String asd_name,
	//              long start_time,
	//              long end_time)
	//            throws BelievabilityException
	//    {
	//        
	//        double[] next_belief = new double[prev_belief.length];
	//        
	//        for ( int cur_state = 0; cur_state < prev_belief.length; cur_state++ ) 
	//        {
	//            
	//            for ( int next_state = 0; 
	//                  next_state < prev_belief.length; 
	//                  next_state++ ) 
	//            {
	//                double trans_prob = _pomdp_model.getTransProbability
	//                        ( asset_name,
	//                          asd_name,
	//                          _pomdp_model.indexToStateName( asset_name,
	//                                                         asd_name,
	//                                                         cur_state ),
	//                          _pomdp_model.indexToStateName( asset_name,
	//                                                         asd_name,
	//                                                         next_state ),
	//                          start_time,
	//                          end_time );
	//               next_belief[next_state] 
	//                       += prev_belief[cur_state]  * trans_prob;
	//
	//           } // for next_state
	//         }  // for cur_state
	//
	//        // Add a sanity check to prevent bogus belief from being
	//        // propogated to other computations. 
	//        //
	//        double sum = 0.0;
	//        for ( int i = 0; i < next_belief.length; i++ )
	//            sum += next_belief[i];
	//
	//        if( ! Precision.isZero( 1.0 - sum ))
	//            throw new BelievabilityException
	//                    ( "Resulting belief doesn't sum to 1.0 : "
	//                      + array2string(next_belief) );
	//                      
	//        return next_belief;
	//
	//    } // method transformWithTransitions
	//
	//    //************************************************************
	//    /**
	//     *  
	//     */
	//    private double[] transformUsingObservations
	//            ( double[] prev_belief,
	//              String asset_name,
	//              String asd_name,
	//              CompositeDiagnosis composite_diagnosis)
	//            throws BelievabilityException
	//    {
	//        double denom = 0.0;
	//
	//        double[] next_belief = new double[prev_belief.length];
	//
	//        for ( int state = 0; state < prev_belief.length; state++ ) 
	//        {
	//
	//            double obs_prob = _pomdp_model.getObsProbability
	//                    (  asset_name,
	//                       asd_name,
//                       _pomdp_model.indexToStateName( asset_name,
	//                                                      asd_name,
	//                                                      state ), 
	//                       composite_diagnosis );
	//            
	//            
	//            next_belief[state] = prev_belief[state] * obs_prob;
	//            
	//            denom += next_belief[state];
	//            
	//        } // for state
	//        
	//        if( Precision.isZero( denom ))
	//            throw new BelievabilityException
	//                    ( "Composite diagnosis is not possible. i.e., Pr("
	//                      + composite_diagnosis.toString() + ") = 0.0.");
	//        
	//        for( int i = 0; i < next_belief.length; i++ )
	//            next_belief[i] /= denom;
	//
	//        return next_belief;
	//
	//    } // method transformUsingObservations
	//
	//
	//    //************************************************************
	//    // Print an array
	//    private String array2string( double[] probarray ) {
	//     int num_states = probarray.length;
	//
	//     String retstring = "[ ";
	//     for ( int i = 0; i < num_states; i++ ) {
	//         retstring = retstring + probarray[i] + " ";
	//     }
	//     retstring += "]";
	//     return retstring;
	//    }

    // Asset model for this asset
    //
    private AssetModel _asset_model;

    // POMDP Believability Model that this asset is associated with
    //
    private POMDPModelInterface _pomdp_model;

    // See the explanation of timing in the transformBeliefState()
    // method for why we need these data structure.
    //
    //    private Hashtable _last_diag_times = new Hashtable();
    //    private Hashtable _last_belief_times = new Hashtable();

    // Logger for error messages
    private Logger _logger;

} // class BeliefUpdate
