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
 * 03 May 2003: Added dequeueMessages in support UC1. (102B)
 * 25 Sep 2002: Revamped queue adds scheduling. (OBJS)
 * 12 Aug 2002: Reworked to resend autonomously. Renamed from AckWaiter. (OBJS)
 * 08 Jun 2002: Revamped and streamlined for 9.2.x. (OBJS)
 * 23 Apr 2002: Split out from MessageAckingAspect. (OBJS)
 */

package org.cougaar.mts.std.acking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;
import org.cougaar.core.mts.AgentState;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.thread.CougaarThread;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.mts.base.AttributedMessage;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.std.AgentID;
import org.cougaar.mts.std.MessageNumberingService;
import org.cougaar.mts.std.MessageSendHistoryAspect;
import org.cougaar.mts.std.MessageUtils;

/**
 **  This is where sent messages wait for their acks, and get resent
 **  if none show up in time.
 **/

class MessageResender implements Runnable
{
  private static final int RESEND_DELAY_INCREMENT = 1000;

  private MessageAckingAspect aspect;
  private LoggingService log;
  private Vector queue;
  private AttributedMessage messages[];
  private Comparator deadlineSort;
  private long minResendDeadline;
  private MessageNumberingService messageNumberingService = null;
  private String FILTERS_ATTRIBUTE = "Filters";

  public static final String MSGLOG_HOLD = "MsglogHold";

  public MessageResender (MessageAckingAspect aspect) 
  {
    this.aspect = aspect;
    queue = new Vector();
    messages = new AttributedMessage[32];
    deadlineSort = new DeadlineSort();
    minResendDeadline = 0;
  }

  public void add (AttributedMessage msg) 
  {
    //  Sanity checks

    if (!MessageUtils.isAckableMessage (msg)) return;

    //  Possibly add (or remove) the message to (from) the agent state

    if (MessageUtils.isRegularMessage (msg))
    {
      AgentState agentState = aspect.getAgentState (MessageUtils.getOriginatorAgent(msg));

      if (agentState != null)
      {
        boolean acked = MessageAckingAspect.wasSuccessfulSend (msg);
        boolean zeroCount = (MessageUtils.getAck(msg).getSendCount() == 0);
        boolean removed = false;
        boolean added = false;

        if (acked || zeroCount)
        {
          synchronized (agentState)
          {
            Vector v = (Vector) agentState.getAttribute (MessageAckingAspect.SENT_BUT_NOT_ACKED_MSGS);
         
            if (acked)
            {
              if (v != null) 
              {
                v.remove (msg);
                removed = true;
              }
            }
            else if (zeroCount)
            {
              if (v == null) 
              {
                v = new Vector();
                agentState.setAttribute (MessageAckingAspect.SENT_BUT_NOT_ACKED_MSGS, v);
              }

              if (!v.contains (msg)) 
              {
                v.add (msg);
                added = true;
              }
            }
          }
        }

        if (debug() && (removed || added)) 
        {
          String s = (removed? "removed" : "added");
          log.debug ("MessageResender: "+s+" to agentState: " +MessageUtils.toString(msg));
        }
      } 
    }

    //  Add the message to the waiting queue

    Ack ack = MessageUtils.getAck (msg);
    long deadline = ack.getSendTime() + ack.getResendTimeout() + ack.getResendDelay();

    synchronized (queue) 
    {
      queue.add (msg);
      offerNewResendDeadline (deadline);
    }

    if (debug()) log.debug ("MessageResender: added timeout=" +(deadline-now())+ " " +MessageUtils.toString(msg));
  }

  public void remove (AttributedMessage msg) 
  {
    //  Sanity check

    if (!MessageUtils.isAckableMessage (msg)) return;

    //  Possibly remove the message from the agent state

    if (MessageUtils.isRegularMessage (msg))
    {
      AgentState agentState = aspect.getAgentState (MessageUtils.getOriginatorAgent(msg));
      boolean removed = false;

      if (agentState != null)
      {
/*
   For right now only remove the message from the agent state if it has been acked

        if (MessageUtils.getAck(msg).getSendCount()==0 || MessageAckingAspect.wasSuccessfulSend(msg))
*/
        if (MessageAckingAspect.wasSuccessfulSend(msg))
        {
          synchronized (agentState)
          {
            Vector v = (Vector) agentState.getAttribute (MessageAckingAspect.SENT_BUT_NOT_ACKED_MSGS);
         
            if (v != null) 
            {
              v.remove (msg);
              removed = true;
            }
          }
        }
      }

      if (removed && debug()) log.debug ("MessageResender: removed from agentState: " +MessageUtils.toString(msg));
    }

    //  Remove the message from the waiting queue

    synchronized (queue) 
    {
      queue.remove (msg);
      if (queue.size() == 0) offerNewResendDeadline (0);
    }

    if (debug()) log.debug ("MessageResender: removed " +MessageUtils.toString(msg));
  }

  private void offerNewResendDeadline (long deadline)
  {
    if (deadline < 0) return;

    synchronized (queue) 
    {
      if (deadline < minResendDeadline || (minResendDeadline == 0 && deadline > 0))
      {
        minResendDeadline = deadline;
        queue.notify();
      }
    }
  }

  public boolean scheduleImmediateResend (AttributedMessage msg) 
  {
    boolean sched = false;

    synchronized (queue) 
    {
      sched = queue.contains (msg);  // msg must already be on queue
    }

    if (!sched)
    {
      if (debug())
      {
        String s = "Msg not on queue, immediate resend abandoned: ";
        log.debug ("MessageResender: " +s+ MessageUtils.toString(msg));
      }
      
      return false;
    }

    Ack ack = MessageUtils.getAck (msg);
    int highTryCount = ack.getNumberOfLinkChoices() * 2;

    if (ack.getSendTry() > highTryCount) 
    {
      if (debug())
      {
        String s = "Msg has high send try count, immediate resend abandoned: ";
        log.debug ("MessageResender: " +s+ MessageUtils.toString(msg));
      }
      
      ack.addResendDelay (RESEND_DELAY_INCREMENT);
      return false;
    }

    if (debug())
    {
      String s = "Scheduling immediate resend: ";
      log.debug ("MessageResender: " +s+ MessageUtils.toString(msg));
    }
    
    ack.setSendTime (now() - (ack.getResendTimeout() + ack.getResendDelay() + 1));
    ding();

    return true;
  }

  public void ding ()
  {
    offerNewResendDeadline (0);  // cause the waiting queue to review its holdings asap
  }

  private boolean debug ()
  {
    if (log == null) log = aspect.getTheLoggingService();
    return (log != null ? log.isDebugEnabled() : false);
  }

  public void run () 
  {
    while (true)
    {
      String s = "MessageResender: Unexpected exception, restarting thread";

      try
      { 
        try 
        { 
          doRun(); 
        } 
        catch (Exception e) 
        {
          s += ": " + stackTraceToString (e);
          log.error (s);
        }
      }
      catch (Exception e)
      {
        try { e.printStackTrace(); } catch (Exception ex) { /* !! */ }
      }
    }
  }

  private void doRun () 
  {
    int len;

    while (true) 
    {
      //  Wait until we have some new messages or acks or we have timed out to 
      //  re-examine old messages.

      synchronized (queue) 
      {
        while (true)
        {
          //  Check how long to wait before we need to satisfy a resend deadline

          long waitTime = 0;  // 0 = wait till notify (or interrupt)

          if (queue.size() > 0)
          {
            waitTime = minResendDeadline - now();
            if (waitTime <= 0) { minResendDeadline = 0;  break; }
          }

          //  Wait until timeout, notify, or interrupt

          try { queue.wait (waitTime); } catch (Exception e) {}
        }

        messages = (AttributedMessage[]) queue.toArray (messages);  // try array reuse
        len = queue.size();
        if (len > 1) Arrays.sort (messages, 0, len, deadlineSort);

// HACK - sync all the code to the queue so not caught out below with attribute
// changes in the DestinationQueue.
//    }  <- formerly sync ended here

      //  Next we try to match the messages with the acks we've collected so far.
      //  Possible many-to-many relationship between the messages and the acks.

      if (len > 0 && debug()) 
      {
        log.debug ("MessageResender: reviewing queue (" +len+ " msg" +(len==1? ")" : "s)"));
      }

      for (int i=0; i<len; i++)
      {
        AttributedMessage msg = messages[i];
        
        //  Avoid already successful sends (when a message is acked it is declared
        //  and recorded as a successful send).

        if (MessageAckingAspect.wasSuccessfulSend (msg))
        {
          if (debug()) log.debug ("MessageResender: Dropping already successful " +MessageUtils.toString(msg));
          remove (msg);
          messages[i] = null;
          continue;
        }

        //  Search for match of message with current received ack data

        int msgNum = MessageUtils.getMessageNumber (msg);
        AckList ackList = AckList.findFirst (MessageAckingAspect.getReceivedAcks(msg), msgNum);

        if (ackList != null)
        {
          //  We have an ack!!

          if (debug()) log.debug ("MessageResender: Got ack for " +MessageUtils.toString(msg));
          Ack ack = MessageUtils.getAck (msg);
          String toNode = MessageUtils.getToAgentNode (msg);
          MessageAckingAspect.addSuccessfulSend (msg);                
          MessageAckingAspect.updateMessageHistory (ack, ackList);
          MessageAckingAspect.removeAcksToSend (toNode, ack.getSpecificAcks()); // acks now acked
          remove (msg);
          messages[i] = null;
        }
      }
      
      //  For any remaining un-acked messages we need to decide if it is time
      //  to resend the message.

      for (int i=0; i<len; i++) if (messages[i] != null)
      {
        AttributedMessage msg = messages[i];

	// don't send held messages
	if (msg.getAttribute(MSGLOG_HOLD) == Boolean.TRUE) {
	    if (debug()) 
		log.debug("MessageResender: skipping resend of held msg "+MessageUtils.toString(msg));
	    continue;
	}

        Ack ack = MessageUtils.getAck (msg);

        //  See if time to resend message

        int timeout = ack.getResendTimeout() + ack.getResendDelay();
        long resendDeadline = ack.getSendTime() + timeout;
        long timeLeft = resendDeadline - now();

        if (debug()) log.debug ("MessageResender: timeLeft=" +timeLeft+ "  timeout=" +timeout+ 
          "  Msg " +MessageUtils.getMessageNumber(msg)+ ": " +MessageUtils.toShortSequenceID(msg));

        if (timeLeft <= 0)
        {
          //  Time to resend the message.  SendMessage re-inserts the message at the
          //  front of the whole send pipeline: it'll come through the AckFrontend
          //  again on its way out to an outgoing link.

          if (debug()) log.debug ("MessageResender: Resending " +MessageUtils.toString(msg));
          remove (msg);  // remove first to avoid race condition with send
          MessageSendHistoryAspect.registerSendFailure (msg);  // since no ack

          //if (debug()) {
          //    Attributes attrs = msg.cloneAttributes();
	  //    if (attrs instanceof SimpleMessageAttributes) {
	  //        log.debug("MessageResender: Attributes are: ");
	  //        ((SimpleMessageAttributes)attrs).listAttributes();
	  //    } else {
	  //	    log.debug("MessageResender: Attributes are instances of class " + attrs.getClass());
	  //    }
	  //}

	  //1045B
	  // Clone Message Attributes to keep resend from screwing up incomplete prior send
          msg = new AttributedMessage(msg);
          
	  msg.removeAttribute(MessageAttributes.IS_STREAMING_ATTRIBUTE); //B1041
	  msg.removeAttribute(MessageAttributes.ENCRYPTED_SOCKET_ATTRIBUTE); //B1041
	  //B1045 msg.removeAttribute(FILTERS_ATTRIBUTE); //B1041
	  msg.setAttribute(FILTERS_ATTRIBUTE, new ArrayList()); //B1045
	  
          //if (debug()) {
          //    Attributes attrs = msg.cloneAttributes();
	  //    if (attrs instanceof SimpleMessageAttributes) {
	  //        log.debug("MessageResender: Attributes are: ");
	  //	    ((SimpleMessageAttributes)attrs).listAttributes();
	  //    } else {
	  //        log.debug("MessageResender: Attributes are instances of class " + attrs.getClass());
	  //    }
	  //}
          
          aspect.sendMessage (msg);

          //  Start adding delay to resending this message if it keeps coming back around

          int highSendCount = ack.getNumberOfLinkChoices();
          if (ack.getSendCount() > highSendCount) ack.addResendDelay (RESEND_DELAY_INCREMENT);
        }
        else
        {
          //  Since the deadlines are time-ordered no other resends will (thread willing)
          //  occur in this go round, so we can quit if we want to.

          offerNewResendDeadline (resendDeadline);
          if (!debug()) break;  // quit if not showing queue review
        }
      }

// HACK - sync all the code to the queue so not caught out below with attribute
// changes in the DestinationQueue (see above).
    }  // temp sync end here

      Arrays.fill (messages, null);  // release references
    }
  }

  private static class DeadlineSort implements Comparator
  {
    public int compare (Object m1, Object m2)
    {
      if (m1 == null)  // drive nulls to bottom (top is index 0)
      {
        if (m2 == null) return 0;
        else return 1;
      }
      else if (m2 == null) return -1;

      //  Sort on resend deadline (sooner deadlines come first)

      Ack a1 = MessageUtils.getAck ((AttributedMessage) m1);
      Ack a2 = MessageUtils.getAck ((AttributedMessage) m2);

      long d1 = a1.getSendTime() + a1.getResendTimeout() + a1.getResendDelay();
      long d2 = a2.getSendTime() + a2.getResendTimeout() + a2.getResendDelay();

      if (d1 == d2) return 0;
      return (d1 > d2 ? 1 : -1);
    }

    public boolean equals (Object obj)
    {
      return (this == obj);
    }
  }

  private static long now ()
  {
    return System.currentTimeMillis();
  }

  private static String stackTraceToString (Exception e)
  {
    java.io.StringWriter stringWriter = new java.io.StringWriter();
    java.io.PrintWriter printWriter = new java.io.PrintWriter (stringWriter);
    e.printStackTrace (printWriter);
    return stringWriter.getBuffer().toString();
  }

/*
  // used to change AgentID and resequence messages queued for an agent that has since restarted   //102B
  void handleMessagesToRestartedAgent(MessageAddress originator, AgentID toAgent) 
      throws org.cougaar.mts.std.NameLookupException, org.cougaar.core.mts.CommFailureException
  {
    if (debug()) 
      log.debug("MessageResender: enter dequeueMessages("+originator+","+toAgent+")");
    synchronized(queue) {
      AgentState agentState = aspect.getAgentState(originator);
      if (agentState != null) {
        synchronized (agentState) {
          Vector msgs = (Vector)agentState.getAttribute(MessageAckingAspect.SENT_BUT_NOT_ACKED_MSGS);
          if (msgs != null) { 
            // sort them by msg num
            Collections.sort(msgs, MsgNumSort.getInstance());
            Iterator i = msgs.iterator();
            while (i.hasNext()) {
              AttributedMessage msg = (AttributedMessage)i.next();
              if (MessageUtils.getToAgent(msg).equals(toAgent) && 
                  !aspect.hasMessageBeenAcked(msg)) {
		// change AgentID and renumber messages
                MessageAddress agentAddr = MessageUtils.getTargetAgent(msg);
                toAgent = AgentID.getAgentID(this, aspect.getServiceBroker(), agentAddr);
                if (toAgent == null) {
                  String s = "Null toAgent for " +agentAddr;
                  if (log.isErrorEnabled()) log.error(s);
                  throw new NameLookupException(new Exception (s));
                } else {
                  MessageUtils.setToAgent(msg, toAgent);
                }
                if (messageNumberingService == null) {
                  ServiceBroker sb = aspect.getServiceBroker();
                  messageNumberingService = 
                    (MessageNumberingService)sb.getService(this, MessageNumberingService.class, null);
                }
                messageNumberingService.renumberMessage(msg);
                if (debug()) 
                  log.debug("MessageResender: dequeued msg: " +MessageUtils.toString(msg));
              }
            }
            offerNewResendDeadline(0);
          }
        }
      }
    }
    if (debug()) 
      log.debug("MessageResender: exit dequeueMessages("+originator+","+toAgent+")");
  }
*/
  
    // used to change AgentID and resequence messages queued for an agent that has since restarted   //102B
    void handleMessagesToRestartedAgent(AgentID fromAgent, AgentID oldToAgent, AgentID newToAgent) 
	throws NameLookupException, CommFailureException
    {
	if (debug()) 
	    log.debug("MessageResender: enter dequeueMessages("+fromAgent+","+oldToAgent+","+newToAgent+")");
	synchronized(queue) {
	    MessageAddress originator = MessageAddress.getMessageAddress(fromAgent.getAgentName());
	    AgentState agentState = aspect.getAgentState(originator);
	    if (agentState != null) {
		synchronized (agentState) {
		    Vector msgs = (Vector)agentState.getAttribute(MessageAckingAspect.SENT_BUT_NOT_ACKED_MSGS);
		    if (msgs != null) { 
			// sort them by msg num
			Collections.sort(msgs, MsgNumSort.getInstance());
			Iterator i = msgs.iterator();
			while (i.hasNext()) {
			    AttributedMessage msg = (AttributedMessage)i.next();
			    if (msg != null) {
				AgentID toAgent = MessageUtils.getToAgent(msg);
				if (toAgent != null) {
				    if (toAgent.equals(oldToAgent)) {
					if (!aspect.hasMessageBeenAcked(msg)) {
					    // change AgentID and renumber messages
					    MessageUtils.setToAgent(msg, newToAgent);
					    if (messageNumberingService == null) {
						ServiceBroker sb = aspect.getServiceBroker();
						messageNumberingService = 
						    (MessageNumberingService)sb.getService(this, MessageNumberingService.class, null);
					    }
					    messageNumberingService.renumberMessage(msg);
					    if (debug())
						log.debug("MessageResender: dequeued msg: " +MessageUtils.toString(msg));
					}
				    }
				} else {
				    if (log.isInfoEnabled())
					log.info("MessageResender: null toAgent on msg " + 
						  MessageUtils.toString(msg) + 
                                                  " in AgentState for " + originator);
				}
			    } else {
				if (log.isInfoEnabled())
				    log.info("MessageResender: null msg in AgentState for " + originator);
			    }
			}
			offerNewResendDeadline(0);
		    }
		}
	    }
	}
	if (debug()) 
	    log.debug("MessageResender: exit dequeueMessages("+fromAgent+","+oldToAgent+","+newToAgent+")");
    }
  
    //102B
    private static class MsgNumSort implements Comparator {
	private static final MsgNumSort instance = new MsgNumSort();   
	private MsgNumSort () {}
	public static MsgNumSort getInstance () { return instance; }
	public int compare (Object m1, Object m2) {
	    if (m1 == null) { // drive nulls to bottom (top is index 0)
		if (m2 == null) return 0;
		else return 1;
	    } else if (m2 == null) return -1;
	    //  Sort on message number (lower numbers come first)
	    int n1 = MessageUtils.getMessageNumber ((AttributedMessage) m1);
	    int n2 = MessageUtils.getMessageNumber ((AttributedMessage) m2);
	    if (n1 == n2) return 0;
	    return (n1 > n2 ? 1 : -1); }
	public boolean equals (Object obj) {
	    return (this == obj); }
    }

    /*
     * Hold (don't resend) any messages to this address until released.
     */
    public void hold (MessageAddress addr) 
    {
	if (debug()) 
	    log.debug("MessageResender: holding messages to "+addr);
	synchronized (queue) {
	    Iterator iter = queue.iterator();
	    while (iter.hasNext()) {
		AttributedMessage msg = (AttributedMessage)iter.next();
		if (msg != null) {
		    MessageAddress target = msg.getTarget().getPrimary();
		    if (target.equals(addr.getPrimary())) {
			if (debug()) 
			    log.debug("MessageResender: holding msg "+MessageUtils.toString(msg));
			msg.setAttribute(MSGLOG_HOLD,Boolean.TRUE);
		    }
		}
	    }
	}
    }

    /*
     * Release (for possible resend) any messages to this address that are on hold.
     */
    public void release (MessageAddress addr) 
    {
	if (debug()) 
	    log.debug("MessageResender: releasing messages to "+addr);
	synchronized (queue) {
	    Iterator iter = queue.iterator();
	    while (iter.hasNext()) {
		AttributedMessage msg = (AttributedMessage)iter.next();
		if (msg != null) {
		    MessageAddress target = msg.getTarget().getPrimary();
		    if (target.equals(addr.getPrimary())) {
			if (debug()) 
			    log.debug("MessageResender: releasing msg "+MessageUtils.toString(msg));
			msg.removeAttribute(MSGLOG_HOLD);
		    }
		}
	    }
	    offerNewResendDeadline(0);
	}
    }
    
 }
