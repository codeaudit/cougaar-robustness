/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: ModelManagerInterface.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/ModelManagerInterface.java,v $
 * $Revision: 1.17 $
 * $Date: 2004-07-31 02:56:57 $
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

import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.techspec.AssetTechSpecInterface;
import org.cougaar.coordinator.techspec.AssetType;
import org.cougaar.coordinator.techspec.AssetStateDimension;
import org.cougaar.coordinator.techspec.ActionTechSpecInterface;
import org.cougaar.coordinator.techspec.DiagnosisTechSpecInterface;
import org.cougaar.coordinator.techspec.EventDescription;
import org.cougaar.coordinator.techspec.ThreatDescription;
import org.cougaar.coordinator.techspec.ThreatModelChangeEvent;
import org.cougaar.coordinator.techspec.ThreatModelInterface;

/**
 * This interface is used by the believability components when they
 * need to access information stored in local models.  The local
 * models will consist of information directly from, and derived from
 * tech spec information. 
 *
 * @author Tony Cassandra
 * @version $Revision: 1.17 $Date: 2004-07-31 02:56:57 $
 *
 */
public interface ModelManagerInterface
{

    //------------------------------------------------------------
    // public interface
    //------------------------------------------------------------

    //----------------------------------------
    // Model accessor methods
    //----------------------------------------

    public double[][] getAssetUtilities
            ( AssetType asset_type,
              AssetStateDimension state_dim )
            throws BelievabilityException;

    public double[] getWeightedAssetUtilities
            ( AssetType asset_type,
              AssetStateDimension state_dim )
            throws BelievabilityException;
    
    public void setMAUWeights( double[] mau_weights )
            throws BelievabilityException;

    public double[] getMAUWeights();

    public long getMaxSensorLatency( AssetType asset_type )
            throws BelievabilityException;

    public long getNumberOfSensors( AssetType asset_type )
            throws BelievabilityException;

    public boolean usesImplicitDiagnoses( AssetType asset_type,
                                          String sensor_name )
            throws BelievabilityException;

    public long getImplicitDiagnosisInterval( AssetType asset_type,
                                              String sensor_name )
            throws BelievabilityException;

    public long getMaxPublishInterval( AssetType asset_type )
            throws BelievabilityException;

    public long getPublishDelayInterval( )
            throws BelievabilityException;

    public double getBeliefUtilityChangeThreshold( );

    public AssetTypeModel getAssetTypeModel( AssetType asset_type );

    //----------------------------------------
    // POMDP Model methods
    //----------------------------------------

    /**
     * Get the a priori probability that the indicated asset type.
     *
     * @param asset_type The type of the asset
     *
     */
    public BeliefState getInitialBeliefState( AssetType asset_type )
            throws BelievabilityException;

    /**
     * Get the a priori probability for the indicated asset id and
     * makes sure the bleif state has that ID set in it.
     *
     * @param asset_id The ID of the asset
     * @return A new belief states set to default values, or null if
     * something goes wrong.  
     *
     */
    public BeliefState getInitialBeliefState( AssetID asset_id )
            throws BelievabilityException;

    /**
     * Used to update the belief state using the given diagnosis.
     *
     * @param start_belief initial belief state
     * @param diagnosis the diagnosis to use to determine new belief
     * state
     * @return the update belief state
     *
     */
    public BeliefState updateBeliefState( BeliefState start_belief,
                                          BeliefUpdateTrigger diagnosis )
            throws BelievabilityException;

    //----------------------------------------
    // Model mutator methods
    //----------------------------------------

    public void addSensorType( DiagnosisTechSpecInterface diag_ts );
    public void addThreatType( ThreatModelInterface threat_model );
    public void addActuatorType( ActionTechSpecInterface actuator_ts );

    public void updateSensorType( DiagnosisTechSpecInterface diag_ts );
    public void updateThreatType( ThreatModelInterface threat_model );
    public void updateActuatorType( ActionTechSpecInterface actuator_ts );

    public void removeSensorType( DiagnosisTechSpecInterface diag_ts );
    public void removeThreatType( ThreatModelInterface threat_model );
    public void removeActuatorType( ActionTechSpecInterface actuator_ts );

    public void handleThreatModelChange( ThreatModelChangeEvent tm_change );

} // class ModelManagerInterface
