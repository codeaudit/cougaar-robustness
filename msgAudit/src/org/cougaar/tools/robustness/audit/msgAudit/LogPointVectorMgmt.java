/*
 * LogPointVectorMgmt.java
 *
 * Created on February 17, 2003, 8:02 PM
 */

package LogPointAnalyzer;
import LogPointAnalyzer.gui.*;
import LogPointAnalyzer.event.*;

import java.util.Vector;
/**
 *
 * @author  Administrator
 */
public class LogPointVectorMgmt {

    public static final Integer NEW = new Integer(255);
    public static final Integer NOT_IN_USE = new Integer(-1);
    public static final Integer NO_MSG = new Integer(-2);
    
    private LogPointsControl gui;
    
    //Contains the LogPointLevel objects
    private Vector sendLogPoints;
    private Vector recvLogPoints;

    //Contains the LogPointLevel NAMES (for faster searching)
    private Vector sendLogPointNames;
    private Vector recvLogPointNames;
    
    //Contains the DISPLAY order of the log points
    private Vector sendLogPointDisplayOrder;
    private Vector recvLogPointDisplayOrder;

    /* The final log point, at which time a msg is considered 
     * to have arrived successfully. The largest receiving seq 
     * number in the recvLogPoints is the default FinalLogPoint.
     * This can be changed via LogPointsControl.
     */
    private int finalLogPoint = 0;
    public boolean isFinalLogPointPos(int _pos)  { return (_pos == finalLogPoint); }
    
    private AgentMgmt agentMgmt = null;
    public void registerAgentMgmt(AgentMgmt _am) { agentMgmt = _am; }
    
    /** Creates a new instance of LogPointVectorMgmt */
    public LogPointVectorMgmt() {

        sendLogPoints = new Vector();
        recvLogPoints = new Vector();

        sendLogPointNames = new Vector();
        recvLogPointNames = new Vector();

        sendLogPointDisplayOrder = new Vector();
        recvLogPointDisplayOrder = new Vector();

        
    }

    public LogPointVectorMgmt(LogPointLevel[] _sendLogPoints, LogPointLevel[] _recvLogPoints) {

        gui = new LogPointsControl(this);
        
        int sendLen = _sendLogPoints.length;
        int recvLen = _recvLogPoints.length;

        sendLogPoints = new Vector(sendLen);
        sendLogPointNames = new Vector(sendLen);
        sendLogPointDisplayOrder = new Vector(sendLen);
        for (int i=0; i< sendLen; i++) {

            this.addLogPoint(_sendLogPoints[i]);
            _sendLogPoints[i].setFromConfig(true);

            //sendLogPoints.addElement(_sendLogPoints[i]);
            //sendLogPointNames.addElement(_sendLogPoints[i].logPointName());
            //int seq = _sendLogPoints[i].seq();
            //sendLogPointDisplayOrder.addElement(new Integer(seq));
        }

        int finalPos = 0;
        int biggest = 0;
        recvLogPoints = new Vector(recvLen);
        recvLogPointNames = new Vector(recvLen);
        recvLogPointDisplayOrder = new Vector(recvLen);
        for (int i=0; i< recvLen; i++) {

            this.addLogPoint(_recvLogPoints[i]);
            _recvLogPoints[i].setFromConfig(true);
            
//            recvLogPoints.addElement(_recvLogPoints[i]);
//            recvLogPointNames.addElement(_recvLogPoints[i].logPointName());

            int seq = _recvLogPoints[i].seq();
            //Find the largest receiving seq number. This is the default FinalLogPoint
            if (biggest < seq) {
                biggest = seq;
                finalPos = i;
            }
            //recvLogPointDisplayOrder.addElement(new Integer(seq));
        }
        //Final Log point
        finalLogPoint = finalPos;
        
        gui.show();
    }
    
    public void showConsole() { gui.show(); }

    public void finalLogPointChanged(LogPointLevel _lpl, int _pos) {
        
        finalLogPoint = _pos;
        
        //Notify AgentMgmt so it can reprocess all data
        agentMgmt.recheckAgentData();
        
        //Alert all guis
        EventHandler.handler().newFinalPointEvent(_lpl);
    
    }
    
    /* Returns the PHYSICAL position in the send vector that this log point occupies
     * This is different than the logical display position. 
     * @return -1 if not found.
     */
    public int getSendVectorPosition(String _logpoint) {
        
        return sendLogPointNames.indexOf(_logpoint);
        
    }

    /* Returns the PHYSICAL position in the recv vector that this log point occupies
     * This is different than the logical display position. 
     * @return -1 if not found.
     */
    public int getRecvVectorPosition(String _logpoint) {
        
        return recvLogPointNames.indexOf(_logpoint);
    }
    
    /*
     * Add a new Log Point to the END of the list of log points.
     * Adds either to the send or receive list.
     */
    public void addLogPoint(LogPointLevel _lp) {
        
        Integer posInt;
        int pos = _lp.seq();
        if (pos == -1) { //unassigned 
            posInt = NEW; //filler until an order is defined
        } else {
            posInt = new Integer(pos);
        }
        
        synchronized(this) {
            if (_lp.isSend()) {
                sendLogPoints.addElement(_lp);
                sendLogPointNames.addElement(_lp.logPointName());
                sendLogPointDisplayOrder.addElement(posInt); 
            } else {
                recvLogPoints.addElement(_lp);
                recvLogPointNames.addElement(_lp.logPointName());
                recvLogPointDisplayOrder.addElement(posInt); 
            }
        }
        gui.addLogPoint(_lp, _lp.isSend());

    }

    /*
     * @return Returns the number of active log points (those assigned an
     * order in which to be displayed).
     */
    public int getActiveLogPointCount(boolean _isSend) {
     
        Vector v;
        if (_isSend) {
            v = sendLogPointDisplayOrder;
        } else {
            v = recvLogPointDisplayOrder;
        }           
        int count=0;
        for (int i=0; i<v.size(); i++) {
            Integer pos = (Integer) v.elementAt(i);
            if ( pos != NEW || pos != NOT_IN_USE) {
                count++;
            }
        }
        return count;
    }
    
    public Vector convertToLogicalOrder(Vector _v, boolean _isSend) {
     
        int sz = getActiveLogPointCount(_isSend);
        Vector result = new Vector(sz);
        //Init vector
        for (int j=0; j<sz; j++) {
            result.addElement(NOT_IN_USE);
        }
        
        Vector displayOrder;
        if (_isSend) {
            displayOrder = sendLogPointDisplayOrder;
        } else {
            displayOrder = recvLogPointDisplayOrder;
        }      
        
        Integer pos;
        for (int i=0; i<_v.size(); i++) {

            
            pos = (Integer)displayOrder.elementAt(i);
            if (pos != NEW && pos != NOT_IN_USE) { //then this is active   
                int posVal = pos.intValue();
                if (posVal >= sz) {
                    System.out.println("**LogPtVectorMgmt error:: position value > vector size! posVal="+posVal+", sz="+sz);
                } else { //go ahead and move into position
                    result.setElementAt(_v.elementAt(i), posVal);                
                }
            }
        }
        
        return result;
        
    }
    

    /* Sets the LOGICAL DISPLAY position in the send/recv vector 
     * depending upon the type of log point
     */
    public void setDisplayPosition(LogPointLevel _lpl, int _vectorPos, int val) {
        
        if (_lpl.isSend())        
            sendLogPointDisplayOrder.setElementAt(new Integer(val), _vectorPos);
        else
            recvLogPointDisplayOrder.setElementAt(new Integer(val), _vectorPos);            
    }
    
    
    /* Returns the LOGICAL DISPLAY position in the send/recv vector 
     * depending upon the type of log point
     * @return logical position, or NEW | NOT_IN_USE
     */
    public Integer getDisplayPosition(LogPointLevel _lpl, int _pos) {
        
        if (_lpl.isSend())        
            return (Integer)sendLogPointDisplayOrder.elementAt(_pos);
        else
            return (Integer)recvLogPointDisplayOrder.elementAt(_pos);
    }

    /* Returns the LOGICAL DISPLAY position in the recv vector 
     * @return logical position, or NEW | NOT_IN_USE
     */
    public Integer getRecvDisplayPosition(int _vectorPos) {
        
        return (Integer)recvLogPointDisplayOrder.elementAt(_vectorPos);
       
    }
    
    /*
     * Returns the current # of send log points
     */
    public int getNumSendPoints() { return sendLogPoints.size(); }

    /*
     * Returns the current # of send log points
     */
    public int getNumRecvPoints() { return recvLogPoints.size(); }


}
