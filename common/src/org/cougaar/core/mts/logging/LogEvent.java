/*
 * TempLogEvent.java
 *
 * Created on February 13, 2003, 1:27 PM
 */

package org.cougaar.core.mts.logging;

import org.apache.log4j.*;

/*
 *  This class is required because Log4J 
 *  treats the message Object as transient. The problem is that it then will 
 *  not get serialized & sent across the socket.
 *
 *  This class adds a new attribute "data" that is the same as the "message" object,
 *  but is serializable.
 */ 
public class LogEvent extends org.apache.log4j.spi.LoggingEvent implements java.io.Serializable {
    
    Object data; 
    
    
    /* 
     * Creates a new instance of LogEvent. 
     */
    public LogEvent(String fqnOfCategoryClass, Category category, Priority priority, Object message, Throwable throwable) {

    
        super(fqnOfCategoryClass, category, 
              priority, message, throwable);        
        
        data = message;
    } 
    
    /*
     * @return the message object
     *
     */
    public Object getData() { return data; }
    
    
}
