/*
 * ThreatProbabilityInterval.java
 *
 * Created on April 29, 2004, 3:21 PM
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
 * @deprecated
 * Defines the threat likelihood for a SPECIFIC interval of time. It's
 * based upon values from ThreatDescriptions & specifically from VulnerabilityFilters.
 */
public class ThreatProbabilityInterval implements NotPersistable {
    
    private int intervalLength;
    private double prob;
    private long start;
    private long end;
    
    private Logger logger;
    
    /** Creates a new instance of ThreatProbabilityInterval */
    public ThreatProbabilityInterval(long start, long end, double probability, int intervalLength) {
        
        this.intervalLength = intervalLength;
        this.prob = probability;
        this.start = start;
        this.end = end;
        
        //this.oid = G_OID++;
        
        logger = Logging.getLogger(this.getClass().getName());
        
    }    
    
    /** @return the poisson probability interval length of this object */
    public int getIntervalLength() { return intervalLength; }
    
    /** Set the oid of this object */
    //private void setOID(int oid) { this.oid = oid; }
    
    /** Set the oid of this object */
    //public int getOID() { return this.oid; }
    
    /** @return the probability of this object */
    public double getProbability() { return prob; }

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
    
}