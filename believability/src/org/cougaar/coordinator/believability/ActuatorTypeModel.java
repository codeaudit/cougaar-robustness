/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: ActuatorTypeModel.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/ActuatorTypeModel.java,v $
 * $Revision: 1.1 $
 * $Date: 2004-06-09 18:01:35 $
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
import java.util.Iterator;
import java.util.Observable;
import java.util.Set;
import java.util.Vector;

import org.cougaar.coordinator.techspec.ActionDescription;
import org.cougaar.coordinator.techspec.ActionTechSpecInterface;
import org.cougaar.coordinator.techspec.AssetState;
import org.cougaar.coordinator.techspec.AssetStateDimension;
import org.cougaar.coordinator.techspec.AssetTransitionWithCost;
import org.cougaar.coordinator.techspec.AssetType;

import org.cougaar.core.util.UID;

/**
 * Believability component's representation for all information it
 * needs concerning an Action (an actuator really) (from the tech
 * specs).
 *
 * @author Tony Cassandra
 * @version $Revision: 1.1 $Date: 2004-06-09 18:01:35 $
 *
 */
class ActuatorTypeModel extends Model
{

    //------------------------------------------------------------
    // package interface
    //------------------------------------------------------------

    /**
     * Main constructor for creating a actuator type model.
     *
     * @param action_ts The action tech spec from which we get actuator
     * information 
     * @param asset_type_model The asset type model for the asset that
     * this actuator will be sensing.
     */
    ActuatorTypeModel( ActionTechSpecInterface action_ts,
                       AssetTypeDimensionModel asset_dim_model )
            throws BelievabilityException
    {
        setContents( action_ts, asset_dim_model );
        setValidity( true );

    }  // constructor ActuatorTypeModel

    //************************************************************
    /**
     * Get the name of the actuator
     *
     * @return the name of the actuator
     **/
    String getName() { return _name; }

    //************************************************************
    /**
     * Get the name of the state dimension this actuator affects
     *
     * @return the name of the state dimension
     **/
    String getStateDimName() { return _state_dim_name; }

    
    //************************************************************
    /**
     * Return the conditional state transition probabilities
     * associated with this actuator.
     *
     */
    double[][] getTransitionProbabilityMatrix( )
    {
        return _trans_prob;

    } // method getTransitionProbabilityMatrix

    //************************************************************
    /**
     * Convert this model to a string.
     *
     */
    public String toString( )
    {
        StringBuffer buff = new StringBuffer();

        buff.append( "\tActuator Type Name: " + _name + "\n" );
        buff.append( "\tState Dimension: " + _state_dim_name  + "\n");

        // FIXME: Need to add more infomration
        
        return buff.toString();
    } // method toString

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    private String _name;

    // Original handles to sources of model information.
    //
    private ActionTechSpecInterface _action_ts;
    private AssetTypeDimensionModel _asset_dim_model;

    // These are all the attributes we need for an aset model.
    //
    private String _state_dim_name;
    private double[][] _trans_prob;

    //************************************************************
    /**
     * Sets this objects internal values to be those form the given
     * actuator info.
     *
     * @param asset_type The source of information for this model
     */
    private void setContents( ActionTechSpecInterface action_ts,
                              AssetTypeDimensionModel state_dim_model )
            throws BelievabilityException
    {
        if (( action_ts == null )
            || ( state_dim_model == null ))
            throw new BelievabilityException
                    ( "ActuatorTypeModel.setContents()",
                      "NULL parameter(s) sent in." );

        // FIXME: Resolve multiple ActionDescriptions to see what this
        // should be.
        this._name = null;

        this._action_ts = action_ts;
        this._asset_dim_model = state_dim_model;
        this._state_dim_name = state_dim_model.getStateDimensionName();

        // FIXME: This is where I need to figure out what do do with
        // all this information.  I think I extract everythong I need,
        // I just do not do anything with it..

        Vector action_desc_list = action_ts.getActions();
        Enumeration action_desc_enum = action_desc_list.elements();
        while( action_desc_enum.hasMoreElements() )
        {
            ActionDescription action_desc
                    = (ActionDescription) action_desc_enum.nextElement();

            String action_name = action_desc.name();
            
            AssetType asset_type = action_desc.getAffectedAssetType();
            
            AssetStateDimension state_dim 
                    = action_desc.getAffectedStateDimension();

            int num_state_values 
                    = state_dim.getPossibleStates().size();

            double[][] intermediate_trans_prob 
                    = new double[num_state_values][num_state_values];

            double[][] end_trans_prob 
                    = new double[num_state_values][num_state_values];

            for ( int from_idx = 0;
                  from_idx < end_trans_prob.length;
                  from_idx++ )
            {
                AssetState from_state 
                        = _asset_dim_model.getAssetState( from_idx );

                AssetTransitionWithCost transition
                        = action_desc.getTransitionForState( from_state );
                
                AssetState to_intermediate_state 
                        = transition.getIntermediateValue();

                AssetState to_end_state = transition.getEndValue();

                int to_intermediate_idx 
                        = _asset_dim_model.getStateDimValueIndex
                        ( to_intermediate_state.getName() );

                int to_end_idx = _asset_dim_model.getStateDimValueIndex
                        ( to_end_state.getName() );

                intermediate_trans_prob[from_idx][to_intermediate_idx] = 1.0;
                end_trans_prob[to_intermediate_idx][to_end_idx] = 1.0;
             
            } // for from_idx
            
            
        } // while action_desc_enum

    } // method setContents

} // class ActuatorTypeModel
