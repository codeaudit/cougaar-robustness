/*
 * AssetStateDescriptor.java
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
 * A state can have one of several possible values.
 *
 * @author Paul Pazandak, Ph.D. OBJS, Inc.
 */
public class AssetStateDescriptor implements NotPersistable {

    /** the associated asset type */
    private AssetType assetType;
    
    /** asset state name */
    private String name;
    
    /** possible asset state values */
    private Vector possibleValues;
    
    /** asset state values */
    private StateValue defaultValue;

    /** Possible state transitions */
    AssetTransition transitions[] = null;

       
    /** Creates a new instance of AssetStateDescriptor 
     *@param the asset type associated with this state
     *@param name  the name of the asset state
     *
     */
    public AssetStateDescriptor(AssetType assetType, String name) {
    
        this.assetType = assetType;
        this.name = name;
        possibleValues = new Vector();
        defaultValue = null;
        transitions = new AssetTransition[0];
    }

    
    /** Creates a new instance of AssetStateDescriptor 
     *@param the asset type associated with this state
     *@param name  the name of the asset state
     *@param values the possible state values this state could have
     *@param defaultState the default value this state will have
     */
    public AssetStateDescriptor(AssetType assetType, String name, Vector values, StateValue defaultValue) {
    
        this.assetType = assetType;
        this.name = name;
        this.possibleValues = values;
        this.defaultValue = defaultValue;
    
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
    public boolean equals(AssetStateDescriptor as) {
        
        return (as.assetType.getName().equalsIgnoreCase(this.assetType.getName()) && as.name.equalsIgnoreCase(this.name) );
    }
    
    /**
     * @return the possible values that this state could have
     */
    public Vector getPossibleValues() { return possibleValues; }
    
    /**
     * @return the default value for the state
     */
    public StateValue getDefaultValue() { return defaultValue; }
    
    /**
     * Add a possible StateValue
     * @param a StateValue to add
     */
    public void addValue(StateValue value) {  possibleValues.addElement(value); }
    
    /**
     * Remove a possible StateValue
     * @param a StateValue to remove
     */
    public void removeValue(StateValue value) {  possibleValues.removeElement(value); }

    /**
     * Set default StateValue
     * @param a default StateValue 
     */
    public void setDefaultValue(StateValue value) {  defaultValue = value; }
    
     /**
      * May be called while threat models are loaded. This allows the creation of 
      * the threat models & reference to StateValues even before they have been
      * loaded by the AssetMgrPlugin... if the timing is off.
      *
      * @return An StateValue with the given name, creating a new one if nec.
      * with a utility of ZERO
      */
     public StateValue findStateValue(String name) {
         
         StateValue value = null;
         Iterator i = possibleValues.iterator();
         while (i.hasNext()) {
            value = (StateValue)i.next();
            if (value.getName().equalsIgnoreCase(name)) {
                return value;
            }
         }
         
         value = new StateValue(name, 0);
         possibleValues.addElement(value);
         return value;
     }
    
     /**
      * @return possible AssetTransitions
      */
     public AssetTransition[] getPossibleTransitions() { return transitions; }

     /**
      * Set possible AssetTransitions
      */
     public void setPossibleTransitions(AssetTransition[] transitions) { this.transitions = transitions; }

     
     /**
      * Add a AssetTransition
      */
     public void addTransition(AssetTransition transition) { 
     
            AssetTransition[] t = new AssetTransition[transitions.length+1];
            for (int i=0; i<transitions.length; i++) {
                t[i] = transitions[i];
            }
            t[t.length-1] = transition;
            transitions = t;
     }
   
     /** Equality operator, based upon name & asset type */
     public boolean equals (Object o) {
         return ( (o instanceof AssetStateDescriptor) &&
              ( (AssetStateDescriptor)o).getAssetType().equals(this.getAssetType()) &&
              ( (AssetStateDescriptor)o).getStateName().equalsIgnoreCase(this.getStateName()) );
     }
}
