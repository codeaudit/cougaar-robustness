package org.cougaar.tools.robustness.ma.ui;

import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.io.*;

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
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.SimpleMessageAddress;
import org.cougaar.core.mobility.ldm.MobilityFactory;
import org.cougaar.core.mobility.AbstractTicket;
import org.cougaar.core.mobility.MoveTicket;
import org.cougaar.core.mobility.RemoveTicket;
import org.cougaar.core.mobility.ldm.AgentControl;
import org.cougaar.core.util.UID;

import org.cougaar.multicast.AttributeBasedAddress;

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

import javax.xml.transform.*;
import javax.xml.transform.stream.*;

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
    // get the logging service
    log =  (LoggingService) serviceBroker.getService(this, LoggingService.class, null);
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
    NodeIdentificationService nis = (NodeIdentificationService)serviceBroker.getService(
        this, NodeIdentificationService.class, null);
    if (nis != null) {
      this.nodeId = nis.getMessageAddress();
      serviceBroker.releaseService(this, NodeIdentificationService.class, nis);
    }
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
      Worker worker = new Worker();
      worker.execute(req, res);
    }
  }

  private String currentCommunityXML = ""; //xml of current displayed community
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
      if(robustnessCommunity == null){
        out.print("<html><body>Can't find robustness community.</body></html>");
        return;
      }
      parseParams();
    }

    private void parseParams() throws IOException
    {
      //remember all parameters for last operation, in case this is just a refresh call.
      if(!operation.equals("")) {
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
            if(name.equalsIgnoreCase("format")) //display page in html or xml?
              format = value;
            if(name.equals("community")) //show attributes of seleced community
            {
              action = "community";
            }
            if(name.equals("showcommunity")) //detail of the community
            {
              action = "showCommunity";
            }
            if(name.equals("node")) //show this community using a remote node
              nodeId = SimpleMessageAddress.getSimpleMessageAddress(value);
            if(name.equals("agent")) {
              action = "showAgent";
              dest = value;
            }
            if(name.equals("control")) //show community control page
            {
              action = "control";
            }
            if(name.equals("operation")) { //do a move or remove?
              action = value;
              operation = value;
            }
            if(name.equals("mobileagent")) //which agent is operated?
              mobileAgent = value;
            if(name.equals("orignode")) //original node of this agent
              origNode = value;
            if(name.equals("destinationnode")) //move to where?
              destNode = value;
            //if(name.equals("forcerestart")) //
              //forceRestart = value;
            if(value.equals("refresh"))//refresh the page
              action = "refresh";
            if(value.equals("remove")){ //remove selected ticket
              action = "removeTicket";
              dest = name;
            }
            if(name.equals("loadBalance")) {
              action = "loadBalance";
            }
            if(name.equalsIgnoreCase("modCommAttr")) {
              action = "modCommAttr";
            }
            if(name.equals("id")) {
              attrId = value;
            }
            if(name.equals("value")) {
              attrValue = value;
            }
          }
        };
      // visit the URL parameters
      ServletUtil.parseParams(vis, request);
      displayParams(action, dest);
    }

    private void displayParams(String command, String value) throws IOException
    {
      if(command.equals("") || command.equals("showCommunity"))
        showCommunityData(format, robustnessCommunity);
      else if(command.equals("community"))
        showCommunityAttributes(format, robustnessCommunity);
      else if(command.equals("control"))
        controlCommunity(format, robustnessCommunity);
      else if(command.equals("Move") || command.equals("Remove")) {
        if(operation.equals(lastop) && mobileAgent.equals(lastma) && origNode.equals(laston)
          && destNode.equals(lastdn)) {// && forceRestart.equals(lastfs))
          currentCommunityXML = displayStatus(robustnessCommunity);
          writeSuccess(format); //this is just for refresh, don't do a publishAdd.
        }
        else {
          try {
            if(command.equals("Move")) {
              //AgentControl ac = createAgentControl(command);
              //addAgentControl(ac);
              publishHealthMonitorMove(robustnessCommunity);
            }
            else { //publish remove tickets to remove agents
              AgentControl ac = createAgentControl(command);
              addAgentControl(ac);
            }
          } catch (Exception e) {
            writeFailure(e);
            return;
          }
          currentCommunityXML = displayStatus(robustnessCommunity);
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
      } else if(command.equals("loadBalance")) {
        if(currentCommunityXML.equals(""))
          currentCommunityXML = displayStatus(robustnessCommunity);
        log.debug("current community status xml: \n" + currentCommunityXML);
        loadBalance(robustnessCommunity);
        writeSuccess(format);
      } else if(command.equals("showAgent")) {
        showAgentAttributes(format, value);
      } else if (command.equalsIgnoreCase("modCommAttr")) {
        modifyCommunityAttribute(robustnessCommunity, attrId, attrValue);
      }
    }

     /**
     * List names of all robustness communities in the homepage. Every name is a link to show
     * the detail of this community.
     */
    /*private void showAllCommunities()
    {
      StringBuffer xmlsb = new StringBuffer();
      xmlsb.append(xmlTitle);
      xmlsb.append("<communities>\n");
      for(Iterator it = communities.iterator(); it.hasNext();)
      {
        String community = (String)it.next();
        xmlsb.append("  <community name=\"" + community + "\"/>\n");
      }
      xmlsb.append("</communities>\n");
      String xml = xmlsb.toString();
      String html = getHTMLFromXML(xml, "communities.xsl");
      out.print(html);
    }*/

    /**
     * Show all agents, their parent nodes and health status in a table. Path is
     * ./ar?showCommunity=xxx.
     * @param format show raw xml or html?
     * @param community the community to shown.
     */
    private void showCommunityData(String format, String community)
    {
      String xml = displayStatus(community);
      if(xml == null) {
        writeNullResult();
        return;
      }
      currentCommunityXML = xml;

      if(format.equals("xml"))
        out.write(xml);
      else
      {
        xml = xml.substring(0, xml.indexOf("</community>"));
        xml += "<remoteNode>" + nodeId.getAddress() + "</remoteNode></community>";
        String html = getHTMLFromXML(xml, "communityStatus.xsl");
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
      String xml = displayStatus(community);
      if(xml == null) {
        writeNullResult();
        return;
      }
      currentCommunityXML = xml;
      writeSuccess(format);
    }

    /**
     * Show attributes of one specified community. Path is ./ar?community=xxx.
     * @param format show raw xml data or html? default is html.
     * @param community the community name
     */
    private void showCommunityAttributes(String format, String community)
    {
      String xml = displayStatus(community);
      if(xml == null) {
        writeNullResult();
        return;
      }
      currentCommunityXML = xml;
      if(format.equals("xml"))
        out.write(xml);
      else
      {
        String html = getHTMLFromXML(xml, "communityAttributes.xsl");
        out.print(html);
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
        out.print(getHTMLFromXML(xml, "agentAttributes.xsl"));
      }
    }

    /**
     * This method is used when user try to load 'community control' page. It will
     * list all agent mobility tickets in blackboard.
     * @param format
     */
    private void writeSuccess(String format){
      Collection col = queryAgentControls();
      String xml;
      if(col.size() > 0) {
        xml = getAgentControlXML(col);
      }
      else
        xml = getAgentControlXML(null);
      if(format.equals("xml"))
        out.write(xml);
      else {
        String html = getHTMLFromXML(xml, "communityControl.xsl");
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
    boolean isForceRestart = false;
    MessageAddress mobileAgentAddr = MessageAddress.getMessageAddress(mobileAgent);
    MessageAddress destNodeAddr = MessageAddress.getMessageAddress(destNode);
    MessageAddress originNodeAddr = MessageAddress.getMessageAddress(origNode);
    MessageAddress target;
    AbstractTicket ticket;
    //boolean isForceRestart = (forceRestart.equals("true") ? true : false);
    if(op.equals("Move")) {
      target = (originNodeAddr != null ? (originNodeAddr) : mobileAgentAddr);
      if (destNodeAddr == null && originNodeAddr != null) {
        destNodeAddr = originNodeAddr;
      }
      ticket =
        new MoveTicket(
                null,
                mobileAgentAddr,
                originNodeAddr,
                destNodeAddr,
                isForceRestart);
    } else {
       //target = (destNodeAddr != null ? destNodeAddr : mobileAgentAddr);
       target = (originNodeAddr != null ? originNodeAddr : mobileAgentAddr);
       ticket =
         new RemoveTicket(
                  null,
                  mobileAgentAddr,
                  //destNodeAddr);
                  originNodeAddr);
    }
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
      log.debug("publishing AgentControl: source=" + ac.getSource().toAddress() + " target=" + ac.getTarget().toAddress());
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
   * Load balance of given community.
   * @param communityName
   */
  private void loadBalance(String communityName) {
    HealthMonitorRequest hmr =
        new HealthMonitorRequestImpl(agentId,
                                     communityName,
                                     HealthMonitorRequest.LOAD_BALANCE,
                                     null,
                                     null,
                                     null,
                                     uidService.nextUID());
    /*MessageAddress target = nodeId != null
        ? nodeId
        : agentId;*/
    AttributeBasedAddress target = AttributeBasedAddress.getAttributeBasedAddress(
        communityName,
        "Role",
        "RobustnessManager");


    if (target.equals(agentId)) {
        if (log.isInfoEnabled()) {
          log.info("publishing HealthMonitorRequest: " + hmr.getRequestTypeAsString());
        }
        try {
          bb.openTransaction();
          bb.publishAdd(hmr);
        } finally {
          bb.closeTransactionDontReset();
        }
    } else {
      // send to remote agent using Relay
      RelayAdapter hmrRa =
          new RelayAdapter(agentId, hmr, hmr.getUID());
      hmrRa.addTarget(target);
      if (log.isInfoEnabled()) {
        log.info("publishing HealthMonitorRequest: " +
                 hmr.getRequestTypeAsString() + ", remote node is " + target.getAddress());
      }
      try {
        bb.openTransaction();
        bb.publishAdd(hmrRa);
      }
      finally {
        bb.closeTransactionDontReset();
      }
   }
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
    log.info("modifyCommunityAttributes:" +
                 " communityName=" + communityName +
                 " attrId=" + attrId +
                 " attrValue=" + attrValue);
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
          log.error("Error setting community attribute:" +
                       " community=" + community.getName() +
                       " attribute=" + newAttrs[i]);
        }
      }
      if (!mods.isEmpty()) {
        CommunityResponseListener crl = new CommunityResponseListener() {
          public void getResponse(CommunityResponse resp) {
            if (resp.getStatus() != CommunityResponse.SUCCESS) {
              log.warn("Unexpected status from CommunityService modifyAttributes request:" +
                          " status=" + resp.getStatusAsString() +
                          " community=" + communityName);
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
      log.error(ex.getMessage(), ex);
    }
    return null;
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
      //File xslf = ConfigFinder.getInstance().locateFile(xsl);
      InputStream in = ARServlet.class.getResourceAsStream(xsl);
      Transformer transformer = tFactory.newTransformer(new StreamSource(in));
      StringWriter writer = new StringWriter();
      transformer.transform(new StreamSource(new StringReader(xml)), new StreamResult(writer));
      html = writer.toString();
    }catch(Exception e){log.error(e.getMessage());}
    return html;
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
        if (log.isInfoEnabled()) {
          log.info("Publishing HealthMonitorRequest:" +
                   " community-" + hmr.getCommunityName());
        }
        try {
          bb.openTransaction();
          bb.publishAdd(hmr);
        } finally {
          bb.closeTransactionDontReset();
        }
        while (hmr.getResponse() == null) {
          try { Thread.sleep(1000); } catch (Exception ex) {}
        }
        hmrResp = (HealthMonitorResponse)hmr.getResponse();
      } else {
        // send to remote agent using Relay
        RelayAdapter hmrRa =
            new RelayAdapter(agentId, hmr, hmr.getUID());
        hmrRa.addTarget(target);
        if (log.isInfoEnabled()) {
          log.info("Publishing HealthMonitorRequest Relay:" +
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
          try { Thread.sleep(1000); } catch (Exception ex) {}
        }
        hmrResp = (HealthMonitorResponse)hmrRa.getResponse();
      }

      if(hmrResp.getStatus() == hmrResp.FAIL)
        log.error("try to get health monitor response: " + hmrResp.getStatusAsString());
      return (String)hmrResp.getContent();
    }

  private String getAgentControlXML(Collection acs) {
    StringBuffer sb = new StringBuffer();
    if(acs != null) {
      sb.append("<AgentControls>\n");
      for (Iterator it = acs.iterator(); it.hasNext(); ) {
        AgentControl ac = (AgentControl) it.next();
        sb.append("  <AgentControl>\n");
        sb.append("    <UID>" + ac.getUID() + "</UID>\n");
        sb.append("    <Ticket>\n");
        String str = ac.getAbstractTicket().toString();
        int index1 = str.indexOf("<");
        if (index1 >= 0) {
          int index2 = str.indexOf(">");
          str = str.substring(0, index1) + str.substring(index2 + 1, str.length());
        }
        sb.append("    " + str + "\n");
        sb.append("    </Ticket>\n");
        sb.append("    <Status>");
        int status = ac.getStatusCode();
        if (status == AgentControl.NONE)
          sb.append("In progress");
        else
          sb.append(ac.getStatusCodeAsString());
        sb.append("</Status>\n");
        sb.append("  </AgentControl>\n");
      }
      sb.append("</AgentControls>\n");
    }

    int index = currentCommunityXML.indexOf("</community>");
    String xml = currentCommunityXML.substring(0, index);
    xml += sb.toString();

    if(!mobileAgent.equals("")) {
      StringBuffer sb2 = new StringBuffer();
      sb2.append("<CurrentTicket>\n");
      sb2.append("  <operation>" + operation + "</operation>\n");
      sb2.append("  <mobileAgent>" + mobileAgent + "</mobileAgent>\n");
      sb2.append("  <origNode>" + origNode + "</origNode>\n");
      sb2.append("  <destNode>" + destNode + "</destNode>\n");
      //sb2.append("  <forceRestart>" + forceRestart + "</forceRestart>\n");
      sb2.append("</CurrentTicket>\n");
      xml += sb2.toString();
    }
    xml += "</community>";
    return xml;
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

  private static final String xmlTitle = "<?xml version=\"1.0\"?>\n";

  public static void main(String[] args) {
    String html = "";
    String xml = "<communities><community name=\"comm1\" /><community name=\"comm2\" /></communities>";
    try{
      TransformerFactory tFactory = TransformerFactory.newInstance();
      //File xslf = ConfigFinder.getInstance().locateFile(xsl);
      InputStream in = ARServlet.class.getResourceAsStream("communities.xsl");
      Transformer transformer = tFactory.newTransformer(new StreamSource(in));
      StringWriter writer = new StringWriter();
      transformer.transform(new StreamSource(new StringReader(xml)), new StreamResult(writer));
      //transformer.transform(new StreamSource(new File("/home/qing/tmp.xml")), new StreamResult(writer));
      html = writer.toString();
    }catch(Exception e){e.printStackTrace();}
    System.out.println(html);
  }

  private void publishHealthMonitorMove(String communityName) {
    HealthMonitorRequest hmr =
          new HealthMonitorRequestImpl(agentId,
          communityName,
          HealthMonitorRequest.MOVE,
          new String[]{mobileAgent},
          origNode,
          destNode,
          uidService.nextUID());
      MessageAddress target = MessageAddress.getMessageAddress(destNode);
      MessageAddress orig = MessageAddress.getMessageAddress(origNode);
      if (orig.equals(agentId)) {
        if (log.isInfoEnabled()) {
          log.info("Publishing HealthMonitorRequest: " + hmr.getRequestTypeAsString());
        }
        try {
          bb.openTransaction();
          bb.publishAdd(hmr);
        } finally {
          bb.closeTransactionDontReset();
        }
      } else {
        // send to remote agent using Relay
        RelayAdapter hmrRa =
            new RelayAdapter(agentId, hmr, hmr.getUID());
        hmrRa.addTarget(orig);
        if (log.isInfoEnabled()) {
          log.info("Publishing HealthMonitorRequest Relay:" +
                   " target=" + hmrRa.getTargets() + " " +
                   hmr.getRequestTypeAsString());
        }
        try {
          bb.openTransaction();
          bb.publishAdd(hmrRa);
        } finally {
          bb.closeTransactionDontReset();
        }
      }
  }
}