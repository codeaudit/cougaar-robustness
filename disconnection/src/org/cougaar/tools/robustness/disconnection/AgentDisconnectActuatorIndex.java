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
import org.cougaar.core.persist.NotPersistable;


public class AgentDisconnectActuatorIndex extends Hashtable implements NotPersistable {

    /** Creates new NodeDisconnectActuatorIndex */
    public AgentDisconnectActuatorIndex() {
    }

    protected final static UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {  
                return 
                    (o instanceof AgentDisconnectActuatorIndex);
            }
        };

    protected AgentDisconnectActuator putAction(AgentDisconnectActuator action) {
        return (AgentDisconnectActuator) super.put(action.getAssetID(), action);
    }
    
    protected AgentDisconnectActuator getAction(AssetID assetID) {
        return (AgentDisconnectActuator) super.get(assetID);
    }

}
