/*
 * DiagnosisMangerPlugin.java
 *
 * Created on February 6, 2004, 11:05 AM
 * <copyright>
 *  Copyright 2003-2004 Object Services and Consulting, Inc.
 *  Copyright 2001-2003 Mobile Intelligence Corp
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

//import org.cougaar.coordinator.timedDiagnosis.TimedDiagnosisKnob;

import java.io.File;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.cougaar.core.persist.NotPersistable;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;

/**
 *
 * This plugin reads in the XML definitions of the Diagnosis Tech Specs and makes them
 * available via a service.
 * @deprecated
 * @author  Paul Pazandak, Ph.D. OBJS, Inc.
 */
public class DiagnosisManagerPlugin extends ComponentPlugin implements NotPersistable  {
    
    
    private LoggingService logger;
    private UIDService us = null;
    private DiagnosisTechSpecServiceProvider dtssp = null;
    private boolean haveServices = false;
    
    private Hashtable allDiagnoses;
    private Vector newDiagnoses;
    private Vector fileParams = null;
    private Vector crossProbs;
    
    /** The Max latency of all defenses. Used to throttle the TimedDiagnosisPlugin via its knob
     *  ***ASSUMES ONLY ONE MONITORING LEVEL PER DEFENSE!!! ***
     */
    //private long maxDefenseLatency = 0L;
    
    public void initialize() {

        super.initialize();
        // create and advertise our service
//        this.dtssp = new DiagnosisTechSpecServiceProvider(this);
//        getServiceBroker().addService(DiagnosisTechSpecService.class, dtssp);
        //System.out.println("Created new DiagnosisTechSpecServiceProvider <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
    }
    
    
    /**
     * Read in parameters passed in via configuration file.
     */
    private void getPluginParams() {
        
        fileParams = new Vector();
        
        Iterator iter = getParameters().iterator();
        if (iter.hasNext()) {
            fileParams.add( (String) iter.next() );
        }
        logger.debug("Total # of diagnosis file arguments = " + fileParams.size());
    }
    
    /** Read in the Diagnoses from the XML tech spec files */
    private void readInDiagnoses(Vector fileParams) {
        
        //**** Iterator thru all of the fileParams & load the deserialized objects into
        // allDiagnoses indexed by class/file name (minus ".xml" suffix).
        if (fileParams == null) { return; }
        
        File f;
        DiagnosisTechSpecInterface diagnosis = null;
        Iterator i = fileParams.iterator();
        while (i.hasNext()) {
            
            String name = (String)i.next();
            
            try {
                
                /////            DiagnosisXMLParser parser = new DiagnosisXMLParser (us);
                
                f = getConfigFinder().locateFile((String)i.next());
                if (!f.exists()) { //look
                    logger.warn("*** Did not find Diagnosis XML file in = " + f.getAbsolutePath());
                    logger.error("**** CANNOT FIND Diagnosis XML file!");
                    return;
                }
                //logger.debug("path for Diagnosis XML file = " + f.getAbsolutePath());
                
                /////            parser.parse(new InputSource(new FileInputStream(f)));
                
                /////            diagnosis = parser.getParsedDiagnosis();
                allDiagnoses.put(name, diagnosis);
                newDiagnoses.add(diagnosis);
                
            } catch (Exception e) {
                logger.error("Exception while importing Diagnosis!",e);
            }
            
            logger.debug("Imported "+allDiagnoses.size()+" Diagnoses!");
            
        }
    }
    
    
    /**
     * Prepare to run this plugin.
     *
     */
    public void load() {
        
        super.load();
        getServices();
        allDiagnoses = new Hashtable(100);
        newDiagnoses = new Vector();
        crossProbs = new Vector();

    }
    
    
    /**
     * Reads in the Diagnoses from XML, publishes them,
     * and then publishes the DiagnosisTechSpecService.
     *
     */
    public void setupSubscriptions() {
        //load tech specs
        //getPluginParams();
        //readInDiagnoses(fileParams);
        
    }
    
    /**
     * Unload the DiagnosisTechSpecService
     */
    public void unload() {
        // revoke our service
        if (dtssp != null) {
            getServiceBroker().revokeService(DiagnosisTechSpecService.class, dtssp);
            dtssp = null;
        }
        super.unload();
    }
    
    
    private void getServices() {
        
        logger =
        (LoggingService) getServiceBroker().getService(this, LoggingService.class, null);
        logger = org.cougaar.core.logging.LoggingServiceWithPrefix.add(logger, agentId + ": ");
        
        us = (UIDService ) getServiceBroker().getService( this, UIDService.class, null ) ;
        
        haveServices = true;
    }
    
    
    /** Publishes new Diagnoses to the BB */
    public void execute() {
        
        if (newDiagnoses.size() > 0) {
            
            synchronized(newDiagnoses) { //no new diagnoses to be added while we're getting them!
                Iterator i = newDiagnoses.iterator();
                while (i.hasNext() ) {
                    this.blackboard.publishAdd((DiagnosisTechSpecInterface)i.next());
                }
                
                //Now empty the vector since we've added them
                newDiagnoses.clear();
            }
        }
        
    }
    
    
    /**
     * Returns the DiagnosisTechSpec for the given class. If it has not been loaded,
     * the TechSpec is searched for, parsed, and loaded.
     *
     * @return NULL if the DiagnosisTechSpec cannot be found.
     */
    DiagnosisTechSpecInterface getTechSpec(String sensorType) {
        
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
    public void addTechSpec(String sensorType, DiagnosisTechSpecInterface a) {
        
        synchronized(this) {
            allDiagnoses.put(sensorType, a);
            newDiagnoses.add(a);
        }
        
        //Look for cross diagnoses & add
        for (Iterator i=crossProbs.iterator(); i.hasNext(); ) {
            
            CrossDiagnosis cd = (CrossDiagnosis) i.next();
            if (cd.getSensorName().equalsIgnoreCase(sensorType)) {
                a.addCrossDiagnosisProbability(cd);
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
            dts = getTechSpec(cd.getSensorName());
        }
        if (dts != null) {
            dts.addCrossDiagnosisProbability(cd);
        } else {
            crossProbs.add(cd);
        }
    }
    
}


