/*
 * LogPointEntry.java
 *
 * Created on February 18, 2003, 11:17 AM
 */

package org.cougaar.tools.robustness.audit.msgAudit;
import org.cougaar.core.mts.logging.LogEvent;
import java.util.Properties;

/**
 *
 * @author  Administrator
 */
public class LogPointEntry {
    
    private String to;
    private String from;
    private String time;
    private int seqNum;
    private String logpoint;
    private LogEvent logEvent;
    private Message msgRoot = null;
    private Boolean isSend = null;
    private boolean isFinalLogPoint = false;
    
    private int logPointPosition;
    
    /** Creates a new instance of LogPointEntry */
    public LogPointEntry(LogEvent _evt, LogPointVectorMgmt _mgmt) {
        Properties props = (Properties)_evt.getData();
        from = props.getProperty("from", "*error*");
        to   = props.getProperty("to",   "*error*");
        time = props.getProperty("time", "*error*");
        String seqNumStr =   props.getProperty("num", "*error*");
        logpoint = props.getProperty("lpName", "*error*");
        //"TYPE", "TRAFFIC_EVENT", "lpName", tag, "time", "" + now(), "from", from, "to", to, 
        //    "num", numS
    
        logEvent = _evt;
        
        try {
            seqNum = Integer.parseInt(seqNumStr);
        } catch (NumberFormatException nfe) {
            System.out.println("LogPointEvent seq # is faulty: " + seqNumStr + ". Assigned -1 value");
            seqNum = -1;
        }
        
        //Identify if send or recv 
        if (logPointName().startsWith(AgentData.SEND_PREFIX))            
            isSend = Boolean.TRUE;
        else
            isSend = Boolean.FALSE;
        
        setLogPointPosition(_mgmt);
        //See if this log point was found
        if (logPointPosition == -1) { //didn't find a log point entry
             System.out.println("LogPointEntry:: adding new logpoint level... " + logpoint);
        
            //Register new log point
            _mgmt.addLogPoint(new LogPointLevel(logpoint, -1, isSend.booleanValue(), logpoint ));
            setLogPointPosition(_mgmt);
            if (logPointPosition == -1) { //didn't find a log point entry
                System.out.println("LogPointEntry:: adding new logpoint level...still couldn't set pos!");
            }
        }
    }

    public String dest() { return to; }
    public String from() { return from; }
    public int seqNumber() { return seqNum; }
    public String time() { return time; }
    public String logPointName() { return logpoint; }
    public int getLogPointLevel() { return logPointPosition; }

    public void setMsgRoot(Message _msg) { msgRoot = _msg; }
    public Message getMsgRoot() { return msgRoot; }
    
    public boolean isFinalLogPoint() { return isFinalLogPoint; }
    
    /*
     * Returns TRUE if this is on the send side, FALSE if on the recv side.
     * Returns NULL if unassigned (e.g. if the LogPoint name is not known to
     * LogPointVectorMgmt)
     */
    public Boolean isSend() { return isSend; }
    public void setIsSend(boolean _s) { isSend = new Boolean(_s); }
    
    /*
     * Assigns logPointPosition to its physical Log Point position in
     * the appropriate vector (send | recv ). Assigns -1 if not found.
     * Sets isSend to TRUE if this is a send-side msg, FALSE if RECV SIDE, and
     * to NULL otherwise.
     */
    private void setLogPointPosition(LogPointVectorMgmt _mgmt) {
        
        //Determine which vector to add to & what slot. Check both send & recv vectors
        //for the logpoint name

        if (isSend.booleanValue()) {
            logPointPosition = _mgmt.getSendVectorPosition(logpoint);
        }  else { //not a send side log point
            logPointPosition = _mgmt.getRecvVectorPosition(logpoint);
            //If recv side msg, see if this is the appointed final log point
            if (logPointPosition >=0) {
                //isSend = Boolean.FALSE;
                isFinalLogPoint = _mgmt.isFinalLogPointPos(logPointPosition);                
            }
        }
    }        
    
    /* 
     * Called after the final log point has been changed. This reclassifies this
     * receive log point event to see if it is or is not the final log point
     */
    public void recheckIsFinalLogPoint(LogPointVectorMgmt _mgmt) {
        if (!isSend.booleanValue()) {
            if (logPointPosition >=0) {
                isFinalLogPoint = _mgmt.isFinalLogPointPos(logPointPosition);                
            }        
        }
    }
    
}
