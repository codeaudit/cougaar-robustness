/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: SubsetIterator.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/SubsetIterator.java,v $
 * $Revision: 1.13 $
 * $Date: 2004-07-15 20:19:42 $
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

/**
 * Gves you an iterator that will return a sequence of 'Subset'
 * objects, such that you get all possible subsets.  The subset are of
 * the integers 0 through (n-1)
 *
 * @author Tony Cassandra
 * @version $Revision: 1.13 $Date: 2004-07-15 20:19:42 $
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
     * @param
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
