/*
 * AssetSubtypeLoader.java
 *
 * Created on March 18, 2004, 5:29 PM
 * <copyright>
 *  Copyright 2003 Object Services and Consulting, Inc.
 *  Copyright 2001-2003 Mobile Intelligence Corp
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

package org.cougaar.coordinator.techspec.xml;

import org.cougaar.coordinator.techspec.*;

import org.cougaar.core.plugin.ComponentPlugin;

import org.cougaar.core.service.LoggingService;

import org.cougaar.core.service.UIDService;

import org.cougaar.core.service.LoggingService;
import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.ArrayList;

import org.w3c.dom.*;

/**
 * This class is used to import AssetSubType techspecs from xml files.
 * These are added to the AssetType class structure, and not published to the BB.
 *
 * @author  Administrator
 */
public class AssetSubtypeLoader extends XMLLoader {
    
   
    /** Creates a new instance of AssetSubtypeLoader */
    public AssetSubtypeLoader() {
        
        super("AssetSubtype", "AssetSubtypes");
    }
  

    /** Called with a DOM "AssetSubtype" element to process */
    protected void processElement(Element element) {
     
        //publish to BB during execute().
        //1. Create a new AssetType instance & 
        String newtype = element.getAttribute("newType");
        String st = element.getAttribute("superType");
        AssetType superType = AssetType.findAssetType(st);

        
        //what to do when assetType is null? - create it, process it later?
        if (superType == null) {
            logger.warn("AssetSubtype XML Error - Asset SuperType unknown: "+st + ".  Not processing new type = " + newtype);
            return;
        }
        
        try {
            //Register new asset subtype
            AssetType.addAssetSubtype(superType, newtype);        
            logger.debug("Created new AssetSubtype = " + newtype);
        } catch (DupWithDifferentSuperTypeException dwd) {
            logger.warn("AssetSubtype XML Error - Asset sub type already exists, but declared with different superType!: \n"+dwd.toString());
        }
    }
    
    
    protected void execute() {}

    
    
}


