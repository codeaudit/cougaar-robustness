/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: StressInstance.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/StressInstance.java,v $
 * $Revision: 1.1 $
 * $Date: 2004-06-29 22:43:18 $
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

import java.util.Vector;

import org.cougaar.coordinator.techspec.AssetStateDimension;
import org.cougaar.coordinator.techspec.AssetType;
import org.cougaar.coordinator.techspec.AssetTechSpecInterface;
import org.cougaar.coordinator.techspec.EventDescription;

/**
 * This wraps and extends the ThreatModelInterface and
 * TransitveEffectModel objects (only one of these is contained in a
 * given StressInstance object).  For the purposes that believability
 * uses them, these two objects should be treated the same.  The only
 * difference that has any impact on the code is that the proabilities
 * for the threat likelihood is time-dependent, while those for the
 * transitive effects are not.
 *
 * @author Tony Cassandra
 * @version $Revision: 1.1 $Date: 2004-06-29 22:43:18 $
 *
 */
abstract class StressInstance extends Model
{

    // Class implmentation comments go here ...

    //------------------------------------------------------------
    // public interface
    //------------------------------------------------------------

    //************************************************************
    /**
     * Just defer the toString to the version that takes a prefix.
     * Prefix allows better formatting for nested structures.
     *
     */
    public String toString( )
    {
        // Method implementation comments go here ...
        
        return toString( "" );
    } // method toString

    //------------------------------------------------------------
    // package interface
    //------------------------------------------------------------

    //************************************************************
    /**
     * Return an identifying label for the stress.
     */
    abstract String getName();

    //************************************************************
    /**
     * Return an identifying label for the event caused by this stress.
     */
    String getEventName()
    {
        return getEventDescription().getName();
    }

    //************************************************************
    /**
     * Return the event description object for this stress.
     */
    abstract EventDescription getEventDescription();

    //************************************************************
    /**
     * Return the asset type that is directly affected by this stress.
     */
    AssetType getAffectedAssetType()
    {
        return getEventDescription().getAffectedAssetType();
    } // method getAffectedAssetType

    //************************************************************
    /**
     * Return the list of applicable assets (a Vector of
     * AssetTechSpecInterface objects.
     */
    abstract Vector getAssetList();

    //************************************************************
    /**
     * Return an identifying label for the particular stress instance
     * (concrete class ID)
     */
    abstract String getTypeStr();

    //************************************************************
    /**
     * Return the asset state dimension that this stress has an
     * immediate relationship to.
     */
    abstract AssetStateDimension getStateDimension();

    //************************************************************
    /**
     * Adds a parent stress instance to the set of parentsit can have.
     */
    void addParent( StressInstance parent )
    {
        _parent_stress_set.add( parent); 
    } // method addParent

    //************************************************************
    /**
     * Determines if this stress instance affects the given asset.
     * This relationship can change over time, so we must be careful
     * about caching the results of this test.
     */
    abstract boolean affectsAsset( AssetTechSpecInterface asset_ts );

    //************************************************************
    /**
     * Returns the stress probability over the given time
     * interval. This includes the immediate probability as well as
     * all the ancestral stresses that can lead to this stress.
     *
     * @param start_time The starting time to use to determine the
     * stess collection probability. 
     * @param end_time The ending time to use to determine the
     * stess collection probability. 
     */
    double getProbability( long start_time, 
                           long end_time )
    {
        // The immediate probability is a conditional probability
        // given the ancestral stresses occurring.  Thus, if there was
        // only one parent stress we would simply multiply the
        // probability of the parent stress by the immediate
        // probability of this stress, given the parent stress.
        //
        // However, if there are multiple parent stresses, then the
        // probability that the ancestral stresses lead to this stress
        // is the probability that at least one parent stress occurs.
        // To compute this, we compute the porbability that no parent
        // stresses occur and subtract it from "1.0".  This is
        // actually done in the StressInstanceCollection
        // getProbability() method.
        //

        // Note that this works even when the parent stress set is
        // empty, because by definition, the probability of the set of
        // no threats happeing is one: if there are no threats, then
        // certainly something will not happen.
        //
        return getImmediateProbability( start_time,
                                        end_time )
                * _parent_stress_set.getProbability( start_time,
                                                     end_time );

    } // method getStressProbability

    //************************************************************
    /**
     * Converts this to a string, but puts prefix at the start of each
     * line Prefix allows better formatting for nested structures.
     *
     * @param  prefix What to put at the start of each line.
     */
    String toString( String prefix )
    {
       StringBuffer buff = new StringBuffer();

        buff.append( prefix + "Stress "
                     + getName() + " is of type " + getTypeStr()
                     + ", cause event " + getEventName() );

        if ( _parent_stress_set.size() > 0 )
        {
            buff.append( "\n" + prefix + "   Parent stresses:\n" );
                     
            buff.append(  _parent_stress_set.toString( prefix + "\t" ));
        }

        else
        {
            buff.append( " (no parents)" );
        }

        return buff.toString();
        
    } // method toString

    //------------------------------------------------------------
    // protected interface
    //------------------------------------------------------------

    //************************************************************
    /**
     * Returns the immediate stress probability over the given time
     * interval. This immediate stress probability *does not* include
     * the ancestor stress probabilities, so is conditioned on those
     * ancestor events happening. Note that not all stresses will be
     * dependent on a time interval.
     *
     * @param start_time The starting time to use to determine the
     * stess collection probability. 
     * @param end_time The ending time to use to determine the
     * stess collection probability. 
     */
    abstract protected double getImmediateProbability( long start_time, 
                                                       long end_time );

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    // Defines all the immediate parent stress instances that can
    // cause this stress to happen.
    //
    private StressInstanceCollection _parent_stress_set 
            = new StressInstanceCollection();

} // class StressInstance
