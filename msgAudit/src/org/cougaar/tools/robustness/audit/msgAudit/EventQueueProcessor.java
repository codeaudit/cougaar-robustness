/*
 * EventQueueProcessor.java
 *
 * Created on February 18, 2003, 4:39 PM
 */

package org.cougaar.tools.robustness.audit.msgAudit;

import org.cougaar.core.mts.logging.LogEvent;

/**
 *
 * @author  Administrator
 */
public class EventQueueProcessor implements Runnable {
    
    EventQueue q;
    LogPointVectorMgmt logPointMgmt;
    AgentMgmt agentMgmt;
    ProblemMessageManager pmm;
    
    /** Creates a new instance of EventQueueProcessor */
    public EventQueueProcessor(EventQueue _q, LogPointVectorMgmt _logPointMgmt, 
                                AgentMgmt _agentMgmt, ProblemMessageManager _pmm ) {
        q = _q;
        logPointMgmt = _logPointMgmt;
        agentMgmt = _agentMgmt;        
        
        pmm = _pmm;
    }
    
    public void run() {
        
      try {
            while (true) {
            
                LogEvent[] evts = q.removeAtLeastOne();
                for (int i=0; i<evts.length; i++) {
                    LogPointEntry lpe = new LogPointEntry(evts[i], logPointMgmt);
                    //See if this evt was emitted on the send or receive side.
                    //and find the agents
                    
                    //Filter out troubled messages
                    if ( hasProblem(lpe.from()) ) {
                        lpe.setFromError(true);
                        pmm.addProblemMessage(lpe);
                        continue;
                    } 

                    if ( hasProblem(lpe.dest()) ) {
                        pmm.addProblemMessage(lpe);
                        continue;
                    }
                    
                    processMessage(lpe);
                }
            }    
        } catch (InterruptedException ie) {
            System.out.println("EventQueueProcessor caught Interrupted exception. Exiting.");
            return;
        } 
    }
    
    private void processMessage(LogPointEntry _lpe) {

        AgentData fromAgent = null;
        AgentData toAgent   = null;

        if (_lpe.isSend().booleanValue()) { 
            //we should know the whole name of the sending agent
            //but we may not know the whole name of the receiving agent
            fromAgent = agentMgmt.createAgent(_lpe.from()); //create agent if nec.
            toAgent   = agentMgmt.lookupAgent(_lpe.dest()); //don't create
        } else {
            //Alternatively, now we know the dest agent's full name
            //*** At the moment it is left as above. Will change when ported to 10.0
            fromAgent = agentMgmt.createAgent(_lpe.from()); //create agent if nec.
            toAgent   = agentMgmt.lookupAgent(_lpe.dest()); //don't create
        }

        //Finally, hand off the log point event to the sending agent
        fromAgent.addLogPointEntry(_lpe, toAgent);
    }
    
    /**
     * Called to reprocess a message whose from or dest name has changed.
     */
    public void reprocessMessage(LogPointEntry _lpe) {
        processMessage(_lpe);
    }
    
    boolean hasProblem(String _agentName) {
        
        if ( (_agentName.indexOf('.') < 0) ||  _agentName.startsWith("NULL") || _agentName.startsWith("NameLookupException") ||
            _agentName.endsWith("NULL") )
            return true;
        else
            return false;
    }
}
