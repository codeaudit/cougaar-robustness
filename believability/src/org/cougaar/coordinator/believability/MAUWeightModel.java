/*
 * MAUWeightModel.java
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

class MAUWeightModel extends Model
{
    // How many weights areb in this model.
    //
    static final int NUM_WEIGHTS         = 3;

    // Indices to use to access the various types of weights.
    //
    static int COMPLETENESS_IDX          = 0;
    static int SECURITY_IDX              = 1;
    static int TIMELINESS_IDX            = 2;

    static String[] WEIGHT_NAME
            = { "Completeness", "Security", "Timeliness" };

    // Default weight values.
    //
    static double DEFAULT_COMPLETENESS   = 0.4;
    static double DEFAULT_SECURITY       = 0.3;
    static double DEFAULT_TIMELINESS     = 0.3;

    /**
     * Constructor which currently uses default MAU weights
     **/
    MAUWeightModel() 
    {
        setWeights( DEFAULT_COMPLETENESS, 
                    DEFAULT_SECURITY, 
                    DEFAULT_TIMELINESS );

    } // constructor MAUWeightModel

    /** 
     * Regular constructor. The three weights should sum to 1.
     * @param completeness_weight The user's weight associated with
     * completeness. 
     * @param security_weight The user's weight associated with
     * security. 
     * @param timeliness_weight The user's weight associated with
     *timeliness. 
     **/
    MAUWeightModel( double completeness_weight, 
                           double security_weight, 
                           double timeliness_weight) 
            throws BelievabilityException
    {
        
        setWeights( completeness_weight, 
                    security_weight, 
                    timeliness_weight );
    } // constructor MAUWeightModel
    
    
    /**
     * Set the MAU array from the input values
     **/
    void setWeights( double completeness_weight,
                            double security_weight, 
                            double timeliness_weight ) 
   {
        _mau_weights[COMPLETENESS_IDX] = completeness_weight;
        _mau_weights[SECURITY_IDX] = security_weight;
        _mau_weights[TIMELINESS_IDX] = timeliness_weight;

        setValidity( true );

    } // method setWeights

    /**
     * Set the MAU array from the input values
     **/
    void setWeights( double[] new_weights ) 
            throws BelievabilityException
   {
       if (( new_weights == null )
           || ( new_weights.length != NUM_WEIGHTS ))
           throw new BelievabilityException
                   ( "MAUWeightModel.setWeights()",
                     "Wrong size array for setting weights." );
       
       for ( int i = 0 ; i < NUM_WEIGHTS; i++ )
           _mau_weights[i] = new_weights[i];

        setValidity( true );

    } // method setWeights


   /**
    * Accessor for the MAU weight array. The orderings in this array
    * are consistent with the constants COMPLETENESS, SECURITY and
    * TIMELINESS defined in this class.
    * @return The array of MAU weights, ordered in the order completeness,
    *                                   security, timeliness.
    **/
    double[] getWeights() { return _mau_weights; }

    //-----------------------------------------------------------------------
    // private interface
    //-----------------------------------------------------------------------

    // The current weights for completeness, security and timeliness.
    // The sum of these weights should always add up to 1.
    private double[] _mau_weights = new double[NUM_WEIGHTS];
}
