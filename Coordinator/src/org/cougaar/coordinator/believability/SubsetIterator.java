/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: SubsetIterator.java,v $
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

import java.util.Iterator;

/**
 * Gves you an iterator that will return a sequence of 'Subset'
 * objects, such that you get all possible subsets.  The subset are of
 * the integers 0 through (n-1)
 *
 * @author Tony Cassandra
 * @version $Revision: 1.1 $Date: 2008-07-25 20:47:16 $
 * @see Subset
 *
 */
public class SubsetIterator implements Iterator
{

    // Class implmentation comments go here ...

    //------------------------------------------------------------
      // public interface
    //------------------------------------------------------------

    /**
     * Constructor documentation comments go here ...
     *
     * @param set_size The sie of the set this is an iterator for.
     */
    public SubsetIterator( int set_size )
    {
        if ( set_size < 1 )
        {
            _counter = 0;
            return;
        }

        _subset = new Subset( set_size );

        // This is the way we keep track of when we are done.  there
        // should be this many subsets.
        //
        _counter = (long) Math.pow( 2.0, set_size );

        // Need to handle the first one as a special case.
        _first_element_sent = false;

    }  // constructor SubsetIterator

    //************************************************************
    /**
     * comments here...
     */
    public boolean hasNext() { return _counter > 0; }

    //************************************************************
    /**
     * comments here...
     */
    public Object next()
    {
        if ( _counter < 1 )
            return null;

        if ( ! _first_element_sent )
        {
            _first_element_sent = true;
            _counter--;
            return _subset;
        }

        int pos = 0;
        while( pos < _subset.getSetSize() )
        {
            if ( ! _subset.inSubset( pos ) )
            {
                _subset.setSubset( pos, true );
                _counter--;
                return _subset;
            }
            
            _subset.setSubset( pos, false );
            pos++;
        } // while true

        // If we exit the while loop without returning from the
        // method, this is an internal error, because counter should
        // run out before this condition happens.
        //
        _counter = 0;
        return null;

    } // method next

    //************************************************************
    /**
     * comments here...
     */
    public void remove() { /* do nothing */ }

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    private Subset _subset;

    private long _counter;

    private boolean _first_element_sent;

    //------------------------------------------------------------
    // Testing
    //------------------------------------------------------------

    public static final void main( String argv[] )
    {
        int size;
        int count = 0;

        try 
        { 
            size = Integer.parseInt( argv[0] ); 
        } 
        catch (NumberFormatException nfe) 
        {
            return;
        }

        System.out.println( "Subsets for size = " + size );

        Iterator iter = new SubsetIterator( size );
        while( iter.hasNext() )
        {
            Subset subset = (Subset) iter.next();

            System.out.print( "[" + count + "]: " );
            for ( int i = 0; i < size ; i++ )
            {
                if ( subset.inSubset( i ))
                    System.out.print( "1 " );
                else
                    System.out.print( "0 " );
            }
            System.out.print( "\n" );
            count++;
        }
    } // method main


} // class SubsetIterator
