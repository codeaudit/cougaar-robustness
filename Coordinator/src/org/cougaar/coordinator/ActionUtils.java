/*
 * ActionUtils.java
 *
/*
 * ActionUtils.java
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

package org.cougaar.coordinator;
import org.cougaar.coordinator.techspec.*;

/**
 * This class provides accessor methods to get at package private methods
 * of the Action class. The Action methods are package private to keep the 
 * Action api simple... for better or worse.
 */
public  class ActionUtils 
{

    /**
     * @return the asset type related to this defense condition
     */
    public static AssetType getAssetType(Action a) { 
        if (a != null) { return a.getAssetType();  }
        else { return null; }
        
    }
    
    
    /**
     * @return expanded name - "type:name"
     */
    public static AssetID getAssetID(Action a) { 
        if (a != null) { return a.getAssetID(); }
        else { return null; }
    }

    /**
     * @return expanded name - "type:name"
     */
    public static String setPermittedValues(Action a, java.util.Set s) { 
        try {
            if (a != null) { a.setPermittedValues(s); }
            return null;
        } catch (Exception e) {
            return e.toString();
        }
    }
    
}
