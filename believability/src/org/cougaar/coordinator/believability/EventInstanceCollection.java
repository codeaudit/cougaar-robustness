/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: EventInstanceCollection.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/EventInstanceCollection.java,v $
 * $Revision: 1.1 $
 * $Date: 2004-06-29 22:43:18 $
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

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.techspec.AssetTechSpecInterface;
import org.cougaar.coordinator.techspec.DefaultAssetTechSpec;
import org.cougaar.coordinator.techspec.EventDescription;

/**
 * Collects EventInstanceModels together for a given asset instance
 * (collection will only refer to a singler state dimension as an
 * artifact of the nature of the threat and event objects). 
 *
 * @author Tony Cassandra
 * @version $Revision: 1.1 $Date: 2004-06-29 22:43:18 $
 *
 */
class EventInstanceCollection extends Model
{

    // Note we do not implement the java.util.Collection interface
    // because we do not yet need most of that API.  If this gets more
    // broadly used, then we would retrofit this.
    //

    // At the top-level of this data structure is a hashtable, which
    // maps event names into a Set of EventInstanceModel objects.
    // This organization is done because grouping by event names is
    // the way the information needs to be accessed in the belief
    // update calculations.
    //

    //------------------------------------------------------------
    // public interface
    //------------------------------------------------------------

    //************************************************************
    /**
     * Method description comments go here ...
     *
     * @param 
     */
    public String toString( )
    {
        StringBuffer buff = new StringBuffer();

        buff.append( "EventInstanceCollection for asset " 
                     + _asset_id + "\n" );
        
        buff.append( "\tTotal events = " 
                     + _event_instance_hash.size() + "\n");

        Enumeration enum = _event_instance_hash.elements();
        while( enum.hasMoreElements() )
        {
            EventInstanceModel event = (EventInstanceModel) enum.nextElement();

            buff.append( event.toString( "\t" ));
        } // while iter

        return buff.toString();
  
    } // method toString

    //------------------------------------------------------------
    // package interface
    //------------------------------------------------------------

    // This is for testing to ignore any event probability that comes
    // from the tech-specs.  If 'true' this will force it to have a
    // small non-zero probility to help debug the belief update
    // calculations.  The tech spec derived probabilities are
    // time-dependent making testing difficult.
    //
    static final boolean OVERRIDE_EVENT_PROBABILITIES = false;

    //************************************************************
    /**
     * Builds the data structure needed to do the believability
     * calculations from an asset ID and a set of
     * ThreatModelinterfaces.
     *
     * @param asset_id The ID objects of the asset instance we want to
     * build the model for.
     * @param stress_set A set of StressInstance objects, from which
     * we will will construct the set of EventInstances that pertain
     * to this asset instance.
     * @param asset_dim_model The asset state dimension model that
     * this event affects
     */
    EventInstanceCollection( AssetID asset_id,
                             HashSet stress_set,
                             AssetTypeDimensionModel asset_dim_model )
            throws BelievabilityException    
    {
        // Hard work is done in here.
        //
        setContents( asset_id, stress_set, asset_dim_model );
        

    }  // constructor EventInstanceCollection

    //************************************************************
    /**
     * Builds the data structure needed to do the believability
     * calculations from an asset ID and a set of
     * ThreatModelinterfaces.
     *
     * @param asset_id The ID objects of the asset instance we want to
     * build the model for.
     * @param stress_set A set of StressInstance objects, from which
     * we will will construct the set of EventInstances that pertain
     * to this asset instance.
     * @param asset_dim_model The asset state dimension model that
     * this event affects
     */
    void setContents( AssetID asset_id,
                      HashSet stress_set,
                      AssetTypeDimensionModel asset_dim_model )
            throws BelievabilityException
    {
        // First we find all the stresses that are aplicable to this
        // asset.  As we find them, we will need to group them by
        // the event they cause.  Thus, we will end up with a
        // (possibly empty) set of events, where each event will have
        // one or more StressInstances that cause this event.
        //
        // Further, the StressInstances may actually be transitively
        // affected by other stresses. However, the transitive
        // connections from a stress instance to the streses that
        // cause it should already be part of the stress instances.
        // These linkages are supposed to be established when the
        // stress instance is first created (happens in the
        // ModelManager). 
        //

        this._asset_id = asset_id;

        logDebug ( "Creating new event collection for asset "
                   + asset_id.getName()
                   + " in state dim "
                   + asset_dim_model.getStateDimensionName( ) );

        logDebug ( "Considering a set of " 
                   + stress_set.size() + " stresses." );

        // This is the way to convert and IS into its techspec.
        //
        AssetTechSpecInterface asset_ts 
                = DefaultAssetTechSpec.findAssetByID( asset_id );

        if ( asset_ts == null )
        {
            logWarning( "Found a NULL tech spec for asset ID "
                        + asset_id.getName() 
                        + ". Assuming no events/stresses." );
            return;
        }

        Iterator stress_iter = stress_set.iterator();
        while( stress_iter.hasNext() )
        {
            StressInstance stress = (StressInstance) stress_iter.next();

            if ( ! stress.affectsAsset( asset_ts ))
            {
                logDebug ( "Stress " + stress.getName()
                           + " is not applicable for " + asset_id.getName() );
                continue;
            }

            logDebug ( "Applicable Stress " + stress.getName()
                       + " found for " + asset_id.getName() );
            
            // See whether or not we already have a stres fo this
            // event.
            //
            EventDescription event_desc
                    = stress.getEventDescription();

            if ( event_desc == null )
            {
                logError( "NULL found for stress event. Skipping."); 
                continue;
            }

            EventInstanceModel event_im 
                    = (EventInstanceModel) _event_instance_hash.get
                    ( event_desc.getName() );

                    // If not, then create one.
            //
            if ( event_im == null )
            {
                event_im = new EventInstanceModel( event_desc,
                                                   asset_dim_model );
                _event_instance_hash.put( event_desc.getName(),
                                          event_im );

                logDebug ( "New stress event added: " + event_im.getName() );
            }
            else
            {
                logDebug ( "Using existing stress event: "
                           + event_im.getName() );
            }

            event_im.addStress( stress );

        } // while stress_iter

    }  // method setContents

    //************************************************************
    /**
     * simple accessor
     */
    int getNumEvents() { return _event_instance_hash.size(); }

    //************************************************************
    /**
     * Returns the transition probability matix resulting from all
     * threats and all events that could affect this asset state
     * dimension (either directly or through trabnsitive events).
     *
     * @param asset_id The ID of the asset instance
     * @param start_time Start time (transitions are time-dependent)
     * @param end_time End time (transitions are time-dependent)
     */
    double[][] getTransitionMatrix( long start_time,
                                    long end_time )
            throws BelievabilityException
    {
        // Note that because of the parameterize-by-time nature of
        // the threat probabilities and the dynamic asset
        // membership of the threats, we *cannot* precompute this
        // transition matrix. 
        //

        // Getting the transition probability matrix at the most
        // abstract is a two step process.  We have to find all the
        // unique events that are acting on this asset state
        // dimension, and blend their individual transition matrices
        // according to the probability of each event.  But to do
        // this, requires us to blend together all the possible
        // threats that can cause each event event.
        //
        
        // Determining the probability of each threat is further
        // complicated by the fact that there are transitive effects
        // and so multiple threats can lead to other threats, etc. on
        // down until one of the threat is applicable to the asset and
        // the event applies.
        //

        // General algorithm: Find each event, and compute the
        // probability of each event (possibly factoring in multiple
        // threats and transitive effects.)  Blending the threats
        // together is done in the StressInstanceCollection class
        // using ProbabilityUtils.computeEventUnionProbability. Then,
        // consider all possible combinations of the events occurring
        // and not occurring, building up the transition matrix from
        // the individual event components.  This blending of events
        // occurs in the ProbabilityUtils.mergeMultiEventTransitions()
        // method.
        //

        // This array will hold (for each event) the transition matrix
        // *given* the event occurs. (We will assume the transitions
        // are the identity matrix for the case when the event does
        // not occur.) First index: the event, second index: the 'from'
        // asset state, third index: the 'to' asset state.
        //
        double event_trans[][][]= new double[getNumEvents()][][];

        // This will hold (for each event) the probability that the
        // event occurs (independent of all others).  This may need to
        // factor in one or more threats by computing the probability
        // of the union of all the threats that can cause a given
        // event.
        //
        double event_probs[] = new double[getNumEvents()];

        // Fill out the arrays by looping over each event.
        //
        Enumeration event_enum = _event_instance_hash.elements();
        for ( int idx = 0; event_enum.hasMoreElements(); idx++ )
        {
            EventInstanceModel event 
                    = (EventInstanceModel) event_enum.nextElement();

            event_trans[idx] = event.getEventTransitionMatrix();

            event_probs[idx] = event.getEventProbability( start_time,
                                                          end_time );

            if ( OVERRIDE_EVENT_PROBABILITIES )
            {
                logError( "OVERRIDE EVENT PROBS.  THIS IS FOR TESTING ONLY!");
                
                if ( event_probs[idx] < 0.0001 )
                    event_probs[idx] = 0.1;
                else if ( event_probs[idx] > 0.9999 )
                    event_probs[idx] = 0.9;
                else
                    event_probs[idx] = 0.5;

                logDebug( "New event prob for  " + event.getName() 
                          + " is " + event_probs[idx] );
            }

        } // for idx

        logDebug( "Event probs: " 
                  + ProbabilityUtils.arrayToString( event_probs ));
        
        logDebug( "Event transitions:\n" 
                  + ProbabilityUtils.arrayToString( event_trans, "\t" ));
        
        // Ok, so now we have the individual state transition
        // probabilities for each event and the individual
        // probabilities that each event will occur.  Next up is
        // merging these into a single transition matrix. Note the
        // assumption that if an event does not occur, then the state
        // remains unchanged. Or more accurately, if no events occurs,
        // there is no state change.
        //
        return ProbabilityUtils.mergeMultiEventTransitions( event_probs,
                                                            event_trans );


    } // method getTransitionMatrix

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    // This is the asset for which this is the collection of
    // stresses/events that apply to it.
    //
    // FIXME: Might this be better to have a set of asset IDs as far
    // as efficiency? Defer until it actually works and is proven to
    // be problematic. 
    //
    private AssetID _asset_id;

    // Maps event names (String) to EventInstance objects.
    //
    Hashtable _event_instance_hash = new Hashtable();

} // class EventInstanceCollection