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
//import org.cougaar.core.persist.NotPersistable;
import java.io.Serializable;

public class RequestToDisconnectNodeDiagnosisIndex extends Hashtable implements Serializable {

    /** Creates new StateEstimationIndex */
    public RequestToDisconnectNodeDiagnosisIndex() {
    }

    protected final static UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {  
                return 
                    (o instanceof RequestToDisconnectNodeDiagnosisIndex);
            }
        };

    protected RequestToDisconnectNodeDiagnosis putDiagnosis(RequestToDisconnectNodeDiagnosis diag) {
        return (RequestToDisconnectNodeDiagnosis) super.put(diag.getAssetID(), diag);
    }
    
    protected RequestToDisconnectNodeDiagnosis getDiagnosis(AssetID assetID) {
        return (RequestToDisconnectNodeDiagnosis) super.get(assetID);
    }

}