/*
 * TestThreatModelXMLParser.java
 *
 * Created on September 18, 2003, 10:27 AM
 *
 * <copyright>
 * 
 *  Copyright 2002-2004 Object Services and Consulting, Inc.
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
 * @deprecated
 * @author  Administrator
 */
public class TestThreatModelXMLParser extends ComponentPlugin {
    
    private String file  = null;
    /** Creates a new instance of TestThreatModelXMLParser */
    public TestThreatModelXMLParser() {
    }
    
    
    public void setupSubscriptions() {
/*    
    
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
        
  */  
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
/*
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
 */
    }
    
}
