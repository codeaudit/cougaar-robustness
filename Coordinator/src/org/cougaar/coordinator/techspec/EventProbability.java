/*
 * EventProbability.java
 *
 * Created on March 26, 2004, 4:22 PM
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
    
    
    /** Creates a new instance of EventProbability */
    public EventProbability() {
        intervals = new Vector();
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
        
        if (intervals.size() == 0) { return 0.0; } // no probability
        
        //break interval up into minutes
        long interval = end - start;
        if (interval < 0) {
            throw new NegativeIntervalException();
        } else if (interval == 0) {
            return 0.0; //zero length interval
        }

        //Convert millisecond interval to minutes, round up, & cast to int.
        int durationInMins = (int) java.lang.Math.ceil( (interval / 60000) );
                
        //See if there is an infinite interval probability. If so, we don't need to break up
        //the entire interval into sub-intervals.
        if (infiniteIntervalProbability != null) {
            return infiniteIntervalProbability.computeIntervalProbability(durationInMins);
        }
        
        GregorianCalendar startTime = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        startTime.setTimeInMillis(start);

        GregorianCalendar endTime = (GregorianCalendar)startTime.clone();
        endTime.add(Calendar.MINUTE, durationInMins); // find ending time
        int durHrs = durationInMins / 60;
        ClockInterval[] clockIntervals = ClockInterval.generateIntervals(startTime, endTime, durHrs);
        
        //Otherwise figure out how many interval probabilities will be used.
        EventProbabilityInterval epi;
        double cumulativeProb = 1.0;
        Iterator i = intervals.iterator();
        while (i.hasNext()) {
            
            epi = (EventProbabilityInterval) i.next();
            int minutes = ClockInterval.computeOverlap(clockIntervals, epi.getClockIntervals() );
            cumulativeProb = cumulativeProb * epi.computeIntervalProbability(minutes);
        }
        
        return 1.0 - cumulativeProb;
    }

    
    
    
    
}

