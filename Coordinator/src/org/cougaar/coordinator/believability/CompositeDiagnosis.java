/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: CompositeDiagnosis.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/Coordinator/src/org/cougaar/coordinator/believability/Attic/CompositeDiagnosis.java,v $
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

import java.util.Vector;

/**
 * A Vector of DiagnosisComponent objects that define a diagnosis when
 * considering multiple defenses.
 *
 * @author Tony Cassandra
 * @version $Revision: 1.1 $Date: 2004-02-26 15:18:22 $
 * @since
 * @see
 */
public class CompositeDiagnosis extends Object
{

    // Class implementation comments go here...

    //------------------------------------------------------------
    // public interface
    //------------------------------------------------------------

    /**
     * Constructor documentation comments go here...
     *
     * @param
     */
    public CompositeDiagnosis( )
    {
    } // constructor CompositeDiagnosis
    //************************************************************

    /**
     * The number of diagnosis components in this composite diagnosis.
     *
     * @return The number of elements in the composite diagnosis
     */
    public int size()
    {
        return _diag_list.size();
    } // method elementAt

    /**
     * Method documentation goes here...
     *
     * @param index The positional index to use to retrive a diagnosis
     * component 
     * @return The DiagnosisComponent at the given position, or null
     * if the index is invalid
     * @see 
     */
    public DiagnosisComponent elementAt( int i )
    {
        if (( i < 0 ) || ( i >= _diag_list.size() ))
            return null;

        return (DiagnosisComponent) _diag_list.elementAt( i );
        
    } // method elementAt

    /**
     * Adding a component
     *
     * @param diag_comp The component to add
     */
    public void add(DiagnosisComponent diag_comp )
    {
        this._diag_list.addElement( diag_comp );
    } // method add
    //************************************************************

    /**
     * Adding a component by the individual component values
     *
     * @param def_cond The defense condition obejct asscoiated with
     * the individual diagnosis component.
     * @param mon_level The name (label) of the monitoring level
     * @param diag_name The name (label) of the diagnosis 
     */
    public void add(DefenseApplicabilityConditionSnapshot def_cond,
                    String mon_level,
                    String diag_name)
    {
        add( new DiagnosisComponent( def_cond,  
                                     mon_level, 
                                     diag_name ));
    } // method add

    /**
     * Returns the time of this compoisite diagnosis
     *
     * @return The time that this composite diagnosis is considered to
     * have happened at.
     */
    public long getTimestamp()
    {
        // Right now, we use an average of all the "TRUE" applicable
        // diagnosis (all diagnoses that are not "OK").
        
        long time_sum = 0;
        int time_count = 0;

        for ( int i = 0; i < _diag_list.size(); i++ )
        {
            DiagnosisComponent dc 
                    = (DiagnosisComponent) _diag_list.elementAt(i);

            // Any diagnosius that is considered OK is ignored.
            //
            if (dc.diagnosisName.equals( IntermediateModel.DIAGNOSIS_OK_STR ))
                continue;

            time_sum += dc.defenseCondition.getTimestamp();
            time_count++;

        } // for i

        // FIXME: This return value makes little sense, but I could
        // not think of  a better one on the spur of the moment.
        //
        if ( time_count == 0 )
            return System.currentTimeMillis();

        return time_sum / time_count;

    } // method getTimestamp

    /**
     * Just show the defense name and its value as a string
     */
    public String toString()
    {
        StringBuffer buff = new StringBuffer();

        for ( int i = 0; i < _diag_list.size(); i++ )
        {
            DiagnosisComponent dc
                    = (DiagnosisComponent) _diag_list.elementAt(i);

            if ( i > 0 )
                buff.append( ", " );
            
            buff.append( dc.getDefenseCondition().getDefenseName()
                         + "=" + dc.getDiagnosisName() );
            
        } // for i

        return buff.toString();

    }
    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    private Vector _diag_list = new Vector();

} // class CompositeDiagnosis
