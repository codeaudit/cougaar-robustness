/*
 * AssetTechSpecInterface.java
 *
 * Created on August 5, 2003, 2:57 PM
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
    public AssetID getAssetID();
    
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
//sjf    public Vector getSubordinates(AssetRole role);
    
    /**
     * @return the AssetRoles of the superior
     */
    public Vector getRolesOfSuperior(AssetTechSpecInterface superior);
}
