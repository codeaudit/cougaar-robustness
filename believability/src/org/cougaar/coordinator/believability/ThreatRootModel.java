/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: ThreatRootModel.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/Attic/ThreatRootModel.java,v $
 * $Revision: 1.2 $
 * $Date: 2004-06-18 00:16:39 $
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

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.cougaar.coordinator.techspec.AssetState;
import org.cougaar.coordinator.techspec.EventDescription;
import org.cougaar.coordinator.techspec.NegativeIntervalException;
import org.cougaar.coordinator.techspec.ThreatDescription;
import org.cougaar.coordinator.techspec.ThreatModelInterface;

/**
 * Local believability model to capture the threat information. This
 * class serves as the threat root model consisting of all the common
 * parts of a threat description.  There is also a
 * ThreatVariationModel that contains those things that can vary on an
 * asset instance by asset instance basis.
 *
 * @author Tony Cassandra
 * @version $Revision: 1.2 $Date: 2004-06-18 00:16:39 $
 * @see ThreatVariationModel
 *
 */
class ThreatRootModel extends Model
{

    // Class implmentation comments go here ...

    //------------------------------------------------------------
    // package interface
    //------------------------------------------------------------

    /**
     * Main constructor
     *
     * @param threat_desc The source of information for this model
     * @param asset_dim_model The asset model that this threat
     * pertains to
     */
    ThreatRootModel( ThreatDescription threat_desc,
                     AssetTypeDimensionModel asset_dim_model )
            throws BelievabilityException
    {
        setContents( threat_desc, asset_dim_model );

        setValidity( true );

    }  // constructor ThreatRootModel

    //************************************************************
    /**
     * Get the name of the threat
     *
     * @return the name of the threat
     **/
    String getName() { return _name; }

    //************************************************************
    /**
     * Simple accessor
     *
     * @return the name of the event caused by this threat
     **/
    String getEventName() { return _event_name; }

    //************************************************************
    /**
     * Get the name of the state dimension this sensor monitors
     *
     * @return the name of the state dimension
     **/
    String getStateDimName() { return _state_dim_name; }

    //************************************************************
    /**
     * Returns the matrix of transition probabilities for the event
     * that is associated with this threat.  These are conditioned on
     * the event that the threat is realized
     */
    double[][] getEventTransitionMatrix()
    {
        return _event_trans_prob;
    } // method getEventTransitionMatrix

    //************************************************************
    /**
     * Adds a threat variation to this root threat model.
     *
     * @param threat_var The threat variation to add.
     */
    ThreatVariationModel addThreatVariation( ThreatModelInterface threat_mi )
    {
        
        ThreatVariationModel threat_var 
                = new ThreatVariationModel( this, threat_mi );

        _threat_variation_set.add( threat_var );
        
        return threat_var;

    } // method addThreatVariation

    //************************************************************
    /**
     * Returns the set of threat variations of this root threat model.
     *
     */
    Set getThreatVariationSet( )
    {

        return _threat_variation_set;
        
    } // method getThreatVariationSet

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
                     + _asset_dim_model.getAssetTypeName() + "\n" );
        
        buff.append( "\t\tState dimension: " 
                     + _state_dim_name + "\n" );
      
        buff.append( "\t\tEvent Transitions:\n" ); 
      
        for ( int from_idx = 0;
              from_idx < _event_trans_prob.length;
              from_idx++ )
        {
            String from_name 
                    = _asset_dim_model.getStateDimValueName( from_idx );
            
            for ( int to_idx = 0;
                  to_idx < _event_trans_prob.length;
                  to_idx++ )
            {
                String to_name 
                        = _asset_dim_model.getStateDimValueName( to_idx );
                
                buff.append( "\t\t\tPr( "
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

    // The originator of all common threat infomration for this class.
    //
    private ThreatDescription _threat_desc;

    // For a threat of a given name, there can be many instances of
    // ThreatDescription objects.  Each individual instance will have
    // a differing set of asset instances it pertains to.  Further,
    // the set of applicable assets for a given ThreatDescription can
    // change over time. Thus, a ThreatRootModel contains all the
    // information that is common to all ThreatDesciptions witha
    // common name, while a set of the individual ThreatDesciptions
    // objects will be maintained to allow us access to the dynamic
    // information.
    //
    private HashSet _threat_variation_set = new HashSet();

    private AssetTypeDimensionModel _asset_dim_model;

    private String _name;

    // The event that occurs if this threat is realized.
    private String _event_name;

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
     * @param asset_dim_model The asset state dimension model that
     * this threat affects
     */
    private void setContents( ThreatDescription threat_desc,
                              AssetTypeDimensionModel asset_dim_model )
            throws BelievabilityException
    {
        if (( threat_desc == null )
            || ( asset_dim_model == null ))
            throw new BelievabilityException
                    ( "ThreatRootModel.setContents()",
                      "NULL parameter(s) sent in." );

        // In case we begin with this model being valid, we want to
        // be able to have this be invalid if we only get part way
        // through this method before throwing some exception.
        //

        this._threat_desc = threat_desc;

        this._asset_dim_model = asset_dim_model;

        this._name = threat_desc.getName();
        
        EventDescription event_desc = threat_desc.getEventThreatCauses();
        
        this._event_name = event_desc.getName();

        if ( event_desc == null )
            throw new BelievabilityException
                    ( "ThreatRootModel.setContents()",
                      "Threat description  has NULL event description: "
                      + this._name );

        _state_dim_name = event_desc.getAffectedStateDimensionName();
        
        if ( ! _state_dim_name.equalsIgnoreCase
             ( _asset_dim_model.getStateDimensionName() ) )
            throw new BelievabilityException
                    ( "ThreatRootModel.setContents()",
                      "Event dimension doesn't match asset: "
                      + _state_dim_name + " != " 
                      + _asset_dim_model.getStateDimensionName() );

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
                    = _asset_dim_model.getAssetState( from_idx );
            
            AssetState to_state 
                    = event_desc.getDirectEffectTransitionForState
                    ( from_state );

            int to_idx = _asset_dim_model.getStateDimValueIndex
                    ( to_state.getName() );

             _event_trans_prob[from_idx][to_idx] = 1.0;

       } // for from_idx

    } // method setContents

} // class ThreatRootModel
