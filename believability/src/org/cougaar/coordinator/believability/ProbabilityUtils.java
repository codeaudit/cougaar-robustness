/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: ProbabilityUtils.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/ProbabilityUtils.java,v $
 * $Revision: 1.7 $
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

import java.util.Iterator;
import java.util.Random;
 
/**
 * Utilities for dealing with probabilities.
 *
 * @author Tony Cassandra
 * @version $Revision: 1.7 $Date: 2004-06-29 22:43:18 $
 */
public class ProbabilityUtils
{

    //------------------------------------------------------------
    // public interface
    //------------------------------------------------------------

    //************************************************************
    /**
     * Computes the probability that at least one event occurs, given
     * that the event random variables are boolean and independent.  
     *
     * @param event_prob An array of the individual event
     * probabilities. 
     * @return The probability that at least one of the events
     * occurs. 
     */
    public static double computeEventUnionProbability( double[] event_prob )
    {
        // Computing the probability of at least one event occuring
        // is identical to computing the the probability that no
        // events happen and subtracting it from one.  Since we make
        // an assumption that all events/threats are independent,
        // computing the intersection of non-events/non-threats is
        // nothing more than computing the product of the complements
        // of individual probabilities.
        //

        if ( event_prob == null )
            return 0.0;

        double prob_product = 1.0;

        for ( int idx = 0; idx < event_prob.length; idx++ )
        {
            prob_product *= 1.0 - event_prob[idx];
        } // for idx
        
        return 1.0 - prob_product;

    } // method computeEventUnionProbability

    //************************************************************
    /**
     * Merges a series of transition matrices into a single transition
     * matrix.  Assumes that each transition matrix is a conditional
     * probability of the transition *given* its correspnding event
     * occurs. The probability of the events occurring is given as an
     * array of numbers as input.  Assume that the transition matrix
     * when the event does *not* occur is the identity matrix.
     *
     * @param event_prob An array of the individual probabilites that
     * an event happens.
     * @param event_trans_prob An array of transition matrices, one
     * for each event. The transition matrices are the transition
     * probabilities *given* the event occurs.  these must be square
     * matrices. 
     * @return The transiiton matrix given all possible events that
     * could occur.
     * occurs. 
     */
    public static double[][] mergeMultiEventTransitions
            ( double[] event_prob,
              double[][][] event_trans_prob )
    {
        // At the heart of this routine is a loop over all possible
        // subsets of the events, corresponding to one possible
        // combination of events occurring.  The full probability can
        // only be calculated by considering all possible subsets,
        // i..e, all possible combinations of events occurring and not
        // occurring.
        //

        // Calculation details.
        //
        // Given:
        //
        //   event_trans_prob: is a transition matrix defining the
        //   probability of transitioning dein state 'i' to 'j' given
        //   that event 'k' occurs. i.e., 
        //
        //       Pr( s_j | s_i, e_k )
        //
        //   event_prob: is the probability of each event 'k'
        //   occurring, i.e.,
        //
        //       Pr( e_k )
        //
        //   Note that the probability of event 'k' will have been
        //   been computed by considering all the threats that could
        //   cause // this event to happen: essentially the
        //   probability that at // least one threat occurs.  This is
        //   handled in the // computeEventUnionProbability() method,
        //   and is:
        //
        //     Pr( e_k ) = 1 - ( \Pi_{t_{k,n} \in T_k} ( 1 - Pr( t_{k,n})))
        //
        //   where T_k is the set of all threats that can cause event
        //   'k', with t_{k,n} being the individual threat
        //   probabilities.  This assumes all threats are mutually
        //   independent.
        //
        // The calculation this method computes is:
        //
        //   Pr( j | i ) 
        //
        // whose derivation is:
        //
        //  Pr( j | i )  = \Sum_{S \in E} ( Pr( S ) Pr( j | i, S ))
        //
        // Where E is the set of all possible events and we are
        // summing over all possible subsets, S, of E.  We also have:
        //
        //  Pr( S ) = ( \Pi_{e_k \in S} Pr( e_k ) ) 
        //               ( \Pi_{e_k \not \in S} ( 1 - Pr( e_k ) ))
        //
        // which assumes that all events are mutually independent.
        // Then we also have:
        //
        //  Pr( j | i, s )  = \left\{   
        //
        //     [ i = j ]                    if S = \emptyset, or else
        //
        //     \frac{1}{|S|} \Sum_{e_k \in S} Pr( j | i, e_k )
        //
        // Here, the expression is an Iverson/Kronecker delta notation
        // signifying the expression is '1' if the inner statement is
        // true and '0' if false.  This assumes that should at least one
        // event occur, only one will lead to a state transition, and
        // the event that occurs is a a random process with a uniform
        // distribution over all the events that have occurred.
        //

        // This is where we will accumulate the transition //
        // probabilities, and what will be returned.
        //
        double[][] trans_prob = new double
                [event_trans_prob[0].length]
                [event_trans_prob[0][0].length];

        // We use this subset iterator to loop over all possible
        // subsets.
        //
        Iterator subset_iter = new SubsetIterator( event_prob.length );
        while( subset_iter.hasNext() )
        {
            Subset event_subset = (Subset) subset_iter.next();

            // Compute the probability that this subset of events
            // occurs. Since we assume all events are pairwise
            // independent, and since we are just computing the
            // intersetion of all these events, we just multiply the
            // probabilities together.  Note that we have to
            // complement the event probabilities for the events that
            // didn't occur.
            //
            double event_subset_prob = 1.0;
            int num_happened_events = 0;

            for ( int k = 0; k < event_subset.getSetSize(); k++ )
            {
                if ( event_subset.inSubset( k ))
                {
                    num_happened_events++;
                    event_subset_prob *= event_prob[k];
                }
                else
                {
                    event_subset_prob *= ( 1.0 - event_prob[k] );
                }

            } // for i

            // Compute the transition probabilities assuming this
            // subset of events occurs.  Here we make the assumption
            // that when two events occur simultaneously, only one of
            // their effects will actually take place.  Further, we
            // assume that exactly which effects take place will be a
            // random process so that any one of the event effects
            // occurs with uniform probability.
            //

            // These doubly nested loops are simply because we need to
            // compute for every entry in the transtion matrix.
            //
            for ( int i = 0; i < event_trans_prob[0].length; i++ )
            {
                for ( int j = 0; j < event_trans_prob[0].length; j++ )
                {

                    double happened_trans = 0.0;
                    
                    // The assumption that only one event takes place
                    // and that which one is a uniform random process
                    // is equivalent to taking the average of the
                    // transition probabilities.  This next loop does
                    // this by accumulating than finally dividing by
                    // the number of events.
                    //
                    for ( int k = 0; k < event_subset.getSetSize(); k++ )
                    {
                        // If the event is considered to have occurred
                        // in this subset, then we accumulate its
                        // transition probability. This is the
                        // numerator portion of the average of all the
                        // "happened" event transitions. 
                        //
                        if ( event_subset.inSubset( k ))
                        {
                            happened_trans += event_trans_prob[k][i][j];
                        }

                    } // for k

                    // After accumulating all event transition
                    // probabilities, we will divide by the number of
                    // events that have occurred.  This gives us the
                    // average of the transition probabilities for all
                    // the events that 'happen' (i.e., are 'in' the
                    // current subset.
                    //

                    // Here is where can accumulate the transition
                    // contribution of this subset of events.  This
                    // requires weighting the just computed transition
                    // probabilities by the probability of this subset
                    // of events happens.
                    // 
                    if ( num_happened_events < 1 )
                    {
                        if ( i == j )
 
                            // This is more verbosely:
                            //
                            //  trans_prob[i][j] += event_subset_prob * 1.0;
                            //
                            trans_prob[i][j] += event_subset_prob;

                        // else case not needed, but verbosely is:
                        //
                        //   trans_prob[i][j] += event_subset_prob * 0.0;

                    } // if this subset has no events happening.
                    
                    // Else, at least one event happened, so set to
                    // the average of all those event's transition
                    // matrices.
                    //
                    else 
                    {
                        trans_prob[i][j] += event_subset_prob
                                * happened_trans / num_happened_events;
                    }

                } // for j
            } // for i
            
        } // while subset_iter
        
        return trans_prob;
        
    } // method mergeMultiEventTransitions

    //************************************************************
    // TESTING AREA
    //************************************************************

    // Use a fixed random seed for reproducibility
    //
    static Random _rand = new Random( 999999 );

    static double getRandomDouble() { return _rand.nextDouble(); }

    static void setRandomDistribution( double[] a )
    {
        if ( a == null )
            return;

        double total = 0.0;
        for ( int idx = 0; idx < a.length; idx++ )
        {
            a[idx] = _rand.nextDouble();
            total += a[idx];
        }

        // Normalize
        for ( int idx = 0; idx < a.length; idx++ )
            a[idx] /= total;

    }

    static void setRandomDistribution( double[][] a )
    {
        if ( a == null )
            return;
        
        for ( int idx = 0; idx < a.length; idx++ )
            ProbabilityUtils.setRandomDistribution( a[idx] );
        
    }

    static String arrayToString( double[] a )
    {
        if ( a == null )
            return "null";

        StringBuffer buff = new StringBuffer();

        buff.append( "[ " + a[0] );
        for ( int i = 1; i < a.length; i++ )
            buff.append( ", " + a[i] );
        buff.append( " ]" );

        return buff.toString();
    }

   static String arrayToString( double[][] a, String prefix )
    {
        if ( a == null )
            return prefix + "null";

        StringBuffer buff = new StringBuffer();

        buff.append( prefix + "[ " );
        for ( int i = 0; i < a.length; i++ )
        {
            buff.append( a[i][0] );
            for ( int j = 1; j < a[i].length; j++ )
                buff.append( ", " + a[i][j] );
            if ( i != (a.length-1))
                buff.append( " ;\n" + prefix + "  " );
            
        }
        buff.append( " ]" );

        return buff.toString();
    }

    static String arrayToString( double[][] a )
    {
        return arrayToString( a, "" );
    }

    static String arrayToString( double[][][] a, String prefix )
    {
        if ( a == null )
            return prefix + "null";

        StringBuffer buff = new StringBuffer();

        for ( int k = 0; k < a.length; k++ )
        {
            buff.append( prefix + "Array " + k + ":\n" );

            buff.append( arrayToString( a[k], prefix + "\t" ));
        }

        return buff.toString();
    }

    static String arrayToString( double[][][] a )
    {
        return arrayToString( a, "" );
    }

    static boolean isEquals( double[][] a1, double[][] a2 )
    {
        if (( a1 == null ) && ( a2 == null ))
            return true;

        if (( a1 == null ) || ( a2 == null ))
            return false;

        if ( a1.length != a2.length )
            return false;

        for ( int i = 0; i < a1.length; i++ )
        {
            if ( a1[i].length != a2[i].length )
                return false;

            for ( int j = 0; j < a1[i].length; j++ )
                if ( ! Precision.isEqual( a1[i][j], a2[i][j] ))
                    return false;
        }

        return true;
    }

    public static final void main( String[] argv )
    {
        int test_num = 0;
        int err_count = 0;

        double arg;
        double[] arg1d;
        double[][] arg2d;
        double[][][] arg3d;

        double ans;
        double[] ans1d;
        double[][] ans2d;
        double[][][] ans3d;

        double expect;
        double[] expect1d;
        double[][] expect2d;
        double[][][] expect3d;

        //------------------------------------------------------------
        System.out.println( "\n** Testing: computeEventUnionProbability() **\n" );
        //------------------------------------------------------------

        double[] threat_prob1 = { 0.6 };
        double[] threat_prob2 = { 0.5, 0.2 };
        double[] threat_prob3 = { 0.7, 0.2, 0.1 };
        double[] threat_prob4 = { 0.8, 0.3, 0.2, 0.6 };

        arg1d = null;
        expect = 0.0;
        System.out.println( "-- TEST #" + ++test_num );
        System.out.println( "Threat Prob Array: " + arrayToString( arg1d ));
        ans = computeEventUnionProbability( arg1d );
        System.out.println( "Probability of event union: " + ans
                            + ", expecting: " + expect );
        if ( ! Precision.isEqual( ans, expect ))
            System.out.println( "** ERROR #" + (++err_count) + " **" );
        else
            System.out.println( "OK" );


        arg1d = threat_prob1;
        expect = 0.6;
        System.out.println( "-- TEST #" + ++test_num );
        System.out.println( "Threat Prob Array: " + arrayToString( arg1d ));
        ans = computeEventUnionProbability( arg1d );
        System.out.println( "Probability of event union: " + ans
                            + ", expecting: " + expect );
        if ( ! Precision.isEqual( ans, expect ))
            System.out.println( "**ERROR #" + (++err_count) + " **" );
        else
            System.out.println( "OK" );

        arg1d = threat_prob2;
        expect = 0.6;
        System.out.println( "-- TEST #" + ++test_num );
        System.out.println( "Threat Prob Array: " + arrayToString( arg1d ));
        ans = computeEventUnionProbability( arg1d );
        System.out.println( "Probability of event union: " + ans
                            + ", expecting: " + expect );
        if ( ! Precision.isEqual( ans, expect ))
            System.out.println( "**ERROR #" + (++err_count) + " **" );
        else
            System.out.println( "OK" );

        arg1d = threat_prob3;
        expect = 0.784;
        System.out.println( "-- TEST #" + ++test_num );
        System.out.println( "Threat Prob Array: " + arrayToString( arg1d ));
        ans = computeEventUnionProbability( arg1d );
        System.out.println( "Probability of event union: " + ans
                            + ", expecting: " + expect );
        if ( ! Precision.isEqual( ans, expect ))
            System.out.println( "**ERROR #" + (++err_count) + " **" );
        else
            System.out.println( "OK" );

        arg1d = threat_prob4;
        expect = 0.9552;
        System.out.println( "-- TEST #" + ++test_num );
        System.out.println( "Threat Prob Array: " + arrayToString( arg1d ));
        ans = computeEventUnionProbability( arg1d );
        System.out.println( "Probability of event union: " + ans
                            + ", expecting: " + expect );
        if ( ! Precision.isEqual( ans, expect ))
            System.out.println( "**ERROR #" + (++err_count) + " **" );
        else
            System.out.println( "OK" );


        //------------------------------------------------------------
        System.out.println( "\n** Testing: mergeMultiEventTransitions() **\n" );
        //------------------------------------------------------------

        double[][][] event_trans1 = { { {0.3, 0.7},
                                        {0.4, 0.6} }};

        double[] event_prob1 = { 0.2 };

        double[][] expect_merge1 = { { 0.86, 0.14 },
                                     { 0.08, 0.92 }};

        arg1d = event_prob1;
        arg3d = event_trans1;
        expect2d = expect_merge1;
        System.out.println( "-- TEST #" + ++test_num );
        System.out.println( "Event probs: " + arrayToString( arg1d ));
        System.out.println( "Event trans:\n" + arrayToString( arg3d ));
        ans2d = mergeMultiEventTransitions( arg1d, arg3d );
        System.out.println( "Result:\n" + arrayToString( ans2d ));
        System.out.println( "Expecting:\n" + arrayToString( expect2d ));

        if ( ! isEquals( ans2d, expect2d ))
            System.out.println( "**ERROR #" + (++err_count) + " **" );
        else
            System.out.println( "OK" );

        double[][][] event_trans2 = { { {0.15, 0.85},
                                        {0.35, 0.65} }};
        double[] event_prob2 = { 1.0 };
        double[][] expect_merge2 = { {0.15, 0.85},
                                     {0.35, 0.65} };

        arg1d = event_prob2;
        arg3d = event_trans2;
        expect2d = expect_merge2;
        System.out.println( "-- TEST #" + ++test_num );
        System.out.println( "Event probs: " + arrayToString( arg1d ));
        System.out.println( "Event trans:\n" + arrayToString( arg3d ));
        ans2d = mergeMultiEventTransitions( arg1d, arg3d );
        System.out.println( "Result:\n" + arrayToString( ans2d ));
        System.out.println( "Expecting:\n" + arrayToString( expect2d ));

        if ( ! isEquals( ans2d, expect2d ))
            System.out.println( "**ERROR #" + (++err_count) + " **" );
        else
            System.out.println( "OK" );

        double[][][] event_trans3 = { { {0.95, 0.05},
                                        {1.0, 0.0} }};
        double[] event_prob3 = { 0.0 };
        double[][] expect_merge3 = { {1.0, 0.0},
                                     {0.0, 1.0} };

        arg1d = event_prob3;
        arg3d = event_trans3;
        expect2d = expect_merge3;
        System.out.println( "-- TEST #" + ++test_num );
        System.out.println( "Event probs: " + arrayToString( arg1d ));
        System.out.println( "Event trans:\n" + arrayToString( arg3d ));
        ans2d = mergeMultiEventTransitions( arg1d, arg3d );
        System.out.println( "Result:\n" + arrayToString( ans2d ));
        System.out.println( "Expecting:\n" + arrayToString( expect2d ));

        if ( ! isEquals( ans2d, expect2d ))
            System.out.println( "**ERROR #" + (++err_count) + " **" );
        else
            System.out.println( "OK" );

        double[][][] event_trans4 = {
            {
                {0.9, 0.1},
                {0.6, 0.4} 
            },
            {
                {0.5, 0.5},
                {0.3, 0.7} 
            }

        };
        double[] event_prob4 = { 0.4, 0.2 };
        double[][] expect_merge4 = { {0.884, 0.116},
                                     {0.264, 0.736} };

        arg1d = event_prob4;
        arg3d = event_trans4;
        expect2d = expect_merge4;
        System.out.println( "-- TEST #" + ++test_num );
        System.out.println( "Event probs: " + arrayToString( arg1d ));
        System.out.println( "Event trans:\n" + arrayToString( arg3d ));
        ans2d = mergeMultiEventTransitions( arg1d, arg3d );
        System.out.println( "Result:\n" + arrayToString( ans2d ));
        System.out.println( "Expecting:\n" + arrayToString( expect2d ));

        if ( ! isEquals( ans2d, expect2d ))
            System.out.println( "**ERROR #" + (++err_count) + " **" );
        else
            System.out.println( "OK" );


        if ( err_count < 1 )
            System.out.println( "\nPASSED." );
        else
            System.out.println( "\nFAILED. Found " + err_count + " errors." );

    } // main

} // class ProbabilityUtils
