/*
 * DefaultAssetTechSpec.java
 *
 * Created on August 28, 2003, 2:48 PM
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
import java.util.Collection;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.Hashtable;

import org.cougaar.core.util.UID;
import org.cougaar.core.persist.NotPersistable;

/**
 * A default implementation of this interface
 *
 * @author Paul Pazandak, OBJS
 */
public class DefaultAssetTechSpec implements AssetTechSpecInterface, NotPersistable {
    
    private static Vector allAssets;
    static { allAssets = new Vector(200,100); }
    
    private String name;
    private String rev;
    private UID uid;
    private AssetType assetType;
    private AssetID assetID;
    private Vector properties;
    private Hashtable superiors;
    private AssetTechSpecInterface host;
    private AssetTechSpecInterface node;
    private static AssetTechSpecInterface network = null; //always null at this pt
    private static AssetTechSpecInterface enclave = null;

    
    /** Creates a new instance of DefaultAssetTechSpec */
    public DefaultAssetTechSpec(AssetTechSpecInterface host, AssetTechSpecInterface node, String assetName, AssetType type, UID uid)  {

        allAssets.add(this);
        
        this.host = host;
        this.node = node;
        this.assetType = type;
        this.name = assetName;
        this.rev = "1.0";
        this.uid = uid;
        this.assetID = new AssetID(assetName, type);
        
        
        properties = new Vector();
        superiors = new Hashtable();
        
    }
    
    
    /**
     * @return the assetStates associated with the asset type (a convenience function)
     */
    public java.util.Vector getAssetStates() { return assetType.getCompositeState() ; }
    
    /**
     * @return the assetType of this asset
     */
    public AssetType getAssetType() {  return AssetType.findAssetType(assetType.getName()); }
    
    /**
     * @return the name of the Asset
     */
    public String getName() { return name;  }
    
    
    /** 
     * @return the DefaultAssetTechSpec for the asset having the given assetID
     */
    public static DefaultAssetTechSpec findAssetByID(AssetID assetID) {
        
        DefaultAssetTechSpec asset = null;
        Iterator iter = allAssets.iterator();
        while (iter.hasNext()) {
            asset = (DefaultAssetTechSpec)iter.next();
            if (asset.getAssetID() == null) {
System.out.println("----------------------------------------------->>>Asset ID is null for Asset = "+asset.getName());
                continue;
            }
            if (asset.getAssetID().equals(assetID)) {
                return asset;
            }
        }
        return null; //not found
    }
            
    
    /**
     *
     * @return the properties of the asset
     */
    public java.util.Vector getProperties() { return properties;}

    
    /**
     *
     * Add a property to the asset. Replaces older property with same name.
     */
    public void addProperty(AssetProperty prop) { 
        
        synchronized(properties) {
            AssetProperty ap;
            for (Iterator i=properties.iterator(); i.hasNext(); ) {
                ap = (AssetProperty)i.next();
                if (ap.getName().equals(prop.getName())) {
                    properties.remove(ap);
                    break; // there will be at most one with the same name.
                }
            }
            properties.addElement(prop);
        }    
    
    }
    
    /**
     * @return the AssetProperty with the given name (case is ignored)
     */
    public AssetProperty findPropertyByName(String name) {
        
        AssetProperty ap;
        Iterator iter = properties.iterator();
        while (iter.hasNext()) {
            ap = (AssetProperty) iter.next();
            if (ap.getName().equalsIgnoreCase(name))
                return ap;
        }
        
        //not found
        return null;
    }
    
    
    /**
     * @return the revision of this interface
     */
    public String getRevision() { return rev;  }
    
    /**
     *@return the UID of this object
     */
    public UID getUID() { return uid; }
    
    /**
     * @return TRUE if the Strings returned by the getName() methods are equal.
     */
    public boolean equals(Object o) {
        return (o != null) && (o instanceof AssetTechSpecInterface) &&
        ((AssetTechSpecInterface)o).getAssetID().equals(this.getAssetID());
    }
    
    /**
     * @return the assetID
     */
    public AssetID getAssetID() { return assetID; }

    
    /**
     * @return a vector of AssetTechSpecInterface which contains the superior assets of this asset
     */
    public void addSuperior(AssetRole role, AssetTechSpecInterface asset) {
        superiors.put(role, asset);
    }
    
    /**
     * @return a vector of AssetTechSpecInterface which contains the superior assets of this asset
     */
    public Collection getSuperiors() { 
        return superiors.values();
    }

    /**
     * @return the AssetTechSpecInterface of the asset which is the superior of this asset in the specified role
     */
    public AssetTechSpecInterface getSuperior(AssetRole role) {
        
        return (AssetTechSpecInterface) superiors.get(role);
    }

    /**
     * @return the AssetTechSpecInterface of the asset which is the host of this asset
     */
    public AssetTechSpecInterface getHost() { return host; }

    /**
     * @return the AssetTechSpecInterface of the asset which is the node of this asset
     */
    public AssetTechSpecInterface getNode() { return node; }
    

    /**
     * @return the AssetTechSpecInterface of the asset which is the network of this asset. ALWAYS NULL at this point.
     */
    public AssetTechSpecInterface getNetwork() { return network; }

    /**
     * @return the AssetTechSpecInterface of the asset which is the enclave of this asset
     */
    public AssetTechSpecInterface getEnclave() { return enclave; }

    /**
     * Sets the enclave using the supplied AssetTechSpecInterface. As there is only one of these for all
     * agents in an enclave, this is a static method.
     */
    public static void setEnclave(AssetTechSpecInterface e) { enclave = e; }
    
    
    /**
     * @return Set the new host & node for this agent
     */
    public void setNewLocation(AssetTechSpecInterface host, AssetTechSpecInterface node) {
        
        this.host = host;
        this.node = node;
    }

    
    /**
     * @return the AssetTechSpecInterfaces of the assets which are the <b>direct</b> subordinates of this asset
     */
    public Vector getSubordinates(AssetRole role) {
        
        Vector found = new Vector();        
        AssetTechSpecInterface asset;
        
        Iterator iter = allAssets.iterator();
        while ( iter.hasNext() ) { 
            asset = (AssetTechSpecInterface)iter.next();
            if (asset.getSuperior(role).equals(this)) {
                found.addElement(asset);
            }
        }
    
        return found;
    }
    
    /**
     * @return a vector of AssetRoles of the superior
     */
    public Vector getRolesOfSuperior(AssetTechSpecInterface superior) {
        
        Vector roles = new Vector();
        AssetRole nextRole;
        Enumeration allRoles = superiors.keys();
        while (allRoles.hasMoreElements()) {
            nextRole = (AssetRole) allRoles.nextElement();
            if ( superior.equals(superiors.get(nextRole) ) )
                roles.add(nextRole);
        }
        return roles;
    }        

    
    /**
     * @return the AssetTechSpecInterfaces of the assets which are the <b>direct</b> subordinates of this asset
     */
    public Vector getAgentsInHost(AssetTechSpecInterface host) {
        
        Vector found = new Vector();        
        AssetTechSpecInterface asset;
        
        Iterator iter = allAssets.iterator();
        while ( iter.hasNext() ) { 
            asset = (AssetTechSpecInterface)iter.next();
            if (this.getHost().equals(host)) {
                found.addElement(asset);
            }
        }
    
        return found;
    }

    /**
     * @return the AssetTechSpecInterfaces of the ALL agent assets which have one of the specified hosts
     */
    public static Vector getAgentsInHosts(Vector hostsV) {
        
        Vector found = new Vector();        
        AssetTechSpecInterface asset;
        AssetTechSpecInterface host;
        
        if (hostsV == null || hostsV.size() == 0) {
            return found;
        }
        
        Iterator hosts = hostsV.iterator();
        while ( hosts.hasNext() ) { 
            host = (AssetTechSpecInterface)hosts.next();
        
            Iterator assets = allAssets.iterator();
            while ( assets.hasNext() ) { 
                asset = (AssetTechSpecInterface)assets.next();
                if (asset.getHost().equals(host)) {
                    found.addElement(asset);
                }
            }

        }
        return found;
    }
    
    /**
     * @return the AssetTechSpecInterfaces of the ALL agent assets which have one of the specified nodes
     */
    public static Vector getAgentsInNodes(Vector nodesV) {
        
        Vector found = new Vector();        
        AssetTechSpecInterface asset;
        AssetTechSpecInterface node;
        
        if (nodesV == null || nodesV.size() == 0) {
            return found;
        }
        
        Iterator nodes = nodesV.iterator();
        while ( nodes.hasNext() ) { 
            node = (AssetTechSpecInterface)nodes.next();
        
            Iterator assets = allAssets.iterator();
            while ( assets.hasNext() ) { 
                asset = (AssetTechSpecInterface)assets.next();
                if (asset.getNode().equals(node)) {
                    found.addElement(asset);
                }
            }

        }
        return found;
    }

    /**
     * @return the AssetTechSpecInterfaces of the ALL node assets which have one of the specified hosts
     */
    public static Vector getNodesInHosts(Vector hostsV) {
        
        Vector found = new Vector();        
        AssetTechSpecInterface asset;
        AssetTechSpecInterface host;
        
        if (hostsV == null || hostsV.size() == 0) {
            return found;
        }
        
        Iterator hosts = hostsV.iterator();
        while ( hosts.hasNext() ) { 
            host = (AssetTechSpecInterface)hosts.next();
        
            Iterator assets = allAssets.iterator();
            while ( assets.hasNext() ) { 
                asset = (AssetTechSpecInterface)assets.next();
                if (asset.getNode().equals(host)) {
                    found.addElement(asset);
                }
            }

        }
        return found;
    }
    
}
