/*
 * RequestToDisconnectAgentDiagnosisIndex.java
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
//import org.cougaar.core.persist.NotPersistable;
import java.io.Serializable;


public class RequestToDisconnectAgentDiagnosisIndex extends Hashtable implements Serializable {

    /** Creates new StateEstimationIndex */
    public RequestToDisconnectAgentDiagnosisIndex() {
    }

    protected final static UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {  
                return 
                    (o instanceof RequestToDisconnectAgentDiagnosisIndex);
            }
        };

    protected RequestToDisconnectAgentDiagnosis putDiagnosis(RequestToDisconnectAgentDiagnosis diag) {
        return (RequestToDisconnectAgentDiagnosis) super.put(diag.getAssetID(), diag);
    }
    
    protected RequestToDisconnectAgentDiagnosis getDiagnosis(AssetID assetID) {
        return (RequestToDisconnectAgentDiagnosis) super.get(assetID);
    }

}