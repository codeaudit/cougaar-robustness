/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: Precision.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/Precision.java,v $
 * $Revision: 1.30 $
 * $Date: 2004-09-21 00:43:49 $
 *</RCS_KEYWORD>
 *
 *<COPYRIGHT>
 * The following source code is protected under all standard copyright
 * laws.
 *</COPYRIGHT>
 *
 *</SOURCE_HEADER>
 */

package org.cougaar.coordinator.believability;

/**
 * Miscellaneous routines that help deasl with floating point
 * precision issues. Floating point precision issues should *not* be
 * ignored: they will cause you problems is care is not taken. 
 *
 * @author Tony Cassandra
 * @version $Revision: 1.30 $Date: 2004-09-21 00:43:49 $
 */
public class Precision
{
    // In the original C code, these methods and their functionality
    // were done using macros, which made they relatively efficient.
    // Will the java compiler in-line these?  I don't know, but
    // depending on the complexity of the models and algorithms, the
    // encapsulation of this functionality as a full java class might
    // lead to an inefficiency that would need to be optimized.

    //--------------------------------------------------
    // public interface
    //--------------------------------------------------

    public static final double DEFAULT_POS_ZERO_TOL_MODEL = 0.001;
    public static final double DEFAULT_NEG_ZERO_TOL_MODEL = -0.001;

    public static final double DEFAULT_POS_ZERO_TOL_COMP = 0.0000000001;
    public static final double DEFAULT_NEG_ZERO_TOL_COMP = -0.0000000001;

    //************************************************************
    /**
     * Compares a number to zero using the current precision set in
     * this module. This is for values that are derived from internal
     * model parameters.
     */
    public static boolean isZeroModel( double value )
    {  
        return ((value < _pos_zero_tol_model) 
                && ( value > _neg_zero_tol_model ));
    } // isZeroModel

    //************************************************************
    /**
     * Compares a number to zero using the current precision set in
     * this module.
     */
    public static boolean isZeroComputation( double value )
    {  
        return ((value < _pos_zero_tol_comp) 
                && ( value > _neg_zero_tol_comp ));
    } // isZeroComputation

    //************************************************************
    /**
     * Compares a number to zero using the current precision set in
     * this module. This is for values that are derived from internal
     * model parameters.
     */
    public static boolean isEqualModel( double value1,
                                        double value2 )
    {  
        return Math.abs( value1 - value2 ) < _pos_zero_tol_model;

    } // isEqualComputation

    //************************************************************
    /**
     * Compares a number to zero using the current precision set in
     * this module.  This is for values that are derived from internal
     * computations.
     */
    public static boolean isEqualComputation( double value1,
                                              double value2 )
    {  
        return Math.abs( value1 - value2 ) < _pos_zero_tol_comp;

    } // isEqualComputation

    //************************************************************
    /**
     * Sets the values the positive and negative tolerances used in
     * zero comparisons. This is for values that are derived from internal
     * model parameters.
     */
    public static void setZeroTolerancesModel( double pos_tolerance, 
                                               double neg_tolerance )
    {
        _pos_zero_tol_model = pos_tolerance;
        _neg_zero_tol_model = neg_tolerance;
    }

    //************************************************************
    /**
     * Sets the values the positive and negative tolerances used in
     * zero comparisons.  Uses just one magnitude value and  applies
     * it symmetrically to zero.  If tolerance_mag is les than zero,
     * this routine will take its absolute value to get the
     * magnitude. This is for values that are derived from internal
     * model parameters.
     */
    public static void setZeroTolerancesModel( double tolerance_mag )
    {
        if ( tolerance_mag < 0.0 )
            tolerance_mag = Math.abs( tolerance_mag );

        setZeroTolerancesModel( tolerance_mag, -1.0 * tolerance_mag );

    }
     //************************************************************
    /**
     * Sets the values the positive and negative tolerances used in
     * zero comparisons. This is for values that are derived from internal
     * computations.
     */
    public static void setZeroTolerancesComputation( double pos_tolerance, 
                                                      double neg_tolerance )
    {
        _pos_zero_tol_comp = pos_tolerance;
        _neg_zero_tol_comp = neg_tolerance;
    }

    //************************************************************
    /**
     * Sets the values the positive and negative tolerances used in
     * zero comparisons.  Uses just one magnitude value and  applies
     * it symmetrically to zero.  If tolerance_mag is les than zero,
     * this routine will take its absolute value to get the
     * magnitude. This is for values that are derived from internal
     * computations.
     */
    public static void setZeroTolerancesComputation( double tolerance_mag )
    {
        if ( tolerance_mag < 0.0 )
            tolerance_mag = Math.abs( tolerance_mag );

        setZeroTolerancesComputation( tolerance_mag, -1.0 * tolerance_mag );

    }
    //************************************************************

    //--------------------------------------------------
    // private interface
    //--------------------------------------------------

    private static double _pos_zero_tol_model = DEFAULT_POS_ZERO_TOL_MODEL;
    private static double _neg_zero_tol_model = DEFAULT_NEG_ZERO_TOL_MODEL;

    private static double _pos_zero_tol_comp = DEFAULT_POS_ZERO_TOL_COMP;
    private static double _neg_zero_tol_comp = DEFAULT_NEG_ZERO_TOL_COMP;

} // class Precision
