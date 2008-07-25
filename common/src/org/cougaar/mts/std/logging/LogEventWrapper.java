/*
 * <copyright>
 *  Copyright 2002,2004 Object Services and Consulting, Inc. (OBJS),
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
 * 23 Feb 2004: Port to 11.0
 * Created on February 12, 2003, 8:49 AM
 */

package org.cougaar.mts.std.logging;

import org.cougaar.util.log.*;
import org.cougaar.core.service.LoggingService;

import org.apache.log4j.Priority;
import java.util.Properties;
import java.util.Enumeration;

/*
 * This class defines logging events. It facilitates output
 * to different logging mechanisms, e.g. 
 *          Log4J, the console, or to Cougaar's logger
 */
public class LogEventWrapper { 
   
    
    private LoggingService cougaarLogger = null;
    private String stringified;
    private String tag = null;
    private int loggingLevel;
    private Throwable theThrowable = null;
    private Properties eventData;
    
    /** Creates a new instance of LogEventWrapper
     * @param log The Cougaar Logging Service to log this event to, if not going to Log4J.
     * @param logLevel This is the int value of the Cougaar logging level (@see org.cougaar.util.log.Logger)
     * @param evtData This is a set of attribute-value pairs containing event-specific data. The recipient of this event must understand how to process it.
     *                Any event typing information must be embedded within this Properties list.
     * @param toStringMsg This string is passed to any event handler that does not support the attribute-values data (e.g. Cougaar logging). However,
     *              if xmlTag is non-null, then an XML string will be produced by wrapping the attribute-value pairs in evtData with the supplied 
     *              xmlTag & this will be returned as the stringified value. [ <xmlTag attr1=value1 attr2=value2... /> ]
     * @param throwable An exception to be included for logging
     * @param xmlTag A tag to use to wrap the attribute-value pairs if this event is stringified
     */
    public LogEventWrapper(LoggingService log, int logLevel, Properties evtData, 
                    String toStringMsg, Throwable throwable, 
                    String xmlTag) {
    
        //event = new LoggingEvent((String)null, (Category)null, getLog4JLevel(logLevel), 
        //                          evtData, throwable);
        cougaarLogger = log;
        loggingLevel = logLevel;
        eventData = evtData;
        stringified = toStringMsg; 
        theThrowable = throwable;
        tag = xmlTag;
    }

    /** Creates a new instance of LogEventWrapper
     * @param log The Cougaar Logging Service to log this event to, if not going to Log4J.
     * @param logLevel This is the int value of the Cougaar logging level (@see org.cougaar.util.log.Logger)
     * @param evtData This is a set of attribute-value pairs containing event-specific data. The recipient of this event must understand how to process it.
     *                Any event typing information must be embedded within this Properties list.
     * @param toStringMsg This string is passed to any event handler that does not support the attribute-values data (e.g. Cougaar logging). However,
     *              if xmlTag is non-null, then an XML string will be produced by wrapping the attribute-value pairs in evtData with the supplied 
     *              xmlTag & this will be returned as the stringified value. [ <xmlTag attr1=value1 attr2=value2... /> ]
     * @param xmlTag A tag to use to wrap the attribute-value pairs if this event is stringified
     */
    public LogEventWrapper(LoggingService log, int logLevel, Properties evtData, 
                    String toStringMsg, String xmlTag) {
    
        this(log, logLevel, evtData, toStringMsg, null, xmlTag);
    }

    
    /** Creates a new instance of LogEventWrapper
     * @param log The Cougaar Logging Service to log this event to, if not going to Log4J.
     * @param logLevel This is the int value of the Cougaar logging level (@see org.cougaar.util.log.Logger)
     * @param evtData This is a String Array containing event-specific data (attribute - value pairs). The recipient of this event must understand how to process the data.
     *                Any event typing information must be embedded within this Properties list. There should be an even number of entries in the array.
     * @param toStringMsg This string is passed to any event handler that does not support the attribute-values data (e.g. Cougaar logging). However,
     *              if xmlTag is non-null, then an XML string will be produced by wrapping the attribute-value pairs in evtData with the supplied 
     *              xmlTag & this will be returned as the stringified value. [ <xmlTag attr1=value1 attr2=value2... /> ]
     * @param xmlTag A tag to use to wrap the attribute-value pairs if this event is stringified
     */
    public LogEventWrapper(LoggingService log, int logLevel, String[] evtData, 
                    String toStringMsg, String xmlTag) {
                    
        this(log, logLevel, getProps(evtData), toStringMsg, null, xmlTag);
    }

    
    /** Creates a new instance of LogEventWrapper
     * @param log The Cougaar Logging Service to log this event to, if not going to Log4J.
     * @param logLevel This is the int value of the Cougaar logging level (@see org.cougaar.util.log.Logger)
     * @param evtData This is a String Array containing event-specific data (attribute - value pairs). The recipient of this event must understand how to process the data.
     *                Any event typing information must be embedded within this Properties list. There should be an even number of entries in the array.
     * @param toStringMsg This string is passed to any event handler that does not support the attribute-values data (e.g. Cougaar logging). However,
     *              if xmlTag is non-null, then an XML string will be produced by wrapping the attribute-value pairs in evtData with the supplied 
     *              xmlTag & this will be returned as the stringified value. [ <xmlTag attr1=value1 attr2=value2... /> ]
     * @param xmlTag A tag to use to wrap the attribute-value pairs if this event is stringified
     */
    public LogEventWrapper(LoggingService log, int logLevel, String[] evtData, 
                    String toStringMsg, Throwable throwable, String xmlTag) {
    
        this(log, logLevel, getProps(evtData), toStringMsg, throwable, xmlTag);
    }
    
    private static Properties getProps(String[] data) {
     
        Properties props = new Properties();
        if ( data == null || data.length == 0 ) { // no pairs!
            return null;
        }
        
        int len = data.length ;
        boolean odd = ( (len % 2) == 1); //see if not everything is paired
        
        String key;
        String val;
        for (int i=0; i<len; ) {
            key = data[i++];
            if ( i < len ) { //then we have a pair
                val = data[i++];
            }
            else { //no value!
                val = "NO_ATTR_VALUE";
            }
            if (val == null) {
                //System.out.println("=====================> LogEventWrapper: val is null. Key = "+key);
                val = "NO_ATTR_VALUE";
            }
            props.put(key,val);
        }
        
        return props;
    }
    
    
    /**
     * @return A stringified value for this log event. If an xmlTag was supplied during 
     *      instantiation, then an XML string will be produced by wrapping the attribute-value pairs in evtData with the supplied 
     *      xmlTag & this will be returned as the stringified value. [ <xmlTag attr1=value1 attr2=value2... /> ]
     */    
    public String toString() { 

        try {
            //If no xml tag then just output stringified attr.
            if (tag == null ) {
                return stringified; 
            }
            //Start building xml
            String out = "<" + tag + " ";

            if (eventData == null) {
                return out + stringified + "/>";
            }
            Enumeration en = eventData.propertyNames();
            while (en.hasMoreElements()) {
                String key = (String) en.nextElement();
                String val = (String)eventData.getProperty(key);
                out = out + key + "=" + val + " ";
            }
            out = out + "/>";
            return out;

        } catch (Exception e) {
            return "Exception stringifying LogEventWrapper: " + e;
        }
    }
    

    /**
     * @return  The event data Properties list.
     */    
    public Properties getEventData() { return eventData; }
    
    /**
     * @return  Return this event's throwable.
     */    
    public Throwable getThrowable() { return theThrowable; }
    
    /**
     * @return  The cougaar logging service to log this event to.
     */    
    public LoggingService getCougaarLogger() { return cougaarLogger; }

    
    /**
     * @return  The Cougaar logging level passed in.
     */    
    public int getLogLevel() { return loggingLevel; }
    
    /**
     * @return  The Log4J best matching equivalent to the Cougaar logging level
     *          Maps Cougaar's SHOUT level to Log4J's WARN as there is no other 
     *          equivalent in Log4J.
     */    
    public Priority getLog4JLevel() {
        
        if ( loggingLevel == Logger.DEBUG ) { return Priority.DEBUG; }
        if ( loggingLevel == Logger.ERROR ) { return Priority.ERROR; }
        if ( loggingLevel == Logger.FATAL ) { return Priority.FATAL; }
        if ( loggingLevel == Logger.INFO )  { return Priority.INFO; }
        if ( loggingLevel == Logger.SHOUT ) { return Priority.WARN; } //Mapped SHOUT to WARN, no equivalent
        if ( loggingLevel == Logger.WARN )  { return Priority.WARN; }
        return Priority.DEBUG;
    }
    
}
