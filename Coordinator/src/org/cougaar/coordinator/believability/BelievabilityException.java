/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: BelievabilityException.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/Coordinator/src/org/cougaar/coordinator/believability/BelievabilityException.java,v $
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

/**
* Simple exception class to catch flaws in the model
*
* @author Misty Nodine
* @version $Revision: 1.1 $Date: 2004-02-26 15:18:21 $
* @see 
*/
public class BelievabilityException extends Exception
{

    //------------------------------------------------------------
    // public interface
    //------------------------------------------------------------

    /**
     */
    public BelievabilityException( )
    {
        super();
    } // constructor BelievabilityException

    /**
     *
     * @param msg A string to associate with this exception.
     */
    public BelievabilityException( String msg )
    {
        super( msg );
    } // constructor BelievabilityException
    
    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

} // class BelievabilityException
