/*
 * ActionTimeout.java
 *
 * Created on October 17, 2003, 3:14 PM
 */

package org.cougaar.coordinator.monitoring;

import org.cougaar.core.persist.NotPersistable;
import org.cougaar.util.UnaryPredicate;

/**
 *
 * @author  David Wells - OBJS
 * @version 
 */

import org.cougaar.coordinator.DiagnosisSnapshot;
import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.ActionsWrapper;
import org.cougaar.coordinator.Action;



public class ActionTimeoutCondition implements NotPersistable {
    
    private AssetID assetID;
    private ActionsWrapper actionsWrapper;
    private String result;

    /** Creates new DefenseTimeout */
    public ActionTimeoutCondition(AssetID assetID, ActionsWrapper actionsWrapper, String result) {
        this.assetID = assetID;
        this.actionsWrapper = actionsWrapper;
        this.result = result;
    }
    
    public AssetID getAssetID() { return assetID; }

    public String getResult() { return result; }
    
    public static UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof ActionTimeoutCondition ) {
                    return true ;
                }
                return false ;
            }
         };
     
}
