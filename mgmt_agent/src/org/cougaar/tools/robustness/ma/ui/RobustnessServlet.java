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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.apache.xerces.dom.DocumentImpl;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.core.service.ServletService;
import org.cougaar.core.service.NamingService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.util.PropertyNameValue;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.blackboard.IncrementalSubscription;

import org.cougaar.core.service.community.CommunityMember;
import org.cougaar.community.CommunityChangeNotification;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.SimpleMessageAddress;
import org.cougaar.core.mobility.MoveTicket;
import org.cougaar.core.mobility.RemoveTicket;
import org.cougaar.core.mobility.ldm.AgentControl;
import org.cougaar.core.mobility.ldm.MobilityFactory;
import org.cougaar.tools.robustness.ma.plugins.HealthStatus;
import org.cougaar.core.service.AgentIdentificationService;

import org.cougaar.tools.robustness.ma.ldm.VacateRequest;
import org.cougaar.tools.robustness.ma.ldm.RestartLocationRequest;
import org.cougaar.tools.robustness.ma.ldm.VacateRequestRelay;

import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Application;

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
  //private TopologyReaderService trs;
  private WhitePagesService wps;
  private UIDService uids;
  private MobilityFactory mobilityFactory;
  protected AgentIdentificationService agentIdService;
  protected NodeIdentificationService nodeIdService;
  protected MessageAddress agentId;
  protected MessageAddress localNode;
  //private RootFactory rootFactory;
  private String indexName = "Communities";

  /**
   * Hard-coded servlet path.
   */
  protected String getPath() {
    return "/robustness";
  }

  public void load() {
    // get the logging service
    log =  (LoggingService) serviceBroker.getService(this, LoggingService.class, null);
    uids = (UIDService)serviceBroker.getService(this, UIDService.class, null);
    if (mobilityFactory == null) {
      log.info("Unable to get 'mobility' domain");
    }
    super.load();
  }

  public void setAgentIdentificationService(
      AgentIdentificationService agentIdService) {
    this.agentIdService = agentIdService;
    this.agentId = agentIdService.getMessageAddress();
  }

  public void setNodeIdentificationService(
      NodeIdentificationService nodeIdService) {
    this.nodeIdService = nodeIdService;
    this.localNode = nodeIdService.getMessageAddress();
  }


  public void setBlackboardService(BlackboardService blackboard) {
    this.bb = blackboard;
    //blackboard.setShouldBePersisted(true);
  }

  public void setDomainService(DomainService ds) {
    this.ds = ds;
    mobilityFactory = (MobilityFactory) ds.getFactory("mobility");
    //this.rootFactory = ds.getFactory();
  }

  /**
   * Create the servlet.
   */
  protected Servlet createServlet() {
    ns = (NamingService)serviceBroker.getService(this, NamingService.class, null);
    if (ns == null) {
      throw new RuntimeException("no naming service?!");
    }
   /* trs = (TopologyReaderService)serviceBroker.getService(this, TopologyReaderService.class, null);
    if(trs == null) {
      throw new RuntimeException("no topology reader service.");
    }*/

    wps = (WhitePagesService)serviceBroker.getService(this, WhitePagesService.class, null);
    if(wps == null) {
      throw new RuntimeException("no white pages service.");
    }
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
      Document element = null;
      if(command.equals("checkChange")) //check if the community got ang changes since last checking
      {
        try{
          bb.openTransaction();
          //Collection col = (Collection)bb.query(getCommunityChangeNotificationPred());
          IncrementalSubscription sub = (IncrementalSubscription)bb.subscribe(getCommunityChangeNotificationPred());
          if(sub.getAddedCollection().size() > 0) {
            result = "succeed";
          }
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
        targets.add(SimpleMessageAddress.getSimpleMessageAddress(mgmtAgent));
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
      else if(command.equals("removeAgent")) //kill the agent
      {
        String agentName = (String)vs.get(1);
        String nodeName = (String)vs.get(2);
        removeAgent(agentName, nodeName);
        result = "succeed";
      }
      else if(command.equals("viewXml")) //show health status xml
      {
        String agentName = (String)vs.get(1);
        element = getXmlOfAgent(agentName);
      }
      else if(command.equals("getParentHost"))
      {
        /*result = trs.getParentForChild(TopologyReaderService.HOST,
           TopologyReaderService.NODE, (String)vs.get(1));*/
        String uri = getEntityURI((String)vs.get(1));
        result = uri.substring(7, uri.lastIndexOf("/"));
      }

      ServletOutputStream outs = res.getOutputStream();
      ObjectOutputStream oout = new ObjectOutputStream(outs);
      try{
        if(command.equals("viewXml")) {
          oout.writeObject(element);
        }
        else {
          oout.writeObject(result);
        }
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
        //Hashtable topology = getTopology();
        //totalList.put("Topology", topology);
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
               if(ncp.getClassName().equals("org.cougaar.core.mts.SimpleMessageAddress"))
               {
                  String uri = getEntityURI(entityName);
                  String nodeName = uri.substring(uri.lastIndexOf("/")+1);
                   //String nodeName = trs.getParentForChild(trs.NODE, trs.AGENT, ncp.getName());
                   entityName += "  (" + nodeName + ")";
                   //String hostName = trs.getEntryForAgent(ncp.getName()).getHost();
                  String hostName = uri.substring(7, uri.lastIndexOf("/"));
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
                     if(!agents.contains(ncp.getName())) {
                       agents.add(ncp.getName());
                     }
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
          SimpleMessageAddress.getSimpleMessageAddress(agentName),
          SimpleMessageAddress.getSimpleMessageAddress(sourceNode),
          SimpleMessageAddress.getSimpleMessageAddress(destNode),
          true);
    AgentControl ma = mobilityFactory.createAgentControl(null, SimpleMessageAddress.getSimpleMessageAddress(agentName), ticket);
    try{
      bb.openTransaction();
      bb.publishAdd(ma);
    }finally
    {bb.closeTransaction();}
    log.info("RobustnessServlet:: Move agent " + agentName + " from " + sourceNode + " to " + destNode);
  }

  private void removeAgent(String agentName, String sourceNode)
  {
    Object ticketId = mobilityFactory.createTicketIdentifier();
    RemoveTicket ticket = new RemoveTicket(
          ticketId,
          SimpleMessageAddress.getSimpleMessageAddress(agentName),
          SimpleMessageAddress.getSimpleMessageAddress(sourceNode));
    AgentControl ma = mobilityFactory.createAgentControl(null, SimpleMessageAddress.getSimpleMessageAddress(sourceNode), ticket);
    try{
      bb.openTransaction();
      bb.publishAdd(ma);
    }finally
    {bb.closeTransaction();}
    log.info("RobustnessServlet:: Remove agent " + agentName + " from " + sourceNode);
  }

  private Document getXmlOfAgent(String agentName)
  {
    Document doc = null;
    try{
      bb.openTransaction();
      IncrementalSubscription sub = (IncrementalSubscription)bb.subscribe(getHealthStatusPred(agentName));
      if(sub.size() > 0)
      {
       HealthStatus hs = (HealthStatus)sub.getAddedCollection().iterator().next();
       doc = new DocumentImpl();
       Element element = hs.getXML((Document)doc);
       doc.appendChild(element);
      }
    }finally
    { bb.closeTransaction(); }
    return doc;
  }

 /* private Hashtable getTopology()
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
  }*/

  private String getEntityURI(String entityName){
    String uri = "";
    try{
      AddressEntry entrys[] = wps.get(entityName);
      for(int i=0; i<entrys.length; i++) {
        if(entrys[i].getApplication().toString().equals("topology") && entrys[i].getAddress().toString().startsWith("node:")) {
          uri = entrys[i].getAddress().toString();
          return uri;
        }
      }
    }catch(Exception e){
      log.error("Try to get location from WhitePagesService: " + e);
    }
    return uri;
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

  protected static UnaryPredicate getHealthStatusPred(final String agentName)
  {
     return new UnaryPredicate() {
       public boolean execute(Object o) {
           if(o instanceof HealthStatus) {
             return ((HealthStatus)o).getAgentId().getAddress().equals(agentName);
           }
           return false;
       }
     };
  }
}
