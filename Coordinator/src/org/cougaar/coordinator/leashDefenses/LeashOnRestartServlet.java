/*
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
