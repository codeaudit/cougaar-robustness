/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: NoSuchBeliefStateException.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/Coordinator/src/org/cougaar/coordinator/believability/Attic/NoSuchBeliefStateException.java,v $
 * $Revision: 1.1 $
 * $Date: 2004-02-26 15:18:24 $
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
* Simple exception class to catch flaws in the model
*
* @author Misty Nodine
* @version $Revision: 1.1 $Date: 2004-02-26 15:18:24 $
* @see 
*/
public class NoSuchBeliefStateException extends Exception
{

    //------------------------------------------------------------
    // public interface
    //------------------------------------------------------------

    /**
     */
    public NoSuchBeliefStateException( )
    {
        super();
    } // constructor NoSuchBeliefStateException

    /**
     *
     * @param msg A string to associate with this exception.
     */
    public NoSuchBeliefStateException( String msg )
    {
        super( msg );
    } // constructor NoSuchBeliefStateException
    
    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

} // class NoSuchBeliefStateException
