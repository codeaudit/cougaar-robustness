/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: Subset.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/Subset.java,v $
 * $Revision: 1.25 $
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

/**
 * Represents subset of a set of the integers 0 through (n-1).
 *
 * @author Tony Cassandra
 * @version $Revision: 1.25 $Date: 2004-08-09 20:46:41 $
 *
 */
public class Subset extends Object
{

    // Class implmentation comments go here ...

    //------------------------------------------------------------
    // public interface
    //------------------------------------------------------------

    /**
     * Constructor documentation comments go here ...
     *
     * @param set_size The size of the set that an instance of this
     * class will be a subset for.
     */
    public Subset( int set_size )
    {
        if ( set_size < 1 )
            return;

        _in_set = new boolean[set_size];

    }  // constructor Subset

    /**
     * Tests to see if a member is in thge subset
     *
     * @param elem_idx The index position of the element to be tested
     * for membership.
     */
    public boolean inSubset( int elem_idx )
    {
        if (( _in_set == null )
            || ( elem_idx < 0 )
            || ( elem_idx >= _in_set.length ))
            return false;

        return _in_set[elem_idx];

    } // method inSubset

    int getSetSize() 
    {
        if ( _in_set == null )
            return 0;

        return _in_set.length;
    }

    void setSubset( int elem_idx, boolean in_set )
    {
        if (( _in_set == null )
            || ( elem_idx < 0 )
            || ( elem_idx >= _in_set.length ))
            return;

        _in_set[elem_idx] = in_set;
    } // method setSubset

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    private boolean[] _in_set;

} // class Subset
