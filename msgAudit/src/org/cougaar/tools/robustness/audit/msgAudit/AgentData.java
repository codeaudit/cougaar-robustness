/*
 * AgentData.java
 *
 * Created on February 17, 2003, 2:43 PM
 */

package org.cougaar.tools.robustness.audit.msgAudit;

import org.cougaar.tools.robustness.audit.msgAudit.gui.*;

import java.util.Vector;
import javax.swing.JLabel;
/**
 *
 * @author  Administrator
 */
public class AgentData {
    
    public static final String SEND_PREFIX = "Before";
    public static final String RECV_PREFIX = "After";
    
    private String name;
    private AgentMgmt agentMgmt;
    private LogPointVectorMgmt logPointMgmt;
    private Vector msgVector; //vector of sent messages
    
    private AgentMessageList msgListGUI = null;
    
    private JLabel jSent = new JLabel();
    private JLabel jRecd = new JLabel();
    private JLabel jLeft = new JLabel();
    
    private int seqWarning = 10000;
    
    private int sent = 0;
    private int recd = 0;
    
    /** Creates a new instance of AgentData */
    public AgentData() {
    }
    
    /** Creates a new instance of AgentData */
    public AgentData(AgentMgmt _agentMgmt, LogPointVectorMgmt _logpointmgmt, String _name) {
        name = _name;
        logPointMgmt = _logpointmgmt;
        agentMgmt = _agentMgmt;
        msgVector = new Vector(100);
        
        msgListGUI = new AgentMessageList(this);

    }

    public String sentLabel() { return ""+sent; }
    public String recdLabel() { return ""+recd; }
    public String leftLabel() { return ""+(sent-recd); }
    
    public String name() { return name; }
    public String toString() { return name; }
    
    public AgentMgmt agentMgmt() { return agentMgmt; }
    public LogPointVectorMgmt getMgmt() { return logPointMgmt; }
    
    public Vector getMsgs() { return msgVector; }
    
    /*
     * Add a LogPointEntry
     */
    public void addLogPointEntry(LogPointEntry _lpe, AgentData _toAgent) {
        
        synchronized(this) {
            //See if a corresponding message has been defined
            int seq = _lpe.seqNumber();
            if (seq > seqWarning) {
                System.out.println("SEQUENCE # is greater than "+seqWarning);
                seqWarning += 5000;
            }
            //Increase size of msgVector if needed
            if ( seq >= msgVector.size() ) {
                msgVector.setSize(seq+20); //increment by 20 instead of 1 at a time
            }

            Message msg = (Message) msgVector.elementAt(seq);
            if (msg == null) { //create a new one & store in msgVector
                msg = new Message(this);
                msgVector.setElementAt(msg, seq);
                sent++;
                msgListGUI.addMessage(msg);
            } 

            if (msg.arrived()) {
                msg.addLogPointEntry(_lpe, _toAgent); 
            } else { //only mark as arrived once
                msg.addLogPointEntry(_lpe, _toAgent); 
                if (msg.arrived()) {
                    recd++;
                }
            }        

            //System.out.println("AgentData:addLogPointEvent:: sent = "+sent+", recd = "+recd);
            //System.out.println("    Logpoint = "+_lpe.logPointName());
        }        
    }
    
    public void showMsgList() { 
        msgListGUI.updateData();
        msgListGUI.show(); 
    }
    
    /*
     * Called after the final point is changed to recheck the status
     * of each message.arrived()
     **/
    public void recheckMessagesArrivedStatus() {
    
        synchronized(this) {
            sent = 0;
            recd = 0;
            int sz = msgVector.size();
            for (int i=0; i<sz; i++) {
                Message m = (Message)msgVector.elementAt(i);
                if (m != null) {
                    sent++;
                    m.recheckMessageArrivedStatus();
                    if (m.arrived()) {
                        recd++;
                    }
                    //System.out.println("AgentData.recheckMessagesArrivedStatus: sent = "+sent+", recd = "+recd);
                }
            }
        }        
        
    }
    
}
