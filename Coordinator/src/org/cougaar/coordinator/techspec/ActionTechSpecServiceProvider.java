/*
 * ActionTechSpecServiceProvider.java
 *
 * Created on February 5, 2004, 4:57 PM
 * <copyright>
 *  Copyright 2004 Object Services and Consulting, Inc.
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
 * </copyright>
 */

package org.cougaar.coordinator.techspec;


import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;

/*
 * Implements a service provider AND TechSpecService
 *
 */
public class ActionTechSpecServiceProvider implements ServiceProvider {
    
    private ActionTechSpecServiceImpl impl;
    private ActionManagerPlugin mgr;
    
    /**
     * Create an ActionTechSpecService & Provider.
     *
     */
    public ActionTechSpecServiceProvider(ActionManagerPlugin mgr) {
    
        this.mgr = mgr;
        impl = new ActionTechSpecServiceImpl(mgr);
    }
    
    public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
        if (ActionTechSpecService.class.isAssignableFrom(serviceClass)) {
            return impl;
        } else {
            return null;
        }
    }
    
    public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object service)  {
    }
    
    private final class ActionTechSpecServiceImpl implements ActionTechSpecService {

        private ActionManagerPlugin mgr;
        
        /**
         * Create an ActionTechSpecService
         */
        private ActionTechSpecServiceImpl(ActionManagerPlugin mgr) {

            this.mgr = mgr;
            
        }
        
        /**
         * @return the ActionTechSpec for this class, and NULL if not found.
         */
        public ActionTechSpecInterface getActionTechSpec(Class cls) {
            
            return mgr.getTechSpec(cls);
            
        }

        /**
         * Add an ActionTechSpec for a class. Targeted to testing
         */
        public void addActionTechSpec(String cls, ActionTechSpecInterface a) {
            
            mgr.addTechSpec(cls, a);
            
        }
        
    }
    
}
