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

package org.cougaar.coordinator.leashDefenses;

import java.io.PrintWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StreamTokenizer;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.cougaar.core.servlet.BaseServletComponent;

public class LeashOnRestartServlet extends BaseServletComponent {

    protected Servlet createServlet() {
	return new MyServlet();
    }
    
    public String toString() {
	return "\""+getPath()+"\" servlet";
    }
    
    protected String getPath() {
	return "/LeashOnRestart";
    }  
    
    private class MyServlet extends HttpServlet {
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) {
	    String leashOnRestart = request.getParameter("LeashOnRestart");
	    String unleashOnRestart = request.getParameter("UnleashOnRestart");
	    response.setContentType("text/html");
	    try {
		PrintWriter out = response.getWriter();
		if (leashOnRestart != null) {
		    System.setProperty("org.cougaar.coordinator.leashOnRestart", "true");
		    sendData(out);
		    out.println("<center><h2>Status Changed - Defenses will be Leashed on subsequent Restarts</h2></center><br>");
		} else if (unleashOnRestart != null) {
		    System.setProperty("org.cougaar.coordinator.leashOnRestart", "false");
		    sendData(out);
		    out.println("<center><h2>Status Changed - Defenses will be Unleashed on subsequent Restarts</h2></center><br>");
		} else {
		    sendData(out);
		}
		out.close();
	    } 
	    catch (java.io.IOException e) { e.printStackTrace(); }
	}
	
	private void sendData(PrintWriter out) {
	    out.println("<html><head></head><body>");
	    out.println("<h2><center>LeashOnRestartServlet</center></h2>");
	    out.println("<form name=\"myForm\" method=\"get\" >" );
	    out.println("<input type=submit name=\"LeashOnRestart\" value=\"LeashOnRestart\"> <br>");
	    out.println("<input type=submit name=\"UnleashOnRestart\" value=\"UnleashOnRestart\"> <br>");
	    out.println("</form>");        
	}
    }
}
