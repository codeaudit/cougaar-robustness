/*
 * <copyright>
 * 
 *  Copyright 2003-2004 Object Services and Consulting, Inc.
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

/*
 * File:           SocietyHandler.java
 * Date:           August 27, 2003  4:55 PM
 *
 * @author  Administrator
 * @version generated by NetBeans XML module
 */
package org.cougaar.coordinator.tools;

import org.xml.sax.*;

public class SocietyHandler implements SocietyHandlerInterface {
    
    public static final boolean DEBUG = false;

    
    public void start_agent(final Attributes meta, SocietyTree tree) throws SAXException {
        if (DEBUG) System.err.println("start_agent: " + meta.getValue("name"));
        tree.addAgent(meta.getValue("name"));
    }
    
    public void end_agent() throws SAXException {
        if (DEBUG) System.err.println("end_agent()");
    }
    
    
    public void start_node(final Attributes meta, SocietyTree tree) throws SAXException {
        if (DEBUG) System.err.println("start_node: " + meta.getValue("name"));
        tree.addNode(meta.getValue("name"));
    }
    
    public void end_node() throws SAXException {
        if (DEBUG) System.err.println("end_node()");
    }
    
    public void start_host(final Attributes meta, SocietyTree tree) throws SAXException {
        if (DEBUG) System.err.println("start_host: " + meta.getValue("name"));
        tree.addHost(meta.getValue("name"));
    }
    
    public void end_host() throws SAXException {
        if (DEBUG) System.err.println("end_host()");
    }
    
    public void start_society(final Attributes meta) throws SAXException {
        if (DEBUG) System.err.println("start_society: " + meta.getValue("name"));
    }
    
    public void end_society() throws SAXException {
        if (DEBUG) System.err.println("end_society()");
    }
    
    public void characters(char[] values, int param, int param2) throws org.xml.sax.SAXException {
    }
    
    public void endDocument() throws org.xml.sax.SAXException {
    }
    
    public void endElement(String str, String str1, String str2) throws org.xml.sax.SAXException {
    }
    
    public void endPrefixMapping(String str) throws org.xml.sax.SAXException {
    }
    
    public void ignorableWhitespace(char[] values, int param, int param2) throws org.xml.sax.SAXException {
    }
    
    public void processingInstruction(String str, String str1) throws org.xml.sax.SAXException {
    }
    
    public void setDocumentLocator(org.xml.sax.Locator locator) {
    }
    
    public void skippedEntity(String str) throws org.xml.sax.SAXException {
    }
    
    public void startDocument() throws org.xml.sax.SAXException {
    }
    
    public void startElement(String str, String str1, String str2, org.xml.sax.Attributes attributes) throws org.xml.sax.SAXException {
    }
    
    public void startPrefixMapping(String str, String str1) throws org.xml.sax.SAXException {
    }
    
}

