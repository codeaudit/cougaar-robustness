/*
 * XMLMessageProblem.java
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
 * Created on December 30, 2002, 4:26 PM
 */

package org.cougaar.tools.robustness.audit.msgAudit;

/**
 *
 * @author  pazandak@objs.com
 */
public class XMLMessageProblem {
    
    XMLMessage msg;
    String error;
    MessageStackResult msgStack = null;

    static final String line = "___________________________________________________________________\n";
    
    /** Creates a new instance of XMLMessageProblem */
    public XMLMessageProblem(XMLMessage _msg, String _err, MessageStackResult _msgStack) {
        msg = _msg;
        error = _err;
        msgStack = _msgStack;
    }

    /** Creates a new instance of XMLMessageProblem */
    public XMLMessageProblem(XMLMessage _msg, String _err) {
        msg = _msg;
        error = _err;
    }
        
    public String toString() { 
        if (msgStack == null)
            return line + msg + "--> "+ error;
        else
            return line + msg + "--> "+ error + "\n" + msgStack;
    }
    
    public String toString(boolean _toFile, boolean _lvl, int _start, int _end, boolean _wrapOutput) { 
        if (msgStack == null)
            return line + msg + "--> "+ error;
        else
            return line + msg + "--> "+ error + "\n" + 
                   msgStack.toString(_toFile, _lvl, _start, _end, _wrapOutput);
    }
    
}
