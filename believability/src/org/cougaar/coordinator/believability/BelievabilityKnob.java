/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: BelievabilityKnob.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/BelievabilityKnob.java,v $
 * $Revision: 1.4 $
 * $Date: 2004-08-04 15:17:35 $
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

import java.io.Serializable;

import org.cougaar.core.persist.NotPersistable;

/**
 * This is the home of all parameter values that we may want to have
 * dynamic control over.
 *
 * @author Tony Cassandra
 * @version $Revision: 1.4 $Date: 2004-08-04 15:17:35 $
 *
 */
public class BelievabilityKnob implements NotPersistable, Serializable 
{

    // Class implmentation comments go here ...

    //------------------------------------------------------------
    // public interface
    //------------------------------------------------------------

    // Policy Constants: These are constant values we used for some
    // parameters, that some future version may turn into dynamic,
    // model or policy generated values.
    //
    public static final double DEFAULT_BELIEF_RELEASE_UTILITY_THRESHOLD = 0.1;
    public static final long DEFAULT_MAX_PUBLISH_INTERVAL = 900000;
    public static final long DEFAULT_IMPLICIT_DIAGNOSIS_INTERVAL = 300000;
    public static final long DEFAULT_PUBLISH_DELAY_INTERVAL = 500;

    //------------------------------------------------------------
    // Accessors
    //------------------------------------------------------------

    public static double getBeliefReleaseUtilityThreshold()
    {
        return _belief_release_utility_threshold;
    }

    public static long getMaxPublishInterval()
    {
        return _max_publish_interval;
    }

    public static long getImplicitDiagnosisInterval()
    {
        return _implicit_diagnosis_interval;
    }

    public static long getPublishDelayInterval()
    {
        return _publish_delay_interval;
    }

    //------------------------------------------------------------
    // Mutators
    //------------------------------------------------------------

    public static void setBeliefReleaseUtilityThreshold( double value )
    {
        _belief_release_utility_threshold = value;
    }

    public static void setMaxPublishInterval( long value )
    {
        _max_publish_interval = value;
    }

    public static void setImplicitDiagnosisInterval( long value )
    {
        _implicit_diagnosis_interval = value;
    }

    public static void setPublishDelayInterval( long value )
    {
        _publish_delay_interval = value;
    }

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    private static double _belief_release_utility_threshold
            = DEFAULT_BELIEF_RELEASE_UTILITY_THRESHOLD;

    private static long _max_publish_interval
            = DEFAULT_MAX_PUBLISH_INTERVAL;

    private static long _implicit_diagnosis_interval
            = DEFAULT_IMPLICIT_DIAGNOSIS_INTERVAL;

    private static long _publish_delay_interval
            = DEFAULT_PUBLISH_DELAY_INTERVAL;

} // class BelievabilityKnob
