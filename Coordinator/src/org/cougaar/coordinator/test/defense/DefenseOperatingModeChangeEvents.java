/*
 * DefenseOperatingModeChangeEvents.java
 *
 * <copyright>
 * 
 *  Copyright 2003-2004 Object Services and Consulting, Inc.
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


package org.cougaar.coordinator.test.defense;

//import org.cougaar.coordinator.DefenseOperatingMode;
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
   * Prints out all new and changed DefenseOperatingModes to
   * the logger and the event service.
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
/*    
     opModes = ( IncrementalSubscription ) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof DefenseOperatingMode ) {
                    return true ;
                }
                return false ;
            }
        }) ;
 */
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
        
        for ( Iterator iter = opModes.getChangedCollection().iterator() ;  iter.hasNext() ; ) {
            Object o = iter.next();
            if (o instanceof DefenseOperatingMode) {
               logger.info("Saw DefenseOperatingMode: " + 
                            ((DefenseOperatingMode)o).getName() + "=" + ((DefenseOperatingMode)o).getValue());
               eventService.event("DefenseOperatingMode: " + 
                            ((DefenseOperatingMode)o).getName() + "=" + ((DefenseOperatingMode)o).getValue());
            }
        }
 */        
    }
 
}
