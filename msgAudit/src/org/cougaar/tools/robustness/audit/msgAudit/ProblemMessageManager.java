/*
 * ProblemMessageManager.java
 *
 * Created on April 22, 2003, 12:25 PM
 */

package org.cougaar.tools.robustness.audit.msgAudit;

import org.cougaar.tools.robustness.audit.msgAudit.gui.*;

import java.util.Vector;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;


/**
 * The manager is passed messages (LogPointEvents) whose dest/from agent
 * name is incomplete. It files these for later resolution.
 */

public class ProblemMessageManager {
    
    Vector problems;
    AgentMgmt agentMgmt;
    AgentSummaryGUI agentSummaryGUI;
    ProblemMessagesDisplay problemsGUI;
    TrafficAuditor auditor;    
    
    private boolean autoResolve = false;

    private Hashtable resolveMappings;
    
    /** Creates a new instance of ProblemMessageManager */
    public ProblemMessageManager(TrafficAuditor _ta, AgentMgmt _am, AgentSummaryGUI _mainGUI) {
        problems = new Vector(100,100);
        
        auditor = _ta;
        agentMgmt = _am;
        agentSummaryGUI = _mainGUI;
        
        problemsGUI = new ProblemMessagesDisplay(this);
        resolveMappings = new Hashtable();

        problemQueue = new ProblemMessageQueue(3000);
        
    }
    
    public void setAutoResolve(boolean _b) { 
        autoResolve = _b; 
        if (autoResolve) { //then autoresolve current list of problems, if any
            resolveCurrentList();
        }
    }

    
    /**
     * Pass in the troubled message. 
     */
    public void addProblemMessage( LogPointEntry _lpe) {    
        problemQueue.add(_lpe);
    }
    
    
    /**
     * Pass in the troubled message. 
     */
    public void handleProblemMessage( LogPointEntry _lpe) {

        boolean handled = false;
        
        String probString = _lpe.isFromError() ? _lpe.from() : _lpe.dest();
        
        //Check and see if we can resolve this using user-defined mappings
        String map = (String)resolveMappings.get(probString);
        if (map != null) {
            //Found an auto map!
           if (_isFrom) {
               _lpe.setFrom(map);
           } else {
               _lpe.setDest(map);
           }
           applyChange(_lpe);
           return;
        }
        
        //AUTO RESOLVE
        if (autoResolve) { //see if there is one matching agent now
            String match = findMatch(probString);
            if (match != null) {
               if (_isFrom) {
                   _lpe.setFrom(match);
               } else {
                   _lpe.setDest(match);
               }
               applyChange(_lpe);
               problemsGUI.incAutoResolvedCount();
               return;
            }             
        }        
        
        for (int j=0; j<problems.size(); j++) {
            ProblemMessage pm = (ProblemMessage)problems.get(j);
            if (pm.getName().equals(probString) && _lpe.seqNumber() == pm.getSeqNumber()) {
                pm.addMsg(_lpe);
                handled = true;
                break;
            }
        }
        if (!handled) {
            problems.add(new ProblemMessage(probString, _lpe, _isFrom));
        }
        agentSummaryGUI.newProblemAlert();
    }
    
    /** 
     * Display the Problems GUI
     */
    public void showGUI() {
        
        refresh();
        problemsGUI.show();
    }

    
    /**
     * Search thru all agents for a match. If one found, return it. 
     * If more than one found, or none found, return null.
     */
    String findMatch(String _ss) {
       
        String match = null;
        int count = 0;
        Vector v = agentMgmt.agentsList();
        for (int i=0; i<v.size(); i++) {
            AgentData ad = (AgentData)v.get(i);
            if (ad.name().indexOf(_ss) > 0) {
                if (count > 1) { return null; }
                match = ad.name();
                count = 1;
            }
        }
        return match;
    }
    
    /**
     * Called to auto resolve current list of problem messages
     */
    private void resolveCurrentList() {
        
        for (int i=0; i<problems.size(); i++) {
            ProblemMessage pm = (ProblemMessage)problems.get(i);
            boolean isFrom = pm.isFrom();
            Vector list = pm.getLPEList();
            for (int j=0; j<list.size(); j++) {             
                LogPointEntry lpe = (LogPointEntry)list.get(j);
                this.handleProblemMessage(lpe, isFrom);
                problems.remove(lpe);
                problemsGUI.incAutoResolvedCount();
            }
            problems.remove(i);// remove after processing
        }                    
    }
    
    
    /**
     * Called to refresh the data
     **/
    public void refresh() {
        problemsGUI.setAgentList(agentMgmt.agentsList());
        problemsGUI.setProblemList(problems);
    }        
    
    /**
     * Commit modifications & reprocess changed messages
     */
    public void applyChanges(Vector _v) {
     
        EventQueueProcessor eqp = auditor.getQueueProcessor();
        if (eqp == null)
            System.out.println("******************** eqp is NULL!");
        
        Iterator mods = _v.iterator();
        while (mods.hasNext()) {
            ProblemMessage pm = (ProblemMessage)mods.next();
            if (pm.isModified()) {
                Iterator iter = pm.getLPEs();
                while (iter.hasNext()) {             
                    //System.out.println("******************** reprocessing lpe");
                    LogPointEntry lpe = (LogPointEntry)iter.next();
                    eqp.reprocessMessage(lpe);                    
                    problems.remove(lpe);
                }
                mods.remove(); // remove after processing
            }
        }                    
    }

    
    /**
     * Commit modifications & reprocess changed messages
     */
    public void applyChange(LogPointEntry _lpe) {     
        EventQueueProcessor eqp = auditor.getQueueProcessor();        
        eqp.reprocessMessage(_lpe);
        problems.remove(_lpe);
    }
    
    /** 
     * Add to list of auto resolve mappings
     */
    public void addToMappings(String _msgName, String _agentName) {
        resolveMappings.put(_msgName, _agentName);
    }
    
    
}

