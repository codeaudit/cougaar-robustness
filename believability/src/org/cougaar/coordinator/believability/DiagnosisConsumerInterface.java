/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: DiagnosisConsumerInterface.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/Attic/DiagnosisConsumerInterface.java,v $
 * $Revision: 1.1 $
 * $Date: 2004-05-10 19:21:56 $
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
 * Used to accept new diagnoses and take the appropriate action.
 *
 * @author Tony Cassandra
 * @version $Revision: 1.1 $Date: 2004-05-10 19:21:56 $
 * 
 *
 */
public interface DiagnosisConsumerInterface
{

    //------------------------------------------------------------
      // public interface
    //------------------------------------------------------------

    public void consumeDiagnosis( BelievabilityDiagnosis diag ) 
	throws Exception;

} // class DiagnosisConsumerInterface
