/*
 * AssetID.java
 *
 * Created on September 11, 2003, 12:44 PM
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

import java.io.Serializable;

/**
 * A class to identify an asset.
 *
 * @author Paul Pazandak, OBJS
 */
public class AssetID implements Serializable {
    
    AssetType type;
    String name;
    String id;
    
    /**
     * Create an asset name object 
     */
    public AssetID(String assetName, AssetType assetType) {
        type = assetType;
        name = assetName;
        id = name + ":" + type.toString(); 
    }
    
    /** Return asset type */
    public AssetType getType() { return type; }

    /** Return asset name */
    public String getName() { return name; }
    
    /** Return true if object is equal to this object */
    public boolean equals(Object o) {
        return  ( (o instanceof AssetID) && ((AssetID)o).getName().equals(this.name) && ( (AssetID)o).getType().equals(this.type) );
    }

    public int hashCode() { return id.hashCode(); }

    public String toString() { return id; }


}
