/*
 * ThreatMembershipFilter.java
 *
 * Created on September 16, 2003, 4:18 PM
 * 
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

package org.cougaar.coordinator.techspec;

import java.util.Vector;
import java.util.Iterator;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;
import org.cougaar.core.persist.NotPersistable;

/**
 * This class describes a filter that is used to determine membership in a threatModel
 *
 * @author Paul Pazandak, Ph.D. OBJS, Inc.
 */
public class ThreatMembershipFilter implements NotPersistable {
    
    private String propertyName;
    private String value;
    private Object objectValue;
    private String operator;    
    private String valueType;
    private int opCode;
    
    private boolean isString = false;
    private boolean isLong = false;
    private boolean isDouble = false;
    private boolean isInteger = false;
    
    private static final int  EQ = 0;
    private static final int  NEQ = 1;
    private static final int  GT = 2;
    private static final int  LT = 3;
    private static final int  GEQ = 4;
    private static final int  LEQ = 5;
    private static final int  NOP = 6;
    
    /** Logger for error msgs */
    private Logger logger;
    
    
    /** Creates a new instance of ThreatMembershipFilter. 
     * @param valType is I, S, D, L - integer, string, double, long - the type of the value
     */
    public ThreatMembershipFilter(String propertyName, String value, String valType, String operator) {
        
        logger = Logging.getLogger(this.getClass().getName());

        this.propertyName = propertyName;
        this.value = value;
        this.operator = operator;
        this.opCode = genOpCode(operator);
        
        if (valType == null) { valType = "S"; } //assign a default
        this.valueType = valType.substring(0,0); //in case xml used full name, e.g. 'Integer'
        if (valueType.equalsIgnoreCase("S")) {
            isString = true;
            objectValue = value;
        } else {
            objectValue = convertValue(valueType, value);
        }
        
    }
    
    public String getPropertyName() { return propertyName; }
    public String getValue() { return value; }
    public String getOperator() { return operator; }
    
    public String toString() {

        return "PropertyName="+propertyName+ "  value="+value+"   operator="+operator;
    }
    
    /**
     * Looks for the FIRST property of the asset with the same name as this filter,
     * where the types of both values match.
     * If found, the asset's property value is compared to this filter's value 
     * using the filter's operator.
     *
     *@return TRUE if the comparison operator returns TRUE. O.w. false.
     */
    public boolean qualifies(AssetTechSpecInterface asset) {
    
        Vector aProps = asset.getProperties();
        //If the asset has no properties, it cannot qualify
        if (aProps == null || aProps.size() == 0 ) {
            return false;
        }
        
        AssetProperty p;
        //Now find the property - both property name & value types must match.
        Iterator list = aProps.iterator();
        while (list.hasNext()) {
             
            p = (AssetProperty)list.next();
            if (p.getName().equalsIgnoreCase(this.propertyName)) { //found!
                if (p.getValue().getClass() == this.objectValue.getClass()) { //types match, so we can compare!
                    return compare(p.getValue());
                } else {
                    logger.warn("Threat Membership: type of asset property ["+p.getValue().getClass()+"] does not match the supplied filter's type ["+this.objectValue.getClass()+"]. Ignoring.");
                }
            }
        }
        return false;
    }
    
    private boolean compare(Object val) {
        
        try {
            int result = ((Comparable)val).compareTo(this.objectValue); 
            switch(this.opCode) {

                case EQ:
                    return result == 0;
                case NEQ:
                    return result != 0;
                case GT:
                    return result > 0; 
                case LT:
                    return result < 0;
                case GEQ:
                    return result >= 0;
                case LEQ:
                    return result <= 0;

                default:
                    return false;

            }
        } catch (Exception e) {
            logger.error("Exception: Could not compare values.",e);
        }
        
        return false;
    }
    
    
    /** @return a comparison code representing the string value */
    private int genOpCode(String op) {
        
        if (op.equals("=")) return EQ;
        if (op.equals("!=")) return NEQ;
        if (op.equals(">")) return GT;
        if (op.equals("<")) return LT;
        if (op.equals(">=")) return GEQ;
        if (op.equals("<=")) return LEQ;
        return NOP;
    }
    
    
    private Object convertValue(String valType, String value) {
        
        try {
            if (valType.equalsIgnoreCase("L")) {
                isLong = true;
                return Long.valueOf(value);    
            }
            if (valType.equalsIgnoreCase("D")) {
                isDouble = true;             
                return Double.valueOf(value);
            }
            if (valType.equalsIgnoreCase("I")) {
                isInteger = true;             
                return Integer.valueOf(value);
            }
        } catch (Exception e) {
            logger.error("Could not convert membership filter value [" + value + "] to type "+valType + ". Resetting to type String.", e);
        }
        
        isString = true;
        isLong = false;
        isDouble = false;
        isInteger = false;            
        this.valueType = "S";
        return value;        
    }
}
