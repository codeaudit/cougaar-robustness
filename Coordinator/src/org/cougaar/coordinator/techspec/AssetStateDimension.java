/*
 * AssetStateDimension.java
 *
 * Created on August 5, 2003, 3:10 PM
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
import java.io.Serializable;

import org.cougaar.core.persist.NotPersistable;


/**
 * This class defines one of many possible states that an asset TYPE may have.
 * A state dimension can have one of several possible states, E.g. 
 * AssetStateDimension = Communicating, states = OK, NotCommunicating
 *
 * @author Paul Pazandak, Ph.D. OBJS, Inc.
 */
public class AssetStateDimension implements NotPersistable, Serializable {

    /** the associated asset type */
    private AssetType assetType;
    
    /** asset state name */
    private String name;
    
    /** possible asset state states */
    private Vector possibleStates;
    
    /** asset state states */
    private AssetState defaultState;

    /** Possible state transitions */
//3    AssetTransition transitions[] = null;

       
    /** Creates a new instance of AssetStateDimension 
     *@param the asset type associated with this state
     *@param name  the name of the asset state
     *
     */
    public AssetStateDimension(AssetType assetType, String name) {
    
        this.assetType = assetType;
        this.name = name;
        possibleStates = new Vector();
        defaultState = null;
//3        transitions = new AssetTransition[0];
    }

    
    /** Creates a new instance of AssetStateDimension 
     *@param the asset type associated with this state
     *@param name  the name of the asset state
     *@param states the possible state states this state could have
     *@param defaultState the default state this state will have
     */
    public AssetStateDimension(AssetType assetType, String name, Vector states, AssetState defaultState) {
    
        this.assetType = assetType;
        this.name = name;
        this.possibleStates = states;
        this.defaultState = defaultState;
    
    }
    
    /**
     *@return the name of this asset state dimension
     */
    public String getStateName() { return name; }
    
    /**
     *@return the asset type this state dimension is associated with
     */
    public AssetType getAssetType() { return assetType; }
    
    /**
     *@return TRUE if assetType and asset name are equal
     */
    public boolean equals(AssetStateDimension as) {
        
        return ( (as != null) && as.assetType.getName().equalsIgnoreCase(this.assetType.getName()) && as.name.equalsIgnoreCase(this.name) );
    }
    
    /**
     * @return the possible AssetState that this state dimension could have
     */
    public Vector getPossibleStates() { return possibleStates; }
    
    /**
     * @return the default state for the state dimension
     */
    public AssetState getDefaultState() { return defaultState; }
    
    /**
     * Add a possible AssetState
     * @param a AssetState to add
     */
    public void addState(AssetState state) {  possibleStates.addElement(state); }
    
    /**
     * Remove a possible AssetState
     * @param a AssetState to remove
     */
    //public void removeState(AssetState state) {  possibleStates.removeElement(state); }

    /**
      * Converts the name of the state into an index number
      * @param state_name the name of the state
      * @return the index for the state, -1 if not found
      **/
    public int StateNameToStateIndex ( String state_name ) {
        
        AssetState as;
        for (int i = 0; i < possibleStates.size(); i++ ) {
            as = (AssetState) possibleStates.get(i);
            if ( (as != null) && as.getName().equals(state_name)) {
                return i;
            }
        }
        return -1;
    }

    /**
      * Converts the index number of the state into a name
      * @param the index for the state
      * @return state_name the name of the state, or NULL if state_index is out of bounds.
      **/
    public String StateIndexToStateName( int state_index ) {

        if (state_index >= possibleStates.size() ) return null;
        AssetState as = (AssetState) possibleStates.get(state_index);
        return (as != null ? as.getName() : null );
    }

    /**
      * Converts the index number of the state into a name
      * @param the index for the state
      * @return state_name the name of the state
      **/
    public int getNumStates( ) {
        return possibleStates.size();
    }
    
    /**
     * Set default AssetState
     * @param a default AssetState 
     */
    public void setDefaultState(AssetState state) {  defaultState = state; }
    
     /**
      * May be called while threat models are loaded. This allows the creation of 
      * the threat models & reference to AssetStates even before they have been
      * loaded by the AssetMgrPlugin... if the timing is off.
      *
      * @return An AssetState with the given name, creating a new one if nec.
      * with a utility of ZERO
      */
     public AssetState findAssetState(String name) {
         
         AssetState state = null;
         Iterator i = possibleStates.iterator();
         while (i.hasNext()) {
            state = (AssetState)i.next();
            if (state.getName().equalsIgnoreCase(name)) {
                return state;
            }
         }
   
//4      //Only import states from XML (for now), if not found return null
         return null; 
            
//3         state = new AssetState(name, 0);
//3         possibleStates.addElement(state);
//3         return state;
     }
    
     /**
      * @return possible AssetTransitions
      */
//3     public AssetTransition[] getPossibleTransitions() { return transitions; }

     /**
      * Set possible AssetTransitions
      */
//3     public void setPossibleTransitions(AssetTransition[] transitions) { this.transitions = transitions; }

     
     /**
      * Add a AssetTransition
      */
/*3     public void addTransition(AssetTransition transition) { 
     
            AssetTransition[] t = new AssetTransition[transitions.length+1];
            for (int i=0; i<transitions.length; i++) {
                t[i] = transitions[i];
            }
            t[t.length-1] = transition;
            transitions = t;
     }
 */  
     /** Equality operator, based upon name & asset type */
     public boolean equals (Object o) {
         return ( (o instanceof AssetStateDimension) &&
              ( (AssetStateDimension)o).getAssetType().equals(this.getAssetType()) &&
              ( (AssetStateDimension)o).getStateName().equalsIgnoreCase(this.getStateName()) );
     }
     
     public String toString() {
               
         String out = "\n   AssetStateDimension [" + this.name + "] AssetType = "+ this.assetType;
         if (this.getPossibleStates() == null) { 
             out = out + "\n    --- No State Dimensions";
         }
         for (Iterator i = this.getPossibleStates().iterator(); i.hasNext(); ) {
              AssetState as = (AssetState)i.next();
              out = out + "\n      State["+as.getName()+"] mauSecurity="+as.getRelativeMauSecurity()+"  mauCompleteness="+as.getRelativeMauCompleteness();
         }
         return out;
     }
}
