/*
 * ActionPatience.java
 *
 * Created on September 18, 2003, 2:06 PM
 *
 * <copyright>
 * 
 *  Copyright 2003-2004 Object Services and Consulting, Inc.
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


package org.cougaar.coordinator.selection;

import org.cougaar.core.util.UID;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.util.UnaryPredicate;
import java.util.Collection;
import java.util.Iterator;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.coordinator.Action;
import org.cougaar.coordinator.techspec.AssetID;

/**
 *
 * @author  David Wells - OBJS
 * @version 
 */
public class ActionPatience implements NotPersistable {

    AssetID assetID;
    Action action;
    boolean expired = false;
    long timeoutTime;

    /** Creates new ActionPatience */
    public ActionPatience(Action action, AssetID assetID, long timeoutTime) {
        this.action = action;
        this.assetID = assetID;
        this.timeoutTime = timeoutTime;
    }

    public AssetID getAssetID() { return assetID; }
    public Action getAction() { return action; }

    public long getDuration() { return timeoutTime; }

    public boolean expired() {
        return expired;
    }
    
    public static final UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {  
                return 
                    (o instanceof ActionPatience);
            }
        };

    /*
    public static ActionPatience find(String defenseName, String expandedName, BlackboardService blackboard) {

        ActionPatience dc = null;
        Collection c = blackboard.query(pred);
        Iterator iter = c.iterator();
        //if (logger.isDebugEnabled()) logger.debug(new Integer(c.size()).toString());
        while (iter.hasNext()) {
           dc = (ActionPatience)iter.next();
           if (dc.compareSignature(expandedName, defenseName)) {
               return dc;
           }
        }
        return null;
    }          

    public static ActionPatience find(UID uid, BlackboardService blackboard) {

        ActionPatience dc = null;
        Collection c = blackboard.query(pred);
        Iterator iter = c.iterator();
        //if (logger.isDebugEnabled()) logger.debug(new Integer(c.size()).toString());
        while (iter.hasNext()) {
           dc = (ActionPatience)iter.next();
           if (dc.compareSignature(uid)) {
               return dc;
           }
        }
        return null;
    }  
*/

}
