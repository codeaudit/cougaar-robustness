/*
 * ClockInterval.java
 *
 * Created on April 30, 2004, 5:16 PM
 * 
 * <copyright>
 * 
 *  Copyright 2004 Object Services and Consulting, Inc.
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
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

/**
 *
 * @author  Administrator
 */
public class ClockInterval {
    
    int hr, startMin, endMin, startSec, endSec; //, overlap;
    boolean fullInterval = false;
    static protected Logger logger;

    static {
        logger = Logging.getLogger(ClockInterval.class); 
    }        
    
    /** Creates a new instance of ClockInterval */
    public ClockInterval(int hr, int startMin, int endMin, int startSec, int endSec) {

        this.hr = hr;
        this.startMin = startMin;
        this.endMin = endMin;
        //overlap = 0;
        if (startMin==0 && endMin==60) {
            fullInterval = true; //ignore seconds accuracy if we have a full 60 minutes
        }

        this.startSec = startSec;
        this.endSec = endSec;
        
        
    }
        
    public String toString() {
     
        return "Start Time: "+ hr + ":" + startMin + "[end min = " + endMin + "], full interval = " + (fullInterval? "TRUE":"FALSE" + "\n");
    }
    
    /** 
     *@param intervalLength  intervalLength in hours
     */
    public static ClockInterval[] generateIntervals(Calendar startTime, Calendar endTime, int intervalLength) {
                
        int endHr = endTime.get(Calendar.HOUR_OF_DAY);
        int finalMin = endTime.get(Calendar.MINUTE);
        int finalSec = endTime.get(Calendar.SECOND);
        
        int startHr = startTime.get(Calendar.HOUR_OF_DAY);
        int startMin = startTime.get(Calendar.MINUTE);
        int startSec = startTime.get(Calendar.SECOND);
        
        if (logger.isDebugEnabled()) { logger.debug(">>generateIntervals(): Current Interval start hr= "+startHr + ">>End interval hr= "+endHr+", #intervals="+intervalLength); }
                
        //Build up array of clockIntervals to compare against
        ClockInterval[] clockIntervals = new ClockInterval[intervalLength]; //in case it's less than an hour!
        
        //Init
        //# of minutes in the first hour's interval, e.g. startTime = 1815, endTime = 20:45
        int sMin = 0; //start Min at start of interval (e.g. 15)
        int eMin = 0; //end Minute at end of interval (e.g. 45)
        int sSec = 0; //start Second at start of interval (e.g. 15)
        int eSec = 0; //end Second at end of interval (e.g. 45)

        GregorianCalendar tempTime = (GregorianCalendar)startTime.clone();
        if (logger.isDebugEnabled()) { logger.debug(">>generateIntervals(): Cloned start hr= "+tempTime.get(Calendar.HOUR_OF_DAY) ); }

        //Populate the interval
        int t = startHr;
        for (int i=0; i< intervalLength; i++) {
            
            sMin = 0; //default start minute
            eMin = 60; //default end minute
            sSec = 0; //default start second
            eSec = 60; //default end second
            
            if (i == 0) { 
                sMin = startMin; //starting minute the first hour's interval
                                 //e.g. startTime = 1815, sMin = 15 minutes
                sSec = startSec; //starting second in the first hour's interval
            }
            //no ELSE here -- it could be both the first AND last interval
            //Check if this is the last interval 
            if (i == (intervalLength - 1 ) ) { 
                eMin = finalMin; //ending minute in the last hour's interval
                                 //e.g. endTime = 1845, startMin = 45 minutes
                eSec = finalSec; //ending second in the last hour's interval
            }

            clockIntervals[i] = new ClockInterval(t, sMin, eMin, sSec, eSec);

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
    protected static double computeOverlap(ClockInterval[] currentIntervals, ClockInterval[] probIntervals, int durationHrs) {
    
        
        ClockInterval cI, pI;
        double totalMin = 0;
        
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
                    double min = findMinuteOverlap( cI, pI);
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

    
    private static double findMinuteOverlap(ClockInterval int1, ClockInterval int2) {
    
        if (int1.fullInterval && int2.fullInterval) {
         
            return 60;
            
        } else if (int1.fullInterval) { // return the # of active minutes in int2;
            
            return ( ((60 - int2.startMin) - (60 - int2.endMin)) + ( (60 - int2.startSec) - (60 - int2.endSec) ));
            
        } else if (int2.fullInterval) { // return the # of active minutes in int1;
         
            return ( ((60 - int1.startMin) - (60 - int1.endMin)) + ((60 - int1.startSec) - (60 - int1.endSec)));
            
        } else {
            //find minutes in common
            double count = 0;
            for (int i=0; i<60; i++) {
                
                if ( ( i >= int1.startMin && i <= int1.endMin ) &&
                     ( i >= int2.startMin && i <= int2.endMin ) ) {
                        
                     count++;
                }
            }

            //find seconds in common
            double secCount = 0;
            for (int i=0; i<60; i++) {
                
                if ( ( i >= int1.startSec && i <= int1.endSec ) &&
                     ( i >= int2.startSec && i <= int2.endSec ) ) {
                        
                     secCount++;
                }
            }
            
            
            return (count + secCount/60d);            
        }
        
        
    }
    
}
