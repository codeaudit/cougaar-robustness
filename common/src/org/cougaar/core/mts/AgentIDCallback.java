/*
 * <copyright>
 *  Copyright 2003 Object Services and Consulting, Inc. (OBJS),
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
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
 *
 * CHANGE RECORD 
 * 30 May 2002: Created. (104B)
 */

package org.cougaar.core.mts;

import java.net.URI;
import java.util.Hashtable;

import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Callback;
import org.cougaar.core.service.wp.Request;
import org.cougaar.core.service.wp.Response;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

public class AgentIDCallback implements Callback {

    private Logger log = null;
    private Object lock;
    private Hashtable ht;
    
    private AgentIDCallback (Object lock, Hashtable ht) {
        this.lock = lock;
        this.ht = ht;
    }
    
    public static AgentIDCallback getAgentIDCallback(Object lock, 
                                              Hashtable ht) {
	return new AgentIDCallback(lock, ht);
    }
    
    public void execute(Response response) {
	Response.Get rg = (Response.Get) response;
	AddressEntry entry = rg.getAddressEntry();
        if (log == null) log = Logging.getLogger(AgentIDCallback.class);
	if (log.isDebugEnabled())
	    log.debug(""+entry);
	synchronized (lock) {
	    if (entry == null) {
		Request.Get req = (Request.Get)rg.getRequest();
		String name = req.getName();
		CbTblEntry cbte = (CbTblEntry)ht.get(name);
		cbte.pending = false;
		cbte.result = null;           
	    } else {
		String name = entry.getName();
		CbTblEntry cbte = (CbTblEntry)ht.get(name);
		cbte.pending = false;
		cbte.result = entry.getURI();
	        if (log.isDebugEnabled())
	          log.debug(""+cbte);
	    } 
	}
    }

    public String toString() {
	return "(AgentIDCallback: lock="+lock+",ht="+ht+")";
    }
    
}


