/*
 * AssetStateDimension.java
 *
 * Created on August 5, 2003, 3:10 PM
 * <copyright>
 *  Copyright 2003 Object Services and Consulting, Inc.
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

package org.cougaar.coordinator.techspec;

import java.util.Vector;
import java.util.Iterator;
import org.cougaar.core.persist.NotPersistable;

/**
 * This class defines one of many possible states that an asset TYPE may have.
 * A state can have one of several possible states.
 *
 * @author Paul Pazandak, Ph.D. OBJS, Inc.
 */
public class AssetStateDimension implements NotPersistable {

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
     *@return the name of this asset state
     */
    public String getStateName() { return name; }
    
    /**
     *@return the asset type this state is associated with
     */
    public AssetType getAssetType() { return assetType; }
    
    /**
     *@return TRUE if assetType and asset name are equal
     */
    public boolean equals(AssetStateDimension as) {
        
        return (as.assetType.getName().equalsIgnoreCase(this.assetType.getName()) && as.name.equalsIgnoreCase(this.name) );
    }
    
    /**
     * @return the possible states that this state could have
     */
    public Vector getpossibleStates() { return possibleStates; }
    
    /**
     * @return the default state for the state
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
    public void removeState(AssetState state) {  possibleStates.removeElement(state); }

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
         if (this.getpossibleStates() == null) { 
             out = out + "\n    --- No State Dimensions";
         }
         for (Iterator i = this.getpossibleStates().iterator(); i.hasNext(); ) {
              AssetState as = (AssetState)i.next();
              out = out + "\n      State["+as.getName()+"] mauSecurity="+as.getRelativeMauSecurity()+"  mauCompleteness="+as.getRelativeMauCompleteness();
         }
         return out;
     }
}
