/*
 * <copyright>
 *  Copyright 2001 Object Services and Consulting, Inc. (OBJS),
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
 * 22 Jan 2003: Created. (OBJS)
 * 06 Mar 2003: Ported to Cougaar 10.2 (OBJS)
 */

package org.cougaar.core.mts;

import java.io.*;
import java.util.*;

import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

import org.cougaar.core.service.ThreadService;

import org.cougaar.core.component.ServiceBroker;

import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Application;
import org.cougaar.core.service.wp.WhitePagesService;

import org.cougaar.core.qos.metrics.MetricsUpdateService;
import org.cougaar.core.qos.metrics.Constants;
import org.cougaar.core.qos.metrics.MetricImpl;
import org.cougaar.core.qos.metrics.Metric;

import java.net.URI;

/**
 **  An aspect which implements a heartbeat piggybacking / optimization scheme.
 **/

public class HeartbeatPiggybackerAspect extends StandardAspect
{
    //In an AttributedMsg this labels the vector of HB msgs.
    private static final String HEARTBEAT_PIGGYBACK_VECTOR = "HeartbeatPiggybackVector";

    private static HeartbeatPiggybackerAspect instance = null;
    private static HeartbeatPiggybacking_Send_Delegate send_Delegate = null;
    private static boolean piggybackingIsOn;

    private static final String AGENT = "Agent" + Constants.KEY_SEPR;
    private static final String HEARDTIME = Constants.KEY_SEPR + "HeardTime";
    private static final double SEND_CREDIBILITY = Constants.SECOND_MEAS_CREDIBILITY;

    private static Vector deliveryQueue;
    private static Hashtable waitingHeartbeats;

    private static Logger log;
    private MetricsUpdateService metricsUpdateService;
    private WhitePagesService whitePages = null;

    static
    {
        
        log = Logging.getLogger(HeartbeatPiggybackerAspect.class); 

        //  Read external properties
        String s = "org.cougaar.tools.robustness.sensors.Heartbeats.piggybackingIsOn";
        piggybackingIsOn = 
            Boolean.valueOf(System.getProperty(s,"true")).booleanValue();

        if (piggybackingIsOn) {
           //Initialize Transfer Queues
           deliveryQueue = new Vector(10);
           waitingHeartbeats = new Hashtable(5);
        } 
    }

    public HeartbeatPiggybackerAspect() 
    {}

    public void load ()
    {
        if (!piggybackingIsOn) return;
        
        super.load();
        if (log.isDebugEnabled()) 
            log.debug("PB========= HeartbeatPiggybackerAspect load() called");

        metricsUpdateService = (MetricsUpdateService)
            getServiceBroker().getService(this, MetricsUpdateService.class, null);

        whitePages = (WhitePagesService)getServiceBroker().getService(this, WhitePagesService.class, null);

        
        synchronized (HeartbeatPiggybackerAspect.class)
        {
          if (piggybackingIsOn && instance == null)
          {
            //Kick off worker thread
            //heartbeatPiggybacker = new HeartbeatPiggybacker (this);
            //(new Thread (heartbeatPiggybacker, "HeartbeatPiggybacker")).start();

            instance = this;
          }
        }
    }

    public static boolean isHeartbeatMessage (AttributedMessage msg)
    {
        String type = (String) msg.getAttribute (org.cougaar.core.mts.Constants.MSG_TYPE);
        return (type != null && type.equals (org.cougaar.core.mts.Constants.MSG_TYPE_HEARTBEAT));
    }
  
    public String getNode(String agentName) {
        
        try {
          AddressEntry ae =
            whitePages.get(
              agentName,
              Application.getApplication("topology"),
              "node");
          URI uri = ae.getAddress();
          //String host = uri.getHost();
          return uri.getPath().substring(1);
        } catch(Exception e) {
            log.warn("Exception calling white pages: "+e);
            return null;
        }
    }        
    

    /* Create delegate */
    public Object getDelegate (Object delegate, Class type) 
    {
        if (!piggybackingIsOn) return null;
        if (type == Router.class) 
        {
          Router router = (Router) delegate;
          send_Delegate = new HeartbeatPiggybacking_Send_Delegate(router);
          return send_Delegate;
        }

        return null;
    }

    /* Create reverse delegate */
    public Object getReverseDelegate (Object delegate, Class type) 
    {
        if (!piggybackingIsOn) return null;       
        if (type == MessageDeliverer.class) 
        {
          return new HeartbeatPiggybacking_Recv_Delegate((MessageDeliverer)delegate);
        }

        return null;
    }

    /********************** HeartbeatPiggybacking_Send_Delegate **************/
    class HeartbeatPiggybacking_Send_Delegate extends RouterDelegateImplBase 
    {
        //Outgoing processing - API uses Object since org.cougaar.core.mts.Router is private
        public HeartbeatPiggybacking_Send_Delegate (Router router)
        {
          super (router);
          if (log.isDebugEnabled()) 
              log.debug("PB========= HeartbeatPiggybacking_Send_Delegate initialized");
          HeartbeatQueueThread hbqt = new HeartbeatQueueThread(this);
          (new Thread (hbqt, "HeartbeatQueueThread")).start();
        }

        public void routeMessage(AttributedMessage message) {
            
            if (isHeartbeatMessage(message))
                handleHeartbeat(message);
            else {
                applyPiggybacking(message); 
                    //this should add a HEARTBEAT_PIGGYBACK_VECTOR attribute 
                    //if piggybacking is being done
        	super.routeMessage(message);
            }
        }

        public void routeMessageNow(AttributedMessage message) {
       	    super.routeMessage(message);
        }

        public void applyPiggybacking(AttributedMessage message) {
         

            //See if this message is going where some HBs need to go
    	    String toNode = getNode(message.getTarget().getAddress());
            if (log.isDebugEnabled()) 
                log.debug("PB========= Checking to piggyback HBs --> have msg going to "+toNode);

            if (toNode == null) { //white pages service could not find node / had exception
                if (log.isDebugEnabled()) {
                    log.debug("PB========= Checking to piggyback HBs --> have msg going to "+toNode);
                }
                return;
            }
                
            Object o = null;
            synchronized(waitingHeartbeats) {
                if (waitingHeartbeats != null && waitingHeartbeats.size() > 0)
                    o = waitingHeartbeats.remove(toNode);
            }
            if (o == null) { //no heartbeats to attach
                return;
            } 
            else {
                HeartbeatData hbd = (HeartbeatData)o;
                message.setAttribute(HEARTBEAT_PIGGYBACK_VECTOR, hbd.getHeartbeats() );
                if (log.isDebugEnabled()) 
                    log.debug("PB========= Removed queue & applied piggybacking going to "+toNode);
            }
        }
        
        public void handleHeartbeat(AttributedMessage message) {
            if (log.isDebugEnabled()) 
                log.debug("PB========= handleHeartbeat() Saw HB from "+message.getOriginator().getAddress());
            //String dest = message.getTarget().getAddress();
    	    String toNode = getNode(message.getTarget().getAddress());
            if (toNode == null) {
                if (log.isDebugEnabled()) 
                    log.debug("PB========= handleHeartbeat() ERROR - WhitePagesService returned null node name "); 
                return;
            }
            synchronized(waitingHeartbeats) {
                Object o = null;
                if (waitingHeartbeats != null)
                    o = waitingHeartbeats.get(toNode);
                if (o == null) { //no heartbeats to dest, so create new entry
                    waitingHeartbeats.put(toNode, new HeartbeatData(message, toNode));
                    if (log.isDebugEnabled()) 
                        log.debug("PB========= handleHeartbeat() created new queue for " + toNode); 
                    return;
                }
                else {
                    HeartbeatData hbd = (HeartbeatData)o;
                    hbd.addHeartbeat(message);
                    if (log.isDebugEnabled()) 
                        log.debug("PB========= handleHeartbeat() added to a current queue: " + toNode); 
                }
                waitingHeartbeats.notify(); //notify HB Q Thread that changes have occurred
            }
            
        }        
        
                  
    }
    /*************************************************************************/
    

    /********************** HeartbeatPiggybacking_Recv_Delegate **************/
    /*
     * This class checks all msgs for a heartbeat vector that may have been piggybacked on.
     * If the msg contains a heartbeat vector or is a heartbest, then the heartbeats are 
     * sent directly to the Metric Service & not forwarded on.
     */
    class HeartbeatPiggybacking_Recv_Delegate extends MessageDelivererDelegateImplBase 
    {
        //Incoming processing
        public HeartbeatPiggybacking_Recv_Delegate (MessageDeliverer link)
        {
          super (link);
          if (log.isDebugEnabled()) 
             log.debug("PB========= HeartbeatPiggybacking_Recv_Delegate initialized");
          ProcessDeliveryQueueThread pdqt = new ProcessDeliveryQueueThread();
          (new Thread (pdqt, "HeartbeatPiggybacker_ProcessDelivery")).start();
          
        }
        
        public MessageAttributes deliverMessage(AttributedMessage message, 
                                                MessageAddress dest)
            throws MisdeliveredMessageException
        {
            //Is this a heartbeat?
            boolean hbMsg = isHeartbeatMessage(message);
            // -- OR --
            //Is there a heartbeat piggyback vector? if so, set pbMsg = true
            Object hbVector = message.getAttribute(HEARTBEAT_PIGGYBACK_VECTOR);
            boolean pbMsg = (hbVector == null) ? false : true;
            
            //If heartbeat or piggybacking then process.
            if (pbMsg || hbMsg) {

                if (log.isDebugEnabled()) 
                    log.debug("PB========= deliverMessage() found msg with heartbeats"); 
                
                //grab HBs and sent to metric service
                deliverHeartbeats((Vector)hbVector);

                if (hbMsg) { // Mark HB as delivered
                    message.setAttribute(MessageAttributes.DELIVERY_ATTRIBUTE, MessageAttributes.DELIVERY_STATUS_DELIVERED);
                    return message; //don't deliver heartbeat msg (already sent to metric service)
                }
            }
            //deliver message
            return super.deliverMessage(message, dest);
        }

        /* Add new heartbeat vector to the queue for delivery by another thread */
        private void deliverHeartbeats(Vector hbVector) {
            synchronized(deliveryQueue) {
                if (log.isDebugEnabled()) 
                    log.debug("PB========= deliverHeartbeats() added to queue for delivery"); 
                
                deliveryQueue.add(hbVector);
                //Wake up thread if it is waiting
                deliveryQueue.notify();
            }
        }
        
    }
    /*************************************************************************/
    
    
    class ProcessDeliveryQueueThread implements Runnable {
        
        public ProcessDeliveryQueueThread() {}
        
        public void run () {
         
            try {
                Vector hbv = null;

                while ( true ) {

                    //See if there are any HB Vectors waiting to be processed
                    synchronized(deliveryQueue) {
                        while ( true ) {
                            if (deliveryQueue.size()>0) {
                                hbv = (Vector)deliveryQueue.remove(0);
                                break;
                            }
                            else { //wait to be awakened & then loop n' check for a new HB Vector
                                deliveryQueue.wait();
                            }
                        }
                    }

                    //Process HB Vector
                    long receiveTime = now();
                    for (Iterator i = hbv.iterator(); i.hasNext() ; ) {
                        String remoteAgent = (String)i.next();
                        String heard_key = AGENT + remoteAgent + HEARDTIME;

                        if (log.isDebugEnabled()) 
                            log.debug("PB========= delivery queue thread sent HB to metrics"); 

                        metricsUpdateService.updateValue(heard_key, longMetric(receiveTime));
                    }

                } //end while true
            } catch ( InterruptedException ie ) {
                log.warn("PB========= process delivery queue thread interrupted.");                 
            }
        } //end run()
    } // end class
    
    
    class HeartbeatQueueThread implements Runnable {
        
        HeartbeatPiggybacking_Send_Delegate delegate;
        
        public HeartbeatQueueThread(HeartbeatPiggybacking_Send_Delegate sd) {
            delegate = sd;        
        }
        
        public void run () {
            
            HeartbeatData hbdata = null;
            Enumeration enum = null;
            long newDeadline = 0;
            long currentTime = 0;
            long sleepTime = 0;
            
            try {
                while (true) {

                    synchronized(waitingHeartbeats) {
                        if (waitingHeartbeats.size()==0)
                            waitingHeartbeats.wait();

                        //Now, search for the soonest deadline time
                        currentTime = now();
                        //Search list for time deadlines
                        enum = waitingHeartbeats.elements();
                        while (enum.hasMoreElements()) {
                            newDeadline = 0;
                            hbdata = (HeartbeatData)enum.nextElement();
                            long dltime = hbdata.getQueueDeliverBy();
                            if (dltime <= currentTime) { //deliver now!
                                if (log.isDebugEnabled()) 
                                    log.debug("PB========= HB sending queue forwarded HBs to "+hbdata.getDestAgent()); 

                                delegate.routeMessageNow(hbdata.getMessageToDeliver());   
                                waitingHeartbeats.remove(hbdata.getDestAgent());
                                    //removing should NOT affect the enum struct
                            }
                            else { //see if this hb has to be delivered next
                                if (dltime < newDeadline | newDeadline == 0) {
                                    newDeadline = dltime;
                                }
                            }
                        }
                        sleepTime = newDeadline - now();
                        if (sleepTime > 0) {
                            if (log.isDebugEnabled()) 
                                log.debug("PB========= HB sending queue going to wait"); 
                            waitingHeartbeats.wait(sleepTime);
                        }
                        //Now, loop and process
                    }

                }
            } catch ( InterruptedException ie ) {
                if (log.isDebugEnabled()) 
                    log.debug("PB========= heartbeat queue thread interrupted.");                 
            }            
        }
    }
    
    
    class HeartbeatData {
        
        long deliverBy; //The earliest time at which one of these heartbeats needs to be sent
        Vector fromAgent_List; //List of heartbeat originators
        AttributedMessage msg;
        String destAgent;
        
        /*
         * Initializes HeartbeatData instance
         *
         */
        public HeartbeatData(AttributedMessage message, String dest) {
         
            fromAgent_List = new Vector(10);

            msg = message;
            destAgent = dest;
            insertNewHeartbeat(message);
            deliverBy = getDeliverBy(message);
        }
        
        long getQueueDeliverBy() { return deliverBy; }
        String getDestAgent() { return destAgent; }
        
        /* Return the list of Strings of source HB addresses */
        Vector getHeartbeats() {
            return fromAgent_List;
        }

        //Add heartbeat sender to list & adjust deliverBy limit
        void addHeartbeat(AttributedMessage message) {
            insertNewHeartbeat(message);
            long db = getDeliverBy(message);
            if (deliverBy > db) deliverBy = db; //found early deadline
        }
        
        void insertNewHeartbeat(AttributedMessage message) {            
            //Add sending agent to list
            fromAgent_List.add(message.getOriginator().getAddress());            
        }
        
        /* Returns the time (system time) by which this heartbeat should be sent */
        long getDeliverBy(AttributedMessage message) {
            long db;
            Integer i = (Integer)message.getAttribute(org.cougaar.core.mts.Constants.SEND_TIMEOUT);
            //set deliverby to 60 secs if null, or to now+timeout-(10 seconds)
            db = (i == null) ? 60000 : (i.longValue()+now())-10000;
            if (log.isDebugEnabled()) 
                log.debug("PB========= calculating deliverby = now() + "+db/1000 + "secs"); 
            return now()+db;
        }
        
        /* Attach current list of heartbeat senders & return original HB as transport */
        AttributedMessage getMessageToDeliver() {
            msg.setAttribute(HEARTBEAT_PIGGYBACK_VECTOR, getHeartbeats() );
            return msg;
        }        
    }
    
    private Metric longMetric(long value) {
	return new MetricImpl(new Long(value),
			      SEND_CREDIBILITY,
			      "",
			      "HeartbeatPiggybackerAspect");
    }

    
    private static long now ()
    {
        return System.currentTimeMillis();
    }

    private static String toDate (long time)
    {
        if (time == 0) return "0";

        //  Return date string with milliseconds

        String date = (new Date(time)).toString();
        String d1 = date.substring (0, 19);
        String d2 = date.substring (19);
        long ms = time % 1000;
        return d1 + "." + ms + d2;
    }
}
