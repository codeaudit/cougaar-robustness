/*
 * MessageStackResult.java
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
 * Created on December 30, 2002, 1:46 PM
 */

package LogPointAnalyzer;

import java.util.Vector;
import java.util.Iterator;

/**
 *
 * @author  pazandak@objs.com
 */
public class MessageStackResult {
    
    private XMLMessage msg = null;
    
    private String sendErr = null;
    private Vector sendStack = null;
    
    private String recvErr = null;
    private Vector recvStack = null;
    
    /** Creates a new instance of MessageStackResult */
    public MessageStackResult(XMLMessage _xml, Vector _s, Vector _r) {
        msg = _xml;
        sendStack = _s;
        recvStack = _r;
    }
    
    /** Creates a new instance of MessageStackResult */
    public MessageStackResult(XMLMessage _xml, Vector _s, String _se, Vector _r, String _re) {
        
        msg = _xml;
        
        sendStack = _s;
        sendErr = _se;
        
        recvStack = _r;
        recvErr = _re;
    }
    
    public Vector getSendStack() {return sendStack;}
    public String getSendError() {return sendErr;}
    
    public Vector getRecvStack() {return recvStack;}
    public String getRecvError() {return recvErr;}
    
    public String toString(boolean _toFile, boolean _lvl, int _start, int _end, boolean _wrapOutput) {
        
        StringBuffer sb = new StringBuffer();
        
        sb.append("\n>>>>>>>>>>>>>>>SEND STACK>>>>>>>>>>>>>>>>>\n");
        if (sendStack != null) {
            Iterator it = sendStack.iterator();
            while(it.hasNext()) {
                XMLMessage xml = (XMLMessage)it.next();
                if (!_toFile && _wrapOutput) //then print on several lines
                    sb.append(xml.toString(_lvl, _start, _end)+"\n\n");
                else
                    sb.append(xml.toString()+"\n\n");
            }
        } else
            sb.append("     ERROR: " + sendErr);
        
        sb.append("\n<<<<<<<<<<<<<<<RECV STACK<<<<<<<<<<<<<<<<<\n");
        if (recvStack != null) {
            Iterator it = recvStack.iterator();
            while(it.hasNext()) {
                XMLMessage xml = (XMLMessage)it.next();
                if (!_toFile) //then print on several lines
                    sb.append(xml.toString(_lvl, _start, _end)+"\n\n");
                else
                    sb.append(xml.toString()+"\n\n");
            }
        } else
            sb.append("     ERROR: " + recvErr);
        
        return sb.toString();
    }

    
    public String toString() {
        return toString(false, false, 0, 0, false);
    }
    
}
