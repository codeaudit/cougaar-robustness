/*
 * AssetTechSpecInterface.java
 *
 * Created on August 5, 2003, 2:57 PM
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
import java.util.Collection;


/**
 * This interface is primarily for external consumers to code to.
 * As such it focuses on the API that is expected such a consumer will
 * require. A more complex API will be found in the implementation,
 * providing much greater functionality -- but primarily oriented toward
 * intra-package use.
 *
 * @author Paul Pazandak, Ph.D. OBJS, Inc.
 */
public interface AssetTechSpecInterface extends TechSpecRootInterface {

    /**
     *@return the vector of asset states associated with this asset tech spec
     *
     */
    public Vector getAssetStates();
    
    /**
     * @return the asset type of the asset described by this tech spec
     */
    public AssetType getAssetType();
    
    /**
     *@return the vector of asset properties of the asset described by this tech spec
     */
    public Vector getProperties();
    
    /**
     * @return expanded name - "type:name"
     */
    public String getExpandedName();
    
    /**
     * @return Set the new host & node for this agent
     */
    public void setNewLocation(AssetTechSpecInterface hsot, AssetTechSpecInterface node);
    
    /**
     * @return the AssetProperty with the given name (case is ignored)
     */
    public AssetProperty findPropertyByName(String name);

    
    /**
     * @return a vector of AssetTechSpecInterface which contains the superior assets of this asset
     */
    public Collection getSuperiors();

    /**
     * @return the AssetTechSpecInterface of the asset which is the superior of this asset in the specified role
     */
    public AssetTechSpecInterface getSuperior(AssetRole role);
    
    /**
     * @return the AssetTechSpecInterface of the asset which is the host of this asset
     */
    public AssetTechSpecInterface getHost();

    /**
     * @return the AssetTechSpecInterface of the asset which is the node of this asset
     */
    public AssetTechSpecInterface getNode();

    /**
     * @return the AssetTechSpecInterface of the asset which is the network of this asset
     */
    public AssetTechSpecInterface getNetwork();

    /**
     * @return the AssetTechSpecInterface of the asset which is the enclave of this asset
     */
    public AssetTechSpecInterface getEnclave();        
    
    /**
     * @return the AssetTechSpecInterface of the asset which are the subordinates of this asset
     */
    public Vector getSubordinates(AssetRole role);
    
    /**
     * @return the AssetRoles of the superior
     */
    public Vector getRolesOfSuperior(AssetTechSpecInterface superior);
}
