/*
 * DamageDistribution.java
 *
 * Created on August 5, 2003, 3:42 PM
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

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;
import org.cougaar.core.persist.NotPersistable;

/**
 *
 * @author Paul Pazandak, Ph.D. OBJS, Inc.
 */
public class DamageDistribution implements NotPersistable {

    /** Used to sequence oids for these objects */
    private static int G_OID = 0;
    
    /** Used by the servlet to identify these objects */
    private int oid;

    /** asset state */
    private AssetStateDimension state;
 
    /** start state */
    private AssetState startState;
    
    /** start state */
    private AssetState endState;

    /** probability */
    private double probability;
       
    /** Logger for error msgs */
    private Logger logger;
    
    
    /** Creates a new instance of DamageDistribution with a single probability for all transitions.
     *
     */
    public DamageDistribution(AssetStateDimension state, AssetState startState, AssetState endState, double probability ) {
        
        this.state = state;
        this.startState = startState;
        this.endState = endState;
        this.probability = probability;
        logger = Logging.getLogger(this.getClass().getName());
        
        this.oid = G_OID++;
    }
    
    
    /** Set the oid of this object */
    private void setOID(int oid) { this.oid = oid; }
    
    /** Set the oid of this object */
    public int getOID() { return this.oid; }
    
    /** Method to clone this object */
    protected Object clone() throws CloneNotSupportedException {
     
        DamageDistribution dd = new DamageDistribution(state, startState, endState, probability);
        dd.setOID(this.oid);
        return dd;
    }
    
   
    /**
     *@return the asset state
     */
    public AssetStateDimension getAssetState() { return state; }
    
    /**
     *@return the probability for this asset & state & transition from startValue to endValue
     */
    public double getProbability() {
        
        return probability;
    }
    
    /**
     * Set the Probability
     */
    public void setProbability(double p) {

        probability = p;
    }
    
    /**
     * @return the start State Value
     */
    public AssetState getStartState() {
        
        return startState;
    }

    /**
     * @return the end State Value
     */
    public AssetState getEndState() {
        
        return endState;
    }
    
    public String toString() {

        return "AssetStateDimension="+state.getStateName()+"  --- startState="+startState.getName()+"   endState="+endState.getName() + "   prob="+probability;
    }    
}
