/*
 * XMLMessage.java
 *
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

import java.util.Properties;

/**
 *
 * @author  pazandak@objs.com
 */
public class XMLMessage {
    
    Properties unknown = null;
    String from = null;
    String to = null;
    String num = null;
    String lpName = null;
    String time = null;
    boolean isValid = true;
    int level = 0;
    
    // 0 = not received by recipient
    // 1 = received
    // oh, oh = received more than once!
    int receiveCount = 0; //only applies to SEND-side msgs.        
        //Gosh Batman, should we subclass for ONE attr or not??
    
    
    static String testStr2 = "<LP lpName=\"After:ReceiveLink.deliverMessage\" time=\"1040157999227\" from=\"FWD-C.2-BDE-1-AD.1040157995962\" to=\"FWD-C.1-6-INFBN.1040157993556\" num=\"9\" />";

    /* for testing purposes */
    static public void main(String[] args) {
        try {
            System.out.println("Running XMLMessage test...");
            XMLMessage xml = XMLMessageParser.getParser().parse(testStr2);
            System.out.println("Parsed:\n   "+xml);
        } catch (Exception e) {
            System.out.println("Saw exception: "+ e);
            e.printStackTrace();
        }
    }
    
    
    
    /** Creates a new instance of XMLMessage */
    public XMLMessage() {
    }
    
    static XMLMessage parseXMLMsg(String _line) throws XMLMessageException {
        return XMLMessageParser.getParser().parse(_line);
    }
    
    public void setNum(String _val) { num = _val; }
    public void setTo(String _val)  { to = _val; }
    public void setFrom(String _val) { from = _val; }
    public void setLPName(String _val) { lpName = _val; }
    public void setTime(String _val) { time = _val; }
    public void addOther(String _attr, String _val) { 
        if (unknown == null) //don't create unless needed
            unknown = new Properties();
        unknown.put(_attr, _val);
    }
    public void setIsValid(boolean _v) { isValid=_v; }
    public void setLevel(int _val) { level = _val; }

    //Return true if node = "null." or incarnation # = 0
    public boolean senderAddrError() { return (from.startsWith("null.") || from.endsWith(".0")); }
    //Return true if node = "null." or incarnation # = 0
    public boolean receiverAddrError() { return (to.startsWith("null.") || to.endsWith(".0")); }
    
    public String getNum() { return num; }
    public String getTo()  { return to; }
    public String getFrom() { return from; }
    public String getLPName() { return lpName; }
    public String getTime() { return time; }
    public Properties getOther() { return unknown; } 
    public boolean isValid() { return isValid; }
    public int getLevel() { return level; }
    
    public String toString() { return "lpName="+lpName+" to="+to+" from="+from+" num="+num+" time="+time; }

    static final String space = "                              ";
    
    //Print with nice indenting
    public String toString(boolean _printLvl, int _firstIndent, int _end) { 
        
        //Make sure _start isn't longer than our space string
        _firstIndent = _firstIndent > space.length()-1 ? space.length()-1 : _firstIndent; 
        
        StringBuffer sb = new StringBuffer();
        String lvl = "";
        if (_printLvl)
            lvl = "Lvl="+level+" ";
        int lvlLen = lvl.length();
        int restIndent = lvlLen + _firstIndent;
        int lineSize = _end - restIndent;
        
        String out = 
            "lpName="+lpName+" to="+to+" from="+from+" num="+num+" time="+time; 
    
        sb.append(space.substring(0, _firstIndent-1));
        sb.append(lvl);
        
        boolean more = true;
        while (more) {

            String temp = null;
            if (out.length() > lineSize) {             
                temp = out.substring(0, lineSize-1);
                out = out.substring(lineSize-1);                
            } else {
                temp = out;
                more = false;
            }
            
            sb.append(temp); 
            sb.append("\n"+space.substring(0,restIndent-1)); //prep next line
            
        }
        
        
        return sb.toString();
    }
    
}
