/*
 * MapLoader.java
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
import org.cougaar.util.ConfigFinder;

import org.cougaar.core.service.LoggingService;
import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Hashtable;

import org.w3c.dom.*;

/**
 * This class is used to import Maps from xml files. They take the form:
 *<p>
 * <Map>
 *   <Mapping name="LOW" value="0">
 *   <Mapping name="MEDIUM" value="1">
 *   ...
 * </Map> 
 *<p>
 * where the values are converted to Floats
 *
 * @author  Administrator
 */
public class MapLoader {

    private static Hashtable maps;
    private static DOMifier dom = null;
    private static Logger logger;
    
    static {        
        maps = new Hashtable();
        logger = Logging.getLogger(MapLoader.class.toString());
    }
    
    public static Hashtable loadMap(ConfigFinder configFinder, String filename) {
        
        //First see if we already have loaded the requested map. If so, just return it.
        if ( dom != null ) {
            Hashtable m = (Hashtable) maps.get(filename);
            if (m != null) { return m; }
        }
        
        //do once
        synchronized(dom) {
            if (dom == null) {
                DOMifier dom = new DOMifier(configFinder);
            }
        }        
        
        Document doc;
        Element root = null;
        
        try {
            doc = dom.parseFile(filename);
            //Call subclass to process the dom tree 
            root = doc.getDocumentElement();
            if (root == null) {
                logger.error("Error parsing XML map file [" + filename + "]. Root element was null.");
                return null;
            }
        } catch (Exception e) {
            logger.error("Error parsing XML map file [" + filename + "]. Error was: "+ e.toString(), e);
            return null;
        }        
        
        Hashtable map = processMap(root, filename);
        maps.put(filename, map);
        logger.debug("Added new map: \n" + filename);
        return map;
    }

    /** Called with a Map element to process */
    protected static Hashtable processMap(Element element, String filename) {

        Hashtable map = new Hashtable();
        
        //Populate the map
        Element e;
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase("Mapping") ) {
                e = (Element)child;
                parseMapping(e, map, filename);
            } //else, likely a text element - ignore
        }

        return map;            
    }
    
    
    
    
    /** Called with a DOM "Threat" element to process */
    protected static void parseMapping(Element element, Hashtable map, String filename) {
        
        //Read the map value
        String name= element.getAttribute("name");
        String value = element.getAttribute("value");
        Float f;
        try {
            f = Float.valueOf(value);
            map.put(name, f);
        } catch (NumberFormatException nfe) {
            logger.error("Error parsing XML map file [" + filename + "]. Float conversion error with value = "+value);
        }            
    }
    
    
}


