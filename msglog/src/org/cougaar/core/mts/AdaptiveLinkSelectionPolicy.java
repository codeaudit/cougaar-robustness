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
 * 30 Oct  2002: Improve link selection in face of send failures. (OBJS)
 * 18 Aug  2002: Various enhancements for Cougaar 9.4.1 (OBJS)
 * 25 Jul  2002: Added code to avoid UDP link if msg size too big. (OBJS)
 * 25 Jul  2002: Added getting RTT from RTT service. (OBJS)
 * 23 Jul  2002: Added linkChoice method. (OBJS)
 * 05 Jul  2002: Convert to 9.4 logging. (OBJS)
 * 05 Jun  2002: Port to Cougaar 9.2.x (OBJS)
 * 18 Apr  2002: Port to Cougaar 9.1.x (OBJS)
 * 21 Mar  2002: Port to Cougaar 9.0.0 (OBJS)
 * 08 Jan  2002: Egregious temporary hack to handle last minute traffic
 *               masking messages.  Add separate ack-ack policy.  (OBJS)
 * 06 Jan  2002: Alter node caching policy to allow selective decache, alter ack
 *               link selection policy slighty to accomodate ack-acks. (OBJS)
 * 15 Dec  2001: Add a policy section for pure ack messages. (OBJS)
 * 13 Dec  2001: Change method of determining last send successful to asking 
 *               the MessageSendHistory aspect directly. (OBJS)
 * 06 Dec  2001: Evolved the policy to be able to use measured messaging performance
 *               as a metric to rank transport links instead of just the old artificial
 *               cost function. (OBJS)
 * 05 Dec  2001: Hacks for missing name server and registry. (OBJS)
 * 04 Dec  2001: Add special policy to deal with msg resending. (OBJS)
 * 29 Nov  2001: Minor changes to integrate with new Acking aspect. (OBJS)
 * 20 Nov  2001: Cougaar 8.6.1 compatibility changes. (OBJS)
 * 05 Nov  2001: Use improved method of random number generation for integers 
 *               in pickUpgradeChoice. (OBJS)
 * 31 Oct  2001: Fix pickNewChoice bug, rename "new" choice to "replacement"
 *               choice. (OBJS)
 * 30 Oct  2001: Add numUpgradeTries property. (OBJS)
 * 29 Oct  2001: Modify the original "classic" adaptive link selection
 *               policy with a completely new and improved selection
 *               algorithm. Added upgradeCostMultiple and maxRetries
 *               properties. (OBJS)
 * 28 Oct  2001: Add printing of "l" for loopback when showing message
 *               traffic. (OBJS)
 * 27 Oct  2001: Add link vector cost = MAX_VALUE filter. (OBJS)
 * 26 Oct  2001: Handle degenerate case in which link selection is
 *               worthless because none of the available links will be
 *               able to send the message. (OBJS)
 * 25 Oct  2001: Changed showProgress output letter to lowercase to
 *               distinguish it from rescind R's. (OBJS)
 * 16 Oct  2001: Add special case for intra-node messages. (OBJS)
 * 27 Sept 2001: Add debug & showTraffic. (OBJS)
 * 26 Sept 2001: Rename: MessageTransport to LinkProtocol. (OBJS)
 * 23 Sept 2001: Port to Cougaar 8.4.1 (OBJS)
 * 16 Sept 2001: Port from Cougaar 8.3.1 to 8.4 (OBJS)
 * 06 Sept 2001: Created. (OBJS)
 */

package org.cougaar.core.mts;

import java.util.*;

import org.cougaar.core.mts.acking.*;
import org.cougaar.core.mts.udp.OutgoingUDPLinkProtocol;
import org.cougaar.core.mts.email.OutgoingEmailLinkProtocol;
import org.cougaar.util.CougaarEvent;
import org.cougaar.util.CougaarEventType;


/**
 * Adaptive link selection policy that uses transport cost and 
 * message send history captured by the MessageSendHistoryAspect
 * to select among Destination Links to be used for transporting
 * a message. 
 * <p>
 * <i>Note: currently, at most one link is chosen at a time.  This 
 * is likely to change.</i>
 * <p>
 * The current algorithm is 
 * <p>
 * System Properties:
 * <p>
 * <b>org.cougaar.message.transport.policy</i>
 * To use this selection policy, set this property to the classname.
 * <br>(i.e. -Dorg.cougaar.message.transport.policy=org.cougaar.core.mts.AdaptiveLinkSelectionPolicy)
 * <p>
 * <b>org.cougaar.message.transport.aspects</b>
 * To use this selection policy, you must also add two aspects, 
 * <i>org.cougaar.core.mts.MessageSendHistoryAspect</i> and <i>org.cougaar.core.mts.MessageNumberAspect</i>, 
 * to this property, adjacent to each other, and in this order.
 * <br>(i.e. -Dorg.cougaar.message.transport.aspects=org.cougaar.core.mts.MessageSendHistoryAspect,
 * org.cougaar.core.mts.MessageNumberAspect)
 * <p>
 * <b>org.cougaar.message.transport.policy.adaptive.showTraffic</b> 
 * If true, the default, prints a single character to System.out
 * indicating which LinkProtocol was used to transport a message
 * <p>
 */

import org.cougaar.core.service.LoggingService;


public class AdaptiveLinkSelectionPolicy extends AbstractLinkSelectionPolicy
{
  private static final MessageHistory messageHistory = MessageSendHistoryAspect.messageHistory;

  private static LoggingService log;
  private static boolean debug;
  private static boolean showTraffic; 

  private static boolean useRTTService; 
  private static final boolean createSocketClosingService;
  private static final int initialNodeTime;
  private static final int tryOtherLinksInterval;
  private static final int minSteadyState;
  private static final int upgradeMetricMultiplier;
  private static final int maxUpgradeTries;

private static int commStartDelaySeconds;

  private static SocketClosingServiceImpl socketClosingService;
  private static DestinationLink loopbackLink;
  private static DestinationLink blackHoleLink = new BlackHoleDestinationLink();

  private static final Hashtable selectionHistory = new Hashtable();
  private static final Hashtable linkSelectionTable = new Hashtable();
  private static final Hashtable topLinkSteadyStateCountTable = new Hashtable();
  private static final Random random = new Random();

  private static boolean firstTime = true;
  private static String thisNode;

private static long startTime = 0;

  private final LinkRanker linkRanker = new LinkRanker();

  private RTTService rttService;

  static 
  {
    //  Read external properties

    String s = "org.cougaar.message.transport.policy.adaptive.useRTTService";
    useRTTService = Boolean.valueOf(System.getProperty(s,"true")).booleanValue();

    s = "org.cougaar.message.transport.policy.adaptive.createSocketClosingService";
    createSocketClosingService = Boolean.valueOf(System.getProperty(s,"true")).booleanValue();

    s = "org.cougaar.message.transport.policy.adaptive.initialNodeTime";
    initialNodeTime = Integer.valueOf(System.getProperty(s,"20000")).intValue();

    s = "org.cougaar.message.transport.policy.adaptive.tryOtherLinksInterval"; // maxSteadyState for topLink
    tryOtherLinksInterval = Integer.valueOf(System.getProperty(s,"50")).intValue();

    s = "org.cougaar.message.transport.policy.adaptive.minSteadyState";
    minSteadyState = Integer.valueOf(System.getProperty(s,"4")).intValue();

    s = "org.cougaar.message.transport.policy.adaptive.upgradeMetricMultiplier";
    upgradeMetricMultiplier = Integer.valueOf(System.getProperty(s,"10")).intValue();

    s = "org.cougaar.message.transport.policy.adaptive.maxUpgradeTries";
    maxUpgradeTries = Integer.valueOf(System.getProperty(s,"2")).intValue();

    s = "org.cougaar.message.transport.policy.adaptive.commStartDelaySeconds";
    commStartDelaySeconds = Integer.valueOf(System.getProperty(s,"0")).intValue();
  } 

  public AdaptiveLinkSelectionPolicy ()
  {}

  public void load () 
  {
    super.load();
    log = loggingService;

    if (useRTTService)
    {
      rttService = (RTTService) getServiceBroker().getService (this, RTTService.class, null);

      if (rttService == null) 
      {
        log.error ("Missing RTTAspect! (useRTTService boolean reset to false)");
        useRTTService = false;
      }
    }

    if (createSocketClosingService)
    {
      synchronized (this)
      {
        if (socketClosingService == null)
        {
          socketClosingService = new SocketClosingServiceImpl (getServiceBroker());
        }
      }
    }

    String sta = "org.cougaar.core.mts.ShowTrafficAspect";
    showTraffic = (getAspectSupport().findAspect(sta) != null);

    thisNode = getRegistry().getIdentifier();

if (commStartDelaySeconds > 0)
{
  //  HACK to attempt to get around nameserver/topology lookup problems
  log.warn ("Comm start delay: Waiting for " +commStartDelaySeconds+ " seconds before allowing any message sends");
  startTime = System.currentTimeMillis();
}
  }

  public String toString ()
  {
    return this.getClass().getName();
  }

  private DestinationLink linkChoice (DestinationLink link, AttributedMessage msg)
  {
    if (debug) log.debug ("Link choice: " +(link != null? getName(link) : "null")+ 
                          " for " +MessageUtils.toString(msg));
    if (link != null)
    {
      MessageUtils.setSendProtocolLink (msg, getName(link));

      Ack ack = MessageUtils.getAck (msg);
      if (ack != null) ack.setSendLink (getName (link));

      // if (showTraffic) showProgress (link);

      //  Shout out some link selection activity for assessment purposes

      if (MessageUtils.isRegularMessage (msg))
      {
        String targetNode = MessageUtils.getToAgentNode (msg);
        DestinationLink lastLink = getLastLinkSelection (targetNode);

        if (lastLink != null)
        {
          String lastLinkName = getName (lastLink);
          String thisLinkName = getName (link);

          if (!lastLinkName.equals (thisLinkName))
          {
            CougaarEvent.postComponentEvent 
            (
              CougaarEventType.STATUS, MessageUtils.getOriginatorAgent(msg).toString(), this.toString(),
              "Switch from " +lastLinkName+ " to " +thisLinkName+ " for messages from " +thisNode+
              " to " +targetNode
            );
          }
        }
      }

      //  Insure newly local msgs (msgs to formerly remote agents that have now moved to our node)
      //  have the correct msg type designation.

      if (link == loopbackLink) MessageUtils.setMessageTypeToLocal (msg);

      //  Remember selected links

      recordLinkSelection (link, msg);
    }

    return link;
  }

  public synchronized DestinationLink selectLink (Iterator links, AttributedMessage msg, 
         AttributedMessage failedMsg, int retryCount, Exception last)
  {
    debug = log.isDebugEnabled();
/*
    if (debug) log.debug ("Entered selectLink: msg= " +MessageUtils.toString(msg)+
      " failedMsg=" +MessageUtils.toString(failedMsg));
*/
if (commStartDelaySeconds > 0)
{
  //  HACK to attempt to get around nameserver/topology lookup problems
  if ((startTime + commStartDelaySeconds*1000) > now()) return null;
}
    //  Return if things not right

    if (links == null || msg == null) return null;

    try
    {
      if (getNameSupport() == null) return null;
      if (getRegistry() == null) return null;
    }
    catch (Exception e) 
    {
      return null;
    }

    //  One time initial RTT initializations

    if (useRTTService && firstTime)
    {
      //  Node time

      rttService.setInitialNodeTime (initialNodeTime);

      //  Comm RTTs

      Vector v = new Vector();

      while (links.hasNext())
      {
        DestinationLink link = (DestinationLink) links.next();
        if (isOutgoingLink(link) && !isLoopbackLink(link)) v.add (link);
      }

      DestinationLink l[] = new DestinationLink[v.size()];
      l = (DestinationLink[]) v.toArray (l); 

      for (int i=0; i<l.length; i++)
      {
        String sendLink = getName (l[i]);
        int sendTime = getInitialOneWayTripTime (l[i]);

        for (int j=0; j<l.length; j++)
        {
          String recvLink = convertOutgoingToIncomingLink (getName(l[j]));
          int recvTime = getInitialOneWayTripTime (l[j]);
          rttService.setInitialCommRTTForLinkPair (sendLink, recvLink, sendTime+recvTime);
        }
      }

      firstTime = false;
      return null;  // used up interator
    }

    //  Handle message attribute transfers in the case of send retries

    if (msg != failedMsg && failedMsg != null) 
    {
      //  Transfer known persistent attributes
      
      if (MessageUtils.hasMessageNumber (failedMsg))
      {
        //  HACK!  How am I supposed to know which out of all the attributes 
        //  set by aspects and protocol links to transfer over or not???

        MessageUtils.setMessageType (msg, MessageUtils.getMessageType (failedMsg));
        MessageNumberingAspect.setMessageNumber (msg, MessageUtils.getMessageNumber (failedMsg));
        MessageUtils.setFromAgent (msg, MessageUtils.getFromAgent (failedMsg)); 
        MessageUtils.setToAgent (msg, MessageUtils.getToAgent (failedMsg)); 
        MessageUtils.setAck (msg, MessageUtils.getAck (failedMsg)); 
        MessageUtils.setSrcMsgNumber (msg, MessageUtils.getSrcMsgNumber (failedMsg)); 
        MessageUtils.setMessageSize (msg, MessageUtils.getMessageSize (failedMsg)); 
        MessageUtils.setSendTimeout (msg, MessageUtils.getSendTimeout (failedMsg)); 
        MessageUtils.setSendDeadline (msg, MessageUtils.getSendDeadline (failedMsg)); 
      }
    }

    //  Handle the special case of local messages  

    MessageAddress targetAgent = MessageUtils.getTargetAgent (msg);
    
    if (getRegistry().isLocalClient (targetAgent))
    {
      if (loopbackLink == null)
      {
        while (links.hasNext()) 
        {
          DestinationLink link = (DestinationLink) links.next(); 

          if (isLoopbackLink (link))
          {
            loopbackLink = link;
            break;
          }
        }
      }

      if (loopbackLink == null)
      {
        loggingService.fatal ("Missing needed loopback transport!!");
      }

      return linkChoice (loopbackLink, msg);      
    }

    //  Get started

    String msgString = MessageUtils.toString (msg);
    if (debug && !MessageUtils.isNewMessage(msg)) log.debug ("Processing " +msgString);

    /**
     **  Special case:  Check on a possible message send deadline.  If the message has 
     **  passed its send deadline we just drop it.
     **/

    if (MessageUtils.haveSendTimeout(msg) && !MessageUtils.haveSendDeadline(msg)) 
    {
      //  Need to set the deadline

      int timeout = MessageUtils.getSendTimeout (msg);
      if (timeout >= 0) MessageUtils.setSendDeadline (msg, now() + timeout);
      else log.warn ("Msg has bad send timeout (" +timeout+ ") (timeout ignored): " +msgString);
    }

    if (MessageUtils.getSendDeadline (msg) < now())  // default deadline is no deadline
    {
      //  [Semi-HACK] Declare message as successfully sent so that any incoming
      //  acks for it are ignored.  Maybe put this in a dropped msg table instead?

      if (debug) log.debug ("Dropping msg past its send deadline: " +msgString); 
      MessageAckingAspect.addSuccessfulSend (msg);
      return blackHoleLink;
    }

    //  Check that the sending agent is a current agent in this node.  If not, it has
    //  moved on, and its outstanding (regular) messages with it, so we just drop all
    //  but pure ack messages.  As with send deadline above, maybe put this in a dropped 
    //  msg table instead of the successful sends table?  

// TODO Could we get into trouble if the agent returns and still it needs to send this message?
// It will be seen as successfully sent here.  Need for dropped msg bucket?  Seems so.  There
// is only a small window for this error to take place, so will punt for the moment.

    if (!getRegistry().isLocalClient(MessageUtils.getOriginatorAgent(msg)))
    {
      if (debug) log.debug ("Msg from no-longer-local agent: " +msgString); 

      if (!MessageUtils.isSomePureAckMessage (msg))
      {
        if (debug) log.debug ("Dropping non-ack msg from no-longer-local agent: " +msgString); 
        MessageAckingAspect.addSuccessfulSend (msg);  // HACK: needs work - see above
        return blackHoleLink;
      }
    }

    //  Lookup up the sending agent of the message in the nameserver.  If it is not registered
    //  we return null and try again later, assuming that it will show up soon.  We cannot number
    //  the message until we have the incarnation number for the agent - and message numbering
    //  occurs after link selection - but here we can cause a wait while down there an exception
    //  will need to be thrown.
    
    AgentID fromAgent = MessageUtils.getFromAgent (msg);

    if (fromAgent == null) 
    {
      MessageAddress origAgent = null;

      try
      {
        origAgent = MessageUtils.getOriginatorAgent (msg);
        fromAgent = AgentID.getAgentID (this, getServiceBroker(), origAgent);
      }
      catch (Exception e) {}

      if (fromAgent == null)
      {
        if (!MessageUtils.isSomePureAckMessage (msg))  // let acks pass, esp. from now moved agents
        {
          if (debug) log.debug ("Postponing link selection: null lookup for originator agent: " +origAgent);
          return linkChoice (null, msg);
        }
      }
      else MessageUtils.setFromAgent (msg, fromAgent);
    }

    //  Get the target node for the message.  If this message is a retry/resend, we try to
    //  get the latest uncached data because the target node may have changed and thus be
    //  the cause of our (real or apparent) send failure.

    Ack ack = MessageUtils.getAck (msg);
    String targetNode = null;

    try
    {
      if (ack != null && ack.getSendTry() > 0)
      {
        //  Retry/resend: bypass topological caching to get latest target agent info

        AgentID toAgent = AgentID.getAgentID (this, getServiceBroker(), targetAgent, true);

log.info ("latest toAgent for " +msgString+ ": " +toAgent);
        
        if (log.isInfoEnabled())
        {
          AgentID oldToAgent = MessageUtils.getToAgent (msg);

          if (oldToAgent != null && (!oldToAgent.equals(toAgent)))
          {
            log.info ("Message re-address: " +MessageUtils.toShortString(msg)+
                      "\n  old destination: " +oldToAgent+
                      "\n  new destination: " +toAgent);
          }
        }

        MessageUtils.setToAgent (msg, toAgent);
        targetNode = toAgent.getNodeName();      
      }
      else
      {
        //  New message: cached info is good enough for now

        targetNode = MessageUtils.getToAgentNode (msg);

        if (targetNode == null)
        {
          //  Get cached target agent info

          AgentID toAgent = AgentID.getAgentID (this, getServiceBroker(), targetAgent, false);
          MessageUtils.setToAgent (msg, toAgent);
          targetNode = toAgent.getNodeName();

log.info ("toAgent for new msg " +msgString+ ": " +toAgent);
        }
      }
    }
    catch (Exception e) 
    {
log.info ("exception getting toAgent for " +msgString+ ": " +e);
    }

    //  Cannot continue without knowing the name of the target node for the message

    if (targetNode == null) 
    {
      if (debug) log.debug ("Postponing link selection: unknown node for target agent: " +targetAgent);
      return linkChoice (null, msg);
    }

    //  Normal operation.
    //
    //  First filter the given links into a vector of outgoing links that are able
    //  to send the given message.
    //
    //  NOTE:  Link selection must insure that the link finally returned is a one 
    //  of these filtered links, as they are the only valid links for this iteration.

    Vector v = new Vector();

    while (links.hasNext()) 
    {
      DestinationLink link = null;
      int cost;

      try
      {
        link = (DestinationLink) links.next(); 

        //  Obvious filters

        if (isLoopbackLink (link)) continue;
        if (!isOutgoingLink (link)) continue;

        //  Cost related filtering

        cost = getLinkCost (link, msg);

        if (cost == Integer.MAX_VALUE) continue; 

        if (cost == 1 && getName(link).equals ("org.cougaar.core.mts.SSLRMILinkProtocol"))
        {
          return linkChoice (link, msg);  //  Temp HACK for UC3
        }

        //  Drop links that cannot handle messages beyond a certain size

        if (getName(link).equals ("org.cougaar.core.mts.udp.OutgoingUDPLinkProtocol"))
        {
          int msgSize = MessageUtils.getMessageSize (msg);
          if (msgSize >= OutgoingUDPLinkProtocol.getMaxMessageSizeInBytes()) continue;
        }
        else if (getName(link).equals ("org.cougaar.core.mts.email.OutgoingEmailLinkProtocol"))
        {
          int msgSize = MessageUtils.getMessageSize (msg);
          if (msgSize >= OutgoingEmailLinkProtocol.getMaxMessageSizeInBytes()) continue;
        }

        //  NOTE: As of 8.6.1 an odd side effect of calling link.cost() above is to do the 
        //  equivalent of a call to canSendMessage(), so we comment it out here for now.
        //  if (!canSendMessage (protocolClass, msg)) continue;
      }
      catch (Exception e)
      {
        e.printStackTrace();
        continue;
      }

      if (link != null) v.add (link);
    }

    if (ack != null) ack.setNumberOfLinkChoices (v.size());

    //  No able outgoing links

    if (v.size() == 0) return linkChoice (null, msg);

    //  Rank the links based on the chosen (via property) metric

    DestinationLink destLinks[] = (DestinationLink[]) v.toArray (new DestinationLink[v.size()]); 
    linkRanker.setMessage (msg);
    Arrays.sort (destLinks, linkRanker);

    DestinationLink topLink = destLinks[0];

    //  Special Case:  Sending pure ack messages.  For pure ack messages we try all possible 
    //  links until we run out or we determine we no longer need to send the pure ack.

    if (MessageUtils.isPureAckMessage (msg))
    {
      PureAck pureAck = (PureAck) ack;
      if (debug) log.debug ("Chosing link for pure ack msg: " +msgString);

      //  First, add all the links that have now piggybacked the ack to our used list.
      //  If there are any, then we may not be needing to send a pure ack this time - we may
      //  wait till sometime after the farthest out response to these sends is expected.

      DestinationLink link;
      long latestDeadline = 0;

      for (int i=0; i<destLinks.length; i++)
      {
        link = destLinks[i];
        if (pureAck.haveLinkSelection (link)) continue;

        long lastSendTime = MessageAckingAspect.getLastSendTime (link, targetNode);

        if (lastSendTime > pureAck.getAckSendableTime())
        {
          boolean newLink = pureAck.addLinkSelection (link);

          if (newLink)
          {
            //  Ok, here's a link that we'll not be needing to send an ack over because
            //  its already been done for us.  Update the latestDeadline for when we
            //  need to attempt to send this ack over another link.

            if (debug) log.debug ("Dropping used " +getName(link)+ " for " +msgString);

            int rtt = rttService.getBestFullRTTForLink (link, targetNode);
            if (rtt <= 0) rtt = getLinkCost (link, msg);

            float spacingFactor = MessageAckingAspect.getInterAckSpacingFactor();
            long deadline = lastSendTime + (long)((float)rtt * spacingFactor);
            if (deadline > latestDeadline) latestDeadline = deadline;
          }
        }
      }

      //  Check if we have exhusted all the links for sending the ack

      if (pureAck.getNumberOfLinkSelections() == destLinks.length)
      {
        if (debug) log.debug ("No untried links left to send pure ack, dropping it: " +msgString);
        return blackHoleLink;
      }

      //  Possibly reschedule the pure ack msg for sending later

      if (latestDeadline > now())
      {
        if (debug) 
        {
          long t = latestDeadline - now();
          log.debug ("Rescheduling pure ack msg with timeout of " +t+ ": " +msgString);
        }

        pureAck.setSendDeadline (latestDeadline);
        MessageAckingAspect.addToPureAckSender ((PureAckMessage)msg);
        return blackHoleLink;
      }

      //  If we get here, it is time to try to send the ack over the highest ranking
      //  link left that has not had the ack already go over it.

      for (int i=0; i<destLinks.length; i++)
      {
        link = destLinks[i];

        if (!pureAck.haveLinkSelection (link)) 
        {
          pureAck.addLinkSelection (link);
          return linkChoice (link, msg);
        }
      }

      //  We shouldn't get here

      log.error ("Error selecting link for pure ack (dropping ack): " +msgString);
      return blackHoleLink;
    }

    //  Special Case:  Sending pure ack-acks.

    if (MessageUtils.isPureAckAckMessage (msg))
    {
      //  Link selection for these messages is limited to whatever link was last used
      //  successfully to its target node (if that link is currently available).
      //  NOTE:  Needs work.

      Class linkClass = MessageAckingAspect.getLastSuccessfulLinkUsed (targetNode);
      DestinationLink link = pickLinkByClass (destLinks, linkClass);
      if (link == null) link = topLink;  // linkClass may not be currently available
      return linkChoice (link, msg);
    }

    //  Special Case:  Sending traffic masking messages.

    if (MessageUtils.isTrafficMaskingMessage (msg))
    {
      //  Link selection for these messages is limited to whatever link was last used
      //  successfully to its target node (if that link is currently available).
      //  This is just preliminary support for traffic masking messages.
     
      if (ack == null || (ack.getSendTry() == 0 && ack.getSendCount() == 0))
      { 
        Class linkClass = MessageAckingAspect.getLastSuccessfulLinkUsed (targetNode);
        DestinationLink link = pickLinkByClass (destLinks, linkClass);
        if (link == null) link = topLink;  // linkClass may not be currently available
        return linkChoice (link, msg);
      }
      else
      {
        //  For now, if there are send failures, just drop masking messages, as
        //  we don't need to be clogging up the pipes with chafe at this time.

        if (debug) log.debug ("Send failure with masking msg, dropping it: " +msgString);
        return blackHoleLink;
      }
    }

/*
was new, now old; should be deleted soon

    //  Special Case:  Message retrying (first send attempt generated an exception)
    //  or resending (message did not get an ack).  What's special here is that we avoid 
    //  (if possible) choosing any link that the message has already tried and actually
    //  or apparently failed with.  HACK!

    if (ack != null && (ack.getSendTry() > 0 || ack.getSendCount() > 0))
    {
      //  First try the last link successfully used to send a message to this destination

      Class linkClass = MessageAckingAspect.getLastSuccessfulLinkUsed (targetNode);
      DestinationLink link = pickLinkByClass (destLinks, linkClass);
      boolean newLink = (link != null ? ack.addLinkSelection (link) : false); 

      if (newLink)
      {
        if (debug) log.debug ("Chose last successful link for retry/resend of " +msgString);
        return linkChoice (link, msg);
      }

      //  Otherwise choose the highest ranking transport link not yet chosen (if any)

      link = null;

      for (int i=0; i<destLinks.length; i++)
      {
        if (!ack.haveLinkSelection (destLinks[i])) 
        {
          link = destLinks[i];
          break;
        }
      }

      //  Tried them all

      if (link == null)
      {
        if (ack.getSendCount() == 0)  // Retry
        {
          //  If we still don't have a choice at this point, we clear all our previous
          //  selections and return null.  This allows the retry to slow down (there
          //  will be an increasing delay with each null), and start the link selection
          //  cycle anew when the message comes back thru again.

          if (debug) log.debug ("Exhausted retry link choices; will cycle thru again after delay");
          ack.clearLinkSelections();
          link = null;
        }
        else  // Resend
        {
          //  Since resends are managed by the acking message resender, including their
          //  timing with any delays, we start the link recycling now and don't induce
          //  any extra delay here like with retries.

          if (debug) log.debug ("Exhausted resend link choices; starting recycle now");
          link = pickLinkByClass (destLinks, ack.getFirstLinkSelection());
          if (link == null) link = topLink;
          ack.clearLinkSelections();
        }
      }

      if (link != null) ack.addLinkSelection (link);
      return linkChoice (link, msg);
    }
*/

    /**
     *  Main Selection Algorithm
     *
     *  Choose a link based on its metric, history of sending messages, and policy (tbd). The
     *  algorithm here now could be extended with more message send & receive history, link
     *  metric data, and Cougaar Policy.
    **/

    //  Get the last link choice made for the destination node for this message

    SelectionChoice lastChoice = (SelectionChoice) selectionHistory.get (targetNode);

//log.debug ("last choice = " + lastChoice);
//if (lastChoice != null) log.debug ("last choice was successful = " + lastChoice.wasSuccessful());

    //  Special Case:  First time a message is sent to this destination
    //  or there is only one link to choose from.  In these cases we always 
    //  choose the top/only link.

    if (lastChoice == null || destLinks.length == 1)
    {
      return linkChoice (setSteadyStateChoice (lastChoice, topLink, msg), msg);
    }

    //  If the last choice was successful, we need to choose between keeping the
    //  same choice, or trying to upgrade (i.e. get a higher ranking transport).  

    if (lastChoice.wasSuccessful())
    {
      //  Preliminary work:  If the last choice was an upgrade or a replacement choice,
      //  then good news!  It worked!  We then transform that choice into our
      //  new steady state case, and go on from there.

      if (lastChoice.wasUpgradeChoice() || lastChoice.wasReplacementChoice())
      {
        lastChoice.transformChoiceToSteadyStateChoice();
      }

      //  Update steady choice link selection count for target node

      int count = getTopLinkSteadyStateCountForNode (targetNode);
      if (++count >= tryOtherLinksInterval) count = 0;
      setTopLinkSteadyStateCountForNode (targetNode, count);

      //  We've used this link so many times in a row now, it may be time
      //  to consider other links that may have improved, or never been tried.

      if (count == 0)
      {
//log.debug ("\n** trying OTHER LINKS");

        //  Look for the highest ranking link left that still is lacking a minimum number of
        //  data points in its average commRTT measurement.

        float MIN_FILL = 0.5f;  // HACK: arbitrary for now

        for (int i=0; i<destLinks.length; i++)
        {
          if (rttService.getHighestCommRTTPercentFilled (destLinks[i], targetNode) < MIN_FILL) 
          {
//log.debug ("\n** chose link still lacking a minimum number of data points in commRTT average");
            return linkChoice (setReplacementChoice (lastChoice, destLinks[i], msg), msg);
          }
        }
//log.debug ("\n** !!all links above the minimum number of data points in commRTT average");

        //  Otherwise, choose the link that was last chosen the longest ago that
        //  is currently available/valid.

        DestinationLink oldestLink = topLink;
        long oldestTime = Long.MAX_VALUE;

        Hashtable linkSelections = getLinkSelections (targetNode);

        for (Enumeration e=linkSelections.elements(); e.hasMoreElements(); )
        {
          LinkSelectionRecord rec = (LinkSelectionRecord) e.nextElement();
          if (!isMemberOf (destLinks, rec.link)) continue;  // link not currently available

          if (rec.selectionTime < oldestTime)
          {
            oldestLink = rec.link;
            oldestTime = rec.selectionTime;
          }
        }

//log.debug ("\n** chose (currenly valid) link last chosen longest ago");
        return linkChoice (setReplacementChoice (lastChoice, oldestLink, msg), msg);
      }

      //  Special Case:  At this point, if last choice is the top link, keep using it

      if (lastChoice.getLink() == topLink)
      {
          return linkChoice (setSteadyStateChoice (lastChoice, topLink, msg), msg);
      }

      //  Decide whether or not to try to upgrade.  An upgrade is by definition
      //  a higher ranking transport than we are currently using.

      DestinationLink upgrade = null;
      int numSends = lastChoice.getNumSteadyStateSends();

      if (numSends >= minSteadyState)
      {
        int sendMetric = getMetric (lastChoice.getSteadyStateLink(), msg);
        int topMetric = getMetric (topLink, msg);

        if (numSends*sendMetric > upgradeMetricMultiplier*topMetric)
        {
          //  We've exceeded the metric of N (default 10) sends by the top link.
          //  Time to think about a better route man.

          upgrade = pickUpgradeChoice (lastChoice, destLinks, msg);
        }
      }

      if (upgrade != null)
      {
        return linkChoice (setUpgradeChoice (lastChoice, upgrade, msg), msg);
      }
      else  // not yet time to upgrade or no upgrade available
      {
        // Keep on keeping on with what we got for now, if it is still valid

        if (isMemberOf (destLinks, lastChoice.getLink()))
        {
          return linkChoice (setSteadyStateChoice (lastChoice, lastChoice.getLink(), msg), msg);
        }
        else // go ahead and jump to the top link
        {
          return linkChoice (setUpgradeChoice (lastChoice, topLink, msg), msg);
        }
      }
    }

    //  The last choice was not successful

    //  We are still in the game.  We respond based on the category the last
    //  choice falls into (upgrade, steady state, or replacement choice).

    if (lastChoice.wasUpgradeChoice())
    {
      //  Ok, so our upgrade failed.  It was worth a shot!  Our 2 choices at this point
      //  are to try another upgrade, or to go back to the tried and true steady state.

      //  For now we will try some number of upgrades before we go back to steady state

      DestinationLink upgrade = null;

      if (lastChoice.getNumUpgradeTries() < maxUpgradeTries)  
      {
        upgrade = pickUpgradeChoice (lastChoice, destLinks, msg);
      }

      if (debug) log.debug ("Upgrade choice failed: new upgrade= " +upgrade);

      if (upgrade != null)
      {
        return linkChoice (setUpgradeChoice (lastChoice, upgrade, msg), msg);
      }
      else  // exceeded # of tries, or no upgrade available
      {
        //  Go back to steady state, if we can

        if (isMemberOf (destLinks, lastChoice.getSteadyStateLink()))
        {
          return linkChoice (setSteadyStateChoice (lastChoice, lastChoice.getSteadyStateLink(), msg), msg);
        }
        else  // surest bet is to get a replacement (an upgrade may not be available)
        {
          DestinationLink replacement = pickReplacementChoice (lastChoice, destLinks, msg);
          return linkChoice (setReplacementChoice (lastChoice, replacement, msg), msg);
        }
      }
    }
    else if (lastChoice.wasSteadyStateChoice())
    {
      //  Uh-oh, our old reliable (at least one message successfully sent!) steady 
      //  state has had anything from a hickup to a coronary.  We basically have 2 choices
      //  here - retry the steady state, or choose a replacement link (that may be higher
      //  or lower ranked, but will be different than the steady state, unless the
      //  steady state is the only link left in town to try).

      //  For now we will always try a replacement choice

      DestinationLink replacement = pickReplacementChoice (lastChoice, destLinks, msg);
      return linkChoice (setReplacementChoice (lastChoice, replacement, msg), msg);
    }
    else if (lastChoice.wasReplacementChoice())
    {
      //  Hmmmm, our replacement choice did not work out.  So we continue to try another
      //  link that we haven't tried lately.  In the case of repeated failures
      //  here, we end up continually cycling through all of the available links.

      DestinationLink replacement = pickReplacementChoice (lastChoice, destLinks, msg);
      return linkChoice (setReplacementChoice (lastChoice, replacement, msg), msg);
    }
    else
    {
      //  What?  How did we get here?  Make a note of it and move on...

      loggingService.error ("Invalid state reached! (but we can deal with it)");

      DestinationLink replacement = pickReplacementChoice (lastChoice, destLinks, msg);
      return linkChoice (setReplacementChoice (lastChoice, replacement, msg), msg);
    }
  }

  private DestinationLink pickUpgradeChoice (SelectionChoice lastChoice, DestinationLink links[], AttributedMessage msg)
  {
    //  First filter the candidate links to those that are higher ranked than the last choice
    //  and that have not been tried yet this go round.

    //  NOTE:  This method must guarantee that the choice it returns is a member of the
    //  given links array (or null), as those links are the only valid ones at the moment.

    Vector v = new Vector();
    int currentMetric = getMetric (lastChoice.getLink(), msg);

    for (int i=0; i<links.length; i++)
    {
      if (getMetric (links[i], msg) >= currentMetric) continue;
      if (lastChoice.hasUpgradeBeenTried (links[i])) continue;
      v.add (links[i]);
    }

    //  Handle special cases

    if (v.size() == 0) return null;
    if (v.size() == 1) return (DestinationLink) v.firstElement();

    //  Ok, at this point we have to choose between 2 or more higher ranking links.
    //  We could just automatically choose the highest ranked link, or we could
    //  look a little deeper at other factors such as send successes and failures,
    //  or we could make a random selection, round robin, etc.

    //  For now, we'll just make a random choice

    int index = random.nextInt (v.size());  // n where 0 <= n < v.size()
    return (DestinationLink) v.elementAt (index);
  }

  private DestinationLink pickReplacementChoice (SelectionChoice lastChoice, DestinationLink links[], AttributedMessage msg)
  {
    //  Pick a replacement choice, meaning a choice that we have not picked before in this go
    //  round, unless we have run out of replacement choices, at which point we return the
    //  given lastChoice steady state link if it is still valid; otherwise we return the
    //  highest ranking link that is not the last choice; otherwise the last choice.

    //  NOTE:  This method must guarantee that the choice it returns is a member of the
    //  given links array (or null), as those links are the only valid ones at the moment.

    if (lastChoice.hasReplacementBeenTried (lastChoice.getSteadyStateLink()))
    {
      //  If we find the lastChoice steady state link in our list of tries, that is the
      //  signal that we have made a complete cycle, and it is time to start over again.
      //  We will return null at this point to cause a delay in trying to send the message
      //  again as it appears its destination is unreachable for now.

      lastChoice.clearReplacementTries();
      if (debug) log.debug ("Cycled thru links, injecting delay before next try"); 
      return null;
    }

    //  Filter the given candidate links to those that have not been tried before and
    //  are not the lastChoice steady state link.

    Vector v = new Vector();

    for (int i=0; i<links.length; i++)
    {
      if (lastChoice.hasReplacementBeenTried (links[i])) continue;
      if (lastChoice.getSteadyStateLink() == links[i]) continue;
      v.add (links[i]);
    }

    //  Handle special cases

    if (v.size() == 0) 
    {
      //  If the last choice steady state link is still valid, return it

      if (isMemberOf (links, lastChoice.getSteadyStateLink()))
      {
        return lastChoice.getSteadyStateLink();
      }
      else lastChoice.clearReplacementTries();  // cycle effectively over

      //  Otherwise, choose the highest ranking link that is not the last choice

      for (int i=0; i<links.length; i++) if (links[i] != lastChoice.getLink()) return links[i];

      //  Ok, no choice, return the last choice

      return lastChoice.getLink();
    }

    if (v.size() == 1) return (DestinationLink) v.firstElement();

    //  Ok, at this point we have to choose between 2 or more replacement links.
    //  We could just automatically choose the highest ranked link, or we could look
    //  a little deeper at other factors such as send successes and failures,
    //  or we could make a random selection, round robin, etc.

    //  For now, we'll choose the highest ranked link that has the most successful
    //  send history.  So re-rank the links we have filtered, find the most successful,
    //  and return it.

    DestinationLink replacementLinks[] = (DestinationLink[]) v.toArray (new DestinationLink[v.size()]); 
    linkRanker.setMessage (msg);
    Arrays.sort (replacementLinks, linkRanker);

    float highestPercent = 0.0f;
    int choice = 0;

    for (int i=0; i<replacementLinks.length; i++)
    {
      int id = getTransportID (replacementLinks[i]);
      float percent = MessageSendHistoryAspect.getPercentSuccessfulSendsByTransportID (id);

      if (percent < 0.01f && !MessageSendHistoryAspect.hasHistory(id)) percent = 1.0f;  // favor unused links

      if (percent > highestPercent)
      {
        highestPercent = percent;
        choice = i;        
      }
    }
    
    return replacementLinks[choice];
  }

  private DestinationLink setSteadyStateChoice (SelectionChoice lastChoice, DestinationLink link, AttributedMessage msg)
  {
    SelectionChoice choice = (lastChoice == null ? new SelectionChoice() : lastChoice);

    int msgNum = MessageUtils.getMessageNumber (msg);
    choice.makeSteadyStateChoice (link, msgNum);
    
    if (lastChoice == null) 
    {
      String targetNode = MessageUtils.getToAgentNode (msg);
      selectionHistory.put (targetNode, choice);
    }

    if (debug) log.debug ("SteadyState selected: " +getName (link));
    return link;
  }

  private DestinationLink setUpgradeChoice (SelectionChoice lastChoice, DestinationLink link, AttributedMessage msg)
  {
    SelectionChoice choice = lastChoice;

    int msgNum = MessageUtils.getMessageNumber (msg);
    choice.makeUpgradeChoice (link, msgNum);

    if (debug) log.debug ("Upgrade selected: " +getName (link));
    return link;
  }

  private DestinationLink setReplacementChoice (SelectionChoice lastChoice, DestinationLink link, AttributedMessage msg)
  {
    if (link == null) return null;

    SelectionChoice choice = lastChoice;

    int msgNum = MessageUtils.getMessageNumber (msg);
    choice.makeReplacementChoice (link, msgNum);

    if (debug) log.debug ("Replacement selected: " +getName (link));
    return link;
  }

  private static class SelectionChoice
  {
    public static final int STEADY_STATE_CHOICE = 0;
    public static final int UPGRADE_CHOICE =      1;
    public static final int REPLACEMENT_CHOICE =  2;

    private int type;
    private DestinationLink link;
    private DestinationLink steadyStateLink;
    private int numSteadyStateSends;
    private Vector upgradeTries;
    private Vector replacementTries;
    private int messageNum;
  
    public SelectionChoice ()
    {}

    public void makeSteadyStateChoice (DestinationLink choice, int msgNum)
    {
      if (link != choice) 
      {
        //  We have a reset of the steady state choice

        numSteadyStateSends = 0;        
      }

      type = STEADY_STATE_CHOICE;
      link = choice;
      steadyStateLink = choice;
      numSteadyStateSends++;
      clearUpgradeTries();
      clearReplacementTries();
      messageNum = msgNum;
    }

    public void makeUpgradeChoice (DestinationLink choice, int msgNum)
    {
      type = UPGRADE_CHOICE;
      link = choice;
      if (upgradeTries == null) upgradeTries = new Vector();
      upgradeTries.add (choice);
      messageNum = msgNum;
    }

    public void makeReplacementChoice (DestinationLink choice, int msgNum)
    {
      type = REPLACEMENT_CHOICE;
      link = choice;
      if (replacementTries == null) replacementTries = new Vector();
      replacementTries.add (choice);
      messageNum = msgNum;
    }

    public void transformChoiceToSteadyStateChoice ()
    {
      type = STEADY_STATE_CHOICE;
      steadyStateLink = link;
      numSteadyStateSends = 1;
      clearUpgradeTries();
      clearReplacementTries();
    }      

    public boolean wasSuccessful ()
    {
      //  We may not know yet whether the last choice sent was successful or not yet.
      //  So instead, we ask whether the last message sent on the transport link used
      //  in the last (this) selection was successful.
 
      return MessageSendHistoryAspect.wasLastSendSuccessful (getTransportID (link));
    }

    public boolean wasUpgradeChoice ()
    {
      return type == UPGRADE_CHOICE;
    }

    public boolean wasSteadyStateChoice ()
    {
      return type == STEADY_STATE_CHOICE;
    }

    public boolean wasReplacementChoice ()
    {
      return type == REPLACEMENT_CHOICE;
    }

    public DestinationLink getLink ()
    {
      return link;
    }

    public DestinationLink getSteadyStateLink ()
    {
      return steadyStateLink;
    }

    public int getNumSteadyStateSends ()
    {
      return numSteadyStateSends;
    }

    public int getNumUpgradeTries ()
    {
      return (upgradeTries != null ? upgradeTries.size() : 0);
    }

    public boolean hasUpgradeBeenTried (DestinationLink link)
    {      
      return (upgradeTries != null ? upgradeTries.contains(link) : false);
    }

    public void clearUpgradeTries ()
    {
      if (upgradeTries != null) upgradeTries.clear();
    }

    public int getNumReplacementTries ()
    {
      return (replacementTries != null ? replacementTries.size() : 0);
    }

    public boolean hasReplacementBeenTried (DestinationLink link)
    {      
      return (replacementTries != null ? replacementTries.contains(link) : false);
    }

    public void clearReplacementTries ()
    {
      if (replacementTries != null) replacementTries.clear();
    }

    public String toString ()
    {
      return (link != null ? getName(link) : "null");
    }
  }

  static class BlackHoleDestinationLink implements DestinationLink 
  {
    private static final MessageAttributes success = new SimpleMessageAttributes();

    static
    {
      success.setAttribute (MessageAttributes.DELIVERY_ATTRIBUTE, MessageAttributes.DELIVERY_STATUS_DELIVERED);
    }

    public MessageAddress getDestination() 
    {
      return null;
    }

    public Class getProtocolClass () 
    {
      return BlackHoleDestinationLink.class;
    }
   
    public int cost (AttributedMessage message) 
    {
      return Integer.MAX_VALUE;
    }

    public Object getRemoteReference ()
    {
      return null;
    }

    public void addMessageAttributes (MessageAttributes attrs)
    {
    }

    public MessageAttributes forwardMessage (AttributedMessage message) 
        throws NameLookupException, UnregisteredNameException,
               CommFailureException, MisdeliveredMessageException
    {
        return success;
    }

    public boolean retryFailedMessage (AttributedMessage message, int retryCount)
    {
      return false;
    }
  } 
  
  private class LinkRanker implements Comparator
  {
    private AttributedMessage message;

    public void setMessage (AttributedMessage msg)
    {
      message = msg;
    }

    public int compare (Object o1, Object o2)
    {
      DestinationLink l1 = (DestinationLink) o1;
      DestinationLink l2 = (DestinationLink) o2;

      int metric1 = getMetric (l1, message);
      int metric2 = getMetric (l2, message);

      if (metric1 == metric2) return 0;
      else return (metric1 > metric2 ? 1 : -1);
    }

    public boolean equals (Object obj)
    {
      return false;  // method not applicable
    }
  }

  private int getMetric (DestinationLink link, AttributedMessage msg)
  {
    //  Get a scalar value to rate using the given link to send the given message 
    //  to its destination.  Smaller is better.

    if (useRTTService)
    {
      String targetNode = MessageUtils.getToAgentNode (msg);
      int bestRTT = rttService.getBestCommRTTForLink (link, targetNode);
//log.debug ("bestRTT: " +bestRTT+ " for " +getName(link)+ " to " +targetNode);
      if (bestRTT > 0) return bestRTT;
    }

    //  Fallback to old cost method

    return getLinkCost (link, msg);
  }

  private static long now ()
  {
    return System.currentTimeMillis();
  }

  private static class Int  // for mutable int objects
  {
    public int value;
    public Int (int v) { value = v; }
  }

  private int getTopLinkSteadyStateCountForNode (String node)
  {
    synchronized (topLinkSteadyStateCountTable)
    {
      Int count = (Int) topLinkSteadyStateCountTable.get (node);
      if (count != null) return count.value;
      return 0;
    }
  }

  private void setTopLinkSteadyStateCountForNode (String node, int newcount)
  {
    synchronized (topLinkSteadyStateCountTable)
    {
      Int count = (Int) topLinkSteadyStateCountTable.get (node);

      if (count == null)
      {
        count = new Int (newcount);
        topLinkSteadyStateCountTable.put (node, count);
      }

      count.value = newcount;
    }
  }

  private static class LinkSelectionRecord
  {
    public DestinationLink link;
    public String targetNode;
    public long selectionTime;

    public LinkSelectionRecord (DestinationLink link, String targetNode, long selectionTime)
    {
      this.link = link;
      this.targetNode = targetNode;
      this.selectionTime = selectionTime;
    }
  }

  private void recordLinkSelection (DestinationLink link, AttributedMessage msg)
  {
    String targetNode = MessageUtils.getToAgentNode (msg);
    recordLinkSelection (link, targetNode);
  }

  private void recordLinkSelection (DestinationLink link, String targetNode)
  {
    if (targetNode == null) return;

    synchronized (linkSelectionTable)
    {
      Hashtable table = (Hashtable) linkSelectionTable.get (targetNode);

      if (table == null)
      {
        table = new Hashtable();
        linkSelectionTable.put (targetNode, table);
      }

      LinkSelectionRecord rec = (LinkSelectionRecord) table.get (link);
      if (rec != null) rec.selectionTime = now();
      else table.put (link, new LinkSelectionRecord (link, targetNode, now()));
    }
  }

  private Hashtable getLinkSelections (String targetNode)
  {
    if (targetNode == null) return null;

    synchronized (linkSelectionTable)
    {
      return (Hashtable) linkSelectionTable.get (targetNode);
    }
  }

  private DestinationLink getLastLinkSelection (String targetNode)
  {
    synchronized (linkSelectionTable)
    {
      Hashtable linkSelections = getLinkSelections (targetNode);
      if (linkSelections == null) return null;

      DestinationLink newestLink = null;
      long newestTime = 0;

      for (Enumeration e=linkSelections.elements(); e.hasMoreElements(); )
      {
        LinkSelectionRecord rec = (LinkSelectionRecord) e.nextElement();

        if (rec.selectionTime > newestTime)
        {
          newestLink = rec.link;
          newestTime = rec.selectionTime;
        }
      }

      return newestLink;
    }
  }

  private DestinationLink pickLinkByClass (DestinationLink links[], Class linkClass)
  {
    if (links == null || linkClass == null) return null;
    for (int i=0; i<links.length; i++) if (links[i].getProtocolClass() == linkClass) return links[i];
    return null;
  }

  //  Utility methods

  public int getLinkCost (DestinationLink link, AttributedMessage msg)
  {
    try { return link.cost (msg); } catch (Exception e) { return Integer.MAX_VALUE; } 
  }

  private boolean isMemberOf (DestinationLink links[], DestinationLink link)
  {
    if (links == null || link == null) return false;
    for (int i=0; i<links.length; i++) if (links[i] == link) return true;
    return false;
  }

  private static String getName (DestinationLink link)
  {
    return link.getProtocolClass().getName();
  }

  private static boolean isLoopbackLink (DestinationLink link)
  {
    return getName(link).equals("org.cougaar.core.mts.LoopbackLinkProtocol");
  }

  public static boolean isOutgoingLink (DestinationLink link)
  {
    //  A hack, based on link naming conventions

    return (getName(link).indexOf(".Incoming") == -1);  // all links but incoming
  }

  private String convertOutgoingToIncomingLink (String link)
  {
    //  A hack, based on link naming conventions

    int i = link.indexOf (".Outgoing");
    if (i > 0) return link.substring(0,i)+ ".Incoming" +link.substring(i+9);
    else return link;
  }

  private int getInitialOneWayTripTime (DestinationLink link)
  {
    if (getName(link).equals("org.cougaar.core.mts.RMILinkProtocol"))         return 5000;   // yep, a HACK
    if (getName(link).equals("org.cougaar.core.mts.SerialedRMILinkProtocol")) return 5000;   // yep, a HACK
    if (getName(link).equals("org.cougaar.core.mts.CorbaLinkProtocol"))       return 5000;   // yep, a HACK
    if (getName(link).equals("org.cougaar.core.mts.SSLRMILinkProtocol"))      return 5000;   // yep, a HACK

    int cost = getLinkCost (link, null);  // another HACK (note null msg)

    if (cost <= 0 || cost == Integer.MAX_VALUE)  // watch for problems
    {
      String s = "Link has unexpected cost (" +cost+ "): " +getName(link);
      log.error (s);
      throw new RuntimeException (s);
    }

    return cost;
  }

  public boolean canSendMessage (Class protocolClass, AttributedMessage msg)
  {
    //  Kind of a hack

    Object obj = null;

    try 
    { 
      String PROTOCOL_TYPE = (String) protocolClass.getField("PROTOCOL_TYPE").get(null); 
      obj = getNameSupport().lookupAddressInNameServer (msg.getTarget(), PROTOCOL_TYPE);
    } 
    catch (Exception e) { e.printStackTrace(); }

    return obj != null;
  }

  public static int getTransportID (DestinationLink link)
  {
    return getTransportID (link.getProtocolClass().getName());
  }

  public static int getTransportID (Class protocolClass)
  {
    return getTransportID (protocolClass.getName());
  }

  public static int getTransportID (String protocolClassname)
  {
    return protocolClassname.hashCode();
  }

  public static String getLinkLetter (DestinationLink link)
  {
    return getLinkLetter (getName (link));
  }

  public static String getLinkLetter (String link)
  {
    if (link == null) link = "";

    String letter;

         if (link.equals("org.cougaar.core.mts.LoopbackLinkProtocol"))              letter = "L";             
    else if (link.equals("org.cougaar.core.mts.RMILinkProtocol"))                   letter = "R";                  
    else if (link.equals("org.cougaar.core.mts.email.OutgoingEmailLinkProtocol"))   letter = "E";  
    else if (link.equals("org.cougaar.core.mts.socket.OutgoingSocketLinkProtocol")) letter = "S";
    else if (link.equals("org.cougaar.core.mts.udp.OutgoingUDPLinkProtocol"))       letter = "U";
    else if (link.equals("org.cougaar.core.mts.NNTPLinkProtocol"))                  letter = "N";                 
    else if (link.equals("org.cougaar.core.mts.SSLRMILinkProtocol"))                letter = "V";               
    else if (link.equals("org.cougaar.core.mts.SerializedRMILinkProtocol"))         letter = "Z";        
    else if (link.equals("org.cougaar.core.mts.FutileSerializingRMILinkProtocol"))  letter = "F"; 
    else if (link.equals("org.cougaar.lib.quo.CorbaLinkProtocol"))                  letter = "C";                 
    else                                                                            letter = "?";

    return letter;
  }

  private void showProgress (DestinationLink link)
  {
    String letter = getLinkLetter (link);
    System.out.print (letter.toLowerCase());
  }

  public static String getLinkType (DestinationLink link)
  {
    return getLinkType (getName (link));
  }

  public static String getLinkType (String classname)
  {
    String type = "unknown";

    if (classname == null) return type;

    if      (classname.equals("org.cougaar.core.mts.LoopbackLinkProtocol")) type = "loopback";
    else if (classname.equals("org.cougaar.core.mts.RMILinkProtocol")) type = "rmi";
    else if (classname.equals("org.cougaar.core.mts.email.OutgoingEmailLinkProtocol")) type = "email";
    else if (classname.equals("org.cougaar.core.mts.email.IncomingEmailLinkProtocol")) type = "email";
    else if (classname.equals("org.cougaar.core.mts.socket.OutgoingSocketLinkProtocol")) type = "socket";
    else if (classname.equals("org.cougaar.core.mts.socket.IncomingSocketLinkProtocol")) type = "socket";
    else if (classname.equals("org.cougaar.core.mts.udp.OutgoingUDPLinkProtocol")) type = "udp";
    else if (classname.equals("org.cougaar.core.mts.udp.IncomingUDPLinkProtocol")) type = "udp";
    else if (classname.equals("org.cougaar.core.mts.NNTPLinkProtocol")) type = "nntp";
    else if (classname.equals("org.cougaar.core.mts.SSLRMILinkProtocol")) type = "rmi-ssl";
    else if (classname.equals("org.cougaar.core.mts.SerializedRMILinkProtocol")) type = "rmi-ser";
    else if (classname.equals("org.cougaar.core.mts.FutileSerializingRMILinkProtocol")) type = "rmi-fser";
    else if (classname.equals("org.cougaar.lib.quo.CorbaLinkProtocol")) type = "corba";

    return type;
  }
}
