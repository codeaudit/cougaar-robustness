/*
 * MAUWeights.java
 *
 * Created on April 24, 2004
 * <copyright>
 *  Copyright 2004 Telcordia Technoligies, Inc.
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

package org.cougaar.coordinator.believability;

/**
 * This is the common class that encapsulates the MAU weight information
 * for the believability plugin.
 **/

public class MAUWeights {
    public static int COMPLETENESS =1;
    public static int SECURITY = 2;
    public static int TIMELINESS = 3;

    public static double DEFAULT_COMPLETENESS = 0.4;
    public static double DEFAULT_SECURITY = 0.3;
    public static double DEFAULT_TIMELINESS = 0.3;

    /**
     * Empty constructor. Uses default MAU weights
     **/
    public MAUWeights() {
        updateMAU( DEFAULT_COMPLETENESS, DEFAULT_SECURITY, DEFAULT_TIMELINESS );
    }


    /** 
     * Regular constructor. The three weights should sum to 1.
     * @param completeness_weight The user's weight associated with completeness.
     * @param security_weight The user's weight associated with security.
     * @param timeliness_weight The user's weight associated with timeliness.
     **/
    public MAUWeights( double completeness_weight, double security_weight, double timeliness_weight) {
        updateMAU( completeness_weight, security_weight, timeliness_weight );
    }


    /**
     * Set the MAU array from the input values
     **/
    public void updateMAU( double completeness_weight, double security_weight, double timeliness_weight ) {
        _mau_weights[COMPLETENESS] = completeness_weight;
        _mau_weights[SECURITY] = security_weight;
        _mau_weights[TIMELINESS] = timeliness_weight;
    }


   /**
    * Accessor for the MAU weight array. The orderings in this array
    * are consistent with the constants COMPLETENESS, SECURITY and
    * TIMELINESS defined in this class.
    * @return The array of MAU weights, ordered in the order completeness,
    *                                   security, timeliness.
    **/
    public double[] getMAUArray() {
        return _mau_weights;
    }


    //-----------------------------------------------------------------------
    // private interface
    //-----------------------------------------------------------------------

    // The current weights for completeness, security and timeliness.
    // The sum of these weights should always add up to 1.
    private double[] _mau_weights;
}
