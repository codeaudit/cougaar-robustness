/*
 * DefenseConstants.java
 *
 * Created on April 7, 2003, 1:21 PM
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
    
    public static final OMCPoint DEF_ENABLED = new OMCStrDefPoint("ENABLED");
    public static final OMCPoint DEF_DISABLED = new OMCStrDefPoint("DISABLED");
    public static final OMCPoint DEF_PEND = new OMCStrDefPoint("PEND");
    public static final OMCPoint DEF_PENDTIME = new OMCStrDefPoint("PENDTIME");
    public static final OMCPoint DEF_PREPARE = new OMCStrDefPoint("PREPARE");
    public static final OMCPoint[] DEF_VALUE_LIST = {DEF_PEND, DEF_PENDTIME, DEF_PREPARE, DEF_ENABLED, DEF_DISABLED};
    public static final OMCRangeList DEF_RANGELIST = new OMCRangeList (DEF_VALUE_LIST);
    
    
    
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
