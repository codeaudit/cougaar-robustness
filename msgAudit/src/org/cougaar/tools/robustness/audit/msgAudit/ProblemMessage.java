/*
 * ProblemMessage.java
 *
 * Created on April 23, 2003, 11:34 AM
 */

package org.cougaar.tools.robustness.audit.msgAudit;

import java.util.Vector;
import java.util.Iterator;


/**
 * Describes a problem message
 *
 */
public class ProblemMessage {

    private LogPointEntry lpe;
    private boolean isFrom;
    
    
    private String name;
    private int seqNum;
    
    private Vector msgs;

    //Value of new agent name
    private String newName = null;
    //True if a new name has been identified
    private boolean toBeModified = false;
    //True when the new name has been applied to the msgs
    private boolean modified = false;
    
    /**
     * Contains possibly a set of LogPointEntries having hte same sequence numer
     */
    public ProblemMessage(String _name, LogPointEntry _lpe, boolean _isFrom) {
        lpe = _lpe;
        isFrom = _isFrom;
        name = _name;
        seqNum = _lpe.seqNumber();        
        
        msgs = new Vector(10);
        msgs.add(_lpe);
    }

    /**
     * Add a LogPointEntry (with the same name & seq # as this Problem Message
     */
    public void addMsg(LogPointEntry _lpe) {
        msgs.add(_lpe);
    }
    
    public String getName() { return name; }
    public int getSeqNumber() { return seqNum; }
    
    /**
     * Sets the new name for all the LogPointEntries in the ProblemMessage
     */
    public void setNewName(String _n) { 
        newName = _n;
        toBeModified = true;
    }
    
    public void abortNewName() {
        newName = null;
        toBeModified = false;
    }
    
    public void commitNewName() {
        if (!toBeModified) {
            return;
        }
        
        Iterator iter = getLPEs();
        while (iter.hasNext()) {
            LogPointEntry lpe = (LogPointEntry)iter.next();
            if (isFrom()) {
                lpe.setFrom(newName);
            } else {
                lpe.setDest(newName);
            }
        }
        modified = true;
    }

    /** Returns true if the name has been changed / resolved to a known agent's name */
    public boolean toBeModified() { return toBeModified; }
    /** Returns true if the name has been changed / resolved to a known agent's name */
    public boolean isModified() { return modified; }
    /** Returns an iterator of all LogPointEntries */
    public Iterator getLPEs() { return msgs.iterator(); }
    /** Returns the from agent name */
    public boolean isFrom() { return isFrom; }
    /** Returns the agent name (from or dest) which is the problem */
    public String toString() { return name; }
    /** Returns the vector of all LogPointEntries */
    public Vector getLPEList() { return msgs; }

}    
