/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: BeliefState.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/BeliefState.java,v $
 * $Revision: 1.23 $
 * $Date: 2004-08-05 17:14:19 $
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
 * @version $Revision: 1.23 $Date: 2004-08-05 17:14:19 $
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
    public AssetID getAssetID() { return _asset_id; }

    //************************************************************
    /**
     * Gets the time at which this belief state was calculated
     */
    public long getTimestamp() { return _timestamp; }

    //************************************************************
    /**
     * Simple accessor
     *
     * @return the trigger object that resulted in the existence of
     * this belief state (if any)
     */
    public BeliefUpdateTrigger getUpdateTrigger() { return _trigger; }

    //************************************************************
    /**
     * Sets the object that resulted in the belief computation.
     *
     * @param trigger The triggering event/object (a diagnosis or action)
     */
    void setUpdateTrigger( BeliefUpdateTrigger trigger ) 
    { 
        _trigger = trigger; 
    }

    //************************************************************
    /**
     * Gets the diagnosis that led to this belief state (if any)
     *
     * @return The diagnosis value if this belief sttae resuklted from
     * the arrival of a diagnosis.  Returns null if this belief sttae
     * resulted from some other triggering event. 
     */
    public BelievabilityDiagnosis getDiagnosis()
    {
        if ( _trigger instanceof BelievabilityDiagnosis)
            return (BelievabilityDiagnosis) _trigger;
        else
            return null;

    } // method 

    //************************************************************
    /**
     * Gets the action that led to this belief state (if any)
     *
     * @return The action value if this belief state resuklted from
     * the arrival of a action.  Returns null if this belief sttae
     * resulted from some other triggering event. 
     */
    public BelievabilityAction getAction() 
    {
        if ( _trigger instanceof BelievabilityAction)
            return (BelievabilityAction) _trigger;
        else
            return null;

    } // method BelievabilityAction

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

        buff.append( "BeliefState (" + super.toString() + "):"
                     + "\n\tAssetID: " + _asset_id
                     + "\n\tAssetType: " + _asset_type_model.getName()
                     + "\n\tTrigger: " + _trigger
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
    /**
     * Simple mutators
     *
     * @param long timestamp to be set
     */
    void setTimestamp( long timestamp ) { _timestamp = timestamp; }

    //************************************************************
    /**
     * Sets Asset ID for this belief state
     *
     * @param aid The asset id to be set
     */
    void setAssetID( AssetID aid ) 
    {
        _asset_id = aid; 
        
        Enumeration enum = _belief_dimensions.elements();
        while ( enum.hasMoreElements() )
        {
            BeliefStateDimension belief_dim 
                    = (BeliefStateDimension) enum.nextElement();

            belief_dim.setAssetID( aid );
        } // while enum

    } // method setAssetID

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
            belief._asset_id = _asset_id;
            belief._trigger = _trigger;
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

    private AssetID _asset_id;

    private AssetTypeModel _asset_type_model;

    // This is the immediate trigger that gave rise to this belief
    // state.  If it is null, then that indicates this is the a priori
    // belief for this asset.  Otherwise, it is set to the handle that
    // is sent in to the updateBelief() 
    //
    private BeliefUpdateTrigger _trigger;

    private long _timestamp;

    // An ordered list of BeliefStateDimension objects that represent
    // the belief state for the individual state dimensions of this
    // asset. 
    //
    private Vector _belief_dimensions = new Vector();

} // class BeliefState
