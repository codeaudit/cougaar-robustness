package org.cougaar.tools.robustness.ma.test;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.lang.reflect.*;
import org.cougaar.core.servlet.*;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.service.*;
import org.cougaar.core.domain.RootFactory;

import org.cougaar.tools.robustness.ma.plugins.ManagementAgentProperties;

/**
 * This servlet enables host to change the parameters of one of the plugins:
 * HealthMOnitorPlugin, RestartLocatorPlugin and DecisionPlugin.</p>
 */
public class ParameterModifierServlet extends BaseServletComponent implements BlackboardClient {
  private BlackboardService blackboard;
  private DomainService ds;
  private RootFactory rootFactory;

  private static final String monitorPlugin = "HealthMonitorPlugin";
  private static final String decisionPlugin = "DecisionPlugin";
  private static final String restartPlugin = "RestartLocatorPlugin";
  private static final String packageName = "org.cougaar.tools.robustness.ma.plugins.";

  private Collection properties = null;
  private ManagementAgentProperties prop = null;
  private String pluginName = monitorPlugin;

  public void load() {
    super.load();
  }

  protected String getPath() {
    return "/modifyParameters";
  }

  public void setBlackboardService(BlackboardService blackboard) {
    this.blackboard = blackboard;
  }

  public void setDomainService(DomainService ds) {
    this.ds = ds;
    this.rootFactory = ds.getFactory();
  }

  protected Servlet createServlet() {
    return new MyServlet();
  }

  public void unload() {
    super.unload();
    if (blackboard != null) {
      serviceBroker.releaseService(
          this, BlackboardService.class, blackboard);
      blackboard = null;
    }
  }

  private class MyServlet extends HttpServlet
  {
    public void doGet(HttpServletRequest req, HttpServletResponse res)
      throws IOException, ServletException
    {

      Modifier mod = new Modifier();
      mod.execute(req, res);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res)
      throws IOException, ServletException
    {

      Modifier mod = new Modifier();
      mod.execute(req, res);
    }
  }

  private class Modifier
  {
    private HttpServletRequest request;
    private HttpServletResponse response;
    private PrintWriter out;
    private boolean doSubmit;
    private String[] newParams;

    public void execute(HttpServletRequest req, HttpServletResponse res)
      throws IOException, ServletException
    {
      this.request = req;
      this.response = res;
      out = response.getWriter();
      getPropertiesFromBlackboard();
      String[][] params = getParamsOfPlugin(pluginName);
      newParams = new String[params.length];
      parseParams();
    }

    private void parseParams() throws IOException
    {
      doSubmit = false;
      // create a URL parameter visitor
      ServletUtil.ParamVisitor vis =
        new ServletUtil.ParamVisitor() {
          public void setParam(String name, String value) {
            if (name.equalsIgnoreCase("plugin")) {
               pluginName = value;
            }
            else if(name.equalsIgnoreCase(monitorPlugin))
              pluginName = monitorPlugin;
            else if(name.equalsIgnoreCase("submit"))
            {
              doSubmit = true;
            }
            else if (name.startsWith("param"))
            {
               String order = name.substring(5, name.length());
               int i = Integer.parseInt(order);
               newParams[i] = value;
            }
          }
        };

      // visit the URL parameters
      ServletUtil.parseParams(vis, request);

      if(doSubmit)
      {
        //String[][] params = new String[prop.size()][2];
        int i=0;
        for(Enumeration enums = prop.keys(); enums.hasMoreElements();)
        {
          //params[i][0] = (String)enums.nextElement();
          //params[i][1] = (String)newParams[i];
          String key = (String)enums.nextElement();
          String value = (String)newParams[i];
          prop.setProperty(key, value);
          i++;
        }
        //newParams.clear();
        //ManagementAgentProperties newProp = ManagementAgentProperties.makeProps(packageName + pluginName, params);

        try{
          blackboard.openTransaction();
          blackboard.publishChange(prop);
          //blackboard.publishRemove(prop);//System.out.println("remove old props: " + prop);
          //blackboard.publishAdd(newProp);//System.out.println("add new props: " + newProp);
        }finally
        { blackboard.closeTransaction(); }
      }

      //decide which page to display
      displayParams(pluginName);
    }

    private String[][] getParamsOfPlugin(String plugin)
    {
      for(Iterator it = properties.iterator(); it.hasNext();)
      {
        prop = (ManagementAgentProperties)it.next();
        if(prop.getPluginName().equals(packageName + plugin))
        {
          String[][] params = new String[prop.size()][2];
          int i=0;
          for(Enumeration enums = prop.keys(); enums.hasMoreElements();)
          {
            String key = (String)enums.nextElement();
            String value = (String)prop.get(key);
            params[i][0] = key;
            params[i][1] = value;
            i++;
          }
          return params;
        }
      }
      return new String[][]{};
    }

    private void getPropertiesFromBlackboard()
    {
      try{
        blackboard.openTransaction();
        properties = (Collection)blackboard.query(getPropertiesPred());
      }finally
      { blackboard.closeTransaction(); }
    }

    private void displayParams(String plugin) throws IOException
    {
      out.print("<html><body>\n<p>\n<br><br><br>\n");
      out.print("<center><table BORDER=0 CELLSPACING=0 CELLPADDING=0 WIDTH=\"780\">\n");
      out.print("<tr>\n");
      out.print("<form method=\"GET\" action=\"" + request.getRequestURI() + "\">\n");
      out.print("<td WIDTH=\"450\">\n</td>\n");
      out.print("<td VALIGN=BOTTOM WIDTH=\"130\">\n");
      //out.print("<input type=image src=\"./test/images/temp.gif\" alt=\"monitorPlugin\" name=\"" + monitorPlugin + "\">\n");
      out.print("<input type=\"submit\" name=\"plugin\" value=\"" + monitorPlugin + "\">\n");
      out.print("</td>\n");
      out.print("<td VALIGN=BOTTOM WIDTH=\"100\">\n");
      out.print("<input type=\"submit\" name=\"plugin\" value=\"" + decisionPlugin + "\">\n");
      out.print("</td>\n");
      out.print("<td VALIGN=BOTTOM WIDTH=\"130\">\n");
      out.print("<input type=\"submit\" name=\"plugin\" value=\"" + restartPlugin + "\">\n");
      out.print("</td>\n");
      out.print("</tr>\n</table></center>\n");
      out.print("<br><br><center><h2>" + "Parameters of " + plugin + "</h2></center>\n");

      out.print("<br><br><br>\n");
      out.print("<center><table BORDER=1 CELLSPACING=1 CELLPADDING=1 WIDTH=\"600\">\n");

      getPropertiesFromBlackboard();
      String[][] params = getParamsOfPlugin(plugin);
      for(int i=0; i<params.length; i++)
        drawOneRole(params[i][0], params[i][1], i);
      out.print("</table></center>\n");

      out.print("<br><br><br><center><input type=\"submit\" name=\"submit\" value=\"Submit\"></center>\n");
      out.print("</form>");
      out.print("</p>\n</body></html>");
    }

    private void drawOneRole(String name, String value, int i)
    {
      out.print("<tr>\n");
      out.print("<td WIDTH=\"120\">\n");
      out.print(name);
      out.print("</td>\n");
      out.print("<td WIDTH=\"250\">\n");
      out.print("<input type=\"text\" size=\"50\" name=\"" + "param" + i + "\" value=\"" + value + "\">\n");
      out.print("</td>\n");
      out.print("</tr>\n");
    }
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

  protected static UnaryPredicate getPropertiesPred()
  {
     return new UnaryPredicate() {
       public boolean execute(Object o) {
           return (o instanceof ManagementAgentProperties);
       }
     };
  }

}