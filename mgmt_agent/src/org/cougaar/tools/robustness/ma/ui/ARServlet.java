package org.cougaar.tools.robustness.ma.ui;

import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.io.*;

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


import org.cougaar.tools.robustness.ma.ldm.HealthMonitorRequest;
import org.cougaar.tools.robustness.ma.ldm.HealthMonitorRequestImpl;
import org.cougaar.tools.robustness.ma.ldm.HealthMonitorResponse;
import org.cougaar.tools.robustness.ma.ldm.RelayAdapter;

import org.cougaar.core.service.community.Community;

import javax.xml.transform.*;
import javax.xml.transform.stream.*;

/**
 * This servlet provides robustness community information.
 */
public class ARServlet extends BaseServletComponent implements BlackboardClient{
  private BlackboardService bb;
  private LoggingService log;
  private UIDService uidService;
  private DomainService domain;
  private MessageAddress agentId;
  private MessageAddress nodeId;
  private MobilityFactory mobilityFactory;

  /**
   * Hard-coded servlet path.
   */
  protected String getPath() {
    return "/ar";
  }

  public void load() {
    // get the logging service
    log =  (LoggingService) serviceBroker.getService(this, LoggingService.class, null);
    uidService =  (UIDService) serviceBroker.getService(this, UIDService.class, null);
    super.load();
  }

  public void setBlackboardService(BlackboardService blackboard) {
    this.bb = blackboard;
  }

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
  //parameters of current poeration
  private String operation = "", mobileAgent = "", origNode = "", destNode = ""; //forceRestart = "";
  private class Worker
  {
    private HttpServletRequest request;
    private HttpServletResponse response;
    private PrintWriter out;
    private String action = "", format = "", dest="";
    //private List communities = new ArrayList();
    private String robustnessCommunity = "";
    private String lastop="", lastma="", laston="", lastdn=""; //lastfs="";

    public void execute(HttpServletRequest req, HttpServletResponse res)
      throws IOException, ServletException
    {
      this.request = req;
      this.response = res;
      out = response.getWriter();
      robustnessCommunity = getRobustnessCommunity();
      if(robustnessCommunity == null){
        out.print("<html><body>Can't find robustness community from blackboard.</body></html>");
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
          && destNode.equals(lastdn))// && forceRestart.equals(lastfs))
          writeSuccess(format); //this is just for refresh, don't do a publishAdd.
        else {
          try {
              //AgentControl ac = createAgentControl(command);
              //addAgentControl(ac);
              publishHealthMonitorMove(robustnessCommunity);
          } catch (Exception e) {
            writeFailure(e);
            return;
          }
          writeSuccess(format);
        }
      }
      else if(command.equals("refresh"))
        writeSuccess(format);
      else if(command.equals("removeTicket")) {
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
      }
      else if(command.equals("loadBalance")) {
        if(currentCommunityXML.equals(""))
          currentCommunityXML = displayStatus(robustnessCommunity);
        log.debug("current community status xml: \n" + currentCommunityXML);
        loadBalance(robustnessCommunity);
        writeSuccess(format);
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
      //String xml = currentCommunityXML;
      currentCommunityXML = xml;
      if(format.equals("xml"))
        out.write(xml);
      else
      {
        String html = getHTMLFromXML(xml, "communityAttributes.xsl");
        out.print(html);
      }
    }

    private void writeSuccess(String format){
      Collection col = queryAgentControls();
      String xml;
      if(col.size() > 0) {
        xml = getAgentControlXML(col);
      }
      else
        xml = currentCommunityXML;
      if(format.equals("xml"))
        out.write(xml);
      else {
        String html = getHTMLFromXML(xml, "communityControl.xsl");
        out.print(html);
      }
    }

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

  private void addAgentControl(AgentControl ac) {
    try {
      bb.openTransaction();
      bb.publishAdd(ac);
    } finally {
      bb.closeTransactionDontReset();
    }
  }

  private void removeAgentControl(AgentControl ac) {
    try {
      bb.openTransaction();
      bb.publishRemove(ac);
    } finally {
      bb.closeTransaction();
    }
  }

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

  private void loadBalance(String communityName) {
    HealthMonitorRequest hmr =
        new HealthMonitorRequestImpl(agentId,
                                     communityName,
                                     HealthMonitorRequest.LOAD_BALANCE,
                                     null,
                                     null,
                                     null,
                                     uidService.nextUID());
    MessageAddress target = nodeId != null
        ? nodeId
        : agentId;

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
                 hmr.getRequestTypeAsString());
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
   * Get all robustness communities from whit page service and save them in a list.
   * @return the list of all community names
   */
  private String getRobustnessCommunity() {
    Collection communityDescriptors = null;
    try{
      bb.openTransaction();
      communityDescriptors = bb.query(communityPredicate);
    }finally{bb.closeTransactionDontReset();}
    try{
      for(Iterator it = communityDescriptors.iterator(); it.hasNext();) {
        Community community = (Community)it.next();
        String type = (String)(community.getAttributes().get("CommunityType").get());
        if(type != null)
          if(community.hasEntity(agentId.getAddress()) && type.equals("Robustness"))
            return community.getName();
      }
    }catch(Exception e)
    {log.error("Try to get robustness community of " + agentId.getAddress() + ":" + e);}
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
    sb.append("<AgentControls>\n");
    for(Iterator it = acs.iterator(); it.hasNext();) {
      AgentControl ac = (AgentControl)it.next();
      sb.append("  <AgentControl>\n");
      sb.append("    <UID>" + ac.getUID() + "</UID>\n");
      sb.append("    <Ticket>\n");
      String str = ac.getAbstractTicket().toString();
      str.replaceAll("<", "");
      str.replaceAll(">", "");
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
      if (target.equals(agentId)) {
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