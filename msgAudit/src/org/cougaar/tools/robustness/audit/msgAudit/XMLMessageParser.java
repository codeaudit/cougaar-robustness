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

import org.xml.sax.*;


/**
 * The class reads XML documents without using a DTD 
 */
public class XMLMessageParser extends org.xml.sax.helpers.DefaultHandler {
    
    static String testStr = "<record foo=\"3\" goo=\"abc\"><param><sender>Me</sender></param><receiver>You</receiver><msgID>123</msgID></record>";
    static String testStr2 = "<LP lpName=\"After:ReceiveLink.deliverMessage\" time=\"1040157999227\" from=\"FWD-C.2-BDE-1-AD.1040157995962\" to=\"FWD-C.1-6-INFBN.1040157993556\" num=\"9\" />";

    private String attr;
    private String val;
    private XMLMessage xmlMessage;
    private InputSource inputSource;
    
    private static XMLMessageParser handler = null;
    private static SAXParser sax = null;
    
    static {
        try {
            handler = new XMLMessageParser();
            SAXParserFactory factory = SAXParserFactory.newInstance();
            sax = factory.newSAXParser();
        } catch (Exception e) {
            System.out.println("Saw exception: "+ e);
            e.printStackTrace();
        }
    }
    
    public static XMLMessageParser getParser() { return handler; }
    
    static public void main(String[] args) {
        try {
            sax.parse(new InputSource(new StringReader(testStr2)) , handler);
        } catch (Exception e) {
            System.out.println("Saw exception: "+ e);
            e.printStackTrace();
        }
    }

    
    public XMLMessage parse(String _xml) throws XMLMessageException {
        try {
            xmlMessage = new XMLMessage();
//            sax.parse(new InputSource(new StringReader(_xml)) , handler);
            inputSource.setCharacterStream(new StringReader(_xml));
            sax.parse(inputSource , handler);
            return xmlMessage;
        } catch (Exception e) {
            if (ConfigData.DEBUG) {
                System.out.println("Saw exception: "+ e);
                e.printStackTrace();
            }
            throw new XMLMessageException("Error parsing log entry["+e+"]: \n    " + _xml);
        }
    }
    
    /**
     * Creates a parser instance.
     */
    public XMLMessageParser() {
        inputSource = new InputSource();
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
        
        for (int i=0; i<attrs.getLength();i++) {
            attr = attrs.getQName(i);
            val  = attrs.getValue(i);
            if (attr.equalsIgnoreCase("num"))
                xmlMessage.setNum(val);
            else if (attr.equalsIgnoreCase("to"))
                xmlMessage.setTo(val);
            else if (attr.equalsIgnoreCase("from"))
                xmlMessage.setFrom(val);
            else if (attr.equalsIgnoreCase("lpName"))
                xmlMessage.setLPName(val);
            else if (attr.equalsIgnoreCase("time"))
                xmlMessage.setTime(val);
            else 
                xmlMessage.addOther(attr, val);
        }
        
        
    }
    
    /**
     * This SAX interface method is implemented by the parser.
     */
    public final void endElement(java.lang.String ns, java.lang.String name, java.lang.String qname) throws SAXException {
         if (ConfigData.DEBUG) System.out.println("end ele");
    }
    


}

