/*
 * DefenseOperatingModeChangeEvents.java
 *
 * <copyright>
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

package org.cougaar.tools.robustness.disconnection.test.disconnection;

import org.cougaar.tools.robustness.deconfliction.DefenseOperatingMode;
import java.util.Iterator;
import org.cougaar.core.adaptivity.ServiceUserPluginBase;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.service.EventService;


public class DefenseOperatingModeChangeEvents extends ServiceUserPluginBase implements NotPersistable {

  private IncrementalSubscription opModes;
  private EventService eventService = null;

  private static final Class[] requiredServices = {
      EventService.class
  };

  /**
   * Prints out all DefenseOperatingModes to
   * the logger or the event service if active.
   */
  public DefenseOperatingModeChangeEvents() {
    super(requiredServices);
  }

  private void getParams() {
/*    if (logger.isInfoEnabled() && getParameters().isEmpty()) logger.info ("plugin saw 0 parameters [must supply AE Node name].");

    Iterator iter = getParameters().iterator (); 
    if (iter.hasNext()) {
         AEAgent = (String)iter.next();
         logger.info ("AEAgent = " + AEAgent);
    }
 */
  }      
  
  /** Load services */
  public void load() {
    // get the EventService
    this.eventService = (EventService)
      getServiceBroker().getService(
          this,
          EventService.class,
          null);
    if (eventService == null) {
      throw new RuntimeException(
          "Unable to obtain EventService");
    }

    super.load();      
    getParams();
    
  }
  
  /** Unload services */
  public void unload() {
   
    super.unload();
    if (eventService != null) {
      getServiceBroker().releaseService(
          this, EventService.class, eventService);
      eventService = null;
    }
  }
  
  public void setupSubscriptions() {
    
     opModes = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof DefenseOperatingMode ) {
                    return true ;
                }
                return false ;
            }
        }) ;
    }

    public void execute() {
/*
        for ( Iterator iter = opModes.getAddedCollection().iterator() ;  iter.hasNext() ; ) {
            Object o = iter.next();
            if (o instanceof DefenseOperatingMode) {
               logger.info("Saw DefenseOperatingMode: " + 
                            ((DefenseOperatingMode)o).getName() + "=" + ((DefenseOperatingMode)o).getValue());
               eventService.event("DefenseOperatingMode: " + 
                            ((DefenseOperatingMode)o).getName() + "=" + ((DefenseOperatingMode)o).getValue());
            }
        }
 */        
        
        for ( Iterator iter = opModes.getCollection().iterator() ;  iter.hasNext() ; ) {
            Object o = iter.next();
            if (o instanceof DefenseOperatingMode) {
                if (eventService == null) //log to logger
                    logger.info("Saw DefenseOperatingMode: " + 
                            ((DefenseOperatingMode)o).getName() + "=" + ((DefenseOperatingMode)o).getValue());
                else
                    eventService.event("DefenseOperatingMode: " + 
                            ((DefenseOperatingMode)o).getName() + "=" + ((DefenseOperatingMode)o).getValue());
            }
        }        
    }
}
