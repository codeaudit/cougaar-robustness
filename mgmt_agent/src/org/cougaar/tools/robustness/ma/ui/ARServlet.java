package org.cougaar.tools.robustness.ma.ui;

import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import javax.naming.*;
import javax.naming.directory.*;
import java.io.*;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.core.service.ServletService;
import org.cougaar.core.servlet.ServletUtil;
import org.cougaar.core.service.NamingService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.BlackboardService;
//import org.cougaar.core.service.TopologyReaderService;
//import org.cougaar.core.service.TopologyEntry;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.PropertyNameValue;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.tools.robustness.ma.plugins.HealthStatus;
import org.cougaar.util.ConfigFinder;

import javax.xml.transform.*;
import javax.xml.transform.stream.*;

import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Application;

/**
 * This servlet provides community information.
 */
public class ARServlet extends BaseServletComponent implements BlackboardClient{
  private NamingService ns;
  private BlackboardService bb;
  private DomainService ds;
  private LoggingService log;
  //private TopologyReaderService trs;
  private WhitePagesService wps;
  //private MessageAddress agentId;
  private String indexName = "Communities";

  /**
   * Hard-coded servlet path.
   */
  protected String getPath() {
    return "/ar";
  }

  public void load() {
    // get the logging service
    log =  (LoggingService) serviceBroker.getService(this, LoggingService.class, null);
    //org.cougaar.planning.plugin.legacy.PluginBindingSite pbs =
      //(org.cougaar.planning.plugin.legacy.PluginBindingSite) bindingSite;
    //this.agentId = pbs.getAgentIdentifier();
    super.load();
  }

  public void setBlackboardService(BlackboardService blackboard) {
    this.bb = blackboard;
  }

  public void setDomainService(DomainService ds) {
    this.ds = ds;
  }

  /**
   * Create the servlet.
   */
  protected Servlet createServlet() {
    ns = (NamingService)serviceBroker.getService(this, NamingService.class, null);
    if (ns == null) {
      throw new RuntimeException("no naming service?!");
    }
    /*trs = (TopologyReaderService)serviceBroker.getService(this, TopologyReaderService.class, null);
    if(trs == null) {
      throw new RuntimeException("no topology reader service.");
    }*/
    wps = (WhitePagesService)serviceBroker.getService(this, WhitePagesService.class, null);
    if(wps == null) {
      throw new RuntimeException("no WhitePagesService.");
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
    public void doPut(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException
    {
      Worker worker = new Worker();
      worker.execute(req, res);
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException
    {
      Worker worker = new Worker();
      worker.execute(req, res);
    }
  }

  private String showcommunity = "", showAgent = "", currentCommunityXML = "", showNode="";
  private class Worker
  {
    private HttpServletRequest request;
    private HttpServletResponse response;
    private PrintWriter out;
    private String action = "", format = "";
    private Hashtable totalList = new Hashtable();

    public void execute(HttpServletRequest req, HttpServletResponse res)
      throws IOException, ServletException
    {
      this.request = req;
      this.response = res;
      out = response.getWriter();
      try{
        InitialDirContext idc = ns.getRootContext();
        Hashtable communities = buildCommunitiesTable(idc, indexName);
        totalList.put("Communities", communities);
      }catch(NamingException e){log.error(e.getMessage());}
      parseParams();
    }

    private void parseParams() throws IOException
    {
      // create a URL parameter visitor
      ServletUtil.ParamVisitor vis =
        new ServletUtil.ParamVisitor() {
          public void setParam(String name, String value) {
            if(name.equalsIgnoreCase("list"))
              action = "list";
            if(name.equalsIgnoreCase("data"))
              action = "data";
            if(name.equalsIgnoreCase("communities"))
                showcommunity = value;
            if(name.equalsIgnoreCase("format"))
              format = value;
            if(name.equals("community"))
            {
              action = "community";
              showcommunity = value;
            }
            if(name.equals("agentAttributes"))
            {
              action = "agentAttributes";
              showAgent = value;
            }
            if(name.equals("nodeAttributes"))
            {
              action = "nodeAttributes";
              showNode = value;
            }
            if(name.equals("showcommunity"))
            {
              action = "showCommunity";
              showcommunity = value;
            }
            if(name.equals("listNode"))
              action = "listNode";
            if(name.equals("listState"))
              action = "listState";
            if(name.equals("listStatus"))
              action = "listStatus";
          }
        };
      // visit the URL parameters
      ServletUtil.parseParams(vis, request);
      displayParams(action);
    }

    private void displayParams(String command) throws IOException
    {
      if(command.equals(""))
        showCoverPage();
      else if(command.equals("list"))
        showAllCommunities(format);
      else if(command.equals("data"))
        showCommunityData(format, showcommunity);
      else if(command.equals("community"))
        showCommunityAttributes(format, showcommunity);
      else if(command.equals("agentAttributes"))
        showAgentAttributes(format, showAgent);
      else if(command.equals("nodeAttributes"))
        showNodeAttributes(format, showNode);
      else if(command.equals("showCommunity"))
        showCommunityData(format, showcommunity);
      else if(command.equals("listNode"))
        showNodeData(format);
      else if(command.equals("listState"))
        showStateData(format);
      else if(command.equals("listStatus"))
        showStatusData(format);
    }

    /**
     * This page is shown when no any parameters in the command line. It lets user
     * select an action.
     */
    private void showCoverPage()
    {
      out.print("<html><body>\n<p>\n<br><br><br>\n");
      out.print("<center>\n");
      out.print("<form method=\"GET\" action=\"" + request.getRequestURI() + "\">\n");
      out.print("<input type=\"submit\" name=\"list\" value=\"list communities\">\n<br>\n");
      out.print("<input type=\"submit\" name=\"data\" value=\"show data\">\n");
      out.print("<select name=\"communities\" onchange=\"submit()\">\n");
      Hashtable list = (Hashtable)totalList.get("Communities");
      for(Enumeration enums = list.keys(); enums.hasMoreElements();)
      {
        out.print("<option>");
        out.print((String)enums.nextElement());
        out.print("</option>\n");
      }
      out.print("</select><br>\n");
      out.print("</center></form>");
      out.print("</p>\n</body></html>");
    }

    /**
     * List names of all communities in the homepage. Every name is a link to show
     * the detail of this community.
     * @param format show raw xml data or html? default is html.
     */
    private void showAllCommunities(String format)
    {
      List links = new ArrayList();
      StringBuffer xmlsb = new StringBuffer();
      xmlsb.append(xmlTitle);
      xmlsb.append("<communities>\n");
      Hashtable list = (Hashtable)totalList.get("Communities");
      for(Enumeration enums = list.keys(); enums.hasMoreElements();)
      {
        String community = (String)enums.nextElement();
        xmlsb.append("  <community name=\"" + community + "\"/>\n");
        links.add("/$Manager/ar?data=show+data&communities=" + community + "&format=xml");
      }
      xmlsb.append("</communities>\n");
      String xml = xmlsb.toString();
      if(format.equals("xml"))
        showXML(xml);
      else
      {
        String html = getHTMLFromXML(xml, "communities.xsl");
        out.print(html);
      }
    }

    /**
     * Show all agents, their parent nodes and health status in a table.
     * @param format show raw xml or html?
     * @param community the community to shown.
     */
    private void showCommunityData(String format, String community)
    {
      String xml = getXMLOfCommunity(totalList, community);
      currentCommunityXML = xml;

      if(format.equals("xml"))
        showXML(xml);
      else
      {
        String html = getHTMLFromXML(xml, "community.xsl");
        out.print(html);
      }
    }

    private void showCommunityAttributes(String format, String community)
    {
      //String xml = getXMLOfCommunity(totalList, community);
      String xml = currentCommunityXML;
      if(format.equals("xml"))
        showXML(xml);
      else
      {
        String html = getHTMLFromXML(xml, "communityAttributes.xsl");
        out.print(html);
      }
    }

    private void showAgentAttributes(String format, String agent)
    {
      String xml = getXMLOfCommunity(totalList, showcommunity);
      //String xml = currentCommunityXML;
      if(format.equals("xml"))
        showXML(xml);
      else
      {
        int index = xml.indexOf("<agent name=\"" + agent + "\">");
        int end = xml.indexOf("</agent>", index);
        String str = xml.substring(index, end + 8);
        String html = getHTMLFromXML(str, "agentAttributes.xsl");
        out.print(html);
      }
    }

    private void showNodeAttributes(String format, String node)
    {
      String xml = currentCommunityXML;
      if(format.equals("xml"))
        showXML(xml);
      else
      {
        int index = xml.indexOf("<node name=\"" + node + "\">");
        int end = xml.indexOf("</node>", index);
        String str = xml.substring(index, end + 7);
        String html = getHTMLFromXML(str, "nodeAttributes.xsl");
        out.print(html);
      }
    }

    private void showNodeData(String format)
    {
      String xml = currentCommunityXML;
      if(format.equals("xml"))
        showXML(xml);
      else
      {
        String html = getHTMLFromXML(xml, "nodes.xsl");
        out.print(html);
      }
    }

    private void showStateData(String format)
    {
      String xml = getXMLOfCommunity(totalList, showcommunity);
      if(format.equals("xml"))
        showXML(xml);
      else
        out.print(getHTMLFromXML(xml, "states.xsl"));
    }

    private void showStatusData(String format)
    {
      String xml = getXMLOfCommunity(totalList, showcommunity);
      if(format.equals("xml"))
        showXML(xml);
      else
        out.print(getHTMLFromXML(xml, "statuses.xsl"));
    }

    private void showXML(String xml)
    {
      out.print("<html><body>\n<p><br>\n");
      out.print(convertSignals(xml));
      out.print("</p>\n</body></html>\n");
    }

  }

  /**
   * To show raw xml in a html page, convert several specific signals.
   * @param xml the given xml string
   * @return converted xml
   */
  private String convertSignals(String xml)
  {
    String tmp1 = xml.replaceAll("<", "&lt;");
    String tmp2 = tmp1.replaceAll(">", "&gt;");
    String tmp3 = tmp2.replaceAll("\n", "<br>");
    String tmp4 = tmp3.replaceAll(" ", "&nbsp;");
    return tmp4;
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

    /**
     * Fetch community information from name service in blackboard and save all
     * information into a hash table.
     */
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
   * Compare properties of one agent with properties of it's parent community, save
   * all unduplicate properties of this agent into a hash table.
   * @param agentName agent be compared
   * @param communityProperties a hash table with properties of parent community
   * @return a hash table
   */
  private Hashtable getUnduplicatePropertiesOfAgent(String agentName, Hashtable communityProperties)
  {
    Hashtable properties = new Hashtable();
    try{
      bb.openTransaction();
      IncrementalSubscription sub = (IncrementalSubscription)bb.subscribe(getHealthStatusPred(agentName));
      if(sub.size() > 0)
      {
        HealthStatus hs = (HealthStatus)sub.getAddedCollection().iterator().next();
        checkElement("currentState", hs.getState(), "string", communityProperties, properties);
        checkElement("priorState", hs.getPriorState(), "string", communityProperties, properties);
        checkElement("currentStatus", hs.getStatusAsString(), "string", communityProperties, properties);
        checkElement("heartbeatRequestStatus", hs.getHeartbeatRequestStatusAsString(), "string", communityProperties, properties);
        checkElement("heartbeatRequestTime", hs.getHeartbeatRequestTime().toString(),"string", communityProperties, properties);
        checkElement("heartbeatStatus", Integer.toString(hs.getHeartbeatStatus()), "int", communityProperties, properties);
        checkElement("pingStatus", hs.getPingStatusAsString(), "string", communityProperties, properties);
        checkElement("hbReqTimeout", Long.toString(hs.getHbReqTimeout()), "long", communityProperties, properties);
        checkElement("hbReqRetries", Long.toString(hs.getHbReqRetries()), "long", communityProperties, properties);
        checkElement("hbFreq", Long.toString(hs.getHbFrequency()), "long", communityProperties, properties);
        checkElement("hbTimeout", Long.toString(hs.getHbReqTimeout()), "long", communityProperties, properties);
        checkElement("hbPctLate", Float.toString(hs.getHbPctLate()), "float", communityProperties, properties);
        checkElement("hbWindow", Float.toString(hs.getHbWindow()), "float", communityProperties, properties);
        checkElement("hbFailRate", Float.toString(hs.getFailureRate()), "float", communityProperties, properties);
        checkElement("hbFailRateThreshold", Float.toString(hs.getHbFailRateThreshold()), "float", communityProperties, properties);
        checkElement("pingTimeout", Long.toString(hs.getPingTimeout()), "long", communityProperties, properties);
        checkElement("pingRetries", Long.toString(hs.getPingRetries()), "long", communityProperties, properties);
        checkElement("activePingFreq", Long.toString(hs.getActivePingFrequency()), "long", communityProperties, properties);
        checkElement("hbReqRetryCtr", Integer.toString(hs.getHbReqRetryCtr()), "int", communityProperties, properties);
        checkElement("PingRetryCtr", Integer.toString(hs.getPingRetryCtr()), "int", communityProperties, properties);
        checkElement("persistable", Boolean.toString(hs.isPersistable()), "string", communityProperties, properties);
      }
    }finally
    { bb.closeTransaction(); }
    return properties;
  }

  /**
   * Search if given hashtable contains one element with the same name and value as
   * given element name and value, if no, save this given element into another hash table.
   * @param element name of the element need to be checked
   * @param value value of the element
   * @param type what type is the element?
   * @param table1 the hashtable be searched
   * @param table2 the hashtable saving unduplicate elements
   */
  private void checkElement(String element, String value, String type, Hashtable table1, Hashtable table2)
  {
    if(table1.containsKey(element))
    {
      String tmp = (String)table1.get(element);
      if(type.equals("string"))
        if(!tmp.equals(value))
          table2.put(element, value);
      else if(type.equals("int"))
        if(Integer.parseInt(value) != Integer.parseInt(tmp))
          table2.put(element, value);
      else if(type.equals("float"))
        if(Float.parseFloat(value) != Float.parseFloat(tmp))
          table2.put(element, value);
      else if(type.equals("long"))
        if(Long.parseLong(value) != Long.parseLong(tmp))
          table2.put(element, value);
    }
    else
      table2.put(element, value);
  }

  /**
   * Using xsl file to transform a xml file into html file.
   * @param xml given xml string
   * @param xsl name of xsl file
   * @return the html string
   */
  private String getHTMLFromXML(String xml, String xsl)
  {
    String html = "";
    try{
      TransformerFactory tFactory = TransformerFactory.newInstance();
      File xslf = ConfigFinder.getInstance().locateFile(xsl);
      Transformer transformer = tFactory.newTransformer(new StreamSource(xslf));
      StringWriter writer = new StringWriter();
      transformer.transform(new StreamSource(new StringReader(xml)), new StreamResult(writer));
      html = writer.toString();
    }catch(Exception e){log.error(e.getMessage());}
    return html;
  }

  /**
   * From community information fetched from name service, produce a xml string
   * contains all this information.
   * @param totalList the hashtable contains community information
   * @param community which community is concerned?
   * @return the xml string
   */
  private String getXMLOfCommunity(Hashtable totalList, String community)
  {
      List restartNodes = new ArrayList(); //save all restart nodes
      Hashtable properties = new Hashtable(); //all attributes of community
      Hashtable list = (Hashtable)totalList.get("Communities");
      StringBuffer xmlsb = new StringBuffer();
      xmlsb.append(xmlTitle);
      xmlsb.append("<community name=\"" + community + "\">\n");
      xmlsb.append("  <properties>\n");
      List content = (List)list.get(community); //all information of this community
      Attributes attributes = (Attributes)content.get(0); //attributes of the community
      try{
        for(NamingEnumeration nes = attributes.getAll(); nes.hasMore();)
        {
          Attribute attr = (Attribute)nes.next();
          if(attr.size() == 1)
          {
            String tmp = (String)attr.get();
            if(attr.getID().equalsIgnoreCase("RestartNode"))
              restartNodes.add(tmp);
            xmlsb.append("    <property name=\"" + attr.getID() + "\" value=\"" + tmp + "\" />\n");
            properties.put(attr.getID(), tmp);
          }
          else
          {
            List tmpList = new ArrayList();
            String str = attr.getID();
            for(NamingEnumeration subattrs = attr.getAll(); subattrs.hasMore();)
            {
              String tmp = (String)subattrs.next();
              if(str.equalsIgnoreCase("RestartNode"))
                restartNodes.add(tmp);
              xmlsb.append("    <property name=\"" + str + "\" value=\"" + tmp + "\" />\n");
              tmpList.add(tmp);
            }
            properties.put(str, tmpList);
          }
        }
      }catch(NamingException e){log.error(e.getMessage());}
      xmlsb.append("  </properties>\n");

      Hashtable allNodes = (Hashtable)content.get(3);//all nodes and agents of current community
      xmlsb.append("  <nodes>\n");
      for(Enumeration enums = allNodes.keys(); enums.hasMoreElements();)
      {
        String nodeName = (String)enums.nextElement();
        xmlsb.append("    <node name=\"" + nodeName + "\">\n");
        xmlsb.append("      <properties>\n");
        if(restartNodes.contains(nodeName))
          xmlsb.append("        <property name=\"type\" value=\"restart\"/>\n");
        else
          xmlsb.append("        <property name=\"type\" value=\"normal\"/>\n");
        xmlsb.append("      </properties>\n");
        xmlsb.append("    </node>\n");
      }
      xmlsb.append("  </nodes>\n");

      xmlsb.append("  <agents>\n");
      for(Enumeration enums = allNodes.keys(); enums.hasMoreElements();)
      {
        String nodeName = (String)enums.nextElement();
        List agents = (List)allNodes.get(nodeName); //all agents of given node
        for(int i=0; i<agents.size(); i++)
        {
          String agentName = (String)agents.get(i);
          xmlsb.append("    <agent name=\"" + agentName + "\">\n");
          xmlsb.append("      <properties>\n");
          xmlsb.append("        <property name=\"node\" value=\"" + nodeName + "\"/>\n");
          Hashtable agentProperties = getUnduplicatePropertiesOfAgent(agentName, properties);
          for(Enumeration enum = agentProperties.keys(); enum.hasMoreElements();)
          {
            String name = (String)enum.nextElement();
            xmlsb.append("        <property name=\"" + name + "\" value=\"" + (String)agentProperties.get(name) + "\"/>\n");
          }
          xmlsb.append("      </properties>\n");
          xmlsb.append("    </agent>\n");
        }
      }
      xmlsb.append("  </agents>\n");

      xmlsb.append("  <events>\n");
      xmlsb.append("  </events>\n");

      xmlsb.append("</community>\n");
     return xmlsb.toString();
  }

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

  private static final String xmlTitle = "<?xml version=\"1.0\"?>\n";
}