/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: StressInstanceFactory.java,v $
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

import org.cougaar.coordinator.techspec.ThreatModelInterface;
import org.cougaar.coordinator.techspec.TransitiveEffectDescription;
import org.cougaar.coordinator.techspec.TransitiveEffectModel;

/**
 * Simple factory class to produce stress instance subclasses from a
 * general object.
 *
 * @author Tony Cassandra
 * @version $Revision: 1.20 $Date: 2004-12-14 01:41:47 $
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
