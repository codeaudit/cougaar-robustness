/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: ThreatTypeModel.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/Attic/ThreatTypeModel.java,v $
 * $Revision: 1.2 $
 * $Date: 2004-05-28 20:01:17 $
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

import java.util.Hashtable;

import org.cougaar.coordinator.techspec.AssetState;
import org.cougaar.coordinator.techspec.EventDescription;
import org.cougaar.coordinator.techspec.NegativeIntervalException;
import org.cougaar.coordinator.techspec.ThreatDescription;

/**
 * Local believability model to capture the threat and event
 * information.  
 *
 * @author Tony Cassandra
 * @version $Revision: 1.2 $Date: 2004-05-28 20:01:17 $
 * 
 *
 */
class ThreatTypeModel extends Model
{

    // Class implmentation comments go here ...

    //------------------------------------------------------------
    // package interface
    //------------------------------------------------------------

    /**
     * Main constructor
     *
     * @param threat_desc The source of information for this model
     * @param asset_type_model The asset model that this threat
     * pertains to
     */
    ThreatTypeModel( ThreatDescription threat_desc,
                     AssetTypeModel asset_type_model )
            throws BelievabilityException
    {
        updateContents( threat_desc, asset_type_model );
        
        // If all goes well, add this threat model to the asset type
        // model, since there is a close relationship between them
        // (state dependencies).  The asset type model will be an
        // observer of this model.
        //
        _asset_type_model.addThreatTypeModel( this );
   
    }  // constructor ThreatTypeModel

    //************************************************************
    /**
     * Resets this objects internal values to be those form the given
     * sensor info.
     *
     * @param threat_desc The source of information for this model
     * @param asset_type_model The asset model that this threat
     * pertains to
     */
    void updateContents( ThreatDescription threat_desc,
                         AssetTypeModel asset_type_model )
            throws BelievabilityException
    {
        try
        {
            setContents( threat_desc, asset_type_model );
        }

        // Whether or not an exception is thrown, we want to notify
        // the observers that we have changed.  If we atempt to update
        // the contents and fail, then the model will be in an invalid
        // state so we need to notify the observers of this.
        //
        finally
        {
            setChanged();
            notifyObservers();
        }
        
    } // method updateContents

    //************************************************************
    /**
     * Get the name of the threat
     *
     * @return the name of the threat
     **/
    String getName() { return _threat_desc.getName(); }

    //************************************************************
    /**
     * Returns the probability of the event associated with this
     * threat over a given time interval.
     *
     * @param start_time The start of the time interval
     * @param end_time The end of the time interval
     * @return the probability that the evenbt occurs in the given
     * time interval 
     **/
    double getEventProbability( long start_time, long end_time )
           throws BelievabilityException
    {
        // Because threat probabilities can change dynamically, we
        // cannot cache these values, but must always refer back to
        // the underlying ThreatModel for these values. 

        try
        {

            return _threat_desc.getEventProbability
                    ().computeIntervalProbability( start_time, end_time );

        }
        catch (NegativeIntervalException nie)
        {
            throw new BelievabilityException
                    ( "ThreatTypeModel.getEventProbability()",
                      "NegativeInterval: " + nie.getMessage() );
        }
    } // methods getEventProbability

    //************************************************************
    /**
     * Returns the probability that the asset state dimension
     * undergoes a state transition from the starting state to the ending
     * state over the given time interval.
     *
     * @param start_time The start of the time interval
     * @param end_time The end of the time interval
     * @return the probability that the the asset occurs in the given
     * time interval 
     */
    double getTransitionProbability( int from_idx, int to_idx,
                                     long start_time, long end_time )
           throws BelievabilityException
    {

        // If the threat does not result in the event happening, then
        // we assume there is no state change.  Thus, most state
        // transitions under the "event did not happen" condition will
        // have probability zero, except...
        //
        double non_event_trans_prob = 0.0;

        // ...for the self-transitions, since absence of the event
        // means we assume that the asset remains in its current
        // state.
        //
        if ( from_idx == to_idx )
            non_event_trans_prob = 1.0;

        // If the event does occur, then the state transition is
        // dictated by the state transition matrix associated with
        // this event.
        //
        double event_trans_prob = _event_trans_prob[from_idx][to_idx];

        // This tells us the probability of the even happening.
        //
        double event_prob = getEventProbability( start_time, end_time );

        // Using the formula for total probability, we consider both
        // cases of the event happening and the event not happening to
        // get the overall state transition probabilities.
        //
        return event_prob * event_trans_prob 
                + ( 1.0 - event_prob) * non_event_trans_prob;

    } // method getTransitionProbability

     //**************************************************************
    /** 
     * This checks to see whether or not the threat type model is
     * still consistent with the AssetTypeModel in terms of the
     * presence of the state dimension and the number of state values
     * it can take.
     * 
     * @return If the threat type model is still consistent with the
     * asset type model, then true is returned. Otherwise, this object
     * is set to be invalid and false is returned.
     */
    protected boolean isStillConsistent( ) 
    {
        // The only thing we care about here is whether this state
        // dimension exists, and has the same number of possible
        // values.  We ignore name changes and other properties that
        // do not impact this threat model.
        //
        
        if ( ! _asset_type_model.isValidStateDimName( _state_dim_name ))
        {
            setValidity( false );
            return false;
        }

        if ( _event_trans_prob.length 
             != _asset_type_model.getNumStateDimValues( _state_dim_name ))
        {
            setValidity( false );
            return false;
        }

        return true;

    } // method isStillConsistent

     //************************************************************
    /**
     * Convert this model to a string.
     *
     */
    public String toString( )
    {
        StringBuffer buff = new StringBuffer();

        buff.append( "\tThreat Type Name: " + _name + "\n" );

        buff.append( "\t\tAsset Type: " 
                     + _asset_type_model.getName() + "\n" );
        
        buff.append( "\t\tState dimension: " 
                     + _state_dim_name + "\n" );
      
        buff.append( "\t\tEvent Transitions:\n" ); 
      
        for ( int from_idx = 0;
              from_idx < _event_trans_prob.length;
              from_idx++ )
        {
            String from_name = _asset_type_model.getStateDimValueName
                    ( _state_dim_name, from_idx );
            
            for ( int to_idx = 0;
                  to_idx < _event_trans_prob.length;
                  to_idx++ )
            {
                String to_name = _asset_type_model.getStateDimValueName
                        ( _state_dim_name, to_idx );
                
                buff.append( "\t\t\tP( "
                             + to_name + "|"
                             + from_name + ") = "
                             + _event_trans_prob[from_idx][to_idx]
                             + "\n" );
                
            } // for to_idx

        } // for from_idx
   
        return buff.toString();
    } // method toString

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    private ThreatDescription _threat_desc;

    private AssetTypeModel _asset_type_model;

    private String _name;

    // The state dimension that this threat affects.
    //
    private String _state_dim_name;

    // Defines the transition probabilities for when the given
    // event occurs.  Note that these *do not* factor in the
    // probbaility of the event happening, so must be altered by the
    // event probability before being used as state transition
    // probabilities. (see method getTransitionProbability() ).
    //
    private double[][] _event_trans_prob;

    //************************************************************
    /**
     * Sets this objects internal values to be those form the given
     * threat info.
     *
     * @param threat_desc The source of information for this model
     * @param asset_type_model The asset model that this threat
     */
    private void setContents( ThreatDescription threat_desc,
                              AssetTypeModel asset_type_model )
            throws BelievabilityException
    {
        if (( threat_desc == null )
            || ( asset_type_model == null ))
            throw new BelievabilityException
                    ( "ThreatTypeModel.setContents()",
                      "NULL parameter(s) sent in." );

        // In case we begin with this model being valid, we want to
        // be able to have this be invalid if we only get part way
        // through this method before throwing some exception.
        //
        setValidity( false );

        this._threat_desc = threat_desc;
        this._asset_type_model = asset_type_model;

        this._name = threat_desc.getName();
        
        EventDescription event_desc = _threat_desc.getEventThreatCauses();
        
        this._state_dim_name = event_desc.getAffectedStateDimensionName();
        
        if ( ! asset_type_model.isValidStateDimName( _state_dim_name ))
            throw new BelievabilityException
                    ( "ThreatTypeModel.setContents()",
                      "Threat's state dimension doesn't match asset: "
                      + _state_dim_name );

        int num_state_values 
                = asset_type_model.getNumStateDimValues( _state_dim_name );

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
            AssetState from_state = _asset_type_model.getAssetState
                    ( _state_dim_name, from_idx );
            
            AssetState to_state = event_desc.getDirectEffectTransitionForState
                    ( from_state );

            int to_idx = _asset_type_model.getStateDimValueIndex
                    ( _state_dim_name, to_state.getName() );

             _event_trans_prob[from_idx][to_idx] = 1.0;

       } // for from_idx

        // Only at this point do we know that we have successfully set
        // all the values.
        //
        setValidity( true );

    } // method setContents

} // class ThreatTypeModel
