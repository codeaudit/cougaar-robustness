/*
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
 *
 * CHANGE RECORD 
 * Created on February 12, 2003, 8:49 AM
 */

package org.cougaar.core.mts.logging;

import org.cougaar.core.service.LoggingService;
import org.cougaar.util.log.*;

import org.apache.log4j.*;
import org.apache.log4j.net.SocketAppender;
import org.apache.log4j.spi.*;
import java.util.Properties;
import java.util.Enumeration;
import org.apache.log4j.helpers.NullEnumeration;

import java.io.ObjectOutputStream;

/*
 * This class routes log events by default to the standard Cougaar logger.
 * If enabled, by setting several commandline parameters, it will route events to
 * a specified Log4J-compliant socket for remote logging.
 *
 * The parameters include:
 * - org.cougaar.core.mts.logging.Log4JHost - the remote host
 * - org.cougaar.core.mts.logging.Log4JPort - the socket port #
 * - org.cougaar.core.mts.logging.Log4JLogger - a logger name
 * - org.cougaar.core.mts.logging.Log4JLevel - a Log4J Level value (e.g. debug)
 */
public class LogEventRouter {
    
    static private boolean DEBUG = true;
    
    static LogEventRouter router;
    private int loggingPort = 0; //0 denotes no Log4J logging.
    private String loggerName;
    private String loggingHost;
    private Level log4JLevel;
    private boolean logToCougaar = true;
    private org.apache.log4j.Logger log4JLogger = null;
    private org.cougaar.util.log.Logger cougaarLogger = null;
    
    static {
        router = new LogEventRouter();
    }
    
    /* 
     * @return the single instance of LogEventRouter
     */
    public static LogEventRouter getRouter() { return router; }
    
    /* 
     * Creates a new instance of LogEventRouter 
     */
    private LogEventRouter() {
        
        if (DEBUG) {
            Logging.currentLogger().debug("LogEventRouter: Initialization****************");         
        }
        
        try {
            //This is a mandatory property to log to a Log4J socket
            String s = "org.cougaar.core.mts.logging.Log4JHost";
            loggingHost = (String)System.getProperty(s,null);
            
            if (loggingHost != null) { //look for the rest of the properties
            
                //This is a mandatory property to log to a Log4J socket
                s = "org.cougaar.core.mts.logging.Log4JPort";
                loggingPort = Integer.valueOf(System.getProperty(s,"0")).intValue();

                s = "org.cougaar.core.mts.logging.Log4JLogger";
                loggerName = (String)System.getProperty(s,"DEFAULT_LOGGER");

                s = "org.cougaar.core.mts.logging.Log4JLevel";
                String lev = (String)System.getProperty(s,"DEBUG");
                log4JLevel = mapStringToLog4JLevel(lev);
                
                if ( loggingPort > 0 ) { // then we're going to route to a socket.

                    Logging.currentLogger().debug("**** Initing Msg Traffic Logging Socket ***");
                    log4JLogger = org.apache.log4j.Logger.getLogger(loggerName);
                    
                    if (log4JLogger.getAllAppenders() instanceof NullEnumeration) {
                        Logging.currentLogger().debug("**** Found no other appenders.");
                    } else {
                        Logging.currentLogger().debug("**** Found other appenders!");
                        log4JLogger.removeAllAppenders();
                    }
                    
                    log4JLogger.addAppender(new SocketAppender(loggingHost, loggingPort));
                    log4JLogger.setLevel(log4JLevel);
                    logToCougaar = false;
                    
                    //Logging.currentLogger().debug("**** StreamMagic = "+ ObjectOutputStream.STREAM_MAGIC); 
                    //Logging.currentLogger().debug("**** StreamVersion = "+ ObjectOutputStream.STREAM_VERSION); 
                    
                }
            }
        } catch (Exception e) {
            Logging.currentLogger().warn("Could not initialize. Exception was: "+e);
        }
    }
    
    
    /*
     * Routes the specified event via cougaar logging or sockets, as configured via command line parameters
     *
     */
    public void routeEvent(LogEventWrapper event) {
    
        Throwable thr = event.getThrowable();
        if (logToCougaar) {

            LoggingService log = event.getCougaarLogger();
            if (log != null) { //use specified logger
                log.log(event.getLogLevel(), event.toString(), thr);
            } else { //look up any cougaar logger
                cougaarLogger = Logging.currentLogger();
                cougaarLogger.log(event.getLogLevel(), event.toString(), thr);
            }
            if (DEBUG) { 
                Logging.currentLogger().debug("LogEventRouter: Logging event to Cougaar w/throwable***********\n"+event.toString() + event.toString()+"\nLogLevel="+event.getLogLevel());         
            }            
        } else { //send to Log4J
            
            if (DEBUG) {
                Logging.currentLogger().debug("LogEventRouter: Logging event to Socket ************");         
            }
            String FQCN = (org.apache.log4j.Logger.class).getName();
            LogEvent evt = new LogEvent(FQCN, log4JLogger, event.getLog4JLevel(), 
                                        (Object)event.getEventData(), (Throwable)null);
            log4JLogger.callAppenders(evt);
        }
        
        
    }
        
    /**
     * @return  Returns Priority object for String log level value
     */    
    public Level mapStringToLog4JLevel(String loggingLevel) {
        
        if ( loggingLevel.equals("DEBUG") ) { return Level.DEBUG; }
        if ( loggingLevel.equals("ERROR") ) { return Level.ERROR; }
        if ( loggingLevel.equals("FATAL") ) { return Level.FATAL; }
        if ( loggingLevel.equals("INFO") )  { return Level.INFO; }
        if ( loggingLevel.equals("SHOUT") ) { return Level.WARN; } //Mapped SHOUT to WARN, no equivalent
        if ( loggingLevel.equals("WARN") )  { return Level.WARN; }
        return Level.DEBUG; //default
    }
    
    
}
