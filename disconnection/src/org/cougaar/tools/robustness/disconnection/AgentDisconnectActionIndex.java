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


public class AgentDisconnectActionIndex extends Hashtable implements NotPersistable {

    /** Creates new NodeDisconnectActuatorIndex */
    public AgentDisconnectActionIndex() {
    }

    protected final static UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {  
                return 
                    (o instanceof AgentDisconnectActionIndex);
            }
        };

    protected AgentDisconnectAction putAction(AgentDisconnectAction action) {
        return (AgentDisconnectAction) super.put(action.getAssetID(), action);
    }
    
    protected AgentDisconnectAction getAction(AssetID assetID) {
        return (AgentDisconnectAction) super.get(assetID);
    }

}
