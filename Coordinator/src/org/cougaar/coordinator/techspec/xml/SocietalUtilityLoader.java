/*
 * SocietalUtilityLoader.java
 *
 * Created on March 23, 2004, 11:15 AM
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
 * This class is used to import SocietalUtility techspecs from xml files.
 * These are added to the AssetType class structure, and not published to the BB.
 *
 * @author  Administrator
 */
public class SocietalUtilityLoader extends XMLLoader {
    
    Vector utilities;
    
    /** Creates a new instance of SocietalUtilityLoader */
    public SocietalUtilityLoader() {
        
        super("SocietalUtility", null);
        utilities = new Vector();
    }
  

    /** Called with a DOM "SocietalUtility" element to process */
    protected void processElement(Element element) {
     
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) { 
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase("RelativeValuation") ) {
                Element e = (Element)child;
        
                String su = null;
                try {
                    String at = e.getAttribute("assetType");
                    su = e.getAttribute("societalUtility");
                    int utility = Integer.parseInt(su);
                    AssetType assetType = AssetType.findAssetType(at);

                    if (assetType != null) {
                        assetType.setUtilityValue(utility);
                    } else {
                        logger.warn("SocietalUtility XML Error - Asset Type unknown: " + at );
                    }
                } catch (NumberFormatException nfe) {
                    logger.warn("SocietalUtility XML Error - NumberFormatException: " + su );
                }
            }
        }
    }
    
    
    protected void execute() {}

    
    
}


