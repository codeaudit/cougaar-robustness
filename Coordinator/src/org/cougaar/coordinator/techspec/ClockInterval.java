/*
 * ClockInterval.java
 *
 * Created on April 30, 2004, 5:16 PM
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
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

/**
 *
 * @author  Administrator
 */
public class ClockInterval {
    
    int hr, startMin, endMin; //, overlap;
    boolean fullInterval = false;
    static protected Logger logger;

    static {
        logger = Logging.getLogger(ClockInterval.class); 
    }        
    
    /** Creates a new instance of ClockInterval */
    public ClockInterval(int hr, int startMin, int endMin) {

        this.hr = hr;
        this.startMin = startMin;
        this.endMin = endMin;
        //overlap = 0;
        if (startMin==0 && endMin==60) {
            fullInterval = true;
        }
    }
        
    public String toString() {
     
        return "Start Time: "+ hr + ":" + startMin + "[end min = " + endMin + "], full interval = " + (fullInterval? "TRUE":"FALSE" + "\n");
    }
    
    /** 
     *  intervalLength in hours
     */
    public static ClockInterval[] generateIntervals(Calendar startTime, Calendar endTime, int intervalLength) {
                
        int endHr = endTime.get(Calendar.HOUR_OF_DAY);
        int finalMin = endTime.get(Calendar.MINUTE);
        int startHr = startTime.get(Calendar.HOUR_OF_DAY);
        int startMin = startTime.get(Calendar.MINUTE);
        
        if (logger.isDebugEnabled()) { logger.debug(">>generateIntervals(): Current Interval start hr= "+startHr + ">>End interval hr= "+endHr+", #intervals="+intervalLength); }
                
        //Build up array of clockIntervals to compare against
        ClockInterval[] clockIntervals = new ClockInterval[intervalLength]; //in case it's less than an hour!
        
        //Init
        //# of minutes in the first hour's interval, e.g. startTime = 1815, endMin = 45 minutes
        int sMin = 0;
        int eMin = 0; 

        GregorianCalendar tempTime = (GregorianCalendar)startTime.clone();
        if (logger.isDebugEnabled()) { logger.debug(">>generateIntervals(): Cloned start hr= "+tempTime.get(Calendar.HOUR_OF_DAY) ); }

        //Populate the interval
        int t = startHr;
        for (int i=0; i< intervalLength; i++) {
            
            sMin = 0; //default start minute
            eMin = 60; //default end minute
            
            if (i == 0) { 
                sMin = startMin; //starting minute the first hour's interval
                                 //e.g. startTime = 1815, sMin = 15 minutes
            }
            //no ELSE here -- it could be both the first AND last interval
            //Check if this is the last interval 
            if (i == (intervalLength - 1 ) ) { 
                eMin = finalMin; //ending minute in the last hour's interval
                                 //e.g. endTime = 1845, startMin = 45 minutes
            }

            clockIntervals[i] = new ClockInterval(t, sMin, eMin);

            if (logger.isDebugEnabled()) { logger.debug(">>Computing Clock Interval start hr= "+t); }

            tempTime.add(Calendar.HOUR_OF_DAY, 1); //add one hour
            t = tempTime.get(Calendar.HOUR_OF_DAY);
        }
        
        return clockIntervals;
    }        
    
    
    /** 
     * Computes the number of minutes that the intervals overlap.
     * Finds all common hours in each interval & then adds the minutes in each that
     * they actually overlap. This is a simplisitic, non-optimized approach & assumes that the
     * probability interval does not exceed a 24 hour length.
     */
    protected static int computeOverlap(ClockInterval[] currentIntervals, ClockInterval[] probIntervals, int durationHrs) {
    
        
        ClockInterval cI, pI;
        int totalMin = 0;
        
        if (logger.isDebugEnabled()) { logger.debug(">>Current Intervals length= "+currentIntervals.length + ">>Prob Intervals length= "+probIntervals.length); }
        
        //Look for matching interval
        for (int i = 0; i<currentIntervals.length; i++) {

            cI = currentIntervals[i];
            if (logger.isDebugEnabled()) { logger.debug(">>Current Interval ["+i+"]: "+cI.toString()); }
            
            //Look for matching interval
            for (int j = 0; j<probIntervals.length; j++) {
            
                pI = probIntervals[j];
                if (logger.isDebugEnabled()) { logger.debug(">> Comparing prob interval "+pI.hr+" to current interval "+cI.hr); }
        
                if (pI.hr == cI.hr) {
                    int min = findMinuteOverlap( cI, pI);
                    totalMin += min;
                    if (logger.isDebugEnabled()) { logger.debug(">> In ClockInterval "+pI.hr+", Min overlap = "+min); }
                    if (durationHrs <23) { // then we won't see this interval again, so stop looking
                        if (logger.isDebugEnabled()) { logger.debug(">> In ClockInterval. Less than 24 hr interval, so stop looking for more matches. "); }
                        break; //skip to next currentInterval
                    }
                }                
            }
        }
        
        return totalMin;
    }

    
    private static int findMinuteOverlap(ClockInterval int1, ClockInterval int2) {
    
        if (int1.fullInterval && int2.fullInterval) {
         
            return 60;
            
        } else if (int1.fullInterval) { // return the # of active minutes in int2;
            
            return ( (60 - int2.startMin) - (60 - int2.endMin) );
            
        } else if (int2.fullInterval) { // return the # of active minutes in int1;
         
            return ( (60 - int1.startMin) - (60 - int1.endMin) );
            
        } else {
            //find minutes in common
            int count = 0;
            for (int i=0; i<60; i++) {
                
                if ( ( i >= int1.startMin && i <= int1.endMin ) &&
                     ( i >= int2.startMin && i <= int2.endMin ) ) {
                        
                     count++;
                }
            }
            return count;            
        }
        
        
    }
    
}
