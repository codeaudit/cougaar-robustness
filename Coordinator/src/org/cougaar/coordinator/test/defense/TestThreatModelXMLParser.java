/*
 * TestThreatModelXMLParser.java
 *
 * Created on September 18, 2003, 10:27 AM
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

//org.cougaar.coordinator.test.defense.TestThreatModelXMLParser
package org.cougaar.coordinator.test.defense;
import org.cougaar.coordinator.techspec.*;

import org.xml.sax.InputSource;
import java.io.File;
import java.io.FileInputStream;
import java.util.Vector;

import java.util.Iterator;
import org.cougaar.core.plugin.ComponentPlugin;

/**
 *
 * @author  Administrator
 */
public class TestThreatModelXMLParser extends ComponentPlugin {
    
    private String file  = null;
    /** Creates a new instance of TestThreatModelXMLParser */
    public TestThreatModelXMLParser() {
    }
    
    
    public void setupSubscriptions() {
    
    
        getPluginParams();
        try {
            ThreatModelXML_DTDHandler h = new ThreatModelXML_DTDHandler();
            ThreatModelXML_DTDParser parser = new ThreatModelXML_DTDParser(h);
            File f = new File(file);
            System.out.println("*************************************");
            System.out.println("*************************************");
            System.out.println("path for file = " + f.getAbsolutePath());
            System.out.println("*************************************");
            System.out.println("*************************************");
            System.out.println("*************************************");
            
            parser.parse(new InputSource(new FileInputStream(f)));

            Vector models = h.getModels();
            System.out.println("Imported "+models.size()+" models!");
            
            Iterator i = models.iterator();
            while (i.hasNext()) {
                System.out.println("________>>>>>>>>>>>>>>>>>________________MODELS________________________________");
                MetaThreatModel m = (MetaThreatModel)i.next();
                if (m == null)
                    System.out.println("Model is null!");
                else {
                    String ms = m.toString();
                    System.out.println(ms);
                }
            }
            
        } catch (Exception e) {
            
            System.out.println("Exception in tester: "+e);
            e.printStackTrace();
        }
        
    
    }
    public void execute() {}
    
    /**
      * Demonstrates how to read in parameters passed in via configuration files. Use/remove as needed. 
      */
    private void getPluginParams() {
        
        //The 'logger' attribute is inherited. Use it to emit data for debugging
        //if (logger.isInfoEnabled() && getParameters().isEmpty()) logger.error("plugin saw 0 parameters.");

        Iterator iter = getParameters().iterator (); 
        if (iter.hasNext()) {
            file = (String) iter.next();
            System.out.println("Rad in plugin Parameter = " + file);
        }
    }       

    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        try {
            ThreatModelXML_DTDHandler h = new ThreatModelXML_DTDHandler();
            ThreatModelXML_DTDParser parser = new ThreatModelXML_DTDParser(h);
            parser.parse(new InputSource(new FileInputStream(new File(args[0]))));

            Vector models = h.getModels();
            System.out.println("Imported "+models.size()+" models!");
        } catch (Exception e) {
            
            System.out.println("Exception: "+e);
            e.printStackTrace();
        }
    }
    
}
