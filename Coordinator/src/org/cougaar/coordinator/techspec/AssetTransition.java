/*
 * AssetTransition.java
 *
 * Created on September 22, 2003, 3:53 PM
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

package org.cougaar.coordinator.techspec;

import java.util.Vector;
import java.util.Iterator;
import org.cougaar.core.persist.NotPersistable;
 
/**
 * This class defines the possible transitions that an Asset make take from one AssetState to another
 * in a given AssetStateDimension.
 *
 * Reused/reengineered for 2004
 *
 * @author Paul Pazandak, Ph.D. OBJS, Inc.
 */
public class AssetTransition implements NotPersistable {
    
    protected AssetState start = null;
    protected AssetState end = null;
    protected AssetStateDimension stateDim = null;
    
    protected AssetType assetType;
    
    /** Creates a new instance of AssetTransition. It takes Strings as the AssetStates may not have been loaded 
     *  into the system yet -- lazy evaluation is used
     */
    public AssetTransition(AssetType assetType, AssetStateDimension stateDim, AssetState start, AssetState end) {
        
        this.start = start;
        this.end = end;
        this.assetType = assetType;
        this.stateDim = stateDim;
    }

    /** @return the starting state value */
    public AssetState getStartValue() { return start; } 
    
    /** @return the ending state value */
    public AssetState getEndValue() { return end; }
    
    /** Return applicable asset type */
    public AssetType getAssetType() { return assetType; }
    
    /** Return applicable AssetStateDimension */
    public AssetStateDimension getAssetStateDimension() { return stateDim; }
    
    public String toString() { return "AssetTransition: Start="+start+", End="+end; }
    
}
