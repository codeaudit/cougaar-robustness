/*
 * TrafficAuditor.java
 *
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
 * Created on December 17, 2002, 6:16 PM
 */

package org.cougaar.tools.robustness.audit.msgAudit;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.PrintStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;
import java.util.Iterator;
import java.util.Vector;

import org.apache.log4j.*;
import org.apache.log4j.net.SocketAppender;
import org.apache.log4j.spi.*;

import org.cougaar.tools.robustness.audit.msgAudit.socket.*;
import org.cougaar.tools.robustness.audit.msgAudit.gui.*;

/**
 *
 * @author  pazandak@objs.com
 */
public class TrafficAuditor {
    
    //Store for problems during analysis
    private XMLMessageProblems problems; 
    
    private ProblemMessageManager pmm = null;
    private EventQueueProcessor qProcessor = null;
    
    private static Logger log;
    public static Logger logger() { return log; }
    
    //Ptr to TrafficAuditor
    TrafficAuditor auditor;

    //Log4J logging
    static final String DEFAULT_LOG_LEVEL = "INFO";
    
    //Log files
    Vector files = null;
    
    //Config File name
    String configFile = null;
    
    //Command-line flag
    static boolean autoResolve = false;
    
    //File to dump errors to 
    File outFile = null;
    public void setOutFile(File _o) { outFile = _o; }
    //True if dumping to file, false o.w.
    public boolean toFile() { return outFile == null ? false : true; }
    
    //PrintStream
    public static PrintStream out = System.out;
    
    //Print flag
    static boolean printToStdIO = true; 
    static public void setPrintToStdIO(boolean _b) { printToStdIO = _b; }

    //Print flag
    static boolean wrapOutput = false; 
    
    private ConfigData configData;
    ConfigData getConfigData() { return configData; }
    
    private Agents agents;
    Agents getAgents() { return agents; }
        
    /** Creates a new instance of TrafficAuditor */
    public TrafficAuditor(Vector _files, String _configFile, File _out) {        

        files = _files;
        configFile = _configFile;
        outFile = _out;
        
        configData = new ConfigData();
        
        try {
            loadConfig(configData, configFile);
        }
        catch (Exception e)
        {
          System.err.println ("Error loading the config file: " +configFile+ ", quitting audit.");
          System.err.println ("Exception was: " +e);
          System.exit (-1);
        }

        agents = new Agents(configData);
        problems = new XMLMessageProblems();
    }

    /** Creates a new instance of TrafficAuditor */
    public TrafficAuditor(int x) {        
        problems = new XMLMessageProblems();
    }

    
    
    static final String line = "___________________________________________________________________\n";
    public static String usage = "Usage: java -jar trafficaudit.jar -logs <logfiles> -config <configFile> -logLevel <level> -port <socketPort> [-wrap] [-out <file>]" +
        "\n    -wrap                - wrap lines (when logging to file) to 80 chars" +
        "\n    -logs <logfiles>     - log files to analyze (ignored if socket is used)" +
        "\n    -config <configFile> - XML configuration file describing event path (required)" +
        "\n    -out <outFile>       - dump log output to file instead of console " +
        "\n                              (disabled if socket is used)" +
        "\n    -port <socketPort>   - Activate socket-based data gathering on this port " +
        "\n                              (-logs & -out options ignored)" +
        "\n    -logLevel <level>    - Logging level to use for debugging" +
        "\n    -autoResolve         - Auto resolve problem msgs if only one matching agent name is found.";

    /*****************************************************************************
     *
     *  MAIN() - called from console
     *
     *****************************************************************************/
    public static void main (String args[])
   {

       //  Command line: should be a set of log file names
        File outF = null;
       
        if (args == null || args.length == 0)
        {
          System.err.println (usage);
          System.exit (-1);
        }


        //  For each log file, create intermediate files of msg send and
        //  receive records.

        System.out.println ("Processing log files...");

        Vector files = new Vector();
        String configFile = null;
        String logLevel = DEFAULT_LOG_LEVEL;
        String port = null;
        
        for (int i=0; i<args.length; i++)
        {
            if (args[i].equalsIgnoreCase("-logs")) {
                while (!args[++i].startsWith("-") && i<args.length) {
                    files.add(args[i]);
                }
                --i;
            } 
            else if (args[i].equalsIgnoreCase("-config") && i<(args.length-1)) {
                configFile = args[++i];
            }             
            else if (args[i].equalsIgnoreCase("-logLevel") && i<(args.length-1)) {
                logLevel = args[++i];
            }             
            else if (args[i].equalsIgnoreCase("-port") && i<(args.length-1)) {
                port = args[++i];
            }             
            else if (args[i].equalsIgnoreCase("-wrap") && i<(args.length-1)) {
                wrapOutput = true;
            } 
            else if (args[i].equalsIgnoreCase("-autoResolve") && i<(args.length-1)) {
                autoResolve = true;
            } 
            else if (args[i].equalsIgnoreCase("-out") && i<(args.length-1)) {
                String out = args[++i];
                try {            
                    outF = new File(out);
                } catch (Exception e) {
                    System.err.println ("Error with output file ["+out+"]. Exception was \n"+e);
                }
            } 
            else
                System.out.println (usage);
        }

        //**************************
        //***** PROCESS INPUTS *****
        //**************************
        
        //Set logging level
        log = Logger.getLogger("LogPointAnalyzer");
        log.addAppender(new ConsoleAppender(new SimpleLayout()));
        Level p = Level.toLevel(logLevel);
        if (p == null) {
            System.out.println("** Logging Level does not exist: " + logLevel);
            p = Level.toLevel(DEFAULT_LOG_LEVEL);
        }
        log.setLevel(p);
        
        //Validate config file
        if (configFile == null) {
            System.out.println ("** No config file specified. Exiting.");
            System.exit(-1);
        } 
        
        //Set port, if specified
        if (port != null) { //SOCKETS!!
            try {
                int portNum = Integer.parseInt(port);
                if (portNum <= 0) {
                    System.out.println("** Invalid port number (must be greater than 0): " + portNum);
                    return;
                }
                //OK, create a TrafficAuditor
                TrafficAuditor ta = new TrafficAuditor(null, configFile, null);
                ta.processViaSocketsGUI(configFile, portNum); //
                
            } catch (NumberFormatException nfe) {
                System.out.println("** Could not convert port number to a valid integer: " + port);
                return;
            }
        } else { //log files
            //OK, create a TrafficAuditor
            TrafficAuditor ta = new TrafficAuditor(files, configFile, outF);
            ta.processViaConsole(ta.getConfigData(), ta.getAgents(), outF);
        }
    }

    LogPointVectorMgmt logPointMgmt = null;
    void processViaSocketsGUI(String _configFile, int _port) { //ConfigData _cd, Agents _agents, int port) {

        EventQueue eq = new EventQueue(2000);
        
        //Init Log4J appender and subsequent event processor 
        //This processor simply places the events on the queue we pass to it.
        EventProcessor taep = new TrafficAnalysisEventProcessor();
        taep.setQueue(eq);
        
        //This appender takes the events from the socket appender
        EventProcessingAppender epa = new EventProcessingAppender(); 
        epa.addEventProcessor(taep);  

        //init gui 
        AgentSummaryGUI agentSummaryGUI = new AgentSummaryGUI(this);
       
        //Get auditor data
        ConfigData configData = getConfigData();
        logPointMgmt = new LogPointVectorMgmt(configData.GET_SEND_LEVELS(), configData.GET_RECV_LEVELS() );        
        AgentMgmt agentMgmt = new AgentMgmt(logPointMgmt, agentSummaryGUI);
        
        //Display GUI
        agentSummaryGUI.show();

        //Init ProblemMessageManager
        pmm = new ProblemMessageManager(this, agentMgmt, agentSummaryGUI);
        if (autoResolve) {
            pmm.setAutoResolve(true);
        }
        
        //init queue processing
        qProcessor = new EventQueueProcessor(eq, logPointMgmt, agentMgmt, pmm);
        new Thread(qProcessor).start();        
        
        //init socket manager
        SocketManager sm = new SocketManager(_port, epa);
        sm.run(); //start socket!
        
    }
    
    /** 
     * Return the LogPointVectorMgmt
     */
    public LogPointVectorMgmt getLogPointMgmt() { return logPointMgmt; }

    /** 
     * Return the ProblemMessageManager
     */
    public ProblemMessageManager getProblemMsgMgr() { return pmm; }
     

    /** 
     * Return the EventQueueProcessor
     */
    public EventQueueProcessor getQueueProcessor() { return qProcessor; }

    /**
     * Code to process logs via log files & not via streaming sockets
     */
    void processViaConsole(ConfigData _cd, Agents _agents, File _out) {

        //Step 1. Identify & extract all LogPoint msgs
        System.out.println(">>>>>>>>>>>>>>>> 1. Extracting LogPoint Messages From Logs <<<<<<<<<<<<<<<<<");

        String logFile = null;
        try {
            if (files.size()==0) {
                System.out.println ("No files loaded. Exiting.");
                System.exit(-1);
            }

            Iterator fileIter = files.iterator();
            while (fileIter.hasNext()) {
                logFile = (String) fileIter.next();
                System.out.print ("Processing " +logFile);
                int count = importLogFile (_agents, logFile);
                System.out.println ("[" +count + " msgs]");
            }
        }
        catch (Exception e)
        {
          System.err.println ("Error processing " +logFile+ ", quitting audit.");
          System.exit (-1);
        }

        processLogData(_cd, _agents);
        lookForProblems(_cd, _agents, System.out, _out);

  }
  
    /*****************************************************************************
    *
    *  loadConfig - loads the configuration file & processes it
    *
    *****************************************************************************/
    private void loadConfig(ConfigData _cd, String _file) throws Exception {
        File cf = new File(_file);
        if (!cf.exists()) throw new Exception("File ["+cf.getAbsolutePath()+"] does not exist.");
        _cd.loadConfig(cf);
    }

  
  private int importLogFile (Agents _agents, String logfile) throws Exception
  {
    BufferedReader log = null;
    int count = 0;
    
    try
    {
      //  Open the logfile

      log = new BufferedReader (new FileReader (logfile));
      int logLineNum = 0;
      int i = logfile.indexOf ('.');

      XMLMessage xmlmsg = null;

      while (true) 
      {
        String line = log.readLine();
        if (line == null) break;
        logLineNum++;

        //Assume XML msgs only at this point
        if (line.startsWith("<LP ")) { // || line.startsWith("<XP ") ) {
            try {
                xmlmsg = XMLMessage.parseXMLMsg(line);  //extract data
                if (xmlmsg != null && xmlmsg.isValid()) {
                    if (ConfigData.DEBUG) {
                        System.out.println("Processing line...");
                        System.out.println("XMLMsg = "+xmlmsg);
                    }
                    
                    //Check for sender/receiver error in node/incarn#.
                    if (xmlmsg.senderAddrError())
                        problems.add(new XMLMessageProblem(xmlmsg, "Sender's node or incarn# is in error"));
                    if (xmlmsg.receiverAddrError())
                        problems.add(new XMLMessageProblem(xmlmsg, "Receiver's node or incarn# is in error"));
                    
                    _agents.storeXMLMsg(xmlmsg); //add data to agent structs
                    count++;
                }
            } catch (XMLMessageException me) {
                System.out.println(me);
            }
         } //end if       
      } //end while(true)
      
    }
    catch (Exception e)
    {
        System.out.println("*** Exception accessing log files ***");
        e.printStackTrace();
        throw e;
    }
    finally     
    {
      try { log.close(); } catch (Exception ce) {}
    }
    return count;
  }
  
  
    //Step 2. Now that all msgs have been extracted we need to 
    //        go thru all received msgs & mark the corresponding 
    //        sent msgs as received.
    void processLogData(ConfigData _cd, Agents _agents) {
      
      
      System.out.println(">>>>>>>>>>>>>>>> 2. Pairing Up Sent & Received Messages <<<<<<<<<<<<<<<<<");

      Iterator iter = _agents.agents(); //get all agents
      Agent agent = null;
      Iterator recvIter;
      Agent sender;
      XMLMessage xmlmsg;
      
      //*** Right now this is only set up to work on a single level***
      //*** May want to add in checking that msgs it each level in stack 
      //*** get paired up.
      while (iter.hasNext()) {
         agent = (Agent)iter.next();
         
         recvIter = agent.recvArray[0].iterator();
         while (recvIter.hasNext()) {
            xmlmsg = (XMLMessage) recvIter.next();
            
            //Find sender agent
            sender = _agents.findAgent(xmlmsg.getFrom());
            if (sender == null) {
                problems.add(new XMLMessageProblem(xmlmsg, "Cannot find sender agent for this msg"));
                //System.out.print("f");
            }
            //Now find sent msg & mark as received
            if (!sender.markMsgReceived(xmlmsg)) {
                problems.add(new XMLMessageProblem(xmlmsg, "Cannot find SEND msg (in sender agent store) for this recv msg"));
                //System.out.print("m");
            }
         }
         //System.out.println();

      }

      
  }

  
   //Step 3. Now go thru & make sure that every sent msg was received
   void lookForProblems(ConfigData _cd, Agents _agents, PrintStream _out, File _outFile) {
      
      Agent sender;
      XMLMessage xmlmsg;

      
      _out.println(">>>>>>>>>>>>>>>> 3. Locating All Sent Msgs That Weren't Received <<<<<<<<<<<<<<<<<");

      Vector ags = (Vector)_agents.agents.clone();
      Iterator iter = ags.iterator(); //get all agents
      Iterator sendMsgIter;
      
      //*** Rigth now this is only set up to work on a single level***
      //*** May want to add in checking that msgs it each level in stack 
      //*** get paired up.
      while (iter.hasNext()) {
         sender = (Agent)iter.next();
         
         sendMsgIter = sender.sendArray[0].iterator();
         while (sendMsgIter.hasNext()) {
             xmlmsg = (XMLMessage) sendMsgIter.next();
             if (xmlmsg.receiveCount == 0) {
                MessageStackResult msr = _agents.getMsgStack(xmlmsg);
                problems.add(new XMLMessageProblem(xmlmsg, "Message Not Received", msr));
             }
             else if (xmlmsg.receiveCount > 1) {
                MessageStackResult msr = _agents.getMsgStack(xmlmsg);
                problems.add(new XMLMessageProblem(xmlmsg, "Message Received "+xmlmsg.receiveCount+" times.", msr));
             }
         }                 
      }
      
      Iterator errIter = problems.iterator();
      if (!errIter.hasNext()) {
          _out.println(">>>>>>>>>>>>>>>>>>>> *** NO ERRORS FOUND *** <<<<<<<<<<<<<<<<<<<<<");
          return;
      } 

      _out.println("\n");
      _out.println(">>>>>>>>>>>>>>>>>>>>----------------------------<<<<<<<<<<<<<<<<<<<<<");
      _out.println(">>>>>>>>>>>>>>>>>>>> ****** ERRORS FOUND ****** <<<<<<<<<<<<<<<<<<<<<");
      _out.println(">>>>>>>>>>>>>>>>>>>>----------------------------<<<<<<<<<<<<<<<<<<<<<");
      _out.println("\n");
      
      if (_outFile != null) {
          _out.println(">>>>>>>>>>>>>>>>>>>> Dumping to "+_outFile.getAbsolutePath());
          try {
              PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(_outFile)));
              while (errIter.hasNext()) {
                 pw.println(errIter.next());
              }
              pw.close();
          } catch (java.io.IOException ioe) {
              _out.println("Error writing to output file: "+outFile.getAbsolutePath());
          }
      } else {
          while (errIter.hasNext()) {
              XMLMessageProblem mp = (XMLMessageProblem)errIter.next();
              _out.println(mp.toString(false,true,5,70,wrapOutput));
          }
      }      
      
  }      
  
}
