/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: BeliefState.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/BeliefState.java,v $
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

import java.util.Enumeration;
import java.util.Vector;

import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.techspec.AssetState;
import org.cougaar.coordinator.techspec.AssetType;
import org.cougaar.coordinator.techspec.AssetStateDimension;

/**
 * This is the class that captures a single belief tstae for a given
 * asset instance.
 *
 * @author Tony Cassandra
 * @version $Revision: 1.2 $Date: 2004-05-28 20:01:17 $
 * 
 *
 */
public class BeliefState implements Cloneable
{

    // Class implmentation comments go here ...

    //------------------------------------------------------------
    // public interface
    //------------------------------------------------------------

    //************************************************************
    /**
     * Gets the asset type for which this is the belief for
     */
    public AssetType getAssetType() 
    {
        return _asset_type_model.getAssetType(); 
    } // method getAssetType

    //************************************************************
    /**
     * Gets the asset id for which this is the belief for
     */
    public AssetID getAssetID() { return _id; }

    //************************************************************
    /**
     * Gets the time at which this belief state was calculated
     */
    public long getTimestamp() { return _timestamp; }

    //************************************************************
    /**
     * Gets the last diagnosis that led to this belief state
     */
    public BelievabilityDiagnosis getDiagnosis() { return _diagnosis; }

    //************************************************************
    /**
     * Gets the set the individual belief states for each state
     * dimension.
     *
     * @return A Vector of BeliefStateDimension objects
     */
    public Vector getAllBeliefStateDimensions()
    {
        return _belief_dimensions;

    } // method getAllBeliefStateDimensions

    //************************************************************
    /**
     * Gets the set the individual belief state for the given state
     * name dimension.
     *
     * @param state_dim_name The name of the state dimension to fetch
     * the component belief state for.  
     */
    public BeliefStateDimension getBeliefStateDimension
            ( String state_dim_name )
    {
        Enumeration belief_dim_enum = _belief_dimensions.elements();
        while( belief_dim_enum.hasMoreElements() )
        {
            BeliefStateDimension belief_dim
                    = (BeliefStateDimension) belief_dim_enum.nextElement();

            if ( belief_dim.getName().equalsIgnoreCase( state_dim_name ))
                return belief_dim;

        } // while belief_dim_enum

        return null;
    }

    //************************************************************
    /**
     * Standard conversion of object to string
     *
     */
    public String toString( )
    {
        StringBuffer buff = new StringBuffer();

        buff.append( "BeliefState:"
                     + "\n\tAssetID: " + _id
                     + "\n\tAssetType: " + _asset_type_model.getName()
                     + "\n\tDiagnosis: " + _diagnosis
                     + "\n\tTimestamp: " + _timestamp
                     + "\n\tProbabilities: "
                     + "\n" );
        
        Enumeration belief_dim_enum = _belief_dimensions.elements();
        while( belief_dim_enum.hasMoreElements() )
        {
            BeliefStateDimension belief_dim
                    = (BeliefStateDimension) belief_dim_enum.nextElement();

            buff.append( "\t\t" + belief_dim.toString() + "\n" );

        } // while belief_dim_enum
        
        return buff.toString();

    } // method toString

    //------------------------------------------------------------
    // package interface
    //------------------------------------------------------------

    //************************************************************
    /**
     * Main constructor that populates belief state with values passed
     * in.
     *
     * @param is The asset ID of which this belief state pertains
     * @param num_state_dims The number of state dimensions of the
     * asset.
     * @param num_values The number of possible state values for each
     * state dimension (ordered )
     * @param num_values The number of possible state values for each
     */
    BeliefState( AssetTypeModel at_model )
            throws BelievabilityException
    {
        if ( at_model == null )
            throw new BelievabilityException
                    ( "BeliefState.BeliefState()",
                      "Asset type model param is NULL." );

        _asset_type_model = at_model;
        
    }  // constructor BeliefState

    //************************************************************
    /**
     * Adds a belief state component to this composite belief state.
     *
     * @param belief_dim The belief state dimension component to be
     * added 
     */
    void addBeliefStateDimension( BeliefStateDimension belief_dim )
            throws BelievabilityException
    {
        if ( belief_dim == null )
            throw new BelievabilityException
                    ( "BeliefState.addBeliefStateDimension()",
                      "Cannot add NULL belief dimension." );

        _belief_dimensions.addElement( belief_dim );
        
    } // method addBeliefStateDimension

    //************************************************************
    // Simple mutators
    //

    void setAssetID( AssetID aid ) { _id = aid; }
    void setTimestamp( long timestamp ) { _timestamp = timestamp; }
    void setDiagnosis( BelievabilityDiagnosis diagnosis ) 
    { 
        _diagnosis = diagnosis; 
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
            BeliefState belief = new BeliefState( _asset_type_model );
            belief._id = _id;
            belief._diagnosis = _diagnosis;
            belief._timestamp = _timestamp;

            Enumeration belief_dim_enum = _belief_dimensions.elements();
            while( belief_dim_enum.hasMoreElements() )
            {
                BeliefStateDimension belief_dim
                        = (BeliefStateDimension) belief_dim_enum.nextElement();

                belief.addBeliefStateDimension
                        ( (BeliefStateDimension) belief_dim.clone() );

            } // while belief_dim_enum

            return belief;

        }
        catch (BelievabilityException be)
        {
            return null;
        }

    } // method clone

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    private AssetID _id;

    private AssetTypeModel _asset_type_model;

    // This is the immediate diagnosis that gave rise to this belief
    // state.  If it is null, then that indicates this is the a priori
    // belief for this asset.
    //
    private BelievabilityDiagnosis _diagnosis;

    private long _timestamp;

    // An ordered list of BeliefStateDimension objects that represent
    // the belief state for the individual state dimensions of this
    // asset. 
    //
    private Vector _belief_dimensions = new Vector();

} // class BeliefState
