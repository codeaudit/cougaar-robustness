/*
 * EventQueueProcessor.java
 *
 * Created on February 18, 2003, 4:39 PM
 */

package LogPointAnalyzer;

import org.cougaar.core.mts.logging.LogEvent;

/**
 *
 * @author  Administrator
 */
public class EventQueueProcessor implements Runnable {
    
    EventQueue q;
    LogPointVectorMgmt logPointMgmt;
    AgentMgmt agentMgmt;
    
    /** Creates a new instance of EventQueueProcessor */
    public EventQueueProcessor(EventQueue _q, LogPointVectorMgmt _logPointMgmt, AgentMgmt _agentMgmt) {
        q = _q;
        logPointMgmt = _logPointMgmt;
        agentMgmt = _agentMgmt;
    }
    
    public void run() {
        
        AgentData fromAgent = null;
        AgentData toAgent   = null;
     
        try {
            while (true) {
            
                LogEvent[] evts = q.removeAtLeastOne();
                for (int i=0; i<evts.length; i++) {
                    LogPointEntry lpe = new LogPointEntry(evts[i], logPointMgmt);
                    //See if this evt was emitted on the send or receive side.
                    //and find the agents
                    if (lpe.isSend().booleanValue()) { 
                        //we should know the whole name of the sending agent
                        //but we may not know the whole name of the receiving agent
                        fromAgent = agentMgmt.createAgent(lpe.from()); //create agent if nec.
                        toAgent   = agentMgmt.lookupAgent(lpe.dest()); //don't create
                    } else {
                        //Alternatively, now we know the dest agent's full name
                        //*** At the moment it is left as above. Will change when ported to 10.0
                        fromAgent = agentMgmt.createAgent(lpe.from()); //create agent if nec.
                        toAgent   = agentMgmt.lookupAgent(lpe.dest()); //don't create
                    }

                    //Finally, hand off the log point event to the sending agent
                    fromAgent.addLogPointEntry(lpe, toAgent);

                }
            }    
        } catch (InterruptedException ie) {
            System.out.println("EventQueueProcessor caught Interrupted exception. Exiting.");
            return;
        } 
    }
    
}
