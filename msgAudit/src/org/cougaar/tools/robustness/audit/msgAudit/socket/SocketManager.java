/*
 * SocketManager.java
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
 * Created on February 10, 2003, 4:58 PM
 */

package LogPointAnalyzer.socket;
import LogPointAnalyzer.*;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;


import org.apache.log4j.*;
import org.apache.log4j.spi.*;
import org.apache.log4j.net.*;

/**
 * Manages the accept() connection from all SocketAppenders & hands each off
 * to a SocketHandler.
 *
 * @author  Administrator
 */
    
public class SocketManager extends Thread {

    static int PORT = 7887;
    ServerSocket server = null;        
    int port=0;
    EventProcessingAppender epa = null;
    
    public SocketManager(int _port, EventProcessingAppender _epa) { 
        port = _port;
        epa = _epa;
    }

    /**
    * @param args the command line arguments
    */
    public static void main (String args[]) {

         System.out.println("OBJS SocketManager -- Copyright 2003, Object Services and Consulting, Inc. All Rights Reserved.");
         try {
            if (args.length>0) // then arg 0 is port #
                PORT = Integer.parseInt(args[0]);        
        } catch (Exception e) {
            System.out.println("Exception parsing port #, exiting...");
            System.exit(-1);
        }
        new SocketManager(PORT, new EventProcessingAppender()).start();
    }

    /* Start Listeneing for connections */
    public void run() {

        try {
            server = new ServerSocket(port, 100, InetAddress.getLocalHost());
            System.out.println("Starting OBJS SocketManager on port "+port+"...");
        } catch (Exception e) {System.out.println("OBJS SocketManager ServerSocket Exception:" +e);}

        try {

            Logger logger = Logger.getLogger("DEFAULT_LOGGER");
            AsyncAppender async = new AsyncAppender();
            async.setBufferSize(1000);
            async.addAppender(epa);
            logger.addAppender(async);
            
            while (true) { //accept connections
                System.out.println("SocketManager waiting for a connection...");                    
                Socket c = server.accept();
                System.out.println("SocketManager accepted a connection...");                    
                SocketNode sh = new SocketNode(c,new Hierarchy(logger));
                new Thread(sh).start();
            }            
        } catch (Exception e) {System.out.println("SocketManager Socket Exception:" +e); e.printStackTrace();}
    }
}


