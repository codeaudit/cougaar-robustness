/*
 * Message.java
 *
 * Created on February 17, 2003, 7:26 PM
 */

package org.cougaar.tools.robustness.audit.msgAudit;
import org.cougaar.tools.robustness.audit.msgAudit.gui.*;

import org.cougaar.core.mts.logging.LogEvent;
import java.util.Properties;
import java.util.Vector;

/**
 *
 * @author  Administrator
 */
public class Message {
    
    private MessageDetail mDetailGUI = null;
    
    private AgentData agent = null;
    private boolean arrived = false;
    
    private boolean guiActive = false;
    
    //Identifying attributes - destination agent & seq #
    private AgentData toAgent = null;
    private int seqNum = -1;
    private String unregisteredDestAgentName = null;
    
    private Vector sendVector;
    private Vector recvVector;
    private LogPointVectorMgmt mgmt;

    //Holds msgs whose LogPoint names are not registered
    private Vector unfiledVector;
    
    /** Creates a new instance of Message */
    public Message(AgentData _agent) {
        
        agent = _agent;
        
        //Init Send / Recv Vectors
        mgmt = agent.getMgmt();
        sendVector = new Vector();
        sendVector.setSize(mgmt.getNumSendPoints());
        
        recvVector = new Vector(mgmt.getNumRecvPoints());
        recvVector.setSize(mgmt.getNumRecvPoints());

        unfiledVector = new Vector(10);
        
        //mDetailGUI = new MessageDetail(this);
    }
    
    /*
     * @return The Destination agent's name. Will return the name() if the destination 
     * agent object exists, o.w. returns the string value from the LogPointEvent.
     */
    public String dest() { 
        if (toAgent != null) {
//System.out.println("Message.dest(): toAgent NOT null");            
            return toAgent.name(); 
        } else {
//System.out.println("Message.dest(): toAgent null, returning unregisteredDestAgentName = "+unregisteredDestAgentName);            
            return unregisteredDestAgentName;
        }
    }
    public AgentData destAgent() { return toAgent; }
    public boolean arrived() { return arrived; }
    public Vector sendVector() { return sendVector; }
    public Vector recvVector() { return recvVector; }
    public Vector unFiledVector() { return unfiledVector; }
    public int getSeqNum() { return seqNum; }
    
    public AgentData getAgent() { return agent; }

    public void showDetails() { 
        
        MessageDetail mDetailGUI = (MessageDetail)WindowManager.getMgr().needWindow(MessageDetail.class);
        //mDetailGUI.setData(sendVector, recvVector);
        if (mDetailGUI == null) { //create a new one & register with Win Mgr
            mDetailGUI = new MessageDetail(this);
            WindowManager.getMgr().registerWindow(mDetailGUI);
        }
        mDetailGUI.setMessage(this);
        mDetailGUI.show();
    }

    /*
     * Sets the state of the associated GUI. If TRUE (active) then we'll
     * send new events to the window.
     */
    public void setGUIActive(boolean _b) {
        guiActive = _b;
        //As a precaution...
        if (mDetailGUI == null || !mDetailGUI.isVisible() || mDetailGUI.getMessage() != this)
            guiActive = false;
        
    }

    public synchronized void addLogPointEntry(LogPointEntry _lpe, AgentData _toAgent) {
        
        _lpe.setMsgRoot(this);
        
        //See if this Message object has any message data yet. If not, init it.
        if (this.toAgent == null) {
            //Get the Destination Agent object IF it exists. O.w. just keep the
            //name and hope that at some pt the dest agent will be created (when IT
            //sends a msg)
            this.toAgent = _toAgent;
            if (this.toAgent == null) { //toAgent still not registered - we'd like to have some name
                unregisteredDestAgentName = _lpe.dest(); 
            }
            seqNum = _lpe.seqNumber();
            
            //Update gui 
            if (guiActive) {            
                mDetailGUI.setToAgent(dest());
                mDetailGUI.setSeqNum(seqNum);
            }
        }
        
        if (_lpe.isFinalLogPoint()) { 
            arrived = true;
        }
        
        //Store MsgEntry in correct vector
        int pos = _lpe.getLogPointLevel();
        //If the position has been identified & is a send or recv msg, add to vector
        if (pos >= 0 && _lpe.isSend() != null) {
            Vector v;
            if (_lpe.isSend().booleanValue()) {
                v = sendVector;
            } else  {
                v = recvVector;
            }
            //Ensure vector is large enough to hold new msg entry
            if (pos >= v.size()) {
                v.setSize(pos+1);
            } 
            //Store msg
            v.setElementAt(_lpe, pos);
            
            //Update gui
            if (guiActive)
                mDetailGUI.addMessage(_lpe);
            
        } else { //LogPoint not found, cannot store in send/recv vector
            System.out.println("**LogPoint ["+_lpe.logPointName()+"]not found. Adding to unknown vector.");
            unfiledVector.add(_lpe);
        }
    }
    
    /* 
     * Called to recheck the status of each Message, e.g. when the
     * final log point has been changed.
     */
    public void recheckMessageArrivedStatus() {

        //reset status
        this.arrived = false;

        int sz = recvVector.size();
        for (int i=0; i< sz; i++) {
         
            LogPointEntry lpe = (LogPointEntry)recvVector.elementAt(i); 
            if (lpe != null) {
                lpe.recheckIsFinalLogPoint(mgmt);
                if (lpe.isFinalLogPoint()) { 
                    this.arrived = true;
                }
            }
        }        
    }
    
    
}

