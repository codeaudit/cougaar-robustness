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
import java.net.URISyntaxException;

import org.cougaar.core.mts.*;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;


/**
 * IncomingEmailLinkProtocol is an IncomingLinkProtocol which receives
 * Cougaar messages from other nodes in a society via email.
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
 * <b>org.cougaar.message.protocol.email.inboxes</b> 
 * Specify the inbound POP3 mailboxes for a node by setting this property 
 * to a string of the form <i>pop3,host,port,user,pswd</i>, where:
 * <pre>
 * pop3:  The literal string "pop3".
 * host:  The fully qualified domain name (FQDN) of the host running the POP3 mail server.
 * port:  The port the POP3 mail server is listening on (typically port 110).
 * user:  The user name for the POP3 mailbox account.
 * pswd:  The password for the POP3 mailbox account
 * </pre>
 * (e.g. -Dorg.cougaar.message.protocol.email.inboxes=pop3,wally.objs.com,110,PerfNodeA,james)
 * <br><i>[Note:  Currently, only one inbox per node can be specified. That will change.]</i>
 * <p>
 * <b>org.cougaar.message.protocol.nntp.ignoreOldMessages</b>
 * By default, IncomingEmailLinkProtocol ignores Cougaar messages 
 * already cached at a POP3 server before the node is initialized, to avoid
 * processing messages that might have been sent in an earlier run.
 * To disable this feature, set this property to false. 
 * <br><i>[Note: this is a temporary workaround, and is likely to disappear.]</i>
 * <p>
 * <b>org.cougaar.message.protocol.email.oldMsgGracePeriod</b>
 * Messages sent this number of milliseconds before initialization
 * of this node will be accepted when <b>ignoreOldMessages</b> is true,
 * to account for the fact clocks might not be synchronized.  It should
 * be set to less than the time between tests.  It defaults to 120000ms
 * (2 minutes). 
 * <br><i>[Note: this is a temporary workaround, and is likely to disappear.]</i>
 * <p>
 * <b>org.cougaar.message.protocol.email.debug</b> 
 * If true, prints debug information to System.out.
 * <p>
 * <b>mail.debug</b> 
 * If true, JavaMail prints debug information to System.out.
 * */

public class IncomingEmailLinkProtocol extends IncomingLinkProtocol
{
  public static final String PROTOCOL_TYPE = "-email";

  private static final MailMessageCache cache = new MailMessageCache();

  private static final boolean useFQDNs;
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
  private MailData myMailData;
  private Vector messageInThreads;
  private ThreadService threadService;
  private MessageAddress myAddress;
  private MailFilters filters;
  private boolean firstTime = true;

  static
  {
    //  Read external properties

    String s = "org.cougaar.message.protocol.email.useFQDNs";
    useFQDNs = Boolean.valueOf(System.getProperty(s,"true")).booleanValue();

    s = "org.cougaar.message.protocol.email.incoming.mailServerPollTimeSecs";
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
    String toFilter = "To: " + nodeID;
    filters.set (fromFilter, toFilter);

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
        // Schedulable thread = threadService().getThread (this, msgInThread, "inbox"+i);
        Thread thread = new Thread (msgInThread, "inbox"+i);
        thread.start();

        registerMailData (nodeID, inboxes[i]);
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
        // unregisterMailData ();
        inboxes[i] = null;
      }

      unregisterMailData ();
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

  public MailBox[] parseInboxes (String inboxesProp)
  {
    if (inboxesProp == null) return null;

    Vector in = new Vector();

    int MaxBoxes = 1;  // only handling one at the moment

    //  Parse the inbox specs (separated by semicolons)

    for (int i=0; i<MaxBoxes; i++)
    {
      StringTokenizer specs = new StringTokenizer (inboxesProp, ";");

      if (specs.hasMoreTokens())
      {
        String spec = specs.nextToken();
        StringTokenizer st = new StringTokenizer (spec, ",");

        while (st.hasMoreTokens()) 
        {
          String protocol = st.nextToken();

          //  POP3 inboxes
          //
          //  Format: pop3,host,port,user,pswd;...;...

          if (protocol.equals ("pop3"))
          {
            try
            {
              String host = nextParm (st);
              String port = nextParm (st);
              String user = nextParm (st);
              String pswd = nextParm (st);

              if (useFQDNs)
              {
                String hostFQDN = getHostnameFQDN (host);
                if (log.isInfoEnabled()) log.info ("Using FQDN " +hostFQDN+ " for inbox mailhost " +host);
                host = hostFQDN;
              }

              in.add (new MailBox (protocol, host, port, user, pswd, "Inbox"));
            }
            catch (Exception e)
            {
              log.error ("Bad inbox spec: " +spec+ " (ignored): " +e);
            }
          }
          else break;
        }
      }
      else break;
    }

    //  Create the inboxes array and return it

    if (in.size() > 0)  
    {
      inboxes =  (MailBox[]) in.toArray  (new MailBox[in.size()]);
      return inboxes;
    }
    else
    {
      log.error ("No inboxes defined in " +inboxesProp);
      return null;
    }
  }

  private String nextParm (StringTokenizer st)
  {
    //  Convert "-" strings into nulls

    String next = st.nextToken().trim();
    if (next.equals("-")) next = null;
    return next;
  }

  private String getHostnameFQDN (String hostname) throws java.net.UnknownHostException
  {
    return InetAddress.getByName(hostname).getCanonicalHostName();
  }

  private void registerMailData (String nodeID, MailBox mbox)
  {
    //  Update the name server
    //  LIMITATION:  Only one MailData per node (thus only one inbox).
    //  How to manage more than one (transactions can come into play).
    MailBox myMailBox;
    try {
     myMailBox = new MailBox (mbox.getServerHost(), mbox.getUsername());  
    } catch (URISyntaxException e) { //100
      log.error("registerMailData: error creating MailBox host=" + mbox.getServerHost() + 
                ",user=" + mbox.getUsername() ); //100
      log.error(stackTraceToString (e)); //100
      return; //100
    } 
    myMailData = new MailData (nodeID, myMailBox);
    MessageAddress nodeAddress = getNameSupport().getNodeMessageAddress();
    getNameSupport().registerAgentInNameServer (myMailData.getURI(), nodeAddress, PROTOCOL_TYPE); //100
  }

  private void unregisterMailData ()
  {
    if (myMailData == null) return;  // no data to unregister
    MessageAddress nodeAddress = getNameSupport().getNodeMessageAddress();
    getNameSupport().unregisterAgentInNameServer (myMailData.getURI(), nodeAddress, PROTOCOL_TYPE); //100
  }

  public final void registerClient (MessageTransportClient client) 
  {
    try 
    {
      if (myMailData == null) return;  // no data to register
      MessageAddress clientAddress = client.getMessageAddress();
      getNameSupport().registerAgentInNameServer (myMailData.getURI(), clientAddress, PROTOCOL_TYPE); //100
    } 
    catch (Exception e) 
    {
      log.error (stackTraceToString (e));
    }
  }

  public final void unregisterClient (MessageTransportClient client) 
  { 
    try 
    {
      if (myMailData == null) return;  // no data to unregister
      MessageAddress clientAddress = client.getMessageAddress();
      getNameSupport().unregisterAgentInNameServer (myMailData.getURI(), clientAddress, PROTOCOL_TYPE); //100
    } 
    catch (Exception e) 
    {
      log.error (stackTraceToString (e));
    }
  }

  public final void registerMTS (MessageAddress addr)
  {
    try 
    {
      if (myMailData == null) return;  // no data to register
      getNameSupport().registerAgentInNameServer (myMailData.getURI(), addr, PROTOCOL_TYPE); //100
    } 
    catch (Exception e) 
    {
      log.error (stackTraceToString (e));
    }
  }

  private ThreadService threadService () 
  {
	if (threadService != null) return threadService;
	threadService = (ThreadService) getServiceBroker().getService (this, ThreadService.class, null);
	return threadService;
  }

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
        try { Thread.sleep (initialReadDelaySecs*1000); } catch (Exception e) {}
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

            try { Thread.sleep (waitTime*1000); } catch (Exception e) {}
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
          log.debug (rid+ "Waiting for next msg from " +mbox);
        }

        mailMsg = getNextMailMessage (rid, mbox);
        if (log.isDebugEnabled()) log.debug (rid+ "Got next msg from " +mbox);
        if (showTraffic) System.err.print ("<E");
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
        if (log.isWarnEnabled()) log.warn (rid+ "Deserialization exception (msg ignored): " +e);
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
            try { Thread.sleep (mailServerPollTimeSecs*1000); } catch (Exception e) {}
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
