/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: BeliefConsumer.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/BeliefConsumer.java,v $
 * $Revision: 1.3 $
 * $Date: 2004-07-15 20:19:41 $
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
 * Simple interface for things that will consume new belief states.
 *
 * @author Tony Cassandra
 * @version $Revision: 1.3 $Date: 2004-07-15 20:19:41 $
 *
 */
public interface BeliefConsumer
{

    //------------------------------------------------------------
    // public interface
    //------------------------------------------------------------

    /**
     * Call this to consume the belief state.
     *
     * @param belief The belief state to consume
     */
    public void consumeBeliefState( BeliefState belief );

} // class BeliefConsumer
