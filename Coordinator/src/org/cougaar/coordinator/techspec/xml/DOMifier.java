/*
 * DOMifier.java
 *
 * Created on March 18, 2004, 12:57 PM
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

import org.cougaar.util.ConfigFinder;

//import com.sun.xml.parser.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.io.File;

import org.apache.crimson.jaxp.*;


import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;


/**
 * This class converts an XML file into a DOM Document
 * @author  Administrator
 */
public class DOMifier {
    
    private DocumentBuilder domBuilder = null;
    private ConfigFinder configFinder;
    
    /** Logger for error msgs */
    private Logger logger;

    
    /** Creates a new instance of DOMifier */
    public DOMifier(ConfigFinder configFinder) {
        
        logger = Logging.getLogger(this.getClass().getName());

        DocumentBuilderFactory factory = DocumentBuilderFactoryImpl.newInstance();
        //Can customize factory before getting domBuilder.
        factory.setValidating(true);
        
        try {
            domBuilder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException pce) {
            logger.error("Could not create domBuilder: " + pce.toString() );
        }
        this.configFinder = configFinder;
    }
    

    /** Parse the xml file & return DOM Document */
    public Document parseFile(String fileParam) throws Exception {    

        if (domBuilder == null) {
            throw new Exception("Error creating DocumentBuilder. Cannot Parse any document. ");
        }
        
        try {
            
            File f = configFinder.locateFile(fileParam); //steve
            if (f==null || !f.exists()) { //look 
                if (f != null) {
                    logger.debug("*** Did not find XML file: " + fileParam);
                    logger.debug("*** Path checked was = " + f.getAbsolutePath()+". Checking CIP...");
                }
                String installpath = System.getProperty("org.cougaar.install.path");
                String defaultPath = installpath + File.separatorChar + "csmart" + File.separatorChar + "config" +
                   File.separatorChar + "lib" + File.separatorChar + "coordinator" + File.separatorChar + fileParam;

                f = new File(defaultPath);
                if (!f.exists()) {                    
                    logger.warn("*** Did not find XML file in = " + f.getAbsolutePath());
                    return null;
                }
            }                
            logger.debug("path for XML file = " + f.getAbsolutePath());
            //(new InputSource(new FileInputStream(f)));

            
            return domBuilder.parse(f);
            
        } catch (Exception e) {         
            logger.warn("DOMifier Exception: "+e, e);
            throw new Exception(e.toString());
        }
    }
    
}
