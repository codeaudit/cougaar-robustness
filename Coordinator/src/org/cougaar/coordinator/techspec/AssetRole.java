/*
 * AssetRole.java
 *
 * Created on August 5, 2003, 3:01 PM
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
import org.cougaar.core.persist.NotPersistable;


/**
 * @author Paul Pazandak, Ph.D. OBJS, Inc.
 */
public class AssetRole implements NotPersistable {


     public final static AssetRole HOST = new AssetRole("host");
     public final static AssetRole NODE = new AssetRole("node");
    
    
    private static Vector types;
    static {
        types = new Vector();
        types.add(HOST);
        types.add(NODE);
    }
    
    /**
     * @return The Asset Role whose name matches the string provided, ignoring case. If no match found, returns NULL.
     */
    public static AssetRole findAssetRole(String name) {
        
        AssetRole found = null;
        Iterator iter = types.iterator();
        while (iter.hasNext()) {
            AssetRole at = (AssetRole)iter.next();
            if (at.getName().equalsIgnoreCase(name)) {
                found = at;
                break;
            }
        }
        return found;
    }
    
    
    /** the name of this asset role */
    private String name;
    
    
    /** Creates a new instance of AssetRole 
     *@param asset role name
     */
    public AssetRole(String name) {
        this.name = name;
    }
    
    /**
     *@return the name of this Asset Role
     *
     */
    public String getName() { return name; }

    /**
     *@return the name of this Asset Role
     *
     */
    public String toString() { return name; }
    
    
}
