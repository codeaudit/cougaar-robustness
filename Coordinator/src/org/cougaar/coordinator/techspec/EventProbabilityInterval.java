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

/**
 *
 * @author  Paul Pazandak, Ph.D, OBJS
 *
 * Defines the threat likelihood for a specific interval of time. Currently,
 * definable for day, night or always. If always is used, that precludes using
 * day or night (but two such intervals, one with day & the other with night is
 * allowed).
 */
public class EventProbabilityInterval implements NotPersistable {
    
    private static int G_OID = 0;

    private int startTime = -1;
    private int intervalLength = 0;
    private float prob = 0;
    private int oid; //used by the servlet gui

    private Logger logger;
    
    /** Creates a new instance of EventProbabilityInterval */
    public EventProbabilityInterval(int startTime, int intervalLength, float probability) {

        this.startTime = startTime;
        this.intervalLength = intervalLength;
        this.prob = probability;
        
        logger = Logging.getLogger(this.getClass().getName());
        
    }

    /** Creates a new instance of EventProbabilityInterval that is ALWAYS applicable (no specified interval) */
    public EventProbabilityInterval(float probability) {

        this.prob = probability;
        
        logger = Logging.getLogger(this.getClass().getName());
        
    }
    
 
    /** Set the oid of this object */
    private void setOID(int oid) { this.oid = oid; }
    
    /** Set the oid of this object */
    public int getOID() { return this.oid; }
    
    /** @return the probability of this object */
    public float getProbability() { return prob; }

    /** Set the probability of this object */
    public void setProbability(float p) { this.prob = p; }
    
    /** @return the computed probability of this object for the interval specified */
    public double computeIntervalProbability(long start, long end) { 
        
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
        
        return result;
    }
    
    public String toString() {
     
        if (this.startTime == -1) { // then this is an "all the time" interval"
            return "Constant Probability = " + this.getProbability();
        } else {
            return "Interval Probability = " + this.getProbability() + "[startTime=" + this.startTime + 
                                           ", durationHrs=" + this.intervalLength;
        }
    }
    
}
