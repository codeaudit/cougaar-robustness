/* 
 * AgentAssetPropertyChange.java
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
 * This class describes the interface for all AgentAssetPropertyChange instances. These
 * objects are published to the BB when one or more property values associated with 
 * an asset change. Subclasses corresponding to each asset type are used.
 *
 * @author Paul Pazandak, Ph.D. OBJS, Inc.
 */
public class AgentAssetPropertyChange extends AssetPropertyChange {
    
   private static final AssetType g_AgentType = AssetType.findAssetType("agent");
    
    /** 
     * Creates a new instance of AgentAssetPropertyChange.
     * One must add property changes separately.
     */
    public AgentAssetPropertyChange(String assetName) {

        super (assetName, g_AgentType);
    }

    /** 
     * Creates a new instance of AgentAssetPropertyChange 
     */
    public AgentAssetPropertyChange(String assetName, AgentAssetProperty property ) {

        super (assetName, g_AgentType, property );
    }
    
    /** 
     * Creates a new instance of AgentAssetPropertyChange, with an array of changes
     */
    public AgentAssetPropertyChange(String assetName, AgentAssetProperty[] properties ) {

        super (assetName, g_AgentType, properties );
    }        
    
    /**
     * Identifies the change as a move change. Adds two property changes to this
     * AgentAssetPropertyChange -- one for host & one for node.
     */
/*    public void moveChange(String hostName, String nodeName) {
        
        AgentAssetProperty aap;
        
        aap = new AgentAssetProperty(AgentAssetProperty. .host, hostName);
        addPropertyChange(aap);
        aap = new AgentAssetProperty(AgentAssetProperty.node, nodeName);
        addPropertyChange(aap);
        
    }
*/
    
}
