package org.cougaar.tools.robustness.ma.ui;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import org.cougaar.core.service.community.CommunityService;
import org.cougaar.core.service.community.Community;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.core.servlet.ServletUtil;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.SimpleMessageAddress;
import org.cougaar.core.mobility.ldm.MobilityFactory;
import org.cougaar.core.mobility.AbstractTicket;
import org.cougaar.core.mobility.RemoveTicket;
import org.cougaar.core.mobility.ldm.AgentControl;
import org.cougaar.core.util.UID;

import org.cougaar.tools.robustness.ma.ldm.HealthMonitorRequest;
import org.cougaar.tools.robustness.ma.ldm.HealthMonitorRequestImpl;
import org.cougaar.tools.robustness.ma.ldm.HealthMonitorResponse;
import org.cougaar.tools.robustness.ma.ldm.RelayAdapter;

import org.cougaar.core.service.community.Community;
import org.cougaar.core.service.community.CommunityResponse;
import org.cougaar.core.service.community.CommunityResponseListener;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

/**
 * This servlet provides robustness community information.
 */
public class ARServlet extends BaseServletComponent implements BlackboardClient{
  private BlackboardService bb; //blackboard service
  private LoggingService log; //logging service provide debug information
  private UIDService uidService; //uid service
  private DomainService domain; //domain service provides mobility factory
  private MessageAddress agentId; //what is current agent
  private MessageAddress nodeId; //what is current node
  private MobilityFactory mobilityFactory; //used to to agents mobility
  private CommunityService commSvc;

  /**
   * Hard-coded servlet path.
   */
  protected String getPath() {
    return "/ar";
  }

  public void setCommunityService(CommunityService cs){
    this.commSvc = cs;
  }

  /**
   * Load the servlet and get necessary services.
   */
  public void load() {
    uidService =  (UIDService) serviceBroker.getService(this, UIDService.class, null);
    super.load();
  }

  /**
   * Add blackboard service to this servlet
   */
  public void setBlackboardService(BlackboardService blackboard) {
    this.bb = blackboard;
  }

  /**
   * Add domain service to this servlet
   */
  public void setDomainService(DomainService domain) {
    this.domain = domain;
    if (domain == null) {
      mobilityFactory = null;
    } else {
      mobilityFactory =
        (MobilityFactory) domain.getFactory("mobility");
    }
  }

  /**
   * Create the servlet.
   */
  protected Servlet createServlet() {
    AgentIdentificationService ais = (AgentIdentificationService)serviceBroker.getService(
        this, AgentIdentificationService.class, null);
    if (ais != null) {
      this.agentId = ais.getMessageAddress();
      serviceBroker.releaseService(this, AgentIdentificationService.class, ais);
    }
    // get the logging service
    log =  (LoggingService) serviceBroker.getService(this, LoggingService.class, null);
    log = org.cougaar.core.logging.LoggingServiceWithPrefix.add(log, agentId + ": ");
    return new MyServlet();
  }

  /**
   * Release the serlvet.
   */
  public void unload() {
    super.unload();
    if (bb != null) {
      serviceBroker.releaseService(
          this, BlackboardService.class, bb);
      bb = null;
    }
    if (domain != null) {
      serviceBroker.releaseService(
          this, DomainService.class, domain);
      domain = null;
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
      log.debug("Request=" + req.getQueryString());
      Worker worker = new Worker();
      worker.execute(req, res);
    }
  }

  private String currentCommunityXML = ""; //xml of current displayed community
  private ArrayList healthMonitors = new ArrayList();
  private ArrayList agents = new ArrayList();
  private Hashtable communityAttributes = new Hashtable();
  //parameters of current operation
  private String operation = "", mobileAgent = "", origNode = "", destNode = ""; //forceRestart = "";
  private class Worker
  {
    private HttpServletRequest request;
    private HttpServletResponse response;
    private PrintWriter out;
    private String action = "", format = "", dest="";
    private String robustnessCommunity = ""; //current community
    private String lastop="", lastma="", laston="", lastdn=""; //lastfs="";
    private String attrId = null;
    private String attrValue = null;
    public void execute(HttpServletRequest req, HttpServletResponse res)
      throws IOException, ServletException
    {
      this.request = req;
      this.response = res;
      out = response.getWriter();
      robustnessCommunity = getRobustnessCommunity();
      if (robustnessCommunity == null) {
        out.print("<html><body>Can't find robustness community.</body></html>");
        return;
      }

      parseParams();
    }

    private void parseParams() throws IOException
    {
      //remember all parameters for last operation, in case this is just a refresh call.
      if (!operation.equals("")) {
        lastop = operation;
        lastma = mobileAgent;
        laston = origNode;
        lastdn = destNode;
        //lastfs = forceRestart;
      }
      operation = "";
      // create a URL parameter visitor
      ServletUtil.ParamVisitor vis =
          new ServletUtil.ParamVisitor() {
        public void setParam(String name, String value) {
          if (name.equalsIgnoreCase("format")) //display page in html or xml?
            format = value;
          if (name.equals("community")) { //show attributes of seleced community
            action = "community";
          }
          if (name.equals("showcommunity")) { //detail of the community
            action = "showCommunity";
          }
          if (name.equals("node")) //show this community using a remote node
            nodeId = SimpleMessageAddress.getSimpleMessageAddress(value);
          if (name.equals("agent")) {
            action = "showAgent";
            dest = value;
          }
          if (name.equals("control")) { //show community control page
            action = "control";
          }
          if (name.equals("operation")) { //do a move or remove?
            action = value;
            operation = value;
          }
          if (name.equals("mobileagent")) //which agent is operated?
            mobileAgent = value;
          if (name.equals("orignode")) //original node of this agent
            origNode = value;
          if (name.equals("destinationnode")) //move to where?
            destNode = value;
          if (value.equals("refresh")) //refresh the page
            action = "refresh";
          if (value.equals("remove")) { //remove selected ticket
            action = "removeTicket";
            dest = name;
          }
          if (value.equalsIgnoreCase("Kill") ||
              value.equalsIgnoreCase("Restart") ||
              value.equalsIgnoreCase("ForcedRestart") ||
              value.equalsIgnoreCase("loadBalance") ||
              value.equalsIgnoreCase("add") ||
              value.equalsIgnoreCase("move")) {
            if (value.equalsIgnoreCase("ForcedRestart"))
              action = "restart";
            else
              action = value.toLowerCase();
          }
          if (name.equalsIgnoreCase("modCommAttr")) {
            action = "modCommAttr";
          }
          if (name.equals("id")) {
            attrId = value;
          }
          if (name.equals("value")) {
            attrValue = value;
          }
        }
      };

      // visit the URL parameters
      ServletUtil.parseParams(vis, request);

      String xml = displayStatus(robustnessCommunity);
      if (xml == null) {
        writeNullResult();
        return;
      }

      String str = xml.substring(xml.indexOf("<community"), xml.indexOf(">"));
      while (str.indexOf("=\"") != -1) {
        String name = str.substring(str.indexOf(" ") + 1, str.indexOf("=\""));
        String value = str.substring(str.indexOf("=\"") + 2,
                                     str.indexOf("\"", str.indexOf("=\"") + 2));
        communityAttributes.put(name, value);
        str = str.substring(str.indexOf("\"", str.indexOf("=\"") + 2));
      }

      int startIndex = xml.indexOf("<healthMonitors");
      int endIndex = xml.indexOf("</healthMonitors>");
      String temp = xml.substring(startIndex, endIndex);
      healthMonitors = getHealthMonitors(temp);
      startIndex = xml.indexOf("<agents");
      endIndex = xml.indexOf("</agents>");
      temp = xml.substring(startIndex, endIndex);
      agents = getAgents(temp);
      currentCommunityXML = xml;

      displayParams(action, dest);
    }

    private void displayParams(String command, String value) throws IOException
    {
      if (command.equals("") ||
          command.equals("showCommunity")) {
        showCommunityData(format, robustnessCommunity);
      } else if (command.equals("community")) {
        showCommunityAttributes(format, robustnessCommunity);
      } else if (command.equals("control")) {
        controlCommunity(format, robustnessCommunity);
      } else if (command.equals("move") ||
                 command.equals("Remove") ||
                 command.equals("kill") ||
                 command.equals("restart") ||
                 command.equals("add") ||
                 command.equals("loadbalance")) {
        if (operation.equals(lastop) &&
            mobileAgent.equals(lastma) &&
            origNode.equals(laston) &&
            destNode.equals(lastdn)) {
          writeSuccess(format); //this is just for refresh, don't do a publishAdd.
        } else {
          try {
            if (command.equals("move") ||
                command.equals("kill") ||
                command.equals("restart") ||
                command.equals("add") ||
                command.equals("loadbalance")) {
              publishHealthMonitorRequest(robustnessCommunity, command);
            } else { //publish remove tickets to remove agents
              AgentControl ac = createAgentControl(command);
              addAgentControl(ac);
            }
          } catch (Exception e) {
            writeFailure(e);
            return;
          }
          writeSuccess(format);
        }
      } else if(command.equals("refresh")) {
        writeSuccess(format);
      } else if(command.equals("removeTicket")) {
        try {
            UID uid = UID.toUID(value);
            AgentControl ac = queryAgentControl(uid);
            if (ac != null) {
              removeAgentControl(ac);
            }
        } catch (Exception e) {
            writeFailure(e);
            return;
        }
        writeSuccess(format);
      }  else if(command.equals("showAgent")) {
        showAgentAttributes(format, value);
      } else if (command.equalsIgnoreCase("modCommAttr")) {
        modifyCommunityAttribute(robustnessCommunity, attrId, attrValue);
      }
    }

    /**
     * Show all agents, their parent nodes and health status in a table. Path is
     * ./ar?showCommunity=xxx.
     * @param format show raw xml or html?
     * @param community the community to shown.
     */
    private void showCommunityData(String format, String community)
    {
      if(format.equals("xml"))
        out.write(currentCommunityXML);
      else
      {
        String html = getHead(robustnessCommunity, style, statusScript) + getStatusBody(robustnessCommunity) + "</html>";
        out.print(html);
      }
    }

    /**
     * Shows the control page of the community. The control page is used to moving
     * or removing agents.
     * @param format show raw xml data or html?
     * @param community the community name
     */
    private void controlCommunity(String format, String community)
    {
      writeSuccess(format);
    }

    /**
     * Show attributes of one specified community. Path is ./ar?community=xxx.
     * @param format show raw xml data or html? default is html.
     * @param community the community name
     */
    private void showCommunityAttributes(String format, String community)
    {
      if(format.equals("xml"))
        out.write(currentCommunityXML);
      else
      {
        out.print(displayCommunityAttributes(robustnessCommunity));
      }
    }

    private void showAgentAttributes(String format, String agent) {
      String xml = currentCommunityXML;
      int start = xml.indexOf("<agent name=\"" + agent);
      int end = xml.indexOf("</agent>", start);
      xml = xml.substring(start, end+9);
      if(format.equals("xml"))
        out.write(xml);
      else {
        out.print(displayAgentAttributes(agent));
      }
    }

    /**
     * This method is used when user try to load 'community control' page. It will
     * list all agent mobility tickets in blackboard.
     * @param format
     */
    private void writeSuccess(String format){
      Collection col = queryAgentControls();
      if(format.equals("xml"))
        out.write(currentCommunityXML);
      else {
        String html = getHead(robustnessCommunity, "", controlScript) + getControlBody(col);
        out.print(html);
      }
    }

    /**
     * Show exception or error message.
     */
    private void writeFailure(Exception e) throws IOException {
        // select response message
        response.setContentType("text/html");
        // build up response
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(baos);
        out.print("<html><body>");
        out.print(
            "<center>"+
            "<h2>Agent Control (failed)</h2>"+
            "</center>"+
            "<p><pre>\n");
        e.printStackTrace(out);
        out.print(
            "</body></html>\n");
        // send error code
        response.sendError(
            HttpServletResponse.SC_BAD_REQUEST,
            new String(baos.toByteArray()));
        out.close();
      }

      private void writeNullResult() {
        out.print("<html><body>Getting response from health monitor is failed.</body></html>");
      }
  }

  /**
   * Create agent control for current operation.
   * @param op "Move" or "Remove"
   * @return
   */
  private AgentControl createAgentControl(String op) {
    MessageAddress mobileAgentAddr = MessageAddress.getMessageAddress(mobileAgent);
    MessageAddress originNodeAddr = MessageAddress.getMessageAddress(origNode);
    MessageAddress target;
    AbstractTicket ticket;
       target = (originNodeAddr != null ? originNodeAddr : mobileAgentAddr);
       ticket =
         new RemoveTicket(
                  null,
                  mobileAgentAddr,
                  originNodeAddr);
     AgentControl ac = ARServlet.this.createAgentControl(null, target, ticket);
    return ac;
  }

  private AgentControl createAgentControl(
      UID ownerUID,
      MessageAddress target,
      AbstractTicket ticket) {
    if (mobilityFactory == null) {
      throw new RuntimeException(
          "Mobility factory (and domain) not enabled");
    }
    AgentControl ac =
      mobilityFactory.createAgentControl(
          ownerUID, target, ticket);
    return ac;
  }

  /**
   * Publish add given agent control to blackboard.
   * @param ac
   */
  private void addAgentControl(AgentControl ac) {
    try {
      bb.openTransaction();
      bb.publishAdd(ac);
      if (log.isDebugEnabled()) {
        log.debug("publishing AgentControl: source=" + ac.getSource().toAddress() +
                  " target=" + ac.getTarget().toAddress());
      }
    } finally {
      bb.closeTransactionDontReset();
    }
  }

  /**
   * Publish remove an agent control from blackboard.
   * @param ac
   */
  private void removeAgentControl(AgentControl ac) {
    try {
      bb.openTransaction();
      bb.publishRemove(ac);
    } finally {
      bb.closeTransaction();
    }
  }

  /**
   * How many mobility tickets in blackboard?
   * @return the collection of all agent controls
   */
  private Collection queryAgentControls() {
    Collection ret = null;
    try {
      bb.openTransaction();
      ret = bb.query(AGENT_CONTROL_PRED);
    } finally {
      bb.closeTransactionDontReset();
    }
    return ret;
  }

  /**
   * Get the agent control with given uid from blackboard.
   * @param uid the identity of agent control
   * @return the agent control
   */
  private AgentControl queryAgentControl(final UID uid) {
    if (uid == null) {
      throw new IllegalArgumentException("null uid");
    }
    UnaryPredicate pred = new UnaryPredicate() {
      public boolean execute(Object o) {
        return
          ((o instanceof AgentControl) &&
           (uid.equals(((AgentControl) o).getUID())));
      }
    };
    AgentControl ret = null;
    try {
      bb.openTransaction();
      Collection c = bb.query(pred);
      if ((c != null) && (c.size() >= 1)) {
        ret = (AgentControl) c.iterator().next();
      }
    } finally {
      bb.closeTransactionDontReset();
    }
    return ret;
  }


  /**
   * Modify specified community attribute.  Attribute is created if it doesn't
   * exist.
   * @param communityName Name of affected community
   * @param attrId        ID of attribute to modify
   * @param attrValue     New value
   */
  protected void modifyCommunityAttribute(String communityName,
                                          String attrId,
                                          String attrValue) {
    if (log.isDebugEnabled()) {
      log.debug("modifyCommunityAttributes:" +
               " communityName=" + communityName +
               " attrId=" + attrId +
               " attrValue=" + attrValue);
    }
    if (communityName != null && attrId != null && attrValue != null) {
      changeAttributes(communityName,
                       new Attribute[] {new BasicAttribute(attrId, attrValue)});
    }
  }


  /**
   * Modify one or more attributes of a community or entity.
   * @param community      Target community
   * @param newAttrs       New attributes
   */
  protected void changeAttributes(final String communityName, Attribute[] newAttrs) {
    Community community = commSvc.getCommunity(communityName, null);
    if (community != null) {
      List mods = new ArrayList();
      for (int i = 0; i < newAttrs.length; i++) {
        try {
          Attributes attrs = community.getAttributes();
          Attribute attr = attrs.get(newAttrs[i].getID());
          if (attr == null || !attr.contains(newAttrs[i].get())) {
            int type = attr == null
                ? DirContext.ADD_ATTRIBUTE
                : DirContext.REPLACE_ATTRIBUTE;
            mods.add(new ModificationItem(type, newAttrs[i]));
          }
        } catch (NamingException ne) {
          if (log.isErrorEnabled()) {
            log.error("Error setting community attribute:" +
                      " community=" + community.getName() +
                      " attribute=" + newAttrs[i]);
          }
        }
      }
      if (!mods.isEmpty()) {
        CommunityResponseListener crl = new CommunityResponseListener() {
          public void getResponse(CommunityResponse resp) {
            if (resp.getStatus() != CommunityResponse.SUCCESS) {
              if (log.isWarnEnabled()) {
                log.warn(
                    "Unexpected status from CommunityService modifyAttributes request:" +
                    " status=" + resp.getStatusAsString() +
                    " community=" + communityName);
              }
            }
          }
      };
        commSvc.modifyAttributes(communityName,
                            null,
                            (ModificationItem[])mods.toArray(new ModificationItem[0]),
                            crl);
      }
    }
  }

  /**
   * Get the robustness community of this agent.
   * @return the list of all community names
   */
  private String getRobustnessCommunity() {
    String parentCommunities[] = commSvc.getParentCommunities(true);
    try {
      for (int i = 0; i < parentCommunities.length; i++) {
        Community community = commSvc.getCommunity(parentCommunities[i], null);
        String type = (String) (community.getAttributes().get("CommunityType").get());
        if (type != null) {
          if (community.hasEntity(agentId.getAddress()) &&
              type.equals("Robustness"))
            return community.getName();
        }
      }
    } catch (Exception ex) {
      if (log.isErrorEnabled()) {
        log.error(ex.getMessage(), ex);
      }
    }
    return null;
  }

  /**
   * Get complete status xml string of given community.
   * @param communityName
   * @return
   */
  private String displayStatus(String communityName) {
      HealthMonitorRequest hmr =
          new HealthMonitorRequestImpl(agentId,
          communityName,
          HealthMonitorRequest.GET_STATUS,
          null,
          null,
          null,
          uidService.nextUID());
      MessageAddress target = nodeId != null
          ? nodeId
          : agentId;

      HealthMonitorResponse hmrResp = null;

      if (target.equals(agentId)) {
        if (log.isDebugEnabled()) {
          log.debug("Publishing HealthMonitorRequest:" + hmr);
        }
        try {
          bb.openTransaction();
          bb.publishAdd(hmr);
        } finally {
          bb.closeTransactionDontReset();
        }
        while (hmr.getResponse() == null) {
          try { Thread.sleep(1000);} catch (Exception ex) {}
        }
        hmrResp = (HealthMonitorResponse)hmr.getResponse();
      } else {
        // send to remote agent using Relay
        RelayAdapter hmrRa =
            new RelayAdapter(agentId, hmr, hmr.getUID());
        hmrRa.addTarget(target);
        if (log.isDebugEnabled()) {
          log.debug("Publishing HealthMonitorRequest Relay:" +
                   " target=" + hmrRa.getTargets() +
                   " community-" + hmr.getCommunityName());
        }
        try {
          bb.openTransaction();
          bb.publishAdd(hmrRa);
        } finally {
          bb.closeTransactionDontReset();
        }
        while (hmrRa.getResponse() == null) {
          try { Thread.sleep(1000);} catch (Exception ex) {}
        }
        hmrResp = (HealthMonitorResponse)hmrRa.getResponse();
      }

      if (hmrResp.getStatus() == hmrResp.FAIL && log.isDebugEnabled()) {
        log.error("try to get health monitor response: " +
                  hmrResp.getStatusAsString());
      }
      return (String)hmrResp.getContent();
    }

  private UnaryPredicate communityPredicate = new UnaryPredicate() {
    public boolean execute (Object o) {
      return (o instanceof Community);
  }};

  private static final UnaryPredicate AGENT_CONTROL_PRED =
    new UnaryPredicate() {
      public boolean execute(Object o) {
        return (o instanceof AgentControl);
      }
    };

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


  /**
   * Publish a health monitor request: kill, forced restart, move or load balancing.
   * @param communityName String
   * @param command String
   */
  private void publishHealthMonitorRequest(String communityName, String command) {
    int request = command.equals("kill") ? HealthMonitorRequest.KILL :
          (command.equals("restart") ? HealthMonitorRequest.FORCED_RESTART :
          (command.equals("loadbalance") ? HealthMonitorRequest.LOAD_BALANCE :
          (command.equals("add") ? HealthMonitorRequest.ADD
                                 : HealthMonitorRequest.MOVE)));
    String orig = origNode;
    String dest = destNode;
    if(origNode.equals("")) orig = null;
    if(destNode.equals("")) dest = null;
    HealthMonitorRequestImpl hmr = new HealthMonitorRequestImpl(agentId,
                                            communityName,
                                            request,
                                            new String[] {mobileAgent},
                                            orig,
                                            dest,
                                            uidService.nextUID());
    publishRequest(hmr);
  }


  /**
   * Get manager of one robustness community. This method should can be done using:
   *     AttributeBasedAddress.getAttributeBasedAddress(community,
   *                                                    "Role",
   *                                                    "RobustnessManager");
   * Well, at the time of coding, AttributeBasedAddress doesn't work as expected.
   * @param community String
   * @return MessageAddress
   */
  private MessageAddress getRobustnessManager(String community) {
    MessageAddress target = null;
    Community comm = commSvc.getCommunity(community, null);
    if(comm != null) {
      try{
        Attribute attr = comm.getAttributes().get("RobustnessManager");
        target = MessageAddress.getMessageAddress( (String) attr.get());
      }catch(Exception e) {}
    }
    return target;
  }


  protected void publishRequest(HealthMonitorRequest hmr) {
    MessageAddress target = getRobustnessManager(hmr.getCommunityName());
    if (log.isDebugEnabled()) {
      log.debug("publishRequest: " + hmr);
    }
    Object request = hmr;
    if (!target.equals(agentId)) {
      // send to remote agent using Relay
      request = new RelayAdapter(agentId, hmr, hmr.getUID());
      ((RelayAdapter)request).addTarget(target);
    }
    try {
      bb.openTransaction();
      bb.publishAdd(request);
    } finally {
      bb.closeTransactionDontReset();
    }
  }

  /**
   * Write the head part of html page.
   * @param communityName
   * @param contentStyle "" if no any css style.
   * @param script
   * @return
   */
  private String getHead(String communityName, String contentStyle, String script){
    StringBuffer sb = new StringBuffer();
    sb.append("<html>\n  <head>\n  <title>Community:" + communityName + "</title>\n");
    if(!contentStyle.equals(""))
      sb.append(contentStyle);
    if(!script.equals(""))
      sb.append(script);
    sb.append("  </head>\n");
    return sb.toString();
  }

  private String getStatusBody(String communityName) {
    StringBuffer sb = new StringBuffer();
    sb.append("<body>\n");
    sb.append("  <form name=\"myForm\" method=\"GET\">\n");
    sb.append("  <table border=\"0\" width=\"98%\">\n");
    sb.append("  <tr><td align=\"left\" bgcolor=\"white\">\n");
    sb.append("     <input type=\"submit\" name=\"control\" value=\"Community Control\" />\n");
    sb.append("  </td></tr>\n  </table></form>\n");
    sb.append("<center>\n  <h1>\n");
    sb.append("  <a href=\"./ar?community=" + communityName + "\">" + communityName + "</a>\n");
    sb.append("  </h1><br>\n");
    sb.append("  <h2>HEALTH MONITORS</h2>\n");
    createTable(sb, "", true, communityName);
    sb.append("<br>\n");
    sb.append("  <h2>AGENTS</h2>\n");

    //get agents count
    int index = currentCommunityXML.indexOf("<agents count");
    String temp = currentCommunityXML.substring(index);
    index = temp.indexOf("\"", 0);
    temp = temp.substring(index+1);
    int num = Integer.parseInt(temp.substring(0, temp.indexOf("\"")));

    sb.append("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\">\n");
    sb.append("  <tr>\n    <td height=\"26\"></td>\n");
    sb.append("     <td rowspan=\"" + (num+1) + "\" colspan=\"10\">\n");
    createTable(sb, "sortColumn(event)", false, communityName);
    sb.append("    <td>\n  </tr>\n");
    for(int i=1; i<num+1; i++)
      sb.append("  <tr><td height=\"30\" valign=\"middle\">" + i + "  </tr>\n");
    sb.append("</table>\n");
    sb.append("</center>\n");
    sb.append("</body>\n");
    return sb.toString();
  }

  private void createTable(StringBuffer sb, String onclick, boolean isHealthMonitor, String communityName) {
    sb.append("<table border=\"1\" cellpadding=\"8\" cellspacing=\"0\"");
    if(!onclick.equals(""))
      sb.append(" onclick=\"" + onclick + "\"");
    sb.append(">\n");
    if(isHealthMonitor) {
      sb.append("  <thead><tr>\n");
      for(int i=0; i<hmTableHeaders.length; i++) {
        sb.append(getTableHead(hmTableHeaders[i], "th"));
      }
      sb.append("  </tr></thead>\n");
    } else {
      sb.append("  <thead><tr valign=\"middle\">\n");
      for(int i=0; i<agentsTableHeaders.length; i++) {
        sb.append(getTableHead(agentsTableHeaders[i], "td"));
      }
      sb.append("  </tr></thead>\n");
    }
    sb.append("  <tbody>\n");
    if(isHealthMonitor)
      getHealthMonitorBody(sb, communityName);
    else
      getAgentsBody(sb);
    sb.append("  </tbody>\n");
    sb.append("</table>\n");
  }

  private String getTableHead(String head, String type) {
    return "    <" + type + " style=\"width: 60px;\" align=\"center\" valign=\"middle\">" + head + "</" + type + ">\n";
  }

  private void getHealthMonitorBody(StringBuffer sb, String communityName) {
    for(int i=0; i<healthMonitors.size(); i++) {
      HealthMonitor hm = (HealthMonitor)healthMonitors.get(i);
      sb.append("<tr");
      if(nodeId != null) {
        if (hm.name.equals(nodeId.getAddress()))
          sb.append(" style=\"background-color:beige\"");
      }
      else if(hm.name.equals(agentId.getAddress()))
          sb.append(" style=\"background-color:beige\"");
      sb.append(">\n");
      sb.append("<td><a href=\"./ar?showcommunity=" + communityName + "&amp;node=" + hm.name + "\">" + hm.name + "</a>\n");
      sb.append("<td>" + hm.type + "\n");
      sb.append("<td>" + hm.state + "\n");
      sb.append("<td>" + hm.vote + "\n");
      sb.append("<td>" + hm.last + "\n");
      sb.append("<td>" + hm.expires + "\n");
    }
  }

  private ArrayList getHealthMonitors(String xml) {
    ArrayList list = new ArrayList();
    while(xml.indexOf("<healthMonitor") != -1) {
      String name = getValue(xml, "name");
      String type = getValue(xml, "type");
      int index1 = xml.indexOf("<vote>");
      int index2 = xml.indexOf("</vote>");
      String vote = xml.substring(index1 + 6, index2);
      xml = xml.substring(index2);
      String state = getValue(xml, "state");
      String last = getValue(xml, "last");
      String expires = getValue(xml, "expires");
      HealthMonitor hm = new HealthMonitor(name, type, vote, state, last, expires, null);//getAttributes(xml));
      list.add(hm);
      xml = xml.substring(xml.indexOf("</healthMonitor>"));
    }
    return list;
  }

  private Hashtable getAttributes(String xml) {
    Hashtable attrs = new Hashtable();
    while(xml.indexOf("<attribute") != -1) {
      String temp = xml.substring(xml.indexOf("<attribute"), xml.indexOf("</attribute>"));
      ArrayList list = new ArrayList();
      String name = getValue(temp, "id");
      while(temp.indexOf("<value>") != -1) {
        String value = temp.substring(temp.indexOf("<value>")+7, temp.indexOf("</value>")).trim();
        list.add(value);
        temp = temp.substring(temp.indexOf("</value>") + 1);
      }
      if(attrs.containsKey(name)) {
        ArrayList l = (ArrayList)attrs.get(name);
        l.addAll(list);
      }
      else
        attrs.put(name, list);
      xml = xml.substring(xml.indexOf("</attribute>")+1);
    }
    return attrs;
  }

  private void getAgentsBody(StringBuffer sb) {
    for(int i=0; i<agents.size(); i++) {
      Agent agent = (Agent)agents.get(i);
      sb.append("  <tr>\n");
      sb.append("    <td><a href=\"./ar?agent=" + agent.name + "\">" + agent.name + "</a>\n");
      sb.append("    <td>" + agent.state + "\n");
      sb.append("    <td align=\"right\">" + agent.last + "\n");
      sb.append("    <td>" + agent.expires + "\n");
      sb.append("    <td>" + agent.current + "\n");
      sb.append("    <td>" + agent.prior + "\n");
      sb.append("  </tr>\n");
    }
  }

  private ArrayList getAgents(String xml) {
    ArrayList list = new ArrayList();
    while(xml.indexOf("<agent") != -1) {
      //xml = xml.substring(xml.indexOf("<agent"));
      String temp = xml.substring(xml.indexOf("<agent"), xml.indexOf("</agent>"));
      String name = getValue(temp, "name");
      String state = getValue(temp, "state");
      String last = getValue(temp, "last");
      String expires = getValue(temp, "expires");
      String current = getValue(temp, "current");
      String prior = getValue(temp, "prior");
      Agent agent = new Agent(name, state, last, expires, current, prior, getAttributes(temp));
      list.add(agent);
      xml = xml.substring(xml.indexOf("</agent>")+1);
    }
    return list;
  }

  private String getValue(String xml, String key) {
    int index = xml.indexOf(key + "=\"");
    xml = xml.substring(index + key.length() + 2);
    index = xml.indexOf("\"");
    String value = xml.substring(0, index);
    xml = xml.substring(index + 1);
    return value;
  }

  private String getControlBody(Collection agentControls) {
    StringBuffer sb = new StringBuffer();
    sb.append("<body>\n");
    sb.append("  <table border=\"0\" width=\"98%\">\n");
    sb.append("    <tr>\n");
    sb.append("      <form name=\"myForm\"><td align=\"left\" bgcolor=\"white\">\n");
    sb.append("       <input type=\"submit\" name=\"showcommunity\" value=\"Show Community Data\" />\n");
    sb.append("      </td></form>\n");
    sb.append("        <form name=\"lbForm\"><td align=\"left\" bgcolor=\"white\">\n");
    sb.append("          <input type=\"submit\" name=\"operation\" value=\"loadBalance\" />\n");
    sb.append("      </td></form>\n");
    sb.append("      <td width=\"65%\" />\n");
    sb.append("    </tr>\n  </table>\n\n");
    sb.append("  <h2>Create a request:</h2>\n");
    sb.append("  <form name=\"myForm1\" method=\"GET\">\n");
    sb.append("  <table border=\"0\" cellpadding=\"8\" cellspacing=\"0\">\n");
    sb.append("    <tr><td>Operation</td>\n");
    sb.append("     <td><select name=\"operation\" onchange=\"changeOp()\">\n");
    sb.append(addOption("Move", operation, ""));
    sb.append(addOption("Remove", operation, ""));
    sb.append(addOption("ForcedRestart", operation, ""));
    sb.append("    </select></td></tr>\n");
    sb.append("    <tr><td>Mobile Agent</td>\n");
    sb.append("    <td><select name=\"mobileagent\" size=\"1\" onclick=\"changeAgent()\">\n");
    String orig = "";
    for(int i=0; i<agents.size(); i++) {
      Agent agent = (Agent)agents.get(i);
      sb.append(addOption(agent.name, mobileAgent, agent.current));
      if(!mobileAgent.equals("") && agent.name.equals(mobileAgent))
        orig = agent.current;
    }
    sb.append("    </select></td></tr>\n");
    sb.append("    <tr><td>Origin Node</td>\n");
    orig = orig.equals("") ? ((Agent)agents.get(0)).current : orig;
    sb.append("      <td><input name=\"orignode\" type=\"text\" size=\"30\" value=\"" + orig + "\">\n    </tr>\n");
    sb.append("    <tr><td>Destination Node</td>\n");
    String disable = (!mobileAgent.equals("") && operation.equals("Remove")) ? " disabled" : "";
    sb.append("      <td><select name=\"destinationnode\"" + disable + ">\n");
    for(int i=0; i<healthMonitors.size(); i++) {
      HealthMonitor hm = (HealthMonitor)healthMonitors.get(i);
      if(hm.type.equals("node")) {
        disable = hm.name.equals(orig) ? " disabled" : "";
        String select = (!mobileAgent.equals("") && hm.name.equals(destNode)) ? " selected" : "";
        sb.append("      <option " + disable + " " + select + ">" + hm.name + "\n");
      }
    }
    sb.append("    </select></td></tr>\n");
    sb.append("    <tr><td>\n      <input type=\"submit\" />\n    </td></tr>\n");
    sb.append("  </table>\n  </form>\n");
    if(agentControls != null && agentControls.size() > 0) {
      sb.append("  <form name=\"myForm2\">\n");
      sb.append("  <br /><h2>Current Requests:</h2>\n");
      sb.append("  <table border=\"1\" cellpadding=\"1\" cellspacing=\"1\" width=\"95%\" bordercolordark=\"#660000\" bordercolorlight=\"#cc9966\">\n");
      sb.append("    <tr>\n      <th>UID</th>\n      <th>Ticket</th>\n      <th>Status</th>\n      <th />\n    </tr>\n");
      for (Iterator it = agentControls.iterator(); it.hasNext(); ) {
        AgentControl ac = (AgentControl) it.next();
        sb.append("    <tr>\n");
        sb.append("      <td>" + ac.getUID() + "\n");
        String str = ac.getAbstractTicket().toString();
        int index1 = str.indexOf("<");
        if (index1 >= 0) {
          int index2 = str.indexOf(">");
          str = str.substring(0, index1) + str.substring(index2 + 1, str.length());
        }
        sb.append("      <td>   " + str + "\n");
        int status = ac.getStatusCode();
        if (status == AgentControl.NONE)
          str = "In progress";
        else
          str = ac.getStatusCodeAsString();
        String bgcolor;
        if (str.equals("FAILURE"))
          bgcolor = "#FFBBBB";
        else if (str.equals("In progress"))
          bgcolor = "#FFFFBB";
        else
          bgcolor = "#BBFFBB";
        sb.append("      <td bgcolor=\"" + bgcolor + "\">" + str + "\n");
        sb.append("      <td align=\"center\" valign=\"middle\">\n");
        sb.append("        <input type=\"submit\" name=\"" + ac.getUID() +
                  "\" value=\"remove\">\n");
        sb.append("      </td>\n    </tr>\n");
      }
      sb.append("  </table>\n");
      sb.append("  <br /><center><input type=\"submit\" name=\"action\" value=\"refresh\" /></center>\n");
      sb.append("  </form>\n");
    }

    sb.append("</body></html>");

    return sb.toString();
  }

  private String addOption(String value, String selectValue, String label) {
    boolean flag = (mobileAgent.equals("") ? true : false);
    boolean equal = (flag ? false : (value.equals(selectValue)));
    String st = equal ? " selectd" : "";
    String lb = label.equals("") ? "" : " label=\"" + label + "\"";
    return "      <option" + lb + st + ">" + value + "\n";
  }

  private void getAttributeHead(StringBuffer sb, String type, String name) {
    sb.append("<html>\n  <head>\n    <title>" + type + ":" + name + "    </title>\n");
    sb.append("  </head>\n  <body><br /><br />\n    <center>\n");
    sb.append("      <h1>" + name + "</h1>\n");
    sb.append("      <table border=\"1\" cellpadding=\"10\" cellspacing=\"0\">\n");
    sb.append("        <th><xsl:text>Property Name</xsl:text></th>\n");
    sb.append("        <th><xsl:text>Property Value</xsl:text></th>\n");
  }

  private String displayAgentAttributes(String agentName) {
    StringBuffer sb = new StringBuffer();
    getAttributeHead(sb, "Agent", agentName);
    for(int i=0; i<agents.size(); i++) {
      Agent agent = (Agent)agents.get(i);
      if(agent.name.equals(agentName)) {
        Hashtable attrs = agent.attributes;
        for(Enumeration enums = attrs.keys(); enums.hasMoreElements();) {
          String id = (String)enums.nextElement();
          ArrayList values = (ArrayList)attrs.get(id);
          for(int j = 0; j<values.size(); j++) {
            sb.append("         <tr>");
            sb.append("<td>" + (j == 0 ? id : "") + "</td>");
            sb.append("<td>" + (String)values.get(j) + "</td></tr>\n");
          }
        }
        break;
      }
    }
    sb.append("      </table>\n    </center>\n  </body>\n</html>");


    return sb.toString();
  }

  private String displayCommunityAttributes(String communityName) {
    StringBuffer sb = new StringBuffer();
    getAttributeHead(sb, "Community", communityName);
    for(Enumeration enums = communityAttributes.keys(); enums.hasMoreElements();) {
      String key = (String)enums.nextElement();
      sb.append("          <tr><td>" + key + "</td><td>" + (String)communityAttributes.get(key) + "</td></tr>\n");
    }
    sb.append("      </table>\n    </center>\n  </body>\n</html>");
    return sb.toString();
  }

  private String getManageAgent(List list) {
    for(Iterator it = list.iterator(); it.hasNext();) {
      HealthMonitor hm = (HealthMonitor)it.next();
      if(hm.type.equals("agent"))
        return hm.name;
    }
    return null;
  }

  private class HealthMonitor {
    String name, type, vote, state, last, expires;
    Hashtable attributes;
    private HealthMonitor(String name, String type, String vote, String state, String last, String expires, Hashtable attrs) {
      this.name = name;
      this.type = type;
      this.vote = vote;
      this.state = state;
      this.last = last;
      this.expires = expires;
      this.attributes = attrs;
    }
  }

  private class Agent {
    String name, state, last, expires, current, prior;
    Hashtable attributes;
    private Agent(String name, String state, String last, String expires, String current, String prior, Hashtable attrs) {
      this.name = name;
      this.state = state;
      this.last = last;
      this.expires = expires;
      this.current = current;
      this.prior = prior;
      this.attributes = attrs;
    }
  }

  private final static String[] hmTableHeaders = {"Name", "Type", "Status", "Vote", "Last", "Expires"};
  private final static String[] agentsTableHeaders = {"AgentName", "Status", "Last", "Expires", "CurrentNode", "PriorNode"};

  private final static String style = "    <style type=\"text/css\">\n" +
  "     table {empty-cells: show;}\n" +
  "     tr  {background: window;}\n" +
  "     td  {color: windowtext; font: menu;}\n\n" +
  "     thead td	{background: buttonface; font: menu; font-weight: bold; border: 1px outset white;\n" +
  "               cursor: default; padding-top: 0; padding: bottom: 0;\n" +
  "               border-top: 1px solid buttonhighlight;\n" +
  "               border-left: 1px solid buttonhighlight;\n" +
  "               border-right: 1px solid buttonshadow;\n" +
  "               border-bottom: 1px solid buttonshadow;\n" +
  "               height: 12px;\n" +
  "              }\n" +
  "     thead .arrow  {font-family: webdings; color: black; padding: 0; font-size: 10px;\n" +
  "                    height: 11px; width: 10px; overflow: hidden;\n" +
  "                    margin-bottom: 2; margin-top: -3; padding: 0; padding-top: 0; padding-bottom: 2;}\n" +
  "  </style>\n";


  private final static String statusScript = "  <script language=\"JavaScript\">\n" +
      "<!--\n"+
  "  var dom = (document.getElementsByTagName) ? true : false;\n" +
"var ie5 = (document.getElementsByTagName && document.all) ? true : false;\n" +
"var arrowUp, arrowDown;\n\n" +
"if (ie5 || dom)\n" +
"        initSortTable();\n\n" +
"function initSortTable() {\n" +
"        arrowUp = document.createElement(\"SPAN\");\n" +
"        var tn = document.createTextNode(\"\");\n" +
"        arrowUp.appendChild(tn);\n" +
"        arrowUp.className = \"arrow\";\n\n" +
"        arrowDown = document.createElement(\"SPAN\");\n" +
"        var tn = document.createTextNode(\"\");\n" +
"        arrowDown.appendChild(tn);\n" +
"        arrowDown.className = \"arrow\";\n" +
"}\n\n\n" +
"function sortTable(tableNode, nCol, bDesc, sType) {\n" +
"        var tBody = tableNode.tBodies[0];\n" +
"        var trs = tBody.rows;\n" +
"        var trl= trs.length;\n" +
"        var a = new Array();\n\n" +
"        for (var i=0; i < trl; i++) {a[i] = trs[i];}\n" +
"        var start = new Date;\n" +
//"        window.status = \"Sorting data...\";\n" +
"        a.sort(compareByColumn(nCol,bDesc,sType));\n" +
//"        window.status = \"Sorting data done\";\n\n" +
"        for (var i = 0; i < trl; i++) {\n" +
"                tBody.appendChild(a[i]);\n" +
//"                window.status = \"Updating row \" + (i + 1) + \" of \" + trl +\n" +
//"                                                \" (Time spent: \" + (new Date - start) + \"ms)\";\n" +
"        }\n\n" +
"        // check for onsort\n" +
"        if (typeof tableNode.onsort == \"string\")\n" +
"                tableNode.onsort = new Function(\"\", tableNode.onsort);\n" +
"        if (typeof tableNode.onsort == \"function\")\n" +
"                tableNode.onsort();\n" +
"}\n\n" +
"function CaseInsensitiveString(s) {\n" +
"        return String(s).toUpperCase();\n" +
"}\n\n" +
"function parseDate(s) {\n" +
"        return Date.parse(s.replace(/\\-/g, '/'));\n" +
"}\n\n" +
"function toNumber(s) {\n" +
"    return Number(s.replace(/[^0-9\\.]/g, \"\"));\n" +
"}\n\n" +
"function compareByColumn(nCol, bDescending, sType) {\n" +
"        var c = nCol;\n" +
"        var d = bDescending;\n" +
"        var fTypeCast = String;\n" +
"        if (sType == \"Number\")\n" +
"                fTypeCast = Number;\n" +
"        else if (sType == \"Date\")\n" +
"                fTypeCast = parseDate;\n" +
"        else if (sType == \"CaseInsensitiveString\")\n" +
"                fTypeCast = CaseInsensitiveString;\n\n" +
"        return function (n1, n2) {\n" +
"                if (fTypeCast(getInnerText(n1.cells[c])) < fTypeCast(getInnerText(n2.cells[c])))\n" +
"                        return d ? -1 : +1;\n" +
"                if (fTypeCast(getInnerText(n1.cells[c])) > fTypeCast(getInnerText(n2.cells[c])))\n" +
"                        return d ? +1 : -1;\n" +
"                return 0;\n" +
"        };\n" +
"}\n\n" +
"function sortColumnWithHold(e) {\n" +
"        // find table element\n" +
"        var el = ie5 ? e.srcElement : e.target;\n" +
"        var table = getParent(el, \"TABLE\");\n" +
"        // backup old cursor and onclick\n" +
"        var oldCursor = table.style.cursor;\n" +
"        var oldClick = table.onclick;\n" +
"        // change cursor and onclick\n" +
"        table.style.cursor = \"wait\";\n" +
"        table.onclick = null;\n" +
"        // the event object is destroyed after this thread but we only need\n" +
"        // the srcElement and/or the target\n" +
"        var fakeEvent = {srcElement : e.srcElement, target : e.target};\n" +
"        // call sortColumn in a new thread to allow the ui thread to be updated\n" +
"        // with the cursor/onclick\n" +
"        window.setTimeout(function () {\n" +
"                sortColumn(fakeEvent);\n" +
"                // once done resore cursor and onclick\n" +
"                table.style.cursor = oldCursor;\n" +
"                table.onclick = oldClick;\n" +
"        }, 100);\n" +
"}\n\n" +
"function sortColumn(e) {\n" +
"        var tmp = e.target ? e.target : e.srcElement;\n" +
"        var tHeadParent = getParent(tmp, \"THEAD\");\n" +
"        var el = getParent(tmp, \"TD\");\n" +
"        if (tHeadParent == null)\n" +
"                return;\n" +
"        if (el != null) {\n" +
"                var p = el.parentNode;\n" +
"                var i;\n" +
"                // typecast to Boolean\n" +
"                el._descending = !Boolean(el._descending);\n" +
"                if (tHeadParent.arrow != null) {\n" +
"                        if (tHeadParent.arrow.parentNode != el) {\n" +
"                                tHeadParent.arrow.parentNode._descending = null;	//reset sort order\n" +
"                        }\n" +
"                        tHeadParent.arrow.parentNode.removeChild(tHeadParent.arrow);\n" +
"                }\n" +
"                if (el._descending)\n" +
"                        tHeadParent.arrow = arrowUp.cloneNode(true);\n" +
"                else\n" +
"                        tHeadParent.arrow = arrowDown.cloneNode(true);\n" +
"                el.appendChild(tHeadParent.arrow);\n" +
"                // get the index of the td\n" +
"                var cells = p.cells;\n" +
"                var l = cells.length;\n" +
"                for (i = 0; i < l; i++) {\n" +
"                        if (cells[i] == el) break;\n" +
"                }\n" +
"                var table = getParent(el, \"TABLE\");\n" +
"                sortTable(table,i,el._descending, el.getAttribute(\"type\"));\n" +
"        }\n" +
"}\n\n" +
"function getInnerText(el) {\n" +
"        if (ie5) return el.innerText;	//Not needed but it is faster\n" +
"        var str = \"\";\n" +
"        var cs = el.childNodes;\n" +
"        var l = cs.length;\n" +
"        for (var i = 0; i < l; i++) {\n" +
"                switch (cs[i].nodeType) {\n" +
"                        case 1: //ELEMENT_NODE\n" +
"                                str += getInnerText(cs[i]);\n" +
"                                break;\n" +
"                        case 3:	//TEXT_NODE\n" +
"                                str += cs[i].nodeValue;\n" +
"                                break;\n" +
"                }\n" +
"        }\n" +
"        return str;\n" +
"}\n\n" +
"function getParent(el, pTagName) {\n" +
"        if (el == null) return null;\n" +
"        else if (el.nodeType == 1 && el.tagName.toLowerCase() == pTagName.toLowerCase())	// Gecko bug, supposed to be uppercase\n" +
"                return el;\n" +
"        else\n" +
"                return getParent(el.parentNode, pTagName);\n" +
"}\n  " +
  "// -->\n"+
"</script>\n";


  private final static String controlScript = "<script language=\"JavaScript\">\n"+
    "<!--\n"+
"     //This function is called when user select one mobile agent. It fills the original\n" +
"     //node field and disable relative node in destination.\n" +
"     function changeAgent() {\n" +
"       var x = document.forms.myForm1.mobileagent;\n" +
"       var node = x.options[x.selectedIndex].label;\n" +
"       var orignode = document.forms.myForm1.orignode;\n" +
"       orignode.readOnly = false;\n" +
"       orignode.value = node;\n" +
"       orignode.readOnly = true;\n" +
"       var destinationnode = document.forms.myForm1.destinationnode;\n" +
"       var selected = false;\n" +
"       for(var i=0; i<destinationnode.length; i++) {\n" +
"         //destinationnode.remove(i);\n" +
"         var value = destinationnode.options[i].text;\n" +
"         if(node == value) {\n" +
"           destinationnode.options[i].disabled = true;\n" +
"           destinationnode.options[i].selected = false;\n" +
"         }\n" +
"         else {\n" +
"           destinationnode.options[i].disabled = false;\n" +
"           if(selected == false) {\n" +
"               destinationnode.options[i].selected = true;\n" +
"               selected = true;\n" +
"           }\n" +
"           else\n" +
"               destinationnode.options[i].selected = false;\n" +
"         }\n" +
"       }\n" +
"     }\n\n" +
"     //This option is called when user change selection in option field. If user\n" +
"     //select 'remove', disable the destination field.\n" +
"     function changeOp() {\n" +
"       var x = document.forms.myForm1.operation;\n" +
"       var op = x.options[x.selectedIndex].text;\n" +
"       var destinationnode = document.forms.myForm1.destinationnode;\n" +
"       destinationnode.disabled = (op != \"Move\");\n" +
"     }\n" +
    "// -->\n" +
"  </script>";


}
