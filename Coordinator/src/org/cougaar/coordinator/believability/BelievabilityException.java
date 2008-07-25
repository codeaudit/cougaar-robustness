/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: BelievabilityException.java,v $
 *</NAME>
 *
 * <copyright>
 *  Copyright 2004 Telcordia Technologies, Inc.
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA)
 *  and the Defense Logistics Agency (DLA).
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

package org.cougaar.coordinator.believability;

/**
 * Simple exception class for the believability plugin
 */
public class BelievabilityException extends Exception
{

    //------------------------------------------------------------
    // public interface
    //------------------------------------------------------------

    /**
     * Constructor
     **/
    public BelievabilityException( )
    {
        super();
    } // constructor BelievabilityException

    /**
     * Constructor that saves the error message
     * @param method_name The qualified name of the method that threw the
     *                    exception
     * @param message A string to associate with this exception.
     */
    public BelievabilityException( String method_name, String message ) {
        super( method_name + ": " + message );
    } // constructor BelievabilityException
    
    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

} // class BelievabilityException
