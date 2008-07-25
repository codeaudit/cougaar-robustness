/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: Subset.java,v $
 *</NAME>
 *
 *<COPYRIGHT>
 *  Copyright 2004 Telcordia Technologies, Inc.
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA)
 *  and the Defense Logistics Agency (DLA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 *</COPYRIGHT>
 *
 *</SOURCE_HEADER>
 */

package org.cougaar.coordinator.believability;

/**
 * Represents subset of a set of the integers 0 through (n-1).
 *
 * @author Tony Cassandra
 * @version $Revision: 1.1 $Date: 2008-07-25 20:47:17 $
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
