/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: ModelManagerInterface.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/ModelManagerInterface.java,v $
 * $Revision: 1.13 $
 * $Date: 2004-07-12 19:30:46 $
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
 * @version $Revision: 1.13 $Date: 2004-07-12 19:30:46 $
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

    public POMDPModelInterface getPOMDPModel();

    public AssetTypeModel getAssetTypeModel( AssetType asset_type );

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
