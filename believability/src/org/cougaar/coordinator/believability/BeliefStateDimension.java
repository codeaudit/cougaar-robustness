/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: BeliefStateDimension.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/BeliefStateDimension.java,v $
 * $Revision: 1.27 $
 * $Date: 2004-08-06 04:18:46 $
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

import java.util.Vector;

import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.techspec.AssetType;
import org.cougaar.coordinator.techspec.AssetStateDimension;

/**
 * This represents one component of a full BeliefState, paralleling
 * the AssetType structure of consisting of multiple state
 * dimensions. 
 *
 * @author Tony Cassandra
 * @version $Revision: 1.27 $Date: 2004-08-06 04:18:46 $
 */
public class BeliefStateDimension
        implements Cloneable
{

    // Class implmentation comments go here ...

    //------------------------------------------------------------
    // public interface
    //------------------------------------------------------------

    //************************************************************
    /**
     * Gets the asset state dimension's name for which this is the
     * belief of
     */
    public String getName()
    {
        return _state_dim_name;
    } // method getName()

    //************************************************************
    /**
     * Gets the tech spec AssetStateDimension object for this state
     * dimension.  This will give access to the possible values for
     * this state dimension.
     */
    public AssetStateDimension getAssetStateDimension()
    {

        return _asset_dim_model.getAssetStateDimension();
    } // method getAssetStateDimension

    //************************************************************
    /** 
     * Simple accessor
     *
     *  @return the asset ID for this belief sate dimension.
     */
    public AssetID getAssetID( )
    {
        return _asset_id;
    } // method getAssetID

    //************************************************************
    /**
     * Gets a probability for a given state value in this state
     * dimension. 
     *
     * @param state_name The name of the state value for this state
     * dimension. 
     */
    public double getProbability( String state_name )
            throws BelievabilityException
    {
        if ( state_name == null )
            throw new BelievabilityException
                    ( "BeliefStateDimension.getProbability()",
                      "State name is NULL." );
        
        int val_idx = _asset_dim_model.getStateDimValueIndex( state_name );

        if ( val_idx < 0 )
            throw new BelievabilityException
                    ( "BeliefStateDimension.getProbability()",
                      "State name not found: " +  state_name );
        
        return _belief_probs[val_idx];

    } // method getProbability

    //************************************************************
    /**
     * Sets Asset ID for this belief state
     *
     * @param aid The asset id to be set
     */
    void setAssetID( AssetID aid ) 
    {
        _asset_id = aid; 
    } // method setAssetID

    //************************************************************
    /**
     * Standard conversion of object to string
     *
     */
    public String toString( )
    {
        StringBuffer buff = new StringBuffer();
        
        buff.append( _state_dim_name + ": [" );
        
        for ( int i = 0; i < _belief_probs.length; i++ )
            buff.append( " " + _belief_probs[i] );
        
        buff.append( " ]" );
        
        return buff.toString();
        
    } // method toString

    //------------------------------------------------------------
    // package interface
    //------------------------------------------------------------

    //************************************************************
    /**
     * Constructor used for cloning
     *
     * @param at_model The asset type model encompassing the state
     * dimension for which this is part of the belief state for
     * @param dim_idx The positional index of the state dimension, in
     * tersm fo the local internal models.
     * @param probs The probability vector defining this belief
     *state. 
     */
    BeliefStateDimension( AssetTypeDimensionModel dim_model,
                          double[] probs,
                          AssetID asset_id )
            throws BelievabilityException
    {
        _asset_dim_model = dim_model;
        _state_dim_name = dim_model.getStateDimensionName( );
        _asset_id = asset_id;

        _belief_probs = new double[probs.length];
        
        for ( int i = 0; i < probs.length; i++ )
            _belief_probs[i] = probs[i];

    } // constructor BeliefStateDimension

    //************************************************************
    /**
     *  Returns the entire array of belief probabbilities. 
     */
    double[] getProbabilityArray( )
    {
        return _belief_probs;
    }

    //************************************************************
    /**
     *  Returns the entire array of belief probabbilities. 
     */
    void setProbabilityArray( double[] probs )
    {
        for ( int i = 0; i < _belief_probs.length; i++ )
            _belief_probs[i] = probs[i];

    }

    //------------------------------------------------------------
    // protected interface
    //------------------------------------------------------------

    //************************************************************
    /**
     * Does a deep clone of this object.
     */
    protected Object clone()
    {
        try
        {
            return new BeliefStateDimension( _asset_dim_model,
                                             _belief_probs,
                                             _asset_id );
        }
        catch (BelievabilityException be)
        {
            return null;
        }

    } // method clone

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    private AssetID _asset_id;

    private AssetTypeDimensionModel _asset_dim_model;

    private String _state_dim_name;

    private double[] _belief_probs;

} // class BeliefStateDimension
