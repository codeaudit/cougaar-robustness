/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: SensorTypeModel.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/SensorTypeModel.java,v $
 * $Revision: 1.4 $
 * $Date: 2004-05-28 20:56:43 $
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

import java.util.Iterator;
import java.util.Observable;
import java.util.Set;
import java.util.Vector;

import org.cougaar.coordinator.techspec.DiagnosisProbability;
import org.cougaar.coordinator.techspec.DiagnosisProbability.DiagnoseAs;
import org.cougaar.coordinator.techspec.DiagnosisTechSpecInterface;

import org.cougaar.core.util.UID;

/**
 * Believability component's representation for all information it
 * needs concerning a SensorType (from the tech specs). 
 *
 * @author Tony Cassandra
 * @version $Revision: 1.4 $Date: 2004-05-28 20:56:43 $
 *
 */
class SensorTypeModel extends Model
{

    // This class is highly dependent on the AssetTypeModel, since
    // this dictates the conditional probabilities of P(obs|state).
    // Thus, this class registers iteself as an observer of the asset
    // type model to track changes.  We also extend the Observable,
    // because other model (e.g., POMDP model) will need to be
    // notified of changes to this model.
    
    //------------------------------------------------------------
    // package interface
    //------------------------------------------------------------

    /**
     * Main constructor for creating a sensor type model.
     *
     * @param diag_ts The diagnosis tech spec from which we get sensor
     * information 
     * @param asset_type_model The asset type model for the asset that
     * this sensor will be sensing.
     */
    SensorTypeModel( DiagnosisTechSpecInterface diag_ts,
                     AssetTypeModel asset_type_model )
            throws BelievabilityException
    {
        updateContents( diag_ts, asset_type_model );
        
        // If all goes well, add this sensory model to the asset type
        // model, since there is a close relationship between them
        // (state dependencies).  The asset type model will be an
        // observer of this model.
        //
        _asset_type_model.addSensorTypeModel( this );

    }  // constructor SensorTypeModel

    //************************************************************
    /**
     * Resets this objects internal values to be those form the given
     * sensor info.
     *
     * @param diag_ts The diagnosis tech spec from which we get sensor
     * information 
     * @param asset_type_model The asset type model for the asset that
     * this sensor will be sensing.
     */
    void updateContents( DiagnosisTechSpecInterface diag_ts,
                         AssetTypeModel asset_type_model )
            throws BelievabilityException
    {
        try
        {
            setContents( diag_ts, asset_type_model );
        }

        // Whether or not an exception is thrown, we want to notify
        // the observers that we have changed.  If we atempt to update
        // the contents and fail, then the model will be in an invalid
        // state so we need to notify the obvservers of this.
        //
        finally
        {
            setChanged();
            notifyObservers();
        }
        
    } // method updateContents

    //************************************************************
    /**
     * Get the name of the sensor
     *
     * @return the name of the sensor
     **/
    String getName() { return _diag_ts.getName(); }

    //************************************************************
    /**
     * Get the latency of this sensor
     *
     * @return the latency of this sensor
     **/
    long getLatency() { return _latency; }

    //************************************************************
    /**
     * Get the positional index value for the given observation
     * (diagnosis) value.
     *
     * @param obs_name The sensor observation (diagnosis) name
     * @return the positional index position, or -1 if the observation
     * name not found or this sensor type does not contain valid
     * values.
     **/
    int getObsNameIndex( String obs_name ) 
    {
        for ( int obs_idx = 0; obs_idx < _obs_names.length; obs_idx++ )
        {
            if ( _obs_names[obs_idx].equalsIgnoreCase( obs_name ))
                return obs_idx;
        } // for dim_idx

        return -1;

    } // method getObsNameIndex

    //************************************************************
    /**
     * Checks to see if the parameter corresponds to a valid
     * observation name for this sensor type model.
     *
     * @param obs_name The sensor observation (diagnosis) name
     * @return true if name matches one of the observation names, and
     * false if not.
     **/
    boolean isValidObsName( String obs_name ) 
    {
        return getObsNameIndex( obs_name ) >= 0;
    } // method isValidStateDimName

    //************************************************************
    /**
     * Convert this model to a string.
     *
     */
    public String toString( )
    {
        StringBuffer buff = new StringBuffer();

        buff.append( "\tSensor Type Name: " + _name + "\n" );
        buff.append( "\tSensor UID: " + _uid  + "\n");
        buff.append( "\tSensor Latency: " + _latency  + "\n");
        buff.append( "\tAsset Type: " + _asset_type_model.getName()  + "\n");
        buff.append( "\tState Dimension: " + _state_dim_name  + "\n");

        buff.append( "\tPossible values [" );
        for ( int obs_idx = 0; obs_idx <  _obs_names.length; obs_idx++ )
        {
            buff.append( " " + _obs_names[obs_idx] );
        }
        buff.append( " ]\n" );

        for ( int state_idx = 0; 
              state_idx < _asset_type_model.getNumStateDimValues
                      ( _state_dim_name ); 
              state_idx++ )
        {
            for ( int obs_idx = 0; obs_idx <  _obs_names.length; obs_idx++ )
            {

                String actual_state = _asset_type_model.getStateDimValueName
                        ( _state_dim_name, state_idx );
                
                buff.append( "\t\tP( " + _obs_names[obs_idx]
                             + " | " + actual_state
                             + " ) = " + _obs_prob[obs_idx][state_idx]
                             + "\n");

            } // for obs_idx
        } // for state_idx
   
        return buff.toString();
    } // method toString

    //------------------------------------------------------------
    // protected interface
    //------------------------------------------------------------

    //**************************************************************
    /** 
     * This checks to see whether or not the sensor type model is
     * still consistent with the AssetTypeModel in terms of the
     * presenbce of the state dimension andthe number of sate values
     * it can take.
     * 
     * @return If the sensor type model is still consistent with the
     * asset type model, then true is returned. Otherwise, this object
     * is set to be invalid and false is returned.
     */
    protected boolean isStillConsistent( ) 
    {
        // The only thing we care about here is whether this state
        // dimension exists, and has the same number of possible
        // values.  We ignore name changes and other properties that
        // do not impact this sensor model.
        //
        
        if ( ! _asset_type_model.isValidStateDimName( _state_dim_name ))
        {
            setValidity( false );
            return false;
        }

        if ( _obs_prob[0].length 
             != _asset_type_model.getNumStateDimValues( _state_dim_name ))
        {
            setValidity( false );
            return false;
        }

        return true;
    } // update

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    // Original handles to sources of model information.
    //
    private DiagnosisTechSpecInterface _diag_ts;
    private AssetTypeModel _asset_type_model;

    // These are all the attributes we need for an aset model.
    //
    private String _name;
    private UID _uid;
    private int _latency;
    private String _state_dim_name;
    private String[] _obs_names;
    private double[][] _obs_prob;

    //************************************************************
    /**
     * Sets this objects internal values to be those form the given
     * sensor info.
     *
     * @param asset_type The source of information for this model
     */
    private void setContents( DiagnosisTechSpecInterface diag_ts,
                              AssetTypeModel asset_type_model )
            throws BelievabilityException
    {
        if (( diag_ts == null )
            || ( asset_type_model == null ))
            throw new BelievabilityException
                    ( "SensorTypeModel.setContents()",
                      "NULL parameter(s) sent in." );

        // In case we begin with this model being valid, we want to
        // be able to have this be invalid if we only get part way
        // through this method before throwing some exception.
        //
        setValidity( false );

        this._diag_ts = diag_ts;
        this._asset_type_model = asset_type_model;

        _name = diag_ts.getName();
        _uid = diag_ts.getUID();
        _latency = diag_ts.getLatency();
        _state_dim_name = diag_ts.getStateDimension().getStateName();

        if ( ! asset_type_model.isValidStateDimName( _state_dim_name ))
            throw new BelievabilityException
                    ( "SensorTypeModel.setContents(" + _name + ")",
                      "Sensor state dimension doesn't match asset:"
                      + _state_dim_name );

        Set obs_set = diag_ts.getPossibleValues();

        if ( obs_set == null )
            throw new BelievabilityException
                    ( "SensorTypeModel.setContents(" + _name + ")",
                      "NULL found when retrieving diagnosis name set." );
        
        if ( obs_set.size() == 0 )
            throw new BelievabilityException
                    ( "SensorTypeModel.setContents(" + _name + ")",
                      "Empty diagnosis name set." );
        
        _obs_names = new String[obs_set.size()];

        Iterator name_iter = obs_set.iterator();
        for ( int obs_idx = 0; name_iter.hasNext(); obs_idx++ )
        {
            _obs_names[obs_idx] = (String) name_iter.next();
            logDebug( "Adding observation name: " + _obs_names[obs_idx] );

        } // while name_iter

        int num_state_values 
                = asset_type_model.getNumStateDimValues( _state_dim_name );

        _obs_prob = new double[obs_set.size()][num_state_values];

        Vector diag_prob_list = _diag_ts.getDiagnosisProbabilities();

         if ( diag_prob_list == null )
            throw new BelievabilityException
                    ( "SensorTypeModel.setContents(" + _name + ")",
                      "NULL found for diagnosis probabilities." );
        
        if ( diag_prob_list.size() == 0 )
            throw new BelievabilityException
                    ( "SensorTypeModel.setContents(" + _name + ")",
                      "Empty diagnosis probability vector." );

        if ( diag_prob_list.size() != num_state_values )
            throw new BelievabilityException
                    ( "SensorTypeModel.setContents(" + _name + ")",
                      "Too few/many probabilities for state values: "
                      + diag_prob_list.size() + " != " +  num_state_values );

        Iterator diag_prob_iter = diag_prob_list.iterator();
        while( diag_prob_iter.hasNext() )
        {
            DiagnosisProbability obs_prob
                    = (DiagnosisProbability) diag_prob_iter.next();

            String actual_state = obs_prob.getActualState().getName();

            int state_idx
                    = asset_type_model.getStateDimValueIndex( _state_dim_name,
                                                              actual_state );

            if ( state_idx < 0 )
                throw new BelievabilityException
                        ( "SensorTypeModel.setContents(" + _name + ")",
                          "Bad asset state value found: "
                          + _state_dim_name + ":" + actual_state );

            Vector diag_as_list = obs_prob.getProbabilities();

            if ( diag_as_list == null )
                throw new BelievabilityException
                        ( "SensorTypeModel.setContents(" + _name + ")",
                          "NULL found for diagnosis probability vector: "
                          + _state_dim_name + ":" + actual_state );
            
            if ( diag_as_list.size() == 0 )
                throw new BelievabilityException
                        ( "SensorTypeModel.setContents(" + _name + ")",
                          "Empty diagnosis probability vector found: "
                          + _state_dim_name + ":" + actual_state );

            double obs_prob_sum = 0.0;

            Iterator diag_as_iter = diag_as_list.iterator();
            while( diag_as_iter.hasNext() )
            {
                DiagnoseAs diag_as = (DiagnoseAs) diag_as_iter.next();

                String obs_name = diag_as.getDiagnosisValue();

                int obs_idx = getObsNameIndex( obs_name );

                if ( obs_idx < 0 )
                    throw new BelievabilityException
                            ( "SensorTypeModel.setContents(" + _name + ")",
                              "Bad diagnosis value found: " + obs_name 
                              + " for " 
                              + _state_dim_name + ":" + actual_state );

                double prob = diag_as.getProbability();

                _obs_prob[obs_idx][state_idx] = prob;
                
                obs_prob_sum += prob;
                
            } // while diag_as_iter

            if ( ! Precision.isEqual( obs_prob_sum, 1.0 ))
                throw new BelievabilityException
                        ( "SensorTypeModel.setContents(" + _name + ")",
                          "Observation probabilities do not sum to 1.0. (sum="
                          + obs_prob_sum + ") for "
                          + _state_dim_name + ":" + actual_state );

        } // while diag_prob_iter

        // Only at this point do we know that we have successfully set
        // all the values.
        //
        setValidity( true );

    } // method setContents

} // class SensorTypeModel
