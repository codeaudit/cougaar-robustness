/*
 * BadWeightsException.java
 *
 * Created on May 13, 2004, 12:52 PM
 */

package org.cougaar.coordinator.costBenefit;

/**
 *
 * @author  David Wells - OBJS
 * @version 
 */
public class BadWeightsException extends Exception {

    double securityWeight;
    double completenessWeight;
    double timelinessWeight;

    /** Creates new BadWeightsException */
    public BadWeightsException(double security, double completeness, double timeliness) {
        this.securityWeight = securityWeight;
        this.completenessWeight = completenessWeight;
        this.timelinessWeight = timelinessWeight;
    }

    public String toString() {
        return "MAU Weights must sum to 1.0. [securityWeight="+securityWeight+", completenessWeight="+completenessWeight+", timelinessWeight="+timelinessWeight+"]";
    }

}
