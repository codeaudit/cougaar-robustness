/*
 * NodeAssetPropertyChange.java
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
 * This class describes the interface for all NodeAssetPropertyChange instances. These
 * objects are published to the BB when one or more property values associated with 
 * an asset change. Subclasses corresponding to each asset type are used.
 *
 * @author Paul Pazandak, Ph.D. OBJS, Inc.
 */
public class NodeAssetPropertyChange extends AssetPropertyChange {
    
   
   private static final AssetType g_NodeType = AssetType.findAssetType("node");

   /** 
     * Creates a new instance of NodeAssetPropertyChange 
     */
    public NodeAssetPropertyChange(String assetName, NodeAssetProperty property ) {

        super (assetName, g_NodeType, property );
    }
    
    /** 
     * Creates a new instance of NodeAssetPropertyChange, with an array of changes
     */
    public NodeAssetPropertyChange(String assetName, NodeAssetProperty[] properties ) {

        super (assetName, g_NodeType, properties );
    }        

    
}
