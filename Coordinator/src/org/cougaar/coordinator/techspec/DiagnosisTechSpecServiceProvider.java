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

/*
 * Implements a service provider AND DiagnosisTechSpecService
 *
 */
public class DiagnosisTechSpecServiceProvider implements ServiceProvider {
    
    private DiagnosisTechSpecServiceImpl impl;
    private DiagnosisManagerPlugin mgr;
    
    /**
     * Create an DiagnosisTechSpecService & Provider.
     *
     */
    public DiagnosisTechSpecServiceProvider(DiagnosisManagerPlugin mgr) {
    
        this.mgr = mgr;
        impl = new DiagnosisTechSpecServiceImpl(mgr);
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

        private DiagnosisManagerPlugin mgr;
        private Vector crossProbs;
        
        /**
         * Create an DiagnosisTechSpecService
         */
        private DiagnosisTechSpecServiceImpl(DiagnosisManagerPlugin mgr) {

            this.mgr = mgr;
            crossProbs = new Vector();
            
        }
        
        /**
         * @return the DiagnosisTechSpec for this class, and NULL if not found.
         */
        public DiagnosisTechSpecInterface getDiagnosisTechSpec(Class cls) {
            
            return mgr.getTechSpec(cls);
            
        }

        /**
         * Add a DiagnosisTechSpec for a class, meant for testing 
         */
        public void addDiagnosisTechSpec(String cls, DiagnosisTechSpecInterface d) {
            
            mgr.addTechSpec(cls, d);
            
        }

        /**
         * Add a DiagnosisTechSpec for a class, meant for testing 
         */
        public void addCrossDiagnosis(CrossDiagnosis dp) {
            
            crossProbs.add(dp);
            
        }
        
    }
    
}
