/*
 * SelectionCompleted.java
 *
 * Created on October 27, 2004
 */

package org.cougaar.coordinator.selection;

/**
 *
 * @author  David Wells - OBJS
 * @version 
 */

import org.cougaar.util.UnaryPredicate;
import org.cougaar.coordinator.techspec.AssetID;

public class SelectionCompleted {

    private AssetID assetID;
    private boolean successful;

    /** Creates new FailedAction */
    public SelectionCompleted(AssetID assetID, boolean successful) {
        this.assetID = assetID;
	  this.successful = successful;
    }

    public AssetID getAssetID() { return assetID; }
    public boolean successfulP() { return successful; }

    public String toString() {
       return "SelectionCompleted: " + getAssetID() + " : " + successfulP();
    }
    

    public final static UnaryPredicate pred = new UnaryPredicate() {
        public boolean execute(Object o) {  
            return 
                (o instanceof SelectionCompleted);
        }
    };

}
