/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: StressInstanceFactory.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/believability/src/org/cougaar/coordinator/believability/StressInstanceFactory.java,v $
 * $Revision: 1.19 $
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

import org.cougaar.coordinator.techspec.ThreatModelInterface;
import org.cougaar.coordinator.techspec.TransitiveEffectDescription;
import org.cougaar.coordinator.techspec.TransitiveEffectModel;

/**
 * Simple factory class to produce stress instance subclasses from a
 * general object.
 *
 * @author Tony Cassandra
 * @version $Revision: 1.19 $Date: 2004-08-09 20:46:41 $
 *
 */
public class StressInstanceFactory extends Object
{

    // Class implmentation comments go here ...

    //------------------------------------------------------------
    // public interface
    //------------------------------------------------------------

    /**
     * Returns the appropriate stress object for the object sent in.
     * If the object is unreckognized, null is returned.
     *
     * @param stress_object The stress object from the OBJS techspec
     * package.
     *
     */
    public static StressInstance create( Object stress_object )
    {

        if ( stress_object instanceof ThreatModelInterface )
            return new TMIStressInstance
                    ( (ThreatModelInterface) stress_object );

        if ( stress_object instanceof TransitiveEffectModel )
            return new TEMStressInstance
                    ( (TransitiveEffectModel) stress_object );

        if ( stress_object instanceof TransitiveEffectDescription )
            return new TEDStressInstance
                    ( (TransitiveEffectDescription) stress_object );

        return null;

    }  // constructor StressInstanceFactory

} // class StressInstanceFactory
