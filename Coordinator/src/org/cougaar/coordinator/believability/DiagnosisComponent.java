/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: DiagnosisComponent.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/Coordinator/src/org/cougaar/coordinator/believability/Attic/DiagnosisComponent.java,v $
 * $Revision: 1.1 $
 * $Date: 2004-02-26 15:18:22 $
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

import org.cougaar.coordinator.DefenseApplicabilityConditionSnapshot;

/**
 * This class encapsulates a single diagnosis as extracted from the
 * techspecs.  It is a convenience container that holds only those
 * parts of the diagnosis that the belief update will need.
 *
 * @author Tony Cassandra
 */
class DiagnosisComponent extends Object
{
    protected DiagnosisComponent( DefenseApplicabilityConditionSnapshot def_cond,
                                  String mon_level,
                                  String diag_name )
    {
        defenseCondition = def_cond;
        monitorLevel = mon_level;
        diagnosisName = diag_name;

    } // constructor DiagnosisComponent

    public DefenseApplicabilityConditionSnapshot
	getDefenseCondition() { return defenseCondition; }
    public String getMonitoringLevel() { return monitorLevel; }
    public String getDiagnosisName() { return diagnosisName; }

    protected DefenseApplicabilityConditionSnapshot defenseCondition;
    protected String monitorLevel;
    protected String diagnosisName;

} // class DiagnosisComponent
