/*
 * EventProcessingAppender.java
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
 * Created on February 13, 2003, 12:37 PM
 */

package org.cougaar.tools.robustness.audit.msgAudit.socket;

import java.util.Properties;
import java.util.Hashtable;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import org.apache.log4j.*;

import java.lang.reflect.Method;
import org.cougaar.core.mts.logging.LogEvent;

/**
 *
 * @author  Administrator
 */
public class EventProcessingAppender extends org.apache.log4j.AppenderSkeleton {
 
    public static final String EVENT_PROCESSOR_FILE = "LogPointProcessors.txt";
    private static Category log;
    private static Hashtable processors;
    static {
        ///log = LogPointAnalyzer.TrafficAuditor.logger();
        log = Category.getInstance("LogPointAnalyzer");
        log.addAppender(new ConsoleAppender(new SimpleLayout()));
        Priority p = Priority.toPriority("INFO");
        
        System.out.println("Initializing EventProcessingAppender: log is " + log);
        processors = new Hashtable(); 
        //readInSettings(EVENT_PROCESSOR_FILE);        
    }
    /** Creates a new instance of EventProcessingAppender */
    public EventProcessingAppender() {
    }
    
    public void addEventProcessor(EventProcessor _ep) {
        processors.put(_ep.getProcessorType(), _ep);
    }
    
    protected void append(org.apache.log4j.spi.LoggingEvent loggingEvent) {
        
        try {
            Object o;
            if (loggingEvent instanceof LogEvent) {

                LogEvent le = (LogEvent)loggingEvent;
                log.debug("It's a LogEvent!! Forwarding to processor");
                o = le.getData();
                if (o instanceof Properties) {
                    Properties p = (Properties)o;
                    String type = p.getProperty("TYPE");
                    EventProcessor ep = (EventProcessor)processors.get(type);
                    if (ep == null) { //no processor
                        log.warn("No processor for this type is registered = " + type);
                    } else {
                        ep.processEvent(le);
                    }
                } else {
                    log.warn("Event Data is wrong type!! (Event not processed): " + o.getClass().getName());
                }
            } else {
                log.warn("Event object is WRONG type!!! [" + loggingEvent.getClass().getName() + "]. Ignoring.");
                //Thread.currentThread().dumpStack();
            }
        } catch (Exception e) {
            log.warn("Exception processing event"+e);            
        }
    }
    
    public void close() {
        
        //Do something useful here
    }
    
    public boolean requiresLayout() {
        return false;
    }
    
 
     //Probably not needed...
     public Hashtable getProcessors() { return processors; }
/*
    private static void readInSettings(String _file) {
    
        //Load settings file
        File initFile = new File(_file);
        try {

            if (!initFile.exists()) {
                log.warn("EventProcessingAppender:: Cannot find Processor File: "+initFile.getAbsolutePath());
                return;
            }
            
            //Open file
            FileReader reader = new FileReader(initFile);
            log.debug("EventProcessingAppender:: Reading in Processor File: "+initFile.getAbsolutePath());
                        
            //Read data
            //1. Set up BufferedReader which has a readLine() method
            BufferedReader bReader = new BufferedReader(reader);  
            for (;;) { 
                String data = bReader.readLine();
                if (data != null) { // non-null until EOF
                    if (data.length() < 5 || data.startsWith("#")) {
                        log.debug("EventProcessingAppender::ignoring line in file - comment or length <5");
                        continue;   //essentially empty line
                    }
                    //System.out.println("Read:      "+ String.valueOf(data));
                    //Parse & Add to the list of plugCatalog 
                    if (!processEntry(data)) { //Error processing data, try to keep processing
                        log.warn("EventProcessingAppender::Error in Global settings file: "+initFile.getAbsolutePath());            
                        log.warn("EventProcessingAppender::=> Each line should contain <GlobalName>=<value>");
                    } else
                        log.debug("EventProcessingAppender::=> Global settings "+data+" has been imported.");
                } else { //no more data, EOF
                    break; 
                }
            }
            //All done, close file.
            bReader.close();

        } catch (java.io.FileNotFoundException fnf) {
            //Assume that the file never existed, so we have no file to load. OK to stop.
            log.info("EventProcessingAppender::No settings found. Looking for: "+initFile);
        } catch (java.io.IOException ioe) {
            log.error("EventProcessingAppender::IO Exception reading file...", ioe);
        } catch (NullPointerException npe) {
            log.error("EventProcessingAppender::IO Exception reading file...", npe);            
        }
    }
 */   
    
    
    /**
      * This routine extracts the var name & value from the text line.
      * It locates the correct global variable & converts the value
      * accordingly.
      */
/*     
    private static boolean processEntry(String _entry) {
        
        String type = null;
        String clsName = null;
        Class cls = null;

        //Delimiter is a '='
        java.util.StringTokenizer st = new java.util.StringTokenizer(_entry, "=");
        
        //Process Data - extract var name & value
        if (st.hasMoreTokens()) 
           type = st.nextToken();

        if (type == null) //no go, stop processing this entry
            return false;
        
        if (st.hasMoreTokens()) { //then get the value
            clsName = st.nextToken();
        }

        if (clsName == null) //no go, stop processing this entry
            return false;
        
        try {
            cls = Class.forName(clsName);
        } catch (ClassNotFoundException cne) {
            log.warn("EventProcessingAppender: Exception while loading EventProcessors from file!\n---->Class Not Found: "+clsName);
            return true;
        }
        //Add entry
        try {            
            Method m = cls.getMethod("getProcessor", null);
            EventProcessor ep = (EventProcessor)m.invoke(null, null);
            processors.put(type, ep);
        } catch (Exception e) {
           log.error("Exception loading EventProcessor from file: " + e);   
        }
        
        return true;
                
    }
 */    
}
