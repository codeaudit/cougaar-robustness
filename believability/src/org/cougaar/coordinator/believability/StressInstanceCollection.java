/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: StressInstanceCollection.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/StressInstanceCollection.java,v $
 * $Revision: 1.19 $
 * $Date: 2004-08-09 20:46:41 $
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

import org.cougaar.coordinator.techspec.EventDescription;

import java.util.HashSet;
import java.util.Iterator;

/**
 * This class encapulates one or more stresses that can all cause the
 * same event to occur for a particular asset instance (or group of
 * assets).
 *
 * @author Tony Cassandra
 * @version $Revision: 1.19 $Date: 2004-08-09 20:46:41 $
 *
 */
class StressInstanceCollection extends Model
{

    // Class implmentation comments go here ...

    //------------------------------------------------------------
    // public interface
    //------------------------------------------------------------
    
    //************************************************************
    /**
     * Method description comments go here ...
     *
     * @param 
     */
    public String toString( )
    {
        // Method implementation comments go here ...
        
        return toString( "" );
    } // method toString

    //------------------------------------------------------------
    // package interface
    //------------------------------------------------------------

    /**
     * Constructor documentation comments go here ...
     *
     * @param
     */
    StressInstanceCollection( )
    {
    }  // constructor StressInstanceCollection


    //************************************************************
    /**
     * Adds a stress to this collection.
     */
    void add( StressInstance stress )
    {
        if ( stress == null )
            return;

        _stress_set.add( stress );

        if ( _event_desc == null )
            setEventDescription( stress.getEventDescription());

        else if ( ! getEventName().equalsIgnoreCase( stress.getEventName()))
            logDebug( "Added stress " + stress.getName()
                      + " in collection has non-matching event name. "
                      + " (" + getEventName() + "!=" 
                      + stress.getName() + ")" );

    } // method addParent

    //************************************************************
    /**
     * Gets the event description for the event that this stress
     * collectsion causes.
     */
    EventDescription getEventDescription( )
    {
        return _event_desc;

    } // method getEventDescription

    //************************************************************
    /**
     * Gets the event description for the event that this stress
     * collectsion causes.
     */
    void setEventDescription( EventDescription event_desc )
    {
        _event_desc = event_desc;

    } // method getEventDescription

    //************************************************************
    /**
     * Gets the name of the event that this stress collectsion causes.
     */
    String getEventName( )
    {
        if ( getEventDescription( ) == null )
            return null;
        
        else 
            return getEventDescription( ).getName();

    } // method getEventName

    //************************************************************
    /**
     * Computes the probability that this give set of stresses will
     * occur.  This computes the probability that at least one of
     * these stresses occurs.  Note that not all stresses are
     * time-dependent. In particular, ThreatModelInterface stress are
     * time dependent, but TransitiveEffectModel stresses are not.
     *
     * @param start_time The starting time to use to determine the
     * stess collection probability. 
     * @param end_time The ending time to use to determine the
     * stess collection probability. 
     */
    double getProbability( long start_time, long end_time )
    {
        // The probability that a collection of stresses occurs is
        // defined (here) to be the probability that at least one of
        // the stresses occurs.  To find this, we assume indepdence of
        // the stresses and then calculate the probability that no
        // stresses occur, then taking that result and subtarcting
        // from one to get the probability that at least one event
        // occurs.
        //

        // If the set of stresses is empty, then the probability of
        // nothing happening is 1.0.  Seems a little odd of aconcept,
        // but it makes the math work out nicely.
        //
        if ( _stress_set.size() < 1 )
            return 1.0;

        double[] stress_prob = new double[_stress_set.size()];

        Iterator stress_iter = _stress_set.iterator();
        for( int stress_idx = 0; stress_iter.hasNext(); stress_idx++ )
        {
            StressInstance stress = (StressInstance) stress_iter.next();

            stress_prob[stress_idx] = stress.getProbability( start_time,
                                                             end_time );
        } // while stress_iter

        logDetail( "Stress collection individual probs = "
                  + ProbabilityUtils.arrayToString( stress_prob ));

        double prob = ProbabilityUtils.computeEventUnionProbability
                ( stress_prob );
        
        logDetail( "Stress collection total prob = " + prob );

        return prob;

    } // method getProbability

    //************************************************************
    /**
     * Simple accessor for the number of stresses in this collection.
     */
    int size() { return _stress_set.size(); }

    //************************************************************
    /**
     * Converts this to a string, but puts prefix at the start of each
     * line
     *
     * @param  prefix What to put at the start of each line.
     */
    String toString( String prefix )
    {
        StringBuffer buff = new StringBuffer();

        buff.append( prefix + "Stress collection (event="
                     + getEventName() + "):\n" );

        Iterator iter = _stress_set.iterator();
        while( iter.hasNext() )
        {
            StressInstance stress = (StressInstance) iter.next();
            
            buff.append( stress.toString( prefix + "\t" ) + "\n" );

        } // while iter

        return buff.toString();
        
    } // method toString

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    // Contains all the stresses in this collection (StressInstance
    // objects).
    //
    private HashSet _stress_set = new HashSet();

    // The description obejhct for the event that this stress
    // collection is the cause of.
    //
    private EventDescription _event_desc;

} // class StressInstanceCollection
