/*
 * ActionsIndex.java
 *
 * Created on April 13, 2004, 12:41 PM
 */
/*
 * <copyright>
 *  Copyright 2004 Object Services and Consulting, Inc.
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA)
 *  and the Defense Logistics Agency (DLA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
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
