/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: TechSpecConsumerInterface.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/Attic/TechSpecConsumerInterface.java,v $
 * $Revision: 1.1 $
 * $Date: 2004-05-10 19:21:57 $
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
import org.cougaar.coordinator.techspec.ActionTechSpecInterface;
import org.cougaar.coordinator.techspec.DiagnosisTechSpecInterface;
import org.cougaar.coordinator.techspec.ThreatModelInterface;


/**
 * Class description goes here ...
 *
 * @author Tony Cassandra
 * @version $Revision: 1.1 $Date: 2004-05-10 19:21:57 $
 * 
 *
 */
public interface TechSpecConsumerInterface
{

    //------------------------------------------------------------
    // public interface
    //------------------------------------------------------------

    // This will probably not be used, but it is here for completeness
    // and in case we change our minds. 
    public void consumeAssetStateDimension( AssetTechSpecInterface asset_ts );
    public void consumeSensorType( DiagnosisTechSpecInterface diag_ts );
    public void consumeThreatModel( ThreatModelInterface threat_model );
    public void consumeActuatorType( ActionTechSpecInterface actuator_ts );

    public void removeAssetStateDimension( AssetTechSpecInterface asset_ts );
    public void removeSensorType( DiagnosisTechSpecInterface diag_ts );
    public void removeThreatModel( ThreatModelInterface threat_model );
    public void removeActuatorType( ActionTechSpecInterface actuator_ts );

} // class TechSpecConsumerInterface
