/*
 * AssetRole.java
 *
 * Created on August 5, 2003, 3:01 PM
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
