/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: BelievabilityKnob.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/BelievabilityKnob.java,v $
 * $Revision: 1.15 $
 * $Date: 2004-10-04 22:22:36 $
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

import org.cougaar.util.UnaryPredicate;

/**
 * This is the home of all parameter values and settings that we may
 * want to have external, dynamic control over.
 *
 * @author Tony Cassandra
 * @version $Revision: 1.15 $Date: 2004-10-04 22:22:36 $
 *
 */
public class BelievabilityKnob implements Serializable, NotPersistable
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
    public static final double DEFAULT_INIT_BELIEF_BLUR_AMOUNT = 0.01;
    public static final double DEFAULT_NO_INFO_BELIEF_BLUR_AMOUNT = 0.10;

    /**
     * Convenience predicate for subscriptions for subscribing to this
     * object on the balckboard.
     */
    public static UnaryPredicate pred = new UnaryPredicate() 
        {
            public boolean execute(Object o) 
            {
                 if ( o instanceof BelievabilityKnob )
                 {
                     return true ;
                 }
                 return false ;
            }
        };
    
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

    public double getInitialBeliefBlurAmount()
    {
        return _init_belief_blur_amount;
    }

    public double getNoInformationBeliefBlurAmount()
    {
        return _no_info_belief_blur_amount;
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

    public void setInitialBeliefBlurAmount( double value )
    {
        _init_belief_blur_amount = value;
    }

    public void setNoInformationBeliefBlurAmount( double value )
    {
        _no_info_belief_blur_amount = value;
    }

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    /**
     * For the instances where believability does a utility value
     * determination before publishing, this sets the threshold value to
     * use to determine whether to publish or not. 
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
    private long _max_publish_interval = Long.valueOf(
        System.getProperty( "org.cougaar.coordinator.believability.MAX_PUBLISH_INTERVAL",
                   String.valueOf(DEFAULT_MAX_PUBLISH_INTERVAL) )
                          ).longValue();

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
    //    private boolean _is_leashed = DEFAULT_IS_LEASHED;
    private boolean _is_leashed = Boolean.valueOf(
        System.getProperty( "org.cougaar.coordinator.believability.IS_LEASHED",
                   String.valueOf( DEFAULT_IS_LEASHED ) )
                          ).booleanValue();

    /**
     * Assuming that something has probbaility is zero is generally a
     * bad idea in a formal probabilistic model, since more than
     * likely, most anything is possible, though often very
     * unprobable.  But unlikely is infinitely different from not
     * possible in the way the math gets done.  For this reason, on
     * startup, though we are pretty sure of the initial state, we do
     * not want to be so arrogant that we will not evenm consider
     * other possible worlds, else the math wil punish us for our
     * close-mindedness. Thus, we use this quantity to blur the
     * initial believe state to ensure that we have at least some
     * probability of being in an unexpected state.
    */
    private double _init_belief_blur_amount = Double.valueOf
             ( System.getProperty
               ( "org.cougaar.coordinator.believability.INIT_BELIEF_BLUR",
                 String.valueOf(DEFAULT_INIT_BELIEF_BLUR_AMOUNT))).doubleValue();

    /**
     * In certain circumstances (such as rehydrating) the
     * believability plugin will lose track of the states of assets,
     * and thus have to recreate the belief state with limited
     * information. The tech-spec derived initial belief state only
     * makes sense to use as the a priori belief if you know the asset
     * has just started (by definition of the a priori belief state.
     * Because committing to the wrong initial belief could lead to
     * bad results, we always want to entertain some probability for
     * every possible state the asset could be in.  However, we want
     * to be biased toward the tech spec initial belief state.  Thus,
     * when we have no other information about an asset's belief
     * state, we will take the techspec initial belief and then blur
     * the probabilities here so that no state has zero probability.
     * The amount of probabiolity to distribute is this quantity.
     * Note that we spread this across all states, not just the zero
     * probability states.  We also need to renormalize afterwards.
     */
    private double _no_info_belief_blur_amount = Double.valueOf
             ( System.getProperty
               ( "org.cougaar.coordinator.believability.NO_INFO_BELIEF_BLUR",
                 String.valueOf(DEFAULT_NO_INFO_BELIEF_BLUR_AMOUNT))).doubleValue();

} // class BelievabilityKnob
