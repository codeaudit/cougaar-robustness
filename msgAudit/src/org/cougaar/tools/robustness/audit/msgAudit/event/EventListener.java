/*
 * EventListener.java
 *
 * Created on February 21, 2003, 12:30 PM
 */

package org.cougaar.tools.robustness.audit.msgAudit.event;
import org.cougaar.tools.robustness.audit.msgAudit.*;

/**
 *
 * @author  Administrator
 */
public interface EventListener {
        
    public void newFinalPointEvent(LogPointLevel _lpl);
    
}
