/*
 * NodeDisconnectDiagnosisIndex.java
 *
 * Created on June 29, 2004, 10:38 PM
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


public class RequestToDisconnectNodeSensorIndex extends Hashtable implements NotPersistable {

    /** Creates new StateEstimationIndex */
    public RequestToDisconnectNodeSensorIndex() {
    }

    protected final static UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {  
                return 
                    (o instanceof RequestToDisconnectNodeSensorIndex);
            }
        };

    protected RequestToDisconnectNodeSensor putDiagnosis(RequestToDisconnectNodeSensor diag) {
        return (RequestToDisconnectNodeSensor) super.put(diag.getAssetID(), diag);
    }
    
    protected RequestToDisconnectNodeSensor getDiagnosis(AssetID assetID) {
        return (RequestToDisconnectNodeSensor) super.get(assetID);
    }

}