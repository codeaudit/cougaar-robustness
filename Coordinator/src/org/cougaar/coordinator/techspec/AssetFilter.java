/*
 * AssetFilter.java
 *
 * Created on March 26, 2004, 4:23 PM
 * <copyright>
 *  Copyright 2003 Object Services and Consulting, Inc.
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA)
 *  and the Defense Logistics Agency (DLA).
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
 */

package org.cougaar.coordinator.techspec;

import java.util.Vector;
import java.util.Iterator;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;
import org.cougaar.core.persist.NotPersistable;

/**
 *
 * @author  Administrator
 */
public class AssetFilter implements NotPersistable {

    private static final int  EQ = 0;
    private static final int  NEQ = 1;
    private static final int  GT = 2;
    private static final int  LT = 3;
    private static final int  GEQ = 4;
    private static final int  LEQ = 5;
    private static final int  NOP = 6;
    
    private Vector terms;
    private Vector specialTerms;
    
    /** Creates a new instance of AssetFilter */
    public AssetFilter() {
        
        logger = Logging.getLogger(this.getClass().getName());
        terms = new Vector(2,2); //not expected to be large
        specialTerms = new Vector(2,2); //not expected to be large        
    }
    
    /**
     * Add a term to filter on
     */
    public void addTerm(String propertyName, String value, String valType, String operator) {        
        terms.add( new Term( propertyName, value, valType, operator) );        
    }
    

    /**
     * Add CorruptHostTerm to filter on
     */
    public void addCorruptHostTerm(String value) {        
        specialTerms.add( new CorruptHostTerm( value ) );        
    }


    /**
     * Add CorruptHostOnNetworkTerm to filter on
     */
    public void addCorruptHostOnNetworkTerm(String value) {        
        specialTerms.add( new CorruptHostOnNetworkTerm( value ) );        
    }

    

    /** Logger for error msgs */
    private Logger logger;
    
    /**
     * Looks for the FIRST property of the asset with the same name as this filter,
     * where the types of both values match.
     * If found, the asset's property value is compared to this filter's value 
     * using the filter's operator.
     *
     * Compare against all terms in this AssetFilter.
     *
     *@return TRUE if the comparison operator returns TRUE. O.w. false.
     */
    public boolean qualifies(ThreatModelManagerPlugin mgr, AssetTechSpecInterface asset) {
    
        if (terms.size() == 0) return true; // no filters, so every asset qualifies

        Vector assetProps = asset.getProperties();
        //If the asset has no properties, it cannot qualify
        if (assetProps == null || assetProps.size() == 0 ) {
            return false;
        }
        
        boolean qualifies = true;
        for (Iterator termsIter = terms.iterator(); termsIter.hasNext() && qualifies; ) {
         
            qualifies = qualifyTerm((Term)termsIter.next(), assetProps);            
            
        }

        //compare against any special filters that exist
        //Specifically, CorruptHostExistsOnNetwork and CorruptHostExists filters
        if (qualifies && specialTerms.size()>0) {
            Iterator i = this.specialTerms.iterator();
            while (i.hasNext()) {
                SpecialTerm st = (SpecialTerm) i.next();
                qualifies = st.qualifies(mgr, asset);
                if (!qualifies) { break; } // no need to continue
            }
        }   
        
        return qualifies;
        
    }

    
    /** 
     * Look for first asset property that matches the term & compare.
     * @return TRUE if term matches asset property 
     */
    private boolean qualifyTerm(Term term, Vector assetProps) {     

        AssetProperty p;
        //Now find the property - both property name & value types must match.
        Iterator list = assetProps.iterator();
        while (list.hasNext()) {
             
            p = (AssetProperty)list.next();        
        
            if (p.getName().equalsIgnoreCase(term.propertyName)) { //found!
                if (p.getValue().getClass() == term.objectValue.getClass()) { //types match, so we can compare!
                    if (logger.isDebugEnabled()) logger.debug("AssetFilter.qualifyTerm: comparing asset property "+p.getName()+"["+p.getValue()+"] with filter value = ["+term.getValue()+"]. ");
                    return term.compare(p.getValue());
                } else {
                    logger.warn("AssetFilter: type of asset property ["+p.getValue().getClass()+"] does not match the supplied filter's type ["+term.objectValue.getClass()+"]. Ignoring.");
                }
            }
        }
        return false;
    }
    
    
    public String toString() {
     
        String s = "    AssetFilter\n";
        Iterator i = this.terms.iterator();
        while (i.hasNext()) {
             Term t = (Term)i.next();
             s = s+ "     - " + t + "\n";
        }        
        i = this.specialTerms.iterator();
        while (i.hasNext()) {
             SpecialTerm st = (SpecialTerm)i.next();
             s = s+ "     - " + st + "\n";
        }        
        return s;
        
    }
    
    
    class Term {

         String propertyName;
         String value;
         Object objectValue;
         String operator;    
         String valueType;
         int opCode;

         boolean isString = false;
         boolean isLong = false;
         boolean isDouble = false;
         boolean isInteger = false;

        
        /** Creates a new instance of Term. 
         * @param valType is I, S, D, L - integer, string, double, long - the type of the value
         */
        public Term(String propertyName, String value, String valType, String operator) {

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

            return "TERM: PropertyName="+propertyName+ "  value="+value+"   operator=\""+operator + "\" valueType="+ valueType;
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

            if (op.equals("EQ")) return EQ;
            if (op.equals("NE")) return NEQ;
            if (op.equals("GT")) return GT;
            if (op.equals("LT")) return LT;
            if (op.equals("GE")) return GEQ;
            if (op.equals("LE")) return LEQ;
            return NOP;
        }
        
    }
    
    /////////////////////////////////////////////////////////////////////////////////////////////////////
    
    // SPECIAL CASE FILTERS
    
    interface SpecialTerm {
     
        public boolean qualifies(ThreatModelManagerPlugin mgr, AssetTechSpecInterface asset);
        public String toString();
    }

    
    /**
     * @return true if the asset (host) is corrupt, false otherwise.
     * Returns false if not a host.
     */
    class CorruptHostTerm implements SpecialTerm {
        
        String value;
        CorruptHostTerm(String value) {
            this.value = value;
        }

        public boolean qualifies(ThreatModelManagerPlugin mgr, AssetTechSpecInterface asset) {
            
            if (logger.isDebugEnabled()) logger.debug("AssetFilter.SpecialTerm - Looking for CorruptHost for this asset");
            //Need to get the host for this asset, then narrow to all hosts that are on the same network as this asset
            //If this asset IS a host, then do nothing.

            if ( asset.getAssetType() != AssetType.HOST ) { return false; }
            
            //As above, look for diagnosis on this asset from security that says that this host is believed to be corrupt
            logger.error("NOT IMPLEMENTED FULLY! Waiting for security to implement diagnosis to look for.");

            AssetProperty ap = asset.findPropertyByName("CORRUPTHOST");
            if (ap != null && ap.equals("TRUE") ) {
                return true;
            } else {
                return false;
            }
            
        }
        
        public String toString() {
            return "CorruptHostTerm value="+this.value;
        }
    }
    
    /**
     *
     */
    class CorruptHostOnNetworkTerm implements SpecialTerm {
        
        String value;
        CorruptHostOnNetworkTerm(String value) {
            this.value = value;            
        }

        public boolean qualifies(ThreatModelManagerPlugin mgr, AssetTechSpecInterface asset) {
            
            if (logger.isDebugEnabled()) logger.debug("AssetFilter.SpecialTerm - Looking for CorruptHostOnNetwork");
            //Need to get a list of ALL hosts...
            //At this point all hosts we see will be on this network.

            //We may need a sensor to watch for such diagnoses & then upon seeing one, modify the 
            //ALL host assets so their membership gets reevaluated??? CORRUPTHOSTONNETWORK = TRUE

            if ( asset.getAssetType() != AssetType.HOST ) { return false; }
            
            //As above, look for diagnosis on this asset from security that says that this host is believed to be corrupt
            logger.error("NOT IMPLEMENTED FULLY! Waiting for security to implement diagnosis to look for.");

            AssetProperty ap = asset.findPropertyByName("CORRUPTHOSTONNETWORK");
            if (ap != null && ap.equals("TRUE") ) {
                return true;
            } else {
                return false;
            }
            
        }

        public String toString() {
            return "CorruptHostOnNetworkTerm value="+this.value;
        }
        
    }
    
    
}
