/*
 * org.cougaar.core.mts.logging.PMLoggingProbePlug.java
 *
 * <copyright>
 *  Copyright 2002,2003 Object Services and Consulting, Inc. (OBJS),
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  The code in this file is free software; you can redistribute it and/or modify
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
 * 20 Feb 2003: Created. (OBJS)
 */

package org.cougaar.core.mts.logging;

import org.cougaar.core.mts.*;
import org.cougaar.core.service.LoggingService;

import org.cougaar.core.component.ServiceBroker;

import org.cougaar.util.log.*;

import com.objs.surveyor.probemeister.probe.GenericArgumentArray;



/*  This class contains probe plugs that 
 *  are called by probes stubs that have been inserted into Cougaar.
 *
 *  These particular plugs emit LogEvent messages to the console, or over a socket to
 *  the MsgAudit tool (if it has been turned on). Used in conjunction with ProbeMeister
 *  one can insert log events / debug stmts on the fly, while Cougaar is running.
 *
 * @author  pazandak@objs.com
 */
public class PMLoggingProbePlug {
    
  //private static final String SEND_PRE = "Before:";
  //private static final String RECV_PRE = "After:";
    
    
    /* 
     * Creates a new instance of PMLoggingProbePlug. Not used.
     */
    public PMLoggingProbePlug() {
    }
    
    
     /* 
      * This probe plug method emits the VM Name, Probe ID, user-supplied message,
      * calling stub name, and method arguments (as strings) to the console.
      *
      *<b> Currently does NOT work because there is not a way to statically get a serviceBroker, which is required to
      *    get access to the WhitePages service. </b>
      *
      * ProbePlug has the following signature:
      * <b>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Thread;Lcom/objs/surveyor/probemeister/probe/GenericArgumentArray;)V </b>
      *
      * This signature matches up with StatementFactory.createStubProbe_ArgsCallWithMsgStmt, and is used by
      * the Stub_PassMethodArgsAndString stub.
      */
    public static void PP_CougaarTrafficLogPointPlug1(
                                              String _vm, 
                                              String _probeID, 
                                              String _stubName, 
                                              String _instrumentedMethod, 
                                              String _msg,
                                              Thread _thr, 
                                              GenericArgumentArray o) {
                              
                                                
        Logging.currentLogger().debug("PP_CougaarTrafficLogPointPlug1 called.");
        Logging.currentLogger().warn("PP_CougaarTrafficLogPointPlug1 cannot run, no access to getServiceBroker() in static context.");

        return;
        //handleCall(_this, o, _instrumentedMethod, _msg);
    }

     /* 
      * This probe plug method emits the VM Name, Probe ID, user-supplied message,
      * calling stub name, and method arguments (as strings) to the console.
      *
      * ProbePlug has the following signature:
      * <b>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Thread;Lcom/objs/surveyor/probemeister/probe/GenericArgumentArray;Ljava/lang/Object;)V </b>
      *
      * This signature matches up with StatementFactory.createStubProbe_ArgsCallWithMsgStmt, and is used by
      * the Stub_PassMethodArgsAndString stub.
      */
    public static void PP_CougaarTrafficLogPointPlug2(
                                              String _vm, 
                                              String _probeID, 
                                              String _stubName, 
                                              String _instrumentedMethod, 
                                              String _msg,
                                              Thread _thr, 
                                              GenericArgumentArray o,
                                              Object _this) {
                              
                                                
        Logging.currentLogger().debug("PP_CougaarTrafficLogPointPlug2 called.");
        
        handleCall(_this, o, _instrumentedMethod, _msg);
        
    }
    
    
    /*
     * Helper method to generate a log event to be emitted
     */
    private static void handleCall(Object _this, GenericArgumentArray _o, String _instrumentedMethod, String _msg) {

        AttributedMessage attrMsg = null; 
        if (_o!=null && _o.length()>0) {
            for (int i=0; i<_o.length(); i++) {                
                Object obj = _o.getValue(i);
                if (obj instanceof AttributedMessage) { 
                    attrMsg = (AttributedMessage)obj;
                    break;
                }
            }
        } 
        
        if (attrMsg == null) {
            Logging.currentLogger().debug("PMLoggingProbePlug:: no argument in "+_instrumentedMethod+" is an Attributed Msg. Exiting.");
            return;
        }

        //Now make sure we can get a service broker from 'this'
        ServiceBroker sb = getServiceBroker(_this);
        if (sb == null) {
            Logging.currentLogger().warn("PMLoggingProbePlug:: "+_instrumentedMethod+" does not have access to getServiceBroker(). Exiting.");
            return;
        }        
        
        emitLogEvent(_this, sb, attrMsg, _msg, _instrumentedMethod);
    
    }
    
    
    /*
     * Helper method to generate a log event to be emitted
     * @return a service broker instance from the instrumented object
     */
    private static ServiceBroker getServiceBroker(Object _this) {
    
        try {
            java.lang.reflect.Method m = _this.getClass().getMethod("getServiceBroker", null);
            return (ServiceBroker)m.invoke(_this, null);            
        } catch (Exception e) {            
            return null;    
        }
    }
    
    
    /*
     * Helper method to emit a log event 
     */
    private static void emitLogEvent(Object _requestor, ServiceBroker _sb, AttributedMessage _attrMsg, String _msg, String _instrumentedMethod) {
        
        String logpointname = null;
        boolean isSend = true;
        if (_msg != null) {
            if (_msg.startsWith("Before:")) {
                logpointname = _msg + _instrumentedMethod;
            } else if (_msg.startsWith("After:")) {
                logpointname = _msg + _instrumentedMethod;
                isSend = false;
            } else {
                Logging.currentLogger().warn("PMLoggingProbePlug:: LogPointEvent msg did not start with 'Before:' or 'After:'. Ignoring");
                return;
            }
        }        
        LogEventWrapper evt = createAuditData(_requestor, _sb, logpointname, _attrMsg, isSend);
        try {
            if (evt != null)
                LogEventRouter.getRouter().routeEvent(evt);
        } catch (Exception e) {
            Logging.currentLogger().warn("PMLoggingProbePlug:: Exception routing event:"+e);
        }
    }
        
    
    
    /*
     * Helper method to generate a log event to be emitted
     * @return LogEventWrapper
     */
     private static LogEventWrapper createAuditData (Object _requestor, ServiceBroker _sb, String _tag, AttributedMessage _msg, boolean _isSend) {

        LogEventWrapper event;

        try {
            MessageAddress fromAgent = _msg.getOriginator();
            MessageAddress toAgent = _msg.getTarget();

            String to   = null;

            if (_isSend) { //Get Sender data
               to = toAgent.toString();
            } else { //This is an incoming msg - Grab node/incarnation # for sender from msg
                try {
                    AgentID agent = AgentID.getAgentID (_requestor, _sb, toAgent);
                    to = agent.getNodeName() + "." + agent.getAgentName() + "." + agent.getAgentIncarnation();          
                } catch (UnregisteredNameException une) {
                    to = "UnregisteredNameException_for_"+toAgent;
                } catch (Exception e) {
                    to = e.toString()+toAgent;
                }
            }

            String from = (String)_msg.getAttribute(Constants.AUDIT_ATTRIBUTE_FROM_NODE) + "." + 
              fromAgent + "." + 
              (Long)_msg.getAttribute(Constants.AUDIT_ATTRIBUTE_FROM_INCARNATION); 

            Integer num = (Integer)_msg.getAttribute(Constants.AUDIT_ATTRIBUTE_NUMSEQ) ;
            String numS;
            numS = (num==null)? "null" : num.toString(); //num HAS been null before **

            String[] data = {"TYPE", "TRAFFIC_EVENT", "lpName", _tag, "time", ""+now(), "from", from, "to", to, "num", numS};
            return new LogEventWrapper(null, LoggingService.INFO, data, null, "LP");

                //    public LogEventWrapper(LoggingService log, int logLevel, String[] evtData, 
                //                    String toStringMsg, String xmlTag) {
        } catch (Exception e) {
            Logging.currentLogger().warn("PMLoggingProbePlug:: Exception generating LogEventWrapper: "+e);
            e.printStackTrace();
            return null;
        }
    }

    /*
     * @return current time in milliseconds
     */
    private static long now () {
        return System.currentTimeMillis();
    }
    
    
    
}
