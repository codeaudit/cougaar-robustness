package org.cougaar.tools.robustness.ma.ui;

import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.core.servlet.ServletUtil;
import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.service.*;
import org.cougaar.core.adaptivity.InterAgentOperatingMode;
import org.cougaar.core.adaptivity.OMCRangeList;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.security.constants.AdaptiveMnROperatingModes;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.service.wp.WhitePagesService;

import org.cougaar.util.UnaryPredicate;

import org.cougaar.core.service.community.CommunityService;
import org.cougaar.core.service.community.CommunityResponse;
import org.cougaar.core.service.community.CommunityResponseListener;
import org.cougaar.core.service.community.Community;
import org.cougaar.core.service.community.Entity;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import EDU.oswego.cs.dl.util.concurrent.Semaphore;

import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.io.*;

public class IAOMServlet extends BaseServletComponent implements BlackboardClient{

  private LoggingService log;
  private UIDService uidService;
  private BlackboardService bb;
  private WhitePagesService wps;
  private CommunityService cs;
  private MessageAddress agentId;

  private static final int LOW = 1;
  private static final int HIGH = 2;

  protected String getPath() {
    return "/iaom";
  }

  /**
   * Load the servlet and get necessary services.
   */
  public void load() {
    // get the logging service
    log =  (LoggingService) serviceBroker.getService(this, LoggingService.class, null);
    org.cougaar.core.logging.LoggingServiceWithPrefix.add(log, agentId + ": ");
    uidService =  (UIDService) serviceBroker.getService(this, UIDService.class, null);
    super.load();
  }

  /**
   * Add blackboard service to this servlet
   */
  public void setBlackboardService(BlackboardService blackboard) {
    this.bb = blackboard;
  }

  public void setWhitePagesService(WhitePagesService wps) {
    this.wps = wps;
  }

  public void setCommunityService(CommunityService cs) {
    this.cs = cs;
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
  }

  private class MyServlet extends HttpServlet {
    PrintWriter out;
    String target = "", level = "";
    List mgmts = new ArrayList();

    public void doPut(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException
    {    }

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException
    {
      out = res.getWriter();
      /* List comms = getCommunities();
       for(Iterator it = comms.iterator(); it.hasNext();) {
         String name = (String)it.next();
         mgmts.add(getMgmtOfCommunity(name));
       }*/
      mgmts.add(agentId.getAddress());
      parseParams(req);
    }

    private void parseParams(HttpServletRequest req) throws IOException{
      ServletUtil.ParamVisitor vis =
        new ServletUtil.ParamVisitor() {
         public void setParam(String name, String value) {
           if(name.equals("target")) {
             target = value;
           }
           if(name.equals("level")) {
             level = value;
           }
         }
      };
      // visit the URL parameters
      ServletUtil.parseParams(vis, req);
      showPage(out, mgmts);
      if(!target.equals("") && !level.equals(""))
        submit();
    }

    private void submit() {
      OMCRangeList list = new OMCRangeList(new Integer(LOW), new Integer(HIGH));
      InterAgentOperatingMode iaom = new InterAgentOperatingMode("org.cougaar.core.security.monitoring.THREATCON_LEVEL", list);
      //MessageAddress target = MessageAddress.getMessageAddress("SMALL-ARManager");
      //iaom.setTarget(target);
      if(level.equalsIgnoreCase("HIGH"))
        iaom.setValue(new Integer(HIGH));
      else
        iaom.setValue(new Integer(LOW));
      iaom.setTarget(MessageAddress.getMessageAddress(target));
      boolean temp = modeExists(iaom);
      if(modeExists(iaom)) {
         publishMode(iaom, "change");
      }
      else {
         publishMode(iaom, "add");
      }
    }

  }

  private void publishMode(InterAgentOperatingMode iaom, String action) {
      try {
        bb.openTransaction();
        if(action.equals("add")) {
          iaom.setUID(uidService.nextUID());
          bb.publishAdd(iaom);
        }
        else
          bb.publishChange(iaom);
          //bb.publishAdd(iaom);
      }
      finally {
        bb.closeTransactionDontReset();
      }
    log.info("publish " + action + " InterAgentOperatingMode: target=" + iaom.getTargets() + " content=" + iaom.toString());
  }

  private boolean modeExists (InterAgentOperatingMode iaom) {
    try {
      bb.openTransaction();
      opModes = (IncrementalSubscription)bb.subscribe(_threatConOM);
      Collection list = opModes.getCollection();
      /*for(Iterator it = list.iterator(); it.hasNext();) {
        InterAgentOperatingMode temp = (InterAgentOperatingMode)it.next();
        if(temp.getTargets() == iaom.getTargets()) {
          return true;
        }
      }
      return false;*/
      return list.size() > 0;
    } finally
    {bb.closeTransactionDontReset();}
  }

  private void showPage(PrintWriter out, List mgmts) {
    StringBuffer sb = new StringBuffer();
    sb.append("<html>\n<head><title>Publish Threat Alert</head></title>\n<body>\n");
    sb.append("<br><form name=\"myForm\">\n");
    sb.append("<table border=\"0\" cellpadding=\"10\" cellspacing=\"0\">\n<tr><td>Relay Targer</td>\n");
    sb.append("<td><select name=\"target\">\n");
    for(Iterator it = mgmts.iterator(); it.hasNext();) {
      sb.append("<option>" + (String)it.next() + "</option>\n");
    }
    sb.append("</select>\n<td>\n<tr>\n");
    sb.append("<tr><td>Alert Level</td>\n<td><select name=\"level\">\n");
    sb.append("<option>HIGH</option>\n<option>LOW</option>\n</select>\n<td>\n<tr>\n");
    sb.append("</table>\n");
    sb.append("<input type=\"submit\" name=\"submit\" value=\"Publish\">\n");
    sb.append("</form>\n");
    sb.append("</body>\n</html>");
    out.print(sb.toString());
  }

  private List getCommunities() {
    List communities = new ArrayList();
    try{
      Set all = wps.list(".comm", 5);
      for(Iterator it = all.iterator(); it.hasNext();) {
        String name = (String)it.next();
        name = name.substring(0, name.indexOf(".comm"));
        communities.add(name);
      }
    }catch(Exception e)
    {log.error("try to get communities from WhitePagesService: " + e);}
    return communities;
  }

  private String getMgmtOfCommunity(String communityName) {
    final List comms = new ArrayList();
    final Semaphore s = new Semaphore(0);
    Community community = cs.getCommunity(communityName, new CommunityResponseListener(){
        public void getResponse(CommunityResponse resp){
          comms.add((Community)resp.getContent());
          s.release();
        }
    });
    if (community != null) {
        comms.add(community);
    } else {
        try {
          s.acquire();
        } catch (InterruptedException e) {}
    }
    community = (Community)comms.iterator().next();

    List mgmts = new ArrayList();
    Attribute attr = community.getAttributes().get("RobustnessManager");
    try {
      return (String)attr.get();
    }catch(NamingException e){log.error(e.getMessage(), e.fillInStackTrace());}

    return null;
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

  private IncrementalSubscription opModes;
  UnaryPredicate _threatConOM = new UnaryPredicate() {
  public boolean execute(Object o) {
    if(o == null || !(o instanceof InterAgentOperatingMode)) {
      return false;
    }
    InterAgentOperatingMode mode = (InterAgentOperatingMode)o;
    return mode.getName().equals("org.cougaar.core.security.monitoring.THREATCON_LEVEL");
  }
};



}