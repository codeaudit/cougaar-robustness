/*
 * SelectionCompleted.java
 *
 * Created on October 27, 2004
 *
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


package org.cougaar.coordinator.selection;

/**
 *
 * @author  David Wells - OBJS
 * @version 
 */

import org.cougaar.util.UnaryPredicate;
import org.cougaar.coordinator.techspec.AssetID;
import org.cougaar.core.persist.NotPersistable;

public class SelectionCompleted implements NotPersistable {

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
