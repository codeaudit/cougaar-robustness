/*
 * AssetType.java
 *
 * Created on August 5, 2003, 3:01 PM
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
import java.io.Serializable;

/**
 * @author Paul Pazandak, Ph.D. OBJS, Inc.
 */
public class AssetType implements NotPersistable, Serializable {

    private static Vector types = new Vector(5,5);

    //public static final AssetType NETWORK = new AssetType("network", null); //no separate network that would differentiate from enclave in 2004
    public static final AssetType ENCLAVE = new AssetType("enclave", null);
    public static final AssetType HOST = new AssetType("host", ENCLAVE);
    public static final AssetType NODE = new AssetType("node", HOST);
    public static final AssetType AGENT = new AssetType("agent", NODE);

    public static final AssetType VIRTUAL = new AssetType("VirtualAsset", null);        
    
    private static void addType(AssetType at) {
        if (types == null) {
            types = new Vector(5,5);
        }
        types.add(at);
    }
    
    public Vector getAllTypes() { return types; }
    
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
    private AssetType(String name, AssetType superType) {
        this.name = name;
        states = new Vector();
        this.superType = superType;
        types.add(this);
    }

    /** 
     * Creates a new instance of AssetType 
     *@param asset type name
     *@param vector of AssetStateDimension
     */
//    private AssetType(String name, Vector assetStates) {
//        this.name = name;
//        this.states = assetStates;
//        if (this.states == null) {
//            states = new Vector();            
//        }
//    }
    
    /**
     * Create a subtype AssetType. Will be added if it doesn't already exist.
     */
    public static void addAssetSubtype(AssetType superType, String newType) throws DupWithDifferentSuperTypeException {
        
        AssetType old = AssetType.findAssetType(newType);
        if (old == null) {
            AssetType at = new AssetType(newType, superType);
            //at.superType = superType;
            //types.add(at);        
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
     *@return the state dimensions that this asset type can be in
     *
     */
    public Vector getCompositeState() { 
    
        return states; 
    
    }
 
    
     /**
      * Add an asset state to the AssetStateDimension vector. Only works if it isn't already there.
      *@return true if added.
      */
     public boolean addStateDimension(AssetStateDimension as) {
         if (this.findStateDimension(as.getStateName()) == null) {
             states.addElement(as);
             return true;
         }
         return false;
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
      * @return TRUE if this asset type is a direct or indirect subtype of the provided type
      */
     public boolean isSupertypeOf(AssetType at) {
      
         AssetType superT = at.getSuperType();
         if (superT == null) { return false; } //type has no super type
         if (this.equals(superT)) { return true; } //direct superType
         return isSupertypeOf(at.getSuperType()); //recursively check parent types
         
     }
     
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
