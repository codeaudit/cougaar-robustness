/*
 * Class.java
 *
 * Created on June 30, 2004, 9:53 AM
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


public class NodeDisconnectActuatorIndex extends Hashtable implements NotPersistable {

    /** Creates new NodeDisconnectActuatorIndex */
    public NodeDisconnectActuatorIndex() {
    }

    protected final static UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {  
                return 
                    (o instanceof NodeDisconnectActuatorIndex);
            }
        };

    protected NodeDisconnectActuator putAction(NodeDisconnectActuator action) {
        return (NodeDisconnectActuator) super.put(action.getAssetID(), action);
    }
    
    protected NodeDisconnectActuator getAction(AssetID assetID) {
        return (NodeDisconnectActuator) super.get(assetID);
    }

}