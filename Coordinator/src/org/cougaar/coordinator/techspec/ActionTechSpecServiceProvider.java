/*
 * ActionTechSpecServiceProvider.java
 *
 * Created on February 5, 2004, 4:57 PM
 * 
 * <copyright>
 * 
 *  Copyright 2004 Object Services and Consulting, Inc.
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 *
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
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
 * Implements a service provider AND TechSpecService
 *
 */
public class ActionTechSpecServiceProvider implements ServiceProvider {
    
    private ActionTechSpecServiceImpl impl;
    
    /**
     * Create an ActionTechSpecService & Provider.
     *
     */
    public ActionTechSpecServiceProvider() {
    
        impl = new ActionTechSpecServiceImpl();
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

        private Hashtable allActions;
        private Vector newActions;
        
        /**
         * Create an ActionTechSpecService
         */
        private ActionTechSpecServiceImpl() {
            allActions = new Hashtable(100);
            newActions = new Vector();            
        }

        /**
         *  @return all diagnoses. May want to return a clone to protect vector.
         *
         */
        public Collection getAllActionTechSpecs() {

            return allActions.values(); // may want to clone so it isn't 
        }
        
        
        /**
         * Returns the ActionTechSpec for the given class. If it has not been loaded,
         * the TechSpec is searched for, parsed, and loaded.
         *
         * @return NULL if the ActionTechSpec cannot be found.
         */
        public ActionTechSpecInterface getActionTechSpec(String cls) {

            ActionTechSpecInterface ats = (ActionTechSpecInterface)allActions.get( cls);
            if (ats == null) {

                //logger.warn("************* action tech spec NOT FOUND: "+cls);        

                //Tech Spec is not loaded...
                //... try finding it, parsing it, putting it in allActions, and returning it.

                //Now add it to newActions so it gets published to the BB
                synchronized(newActions) {
                        //add to new Actions

                }

            }

            return ats; //even if null
        }

        /**
         * Add an ActionTechSpec for a class. Targeted to testing
         */
        public void addActionTechSpec(String cls, ActionTechSpecInterface a) {

            allActions.put(cls, a); 
            newActions.add(a);
            //logger.debug("************* add action tech spec: "+cls);        
        }
        
        
        
    }
    
}
