/*
 * XMLAssetExtractor.java
 *
 * Created on August 27, 2003, 4:41 PM
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


package org.cougaar.coordinator.tools;

import org.cougaar.coordinator.techspec.DefaultAssetTechSpec;
import org.cougaar.coordinator.techspec.DefaultDefenseTechSpec;

import org.cougaar.core.persist.NotPersistable;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.util.UID;
import org.cougaar.core.component.ServiceBroker;

import org.xml.sax.*;
import java.io.FileInputStream;
import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;
import java.util.Iterator;
import java.util.Vector;



/**
 * @author Administrator
 */
public class XMLTechSpecGeneratorPlugin extends org.cougaar.core.plugin.ComponentPlugin {
    
    private String xmlFile = null;
    
    private Logger logger;
    
    private Vector defenses;
    private Vector threats;
    private Vector assets;
    private UIDService us = null;
    
    
    /** Creates a new instance of XMLAssetExtractor */
    public XMLTechSpecGeneratorPlugin() {
        super();
    }
    
    
    public static void main(String[] args) {
        
        try {
            String file = args[0];
            InputSource is = new InputSource(new FileInputStream(file));
            SocietyTree tree = SocietyParser.parse(is, new SocietyHandler());
            tree.printTree();
        } catch (Exception e) {
            System.out.println("Exception: "+e);
        }
    }
    
    protected void execute() {
    }
    
    /**
     * Load in XML definition of soc & create asset instances for each host, node & agent.
     * Also create the defense & threat tech specs & make them available.
     */
    protected void setupSubscriptions() {
        
        getServices();
        
        logger = Logging.getLogger(this.getClass().getName());
        
        //Load in XML File & generate TechSpecs
        SocietyTree societyTree = loadXML();
        if (societyTree != null)
            genTechSpecs(societyTree);
        
        
        //Create & publish defense tech specs
        initObjects();
        
    }
    
    
    private SocietyTree loadXML() {
        
        getPluginParams();
        if (xmlFile != null) {
            try {
                InputSource is = new InputSource(new FileInputStream(xmlFile));
                return SocietyParser.parse(is, new SocietyHandler());
                //tree.printTree();
            } catch (Exception e) {
                logger.error("Parsing Exception: "+e);
            }
        }
        return null;
    }
    
    
    private void genTechSpecs(SocietyTree societyTree) {
        
        assets = societyTree.toAssets(us);
        
    }
    
    private void getPluginParams() {
        if (getParameters().isEmpty()) {
            if (logger.isInfoEnabled()) logger.error("plugin saw 0 parameters [must supply XML Society file to read in].");
        }
        
        Iterator iter = getParameters().iterator();
        if (iter.hasNext()) {
            xmlFile = (String)iter.next();
            logger.debug("Setting xmlFile= " + xmlFile);
        }
    }
    
    
    /**
     * Create & Publish Defense tech specs & threats.
     *
     */
    private void initObjects() {
        
        DefaultDefenseTechSpec restart = new DefaultDefenseTechSpec("RESTART", nextUID(), null, false, false);
        DefaultDefenseTechSpec msglog = new DefaultDefenseTechSpec("MSGLOG", nextUID(), null, false, false);
        DefaultDefenseTechSpec dos = new DefaultDefenseTechSpec("DENIAL_OF_SERVICE", nextUID(), null, false, false);
        DefaultDefenseTechSpec disconnect = new DefaultDefenseTechSpec("DISCONNECT", nextUID(), null, false, false);
        
        getBlackboardService().openTransaction();
        getBlackboardService().publishAdd(restart);
        getBlackboardService().publishAdd(msglog);
        getBlackboardService().publishAdd(dos);
        getBlackboardService().publishAdd(disconnect);
        getBlackboardService().closeTransaction();
        
        
    }
    
    
    /** Get a UID. Return 0 if the UIDService is null */
    private UID nextUID() {
        if (us !=null) return us.nextUID();
        return null;
    }
    
    
    private boolean getServices() {
        if (us != null) return true;
        
        ServiceBroker sb = getServiceBroker();
        us = (UIDService )
        sb.getService( this, UIDService.class, null ) ;
        
        if (us != null) {
            return true;
        }        
        
        if (logger.isWarnEnabled()) {
            logger.warn("*** getServices - did NOT acquire UIDService!");
        }
        //Didn't acquire uidService
        return false;
    }
    
    
}
