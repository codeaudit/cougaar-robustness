/*
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
 * CHANGE RECORD 
 * 04 Jun 2002: Created. (OBJS)
 */

package org.cougaar.core.mts;

import java.util.Hashtable;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.service.TopologyReaderService;
import org.cougaar.core.service.TopologyEntry;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.util.log.Logging;


public class AgentID implements java.io.Serializable
{
  private static final int callTimeout;
  private static final Hashtable topologyLookupTable = new Hashtable();

  private static ThreadService threadService;
  private static TopologyReaderService topologyReaderService;

  private String nodeName;
  private String agentName;
  private String agentIncarnation;

  static
  {
    //  Read external properties

    String s = "org.cougaar.message.transport.mts.topology.callTimeout";
    callTimeout = Integer.valueOf(System.getProperty(s,"500")).intValue();
  }

  public AgentID (String nodeName, String agentName, String agentIncarnation)
  {
    this.nodeName = nodeName;
    this.agentName = agentName;
    this.agentIncarnation = agentIncarnation;
  }

  public AgentID (TopologyEntry entry)
  {
    this.nodeName = entry.getNode();
    this.agentName = entry.getAgent();
    this.agentIncarnation = "" + entry.getIncarnation();
  }

  public AgentID (AgentID aid)
  {
    this.nodeName = aid.nodeName;
    this.agentName = aid.agentName;
    this.agentIncarnation = aid.agentIncarnation;
  }

  public String getNodeName ()
  {
    return nodeName;
  }

  public void setNodeName (String name)
  {
    nodeName = name;
  }

  public String getAgentName ()
  {
    return agentName;
  }

  public String getAgentIncarnation ()
  {
    return agentIncarnation;
  }

  public long getAgentIncarnationAsLong ()
  {
    try
    {
      return Long.parseLong (agentIncarnation);
    }
    catch (Exception e)
    {
      return -1;
    }
  }

  public String getNumberSequenceKey ()
  {
    return agentName+ "/" +agentIncarnation;
  }

  public static String makeAgentPairID (AgentID fromAgent, AgentID toAgent)
  {
    return fromAgent.getNumberSequenceKey() +"::"+ toAgent.getNumberSequenceKey();
  }

  public String getID ()
  {
    return nodeName+ "/" +agentName+ "/" +agentIncarnation;
  }

  public String getShortID ()
  {
    return nodeName+ "/" +agentName;
  }

  public String toString ()
  {
    return getID();
  }

  public String toShortString ()
  {
    return getShortID();
  }

  public String toAltShortString ()
  {
    return agentName +"@"+ nodeName;
  }

  public static String makeAckingSequenceID (AgentID fromAgent, AgentID toAgent)
  {
    return fromAgent +"::"+ toAgent;
  }

  public static String makeShortSequenceID (AgentID fromAgent, AgentID toAgent)
  {
    return fromAgent.toShortString() +" to "+ toAgent.toShortString();
  }

  public static String makeAltShortSequenceID (AgentID fromAgent, AgentID toAgent)
  {
    return fromAgent.toAltShortString() +" to "+ toAgent.toAltShortString();
  }

  public static TopologyReaderService getTopologyReaderService (Object requestor, ServiceBroker sb)
  {
    if (topologyReaderService != null) return topologyReaderService;
	topologyReaderService = (TopologyReaderService) sb.getService (requestor, TopologyReaderService.class, null);
    return topologyReaderService;
  }

  private static ThreadService getThreadService (Object requestor, ServiceBroker sb)
  {
    if (threadService != null) return threadService;
    threadService = (ThreadService) sb.getService (requestor, ThreadService.class, null);
    return threadService;
  }

  public static AgentID getAgentID (Object requestor, ServiceBroker sb, MessageAddress agent) 
    throws NameLookupException
  {
    return getAgentID (requestor, sb, agent, false);    
  }

  public static AgentID getAgentID (Object requestor, ServiceBroker sb, MessageAddress agent, boolean refreshCache) 
    throws NameLookupException
  {
    if (agent == null) return null;

    //  If we are not to refresh the nameserver cache, try our own local cache first

    if (!refreshCache)
    {
      TopologyEntry entry = (TopologyEntry) getCachedTopologyLookup (agent);
      if (entry != null) return new AgentID (entry);
    }

    //  Make the topology lookup call in another thread

    TopologyReaderService svc = getTopologyReaderService (requestor, sb);
    TopologyLookup topoLookup = new TopologyLookup (svc, agent, refreshCache);
    String name = "TopologyLookup_" +agent;
    Schedulable thread = getThreadService(requestor,sb).getThread (requestor, topoLookup, name);
    thread.start();

    //  Wait till we get the topology lookup or we time out

    final int POLL_TIME = 100;
    long callDeadline = now() + callTimeout;
    TopologyEntry entry = null;
    boolean hadException = false;
    boolean timedOut = false;

Logging.getLogger(AgentID.class).warn ("starting timed topology lookup ("+callTimeout+" ms) for agent " +agent);

    while (true)
    {
      if (topoLookup.isFinished()) 
      {
        entry = topoLookup.getLookup();
        hadException = topoLookup.hadException();
        break;
      }

      try { Thread.sleep (POLL_TIME); } catch (Exception e) {}

      if (now() > callDeadline) 
      {
        timedOut = true;
        break;
      }
    }

    //  If the call failed or timed out, try a value from our cache, else set the cache

    if (hadException || timedOut) 
    {
      entry = (TopologyEntry) getCachedTopologyLookup (agent);
String s = (hadException ? "had exception" : "timed out");
Logging.getLogger(AgentID.class).warn ("timed topology lookup "+s+", using value from cache for agent "+agent+": " +entry);
    }
    else 
    {
Logging.getLogger(AgentID.class).warn ("timed topology lookup completed on time for agent " +agent);
      cacheTopologyLookup (agent, entry);
    }

    if (entry == null)
    {
      Exception e = new Exception ("Topology service blank on agent: " +agent);
      throw new NameLookupException (e);
    }

    return new AgentID (entry);
  }

  private static class TopologyLookup implements Runnable
  {
    private TopologyReaderService topologyService;
    private MessageAddress agent;
    private boolean refreshCache;
    private TopologyEntry entry;
    private Exception exception;
    private boolean callFinished;

    public TopologyLookup (TopologyReaderService svc, MessageAddress agent, boolean refreshCache)
    {
      this.topologyService = svc;
      this.agent = agent;
      this.refreshCache = refreshCache;
    }

    public void run ()
    {
      entry = null;
      exception = null;
      callFinished = false;

      try
      {
        //System.err.println ("timed topology lookup called: agent=" +agent+ " refreshCache=" +refreshCache);

        if (!refreshCache) entry = topologyService.getEntryForAgent    (agent.getAddress());
        else               entry = topologyService.lookupEntryForAgent (agent.getAddress());

        //System.err.println ("timed topology lookup returned");
      }
      catch (Exception e)
      {
Logging.getLogger(AgentID.class).warn ("timed topology lookup exception: " +stackTraceToString(e));
        exception = e;
      }

      callFinished = true;
    }

    public boolean isFinished ()
    {
      return callFinished;
    }

    public boolean hadException ()
    {
      return exception != null;
    }

    public Exception getException ()
    {
      return exception;
    }

    public TopologyEntry getLookup ()
    {
      return entry;
    }
  }

  private static Object getCachedTopologyLookup (MessageAddress agent)
  {
    synchronized (topologyLookupTable)
    {
      String key = agent.toString();
      return topologyLookupTable.get (key);
    }
  }

  private static void cacheTopologyLookup (MessageAddress agent, TopologyEntry entry)
  {
    synchronized (topologyLookupTable)
    {
      String key = agent.toString();
      if (entry != null) topologyLookupTable.put (key, entry);
      else topologyLookupTable.remove (key);
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
}
