/*
 * File:           SocietyHandlerInterface.java
 * Date:           August 27, 2003  4:55 PM
 *
 * @author  Administrator
 * @version generated by NetBeans XML module
 */
package org.cougaar.coordinator.tools;

import org.xml.sax.*;

public interface SocietyHandlerInterface extends ContentHandler {
    
    /**
     * A container element start event handling method.
     * @param meta attributes
     */
    public void start_node(final Attributes meta, SocietyTree tree) throws SAXException;
    
    /**
     * A container element end event handling method.
     */
    public void end_node() throws SAXException;

    
    /**
     * A container element start event handling method.
     * @param meta attributes
     */
    public void start_agent(final Attributes meta, SocietyTree tree) throws SAXException;
    
    /**
     * A container element end event handling method.
     */
    public void end_agent() throws SAXException;
    
    /**
     * A container element start event handling method.
     * @param meta attributes
     */
    public void start_host(final Attributes meta, SocietyTree tree) throws SAXException;
    
    /**
     * A container element end event handling method.
     */
    public void end_host() throws SAXException;
    
    /**
     * A container element start event handling method.
     * @param meta attributes
     */
    public void start_society(final Attributes meta) throws SAXException;
    
    /**
     * A container element end event handling method.
     */
    public void end_society() throws SAXException;
    
}

