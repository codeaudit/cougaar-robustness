/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: BelievabilityKnob.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/BelievabilityKnob.java,v $
 * $Revision: 1.5 $
 * $Date: 2004-08-04 23:45:19 $
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
 * This is the home of all parameter values and settings that we may
 * want to have external, dynamic control over.
 *
 * @author Tony Cassandra
 * @version $Revision: 1.5 $Date: 2004-08-04 23:45:19 $
 *
 */
public class BelievabilityKnob implements Serializable 
{

    // Mostly just a collection of variables with simple accessors and
    // mutators.

    //------------------------------------------------------------
    // public interface
    //------------------------------------------------------------

    // These are default values we used for parameters.
    //
    public static final double DEFAULT_BELIEF_RELEASE_UTILITY_THRESHOLD = 0.1;
    public static final long DEFAULT_MAX_PUBLISH_INTERVAL = 900000;
    public static final long DEFAULT_PUBLISH_DELAY_INTERVAL = 500;
    public static final boolean DEFAULT_IS_LEASHED   = true;
    
    /**
     * Default constructor, where everything just takes default
     * values.
     */
    public BelievabilityKnob()
    {
    } // constructor

    //------------------------------------------------------------
    // Accessors
    //------------------------------------------------------------

    public double getBeliefReleaseUtilityThreshold()
    {
        return _belief_release_utility_threshold;
    }

    public long getMaxPublishInterval()
    {
        return _max_publish_interval;
    }

    public long getPublishDelayInterval()
    {
        return _publish_delay_interval;
    }

    public boolean isLeashed()
    {
        return _is_leashed;
    }

    //------------------------------------------------------------
    // Mutators
    //------------------------------------------------------------

    public void setBeliefReleaseUtilityThreshold( double value )
    {
        _belief_release_utility_threshold = value;
    }

    public void setMaxPublishInterval( long value )
    {
        _max_publish_interval = value;
    }

    public void setPublishDelayInterval( long value )
    {
        _publish_delay_interval = value;
    }

    public void setIsLeashed( boolean value )
    {
        _is_leashed = value;
    }

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    /**
     * For the instances where believability does a utility value
     * determination before publishing, this sets the threshold value to
     * use to detremine whether to publish or not. 
     */
    private double _belief_release_utility_threshold
            = DEFAULT_BELIEF_RELEASE_UTILITY_THRESHOLD;

    /**
     * This defines the maximum amount of time that will be allowed to
     * elapse between state estimation p[ublications for a given
     * asset.  Even if we see no activity for an asset, once this time
     * period is exceeded, we will compute a new belief state and
     * publish it.
     */
    private long _max_publish_interval
            = DEFAULT_MAX_PUBLISH_INTERVAL;

    /**
     * We do not want to be too quick to publish a state estimation
     * for an asset.  Thus, once we determine we have all the
     * information we need to update and publish a new belief state,
     * we wait some small amount of time to allow very closely
     * associated (in time) other events that might be imminent.  This
     * will help to throttle the output frequency of the believability
     * plugin and also ensure that simultaneous events are fully
     * accounted for in the same belief (rather than an arbitrary
     * sequence of belief states, one for each event.)
     */
    private long _publish_delay_interval
            = DEFAULT_PUBLISH_DELAY_INTERVAL;

    /**
     * Leashing means that we should not do anything because the
     * system needs to acquiece, or some other known disturbance is
     * rendering actions/diagnoses invalid.
     */
    private boolean _is_leashed = DEFAULT_IS_LEASHED;

} // class BelievabilityKnob
