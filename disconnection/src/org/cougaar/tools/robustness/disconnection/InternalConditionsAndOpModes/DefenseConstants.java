/*
 * DefenseConstants.java
 *
 * Created on April 7, 2003, 1:21 PM
 * * 
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

package org.cougaar.tools.robustness.disconnection.InternalConditionsAndOpModes;

import org.cougaar.core.adaptivity.OMCPoint;
import org.cougaar.core.adaptivity.OMCRangeList;

/**
 *
 */

public class DefenseConstants {
    
    public DefenseConstants() {}
    
    public static final OMCPoint MON_ENABLED = new OMCStrMonPoint("ENABLED");
    public static final OMCPoint MON_DISABLED = new OMCStrMonPoint("DISABLED");
    public static final OMCPoint[] MON_VALUE_LIST = {MON_ENABLED, MON_DISABLED}; 
    public static final OMCRangeList MON_RANGELIST = new OMCRangeList (MON_VALUE_LIST);
    
   
    public static final OMCStrBoolPoint BOOL_TRUE  = new OMCStrBoolPoint("TRUE");
    public static final OMCStrBoolPoint BOOL_FALSE = new OMCStrBoolPoint("FALSE");
    public static final OMCPoint[] BOOL_VALUE_LIST = {BOOL_TRUE, BOOL_FALSE};    
    public static OMCRangeList BOOL_RANGELIST = new OMCRangeList (BOOL_VALUE_LIST);
    
    public static final OMCPoint DISCONNECT_ALLOWED = new OMCStrDefPoint("DISCONNECT_ALLOWED");
    public static final OMCPoint DISCONNECT_DENIED = new OMCStrDefPoint("DISCONNECT_DENIED");
    public static final OMCPoint CONNECT_ALLOWED = new OMCStrDefPoint("CONNECT_ALLOWED");
    public static final OMCPoint CONNECT_DENIED = new OMCStrDefPoint("CONNECT_DENIED");
    public static final OMCPoint DEF_PEND = new OMCStrDefPoint("PEND");
    public static final OMCPoint DEF_PENDTIME = new OMCStrDefPoint("PENDTIME");
    public static final OMCPoint DEF_PREPARE = new OMCStrDefPoint("PREPARE");
    public static final OMCPoint[] DEF_VALUE_LIST = {DEF_PEND, DEF_PENDTIME, DEF_PREPARE, DISCONNECT_ALLOWED, DISCONNECT_DENIED, CONNECT_ALLOWED, CONNECT_DENIED};
    public static final OMCRangeList DEF_RANGELIST = new OMCRangeList (DEF_VALUE_LIST);
    
    // XXX: Dummy values referenced by msglog
    public static final OMCPoint DEF_ENABLED = new OMCStrMonPoint("DEF_ENABLED");
    public static final OMCPoint DEF_DISABLED = new OMCStrMonPoint("DEF_DISABLED");
    
    
    
    /** tiny helper class for Operating Modes */
    public static class OMCStrMonPoint extends OMCPoint {
        protected OMCStrMonPoint (String a) { super (a); }
    }

    /** tiny helper class for Operating Modes */
    public static class OMCStrDefPoint extends OMCPoint {
        protected OMCStrDefPoint (String a) { super (a); }
    }
    
    
    /** tiny helper class for Boolean Operating Modes */
    public static class OMCStrBoolPoint extends OMCPoint {
        protected OMCStrBoolPoint (String a) { super (a); }
    }
    
    
}
