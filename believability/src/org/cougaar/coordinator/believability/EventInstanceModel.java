/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: EventInstanceModel.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/EventInstanceModel.java,v $
 * $Revision: 1.19 $
 * $Date: 2004-08-09 20:46:41 $
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

import org.cougaar.coordinator.techspec.EventDescription;
import org.cougaar.coordinator.techspec.AssetState;

/**
 * Defines an event for a specific asset ID. This event determines how
 * the asset will change state based on all the stresses (direct and
 * indirect) that can lead to this event. 
 *
 * @author Tony Cassandra
 * @version $Revision: 1.19 $Date: 2004-08-09 20:46:41 $
 *
 */
class EventInstanceModel extends Model
{

    //------------------------------------------------------------
    // package interface
    //------------------------------------------------------------

    //************************************************************
    /**
     * Main constructor.
     *
     * @param event_desc The event description that has the
     * information we need to build this instance.
     * @param asset_dim_model The asset state dimension model that
     * this event affects
     */
    EventInstanceModel( EventDescription event_desc,
                        AssetTypeDimensionModel asset_dim_model )
            throws BelievabilityException
    {
        setContents( event_desc, asset_dim_model);

    }  // constructor EventInstanceModel

    //************************************************************
    /**
     * Main constructor.
     *
     * @param event_desc The event description that has the
     * information we need to build this instance.
     * @param asset_dim_model The asset state dimension model that
     * this event affects
     */
    void setContents( EventDescription event_desc,
                      AssetTypeDimensionModel asset_dim_model )
            throws BelievabilityException       
    {
        this._event_desc = event_desc;

        if ( event_desc == null )
            throw new BelievabilityException
                    ( "EventInstanceModel.setContents()",
                      "Event description is NULL: " );

        String state_dim_name = event_desc.getAffectedStateDimensionName();
        
        if ( ! state_dim_name.equalsIgnoreCase
             ( asset_dim_model.getStateDimensionName() ) )
            throw new BelievabilityException
                    ( "ThreatRootModel.setContents()",
                      "Event dimension doesn't match asset: "
                      + state_dim_name + " != " 
                      + asset_dim_model.getStateDimensionName() );

        int num_state_values 
                = asset_dim_model.getNumStateDimValues( );

        // Note that in the current event effects model, there is only
        // the presence or absence of a state transition, so the
        // probabilkities are 0.0 for everything but those things
        // explicitly shown in the direct effects, and those
        // have probability of 1.0.
        //
        this._event_trans_prob 
                = new double[num_state_values][num_state_values];

        for ( int from_idx = 0;
              from_idx < _event_trans_prob.length;
              from_idx++ )
        {
            AssetState from_state 
                    = asset_dim_model.getAssetState( from_idx );
            
            AssetState to_state 
                    = event_desc.getDirectEffectTransitionForState
                    ( from_state );

            int to_idx = asset_dim_model.getStateDimValueIndex
                    ( to_state.getName() );

             _event_trans_prob[from_idx][to_idx] = 1.0;

       } // for from_idx

    }  // method setContents

    //************************************************************
    /**
     * Simple accessor
     */
    EventDescription getEventDescription() 
    {
        return _event_desc;
    } // method getEventDescription

    //************************************************************
    /**
     * Simple accessor
     */
    String getName() { return getEventDescription().getName(); }

    //************************************************************
    /**
     * Simple accessor
     */
    StressInstanceCollection getStressCollection() { return _stress_set; }

    //************************************************************
    /**
     * Adds a stress as a direct cause of this event instance.
     *
     * @param stress The stress to be added as acause of this event.
     */
    void addStress( StressInstance stress )
    {
        // Method implementation comments go here ...
        _stress_set.add( stress );

    } // method addStress
    //************************************************************
    /**
     * Returns the matrix of transition probabilities for the event
     * that is associated with this threat.  These are conditioned on the
     * event that the threat is realized
     */
    double[][] getEventTransitionMatrix()
    {
        return _event_trans_prob;
    } // method getEventTransitionMatrix

    //************************************************************
    /**
     * Returns the probability that this event will occur. It
     * considers all the possible stresses, but directly and
     * indirectly affect this event.
     *
     * @param start_time The starting time to use to determine the
     * stess collection probability. 
     * @param end_time The ending time to use to determine the
     * stess collection probability. 
     */
    double getEventProbability( long start_time,
                                long end_time )
    {
        return _stress_set.getProbability( start_time, end_time );
    } // method getEventTransitionMatrix

    //************************************************************
    /**
     * Converts this to a string, but puts prefix at the start of each
     * line
     *
     * @param  prefix What to put at the start of each line.
     */
    String toString( String prefix )
    {
        StringBuffer buff = new StringBuffer();

        buff.append( prefix + "EventInstance for event " 
                     + getName() + "\n" );

        buff.append( prefix + "   Transition matrix:\n" );

        buff.append( ProbabilityUtils.arrayToString( _event_trans_prob,
                                                     prefix + "\t" ));

        buff.append( "\n" + _stress_set.toString( prefix + "\t" ));
        
        return buff.toString();
        
    } // method toString

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    // The originator of all common threat information for this class.
    //
    private EventDescription _event_desc;

    // The set of all stresses that are direct cuased of this event
    // instance. 
    //
    StressInstanceCollection _stress_set = new StressInstanceCollection();

    // Defines the transition probabilities for when the given event
    // occurs.  Note that these *do not* factor in the probbability of
    // the event happening, so must be altered by the event
    // probability before being used as state transition
    // probabilities. (see method getTransitionProbability() ).
    //
    private double[][] _event_trans_prob;

} // class EventInstanceModel
