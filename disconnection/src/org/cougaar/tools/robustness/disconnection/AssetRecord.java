/*
 * AssetRecord.java
 *
 * Created on July 25, 2004, 11:17 PM
 */

package org.cougaar.tools.robustness.disconnection;

/**
 *
 * @author  David Wells
 * @version 
 */

import org.cougaar.coordinator.techspec.AssetID;
import java.io.Serializable;
import java.io.Serializable;
import org.cougaar.core.persist.Persistable;

public class AssetRecord implements Persistable, Serializable {

    /** Creates new AssetRecord */
    public AssetRecord(AssetID assetID) {
        this.assetID=assetID;
    }

    public boolean isPersistable() { return true; }
    
    private AssetID assetID;
    private long timeout = 0L;

    public AssetID getAssetID() { return assetID; }

    public void setTimeout(long timeout) { this.timeout = timeout; }
    public long getTimeout() { return timeout; }
    
}
