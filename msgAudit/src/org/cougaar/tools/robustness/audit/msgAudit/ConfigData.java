/*
 * ConfigData.java
 * <copyright>
 *  Copyright 2002 Object Services and Consulting, Inc. (OBJS),
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 *
 * Created on December 17, 2002, 5:49 PM
 */

package LogPointAnalyzer;

import java.io.File;
import java.util.Vector;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.Comparator;
/**
 *
 * @author  pazandak@objs.com
 */
public class ConfigData {
    
    LogPointLevel[] sendLevels;
    LogPointLevel[] recvLevels;
    Vector levelData;
    LPLComparator comp;
    
    static public boolean DEBUG = false;
    static public void setDebug(boolean _b) { DEBUG = _b; }
    
    
    /** Creates a new instance of ConfigData */
    public ConfigData() {
        recvLevels = new LogPointLevel[0];
        sendLevels = new LogPointLevel[0];
        
        levelData = new Vector(20);
        comp = new LPLComparator();
    }
    
    static public void main(String[] args) {
        try {
            ConfigData cd = new ConfigData();
            ConfigDataParser cdp = new ConfigDataParser();
            //getCD().setDebug(true);
            cdp.parse(new File(args[0]), cd);
            if (cd.levelData.size() == 0)
                throw new Exception("**Cannot continue. No levels found in config file: \n   ");
                
            if (DEBUG) 
                System.out.println("Executing processLevels()");
            cd.processLevels();
        } catch (Exception e) {
            System.out.println("Saw exception: "+ e);
            e.printStackTrace();
        }
    }
    
    
    public void loadConfig(File _cf) throws Exception {
        
        try {
            ConfigDataParser cdp = new ConfigDataParser();
            cdp.parse(_cf, this);            
            if (levelData.size() == 0)
                throw new Exception("**Cannot continue. No levels found in config file: \n   ");
                
            if (DEBUG) 
                System.out.println("Executing processLevels()");
            processLevels();

        } catch ( Exception e ) {
            throw new Exception("**Exception parsing config file: \n   "+e);
        }
    }
    
    public int GET_NUM_SEND_LEVELS() {     
        return sendLevels.length;
    }

    public int GET_NUM_RECV_LEVELS() {     
        return recvLevels.length;
    }
    
    public LogPointLevel[] GET_SEND_LEVELS() { 
        return sendLevels;
    }

    public LogPointLevel[] GET_RECV_LEVELS() { 
        return recvLevels;
    }

    //Return level at which the supplied level name is defined.
    //Returns positive (1 to j) level if a SEND side msg, and 
    //returns a negative level if on the RECV side.
    //If not found, returns 0;
    public int GET_LEVEL(String _lev) { 
        for (int i=0; i< sendLevels.length; i++) 
            if (sendLevels[i].ID.equalsIgnoreCase(_lev))
                return i+1;
        
        for (int i=0; i< recvLevels.length; i++) 
            if (recvLevels[i].ID.equalsIgnoreCase(_lev))
                return -(i+1);

        return 0; // not found
    }
    
    public void addLevel(LogPointLevel _lpl) { levelData.add(_lpl); }


    private void processLevels() {
        
        //Ordered set -- we want to make sure that 
        //the levels are sorted in order.
        TreeSet send = new TreeSet(comp);
        TreeSet recv = new TreeSet(comp);
        LogPointLevel lpl;
        
        Iterator iter = levelData.iterator();
        while (iter.hasNext()) {
            lpl = (LogPointLevel) iter.next();
            if (lpl.SEND) 
                send.add(lpl);
            else
                recv.add(lpl);
        }

        
        //Now copy into send & recv arrays
        Iterator si = send.iterator();
        sendLevels = new LogPointLevel[send.size()];
        int c=0;
        while (si.hasNext())
            sendLevels[c++] = (LogPointLevel) si.next();
        
        si = recv.iterator();
        recvLevels = new LogPointLevel[recv.size()];
        c=0;
        while (si.hasNext())
            recvLevels[c++] = (LogPointLevel) si.next();

        
        if (DEBUG) {
            for (int i=0; i< sendLevels.length; i++) 
                System.out.println(sendLevels[i]);

            for (int i=0; i< recvLevels.length; i++) 
                System.out.println(recvLevels[i]);
        }
    }
        
    class LPLComparator implements Comparator {
        
        public int compare(Object o1, Object o2) {
            int s1 = ((LogPointLevel)o1).SEQ;
            int s2 = ((LogPointLevel)o2).SEQ;
            if (s1 < s2) return -1;
            if (s1 == s2) return 0;
            else 
                return 1;
        }
        
        public boolean equals(Object obj) {
            if (obj instanceof LPLComparator) return true;
            else
                return false;            
        }
        
    }
}

