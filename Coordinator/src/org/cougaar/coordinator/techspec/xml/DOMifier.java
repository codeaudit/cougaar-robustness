/*
 * DOMifier.java
 *
 * Created on March 18, 2004, 12:57 PM
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

import org.cougaar.util.ConfigFinder;

//import com.sun.xml.parser.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import org.apache.crimson.jaxp.*;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;
import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;


/**
 * This class converts an XML file into a DOM Document
 * @author  Administrator
 */
public class DOMifier implements ErrorHandler, EntityResolver  {
    
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
            domBuilder.setErrorHandler(this);
            domBuilder.setEntityResolver(this);
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
            
            File f = findFile(fileParam);
            
            try {
                return domBuilder.parse(f);
            } catch (Exception e) {
                logger.error("SOME TECH SPECS WILL NOT BE LOADED!! Exception finding/parsing file tech spec ["+f.getName()+"] ");
                return null;
            }
        } catch (Exception e) {         
            logger.error("DOMifier Exception: "+e);
            //throw new Exception(e.toString());
            return null;
        }
    }

/* source of much confusion
    private File findFile(String fileParam) {
        
            File f = configFinder.locateFile(fileParam); //steve
            if (f==null || !f.exists()) { //look 
                if (f != null) {
                    if (logger.isDebugEnabled()) logger.debug("*** Did not find XML file: " + f.getAbsolutePath());
                    if (logger.isDebugEnabled()) logger.debug("*** Path checked was = " + f.getAbsolutePath()+". Checking CIP...");
                }
                String installpath = System.getProperty("org.cougaar.install.path");
                String defaultPath = installpath + File.separatorChar + "csmart" + File.separatorChar + "config" +
                   File.separatorChar + "lib" + File.separatorChar + "coordinator" + File.separatorChar + fileParam;

                //Now, try local dir...
                f = new File(fileParam);
                if (!f.exists()) {     
                    if (f!= null) { logger.warn("*** Did not find XML file in = " + f.getAbsolutePath()); }
                    //now try default path
                    f = new File(defaultPath);
                    if (!f.exists()) {                    
                        logger.warn("*** Did not find XML file in = " + f.getAbsolutePath());
                        return null;
                    }
                }
            }                
            if (logger.isDebugEnabled()) logger.debug("path for XML file = " + f.getAbsolutePath());
            //(new InputSource(new FileInputStream(f)));

           return f;
    }
*/
  
    private File findFile(String fileParam) {
	File f = configFinder.locateFile(fileParam);
	if (f == null) {
            if (logger.isErrorEnabled()) 
		logger.error("Did not find XML file="+fileParam+" in config path="+configFinder.getConfigPath());	
	} else if (!f.exists()) { 
	    if (logger.isErrorEnabled())
		logger.error("XML File "+f.getAbsolutePath()+" does not exist.");
	} else {
	    if (logger.isInfoEnabled())
		logger.info("Found XML File "+f.getAbsolutePath());
	}
	return f;
    }

    //FROM ErrorHandler ------------------------------------------------------------------------------------------------------------------
    
    public void warning(SAXParseException exception) throws SAXException {
        if (logger.isDebugEnabled()) logger.debug("SAXException - Warning at line/col["+exception.getLineNumber()+":"+exception.getColumnNumber()+"]: "+ exception);
    }
    
    public void error(SAXParseException exception) throws SAXException {
        logger.error("SAXException - Error at line/col["+exception.getLineNumber()+":"+exception.getColumnNumber()+"]: "+ exception);
    }
    
    public void fatalError(SAXParseException exception) throws SAXException {
        logger.error("SAXException - FatalError at line/col["+exception.getLineNumber()+":"+exception.getColumnNumber()+"]: "+ exception);
    }

    
    
    // FROM EntityResolver  ------------------------------------------------------------------------------------------------------------
    
   /**
    *  Used by the parser to find the DTDs
    */ 
   public InputSource resolveEntity (String publicId, String systemId)
   {
        //take everything off until the last slash
       int index = systemId.lastIndexOf('/');
       String filename = systemId.substring(index+1);
        
        File f = findFile(filename);
        if (f == null) { return null; }
        try {         
            return (new InputSource(new FileInputStream(f)));
        } catch (FileNotFoundException fnf) {
            return null;
        }
   }
     
}
