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

package org.cougaar.tools.robustness.threatalert;

import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.io.*;

import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.core.servlet.ServletUtil;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.community.CommunityService;
import org.cougaar.core.service.community.CommunityResponse;
import org.cougaar.core.service.community.CommunityResponseListener;
import org.cougaar.core.service.community.Community;
import org.cougaar.core.service.community.Entity;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.mts.MessageAddress;

import org.cougaar.util.UnaryPredicate;

import org.cougaar.multicast.AttributeBasedAddress;

import org.cougaar.community.RelayAdapter;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import EDU.oswego.cs.dl.util.concurrent.Semaphore;

/**
 * Let user choose all conditions of one threat alert and publish it to the blackboard.
 */
public class ThreatAlertServlet extends BaseServletComponent implements BlackboardClient {
  private LoggingService log;    //logging service
  private BlackboardService bb;  //blackboard service
  private WhitePagesService wps; //white pages service
  private UIDService uidService; //uid service
  private CommunityService cs;   //community service
  private ThreatAlertService threatAlertService;
  private MessageAddress agentId; //which agent address is invoked?

  /**
   * Hard-coded servlet path.
   */
  protected String getPath() {
    return "/alert";
  }

  /**
   * Load the servlet and get necessary services.
   */
  public void load() {
    // get the logging service
    log =  (LoggingService) serviceBroker.getService(this, LoggingService.class, null);
    uidService = (UIDService) serviceBroker.getService(this, UIDService.class, null);
    super.load();
  }

  /**
   * Add blackboard service to this servlet
   */
  public void setBlackboardService(BlackboardService blackboard) {
    this.bb = blackboard;
  }

  /**
   * Add community service.
   */
  public void setCommunityService(CommunityService cs) {
    this.cs = cs;
  }

  /**
   * Add Threat Alert service.
   */
  public void setThreatAlertService(ThreatAlertService tas) {
    this.threatAlertService = tas;
  }

  /**
   * Add white pages service.
   */
  public void setWhitePagesService(WhitePagesService wps) {
    this.wps = wps;
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
    if(log != null) {
      serviceBroker.releaseService(this, LoggingService.class, log);
      log = null;
    }
    if(wps != null) {
      serviceBroker.releaseService(this, WhitePagesService.class, wps);
      wps = null;
    }
    if(cs != null) {
      serviceBroker.releaseService(this, CommunityService.class, cs);
      cs = null;
    }
    if(threatAlertService != null) {
      serviceBroker.releaseService(this, ThreatAlertService.class, threatAlertService);
      threatAlertService = null;
    }
    if(uidService != null) {
      serviceBroker.releaseService(this, UIDService.class, uidService);
      uidService = null;
    }
  }

  private class MyServlet extends HttpServlet {
    private HttpServletRequest request;
    private PrintWriter out;
    private List communities;
    private String command, target;
    private HashMap conditions = new HashMap();

    public void doPut(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException
    {
      doGet(req, res);
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException
    {
      this.request = req;
      out = res.getWriter();
      communities = getCommunities();
      if(communities.size() == 0){
        out.print("<html><body>No communities catched from blackboard.</body></html>");
        return;
      }
      parseParams();
    }

    public void parseParams() throws IOException {
      command = "";
      target = "";
      conditions.clear();
      // create a URL parameter visitor
      ServletUtil.ParamVisitor vis =
        new ServletUtil.ParamVisitor() {
          public void setParam(String name, String value) {
            if(value.equals("submit")) { //submit a threat alert
              command = "submit";
            }
            else if(!value.equals("")){
              conditions.put(name, value); //these are conditions of the threat alert
            }
          }
      };
      // visit the URL parameters
      ServletUtil.parseParams(vis, request);
      displayParams(command, target);
    }

    private void displayParams(String name, String value) throws IOException {
      if(name.equals("")) {
        out.print(getPage(communities, null));
      }
      else if(name.equals("submit")) {
        fireThreatAlert(conditions);
        out.print(getPage(communities, conditions));
      }
    }
  }

  /**
   * Fetch all conditions from user inputing and get the object ThreatAlert. Publish
   * the object to blackboard.
   * @param conditions name-value pair of all conditions of the threat alert.
   */
  private void fireThreatAlert(HashMap conditions) {

    Date createTime = new Date();
    int level = getSeverityLevel((String)conditions.get("level")); //alert level
    Date start;
    if(conditions.containsKey("startFromAction")) {
      String t = (String)conditions.get("startFromAction");
      start = getDateCondition(t);
      conditions.remove(t);
    }
    else
      start = getDateCondition("start", conditions); //alert start time
    Date expire;
    if(conditions.containsKey("expireFromAction")) {
      String t = (String)conditions.get("expireFromAction");
      expire = getDateCondition(t);
      conditions.remove(t);
    }
    else
      expire = getDateCondition("expire", conditions); //alert expire time
    ThreatAlertImpl tai = new ThreatAlertImpl(agentId, level, start, expire, uidService.nextUID());
    tai.setCreationTime(createTime); //alert create time
    //add assets to this alert
    for(int i=0; i<5; i++) {
      if(conditions.containsKey("type" + i)) {
        String type = (String)conditions.get("type" + i);
        String id = (String)conditions.get("id" + i);
        tai.addAsset(new AssetImpl(type, id));
      }
    }

    threatAlertService.sendAlert(tai, (String)conditions.get("community"), (String)conditions.get("role"));

  }

  private int getMonth(String month) {
    for(int i=0; i<months.length; i++) {
      if(month.equals(months[i]))
        return i;
    }
    return -1;
  }

  /**
   * get start time and expired time from user input
   * @param use can be two values: "start" or "expire"
   */

  private Date getDateCondition(String use, HashMap conditions) {
    int year = Integer.parseInt((String)conditions.get(use + "year"));
    int month = getMonth((String)conditions.get(use + "month"));
    int day = Integer.parseInt((String)conditions.get(use + "day"));
    int hour = Integer.parseInt((String)conditions.get(use + "hour"));
    int minute = Integer.parseInt((String)conditions.get(use + "minute"));
    String ampm = (String)conditions.get(use + "ampm");
    if(ampm.equals("PM")) hour += 12;
    Calendar calendar = Calendar.getInstance();
    calendar.set(year, month, day, hour, minute);
    return calendar.getTime();
  }

  private Date getDateCondition(String date) {
    double sec = Double.parseDouble(date);
    long s = (long)(sec * 1000);
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(s);
    return calendar.getTime();
  }

  private int getSeverityLevel(String level) {
    for(int i=0; i<levels.length; i++) {
      if(levels[i].equalsIgnoreCase(level)) {
        if(i != 5)
          return i;
        else
          return -1;
      }
    }
    return -2;
  }

  /**
   * Use WhitePagesService to get all communities in current society
   * @return
   */
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

  /**
   * The main page.
   */
  private String getPage(List communities, HashMap map) {
    StringBuffer sb = new StringBuffer();
    sb.append("<html>\n<head><title>Threat Alert</title></head>\n");
    sb.append(getJavaScript());
    sb.append("<body>\n");
    sb.append("<form name=\"myForm\">\n");
    sb.append("<table border=\"0\" cellpadding=\"10\" cellspacing=\"0\">\n");
    sb.append("<tr>\n<td>Community</td>\n");
    sb.append("<td><select name=\"community\" onclick=\"changeRole()\">\n");
    for(Iterator it = communities.iterator(); it.hasNext(); ) {
      String option = (String)it.next();
      List roles = getRolesOfCommunity(option);
      sb.append("<option");
      if(map != null) {
        sb.append((option.equals((String)map.get("community"))) ? " selected" : "");
      }
      sb.append(" label=\"" + roles.toString() + "\"");
      sb.append(">" + option + "</option>\n");
    }
    sb.append("</select></td>\n</tr>\n");
    sb.append("<tr><td>Role</td>\n");
    sb.append("<td><select name=\"role\">\n");
    List roles = getRolesOfCommunity((String)communities.iterator().next());
    for(Iterator it = roles.iterator(); it.hasNext();) {
      String role = (String)it.next();
      sb.append("<option");
      if(map != null) {
        sb.append((role.equals((String)map.get("role"))) ? " selected" : "");
      }
      sb.append(">" + role + "</option>\n");
    }
    sb.append("</select></td>\n</tr>\n");
    sb.append("<tr><td>Severity Level</td>\n");
    sb.append("<td><select name=\"level\">\n");
    for(int i=0; i<levels.length; i++) {
      sb.append("<option");
      if(map != null) {
        sb.append((levels[i].equals((String)map.get("level"))) ? " selected" : "");
      }
      sb.append(">" + levels[i] + "</option>\n");
    }
    sb.append("</td></tr>\n");
    sb.append("<tr><td>Start Time</td>\n");
    sb.append("<td>" + getDate("start", map) + "</td><tr>\n");
    sb.append("<tr><td>Expire Time</td>\n");
    sb.append("<td>" + getDate("expire", map) + "</td><tr>\n");
    sb.append("</table>\n");
    sb.append(addAssets(map));
    sb.append("<br><input type=\"submit\" name=\"submit\" value=\"submit\">");
    sb.append("</form>\n");
    sb.append("</body>\n</html>");
    return sb.toString();
  }

  private String getDate(String use, HashMap map) {
    int year, month=0, day, hour, minute, ampm;
    if(map == null) {
      Calendar rightnow = Calendar.getInstance();
      year = rightnow.get(Calendar.YEAR);
      month = rightnow.get(Calendar.MONTH);
      day = rightnow.get(Calendar.DAY_OF_MONTH);
      hour = rightnow.get(Calendar.HOUR);
      minute = rightnow.get(Calendar.MINUTE);
      ampm = rightnow.get(Calendar.AM_PM);
    }
    else {
      year = Integer.parseInt((String)map.get(use + "year"));
      String t = (String)map.get(use + "month");
      for(int i=0; i<months.length; i++)
        if(months[i].equals(t))
          month = i;
      day = Integer.parseInt((String)map.get(use + "day"));
      hour = Integer.parseInt((String)map.get(use + "hour"));
      minute = Integer.parseInt((String)map.get(use + "minute"));
      t = (String)map.get(use + "ampm");
      if(t.equals("AM")) {
        ampm = Calendar.AM;
      }
      else
        ampm = Calendar.PM;
    }
    StringBuffer sb = new StringBuffer();
    sb.append("<select name=\"" + use + "month\">\n");
    for(int i=0; i<months.length; i++) {
      if(i == month)
        sb.append("<option selected>");
      else
        sb.append("<option>");
      sb.append(months[i] + "</option>\n");
    }
    sb.append("</select>\n");
    sb.append("<select name=\"" + use + "day\">\n");
    for(int i=1; i<32; i++) {
      if(i == day)
        sb.append("<option selected>");
      else
        sb.append("<option>");
      sb.append(Integer.toString(i) + "</option>\n");
    }
    sb.append("</select>\n");
    sb.append("<select name=\"" + use + "year\">\n");
    for(int i=year; i<year + 11; i++) {
      sb.append("<option>" + Integer.toString(i) + "</option>\n");
    }
    sb.append("</select>\n");
    sb.append("&nbsp;&nbsp;");
    sb.append("<select name=\"" + use + "hour\">\n");
    for(int i=1; i<13; i++) {
      if(i == hour)
        sb.append("<option selected>");
      else
        sb.append("<option>");
      sb.append(Integer.toString(i) + "</option>\n");
    }
    sb.append("</select>\n");
    sb.append("&nbsp;:&nbsp;");
    sb.append("<select name=\"" + use + "minute\">\n");
    for(int i=0; i<61; i++) {
      if(i == minute)
        sb.append("<option selected>");
      else
        sb.append("<option>");
      sb.append(Integer.toString(i) + "</option>\n");
    }
    sb.append("</select>\n");
    sb.append("<select name=\"" + use + "ampm\">\n");
    if(ampm == Calendar.AM) {
      sb.append("<option selected>AM</option>\n");
      sb.append("<option>PM</option>\n");
    }
    else {
      sb.append("<option>AM</option>\n");
      sb.append("<option selected>PM</option>\n");
    }
    sb.append("</select>\n");
    return sb.toString();
  }

  private String addAssets(HashMap map) {
    StringBuffer sb = new StringBuffer();
    sb.append("<table border=\"0\" cellpadding=\"10\" cellspacing=\"0\">\n");
    sb.append("<tr><th>Asset Type</th><th>Asset Identifier</th></tr>\n");
    for(int i=0; i<5; i++) {
      String type = "", id="";
      if(map != null && map.containsKey("type" + i)) {
        type = (String)map.get("type" + i);
        id = (String)map.get("id" + i);
      }
      sb.append(addOneAsset(i, type, id));
    }
    sb.append("</table>\n");
    return sb.toString();
  }

  private String addOneAsset(int i, String type, String id) {
    return "<tr><td><input type=\"text\" name=\"type" + i + "\" size=\"20\" value=\"" + type + "\" /></td>\n" +
        "<td><input type=\"text\" name=\"id" + i + "\" size=\"20\" value=\"" + id + "\" /></td></tr>\n";
  }

  private String getJavaScript() {
    StringBuffer sb = new StringBuffer();
    sb.append("<script language=\"JavaScript\">\n");
    sb.append("function changeRole() {\n");
    sb.append("  var x = document.forms.myForm.community;\n");
    sb.append("  var roles = x.options[x.selectedIndex].label;\n");
    sb.append("  var r = document.forms.myForm.role;\n");
    sb.append("  for(var i=0; i<r.length; i++) {\n");
    sb.append("    r.remove(i);}\n");
    sb.append("  var length = roles.length;\n");
    sb.append("  roles = roles.substring(1, length-1);\n");
    sb.append("  var array = roles.split(\",\");\n");
    sb.append("  for(var i=0; i<array.length; i++) {\n");
    sb.append("    var option = new Option(array[i], array[i]);\n");
    sb.append("    r.options[i] = option;}\n");
    sb.append("}\n");
    sb.append("</script>\n");
    return sb.toString();
  }

  /**
   * Fetch all active roles of given community using CommunityService.
   * @param communityName
   * @return
   */
  private List getRolesOfCommunity(String communityName) {
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

    List crs = new ArrayList();
    for(Iterator it = community.getEntities().iterator(); it.hasNext();) {
      Entity entity = (Entity) it.next();
      Attribute att = entity.getAttributes().get("Role");
      try {
        for (NamingEnumeration ne = att.getAll(); ne.hasMore(); ) {
          String role = (String) ne.next();
          if (!crs.contains(role))
            crs.add(role);
        }
      }
      catch (NamingException e) {
        log.error(e.getMessage(), e.fillInStackTrace());
      }
    }
    return crs;
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

  /*private UnaryPredicate communityPredicate = new UnaryPredicate() {
  public boolean execute (Object o) {
    return (o instanceof Community);
  }};*/

  //private static final String[] roles = new String[]{"Member", "HealthMonitor", "ThreatAlertListener"};
  private static final String[] levels = new String[]{"Max", "High", "Medium", "Low", "Min", "Undefined"};
  private static final String[] months = new String[]{"January", "Feburary", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"};

}
