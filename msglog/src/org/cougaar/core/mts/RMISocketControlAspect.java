/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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
 */

package org.cougaar.core.mts;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;

import java.net.Socket;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimerTask;


/**
 * This aspect adds simple statistics gathering to the client side
 * OutputStream of RMI connections.
 */
public class RMISocketControlAspect
    extends StandardAspect

{
    private Impl impl;

    public RMISocketControlAspect() {
    }

    public void load() {
	super.load();

	Provider provider = new Provider();
	impl = new Impl();
	getServiceBroker().addService(RMISocketControlService.class, 
				      provider);
    }

    public Object getDelegate(Object object, Class type) 
    {
	if (type == Socket.class) {
	    impl.cacheSocket((Socket) object);
	}
	return null;
    }

    private class Provider implements ServiceProvider {
	public Object getService(ServiceBroker sb, 
				 Object requestor, 
				 Class serviceClass) 
	{
	    if (serviceClass == RMISocketControlService.class) {
		return impl;
	    } else {
		return null;
	    }
	}

	public void releaseService(ServiceBroker sb, 
				   Object requestor, 
				   Class serviceClass, 
				   Object service)
	{
	}
    }

    private class Impl implements RMISocketControlService {
	HashMap sockets,      // host:port -> list of sockets
	    references,       // MessageAddress -> Remote stub 
	    addresses,        // Remote stub -> MessageAddress
	    default_timeouts, // MessageAddress -> timeout
	    referencesByKey;  // host:port -> Remote stub

	private Impl() {
	    sockets = new HashMap();
	    references = new HashMap();
	    addresses = new HashMap();
	    default_timeouts = new HashMap();
	    referencesByKey = new HashMap();
	    TimerTask reaper = new TimerTask() {
		    public void run() {
			reapClosedSockets();
		    }
		};
	    getThreadService().schedule(reaper, 0, 5000);
	}

	private String getKey(String host, int port) {
	    return getKey(host, Integer.toString(port));
	}
	
	private String getKey(String host, String port) {
	    // May need to canonicalize the host
	    return host+ ":" +port;
	}

	private String getKey(Remote ref) {
	    // Dig out the host and port, then look it up in 'sockets'.
	    // form is
	    // classname[RemoteStub [ref: [endpoint:[host:port](local),objID:[0]]]]
	    String refString = ref.toString();
	    int host_start = refString.indexOf("[endpoint:[");
	    if (host_start < 0) return null;
	    host_start += 11;
	    int host_end = refString.indexOf(':', host_start);
	    if (host_end < 0) return null;
	    String host = refString.substring(host_start, host_end);
	    int port_start = 1 + host_end;
	    int port_end = port_start;
	    int port_end_1 = refString.indexOf(',', host_end);
	    int port_end_2 = refString.indexOf(']', host_end);
	    if (port_end_1 < 0 && port_end_2 < 0) return null;
	    if (port_end_1 < 0) 
		port_end = port_end_2;
	    else if (port_end_2 < 0)
		port_end = port_end_1;
	    else
		port_end = Math.min(port_end_1, port_end_2);
	    String portString = refString.substring(port_start, port_end);

	    String key = getKey(host, portString);
	    referencesByKey.put(key, ref);

	    return key;
	}

	private String getKey(Socket skt) {
	    String host = skt.getInetAddress().getHostAddress();
	    int port = skt.getPort();
	    return getKey(host, port);
	}

	private Integer getDefaultTimeout(String key) {
	    Object ref = referencesByKey.get(key);
	    Object addr = addresses.get(ref);
	    Integer result = (Integer)default_timeouts.get(addr);
	    return result;
	}

	private void cacheSocket (Socket skt) {
	    String key = getKey(skt);
	    Integer timeout = (Integer) getDefaultTimeout(key);
	    if (timeout != null) {
		try {
		    skt.setSoTimeout(timeout.intValue());
		} catch (java.net.SocketException ex) {
		    // Don't care
		}
	    }
	    synchronized (this) {
		ArrayList skt_list = (ArrayList) sockets.get(key);
		if (skt_list == null) {
		    skt_list = new ArrayList();
		    sockets.put(key, skt_list);
		}
		skt_list.add(skt);
	    }
	}

	synchronized void reapClosedSockets() {
	    // Prune closed sockets
	    Map.Entry entry;
	    Socket socket;
	    Iterator itr2;
	    Iterator itr = sockets.entrySet().iterator();
	    while (itr.hasNext()) {
		entry = (Map.Entry) itr.next();
		itr2 = ((ArrayList) entry.getValue()).iterator();
		while (itr2.hasNext()) {
		    socket = (Socket) itr2.next();
		    if (socket.isClosed()) itr2.remove();
		}
	    }
	}

	synchronized boolean setSoTimeout(Remote reference, int timeout)
	{
	    String key = getKey(reference);
	    ArrayList skt_list =  (ArrayList) sockets.get(key);
	    if (skt_list == null) return false;
	    boolean success = false;
	    Iterator itr = skt_list.iterator();
	    while (itr.hasNext()) {
		Socket skt = (Socket) itr.next();
		try { 
		    skt.setSoTimeout(timeout);
		    success = true;
		} catch (java.net.SocketException ex) {
		    itr.remove();
		}
	    }
	    return success;
	}

	public boolean setSoTimeout(MessageAddress addr, int timeout) {
	    // Could use the NameService to lookup the Reference from
	    // the address.
	    default_timeouts.put(addr, new Integer(timeout));
	    Remote reference = (Remote) references.get(addr);
	    if (reference != null) {
		return setSoTimeout(reference, timeout);
	    } else {
		return false;
	    }
	}

	public synchronized void setReferenceAddress(Remote reference, 
						     MessageAddress addr)
	{
	    references.put(addr, reference);
	    addresses.put(reference, addr);
	    Integer timeout = (Integer) default_timeouts.get(addr);
	    if (timeout != null) setSoTimeout(reference, timeout.intValue());
	}

      public Socket getSocket(MessageAddress addr) 
      {
          Remote ref = (Remote)references.get(addr);
          if (ref == null) return null;
          String key = getKey(ref);
          ArrayList skt_list = (ArrayList)sockets.get(key);
	    if (skt_list == null) return null;
          if (skt_list.size() > 1 && loggingService.isWarnEnabled())
              loggingService.warn(skt_list.size() + " sockets with same key = " + key);
          Socket s = (Socket)skt_list.get(0);
          return s;
	}
    }
}
