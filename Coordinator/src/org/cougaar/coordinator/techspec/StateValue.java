/*
 * StateValue.java
 *
 * Created on September 8, 2003, 1:22 PM
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
import org.cougaar.core.persist.NotPersistable;

/**
 * @author Paul Pazandak, Ph.D. OBJS, Inc.
 * @deprecated It has been replaced with AssetState
 */
public class StateValue implements NotPersistable {
    
    
    private String name;
    private int utility;
    
    /** Creates a new instance of StateValue */
    public StateValue(String name, int utility) {
        this.name = name;
        this.utility = utility;
    }
    
    /** Return the string value of this state */
    public String getName() { return name; }
    
    /** @return the utility value of being in this state, or phrased more otherwise,
     * the utility value of having the value getName() for the associated AssetStateDescriptor
     */
    public int getUtility() { return utility; }
    
    /** @return TRUE if the value returned by getName() of each obhect matches */
    public boolean equals(Object o) {     
        return ( (o instanceof StateValue) && (o != null) && ( getName().equals(((StateValue)o).getName()) ) );
    }
}
