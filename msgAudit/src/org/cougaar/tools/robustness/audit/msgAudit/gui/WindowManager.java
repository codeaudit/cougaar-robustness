/*
 * WindowManager.java
 *
 * Created on February 21, 2003, 5:03 PM
 */

package org.cougaar.tools.robustness.audit.msgAudit.gui;

import java.util.Vector;
import java.util.Iterator;

import javax.swing.JFrame;
import java.awt.Component;

/**
 *
 * @author  Administrator
 */
public class WindowManager {
    
    Vector wins;
    
    static WindowManager mgr;    
    static {
        mgr = new WindowManager();
    }
    static public WindowManager getMgr() { return mgr; }
    
    /** Creates a new instance of WindowManager */

    private WindowManager() {
        
        wins = new Vector();
    }
    

    public Component needWindow(Class _cls) {
        
        Iterator iter = wins.iterator();
        while (iter.hasNext()) {
            Component c = (Component)iter.next();
            if (c.getClass().equals(_cls) && !c.isVisible()) {
//Thread.currentThread().dumpStack();
//System.out.println("***** WindowMgr::Found class!!");
                return c;
            }
        }
        return null;
    }
     
    public void registerWindow(Component _c) {        
        wins.addElement(_c);
    }
    
}
