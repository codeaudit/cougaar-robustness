/*
 * LogPointLevel.java
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
 *
 * Created on December 20, 2002, 3:56 PM
 */

package org.cougaar.tools.robustness.audit.msgAudit;

/**
 *
 * @author  Administrator
 */
public class LogPointLevel {
    
    String ID;
    String NAME;
    int SEQ;
    boolean SEND = false;
    
    boolean fromConfig = false;

    
    /** Creates a new instance of LogPointLevel */
    public LogPointLevel(String _id, int _seq, boolean _send, String _vizName) {
        
        ID   = _id;
        NAME = _vizName;
        SEND = _send;
        SEQ = _seq;
    }
    
    /** Creates a new instance of LogPointLevel */
    public LogPointLevel(String _id, String _seq, String _send, String _vizName) {
        
        ID   = _id;
        NAME = _vizName;
        if (_send.equalsIgnoreCase("TRUE"))
            SEND = true;
        else if (_send.equalsIgnoreCase("FALSE"))
            SEND = false;
        else {
            System.out.println("*** While Adding LogPointLevel, found invalid or missing SEND value: " + _send);
            System.out.println("    Setting to false... just to irritate you.");
        }    
        
        try {            
            SEQ = Integer.parseInt(_seq);
        } catch ( java.lang.NumberFormatException nfe ) {
            System.out.println("*** While Adding LogPointLevel, found invalid or missing seq num: " + _send);
            System.out.println("    Setting to false... just to irritate you.");
        }
    }
    
    public String logPointName() { return ID; }
    public String userName() { return NAME; }
    public int seq() { return SEQ; }
    public void setSeq(int _i) { SEQ = _i; }
    public boolean isSend() { return SEND; }

    public String toString() { return NAME + "  " + ID  + "  " + SEQ + "  " + SEND; }
    
    /*
     * @return TRUE if this LogPoint was loaded from the config file.
     */
    public boolean fromConfig() { return fromConfig; }
    public void setFromConfig(boolean _b) { fromConfig = _b; }
    
}
