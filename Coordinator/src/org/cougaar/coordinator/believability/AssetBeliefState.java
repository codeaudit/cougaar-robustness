/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: AssetBeliefState.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/Coordinator/src/org/cougaar/coordinator/believability/Attic/AssetBeliefState.java,v $
 * $Revision: 1.1 $
 * $Date: 2004-02-26 15:18:21 $
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

import java.util.Hashtable;
import java.util.Enumeration;

/**
 * Class description goes here...
 *
 * @author Tony Cassandra
 * @version $Revision: 1.1 $Date: 2004-02-26 15:18:21 $
 * @since
 * @see
 */
public class AssetBeliefState extends Object
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
    public AssetBeliefState( String asset_name,
                             String state_desc_name,
                             int num_states )
    {
        _asset_name = asset_name;
        _state_desc_name = state_desc_name;
        _num_states = num_states;

    } // constructor AssetBeliefState

    public String getAssetName() { return _asset_name; }
    public String getAssetStateDescName() { return _state_desc_name; }
    public int getNumStates() { return _num_states; }

    // FIXME:  This no longer makes any sense. Remove.
    public void setDefenseCondition( DefenseApplicabilityConditionSnapshot def_cond )
    {
        _defense_cond = def_cond;
    }

    // FIXME:  This no longer makes any sense. Remove.
    public DefenseApplicabilityConditionSnapshot getDefenseCondition() 
    {
        // FIXME: I only put this here so that the API did not change
        // and that something of the right type gets returned. The
        // first diagnosis component has no real meaning here.
        return _diagnosis.elementAt(0).defenseCondition;
    }
    
    public void setCompositeDiagnosis( CompositeDiagnosis diagnosis )
    {
        _diagnosis = diagnosis;
    }

    public CompositeDiagnosis getCompositeDiagnosis()
    {
        return _diagnosis;
    }

    /**
     * Set a probability in this belief state using a numeric state.
     *
     * @param state The state to set the probability to as an integer.
     * @param prob The probability to set this state to, opverwriting
     * any previous value.  
     */
    public void set( String state, double prob )
    {
        _belief.put( state, new Double( prob ));
    }

    /**
     * Gets a probability to this belief state using a numeric state.
     *
     * @param state The state to set the probability to as an integer.
     * any previous value.  
     * @exception NoSuchBeliefStateException if the belief state is not
     *            known.
     */
    public double get( String state )
            throws NoSuchBeliefStateException 
    {
        
        Double d = (Double) _belief.get( state );
        if ( d != null ) 
            return d.doubleValue();
        else 
            throw new NoSuchBeliefStateException( "No value for " + state );
    }  // method get


    /**
     * Return an enumeration of the names of all of the states in this
     * AssetBeliefState. This is the list of known state names having 
     * associated probabilities.
     *
     * @return an Enumeration of known states with associated probabilities
     **/

    public Enumeration getStateNames() {
     return _belief.keys();
    }
    

    /**
     * Returns a string represenation of the AssetBeliefState
     *
     * @return the string
     **/
    public String toString()
    {
        StringBuffer buff = new StringBuffer();
        
        buff.append( _asset_name 
                     + " : " + _state_desc_name
                     + " : [" );

        Enumeration key_enum = _belief.keys();

        while ( key_enum.hasMoreElements())
        {
            String state = (String) key_enum.nextElement();
            
            buff.append( " " + state + ":" + _belief.get( state ) );
        }

        buff.append( " ]" );
        
        return buff.toString();

    } // method toString


    /**
     * Convert this belief state to an array,m using the
     * POMDPModelInterface to map integer indices to names.
     *
     * @param pomdp_inter The interface that helps define the mapping
     * from aray indices to state value names.
     * @return A probability distribution over asset stattes as an
     * array 
     */
    public double[] toArray( POMDPModelInterface pomdp_inter )
            throws BelievabilityException
    {
        double[] belief = new double[_num_states];

        try 
        {
            for ( int i = 0; i < _num_states; i++ ) 
            {
                belief[i] = get( pomdp_inter.indexToStateName
                                 ( _asset_name,
                                   _state_desc_name,
                                   i ));
            } // for i

        }
        catch ( NoSuchBeliefStateException nsbse ) 
        {
            throw new BelievabilityException 
                    ( ".toArray -- " +
                      "Trouble converting to array -- "
                      + nsbse.getMessage() );
        }

        return belief;
 
    } // method toArray

   /**
     * Set this belief dtate to be the same as the array values passed
     * in. This needs a POMDPModelInterface to map integer indices to
     * names.
     *
     * @param belief The belief state as an array
     * @param pomdp_inter The interface that helps define the mapping
     * from aray indices to state value names.
     * @return A probability distribution over asset stattes as an
     * array 
     */
    public void set( double[] belief,
                     POMDPModelInterface pomdp_inter )
            throws BelievabilityException
    {
        for ( int i = 0; i < _num_states; i++ )
            set( pomdp_inter.indexToStateName( _asset_name,
                                               _state_desc_name,
                                               i ), 
                 belief[i] );
        
    } // set

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------
    
    private String _asset_name;
    private String _state_desc_name;
    private int _num_states;

    // FIXME:  This no longer makes any sense. Remove.
    private DefenseApplicabilityConditionSnapshot _defense_cond;

    private CompositeDiagnosis _diagnosis;

    private Hashtable _belief = new Hashtable();

} // class AssetBeliefState
