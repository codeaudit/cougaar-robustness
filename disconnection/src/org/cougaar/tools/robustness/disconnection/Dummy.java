/*
 * Dummy.java
 *
 * Created on August 23, 2003, 8:53 PM
 */

package org.cougaar.tools.robustness.disconnection;

/**
 *
 * @author  administrator
 * @version 
 */
public class Dummy implements java.io.Serializable {
    
    private String state = "DUMMY";

    /** Creates new Dummy */
    public Dummy(String d) {
        state = d;
    }

}
