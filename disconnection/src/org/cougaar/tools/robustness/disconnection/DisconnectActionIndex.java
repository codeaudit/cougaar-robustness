/*
 * Class.java
 *
 * Created on July 14, 2004, 9:52 AM
 */

package org.cougaar.tools.robustness.disconnection;

/**
 *
 * @author  administrator
 * @version 
 */
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

public class DisconnectActionIndex extends Hashtable implements Serializable {

    /** Creates new NodeDisconnectActuatorIndex */
    public DisconnectActionIndex() {
    }
    
    protected final static UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {  
                return 
                    (o instanceof DisconnectActionIndex);
            }
        };

    protected DisconnectAction putAction(DisconnectAction action) {
        return (DisconnectAction) super.put(action.getAssetID(), action);
    }
    
    protected DisconnectAction getAction(AssetID assetID) {
        return (DisconnectAction) super.get(assetID);
    }

}
