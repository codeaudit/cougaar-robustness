/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: ActuatorTypeModel.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/ActuatorTypeModel.java,v $
 * $Revision: 1.27 $
 * $Date: 2004-09-21 00:43:49 $
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
 * @version $Revision: 1.27 $Date: 2004-09-21 00:43:49 $
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
    String getStateDimName() 
    {
        return _asset_dim_model.getStateDimensionName(); 
    }
    
    //************************************************************
    /**
     * Return the index corresponding to this acxtion name or -1 if
     * not found.
     *
     * @param action_name The action name to map to an integer index.
     */
    int getActionValueIndex( String action_name )
    {
        if ( action_name == null )
            return -1;

        for ( int action_idx = 0; 
              action_idx < _possible_values.length; 
              action_idx++ )
        {
            if ( _possible_values[action_idx].equalsIgnoreCase
                 ( action_name ))
                return action_idx;
        } // for dim_idx
        
        return -1;
        
    } // method getTransitionProbabilityMatrix

    //************************************************************
    /**
     * Return the conditional state transition probabilities
     * associated with this actuator.
     *
     */
    double[][] getTransitionProbabilityMatrix( int action_idx )
                throws BelievabilityException
    {
        if (( action_idx < 0 )
            || ( action_idx >= _possible_values.length ))
            throw new BelievabilityException
                    ( "ActuatorTypeModel.getTransitionProbabilityMatrix()",
                      "Bad index found: " + action_idx );

        return _trans_prob[action_idx];

    } // method getTransitionProbabilityMatrix

    //************************************************************
    /**
     * Return the conditional state transition probabilities
     * associated with this actuator.
     *
     */
    double[][] getTransitionProbabilityMatrix( String action_name )
                throws BelievabilityException
    {
        return getTransitionProbabilityMatrix
                ( getActionValueIndex( action_name ));

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
        buff.append( "\tState Dimension: " + getStateDimName() + "\n" );
        buff.append( "\tNum values: " + _possible_values.length + "\n" );
        buff.append( "\tTransitions:\n" );
        
        int num_state_values = _asset_dim_model.getNumStateDimValues();

        for ( int action_idx = 0; 
              action_idx < _possible_values.length;
              action_idx++ )
        {
            for ( int from_idx = 0;
                  from_idx < num_state_values;
                  from_idx++ )
            {
                for ( int to_idx = 0;
                      to_idx < num_state_values;
                      to_idx++ )
                {
                    if ( Precision.isZeroModel
                         ( _trans_prob[action_idx][from_idx][to_idx] ))
                        continue;
                    
                    String from_state 
                            = _asset_dim_model.getStateDimValueName(from_idx);
                    String to_state 
                            = _asset_dim_model.getStateDimValueName(to_idx);
                    
                    buff.append( "\t\tPr( "
                                 + to_state + " | "
                                 + from_state + ", "
                                 + _possible_values[action_idx]
                                 + " ) = " 
                                 + _trans_prob[action_idx][from_idx][to_idx]
                                 + "\n" );
                    
                } // for to_idx
            } // for from_idx
        } // for action_idx

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

    private String[] _possible_values;
    private double[][][] _trans_prob;

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

        this._action_ts = action_ts;
        this._name = _action_ts.getName();
        this._asset_dim_model = state_dim_model;

        Vector action_desc_list = action_ts.getActions();

        if ( action_desc_list.size() < 1 )
            throw new BelievabilityException
                    ( "ActuatorTypeModel.setContents()",
                      "Actuator has no action descriptions." );

        // We will have a separate transition matrix for each
        // ActionDescription.
        //
        _possible_values = new String[action_desc_list.size()];
        _trans_prob = new double[action_desc_list.size()][][];

        Enumeration action_desc_enum = action_desc_list.elements();
        for( int action_idx = 0; 
             action_desc_enum.hasMoreElements();
             action_idx++ )
        {
            ActionDescription action_desc
                    = (ActionDescription) action_desc_enum.nextElement();

            _possible_values[action_idx] = action_desc.name();
            
            // FIXME: Uneeded sanity check?
            //
            AssetStateDimension state_dim 
                    = action_desc.getAffectedStateDimension();
            if ( ! state_dim.getStateName().equalsIgnoreCase
                 ( _asset_dim_model.getStateDimensionName()))
                throw new BelievabilityException
                        ( "ActuatorTypeModel.setContents()",
                          "Action description state dimension error: is "
                          + state_dim.getStateName()  
                          + " but should be " 
                          + _asset_dim_model.getStateDimensionName() );

            int num_state_values = _asset_dim_model.getNumStateDimValues();

            // NOTE: Our assumption is that the believability plugin
            // will only factor in asset state change actions for
            // those actions that have been reported to succeed.
            // Thus, we cann ignore the call to get the intermediate
            // sttae tranition values and only worry about the end
            // state transition values. 
            //
            _trans_prob[action_idx]
                    = new double[num_state_values][num_state_values];

            for ( int from_idx = 0;
                  from_idx < num_state_values;
                  from_idx++ )
            {
                AssetState from_state 
                        = _asset_dim_model.getAssetState( from_idx );

                AssetTransitionWithCost transition
                        = action_desc.getTransitionForState( from_state );

                if ( transition == null )
                    throw new BelievabilityException
                            ( "ActuatorTypeModel.setContents()",
                              "NULL transition from action descripton: " 
                              + _name + " with action name " 
                              + _possible_values[action_idx]
                              + " and from_state " + from_state.getName() );
                
                AssetState to_state = transition.getEndValue();

                int to_idx = _asset_dim_model.getStateDimValueIndex
                        ( to_state.getName() );

                _trans_prob[action_idx][from_idx][to_idx] = 1.0;
             
            } // for from_idx
            
        } // while action_desc_enum

    } // method setContents

} // class ActuatorTypeModel
