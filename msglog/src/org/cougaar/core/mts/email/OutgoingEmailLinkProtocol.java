/*
 * <copyright>
 *  Copyright 2001 Object Services and Consulting, Inc. (OBJS),
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
 * 24 Sep 2002: Add new serialization & socket closer support. (OBJS)
 * 18 Aug 2002: Various enhancements for Cougaar 9.4.1 release. (OBJS)
 * 18 Jun 2002: Restored Node name to outboxes properties to facilitate
                CSMART test configuration. (OBJS)
 * 11 Apr 2002: Removed Node name from outboxes property (2 reasons -
 *              no longer able to get Node name, and, more importantly,
 *              each Node has it's own properties, the differentiation
 *              was just done for testing convienience.)  Update from 
 *              Cougaar 9.0.0 to 9.1.x (OBJS)
 * 26 Mar 2002: Update from Cougaar 8.6.2.x to 9.0.0 (OBJS)
 * 19 Dec 2001: Commented out FQDN warning per Steve's request. (OBJS)
 * 15 Dec 2001: Changed missing msg nums to "-" in mail header. (OBJS)
 * 11 Dec 2001: Save backup of cached mail data from name server. (OBJS) 
 * 02 Dec 2001: Implement getRemoteReference (8.6.2.0) (OBJS)
 * 02 Dec 2001: Now set the msgNum in the mail message subject line here. (OBJS)
 * 20 Nov 2001: Cougaar 8.6.1 compatibility changes. (OBJS)
 * 21 Oct 2001: Make call to sendMessage class synchronized. (OBJS)
 * 26 Sep 2001: Rename: MessageTransport to LinkProtocol, add debug & cost
 *              properties. (OBJS)
 * 25 Sep 2001: Updated from Cougaar 8.4 to 8.4.1 (OBJS)
 * 16 Sep 2001: Updated from Cougaar 8.3.1 to 8.4 (OBJS)
 * 24 Aug 2001: Revamped for new 8.3.1 component model. (OBJS)
 * 14 Jul 2001: Require FQDN hostnames in props file, since FQDN not avail
 *              from java.net.InetAddress under Windows & JDK 1.3. (OBJS)
 * 11 Jul 2001: Added initial name server support. (OBJS)
 * 08 Jul 2001: Created. (OBJS)
 */

package org.cougaar.core.mts.email;

import java.io.*;
import java.util.*;
import java.net.InetAddress;
import javax.mail.URLName;
import java.security.MessageDigest;

import org.cougaar.core.mts.*;
import org.cougaar.core.service.LoggingService;

/**
 * OutgoingEmailLinkProtocol is an OutgoingLinkProtocol which uses email to
 * send Cougaar messages from the current node to other nodes in the society.
 * <p>
 * <b>System Properties:</b>
 * <p>
 * <b>org.cougaar.message.protocol.classes</b>
 * Cause this link protocol to be loaded at init time by adding
 * org.cougaar.core.mts.email.OutgoingEmailLinkProtocol to this System
 * property defined in your setarguments.bat file. If you don't have such a property, add one. 
 * Multiple protocols are separated by commas.
 * <br>(e.g. -Dorg.cougaar.message.transport.classes=org.cougaar.core.mts.email.OutgoingEmailLinkProtocol,
 * org.cougaar.core.mts.email.IncomingEmailLinkProtocol)
 * <p>
 * <b>org.cougaar.message.protocol.email.outboxes.<node-name></b> 
 * Specify the outbound SMTP servers for a node by setting this property 
 * to a string of the form <i>smtp,host,port,replyTo,cc,bcc</i>, where:
 * <pre>
 * smtp:  The literal string "smtp".
 * host:  The fully qualified domain name (FQDN) of the host running the SMTP mail server.
 * port:  The port the SMTP mail server is listening on (typically port 25).
 * replyTo:  Either an email address (e.g. someone@somewhere.com) or a dash ("-"). Use of the "-" 
 * means skip this feature.  If an email address is supplied, it is set as the Reply-To field on all 
 * email sent out via this SMTP server.  This feature is expected to only be used in rare testing 
 * and debugging situations.
 * cc:   Either an email address (e.g. someone@somewhere.com) or a dash ("-").  Use of the "-" means 
 * skip this feature.  If an email address is supplied, it is set as the CC (carbon copy)  field on 
 * all email sent out via this SMTP server.  This feature is expected to only be used in rare testing 
 * and debugging situations.
 * bcc:   Either an email address (e.g. someone@somewhere.com) or a dash ("-").  Use of the "-" means 
 * skip this feature.  If an email address is supplied, it is set as the BCC (blind carbon copy) field 
 * on all email sent out via this SMTP server.  This feature is expected to only be used in rare 
 * testing and debugging situations.
 * </pre>
 * (e.g. for a node named "X": 
 * -Dorg.cougaar.message.transport.email.outboxes.X=smtp,wally.objs.com,25,-,-,-)
 * <br><i>[Note:  Currently, only one outbox per node can be specified. That will change.]</i>
 * <p>
 * <b>org.cougaar.message.protocol.email.cost</b>
 * The cost function of the DestinationLink inner subclass defaults to 1500, so 
 * that, using the default MinCostLinkSelectionPolicy, it will be chosen after 
 * OutgoingSocketLinkProtocol and RMILinkProtocol, and before NNTPLinkProtocol. 
 * When using AdaptiveLinkSelectionPolicy, cost is
 * one of the factors that are used to select a protocol. To modify the default
 * cost, set the property to an integer 
 * <br>(e.g. org.cougaar.message.protocol.email.cost= 750).
 * <p>
 * <b>org.cougaar.message.protocol.email.debug</b> 
 * If true, prints debug information to System.out.
 * <p>
 * <b>mail.debug</b> 
 * If true, JavaMail prints debug information to System.out.
 * */

public class OutgoingEmailLinkProtocol extends OutgoingLinkProtocol
{
  public static final String PROTOCOL_TYPE = "-email";

  private static final int protocolCost;
  private static final boolean useFQDNs;
  private static final int socketTimeout;
  private static final long maxMessageSizeKB;
  private static final boolean embedMessageDigest;
  private static final boolean showMailServerInteraction;

  private static int SID;

  private LoggingService log;
  private Hashtable mailDataCache;
  private HashMap links;
  private MailBox outboxes[];

  static
  {
    //  Read external properties

    String s = "org.cougaar.message.protocol.email.cost";  // one way
    protocolCost = Integer.valueOf(System.getProperty(s,"10000")).intValue();  // was 5000

    s = "org.cougaar.message.protocol.email.useFQDNs";
    useFQDNs = Boolean.valueOf(System.getProperty(s,"true")).booleanValue();

    s = "org.cougaar.message.protocol.email.outgoing.socketTimeout";
    socketTimeout = Integer.valueOf(System.getProperty(s,"5000")).intValue();

    s = "org.cougaar.message.protocol.email.outgoing.maxMessageSizeKB";
    maxMessageSizeKB = Integer.valueOf(System.getProperty(s,"1000")).intValue();

    s = "org.cougaar.message.protocol.email.outgoing.embedMessageDigest";
    embedMessageDigest = Boolean.valueOf(System.getProperty(s,"true")).booleanValue();

    s = "org.cougaar.message.protocol.email.outgoing.showMailServerInteraction";
    showMailServerInteraction = Boolean.valueOf(System.getProperty(s,"false")).booleanValue();
  }

  public OutgoingEmailLinkProtocol ()
  {
    mailDataCache = new Hashtable();
    links = new HashMap();
  }

  public void load ()
  {
    super_load();
    log = loggingService;

    if (log.isInfoEnabled()) log.info ("Creating " + this);

    MailMan.setServiceBroker (getServiceBroker());
    MailMan.setSmtpSocketTimeout (socketTimeout);
    MailMan.setSmtpDebug (showMailServerInteraction);

    String nodeID = getRegistry().getIdentifier();
    String s = "org.cougaar.message.protocol.email.outboxes." + nodeID;
    String outboxesProp = System.getProperty (s);

    if (outboxesProp == null || outboxesProp.equals(""))
    {
      String str = "Bad or missing property: " +s;
      log.error (str);
      throw new RuntimeException (str);
    }

    if (startup (outboxesProp) == false)
    {
      String str = "Failure starting up " + this;
      log.error (str);
      throw new RuntimeException (str);
    }
  }

  public synchronized boolean startup (String outboxesProp)
  {
    shutdown();

    //  Only using the first outbox at this time

    outboxes = parseOutboxes (outboxesProp);
    if (outboxes == null || outboxes.length < 1) return false;
    MailBox outbox = outboxes[0];

    try
    {
      //  Speak up if a mail server is not accessible right now

      if (MailMan.checkOutboxAccess (outbox) == false)
      {
        if (log.isWarnEnabled())
        {
          log.warn 
          (
            "ALERT: Is your mail server up?  Outbox configured?\n" +
            "Unable to access mail outbox: " +outbox.toStringDiscreet()
          );
        }
      }
    }
    catch (Exception e)
    {
      log.error (stackTraceToString (e));
      return false;
    }

    return true;
  }

  public synchronized void shutdown ()
  {
    outboxes = null;
  }

  public MailBox[] parseOutboxes (String outboxesProp)
  {
    if (outboxesProp == null) return null;

    Vector out = new Vector();
 
    int MaxBoxes = 1;  // FIXED to 1 for now, perhaps forever

    //  Parse the outbox specs (separated by semicolons)

    for (int i=0; i<MaxBoxes; i++)
    {
      StringTokenizer specs = new StringTokenizer (outboxesProp, ";");

      if (specs.hasMoreTokens())
      {
        String spec = specs.nextToken();
        StringTokenizer st = new StringTokenizer (spec, ",");

        while (st.hasMoreTokens()) 
        {
          String protocol = st.nextToken();

          //  SMTP outboxes  
          //
          //  Format: smtp,host,port,replyTo,cc,bcc;...;... (general)
          //          smtp,host,port,-,-,-                  (typical)

          if (protocol.equals ("smtp"))
          {
            try
            {
              String host = nextParm (st);
              String port = nextParm (st);

              if (useFQDNs)
              {
                String hostFQDN = getHostnameFQDN (host);
                if (log.isInfoEnabled()) log.info ("Using FQDN " +hostFQDN+ " for outbox mailhost " +host);
                host = hostFQDN;
              }

              MailBox mbox = new MailBox (protocol, host, port);

              String replyTo = nextParm (st);
              String cc = nextParm (st);
              String bcc = nextParm (st);

              if (replyTo != null || cc != null || bcc != null)
              {
                MailMessageHeader hdr = new MailMessageHeader (null, replyTo, null, cc, bcc, null);
                mbox.setBoxHeader (hdr);
              }

              out.add (mbox);
            }
            catch (Exception e)
            {
              log.error ("Bad outbox spec: " +spec+ " (ignored): " +e);
            }
          }
          else break;
        }
      }
      else break;
    }

    //  Create the outboxes array and return it

    if (out.size() > 0)  
    {
      outboxes = (MailBox[]) out.toArray (new MailBox[out.size()]);
      return outboxes;
    }
    else
    {
      log.error ("No outboxes defined in " +outboxesProp);
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

  public String toString ()
  {
    return this.getClass().getName();
  }

  private MailData lookupMailData (MessageAddress address)
  {
    Object obj = getNameSupport().lookupAddressInNameServer (address, PROTOCOL_TYPE);

    if (obj != null)
    {
      if (obj instanceof MailData)
      {
        return (MailData) obj;
      }
      else
      {
        log.error ("Invalid non-MailData object in name server!");
      }
    }

    return null;
  }

  private MailData getMailData (MessageAddress address) throws NameLookupException
  {
    synchronized (mailDataCache)
    {
      MailData mailData = (MailData) mailDataCache.get (address);
      if (mailData != null) return mailData;
      mailData = lookupMailData (address);
      if (mailData != null) mailDataCache.put (address, mailData);
      return mailData;
    }
  }

  private synchronized void clearCaches ()
  {
    mailDataCache.clear();
  }

  public boolean addressKnown (MessageAddress address) 
  {
    try 
    {
      return (getMailData (address) != null);
    } 
    catch (Exception e) 
    {
      log.error (stackTraceToString (e));
    }

    return false;
  }

  public DestinationLink getDestinationLink (MessageAddress address) 
  {
    DestinationLink link = (DestinationLink) links.get (address);

    if (link == null) 
    {
      link = new EmailOutLink (address);
      link = (DestinationLink) attachAspects (link, DestinationLink.class);
      links.put (address, link);
    }

    return link;
  }

  public static long getMaxMessageSizeInBytes ()
  {
    return maxMessageSizeKB*1024;
  }

  private synchronized static int getNextSendID ()  // for debugging purposes
  {
    return SID++;
  }

  class EmailOutLink implements DestinationLink 
  {
    private MessageAddress destination;
    private MailData mailData, savedMailData;
    private String sid;

    public EmailOutLink (MessageAddress destination) 
    {
      this.destination = destination;
    }

    public MessageAddress getDestination () 
    {
      return destination;
    }

    public String toString ()
    {
      return OutgoingEmailLinkProtocol.this +"-destination:"+ destination;
    }

    public Class getProtocolClass () 
    {
      return OutgoingEmailLinkProtocol.class;
    }
   
    public int cost (AttributedMessage msg) 
    {
      if (msg == null) return protocolCost;  // forced HACK
      return (addressKnown(destination) ? protocolCost : Integer.MAX_VALUE);
    }

    public Object getRemoteReference ()
    {
      return null;
    }
    
    public void addMessageAttributes (MessageAttributes attrs)
    {}

    public boolean retryFailedMessage (AttributedMessage msg, int retryCount)
    {
      return true;
    }

    private synchronized void dumpCachedData ()
    {
      clearCaches();
    }

    public synchronized MessageAttributes forwardMessage (AttributedMessage msg) 
      throws NameLookupException, UnregisteredNameException,
             CommFailureException, MisdeliveredMessageException
    {
      //  Dump our cached data on message send retries

      if (MessageUtils.getSendTry (msg) > 1) dumpCachedData();

      //  Get emailing info for destination
    
      MailData mailData = getMailData (destination);

      if (mailData == null)
      {
        String s = "No nameserver info for " +destination;
        if (log.isWarnEnabled()) log.warn (s);
        throw new NameLookupException (new Exception (s));
      }

      //  Send message via email

      boolean success = false;
      Exception save = null;

      try 
      {
        success = sendMessage (msg, mailData);
      } 
      catch (Exception e) 
      {
        if (log.isDebugEnabled()) log.debug ("sendMessage: " +stackTraceToString(e));
        save = e;
      }

      //  Dump our cached data on failed sends and throw an exception

      if (success == false)
      {
        dumpCachedData();
        Exception e = (save==null ? new Exception ("email sendMessage unsuccessful") : save);
        throw new CommFailureException (e);
      }

      //  Successful send

	  MessageAttributes successfulSend = new SimpleMessageAttributes();
      String status = MessageAttributes.DELIVERY_STATUS_DELIVERED;
	  successfulSend.setAttribute (MessageAttributes.DELIVERY_ATTRIBUTE, status);
      return successfulSend;
    }
   
    private synchronized boolean sendMessage (AttributedMessage msg, MailData destAddr) throws Exception
    {
      if (log.isDebugEnabled())
      {
        sid = "s" +getNextSendID()+ " ";
        log.debug (sid+ "Sending " +MessageUtils.toString(msg));
      }

      //  Serialize the message into a byte array.  Optionally help insure message integrity
      //  via a message digest (eg. an embedded MD5 hash of the message).

      MessageDigest digest = (embedMessageDigest ? MessageDigest.getInstance("MD5") : null);
      byte msgBytes[] = MessageSerializationUtils.writeMessageToByteArray (msg, digest);
      if (msgBytes == null) return false;

      //  Make sure the message is not too large (or small) for an email message

      if (msgBytes.length >= getMaxMessageSizeInBytes())
      {
        if (log.isWarnEnabled())
        {
          log.warn (sid+ "Msg exceeds " +(getMaxMessageSizeInBytes()/1024)+ " KB max email " +
            "message size! (" +(msgBytes.length/1024)+ " KB): " +MessageUtils.toString(msg));
        }

        MessageUtils.setMessageSize (msg, msgBytes.length);  // size stops link selection
        return false;
      }
      else if (msgBytes.length == 0)
      {
        if (log.isWarnEnabled()) log.warn (sid+ "No email sent as msg is 0 bytes");
        return true;
      }

      //  Send the message as an email

      MailBox outbox = outboxes[0];  // currently only sending to one mail server

      try
      {
        //  Create email header
 
        String localnode = replaceSpaces (MessageUtils.getFromAgentNode (msg));
        String localhost = outbox.getServerHost();

        String remotenode = replaceSpaces (destAddr.getNodeID());
        String remoteuser = destAddr.getInbox().getUsername();
        String remotehost = destAddr.getInbox().getServerHost();

        MailMessageHeader boxHeader = outbox.getBoxHeader();

        String from = "EmailStream#"+ localnode +"@"+ localhost;
        String replyTo = null, cc = null, bcc = null;

        if (boxHeader != null)
        {
          replyTo = boxHeader.getReplyTo().getMaxAddress();
          cc =      boxHeader.getCc().getMaxAddress();
          bcc =     boxHeader.getBcc().getMaxAddress();
        }

        String to = remoteuser +"@"+ remotehost;
        String subject = "To: " +remotenode+ " Msg: " + MessageUtils.getMessageNumber (msg);

        MailMessageHeader header = new MailMessageHeader (from, replyTo, to, cc, bcc, subject);

        //  Create email body

        MailMessageBody body = new MailMessageBody (msgBytes);

        //  Create email message

        MailMessage emailMsg = new MailMessage (header, body);

        //  Send email message

        if (log.isDebugEnabled()) log.debug (sid+ "Sending email:\n" + emailMsg);
        MailMan.sendMessage (outbox, emailMsg);
      }
      catch (Exception e)
      {
        throw (e);
      }

      return true;  // send successful
    }

    private String replaceSpaces (String s)
    {
      return s.replace (' ', '_');  // replace spaces with underscores
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
