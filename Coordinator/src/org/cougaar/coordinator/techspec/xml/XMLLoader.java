/*
 * XMLLoader.java
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


import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.component.ServiceBroker;

import org.cougaar.core.service.LoggingService;
import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.ArrayList;

import org.xml.sax.InputSource;
import java.io.File;
import java.io.FileInputStream;
import org.cougaar.core.persist.NotPersistable;

import org.w3c.dom.*;

/**
 * This class is used to import techspecs from xml files.
 *
 * @author  Administrator
 */
public abstract class XMLLoader implements NotPersistable  {
    
    protected Logger logger;
    private String singleTag;
    private String pluralTag;
    protected ServiceBroker serviceBroker;
    
    protected UIDService us = null;
    
    /** Creates a new instance of AssetTypeLoader */
    public XMLLoader(String singleTag, String pluralTag, ServiceBroker serviceBroker, UIDService us) { 
        this.singleTag = singleTag;
        this.pluralTag = pluralTag;
        
        this.serviceBroker = serviceBroker;
        this.us = us;
        
        logger = Logging.getLogger(getClass()); 
    }

    
    /** Returns true if the argument (the name of an XML element) is a valid root tag */
    protected boolean isValidTag(String tag) {
        return (tag.equalsIgnoreCase(singleTag) || tag.equalsIgnoreCase(pluralTag) );
    }
    
    
    /**
     *  Called with a DOM element to process.
     *  This is where the custom subclass code goes to walk thru the tree
     *  and to instantiate a tech spec instance.
     */
    protected abstract Vector processElement(Element element);
    
    
    /** Called with a DOM document to process */
    protected Vector processDocument(org.w3c.dom.Document doc) {
        
        Vector objects = null;
        
        //First see if there is more than one
        if (doc == null) return null;
        
        Element root = doc.getDocumentElement();
        String tag = root.getTagName();
        
        if (tag.equalsIgnoreCase(singleTag)) {
            return processElement(root);
        } else if (pluralTag != null && tag.equalsIgnoreCase(pluralTag)) {
            
            for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase(singleTag) ) {
                    objects = processElement( (Element)child ) ;
                }
            }
            
            return objects; //vector is a cumulative representation
            
        } //else it's a text Node, so don't process.
        
        return null;
    }
    
}
