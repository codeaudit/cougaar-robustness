/*
 * EventHandler.java
 *
 * Created on February 21, 2003, 12:32 PM
 */

package LogPointAnalyzer.event;

import LogPointAnalyzer.*;
import java.util.Vector;
/**
 *
 * @author  Administrator
 */
public class EventHandler implements EventListener {
    
    public static Vector listeners;
    static EventHandler handler;
    static {
        handler = new EventHandler();
    }
    public static EventHandler handler() { return handler; }
    
    /** Creates a new instance of EventHandler */
    public EventHandler() {
        listeners = new Vector(1);
    }
    
    public void addListener(EventListener _l) {
        listeners.add(_l);
    }

    public void removeListener(EventListener _l) {
        listeners.remove(_l);
    }

    /* Notify all listeners about the new event */
    public void newFinalPointEvent(LogPointLevel _lpl) {
    
        for (int i=0; i<listeners.size(); i++) {
            EventListener l = (EventListener)listeners.elementAt(i);
            l.newFinalPointEvent(_lpl);
        }    
    }
    
    
}
