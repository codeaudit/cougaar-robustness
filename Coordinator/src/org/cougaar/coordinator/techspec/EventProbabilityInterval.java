/*
 * EventProbabilityInterval.java
 *
 * Created on March 29, 2004, 4:45 PM
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

import JSci.maths.statistics.PoissonDistribution;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;
import org.cougaar.core.persist.NotPersistable;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 *
 * @author  Paul Pazandak, Ph.D, OBJS
 *
 * Defines the threat likelihood for a specific interval of time. 
 */
public class EventProbabilityInterval implements NotPersistable {

    //This is the assume interval over which a probability of occurrence is specified in the tech specs.
    private static float G_PROB_LENGTH_IN_MINUTES = 60.0F;
    
    private static int G_OID = 0;

    private int startHr = -1;
    private int startMin = -1;
    private int intervalLength = 0;
    private float prob = 0;
    private float minuteProbOfNotOccuring = 0;
    
    private int oid; //used by the servlet gui
    private ClockInterval[] clockIntervals;
    
    private Logger logger;

    static {

        Logger logger = Logging.getLogger(EventProbabilityInterval.class.getName());
        if (logger.isDebugEnabled()) { logger.debug("Assuming interval length of techspec probabilities are "+G_PROB_LENGTH_IN_MINUTES+" minutes."); }
        
    }
    
    /** 
     * Creates a new instance of EventProbabilityInterval, with a specified applicability interval 
     * over some clock period. startTime should be in the range from 0001-2400 (24 hr clock). intervalLength
     * (in HOURS) should be less than 24 hours & the interval must not overlap itself.
     */
    public EventProbabilityInterval(int startHr, int startMin, int intervalLength, float probability) {

        this.startHr = startHr;
        this.startMin = startMin;
        this.intervalLength = intervalLength;
        this.prob = probability;
        this.minuteProbOfNotOccuring = (float) java.lang.Math.pow((1.0-prob), 1d/60d );
        
        logger = Logging.getLogger(this.getClass().getName());
        
        
        GregorianCalendar start = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        start.set(Calendar.HOUR_OF_DAY, startHr);
        start.set(Calendar.MINUTE, startMin);
        
        GregorianCalendar end = (GregorianCalendar)start.clone();
        end.add(Calendar.HOUR_OF_DAY, intervalLength);

        //Build up array of clockIntervals to compare against
        if (logger.isDebugEnabled()) { logger.debug(">>EventProbabilityInterval init(): creating interval with start time hr= "+start.get(Calendar.HOUR_OF_DAY)+":"+ start.get(Calendar.MINUTE)+ ">>End time= "+end.get(Calendar.HOUR_OF_DAY)+":"+end.get(Calendar.MINUTE) + ", IntervalLength="+intervalLength+" -- minuteProbOfNotOccuring="+minuteProbOfNotOccuring); }
        clockIntervals = ClockInterval.generateIntervals(start, end, intervalLength);
        
    }

    /** Creates a new instance of EventProbabilityInterval that is ALWAYS applicable (infinite interval) */
    public EventProbabilityInterval(float probability) {

        this.prob = probability;
        this.minuteProbOfNotOccuring = (float)java.lang.Math.pow((1.0-prob), 1d/60d );
        
        logger = Logging.getLogger(this.getClass().getName());
        if (logger.isDebugEnabled()) { logger.debug(">>EventProbabilityInterval static init(): -- probOfOccurring="+prob+", minuteProbOfNotOccuring="+minuteProbOfNotOccuring); }
        
    }

    /**
     * @return the ClockIntervals for this probability
     */
    protected ClockInterval[] getClockIntervals() { return clockIntervals; }
    
    /**
     * Use startTime & intervalLength to determine the applicability of this EventProbabilityInterval,
     * then use computeIntervalProbability to compute the probability over the qualifying interval.
     *
     *@return the start time of this interval probability, -1 if infinite interval
     */
    public int getStartTime() { return this.startHr; }

    /**
     *@return the length (in hours) of this interval probability, 0 if an infinite length.
     */
    public int getIntervalLength() { return this.intervalLength; }
        
    
    /** @return the probability of occurrence over the specified interval. Unit time probability in minutes.*/
    public float getProbability() { return prob; }

    /** Set the probability of this object */
   // public void setProbability(float p) { this.prob = p; }
    
    /** @return the computed, weighted probability of NOT OCCURRING for the interval specified */
    public double computeIntervalProbability(int intervalMinutes) {         
                
        return java.lang.Math.pow(minuteProbOfNotOccuring, (double)intervalMinutes);        
    }
    
    public String toString() {
     
        if (this.startHr == -1) { // then this is an "all the time" interval"
            return "Constant Probability = " + this.getProbability();
        } else {
            String s= "\nClockIntervals:";
            for (int i=0; i< clockIntervals.length; i++) {
             
                s += "\n     " + clockIntervals[i].toString();
            }
            return "Interval Probability: " + this.getProbability() + "[startTime=" + this.startHr + 
                                           ", durationHrs=" + this.intervalLength + s;
        }
    }

    
/*
         long interval = end - start;
        double x = (double)intervalLength / (double)interval ;
        double lambda = prob / (x);
        
        logger.debug("poisson for lambda = " + lambda);
        logger.debug("poisson for prob = " + prob);
        logger.debug("poisson for x = " + x);
        logger.debug("poisson for intervalLength = " + intervalLength);
        logger.debug("poisson for interval = " + interval);
        
        PoissonDistribution pd = null;
        try {
             pd = new PoissonDistribution(lambda);
        } catch (Exception e) { 
            logger.error("Exception generating poisson probability. Returning probability = 0.", e);
        }
        
        double result = 0.0;
        
        try {
            //Get probability of all occurences > 0
            result = 1 - pd.probability(0);
        } catch (Exception e) { 
            logger.error("Exception generating poisson probability. Returning probability = 0.", e);
        }
        
        if (result == 0.0) { return 0.0; }
        //Return weighted probability
        return result * ( (end-start) / entireInterval );
*/    
    
   /* keep until sure its not needed
        int fromHr = cal.get(Calendar.HOUR_OF_DAY);
        int fromMin = date.get(Calendar.MINUTE);

        //Init
        int minIn = 0;
        int minOut = (fromMin == 0) ? 0 : 60-fromMin; //# of minutes in the first hour's interval
                                                      //e.g. startTime = 1815, minOut = 45 minutes
        
        //Populate the interval
        int t = fromHr;
        for (int i=0, t=fromHr; i< intervalLength; i++) {
            
            //Check if this is the last interval
            if (i == (intervalLength - 1 ) { 
            //recompute minIn & minOut
        
                minIn = fromMin; //# of minutes in the last hour's interval
                                 //e.g. startTime = 1815, minIn = 15 minutes
                minOut = 0;
            }

            clockIntervals[i] = new ClockInterval(t, minIn, minOut, false);
            cal.add(Calendar.HOUR_OF_DAY, 1); //add one hour
            t = cal.get(Calendar.HOUR_OF_DAY);
            minIn = 0;
            minOut = 0;
            
        }
    */     
}
