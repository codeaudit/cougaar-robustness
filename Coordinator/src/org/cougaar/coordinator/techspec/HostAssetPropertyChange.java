/*
 * HostAssetPropertyChange.java
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
 * This class describes the interface for all HostAssetPropertyChange instances. These
 * objects are published to the BB when one or more property values associated with 
 * an asset change. Subclasses corresponding to each asset type are used.
 *
 * @author Paul Pazandak, Ph.D. OBJS, Inc.
 */
public class HostAssetPropertyChange extends AssetPropertyChange {
    

    private static final AssetType g_HostType = AssetType.findAssetType("host");    
   
    /** 
     * Creates a new instance of HostAssetPropertyChange 
     */
    public HostAssetPropertyChange(String assetName, HostAssetProperty property ) {

        super (assetName, g_HostType, property );
    }
    
    /** 
     * Creates a new instance of HostAssetPropertyChange, with an array of changes
     */
    public HostAssetPropertyChange(String assetName, HostAssetProperty[] properties ) {

        super (assetName, g_HostType, properties );
    }        

    
}
