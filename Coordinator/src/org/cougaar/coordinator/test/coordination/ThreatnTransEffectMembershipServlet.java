/*
 * ThreatnTransEffectMembershipServlet.java
 *
 * Created on June 30, 2004, 9:46 AM
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

package org.cougaar.coordinator.test.coordination;

import org.cougaar.coordinator.techspec.*;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Iterator;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.cougaar.coordinator.*;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ServletService;
import org.cougaar.util.UnaryPredicate;

/**
 * Servlet for displaying Threat and TransEffect Memberships
 */
public class ThreatnTransEffectMembershipServlet extends ComponentPlugin
{
    private ServiceBroker sb;
    private LoggingService log;
    private ServletService servletSvc;

    MyServlet myServlet;

    private IncrementalSubscription threatSub;

    private UnaryPredicate threatPred = new UnaryPredicate() {
	    public boolean execute(Object o) {
		return (o instanceof DefaultThreatModel);}};
      
      
    public void load() {
	super.load();
	sb = getServiceBroker();
	log = (LoggingService) 
	    sb.getService(this, LoggingService.class, null);
	if (log == null) {
	    log = LoggingService.NULL;
	}
	servletSvc = (ServletService)
	    sb.getService(this, ServletService.class, null);
    }

    public void unload() {
	if ((log != null) &&
	    (log != LoggingService.NULL)) {
	    sb.releaseService(this, LoggingService.class, log);
	    log = LoggingService.NULL;
	}
	if (servletSvc != null) {
	    sb.releaseService(this, ServletService.class, servletSvc);
	}
	super.unload();
    }
    
    public void setupSubscriptions() 
    {
	try {
	    servletSvc.register("/Coordinator_ThreatnTransEffectMembershipServlet", new MyServlet());
	} catch (Exception e) {
	    log.error("Unable to register ThreatnTransEffectMembershipServlet servlet",e);
	}
        threatSub = 
	    (IncrementalSubscription)blackboard.subscribe(threatPred);
	myServlet = new MyServlet();
    }

    public void execute() {	
	//Iterator iter = threatSub.getAddedCollection().iterator();
	//while (iter.hasNext()) {
	//    DefaultThreatModel threat = (DefaultThreatModel)iter.next();
	//    if (threat != null) {
	//	allThreats.add(threat);
	//    }
	//}
    }
    
    private class MyServlet extends HttpServlet {
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) {
            
	    response.setContentType("text/html");
	    
	    try {
		PrintWriter out = response.getWriter();
		out.println("<html>");
		out.println("<META HTTP-EQUIV=\"PRAGMA\" CONTENT=\"NO-CACHE\">");
		out.println("<head></head><body>");
		out.println("<h2>Coordinator Threat & TransitiveEffect Membership Servlet</h2>");
		out.println("<hr>");
		out.println("<h3>Threats</h3><UL>");
		Iterator iter = threatSub.getCollection().iterator();
		while (iter.hasNext()) {
		    DefaultThreatModel threat = (DefaultThreatModel)iter.next();
		    if (threat != null) {
			out.println("<LI><p><b>ThreatName: </b>"+threat.getName() + "<nbsp> ------- Asset type = "+threat.getAssetType());
                        //output threat membership
                        Vector assets = threat.getAssetList();
			out.println("<UL><LI>Members:<UL>");
                        
                        for (Iterator i=assets.iterator();i.hasNext();) {
                            out.println("<LI>"+ ( (AssetTechSpecInterface)i.next()).getName() );
                        }
			out.println("</UL>"); //end of members list
                        
                        EventDescription ed = threat.getThreatDescription().getEventThreatCauses();
                        TransitiveEffectDescription ted = ed.getTransitiveEffect();
                        emitTransitiveEffect(ted, out);
                        //check for child trans effect
                        if (ted != null && ted.getTransitiveEvent() != null) {
                            TransitiveEffectDescription childTED = ted.getTransitiveEvent().getTransitiveEffect();
                            if (childTED != null) {
                                emitTransitiveEffect(childTED, out);                        
                                if (childTED.getTransitiveEvent() != null) {
                                    TransitiveEffectDescription grandchildTED = childTED.getTransitiveEvent().getTransitiveEffect();
                                    if (grandchildTED != null) {
                                        emitTransitiveEffect(grandchildTED, out);                        
                                        if (grandchildTED.getTransitiveEvent() != null) {
                                            TransitiveEffectDescription greatgrandchildTED = grandchildTED.getTransitiveEvent().getTransitiveEffect();
                                            if (greatgrandchildTED != null) {
                                                emitTransitiveEffect(greatgrandchildTED, out);                        
                                            }
                                        }
                                    }
                                }
                            }
                        }                        
			out.println("</UL>"); //end of one threat
		    }
		}
        	out.println("</UL>"); //end of all threats
		out.println("<hr>");
		out.println("</html>");
		out.close();
	    } catch (IOException e) { 
		log.error("Exception writing servlet response",e);
	    }
	}
        
        private void emitTransitiveEffect(TransitiveEffectDescription ted, PrintWriter out) {                        

            if (ted == null) {
                out.println("<LI>No (more) transitive effects"); //end of one threat                            
            } else
            if (ted.getInstantiation() == null) {
                out.println("<LI>Transitive effect for Event ["+ted.getTransitiveEventName()+"], but not instantiated"); //end of one threat                            
            } else {
                AssetType at = ted.getTransitiveAssetType();
                Vector tAssets = ted.getInstantiation().getAssetList();
                out.println("<LI>Transitive Effect Members for Event ["+ted.getTransitiveEventName()+"], on asset type="+at+" <UL>");

                for (Iterator i=tAssets.iterator();i.hasNext();) {
                    out.println("<LI>"+ ( (AssetTechSpecInterface)i.next()).getName() );
                }
                out.println("</UL>"); //end of members list

            }

        }
    }
}
