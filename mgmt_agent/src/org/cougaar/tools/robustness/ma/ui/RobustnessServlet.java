/*
 * <copyright>
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
 */
package org.cougaar.tools.robustness.ma.ui;

import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import javax.naming.*;
import javax.naming.directory.*;
import java.io.*;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.core.servlet.ServletService;
import org.cougaar.core.service.NamingService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.TopologyReaderService;
import org.cougaar.core.service.TopologyEntry;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.util.PropertyNameValue;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.domain.RootFactory;

import org.cougaar.core.service.community.CommunityMember;
import org.cougaar.community.CommunityChangeNotification;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.lib.web.arch.root.GlobalEntry;
import org.cougaar.core.mts.MTImpl;
import org.cougaar.core.node.NodeIdentifier;
import org.cougaar.core.mobility.MoveTicket;
import org.cougaar.core.mobility.ldm.AgentControl;
import org.cougaar.core.mobility.ldm.MobilityFactory;

import org.cougaar.tools.robustness.ma.ldm.VacateRequest;
import org.cougaar.tools.robustness.ma.ldm.RestartLocationRequest;

import org.cougaar.tools.robustness.ma.ldm.VacateRequestRelay;

/**
 * This servlet provides an interface to the ManagementAgent for the
 * RobustnessUI.  This servlet is used to retrieve community information
 * from the name server and to publish vacate requests to ManagementAgents
 * blackboard.
 */
public class RobustnessServlet extends BaseServletComponent implements BlackboardClient
{
  private NamingService ns;
  private BlackboardService bb;
  private DomainService ds;
  private LoggingService log;
  private TopologyReaderService trs;
  private UIDService uids;
  private ClusterIdentifier agentId;
  private MobilityFactory mobilityFactory;
  private RootFactory rootFactory;
  private String indexName = "Communities";

  /**
   * Hard-coded servlet path.
   */
  protected String getPath() {
    return "/robustness";
  }

  public void load() {
    org.cougaar.core.plugin.PluginBindingSite pbs =
      (org.cougaar.core.plugin.PluginBindingSite) bindingSite;
    this.agentId = pbs.getAgentIdentifier();
    uids = (UIDService)serviceBroker.getService(this, UIDService.class, null);
    mobilityFactory = (MobilityFactory) ds.getFactory("mobility");
    if (mobilityFactory == null) {
      System.out.println("Unable to get 'mobility' domain");
    }
    super.load();
  }

  public void setBlackboardService(BlackboardService blackboard) {
    this.bb = blackboard;
    //blackboard.setShouldBePersisted(true);
  }

  public void setDomainService(DomainService ds) {
    this.ds = ds;
    this.rootFactory = ds.getFactory();
  }

  /**
   * Create the servlet.
   */
  protected Servlet createServlet() {
    ns = (NamingService)serviceBroker.getService(this, NamingService.class, null);
    if (ns == null) {
      throw new RuntimeException("no naming service?!");
    }
    trs = (TopologyReaderService)serviceBroker.getService(this, TopologyReaderService.class, null);
    if(trs == null) throw new RuntimeException("no topology reader service.");
    // get the logging service
    log =  (LoggingService) serviceBroker.getService(this, LoggingService.class, null);
    return new MyServlet();
  }

  /**
   * Release the serlvet.
   */
  public void unload() {
    super.unload();
    // release the naming service
    if (ns != null) {
      serviceBroker.releaseService(
        this, ServletService.class, servletService);
      ns = null;
    }
  }

  private class MyServlet extends HttpServlet {
    /**
     * This method is called after user vacates a host. It gets the host name and
     * management agent of the community, then publishes a VacateRequestRelay to
     * the management agent.
     * @param req the servlet request
     * @param res the servlet response
     * @throws IOException
     */
    public void doPut(HttpServletRequest req, HttpServletResponse res) throws IOException
    {
      ServletInputStream in = req.getInputStream();
      ObjectInputStream ois = new ObjectInputStream(in);
      Vector vs = null;
      try{
        vs = (Vector)ois.readObject();
      }catch(ClassNotFoundException e){log.error("RobustnessServlet:: class Vector not found");}
      String command = (String)vs.get(0);
      String result = "unchanged";
      if(command.equals("checkChange")) //check if the community got ang changes since last checking
      {
        try{
          bb.openTransaction();
          //Collection col = (Collection)bb.query(getCommunityChangeNotificationPred());
          IncrementalSubscription sub = (IncrementalSubscription)bb.subscribe(getCommunityChangeNotificationPred());
          if(sub.getAddedCollection().size() > 0)
            result = "succeed";
        }finally
        { bb.closeTransaction(); }
      }
      else if(command.equals("vacateHost")) //send vacate request of given host to management agent
      {
        String host = (String)vs.get(1);
        String mgmtAgent = (String)vs.get(2);
        VacateRequest request = new VacateRequest(VacateRequest.VACATE_HOST);
        request.setHost(host);
        Set targets = new HashSet();
        targets.add(new ClusterIdentifier(mgmtAgent));
        VacateRequestRelay relay = new VacateRequestRelay(uids.nextUID(), agentId, targets, (Object)request, null);
        try{
          bb.openTransaction();
          bb.publishAdd(relay);
        }finally
        { bb.closeTransaction(); }
        result = "succeed";
      }
      else if(command.equals("moveAgent")) //relocate given agent
      {
        String agentName = (String)vs.get(1);
        String sourceNode = (String)vs.get(2);
        String destNode = (String)vs.get(3);
        moveAgent(agentName, sourceNode, destNode);
        result = "succeed";
      }

      ServletOutputStream outs = res.getOutputStream();
      ObjectOutputStream oout = new ObjectOutputStream(outs);
      try{
          oout.writeObject(result);
      }catch(java.util.NoSuchElementException e){log.error(e.getMessage());}
      catch(java.lang.NullPointerException e){log.error(e.getMessage());}
    }

    /**
     * This method gets all information of communities from naming service in
     * blackboard and records the information into a hashtable, then transfer
     * the hashtable to relative UI.
     * @param req servlet request
     * @param res servlet response
     * @throws IOException
     */
    public void doGet(
        HttpServletRequest req,
        HttpServletResponse res) throws IOException {
      Hashtable totalList = new Hashtable();
      ServletOutputStream outs = res.getOutputStream();
      ObjectOutputStream oout = new ObjectOutputStream(outs);
      try{
        InitialDirContext idc = ns.getRootContext();
        Hashtable communities = buildCommunitiesTable(idc, indexName);
        totalList.put("Communities", communities);
        Hashtable topology = getTopology();
        totalList.put("Topology", topology);
        oout.writeObject(totalList);
      }catch(NamingException e){log.error(e.getMessage());}
    }
  }

  /**
     * Get attributes of given parameter from naming service.
     * @param context Directory context
     * @param name The parameter need to search
     * @return the attributes.
     */
    private Attributes getAttributes(DirContext context, String name)
    {
      Attributes attrs = null;
      try{
        attrs = context.getAttributes(name);
      }catch(NamingException e){log.error(e.getMessage());}
      return attrs;
    }

    private Hashtable buildCommunitiesTable(InitialDirContext idc, String index)
    {
      Hashtable list = new Hashtable();
      try{
        DirContext communitiesContext = (DirContext)idc.lookup(index);
        NamingEnumeration enum = communitiesContext.list("");
        while (enum.hasMore()) {
             NameClassPair ncPair = (NameClassPair)enum.next();
             List contents = new ArrayList();
             contents.add(getAttributes(communitiesContext, ncPair.getName())); //attributes of this community
             Hashtable entities = new Hashtable(); //records all entities of this community
             Hashtable hosts = new Hashtable(); //records all hosts of this community
             Hashtable allnodes = new Hashtable(); //records all nodes of this community
             DirContext entityContext = (DirContext)communitiesContext.lookup(ncPair.getName());
             NamingEnumeration entityEnums = entityContext.list("");
             while(entityEnums.hasMore())
             {
               NameClassPair ncp = (NameClassPair)entityEnums.next();
               String entityName = ncp.getName();
               if(ncp.getClassName().equals("org.cougaar.core.agent.ClusterIdentifier"))
               {
                   String nodeName = trs.getParentForChild(trs.NODE, trs.AGENT, ncp.getName());
                   entityName += "  (" + nodeName + ")";
                   String hostName = trs.getEntryForAgent(ncp.getName()).getHost();
                   if(hosts.containsKey(hostName))
                   {
                     Hashtable nodes = (Hashtable)hosts.get(hostName);
                     if(nodes.containsKey(nodeName))
                     {
                       List temp = (List)nodes.get(nodeName);
                       temp.add(ncp.getName());
                     }
                     else
                     {
                       List agents = new ArrayList();
                       agents.add(ncp.getName());
                       nodes.put(nodeName, agents);
                     }
                   }
                   else //build a new entry into the hashtable
                   {
                     Hashtable temp = new Hashtable();
                     List agents = new ArrayList();
                     agents.add(ncp.getName());
                     temp.put(nodeName, agents);
                     hosts.put(hostName, temp);
                   }

                   if(allnodes.containsKey(nodeName))
                   {
                     List agents = (List)allnodes.get(nodeName);
                     if(!agents.contains(ncp.getName()))
                       agents.add(ncp.getName());
                   }
                   else
                   {
                     List agents = new ArrayList();
                     agents.add(ncp.getName());
                     allnodes.put(nodeName, agents);
                   }
               }
               entities.put(entityName, getAttributes(entityContext, ncp.getName()));
             }
             contents.add(entities);
             contents.add(hosts);
             contents.add(allnodes);
             list.put(ncPair.getName(), contents);
        }
      }catch(NamingException e){log.error(e.getMessage());}
      return list;
    }

  /**
   * Publish a move ticket of given agent.
   * @param agentName name of mobility agent
   * @param sourceNode name of the original node of this agent
   * @param destNode name of target node of this agent
   */
  private void moveAgent(String agentName, String sourceNode, String destNode)
  {
    Object ticketId = mobilityFactory.createTicketIdentifier();
    MoveTicket ticket = new MoveTicket(
          ticketId,
          new MessageAddress(agentName),
          new MessageAddress(sourceNode),
          new MessageAddress(destNode),
          true);
    AgentControl ma = mobilityFactory.createAgentControl(null, new MessageAddress(agentName), ticket);
    try{
      bb.openTransaction();
      bb.publishAdd(ma);
    }finally
    {bb.closeTransaction();}
    log.info("RobustnessServlet:: Move agent " + agentName + " from " + sourceNode + " to " + destNode);
  }

  private Hashtable getTopology()
  {
    Hashtable topology = new Hashtable();
    Set hosts = trs.getAll(TopologyReaderService.HOST);
    for(Iterator it = hosts.iterator(); it.hasNext();)
    {
      String host = (String)it.next();
      Hashtable nodes_agents = new Hashtable();
      Set nodes = trs.getChildrenOnParent(TopologyReaderService.NODE, TopologyReaderService.HOST, host);
      for(Iterator nit = nodes.iterator(); nit.hasNext();)
      {
        String node = (String)nit.next();
        Set agents = trs.getChildrenOnParent(TopologyReaderService.AGENT, TopologyReaderService.NODE, node);
        nodes_agents.put(node, agents);
      }
      topology.put(host, nodes_agents);
    }
    return topology;
  }

  // odd BlackboardClient method:
  public String getBlackboardClientName() {
    return toString();
  }

  // odd BlackboardClient method:
  public long currentTimeMillis() {
    throw new UnsupportedOperationException(
        this+" asked for the current time???");
  }

  // unused BlackboardClient method:
  public boolean triggerEvent(Object event) {
    // if we had Subscriptions we'd need to implement this.
    throw new UnsupportedOperationException(
        this+" only supports Blackboard queries, but received "+
        "a \"trigger\" event: "+event);
  }

  protected static UnaryPredicate getCommunityChangeNotificationPred()
  {
     return new UnaryPredicate() {
       public boolean execute(Object o) {
           return (o instanceof CommunityChangeNotification);
       }
     };
  }
}
