/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: StateEstimationUpdateInterface.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/Attic/StateEstimationUpdateInterface.java,v $
 * $Revision: 1.1 $
 * $Date: 2004-05-10 19:21:57 $
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

import org.cougaar.coordinator.Diagnosis;

/**
 * Used for computing the next state estimation from a current
 * estimate and either a current time or a current diagnosis.
 *
 * @author Tony Cassandra
 * @version $Revision: 1.1 $Date: 2004-05-10 19:21:57 $
 * 
 *
 */
public interface StateEstimationUpdateInterface
{

    //------------------------------------------------------------
    // public interface
    //------------------------------------------------------------

    public StateEstimation updateStateEstimation( StateEstimation se,
                                                  long time );

    public StateEstimation updateStateEstimation( StateEstimation se,
                                                  Diagnosis diag );

} // class StateEstimationUpdateInterface
