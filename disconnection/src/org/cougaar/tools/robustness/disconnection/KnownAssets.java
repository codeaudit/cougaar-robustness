/*
 * KnownAssets.java
 *
 * Created on July 22, 2004, 6:26 PM
 */

package org.cougaar.tools.robustness.disconnection;

/**
 *
 * @author  David Wells - OBJS
 * @version 
 */

import org.cougaar.util.UnaryPredicate;

public class KnownAssets extends java.util.HashSet {

    /** Creates new KnownAssets */
    public KnownAssets() {
    }

    public static final UnaryPredicate pred = new UnaryPredicate() {
            public boolean execute(Object o) {
                if ( o instanceof KnownAssets ) {
                    return true ;
                }
                return false ;
            }
        };


}
