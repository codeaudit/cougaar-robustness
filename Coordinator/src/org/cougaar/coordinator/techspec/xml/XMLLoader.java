/*
 * XMLLoader.java
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
