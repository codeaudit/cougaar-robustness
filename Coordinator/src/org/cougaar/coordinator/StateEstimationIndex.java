/*
 * StateEstimationIndex.java
 *
 * Created on April 27, 2004, 4:30 PM
 */

package org.cougaar.coordinator;

/**
 *
 * @author  David Wells - OBJS
 * @version 
 */

import java.util.Hashtable;
import java.util.Collection;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.coordinator.believability.StateEstimation;
import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.housekeeping.IndexKey;
import org.cougaar.core.persist.NotPersistable;


public class StateEstimationIndex extends Hashtable implements NotPersistable {

    /** Creates new StateEstimationIndex */
    public StateEstimationIndex() {
    }

    protected final static UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {  
                return 
                    (o instanceof StateEstimationIndex);
            }
        };

    protected StateEstimation indexStateEstimation(StateEstimation se, IndexKey key) {
        return (StateEstimation) super.put(se.getAssetID(), se);
    }
    
    protected StateEstimation findStateEstimation(AssetID assetID) {
        return (StateEstimation) super.get(assetID);
    }

    protected StateEstimation removeStateEstimation(StateEstimation se, IndexKey key) {
        return (StateEstimation) super.remove(se.getAssetID());
    }

}
