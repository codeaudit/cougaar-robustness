/*
 * AssetPropertyChange.java
 *
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
    
    /** the (simple, not 'expanded') name of the asset */
    private String assetName;
    
    /** the asset type */
    private AssetType type;
    
    /** the changes that are being declared */
    private AssetProperty[] changes;
    
    /** the expanded name (type:name) of the asset */
    private String expandedName;
    
    /** 
     * Creates a new instance of AssetPropertyChange 
     */
    public AssetPropertyChange(String assetName, AssetType type) {
        
        this.assetName = assetName;
        this.type = type;
        changes = new AssetProperty[0];

        expandedName = AssetName.generateExpandedAssetName(assetName, type);
    }

    /** 
     * Creates a new instance of AssetPropertyChange 
     */
    public AssetPropertyChange(String assetName, AssetType type, AssetProperty property ) {
        
        this.assetName = assetName;
        this.type = type;
        changes = new AssetProperty[1];
        changes[0] = property;

        expandedName = AssetName.generateExpandedAssetName(assetName, type);
    }
    
    /** 
     * Creates a new instance of AssetPropertyChange, with an array of changes
     */
    public AssetPropertyChange(String assetName, AssetType type, AssetProperty[] properties ) {
        this.assetName = assetName;
        this.type = type;
        changes = properties;

        expandedName = AssetName.generateExpandedAssetName(assetName, type);
    }        

    /**
     * @return expanded name - "type:name" for the asset that this change affects
     */
    public String getExpandedName() { return expandedName; }
    
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
