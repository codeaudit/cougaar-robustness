/*
 * AssetType.java
 *
 * Created on August 5, 2003, 3:01 PM
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
import java.io.Serializable;

/**
 * @author Paul Pazandak, Ph.D. OBJS, Inc.
 */
public class AssetType implements NotPersistable, Serializable {

    public static final AssetType NETWORK = new AssetType("network", null);
    public static final AssetType ENCLAVE = new AssetType("enclave", null);
    public static final AssetType HOST = new AssetType("host", null);
    public static final AssetType NODE = new AssetType("node", null);
    public static final AssetType AGENT = new AssetType("agent", null);

    public static final AssetType VIRTUAL = new AssetType("VirtualAsset", null);
    
    
    private static Vector types;
    static {
        types = new Vector();
        types.add(HOST);
        types.add(NODE);
        types.add(AGENT);
    }
    
    /**
     * @return The Asset Type whose name matches the string provided, ignoring case. If no match found, returns NULL.
     */
    public static AssetType findAssetType(String name) {
        
        AssetType found = null;
        Iterator iter = types.iterator();
        while (iter.hasNext()) {
            AssetType at = (AssetType)iter.next();
            if (at.getName().equalsIgnoreCase(name)) {
                found = at;
                break;
            }
        }
        //System.out.println("*****************************name = "+ name);
        //System.out.println("*****************************Asset type = "+ found);
        return found;
    }
    
    
    /** the name of this asset type */
    private String name;
    
    /** the asset states (ala AssetStateDimension) that this type can be in */
    private Vector states;
    
    /** the superType of the asset type */
    private AssetType superType = null;
    
    /** The societal utility of this asset type. <b>Defaults to 0</b> */
    private int societalUtility = 0;
    
    /** Creates a new instance of AssetType 
     *@param asset type name
     */
    private AssetType(String name) {
        this.name = name;
        states = new Vector();
    }

    /** 
     * Creates a new instance of AssetType 
     *@param asset type name
     *@param vector of AssetStateDimension
     */
    private AssetType(String name, Vector assetStates) {
        this.name = name;
        this.states = assetStates;
        if (this.states == null) {
            states = new Vector();            
        }
    }
    
    /**
     * Create a subtype AssetType. Will be added if it doesn't already exist.
     */
    public static void addAssetSubtype(AssetType superType, String newType) throws DupWithDifferentSuperTypeException {
        
        AssetType old = AssetType.findAssetType(newType);
        if (old == null) {
            AssetType at = new AssetType(newType);
            at.superType = superType;
            types.add(at);        
        } else { // check to make sure existing  & the one being requested have the same supertype         
            if (!old.superType.equals(superType)) {
                throw new DupWithDifferentSuperTypeException("new type="+newType+". First supertype = "+old.superType.name + "  new supertype = " + superType);
            }
        }
    }

    
    /**
     *@return the name of this asset type
     *
     */
    public String getName() { return name; }

    /**
     *@return the name of this asset type
     *
     */
    public String toString() { return name; }
    
    /**
     *@return the states that this asset type can be in
     *
     */
    public Vector getCompositeState() { 
    
        return states; 
    
    }
 
    
     /**
      * Add an asset state to the AssetStateDimension vector
      */
     public void addStateDimension(AssetStateDimension as) {
         states.addElement(as);
     }
     
     /**
      * Remove an asset state from the AssetStateDimension vector
      */
     public void removeStateDimension(AssetStateDimension as) {
         states.removeElement(as);
     }

     /**
      * May be called while threat models are loaded. This allows the creation of 
      * the threat models & reference to AssetStateDimensions.
      *
      * @return An AssetStateDimension with the given name
      */
     public AssetStateDimension findStateDimension(String name) {
         
         AssetStateDimension state = null;
         Iterator i = states.iterator();
         while (i.hasNext()) {
            state = (AssetStateDimension)i.next();
            if (state.getStateName().equalsIgnoreCase(name)) {
                return state;
            }
         }
         
         return null;
         //state = new AssetStateDimension( this, name );
         //states.addElement(state);
         //return state;
     }

     /**
      * @return the superType of this asset type
      */
     public AssetType getSuperType() { return superType; }
     
     /**
      * @return TRUE if this asset type has a superType 
      */
     public boolean hasSuperType() { return superType != null; }

     
     /** Equality based upon name */
     public boolean equals(Object o) {
         
         return ( (o instanceof AssetType) &&
                ( (AssetType)o).getName().equalsIgnoreCase(this.getName()) );
     }

     /**
      * @return the societal utility of the asset type
      */
     public int getUtilityValue() { return societalUtility; }

     /**
      * Set the societal utility of the asset type
      */
     public void setUtilityValue(int value) { societalUtility = value; }
     

}
