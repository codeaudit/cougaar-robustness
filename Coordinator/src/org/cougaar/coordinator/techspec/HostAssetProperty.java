/*
 * HostAssetProperty.java
 *
 * Created on September 11, 2003, 9:32 AM
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
 *
 * @author Paul Pazandak, Ph.D. OBJS, Inc.
 */
public class HostAssetProperty extends AssetProperty {
  
    public static final HostAssetPropertyName geoLocale = new HostAssetPropertyName("GEO_LOCATION");
    public static final AssetPropertyName[] names = {geoLocale};    
    
    /** Creates a new instance of HostAssetProperty */
    public HostAssetProperty(HostAssetPropertyName propName, Object value) {
        super(propName, value);
    }
    
    /**
     * @return the valid property names that are accepted
     */
    public static AssetPropertyName[] getValidPropertyNames() { return names; }

    /** This class is used to control the creation of property names */
    static class HostAssetPropertyName implements AssetPropertyName {        
        private String name;
        private HostAssetPropertyName(String name) { this.name = name; }        
        public String toString() { return name; }
    }
    
}
