/*
 * AssetName.java
 *
 * Created on September 11, 2003, 12:44 PM
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
 * A container for the generateExpandedAssetName() method.
 *
 * @author Paul Pazandak, OBJS
 */
public class AssetName {
    
    AssetType type;
    String name;
    
    /**
     * Create an asset name object 
     */
    public AssetName(String assetName, AssetType assetType) {
        type = assetType;
        name = assetName;
    }
    
    /** Return asset type */
    public AssetType getType() { return type; }

    /** Return asset name */
    public String getName() { return name; }
    
    /** Return true if object is equal to this object */
    public boolean equals(Object o) {
        return ( (o instanceof AssetName) && ((AssetName)o).getName().equals(this.name) && ( (AssetName)o).getType().equals(this.type) );
    }

    public String toString() { return name + ":" + type.toString(); }

}
