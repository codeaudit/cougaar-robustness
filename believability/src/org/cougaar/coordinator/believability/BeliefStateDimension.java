/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: BeliefStateDimension.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/BeliefStateDimension.java,v $
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

import java.util.Vector;

import org.cougaar.coordinator.techspec.AssetType;
import org.cougaar.coordinator.techspec.AssetStateDimension;

/**
 * This represents one component of a full BeliefState, paralleling
 * the AssetType structure of consisting of multiple state
 * dimensions. 
 *
 * @author Tony Cassandra
 * @version $Revision: 1.2 $Date: 2004-05-28 20:01:17 $
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

        AssetType asset_type = _asset_type_model.getAssetType();

        return asset_type.findStateDimension( _state_dim_name );

    } // method getAssetStateDimension

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
        
        int val_idx = _asset_type_model.getStateDimValueIndex( _state_dim_name,
                                                               state_name );

        if ( val_idx < 0 )
            throw new BelievabilityException
                    ( "BeliefStateDimension.getProbability()",
                      "State name not found." );
        
        return _belief_probs[val_idx];

    } // method getProbability

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
    // protected interface
    //------------------------------------------------------------

    //************************************************************
    /**
     * Main constructor
     *
     * @param at_model The asset type model encompassing the state
     * dimension for which this is part of the belief state for
     * @param dim_idx The positional index of the state dimension, in
     * tersm fo the local internal models.
     * @param probs The probability vector defining this belief
     *state. 
     */
    protected BeliefStateDimension( AssetTypeModel at_model,
                                    int dim_idx,
                                    double[] probs )
            throws BelievabilityException
    {
        if ( at_model == null )
            throw new BelievabilityException
                    ( "BeliefStateDimension.BeliefStateDimension()",
                      "Asset type model param is NULL." );

        _asset_type_model = at_model;
        _state_dim_name = at_model.getStateDimName( dim_idx );

        if ( _state_dim_name == null )
            throw new BelievabilityException
                    ( "BeliefStateDimension.BeliefStateDimension()",
                      "State dimension name not found." );

        _belief_probs = probs;

    }  // constructor BeliefStateDimension

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
    protected BeliefStateDimension( AssetTypeModel at_model,
                                    String state_dim_name,
                                    double[] probs )
            throws BelievabilityException
    {
        _asset_type_model = at_model;
        _state_dim_name = state_dim_name;

        _belief_probs = new double[probs.length];
        
        for ( int i = 0; i < probs.length; i++ )
            _belief_probs[i] = probs[i];

    } // constructor BeliefStateDimension

    //************************************************************
    /**
     *  Returns the entire array of belief probabbilities. 
     */
    protected double[] getProbabilityArray( )
    {
        return _belief_probs;
    }

    //************************************************************
    /**
     * Does a deep clone of this object.
     */
    protected Object clone()
    {
        try
        {
            return new BeliefStateDimension( _asset_type_model,
                                             _state_dim_name,
                                             _belief_probs );
        }
        catch (BelievabilityException be)
        {
            return null;
        }

    } // method clone

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    private AssetTypeModel _asset_type_model;

    private String _state_dim_name;

    private double[] _belief_probs;

} // class BeliefStateDimension
