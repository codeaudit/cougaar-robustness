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
 * 27 May 2003: Ported to 10.4 - changes to NameSupport access (104B)
 * 07 May 2003: Updated addMessageAttributes for Security compatibility. (102B)
 * 06 Mar 2003: Switched to URIs and added multiple outboxes. (OBJS)
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

package org.cougaar.mts.std.email;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.URI;
import javax.mail.URLName;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.SimpleMessageAttributes;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Callback;
import org.cougaar.core.service.wp.Request;
import org.cougaar.core.service.wp.Response;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.LinkProtocol;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.StandardAspect;
import org.cougaar.mts.base.UnregisteredNameException;
import org.cougaar.mts.std.AgentIDCallback;
import org.cougaar.mts.std.AttributedMessage;
import org.cougaar.mts.std.CbTblEntry;
import org.cougaar.mts.std.MessageSerializationUtils;
import org.cougaar.mts.std.MessageUtils;

/**
 * OutgoingEmailLinkProtocol is a LinkProtocol which sends 
 * Cougaar messages via the SMTP protocol.
 * <p>
 * <b>System Properties:</b>
 * <p>
 * <b>org.cougaar.message.protocol.classes</b>
 * Cause this link protocol to be loaded at init time by adding
 * org.cougaar.mts.std.email.OutgoingEmailLinkProtocol to this System
 * property defined in your setarguments.bat file. If you don't have such a property, add one. 
 * Multiple protocols are separated by commas.
 * <br>(e.g. -Dorg.cougaar.message.transport.classes=org.cougaar.mts.std.email.OutgoingEmailLinkProtocol,
 * org.cougaar.mts.std.email.IncomingEmailLinkProtocol)
 * <p>
 * <b>org.cougaar.message.protocol.email.outboxes.<node-name></b> 
 * Specify the outbound SMTP servers for a node by setting this property 
 * to a list of URIs delimited by vertical bars, <i>smtp://user:pswd@host:port</i>, where:
 * <pre>
 * smtp:  The literal string "smtp".
 * user:  The user name for access to the smtp server.
 * pswd:  The password for access to the smtp server.
 * host:  The fully qualified domain name (FQDN) of the host running the SMTP mail server.
 * port:  The port the SMTP mail server is listening on (typically port 25).
 * </pre>
 * (e.g. for a node named "X": 
 * -Dorg.cougaar.message.transport.email.outboxes.X=smtp://node1:kjasd@wally.objs.com:25)
 * <br>
 * <p>
 * <b>org.cougaar.message.protocol.email.cost</b>
 * The cost function of the DestinationLink inner subclass defaults to 1500, so 
 * that, using the default MinCostLinkSelectionPolicy, it will be chosen after 
 * OutgoingUDPLinkProtocol, OutgoingSocketLinkProtocol and RMILinkProtocol, and 
 * before NNTPLinkProtocol. 
 * When using AdaptiveLinkSelectionPolicy, cost is
 * one of the factors that are used to select a protocol. To modify the default
 * cost, set the property to an integer 
 * <br>(e.g. org.cougaar.message.protocol.email.cost=750).
 * <p>
 * <b>org.cougaar.message.protocol.email.outgoing.socketTimeout</b>
 * The number of milliseconds to wait before closing an unresponsive SMTP socket.  The default is 10000 milliseconds.
 * <br>(e.g. -Dorg.cougaar.message.protocol.email.incoming.socketTimeout=10000)
 * <p>
 * <b>org.cougaar.message.protocol.email.outgoing.maxMessageSizeKB</b>
 * The upper limit for the size in kilobytes of messages that will be 
 * sent via this protocol.  The default is 10000KB (10MB).
 * Decrease or increase this setting depending on the constraints of the smtp server.
 * <br>(e.g. -org.cougaar.message.protocol.email.outgoing.maxMessageSizeKB=10000)
 * <p>
 * <b>org.cougaar.message.protocol.email.outgoing.embedMessageDigest</b>
 * If true, help insure message integrity by embedding an MD5 hash of the message
 * in the message.  The default is true.
 * <br>(e.g. -org.cougaar.message.protocol.email.outgoing.embedMessageDigest=true)
 * <p>
 * <b>org.cougaar.message.protocol.email.outgoing.showMailServerInteraction</b>
 * If true, detailed interaction with the POP3 Server is logged at the DEBUG level.  The default is false.
 * <br>(e.g. -Dorg.cougaar.message.protocol.email.incoming.showMailServerInteraction=false)
 * */

public class OutgoingEmailLinkProtocol extends OutgoingLinkProtocol
{
  public static final String PROTOCOL_TYPE = "-email";

  private static final int protocolCost;
  //102 private static final boolean useFQDNs;
  private static final int socketTimeout;
  private static final long maxMessageSizeKB;
  private static final boolean embedMessageDigest;
  private static final boolean showMailServerInteraction;

  private static int SID;

  private LoggingService log;
  private Hashtable uriCache;
  private HashMap links;
  private MailBox outboxes[];
  private WhitePagesService wp;

  static
  {
    //  Read external properties

    String s = "org.cougaar.message.protocol.email.cost";  // one way
    protocolCost = Integer.valueOf(System.getProperty(s,"10000")).intValue();  // was 5000

    //102 s = "org.cougaar.message.protocol.email.useFQDNs";
    //102 useFQDNs = Boolean.valueOf(System.getProperty(s,"true")).booleanValue();

    s = "org.cougaar.message.protocol.email.outgoing.socketTimeout";
    socketTimeout = Integer.valueOf(System.getProperty(s,"5000")).intValue();

    s = "org.cougaar.message.protocol.email.outgoing.maxMessageSizeKB";
    maxMessageSizeKB = Integer.valueOf(System.getProperty(s,"10000")).intValue();

    s = "org.cougaar.message.protocol.email.outgoing.embedMessageDigest";
    embedMessageDigest = Boolean.valueOf(System.getProperty(s,"true")).booleanValue();

    s = "org.cougaar.message.protocol.email.outgoing.showMailServerInteraction";
    showMailServerInteraction = Boolean.valueOf(System.getProperty(s,"false")).booleanValue();
  }

  public OutgoingEmailLinkProtocol ()
  {
    uriCache = new Hashtable();
    links = new HashMap();
  }

  public void load ()
  {
    super_load();
    log = loggingService;

    if (log.isInfoEnabled()) log.info ("Creating " + this);

    ServiceBroker sb = getServiceBroker();

    //104B
    wp = (WhitePagesService)sb.getService(this, 
                                          WhitePagesService.class, 
                                          null);

    MailMan.setServiceBroker(sb);
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

  //102 convert to URIs and multiple outboxes
  public MailBox[] parseOutboxes (String outboxesProp)
  {
    if (outboxesProp == null) return null;

    Vector out = new Vector();
 
    // Parse multiple outbox URIs (separated by vertical bars)
    StringTokenizer URIs = new StringTokenizer (outboxesProp, "|");

    while (URIs.hasMoreTokens()) {
      String tkn = null;
      try {
        // SMTP outbox URIs have the following form:
        // smtp://userid:password@smtpServerHostName:port
        // e.g. smtp://node1:passwd@atom.objs.com:25
        
        out.add(new MailBox(new URI(URIs.nextToken())));
              
        /*  //102 may want to add support for FQDNs back in later              
          if (useFQDNs) {
            String hostFQDN = InetAddress.getByName(host).getCanonicalHostName();
            if (log.isInfoEnabled()) log.info ("Using FQDN " +hostFQDN+ " for outbox mailhost " +host);
            host = hostFQDN;
          }
        */

      } catch (Exception e) {
        log.error ("Bad outbox URI: " +tkn+ " (ignored): " +e);
      }
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

  public String toString ()
  {
    return this.getClass().getName();
  }
  
  //104B
  private URI lookupEmailAddress (MessageAddress address) throws NameLookupException
  {
    URI uri = null;   
    String agentName = address.getAddress();             

    //check the WP cache first
    try {
      if (log.isDebugEnabled())
        log.debug("Calling wp.get("+agentName+","+PROTOCOL_TYPE+",-1)");
      AddressEntry ae = wp.get(agentName, PROTOCOL_TYPE, -1); 
      if (log.isDebugEnabled())
        log.debug("ae="+ae);
      if (ae != null) {
        uri = ae.getURI();
        if (uri != null) {
          if (log.isDebugEnabled())
            log.debug("found addr "+uri+" for Agent "+agentName+" in WP cache");
          return uri;
        }
      }
    } catch (Exception e) {
      if (log.isDebugEnabled())
        log.debug("CACHE_ONLY WP lookup of addr for Agent "+agentName+" threw exception", e);
      throw new NameLookupException(e);
    }

    synchronized (uriCache) {

      // get the callback table entry for this agent
      CbTblEntry cbte = (CbTblEntry)uriCache.get(agentName);

      // if none, create one (first lookup)
      if (cbte == null) {
        cbte = new CbTblEntry();
        uriCache.put(agentName,cbte);
      }

      if (log.isDebugEnabled())
        log.debug("cbte="+cbte+",uriCache="+uriCache);

      // check the callback first
      if (cbte.result != null) {
        uri = cbte.result;
        if (log.isDebugEnabled())
          log.debug("found URI="+uri);

      // else, if a callback isn't already pending, start one
      } else if (!cbte.pending) {
        cbte.pending = true;
        if (log.isDebugEnabled())
          log.debug("uriCache="+uriCache);
        AgentIDCallback uriCb = AgentIDCallback.getAgentIDCallback(uriCache, uriCache);
        if (log.isDebugEnabled())
          log.debug("Calling lookupAddressInNameServer("+address+","+PROTOCOL_TYPE+","+uriCb+")");
        getNameSupport().lookupAddressInNameServer(address, PROTOCOL_TYPE, uriCb);
       
      // else, callback is pending, so do nothing
      } 
      return uri;
    }
  }

  /*
   * Decache destination URI.
   */
  public synchronized void decache (MessageAddress addr)
  {
    CbTblEntry cbte = (CbTblEntry)uriCache.get(addr.getAddress());
    if (cbte != null)
      cbte.result = null;
  }

  public boolean addressKnown (MessageAddress address) 
  {
    try 
    {
      return (lookupEmailAddress(address) != null);
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

  public class EmailOutLink implements DestinationLink 
  {
    private MessageAddress destination;
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

    public boolean isValid () 
    {
      return true;
    }

    public Object getRemoteReference ()
    {
      return null;
    }
    
    public boolean retryFailedMessage (AttributedMessage msg, int retryCount)
    {
      return true;
    }
    
    //104B
    private synchronized void decache () 
    {
      OutgoingEmailLinkProtocol.this.decache(destination);
    }

    public synchronized MessageAttributes forwardMessage (AttributedMessage msg) 
      throws NameLookupException, UnregisteredNameException,
             CommFailureException, MisdeliveredMessageException
    {
if (log.isDebugEnabled()) log.debug("Enter forwardMessage("+MessageUtils.toString(msg)+")", new Throwable());
//if (log.isDebugEnabled()) log.debug("raw msg = "+msg.getRawMessage(), new Throwable());


      //  Dump our cached data on message send retries
      if (MessageUtils.getSendTry (msg) > 3) decache();  //104B changed to 3 because of wp callback

      //  Get emailing info for destination
    
      URI uri = lookupEmailAddress(destination);

      if (uri == null)
      {
        String s = "No Email Address found for " + destination + " Will retry.";
        if (log.isInfoEnabled()) log.info(s);
if (log.isDebugEnabled()) log.debug("Exit forwardMessage("+MessageUtils.toString(msg)+")");
//if (log.isDebugEnabled()) log.debug("raw msg = "+msg.getRawMessage());
        throw new UnregisteredNameException(destination);
      }

      //  Send message via email

      boolean success = false;
      Exception save = null;

      try 
      {
        success = sendMessage (msg, uri);
      } 
      catch (Exception e) 
      {
        if (log.isDebugEnabled()) log.debug ("sendMessage: " +stackTraceToString(e));
        save = e;
      }

      //  Dump our cached data on failed sends and throw an exception

      if (success == false)
      {
        decache(); //104B
        Exception e = (save==null ? new Exception ("email sendMessage unsuccessful") : save);
if (log.isDebugEnabled()) log.debug("Exit forwardMessage("+MessageUtils.toString(msg)+")");
//if (log.isDebugEnabled()) log.debug("raw msg = "+msg.getRawMessage());
        throw new CommFailureException (e);
      }

      //  Successful send

      MessageAttributes successfulSend = new SimpleMessageAttributes();
      String status = MessageAttributes.DELIVERY_STATUS_DELIVERED;
      successfulSend.setAttribute (MessageAttributes.DELIVERY_ATTRIBUTE, status);
if (log.isDebugEnabled()) log.debug("Exit forwardMessage("+MessageUtils.toString(msg)+")");
//if (log.isDebugEnabled()) log.debug("raw msg = "+msg.getRawMessage());
      return successfulSend;
    }
   
    private synchronized boolean sendMessage (AttributedMessage msg, URI destAddr) throws Exception
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

        //102 String remotenode = replaceSpaces(destAddr.getNodeID());
        String remoteAgent = replaceSpaces(msg.getTarget().toString());

        MailMessageHeader boxHeader = outbox.getBoxHeader();

        String from = "EmailStream#"+ localnode +"@"+ localhost;
        String replyTo = null, cc = null, bcc = null;

        if (boxHeader != null)
        {
          replyTo = boxHeader.getReplyTo().getMaxAddress();
          cc =      boxHeader.getCc().getMaxAddress();
          bcc =     boxHeader.getBcc().getMaxAddress();
        }

        String to = destAddr.getSchemeSpecificPart(); //102
        String subject = "To: " +remoteAgent+ " Msg: " + MessageUtils.getMessageNumber (msg);

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

    //102B
    public void addMessageAttributes(MessageAttributes attrs) {
      attrs.addValue(MessageAttributes.IS_STREAMING_ATTRIBUTE,
                     Boolean.FALSE);
      attrs.addValue(MessageAttributes.ENCRYPTED_SOCKET_ATTRIBUTE,
                     Boolean.FALSE);
    }

    public void incarnationChanged() {
      decache();
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
