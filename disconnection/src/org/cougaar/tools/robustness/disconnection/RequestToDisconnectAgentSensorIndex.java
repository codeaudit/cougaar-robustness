/*
 * RequestToDisconnectAgentSensorIndex.java
 *
 * Created on July 14, 2004, 10:37 AM
 */

package org.cougaar.tools.robustness.disconnection;
/**
 *
 * @author  David Wells - OBJS
 * @version 
 */
import java.util.Hashtable;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.core.persist.NotPersistable;


public class RequestToDisconnectAgentSensorIndex extends Hashtable implements NotPersistable {

    /** Creates new StateEstimationIndex */
    public RequestToDisconnectAgentSensorIndex() {
    }

    protected final static UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {  
                return 
                    (o instanceof RequestToDisconnectAgentSensorIndex);
            }
        };

    protected RequestToDisconnectAgentSensor putDiagnosis(RequestToDisconnectAgentSensor diag) {
        return (RequestToDisconnectAgentSensor) super.put(diag.getAssetID(), diag);
    }
    
    protected RequestToDisconnectAgentSensor getDiagnosis(AssetID assetID) {
        return (RequestToDisconnectAgentSensor) super.get(assetID);
    }

}