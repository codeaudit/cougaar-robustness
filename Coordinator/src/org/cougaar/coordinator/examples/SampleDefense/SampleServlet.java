/*
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

package org.cougaar.coordinator.examples.SampleDefense;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
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
 * Servlet for Sample Defense
 */
public class SampleServlet extends ComponentPlugin
{
    private ServiceBroker sb;
    private LoggingService log;
    private ServletService servletSvc;

    SampleRawSensorData sensorData = null;
    SampleRawActuatorData actuatorData = null;
    MyServlet myServlet;

    private IncrementalSubscription rawSensorDataSub;

    private UnaryPredicate rawSensorDataPred = new UnaryPredicate() {
	    public boolean execute(Object o) {
		return (o instanceof SampleRawSensorData);}};
      
    private IncrementalSubscription rawActuatorDataSub;

    private UnaryPredicate rawActuatorDataPred = new UnaryPredicate() {
	    public boolean execute(Object o) {
		return (o instanceof SampleRawActuatorData);}};

    static final Hashtable codeTbl;

    static {
	 codeTbl = new Hashtable();
	 codeTbl.put("COMPLETED", "COMPLETED");
	 codeTbl.put("ABORTED", "ABORTED");
	 codeTbl.put("FAILED", "FAILED");
    }
      
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
	    servletSvc.register("/SampleDefense", new MyServlet());
	} catch (Exception e) {
	    log.error("Unable to register timeControl servlet",e);
	}
        rawSensorDataSub = 
	    (IncrementalSubscription)blackboard.subscribe(rawSensorDataPred);
        rawActuatorDataSub = 
	    (IncrementalSubscription)blackboard.subscribe(rawActuatorDataPred);
	myServlet = new MyServlet();
    }

    public void execute() {	
	Iterator iter = rawSensorDataSub.getAddedCollection().iterator();
	while (iter.hasNext()) {
	    SampleRawSensorData data = (SampleRawSensorData)iter.next();
	    if (data != null) {
		sensorData = data;
	    }
	}
	iter = rawActuatorDataSub.getAddedCollection().iterator();
	while (iter.hasNext()) {
	    SampleRawActuatorData data = (SampleRawActuatorData)iter.next();
	    if (data != null) {
		actuatorData = data;
	    }
	}
    }
    
    private class MyServlet extends HttpServlet {
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) {
            
	    int refreshRate = 10000;
	    String refreshError = null;
	    boolean sensorDataUpdated = false;
	    boolean valuesOfferedUpdated = false;
	    boolean actionValueUpdated = false;
	    boolean completionCodeUpdated = false;
            int command = -1;

	    if (request != null) {
		String parm = request.getParameter("REFRESH");
		if (parm != null) {
		    try {
			int r = Integer.parseInt(parm);
			if (r < 1000) {
			    refreshRate = 1000;
			} else {
			    refreshRate = r;
			}
		    } catch (NumberFormatException e) {
			refreshError = "Could not set refresh rate to " + parm +
			        ". NumberFormatException occurred.";
		    }
		}
		String val = request.getParameter("NEWVALUE");
		if (val != null) { //the user updated the value
		    blackboard.openTransaction();
		    Iterator iter = rawSensorDataSub.getCollection().iterator();
		    while (iter.hasNext()) {
			SampleRawSensorData data = (SampleRawSensorData)iter.next();
			if (data != null) {
			    data.setValue(val);
			    blackboard.publishChange(data);
			    sensorDataUpdated = true;
			    if (log.isDebugEnabled()) 
				log.debug(sensorData + " changed."); 
			    break; // there's only one SampleRawSensorData object
			}
		    }
		    blackboard.closeTransaction();
		}
		String[] valuesOffered = request.getParameterValues("VALUESOFFERED");
		if (valuesOffered != null && valuesOffered.length > 0) { //the user updated valuesOffered
                    HashSet hs = new HashSet();
                    for (int i=0; i<valuesOffered.length; i++) {
                        hs.add(valuesOffered[i]);
                    }
		    blackboard.openTransaction();
		    Iterator iter = rawActuatorDataSub.getCollection().iterator();
		    while (iter.hasNext()) {
			SampleRawActuatorData data = (SampleRawActuatorData)iter.next();
			if (data != null) {
			    data.setCommand(SampleRawActuatorData.SET_VALUES_OFFERED);
			    data.setValuesOffered(hs);
			    blackboard.publishChange(data);
			    valuesOfferedUpdated = true;
			    if (log.isDebugEnabled()) 
				log.debug(data + " changed."); 
			    break; // there's only one SampleRawActuatorData object
			}
		    }
		    blackboard.closeTransaction();
		}
		String actionValue = request.getParameter("START");
		if (actionValue != null) { //the user updated the value
		    blackboard.openTransaction();
		    Iterator iter = rawActuatorDataSub.getCollection().iterator();
		    while (iter.hasNext()) {
			SampleRawActuatorData data = (SampleRawActuatorData)iter.next();
			if (data != null) {
			    data.setCommand(SampleRawActuatorData.START);
			    data.setActionValue(actionValue);
			    blackboard.publishChange(data);
			    actionValueUpdated = true;
			    if (log.isDebugEnabled()) 
				log.debug(data + " changed."); 
			    break; // there's only one SampleRawActuatorData object
			}
		    }
		    blackboard.closeTransaction();
		}
		String code = request.getParameter("STOP");
		if (code != null) { //the user updated the value
		    blackboard.openTransaction();
		    Iterator iter = rawActuatorDataSub.getCollection().iterator();
		    while (iter.hasNext()) {
			SampleRawActuatorData data = (SampleRawActuatorData)iter.next();
			if (data != null) {
			    data.setCommand(SampleRawActuatorData.STOP);
			    data.setCompletionCode(code);
			    blackboard.publishChange(data);
			    completionCodeUpdated = true;
			    if (log.isDebugEnabled()) 
				log.debug(data + " changed."); 
			    break; // there's only one SampleRawActuatorData object
			}
		    }
		    blackboard.closeTransaction();
		}
	    }
	    response.setContentType("text/html");
	    
	    try {
		PrintWriter out = response.getWriter();
		out.println("<html>");
		out.println("<META HTTP-EQUIV=\"PRAGMA\" CONTENT=\"NO-CACHE\">");
		out.println("<script type=\"text/javascript\">");
		out.println("function refreshPage(){");   
		out.println("     var szURL = \"?REFRESH="+refreshRate+" \";");
		out.println("     self.location.replace(szURL);");
		out.println("}</script>");
		out.println("<head></head><body onload=\"setTimeout('refreshPage()', " +refreshRate+");\">");
		out.println("<h2>Sample Defense Servlet</h2>");
		out.println("<p>Will refresh every " + (refreshRate/1000) + " seconds. ");
		out.println("You can change this rate here: (in milliseconds)");
		out.print("<form clsname=\"myForm\" method=\"get\" >" );
		out.println("Refresh Rate: <input type=text name=REFRESH value=\""+refreshRate+"\" size=7 >");
		out.println("<input type=submit name=\"Submit\" value=\"Change\" size=10 >");
		out.println("</form>");
                if (refreshError != null) {
                    out.print("<b><font color=\"Red\">"+ refreshError + "</font></b>");
                }
		out.println("<hr>");
		out.println("<h3>Sample Diagnosis</h3>");
		Iterator iter = rawSensorDataSub.getCollection().iterator();
		while (iter.hasNext()) {
		    SampleRawSensorData data = (SampleRawSensorData)iter.next();
		    if (data != null) {
			out.println("<p><b>AssetName: </b>"+data.getAssetName());
			String[] vals = (String[])data.getPossibleValues().toArray(new String[0]);
			out.print("<form clsname=\"SENSOR\" method=\"get\" >" );
			out.print("<b>Possible Values: </b><select name=\"NEWVALUE\" size=3>");
			out.print("Value: <input type=text name=NEWVALUE value=\""+data.getValue()+"\" size=10 >");
			out.print("<p><b>Value: </b>");
			for (int i=0; i < vals.length; i++) {
			    String str = vals[i];
			    out.println("<option value=\"" + str +"\" UNSELECTED />" + str);
			}
			out.print("</select>");
			out.print("<input type=submit name=\"Submit\" value=\"Set Sensor Value\" size=16 ");
			if (sensorDataUpdated) {
			    out.println("<b><font color=\"Blue\"> Sensor Value updated.</font></b>");
			    sensorDataUpdated = false;
			}
			out.println("<input type=hidden name=\"REFRESH\" value=\""+refreshRate+"\" >");
			out.println("</form>");
			out.println("<p><b>Current Sensor Value: </b>"+data.getValue());
			break; // there should be at most one SampleRawSensorData object
		    }
		}
		out.println("<hr>");
		out.println("<h3>Sample Action</h3>");
		iter = rawActuatorDataSub.getCollection().iterator();
		while (iter.hasNext()) {
		    SampleRawActuatorData data = (SampleRawActuatorData)iter.next();
		    if (data != null) 
			{
			out.println("<p><b>AssetName: </b>"+data.getAssetName());
 
                        // display possibleValues, select valuesOffered
			out.print("<form clsname=\"SETVALUESOFFERED\" method=\"get\" >" );
       			String[] vals = (String[])data.getPossibleValues().toArray(new String[0]);
			out.print("<b>Possible Values: </b><select multiple name=\"VALUESOFFERED\" size=3>");
			for (int i=0; i < vals.length; i++) {
			    String str = vals[i];
			    out.println("<option value=\"" + str +"\" UNSELECTED />" + str);
			}
			out.print("</select>");
			out.print("<input type=submit name=\"Submit\" value=\"Set Values Offered\" size=17 >");
			out.println("<input type=hidden name=\"REFRESH\" value=\""+refreshRate+"\" >");
			out.println("</form>");

                        // display valuesOffered
			out.print("<form clsname=\"VALUESOFFERED\" method=\"get\" >" );
			vals = (String[])data.getValuesOffered().toArray(new String[0]);
			out.print("<b>ValuesOffered: </b><select name=\"VALUESOFFERED\" size=3>");
			for (int i=0; i < vals.length; i++) {
			    String str = vals[i];
			    out.println("<option value=\"" + str +"\" UNSELECTED />" + str);
			}
			out.print("</select>");
			if (valuesOfferedUpdated) {
			    out.println("<b><font color=\"Blue\">Values Offered updated.</font></b>");
			    valuesOfferedUpdated = false;
			}
			out.println("</form>");

                        // display permittedValues, select actionValue and start
			out.print("<form clsname=\"START\" method=\"get\" >" );
			vals = (String[])data.getPermittedValues().toArray(new String[0]);
			out.print("<b>Permitted Values: </b><select name=\"START\" size=3>");
			for (int i=0; i < vals.length; i++) {
			    String str = vals[i];
			    out.println("<option value=\"" + str +"\" UNSELECTED />" + str);
			}
			out.print("</select>");
			out.print("<input type=submit name=\"Submit\" value=\"Start\" size=17>");
			out.println("<input type=hidden name=\"REFRESH\" value=\""+refreshRate+"\" >");
			if (actionValueUpdated) {
			    out.println("<b><font color=\"Blue\">Action "+data.getActionValue()+" Started.</font></b>");
			    actionValueUpdated = false;
			}
			out.println("</form>");

                        // display completionCodes, select completionCode and stop
			out.print("<form clsname=\"STOP\" method=\"get\" >" );
			out.print("<b>Completion Codes: </b><select name=\"STOP\" size=3>");
			Enumeration codes = codeTbl.keys(); 
			while (codes.hasMoreElements()) {
			    String code = (String)codes.nextElement();
			    out.println("<option value=\"" + code + "\" UNSELECTED />" + code);
			}
			out.print("</select>");
			out.print("<input type=submit name=\"Submit\" value=\"Stop\" size=17>");
			out.println("<input type=hidden name=\"REFRESH\" value=\""+refreshRate+"\" >");
			if (completionCodeUpdated) {
			    out.println("<b><font color=\"Blue\">Action "+data.getCompletionCode()+".</font></b>");
			    completionCodeUpdated = false;
			}
			out.println("</form>");

			out.println("<p><b>Current Action: </b>"+data.getActionValue());
			out.println("<p><b>Current CompletionCode: </b>"+data.getCompletionCode());

			break; // there should be at most one SampleRawActuatorData object
		    }
		}
		out.println("<hr>");
		out.println("</html>");
		out.close();
	    } catch (IOException e) { 
		log.error("Exception writing servlet response",e);
	    }
	}
    }
}
