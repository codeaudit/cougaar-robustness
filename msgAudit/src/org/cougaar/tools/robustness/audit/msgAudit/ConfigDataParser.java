/*
 * File:           XMLMessageParser.java
 * <copyright>
 *  Copyright 2002 Object Services and Consulting, Inc. (OBJS),
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
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
 *
 * Created on December 17, 2002, 6:53 PM
 */

package LogPointAnalyzer;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXParseException;
import java.io.StringReader;
import java.io.File;
import java.io.FileReader;

import org.xml.sax.*;


/**
 * The class reads XML documents without using a DTD 
 */
public class ConfigDataParser extends org.xml.sax.helpers.DefaultHandler {
    
    static String testStr = "<record foo=\"3\" goo=\"abc\"><param><sender>Me</sender></param><receiver>You</receiver><msgID>123</msgID></record>";
    static String testStr2 = "<LP lpName=\"After:ReceiveLink.deliverMessage\" time=\"1040157999227\" from=\"FWD-C.2-BDE-1-AD.1040157995962\" to=\"FWD-C.1-6-INFBN.1040157993556\" num=\"9\" />";

    private String attr;
    private String val;
    private LogPointLevel lpi;
    
    private ConfigData configData = null;
    
    private int count = 0;
    
    private static SAXParser sax = null;
    
    static public void main(String[] args) {
        try {
            ConfigData cd = new ConfigData();
            ConfigDataParser cdp = new ConfigDataParser();
            ConfigData.setDebug(true);
            cdp.parse(new File(args[0]), cd);
        } catch (Exception e) {
            System.out.println("Saw exception: "+ e);
            e.printStackTrace();
        }
    }

        
    /**
     * Creates a parser instance.
     */
    public ConfigDataParser() {}
    
    //Parse the specified file. Callbacks are made to the ConfigData instance 
    //with the parsed data.
    public void parse(File _file, ConfigData _config) throws Exception {
        
        count = 0;
        configData = _config;
        
        //Get Parser
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            sax = factory.newSAXParser();
        } catch (Exception e) {
            System.out.println("Saw exception: "+ e);
            e.printStackTrace();
        }
                
        //Go ahead and parse already!
        try {
            System.out.println("Reading in LogPointInfo entries from config file...");
            sax.parse(new InputSource(new FileReader(_file)) , this);
            System.out.println();
        } catch (Exception e) {
            if (ConfigData.DEBUG) {
                System.out.println("Saw exception: "+ e);
                e.printStackTrace();
            }
            throw new Exception("Error parsing config data["+e+"]");
        }
    }
    
    
    
    /**
     * This SAX interface method is implemented by the parser.
     */
    public final void startDocument() throws SAXException {
        if (ConfigData.DEBUG) System.out.println("start doc");
    }
    
    /**
     * This SAX interface method is implemented by the parser.
     */
    public final void endDocument() throws SAXException {
        if (ConfigData.DEBUG) System.out.println("end doc");
    }
    
    /**
     * This SAX interface method is implemented by the parser.
     */
    public final void startElement(java.lang.String ns, java.lang.String name, java.lang.String qname, Attributes attrs) throws SAXException {
        if (ConfigData.DEBUG) {
            System.out.println("element: "+qname);
            System.out.println("  Attrs:");
            for (int i=0; i<attrs.getLength();i++)
                System.out.println("     "+attrs.getQName(i)+" = "+attrs.getValue(i));
        }

        String ID   = "";
        String NAME = "";
        String SEQ  = "";
        String SEND = "";

        if (qname.equalsIgnoreCase("LogPointInfo")) {
            System.out.print(".."+(count++));

            for (int i=0; i<attrs.getLength();i++) {
                attr = attrs.getQName(i);
                val  = attrs.getValue(i);
                if (attr.equalsIgnoreCase("SEQ_NUM"))
                    SEQ = val;
                else if (attr.equalsIgnoreCase("ID"))
                    ID = val;
                else if (attr.equalsIgnoreCase("USER_VISIBLE_NAME"))
                    NAME = val;
                else if (attr.equalsIgnoreCase("SEND_STACK"))
                    SEND = val;
            }

            lpi = new LogPointLevel(ID, SEQ, SEND, NAME);
            configData.addLevel(lpi);

            if (ConfigData.DEBUG) 
                System.out.println("ConfigDataParser:Adding new LogPointInfo entry:\n   "+lpi);
        
        }        
    }
    
    /**
     * This SAX interface method is implemented by the parser.
     */
    public final void endElement(java.lang.String ns, java.lang.String name, java.lang.String qname) throws SAXException {
         if (ConfigData.DEBUG) System.out.println("end ele");
    }
    
    //<LogPointInfo SEQ_NUM="0" ID="value" USER_VISIBLE_NAME="value" SEND_STACK="TRUE">


}

