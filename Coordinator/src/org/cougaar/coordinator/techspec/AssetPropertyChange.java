/*
 * AssetPropertyChange.java
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


/**
 * This class describes the interface for all AssetPropertyChange instances. These
 * objects are published to the BB when one or more property values associated with 
 * an asset change. Subclasses corresponding to each asset type are used.
 *
 * This class & its subtypes are NOT is use at this time. Once we start to support
 * the use of agent properties then these classes will be needed.
 *
 * @author Paul Pazandak, Ph.D. OBJS, Inc.
 */
public abstract class AssetPropertyChange  {
    
    /** the changes that are being declared */
    private AssetProperty[] changes;
    
    /** the assetID of the asset */
    private AssetID  assetID;
    
    /** 
     * Creates a new instance of AssetPropertyChange 
     */
    public AssetPropertyChange(String assetName, AssetType type) {
        
        changes = new AssetProperty[0];

        assetID = new AssetID(assetName, type);
    }

    /** 
     * Creates a new instance of AssetPropertyChange 
     */
    public AssetPropertyChange(String assetName, AssetType type, AssetProperty property ) {
        
        changes = new AssetProperty[1];
        changes[0] = property;

        assetID = new AssetID(assetName, type);
    }
    
    /** 
     * Creates a new instance of AssetPropertyChange, with an array of changes
     */
    public AssetPropertyChange(String assetName, AssetType type, AssetProperty[] properties ) {
        changes = properties;

        assetID = new AssetID(assetName, type);
    }        

    /**
     * @return the assetID for the asset that this change affects
     */
    public AssetID getAssetID() { return assetID; }
    
    /** 
     * @return the property changes
     */
    public AssetProperty[] getPropertyChanges() { return changes; }
    
    /** 
     * Adds a property change
     */
    public void addPropertyChange(AssetProperty change) { 
    
        int len = changes.length;
        changes = new AssetProperty[len + 1];
        changes[len] = change;
    
    }
}
