/*
 * AssetSubtypeLoader.java
 *
 * Created on March 18, 2004, 5:29 PM
 * 
 * <copyright>
 * 
 *  Copyright 2004 Object Services and Consulting, Inc.
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

package org.cougaar.coordinator.techspec.xml;

import org.cougaar.coordinator.techspec.*;

import org.cougaar.core.plugin.ComponentPlugin;

import org.cougaar.core.service.LoggingService;

import org.cougaar.core.service.UIDService;
import org.cougaar.core.component.ServiceBroker;

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
    public AssetSubtypeLoader(ServiceBroker serviceBroker, UIDService us) {
        
        super("AssetSubtype", "AssetSubtypes", serviceBroker, us);
    }
  
    public void load() {}

    /** Called with a DOM "AssetSubtype" element to process */
    protected Vector processElement(Element element) {
     
        //publish to BB during execute().
        //1. Create a new AssetType instance & 
        String newtype = element.getAttribute("newType");
        String st = element.getAttribute("superType");
        AssetType superType = AssetType.findAssetType(st);

        
        //what to do when assetType is null? - create it, process it later?
        if (superType == null) {
            logger.warn("AssetSubtype XML Error - Asset SuperType unknown: "+st + ".  Not processing new type = " + newtype);
            return null;
        }
        
        try {
            //Register new asset subtype
            AssetType.addAssetSubtype(superType, newtype);        
            if (logger.isDebugEnabled()) logger.debug("Created new AssetSubtype = " + newtype);
        } catch (DupWithDifferentSuperTypeException dwd) {
            logger.warn("AssetSubtype XML Error - Asset sub type already exists, but declared with different superType!: \n"+dwd.toString());
        }
        
        return null;
    }
    
    
}


