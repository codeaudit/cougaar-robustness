/*
 * DiagnosisTechSpecServiceProvider.java
 *
 * Created on February 5, 2004, 4:57 PM
 * <copyright>
 *  Copyright 2004 Object Services and Consulting, Inc.
 *  under sponsorship of the Diagnosis Advanced Research Projects Agency (DARPA)
 *  and the Diagnosis Logistics Agency (DLA).
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
 * </copyright>
 */

package org.cougaar.coordinator.techspec;


import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;

import java.util.Vector;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;


/*
 * Implements a service provider AND DiagnosisTechSpecService
 *
 */
public class DiagnosisTechSpecServiceProvider implements ServiceProvider {
    
    private DiagnosisTechSpecServiceImpl impl;
    
    /**
     * Create an DiagnosisTechSpecService & Provider.
     *
     */
    public DiagnosisTechSpecServiceProvider() {
    
        impl = new DiagnosisTechSpecServiceImpl();
    }
    
    public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
        if (DiagnosisTechSpecService.class.isAssignableFrom(serviceClass)) {
            return impl;
        } else {
            return null;
        }
    }
    
    public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object service)  {
    }
    
    private final class DiagnosisTechSpecServiceImpl implements DiagnosisTechSpecService {

        private Vector crossProbs;
        private Hashtable allDiagnoses;
        private Vector newDiagnoses;
        
        /**
         * Create an DiagnosisTechSpecService
         */
        private DiagnosisTechSpecServiceImpl() {

            allDiagnoses = new Hashtable(100);
            newDiagnoses = new Vector();
            crossProbs = new Vector();
            
        }
        

        /**
         * Returns the DiagnosisTechSpec for the given class. If it has not been loaded,
         * the TechSpec is searched for, parsed, and loaded.
         *
         * @return NULL if the DiagnosisTechSpec cannot be found.
         */
            public DiagnosisTechSpecInterface getDiagnosisTechSpec(String sensorType) {

            DiagnosisTechSpecInterface dts = (DiagnosisTechSpecInterface)allDiagnoses.get( sensorType );
            if (dts == null) {

                //Tech Spec is not loaded...
                //... try finding it, parsing it, putting it in allDiagnoses, and returning it.

                //Now add it to newDiagnoses so it gets published to the BB
                synchronized(newDiagnoses) {
                    //add to new Diagnoses
                }
            }

            return dts; //even if null
        }


        /**
         *  @return all diagnoses. May want to return a clone to protect vector.
         *
         */
        public Collection getAllDiagnosisTechSpecs() {

            return allDiagnoses.values(); // may want to clone so it isn't 
        }

        /**
         * Add an DiagnosisTechSpec for a class. Targeted to testing
         */
        public void addDiagnosisTechSpec(String sensorType, DiagnosisTechSpecInterface dtsi) {

            synchronized(this) {
                allDiagnoses.put(sensorType, dtsi);
                newDiagnoses.add(dtsi);
            }

            //Look for cross diagnoses & add
            for (Iterator i=crossProbs.iterator(); i.hasNext(); ) {

                CrossDiagnosis cd = (CrossDiagnosis) i.next();
                if (cd.getSensorName().equalsIgnoreCase(sensorType)) {
                    dtsi.addCrossDiagnosisProbability(cd);
                    crossProbs.remove(cd);
                }            
            }        
        }


        /**
         * Add a DiagnosisTechSpec for a class, meant for testing
         */
        public void addCrossDiagnosis(CrossDiagnosis cd) {

            DiagnosisTechSpecInterface dts = null;
            synchronized(this) {
                dts = this.getDiagnosisTechSpec(cd.getSensorName());
            }
            if (dts != null) {
                dts.addCrossDiagnosisProbability(cd);
            } else {
                crossProbs.add(cd);
            }
        }

    }
    
}
