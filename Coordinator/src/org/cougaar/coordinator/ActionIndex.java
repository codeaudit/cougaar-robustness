/*
 * ActionsIndex.java
 *
 * Created on April 13, 2004, 12:41 PM
 */
/*
 * <copyright>
 * 
 *  Copyright 2004 Object Services and Consulting, Inc.
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 *
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * </copyright>
 */

package org.cougaar.coordinator;

/**
 *
 * @author  David Wells - OBJS
 * @version 
 */

import java.util.Hashtable;
import java.util.Collection;

import org.cougaar.coordinator.Action;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.coordinator.housekeeping.IndexKey;
import org.cougaar.core.persist.NotPersistable;
import java.util.Iterator;

public class ActionIndex implements NotPersistable {

    private Hashtable entries = new Hashtable();

    /** Creates new ActionsIndex */
    public ActionIndex() {
    }
    
    protected final static UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {  
                return 
                    (o instanceof ActionIndex);
            }
        };
    
    protected ActionsWrapper indexAction(ActionsWrapper aw, IndexKey key) {
        Action a = aw.getAction();
        Hashtable c = (Hashtable) entries.get(a.getAssetID());
        if (c == null) {
            c = new Hashtable();
            entries.put(a.getAssetID(), c);
            }
        return (ActionsWrapper) c.put(ActionUtils.getActuatorType(a), aw);
    }
    
    protected ActionsWrapper findAction(AssetID assetID, String actuatorType) {
        Collection c = findActionCollection(assetID);
        if (c==null) return null;
	Iterator iter = c.iterator();
	while (iter.hasNext()) {
            ActionsWrapper aw = (ActionsWrapper) iter.next();
            Action a = aw.getAction();
            if (a.getAssetID().equals(assetID)) return aw;
        }
        return null;
    }

    // returns a Collection of ActionWrapper(s)
    protected Collection findActionCollection(AssetID assetID) {
        Hashtable c = (Hashtable) entries.get(assetID);
        if (c != null) return c.values();
        else return null;
    }

    public String toString() {
        return entries.toString();
    }
}
