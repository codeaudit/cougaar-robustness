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

public class AdaptiveLinkSelectionPolicy extends AbstractLinkSelectionPolicy
{
// HACK - where to house history?
  private static final MessageHistory messageHistory = MessageSendHistoryAspect.messageHistory;
  //public static final MessageHistory messageHistory = new MessageHistory();

  private static boolean doDebug;
  private static boolean showTraffic; 

  private static final boolean useRTTService; 
  private static final int tryOtherLinksInterval;
  private static final int upgradeMetricMultiplier;
  private static final int maxUpgradeTries;

  private static DestinationLink loopbackLink;
  private static DestinationLink blackHoleLink = new BlackHoleDestinationLink();

  private static final Hashtable selectionHistory = new Hashtable();
  private static final Hashtable linkSelectionTable = new Hashtable();
  private static final Hashtable topLinkSteadyStateCountTable = new Hashtable();
  private static final Random random = new Random();

  private final LinkRanker linkRanker = new LinkRanker();

  private RTTService rttService;

  static 
  {
    //  Read external properties

    String s = "org.cougaar.message.transport.policy.adaptive.useRTTService";
    useRTTService = Boolean.valueOf(System.getProperty(s,"true")).booleanValue();

    s = "org.cougaar.message.transport.policy.adaptive.tryOtherLinksInterval";
    tryOtherLinksInterval = Integer.valueOf(System.getProperty(s,"50")).intValue();

    s = "org.cougaar.message.transport.policy.adaptive.upgradeMetricMultiplier";
    upgradeMetricMultiplier = Integer.valueOf(System.getProperty(s,"10")).intValue();

    s = "org.cougaar.message.transport.policy.adaptive.maxUpgradeTries";
    maxUpgradeTries = Integer.valueOf(System.getProperty(s,"2")).intValue();
  } 

  public AdaptiveLinkSelectionPolicy ()
  {}

  public void initialize () 
  {
    super.initialize();

    if (useRTTService)
    {
      rttService = (RTTService) getServiceBroker().getService (this, RTTService.class, null);
    }
  }

  public void load () 
  {
    super.load();
    String sta = "org.cougaar.core.mts.ShowTrafficAspect";
    showTraffic = (getAspectSupport().findAspect(sta) != null);
  }

  private void debug (String s)
  {
    loggingService.debug (s);
  }

  private DestinationLink linkChoice (DestinationLink link, AttributedMessage msg)
  {
    recordLinkSelection (link, msg);
    MessageUtils.setSendProtocolLink (msg, link.getProtocolClass().getName());
    if (showTraffic) showProgress (link);
//debug ("Chose link = "+ link.getProtocolClass().getName());
    return link;
  }

  public synchronized DestinationLink selectLink (Iterator links, AttributedMessage msg, 
         AttributedMessage failedMsg, int retryCount, Exception last)
  {
    doDebug = loggingService.isDebugEnabled();

    if (links == null || msg == null) return null;

    //  Return if the node is not yet ready

    try
    {
      if (getNameSupport() == null) return null;
      if (getRegistry() == null) return null;
    }
    catch (Exception e) 
    {
      return null;
    }

    //  Handle retries

    if (msg != failedMsg && failedMsg != null) 
    {
      //  How am I supposed to know which out of all the attributes 
      //  set by aspects and protocol links to transfer over or not???
      
      if (MessageUtils.hasMessageNumber (failedMsg))
      {
        //  Transfer known persistent attributes

        MessageUtils.setMessageType (msg, MessageUtils.getMessageType (failedMsg));
        MessageNumberingAspect.setMessageNumber (msg, MessageUtils.getMessageNumber (failedMsg));
        MessageUtils.setFromAgent (msg, MessageUtils.getFromAgent (failedMsg)); 
        MessageUtils.setToAgent (msg, MessageUtils.getToAgent (failedMsg)); 
        MessageUtils.setAck (msg, MessageUtils.getAck (failedMsg)); 
        MessageUtils.setMessageSize (msg, MessageUtils.getMessageSize (failedMsg)); 
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
          Class transportClass = link.getProtocolClass();

          if (transportClass == org.cougaar.core.mts.LoopbackLinkProtocol.class)
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

    if (doDebug)
    {
      if (!MessageUtils.isNewMessage(msg)) debug ("Processing " +msgString);
    }

    /**
     **  Special case:  Message has passed its send deadline so we just drop it
     **/

    if (MessageUtils.getSendDeadline (msg) < now())
    {
      if (doDebug) debug ("Dropping msg past its send deadline: " +msgString); 

      //  [Semi-HACK] Declare message as successfully sent so that any incoming
      //  acks for it are ignored.  Maybe put this in a dropped msg table?

      MessageAckingAspect.addSuccessfulSend (msg);

      //  Drop message

      return blackHoleLink;
    }

    //  Normal operation.
    //
    //  First filter the given links into a vector of outgoing links that
    //  are able to send the given message.

    Vector v = new Vector();

    while (links.hasNext()) 
    {
      DestinationLink link = null;

      try
      {
        link = (DestinationLink) links.next(); 
        String protocol = link.getProtocolClass().getName();

        if (protocol.equals ("org.cougaar.core.mts.LoopbackLinkProtocol")) continue;

        if (!isOutgoingTransport (protocol)) continue;

        if (link.cost (msg) == Integer.MAX_VALUE) continue; 

        if (protocol.equals ("org.cougaar.core.mts.udp.OutgoingUDPLinkProtocol"))
        {
          //  Avoid UDP if the message exceeds the datagram size limit

          int msgSize = MessageUtils.getMessageSize (msg);
          if (msgSize >= OutgoingUDPLinkProtocol.MAX_UDP_MSG_SIZE) continue;
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

    //  No able outgoing links

    if (v.size() == 0) return null;

    //  If this message is a resend, we dump its agent to node mapping in case
    //  that is the reason for the resend (ie. we sent the last send to the 
    //  wrong place).

// Near Future:  Need to force the link protocols to drop their
// cached info on the destination as well.

    String targetNode = null;
    Ack ack = MessageUtils.getAck (msg);

    try
    {
      if (ack != null && ack.getSendCount() > 0)
      {
        //  Bypass topological caching to get latest target agent info

// error: cannot just change destination - have to renumber for new sequence

//  NOTE: now we do not renumber as numbers are node-independent.  Need new
//  node info though

// log INFO that msg num/seq is changing from/to

        AgentID toAgent = AgentID.getAgentID (this, getServiceBroker(), targetAgent, true);
        MessageUtils.setToAgent (msg, toAgent);

        targetNode = toAgent.getNodeName();      
      }
      else
      {
        targetNode = MessageUtils.getToAgentNode (msg);

        if (targetNode == null)
        {
          //  Get cached target agent info

// fix as needed like above

          AgentID toAgent = AgentID.getAgentID (this, getServiceBroker(), targetAgent, false);
          MessageUtils.setToAgent (msg, toAgent);
          targetNode = toAgent.getNodeName();
        }
      }
    }
    catch (Exception e)
    {
      if (doDebug) debug ("Unable to get node for agent: " +targetAgent);
    }

    //  Cannot continue past this point without knowing the name 
    //  of the target node for the message.

    if (targetNode == null) return null;  // send will automatically be tried again later

    //  Rank the links based on the chosen metric

    DestinationLink destLinks[] = (DestinationLink[]) v.toArray (new DestinationLink[v.size()]); 
    linkRanker.setMessage (msg);
    Arrays.sort (destLinks, linkRanker);

    DestinationLink topLink = destLinks[0];
//debug ("\ntopLink = " +topLink.getProtocolClass().getName());

    //  HACK!

    if (MessageUtils.isTrafficMaskingMessage (msg))
    {
      //  Link selection for these messages is limited to whatever link was last used
      //  successfully to its target node.
      
      Class linkClass = MessageAckingAspect.getLastSuccessfulLinkUsed (targetNode);
      DestinationLink link = pickLinkByClass (destLinks, linkClass);
      if (link == null) link = topLink;  // last link may not be currently available

      if (doDebug) debug ("Chose link for traffic masking msg: " +msgString);
      return linkChoice (link, msg);
    }

    //  Special Case:  Sending pure acks-acks.  For ack-acks we will just chose
    //  the last link that successfully sent to the target node.

    if (MessageUtils.isPureAckAckMessage (msg))
    {
      //  Link selection for these messages is limited to whatever link was last used
      //  successfully to its target node.
      
      Class linkClass = MessageAckingAspect.getLastSuccessfulLinkUsed (targetNode);
      DestinationLink link = pickLinkByClass (destLinks, linkClass);
      if (link == null) link = topLink;  // last link may not be currently available

      if (doDebug) debug ("Chose link for pure ack-ack msg: " +msgString);
      return linkChoice (link, msg);
    }

    //  Special Case:  Sending pure acks.  For pure ack messages we try all possible 
    //  links until we run out or we determine we no longer need to send the message.

    if (MessageUtils.isPureAckMessage (msg))
    {
      PureAck pureAck = (PureAck) ack;

      //  First, add all the links that have now piggybacked the ack to our used list.
      //  If there are any, then we may not be needing to send a pure ack this time - we may
      //  wait till sometime after the farthest out response to these sends is expected.

      DestinationLink link;
      boolean newLink = false;
      long latestDeadline = 0;

      for (int i=0; i<destLinks.length; i++)
      {
        link = destLinks[i];

        if (pureAck.haveLinkSelection (link)) continue;

        long lastSendTime = MessageAckingAspect.getLastSendTime (link, targetNode);

        if (lastSendTime > pureAck.getAckSendableTime())
        {
          newLink = pureAck.addLinkSelection (link);

          if (newLink)
          {
            //  Ok, here's a link that we'll not be needing to send an ack over because
            //  its already been done for us.  Update the latestDeadline for when we
            //  need to attempt to send this ack over another link.

// TODO - new rtt with acking

            int roundtrip = MessageAckingAspect.getRoundtripTimeForAck (link, pureAck);
            if (roundtrip < 0) roundtrip = MessageAckingAspect.getBestRoundtripTimeForLink (link, targetNode);

            float spacingFactor = MessageAckingAspect.getInterAckSpacingFactor();
            long deadline = lastSendTime + (long)((float)roundtrip * spacingFactor);
            if (deadline > latestDeadline) latestDeadline = deadline;
          }
        }
      }

      if (newLink && latestDeadline > now())
      {
        //  Reschedule the ack and don't send it now

        pureAck.setSendDeadline (latestDeadline);
        MessageAckingAspect.addToPureAckSender ((PureAckMessage)msg);

        if (doDebug) debug ("Rescheduling pure ack msg: " +msgString);
        return blackHoleLink;  // msgs go in, but never come out!
      }

      //  If we get here, it is time to try to send the ack over the highest ranking
      //  link left that has not had the ack already go over it.

      for (int i=0; i<destLinks.length; i++)
      {
        link = destLinks[i];

        if (!pureAck.haveLinkSelection (link)) 
        {
          pureAck.addLinkSelection (link);

          if (doDebug) debug ("Chose link for pure ack msg: " +msgString);
          return linkChoice (link, msg);
        }
      }

      //  Final case: No untried links left - we are done sending pure ack

      if (doDebug) debug ("No need to send pure ack msg: " +msgString);
      return blackHoleLink;
    }

    //  Special Case: Resending of messages (due to no acks received for it).
    //  What's special here is that we avoid (if possible) choosing any link
    //  that the message has already tried and (essentially) failed with.  
    //  When message history and acking are better integrated the need for this 
    //  may go away.

    if (ack != null)
    {
      if (ack.getSendCount() > 0) // right now only resends are regular messages w/ acks
      {
        //  First try the last link used to send a message to this destination

        Class linkClass = MessageAckingAspect.getLastSuccessfulLinkUsed (targetNode);
        DestinationLink link = pickLinkByClass (destLinks, linkClass);
        
        boolean newLink = (link != null ? ack.addLinkSelection (link) : false); 

        if (newLink)
        {
          if (doDebug) debug ("Chose last successful link used for resending msg: " +msgString);
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

        //  If we still don't have a choice at this point, we start over with our
        //  previous first choice.

        if (link == null)
        {
          if (doDebug) debug ("Starting over with first link choice for msg resend");
          link = pickLinkByClass (destLinks, ack.getFirstLinkSelection());
          ack.clearLinkSelections();
        }

        //  In case of unexpected failure in link chosing

        if (link == null)
        {
          if (doDebug) debug ("Unexpected failures, chosing top link");
          link = topLink;
        }

        if (doDebug) debug ("Made link choice for resend msg: " + msgString);
        ack.addLinkSelection (link);
        return linkChoice (link, msg);
      }   
    }

    /**
     *  Choose a link based on its metric, history of sending messages, and policy (tbd). The
     *  algorithm here now is less advanced compared to what will be able to be done with a rich 
     *  message history (incl. sends and receives) and the Cougaar Messaging Policy system.
    **/

    //  Get the last link choice made for the destination node for this message

    SelectionChoice lastChoice = (SelectionChoice) selectionHistory.get (targetNode);

//debug ("last choice = " + lastChoice);
//if (lastChoice != null) debug ("last choice was successful = " + lastChoice.wasSuccessful());

    //  Special cases: First time a message is sent to this destination
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

      //  We've used this link so many times in a row now, it is time
      //  to consider other links that may have improved, or never been tried

//debug ("\n** COUNT= "+count);

      if (count == 0)
      {
//debug ("\n** trying OTHER LINKS");

        //  Look for the highest ranking link left that still has a RTT of 0 (which means
        //  there are not enough samples to establish a RTT for the link/node combo).

        for (int i=0; i<destLinks.length; i++)
        {
          if (rttService.getBestRoundtripTimeForLink (destLinks[i], targetNode) == 0) 
          {
//debug ("\n** chose link with RTT = 0");
            return linkChoice (setReplacementChoice (lastChoice, destLinks[i], msg), msg);
          }
        }

        //  Otherwise, choose the link that was last chosen the longest ago that
        //  can currently be chosen.

        DestinationLink oldestLink = topLink;
        long oldestTime = Long.MAX_VALUE;

        Hashtable linkSelections = getLinkSelections (targetNode);

        for (Enumeration e=linkSelections.elements(); e.hasMoreElements(); )
        {
          LinkSelectionRecord rec = (LinkSelectionRecord) e.nextElement();
          if (!isMemberOf (destLinks, rec.link)) continue;

          if (rec.selectionTime < oldestTime)
          {
            oldestLink = rec.link;
            oldestTime = rec.selectionTime;
          }
        }

//debug ("\n** chose link last chosen longest ago");
        return linkChoice (setReplacementChoice (lastChoice, oldestLink, msg), msg);
      }

      //  Special case: If last choice is already the top link, keep using it

      if (lastChoice.getLink() == topLink)
      {
          return linkChoice (setSteadyStateChoice (lastChoice, topLink, msg), msg);
      }

      //  Decide whether or not to try to upgrade.  An upgrade is by definition
      //  a higher ranking transport than we are currently using.

      int numSends = lastChoice.getNumSteadyStateSends();
      int sendMetric = getMetric (lastChoice.getSteadyStateLink(), msg);
      int topMetric = getMetric (topLink, msg);

      DestinationLink upgrade = null;

      if (numSends*sendMetric > upgradeMetricMultiplier*topMetric)
      {
        //  We've exceeded the metric of N (default 10) sends by the top link.
        //  Time to think about a better route man.

        upgrade = pickUpgradeChoice (lastChoice, destLinks, msg);
      }

      if (upgrade != null)
      {
        return linkChoice (setUpgradeChoice (lastChoice, upgrade, msg), msg);
      }
      else  // not yet time to upgrade or no upgrade available
      {
        // Keep on keeping on with what we got for now, if it is still available

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

    //  Ok, we are still in the game.  We respond based on the category the last
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

      if (doDebug) debug ("Upgrade choice failed: new upgrade= " +upgrade);

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
        else
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
      //  What?  How did we get here?  Make a note of it and move on.

      loggingService.error ("Invalid state reached!");

      DestinationLink replacement = pickReplacementChoice (lastChoice, destLinks, msg);
      return linkChoice (setReplacementChoice (lastChoice, replacement, msg), msg);
    }
  }

//  TODO - need to guarantee the choices returned are members of links[]

  private DestinationLink pickUpgradeChoice (SelectionChoice lastChoice, DestinationLink links[], AttributedMessage msg)
  {
    //  First filter the candidate links to those that are higher ranked than the last choice
    //  and that have not been tried yet this go round.

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
    //  given lastChoice steady state link.

    //  If we find the lastChoice steady state link in our list of tries, that is the
    //  signal that we have made a complete cycle, and it is time to start over again.

    if (lastChoice.hasReplacementBeenTried (lastChoice.getSteadyStateLink()))
    {
      lastChoice.clearReplacementTries();
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

    if (v.size() == 0) return lastChoice.getSteadyStateLink();  // time to do this now
    if (v.size() == 1) return (DestinationLink) v.firstElement();

    //  Ok, at this point we have to choose between 2 or more replacement links.
    //  We could just automatically choose the highest ranked link, or we could look
    //  a little deeper at other factors such as send successes and failures,
    //  or we could make a random selection, round robin, etc.

    //  For now, we'll choose the highest ranked link.  So rank the links we have and 
    //  return the first one.

    DestinationLink replacementLinks[] = (DestinationLink[]) v.toArray (new DestinationLink[v.size()]); 
    linkRanker.setMessage (msg);
    Arrays.sort (replacementLinks, linkRanker);
    return replacementLinks[0];
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

    if (doDebug) debug ("SteadyState selected: " +link.getProtocolClass().getName());

    return link;
  }

  private DestinationLink setUpgradeChoice (SelectionChoice lastChoice, DestinationLink link, AttributedMessage msg)
  {
    SelectionChoice choice = lastChoice;

    int msgNum = MessageUtils.getMessageNumber (msg);
    choice.makeUpgradeChoice (link, msgNum);

    if (doDebug) debug ("Upgrade selected: " +link.getProtocolClass().getName());

    return link;
  }

  private DestinationLink setReplacementChoice (SelectionChoice lastChoice, DestinationLink link, AttributedMessage msg)
  {
    SelectionChoice choice = lastChoice;

    int msgNum = MessageUtils.getMessageNumber (msg);
    choice.makeReplacementChoice (link, msgNum);

    if (doDebug) debug ("Replacement selected: " +link.getProtocolClass().getName());

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
      return (link != null ? link.getProtocolClass().getName() : "null");
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
      int bestRTT = rttService.getBestRoundtripTimeForLink (link, targetNode);
//debug ("bestRTT: " +bestRTT+ " for " +link.getProtocolClass().getName()+ " to " +targetNode);
      if (bestRTT > 0) return bestRTT;
    }

    //  Fallback to old cost method

    return link.cost (msg);
  }

  private static long now ()
  {
    return System.currentTimeMillis();
  }

  private static class Int  // a mutable int
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
    synchronized (linkSelectionTable)
    {
      return (Hashtable) linkSelectionTable.get (targetNode);
    }
  }

  private DestinationLink pickLinkByClass (DestinationLink links[], Class linkClass)
  {
    if (links == null || linkClass == null) return null;
  
    for (int i=0; i<links.length; i++)
    {
      if (links[i].getProtocolClass() == linkClass) return links[i];
    }
  
    return null;
  }

  private boolean isMemberOf (DestinationLink links[], DestinationLink link)
  {
    if (links == null || link == null) return false;
    for (int i=0; i<links.length; i++) if (links[i] == link) return true;
    return false;
  }

  private static String getLinkClassname (DestinationLink link)
  {
    return link.getProtocolClass().getName();
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
    return getTransportID (link.getProtocolClass());
  }

  public static int getTransportID (Class transportClass)
  {
    return transportClass.hashCode();
  }

  public static boolean isOutgoingTransport (String protocol)
  {
    //  A hack, hopefully temporary

         if (protocol.equals ("org.cougaar.core.mts.udp.IncomingUDPLinkProtocol"))       return false;   
    else if (protocol.equals ("org.cougaar.core.mts.socket.IncomingSocketLinkProtocol")) return false;   
    else if (protocol.equals ("org.cougaar.core.mts.email.IncomingEmailLinkProtocol"))   return false;

    return true;
  }

  /**
   *  Prints the second character (after the dash(-)) from  
   *  the static field PROTOCOL_TYPE in the LinkProtocol  
   *  class in which a DestinationLink is embedded as an 
   *  indicator for which protocol was selected. 
  **/

  private void showProgress (DestinationLink link)
  {
    try 
    { 
      String name = link.getProtocolClass().getName();                                                           
      String statusChar;

           if (name.equals("org.cougaar.core.mts.LoopbackLinkProtocol")) statusChar = "l";             
      else if (name.equals("org.cougaar.core.mts.RMILinkProtocol")) statusChar = "r";                  
      else if (name.equals("org.cougaar.core.mts.email.OutgoingEmailLinkProtocol")) statusChar = "e";  
      else if (name.equals("org.cougaar.core.mts.socket.OutgoingSocketLinkProtocol")) statusChar = "s";
      else if (name.equals("org.cougaar.core.mts.udp.OutgoingUDPLinkProtocol")) statusChar = "u";
      else if (name.equals("org.cougaar.core.mts.NNTPLinkProtocol")) statusChar = "n";                 
      else if (name.equals("org.cougaar.core.mts.SSLRMILinkProtocol")) statusChar = "v";               
      else if (name.equals("org.cougaar.core.mts.SerializedRMILinkProtocol")) statusChar = "z";        
      else if (name.equals("org.cougaar.core.mts.FutileSerializingRMILinkProtocol")) statusChar = "f"; 
      else if (name.equals("org.cougaar.lib.quo.CorbaLinkProtocol")) statusChar = "c";                 
      else statusChar = "?";                                                                           

      System.out.print (statusChar);
    } 
    catch (Exception e) { e.printStackTrace(); }
  }

  public static String getLinkType (String classname)
  {
    if (classname == null) return null;

    String type = "unknown";

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
