/*
 * EventProbability.java
 *
 * Created on March 26, 2004, 4:22 PM
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

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

import java.util.Vector;
import java.util.Iterator;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Holds the list of interval probabilities for a given threat.
 * It can be composed of several discrete interval probabilities, or one 
 * infinite interval probability.
 *
 * @author  Administrator
 */
public class EventProbability {
    
    Vector intervals;
    EventProbabilityInterval infiniteIntervalProbability = null;
    protected Logger logger;
    
    
    /** Creates a new instance of EventProbability */
    public EventProbability() {
        intervals = new Vector();
        logger = Logging.getLogger(getClass()); 
    }
    
    /**
     * Add a EventProbabilityInterval. If an infinite interval, then it will be the only 
     * one used -- all others will be ignored.
     */
    public void addInterval(EventProbabilityInterval epi) {
        if (epi.getStartTime() == -1) { // this is an infinite interval prob
            infiniteIntervalProbability = epi;
        } else {
            intervals.add(epi);
        }
    }
    
    public String toString() {
     
        String s = "";
        if (infiniteIntervalProbability != null) {
             s = s+ infiniteIntervalProbability + "\n      ";            
        } else {
            Iterator i = this.intervals.iterator();
            while (i.hasNext()) {
                 EventProbabilityInterval ep = (EventProbabilityInterval)i.next();
                 s = s+ ep + "\n      ";
            }        
        }
        return s;
    }
    
    /** @return the computed probability of this object for the interval specified 
     *  Divides the specified interval up as required using the defined event probability intervals,
     *  then computes a over each interval. Returns 0.0 when end = start.
     *
     * start and end are expressed in milliseconds. The minimum interval (end - start) is 1 minute.
     * Any interval  less than this but greater than 0 will be rounded to 1 minute.  
     * Intervals will be rounded up to the next minute.
     *
     * @throws NegativeIntervalException When end-start is negative.
     */
    public double computeIntervalProbability(long start, long end) throws NegativeIntervalException { 
        
        if (intervals.size() == 0 && infiniteIntervalProbability == null) { 
            if (logger.isDebugEnabled()) { logger.debug("No intervals, returning 0.0"); }
            return 0.0; 
        } // no probability
        
        //break interval up into minutes
        long interval = end - start;
        if (interval < 0) {
            throw new NegativeIntervalException();
        } else if (interval == 0) {
            if (logger.isDebugEnabled()) { logger.debug("Interval length is 0, returning 0.0"); }
            return 0.0; //zero length interval
        }

        //Convert millisecond interval to fractional minutes
        double durationInMinFractions = ( (double)interval / 60000d);
//        int durationInMins = (int) java.lang.Math.ceil( (interval / 60000) );

        //Convert to minutes (and fractions of minutes)
//        double durationInMins = (interval / 60000) );
        
        //See if there is an infinite interval probability. If so, we don't need to break up
        //the entire interval into sub-intervals.
        if (infiniteIntervalProbability != null) {
            if (logger.isDebugEnabled()) { logger.debug("computeIntervalProbability(). IntervalInMSECS="+interval+", durationInMinFractions="+durationInMinFractions); }            
            double pr = infiniteIntervalProbability.computeIntervalProbability(durationInMinFractions); //returns prob of NOT occurring during interval
            if (logger.isDebugEnabled()) { logger.debug("Infinite interval, called infiniteIntervalProbability.computeIntervalProbability(). Prob of non-occurrence= "+pr+", prob of occurrence="+(1.0 - pr)); }            
            return (1.0 - pr);
        }
        
        GregorianCalendar startTime = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        startTime.setTimeInMillis(start);
        
        GregorianCalendar endTime = (GregorianCalendar)startTime.clone();
//        endTime.add(Calendar.MINUTE, durationInMins); // find ending time
        endTime.add(Calendar.SECOND, ((int)(end-start)/1000)); // find ending time
        
        //durHrs = the length of the interval in hour units. Any # of minutes > 0 will consume 1 interval, e.g.
        // 1.0 = 1 interval, 1.1 = 2 intervals, 5.3 hours = 6 intervals
        
        int startHr = startTime.get(Calendar.HOUR_OF_DAY);
        int endHr   = endTime.get(Calendar.HOUR_OF_DAY);
        int endMin   = endTime.get(Calendar.MINUTE);
        int endSec   = endTime.get(Calendar.SECOND);
        if (endHr < startHr) { endHr += 24; } // add 24 hours so we can subtract to get interval length
        if ( (endMin > 0) || (endMin ==0 && endSec >0) ) { endHr++; } //it extends into the next hour so add one.
        
        int numHrIntervals = endHr - startHr;
        
        if (logger.isDebugEnabled()) { logger.debug("computeIntervalProbability(). Interval="+interval+", durationInMinFractions="+durationInMinFractions + ", numHrIntervals="+numHrIntervals); }            
        
        if (logger.isDebugEnabled()) { logger.debug("Calculating prob for startTime= "+startTime.get(Calendar.HOUR_OF_DAY)+":"+ startTime.get(Calendar.MINUTE)+ ">>End time= "+endTime.get(Calendar.HOUR_OF_DAY)+":"+endTime.get(Calendar.MINUTE) + ", and occupies "+ numHrIntervals + " intervals"); }                    
        
        ClockInterval[] clockIntervals = ClockInterval.generateIntervals(startTime, endTime, numHrIntervals);
        
        if (logger.isDebugEnabled()) { logger.debug("Number of clock intervals that overlap with threat intervals = "+ clockIntervals.length ); }            
        
        //Otherwise figure out how many interval probabilities will be used.
        EventProbabilityInterval epi;
        double cumulativeProb = 1.0;
        Iterator i = intervals.iterator();
        while (i.hasNext()) {
            
            epi = (EventProbabilityInterval) i.next();
            double minutes = ClockInterval.computeOverlap(clockIntervals, epi.getClockIntervals(), numHrIntervals );
            if (minutes == 0d) continue; // no probability in this interval...
            double prob = epi.computeIntervalProbability(minutes);
            if (logger.isDebugEnabled()) { logger.debug("Minutes in interval="+minutes+", prob = "+prob); }            
            
            cumulativeProb = cumulativeProb * prob;
        }
        if (logger.isDebugEnabled()) { logger.debug("Returning prob of ="+(1.0-cumulativeProb)+" -- cumulativeProb = "+cumulativeProb ); }            
        
        return 1.0 - cumulativeProb;
    }

    
    
    
    
}

