/*
 * TrafficAnalysisEventProcessor.java
 *
 * <copyright>
 *  Copyright 2002 Object Services and Consulting, Inc. (OBJS),
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
 * Created on February 13, 2003, 7:59 PM
 */

package LogPointAnalyzer.socket;
import LogPointAnalyzer.*;

import org.cougaar.core.mts.logging.LogEvent;
import java.util.Properties;

/**
 *
 * @author  Administrator
 */
public class TrafficAnalysisEventProcessor implements LogPointAnalyzer.socket.EventProcessor {
    
    protected String EVENT_TYPE = "TRAFFIC_EVENT";
    public String getProcessorType() { return EVENT_TYPE; }

    protected EventQueue queue = null; //Queue if this processor passes off events to another class
    public void setQueue(EventQueue _q) { queue = _q; }

    public void processEvent(LogEvent le) {
        
        if (queue == null) {
            System.out.println("Cannot forward event, queue is null");
            return;
        } 
        
        try {
            //System.out.println("Adding event to queue");
            queue.add(le);
        } catch (Exception e) {
            System.out.println("Exception adding event to queue: " + e);            
        }
    }
    
}
