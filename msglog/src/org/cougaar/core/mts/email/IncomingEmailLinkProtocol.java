/*
 * <copyright>
 *  Copyright 2001-2003 Object Services and Consulting, Inc. (OBJS),
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
 * CHANGE RECORD 
 * 06 Mar 2003: Completed support for URIs and multiple inboxes. (OBJS)
 * 12 Feb 2003: Port to 10.0 (OBJS)
 * 24 Sep 2002: Add new serialization, socket closer support and remove
 *              email streams stuff for more direct reading of email. (OBJS)
 * 19 Jun 2002: Removed "ignoreOldMessages" functionality - obsolete in
 *              a persistent world. (OBJS)
 * 18 Jun 2002: Restored Node name to inboxes properties due to facilitate
                CSMART test configuration. (OBJS)
 * 11 Jun 2002: Move to Cougaar threads. (OBJS)
 * 11 Apr 2002: Removed Node name from inboxes property (2 reasons -
 *              no longer able to get Node name, and, more importantly,
 *              each Node has it's own properties, the differentiation
 *              was just done for testing convienience.)  (OBJS)
 * 08 Jan 2002: Add mailServerPollTimeSecs property.  (OBJS)
 * 19 Dec 2001: Commented out FQDN warning per Steve's request. (OBJS)
 * 29 Nov 2001: Restrict data registered with nameserver to just that
 *              needed by the mail senders to us. (OBJS)
 * 29 Oct 2001: Conditionally print "<E" on successful message receipt. (OBJS)
 * 22 Oct 2001: Rewrote message in thread to handle server outages and
 *              restorations.  Went to a no header ObjectInputStream. (OBJS)
 * 26 Sep 2001: Rename: MessageTransport to LinkProtocol, add debug
 *              property. (OBJS)
 * 25 Sep 2001: Port to Cougaar 8.4.1 (OBJS)
 * 16 Sep 2001: Port to Cougaar 8.4 (OBJS)
 * 26 Aug 2001: Revamped for new 8.3.1 component model. (OBJS)
 * 14 Jul 2001: Require FQDN hostnames in props file, since FQDN not avail
 *               from java.net.InetAddress under Windows & JDK 1.3. Add feature
 *               to props file wherein "-" for mailbox username defaults
 *               to node name. (OBJS)
 * 11 July 2001: Added initial name server support. (OBJS)
 * 08 July 2001: Created. (OBJS)
 */

package org.cougaar.core.mts.email;

import java.io.*;
import java.util.*;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;

import org.cougaar.core.mts.*;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;


/**
 * IncomingEmailLinkProtocol is a LinkProtocol which receives 
 * Cougaar messages via the POP3 protocol.
 * <p>
 * <b>System Properties:</b>
 * <p>
 * <b>org.cougaar.message.protocol.classes</b>
 * Cause this link protocol to be loaded at init time by adding
 * org.cougaar.core.mts.email.IncomingEmailLinkProtocol to this System
 * property defined in your setarguments.bat file. If you don't have such a property, add one. 
 * Multiple protocols are separated by commas.
 * <br>(e.g. -Dorg.cougaar.message.protocol.classes=org.cougaar.core.mts.email.OutgoingEmailLinkProtocol,
 * org.cougaar.core.mts.email.IncomingEmailLinkProtocol)
 * <p>
 * <b>org.cougaar.message.protocol.email.inboxes.<node-name></b> 
 * Specify the inbound POP3 mailboxes for a node by setting this property 
 * to a list of URIs delimited by vertical bars, <i>pop3://user:pswd@host:port</i>, where:
 * <pre>
 * pop3:  The literal string "pop3".
 * host:  The fully qualified domain name (FQDN) of the host running the POP3 mail server.
 * port:  The port the POP3 mail server is listening on (typically port 110).
 * user:  The user name for the POP3 mailbox account.
 * pswd:  The password for the POP3 mailbox account
 * </pre>
 * (e.g. for a node named "X":  
 * -Dorg.cougaar.message.protocol.email.inboxes.X=pop3://node1:passwd@wally.objs.com:110)
 * <p>
 * <b>org.cougaar.message.protocol.email.incoming.mailServerPollTimeSecs</b>
 * The number of seconds waited between polls of the POP3 Servers.  The default is 5 seconds.
 * <br>(e.g. -Dorg.cougaar.message.protocol.email.incoming.mailServerPollTimeSecs=5)
 * <p>
 * <b>org.cougaar.message.protocol.email.incoming.socketTimeout</b>
 * The number of milliseconds to wait before closing an unresponsive POP3 socket.  The default is 10000 milliseconds.
 * <br>(e.g. -Dorg.cougaar.message.protocol.email.incoming.socketTimeout=10000)
 * <p>
 * <b>org.cougaar.message.protocol.email.incoming.initialReadDelaySecs</b>
 * The number of seconds to wait at initialization before starting to poll the POP3 Server.  The default is 20 seconds.
 * <br>(e.g. -Dorg.cougaar.message.protocol.email.incoming.initialReadDelaySecs=20)
 * <p>
 * <b>org.cougaar.message.protocol.email.incoming.showMailServerInteraction</b>
 * If true, detailed interaction with the POP3 Server is logged at the DEBUG level.  The default is false.
 * <br>(e.g. -Dorg.cougaar.message.protocol.email.incoming.showMailServerInteraction=false)
 * */

public class IncomingEmailLinkProtocol extends IncomingLinkProtocol
{
  public static final String PROTOCOL_TYPE = "-email";

  private static final MailMessageCache cache = new MailMessageCache();

  //102 private static final boolean useFQDNs;
  private static final int mailServerPollTimeSecs;
  private static final int socketTimeout;
  private static final int initialReadDelaySecs;
  private static final boolean showMailServerInteraction;

  private static boolean showTraffic;
  private static LoggingService log;
  private static int RID;

  private String nodeID;
  private String inboxesProp;
  private MailBox inboxes[];
  private Vector messageInThreads;
  //102 private ThreadService threadService;
  private MailFilters filters;

  static
  {
    //  Read external properties

    //102 String s = "org.cougaar.message.protocol.email.useFQDNs";
    //102 useFQDNs = Boolean.valueOf(System.getProperty(s,"true")).booleanValue();

    String s = "org.cougaar.message.protocol.email.incoming.mailServerPollTimeSecs";
    mailServerPollTimeSecs = Integer.valueOf(System.getProperty(s,"5")).intValue();

    s = "org.cougaar.message.protocol.email.incoming.socketTimeout";
    socketTimeout = Integer.valueOf(System.getProperty(s,"10000")).intValue();

    s = "org.cougaar.message.protocol.email.incoming.initialReadDelaySecs";
    initialReadDelaySecs = Integer.valueOf(System.getProperty(s,"20")).intValue();

    s = "org.cougaar.message.protocol.email.incoming.showMailServerInteraction";
    showMailServerInteraction = Boolean.valueOf(System.getProperty(s,"false")).booleanValue();
  }

  public IncomingEmailLinkProtocol ()
  {
    messageInThreads = new Vector();
    filters = new MailFilters();
  }

  public String toString ()
  {
    return this.getClass().getName();
  }

  public void load ()
  {
    super_load();
    log = loggingService;

    if (log.isInfoEnabled()) log.info ("Creating " + this);

    MailMan.setServiceBroker (getServiceBroker());
    MailMan.setPop3SocketTimeout (socketTimeout);
    MailMan.setPop3Debug (showMailServerInteraction);

    String s = "org.cougaar.core.mts.ShowTrafficAspect";
    showTraffic = (getAspectSupport().findAspect(s) != null);

    nodeID = getRegistry().getIdentifier();
    s = "org.cougaar.message.protocol.email.inboxes." +nodeID;
    inboxesProp = System.getProperty (s);

    if (inboxesProp == null || inboxesProp.equals(""))
    {
      String str = "Bad or missing property: " +s;
      log.error (str);
      throw new RuntimeException (str);
    }

    String fromFilter = "EmailStream#";
    //102 String toFilter = "To: " + nodeID;
    //102 filters.set (fromFilter, toFilter);
    filters.set (fromFilter, null);

    if (startup (inboxesProp) == false)
    {
      String str = "Failure starting up " + this;
      log.error (str);
      throw new RuntimeException (str);
    }
  }

  public synchronized boolean startup (String inboxesProp)
  {
    shutdown();

    //  Only using the first inbox at this time
    inboxes = parseInboxes (inboxesProp);
    if (inboxes == null || inboxes.length < 1) return false;

    //  Start the threads that monitor the inboxes for the incoming
    //  Cougaar message emails.
    for (int i=0; i<inboxes.length; i++)
    {
      MessageInThread msgInThread = null;

      try
      {
        //  Speak up if a mailbox is not accessible right now

        if (MailMan.checkInboxAccess (inboxes[i]) == false)
        {
          if (log.isWarnEnabled()) 
          {
            log.warn 
            (
              "ALERT: Is your mail server up?  Inbox configured?\n" +
              "Unable to access mail inbox: " +inboxes[i].toStringDiscreet()
            );
          }
        }
        msgInThread = new MessageInThread (inboxes[i]);  // actually a Runnable
        //102 should probably give this a try again to see if thread starvation is still an issue
        // Schedulable thread = threadService().getThread (this, msgInThread, "inbox"+i);
        Thread thread = new Thread (msgInThread, "inbox"+i);
        thread.start();
        messageInThreads.add (msgInThread);
      }
      catch (Exception e)
      {
        log.error ("startup: inbox"+i+" exception: " +stackTraceToString(e));
        if (msgInThread != null) msgInThread.quit();
      }
    }

    //  We can't call it a successful startup unless at least one thread 
    //  was successfully started.
    return messageInThreads.size() > 0;
  }

  public synchronized void shutdown ()
  {
    //  Kill all the inboxes

    if (inboxes != null)
    {
      for (int i=0; i<inboxes.length; i++)
      {
        inboxes[i] = null;
      }
      inboxes = null;
    }

    //  Halt and get rid of all the inbox threads

    stopMailReadingThreads();
  }

  private void stopMailReadingThreads ()
  {
    for (Enumeration e = messageInThreads.elements(); e.hasMoreElements(); ) 
    {
      MessageInThread msgInThread = (MessageInThread) e.nextElement();
      msgInThread.quit();  
    }
    messageInThreads.removeAllElements();
  }
  
  //102 convert to URIs and multiple inboxes
  private MailBox[] parseInboxes (String inboxesProp)
  {
    if (inboxesProp == null) return null;

    Vector in = new Vector();

    // Parse multiple inbox URIs (separated by vertical bars)
    StringTokenizer URIs = new StringTokenizer (inboxesProp, "|");

    while (URIs.hasMoreTokens()) {
      String tkn = null;
      try {
        // POP3 inbox URIs have the following form:
        // pop://userid:password@pop3ServerHostName:port
        // e.g. pop://node1:passwd@atom.objs.com:110
        
        tkn = URIs.nextToken();
        in.add(new MailBox(new URI(tkn)));
        
        /*  //102 may want to add support for FQDNs back in later
          if (useFQDNs) {
            String hostFQDN = InetAddress.getByName(host).getCanonicalHostName();
            if (log.isInfoEnabled()) log.info ("Using FQDN " +hostFQDN+ " for inbox mailhost " +host);
            host = hostFQDN;
          }
        */

      } catch (Exception e) {
        log.error ("Bad inbox URI: " + tkn + " (ignored): " +e);
      }
    }
    //  Create the inboxes array and return it
    if (in.size() > 0) 
    {
      inboxes = (MailBox[])in.toArray(new MailBox[in.size()]);
      return inboxes;
    } 
    else 
    {
      log.error ("No inboxes defined in " +inboxesProp);
      return null;
    }
  }

  public final void registerClient (MessageTransportClient client) //102 convert to URIs and multiple inboxes
  {
    try 
    {
      if (inboxes == null) return;  // nothing to register
      MessageAddress clientAddress = client.getMessageAddress();
      for (int i = 0; i<inboxes.length; i++) {
        getNameSupport().registerAgentInNameServer (inboxes[i].getMailto(), clientAddress, PROTOCOL_TYPE);
      }
    } 
    catch (Exception e) 
    {
      log.error (stackTraceToString (e));
    }
  }

  public final void unregisterClient (MessageTransportClient client) //102 convert to URIs and multiple inboxes
  { 
    try 
    {
      if (inboxes == null) return;  // no data to unregister
      MessageAddress clientAddress = client.getMessageAddress();
      for (int i = 0; i<inboxes.length; i++) {
        getNameSupport().unregisterAgentInNameServer (inboxes[i].getMailto(), clientAddress, PROTOCOL_TYPE);
      }
    } 
    catch (Exception e) 
    {
      log.error (stackTraceToString (e));
    }
  }

  public final void registerMTS (MessageAddress addr)
  {
/* //102 no longer required
    try 
    {
      if (inboxes == null) return;  // no data to unregister
      for (int i = 0; i<inboxes.length; i++) {
        getNameSupport().registerAgentInNameServer (myMailData.getURI(), addr, PROTOCOL_TYPE); //100
    } 
    catch (Exception e) 
    {
      log.error (stackTraceToString (e));
    }
*/
  }

/* //102 not currently in use due to thread starvation problems
  private ThreadService threadService () 
  {
	if (threadService != null) return threadService;
	threadService = (ThreadService) getServiceBroker().getService (this, ThreadService.class, null);
	return threadService;
  }
*/

  private synchronized static int getNextReceiveID ()  // for debugging purposes
  {
    return RID++;
  }

  private class MessageInThread implements Runnable
  { 
    private MailBox inbox;
    private String rid;
    private boolean quitNow;
    private boolean firstTime = true;

    public MessageInThread (MailBox inbox)
    {
      this.inbox = inbox;
    }

    public void quit ()
    {
      quitNow = true;
    }

    public void run() 
    {
      //  HACK - workaround for problem where email messages are read and deserialized
      //  before the node is ready for them (thus causing exceptions).
      if (firstTime)
      {
        try { Thread.sleep (initialReadDelaySecs*1000); } 
        catch (Exception e) {
          log.debug("Caught exception in sleep " +e);
        }
        if (log.isInfoEnabled()) log.info ("Initial email reading delay now over for " +inbox);
        firstTime = false;
      }

      boolean access;
      int n, waitTime;
        
      while (!quitNow) 
      {
        //  Make sure inbox is still accessible

        access = false;
        n = 0;

        while (!quitNow)
        {
          try
          {
            access = MailMan.checkInboxAccess (inbox);
          }
          catch (Exception e)
          {
            log.error ("Checking inbox access: " +e);
          }

          if (access == false)
          {
            //  Print out a message that the inbox is not accessible
            //  and then wait for awhile till we try again.
            
            n++;
           
            if (n < 20)       waitTime = 15;
            else if (n < 100) waitTime = 30;
            else              waitTime = 60;
         
            if (log.isInfoEnabled())    
            {
              log.info 
              (
                "ALERT: Unable to access mail inbox:\n" +inbox.toStringDiscreet()+
                "\nTry " +n+ ": Will try to access it again in " +waitTime+ " seconds..."
              );
            }

            try { Thread.sleep (waitTime*1000); } 
            catch (Exception e) {
              log.debug("Caught exception in sleep " +e);
            }
          }
          else
          {
            if (n > 0 && log.isInfoEnabled()) log.info ("Inbox is accessible again: " +inbox);
            break;
          }
        }

        //  Read and process incoming email messages.  If we encounter a failure we stop 
        //  and go back to the top and start over establishing the inbox access.

        while (!quitNow)
        {
          if (readAndDeliverMessage (inbox) == false) break;
        }
      }    
    }

    private final synchronized boolean readAndDeliverMessage (MailBox mbox)
    {
      MailMessage mailMsg = null;

      try 
      {
        //  Get next mail message, waiting for it if needed
      
        if (log.isDebugEnabled()) 
        {
          rid = "r" +getNextReceiveID()+ " ";
          log.debug (rid+ " Waiting for next msg from " +mbox);
        }

        mailMsg = getNextMailMessage (rid, mbox);
        if (log.isDebugEnabled()) log.debug (rid+ "Got next msg from " +mbox);
        if (showTraffic) System.out.print ("<E");
      } 
      catch (Exception e) 
      {
        if (log.isInfoEnabled())
        {
          if (mailMsg == null) 
          {
            String detail = (log.isDebugEnabled() ? stackTraceToString(e) : "");
            log.info (rid+ "Lost access to " +mbox+ "... " +detail);
          }
        }

        return false;  // inbox reading failure
      }

      if (mailMsg == null) return false;
      byte[] msgBytes = mailMsg.getBodyBytes();

      //  Deserialize the read bytes into a Cougaar message

      AttributedMessage msg = null;

      try
      {
        msg = MessageSerializationUtils.readMessageFromByteArray (msgBytes);
      }
      catch (MessageDeserializationException e)
      {
        if (log.isWarnEnabled()) log.warn(rid+ " Deserialization exception d): " +e);
        if (log.isWarnEnabled()) log.warn("A few of these are ok, else lengthen email stream timeouts.");
        return true;  
      }

      if (log.isDebugEnabled()) log.debug (rid+ "Got " +MessageUtils.toString(msg));

      //  Deliver the message.  Nobody to send exceptions to, so we just log them.

      try
      {
        if (msg != null) getDeliverer().deliverMessage (msg, msg.getTarget());
      }
      catch (MisdeliveredMessageException e)
      { 
        if (log.isDebugEnabled()) 
          log.debug (rid+ "Got MisdeliveredMessageException for " +MessageUtils.toString(msg)+ ": " +e);
      }
      catch (Exception e)
      { 
        if (log.isWarnEnabled()) 
          log.warn (rid+ "Exception delivering " +MessageUtils.toString(msg)+ ": " +stackTraceToString(e));
      }

      return true;  // mbox still accessible
    }

    private MailMessage getNextMailMessage (String rid, MailBox mbox) throws Exception
    {
      while (true)
      {
        //  Check the cache for a mail message

        MailMessage mailMsg = cache.getNextMessage();  // messages NOT necessarily in order
        if (mailMsg != null) return mailMsg;

        //  Try reading some messages from the inbox on the mail server.  If we
        //  don't get any, sleep for awhile.  Otherwise put them in the cache
        //  (where some message filtering may occur) and then try the cache again.

        while (true)
        {
          MailMessage[] mailMsgs = MailMan.readMessages (mbox, filters, 0);
          int n = (mailMsgs != null ? mailMsgs.length : 0);

          if (n > 0) 
          {
            if (log.isDebugEnabled()) 
              for (int i=0; i<n; i++) log.debug (rid+ "Read email:\n" + mailMsgs[i]);

            cache.addMessages (mailMsgs);
            break;
          }
          else 
          {
            try { Thread.sleep (mailServerPollTimeSecs*1000); } 
            catch (Exception e) {
              log.debug("Caught exception in sleep " +e);
            }
          }
        }
      }
    }
  }

  private static String stackTraceToString (Exception e)
  {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter (stringWriter);
    e.printStackTrace (printWriter);
    return stringWriter.getBuffer().toString();
  }
}
