/*
 * EventListener.java
 *
 * Created on February 21, 2003, 12:30 PM
 */

package LogPointAnalyzer.event;
import LogPointAnalyzer.*;

/**
 *
 * @author  Administrator
 */
public interface EventListener {
        
    public void newFinalPointEvent(LogPointLevel _lpl);
    
}
