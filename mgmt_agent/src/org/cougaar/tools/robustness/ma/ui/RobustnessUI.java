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

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import javax.naming.directory.Attributes;
import javax.naming.directory.Attribute;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.cougaar.lib.web.arch.root.GlobalEntry;
import org.cougaar.core.mts.MTImpl;

import org.cougaar.util.log.*;

/**
 * Utility for viewing the Robustness Communities and sending vacate commands.
 */
public class RobustnessUI extends JPanel
{
  private static Logger log = Logging.getLogger(org.cougaar.tools.robustness.ma.ui.RobustnessUI.class.getName());
  private JTree tree;
  private ArrayList path = new ArrayList();
  private DefaultTreeModel model;
  private Hashtable nodeandpath = new Hashtable();
  private Point lastVisiblePoint;
  private JScrollPane pane;
  private DefaultMutableTreeNode selectedNode = null;
  private static URL url1 = null;

  private static String host = "localhost";
  private static String port = "8800";
  private static String agent = "TestAgent";

  //parameters for vacate host
  private String parentCommunityOfSelectedHost;
  private String selectedHost;

  private static Hashtable topology;

  public RobustnessUI()
  {
    pane = new JScrollPane();
    tree = getTreeFromCommunity(new ArrayList());
    pane.getViewport().add(tree);

    pane.getViewport().addChangeListener(new ChangeListener(){
      public void stateChanged(ChangeEvent e)
      {
        lastVisiblePoint = pane.getViewport().getViewPosition();
      }
    });

    pane.setBackground(Color.white);
    pane.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
    this.setLayout(new BorderLayout());
    this.add(pane, BorderLayout.CENTER);

    JButton refresh = new JButton("refresh");
    refresh.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      { refresh();      }
    });
    this.setBackground(Color.white);
    this.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
    this.add(refresh, BorderLayout.SOUTH);

    refreshAfterVacate();
  }

  private Hashtable table;
  /**
   * Get structure tree of the society.
   * @param expands the paths need be expanded.
   * @return the tree
   */
  private JTree getTreeFromCommunity(List expands)
  {
    List nodes = new ArrayList();
    path.clear();
    nodeandpath.clear();
    table = getInfoFromServlet();
    topology = (Hashtable)table.get("Topology");
    DefaultMutableTreeNode root = new DefaultMutableTreeNode();
    /*for(Enumeration enums=table.keys(); enums.hasMoreElements();)
    {
      String name = (String)enums.nextElement();
      if(name.equalsIgnoreCase("Topology"))
      {
        Hashtable c = (Hashtable)table.get(name);
        DefaultMutableTreeNode toNode = buildTopologyTree(c, nodes);
        root.add(toNode);
        nodes.add(toNode);
      }
      else if(name.equalsIgnoreCase("Webservers"))
      {
        Hashtable c = (Hashtable)table.get(name);
        DefaultMutableTreeNode webnode = buildWebserverTree(c, nodes);
        root.add(webnode);
        nodes.add(webnode);
      }
      else if(!name.equalsIgnoreCase("Communities"))
      {
        DefaultMutableTreeNode subroot = new DefaultMutableTreeNode(name);
        nodes.add(subroot);
        List contents = (List)table.get(name);
        for(int i=0; i<contents.size(); i++)
        {
          String c = contents.get(i).toString();
          DefaultMutableTreeNode leaf = new DefaultMutableTreeNode(c);
          subroot.add(leaf);
          nodes.add(leaf);
        }
        root.add(subroot);
      }
      else
      {*/
        final Hashtable c = (Hashtable)table.get("Communities");
        DefaultMutableTreeNode communities = buildCommunitySubTree(c, nodes);
        root.add(communities);
        nodes.add(communities);
      //}
    //}


    model = new DefaultTreeModel(root);
    //tree = new JTree(model);
    tree = new DNDJTree(model, this);
    tree.setRowHeight(17);
    tree.addTreeExpansionListener(new TreeExpansionListener(){
      public void treeExpanded(TreeExpansionEvent e) {
         path.add(e.getPath().toString());
      }
      public void treeCollapsed(TreeExpansionEvent e) {
         String str = e.getPath().toString();
         int unremove = 0;
         int size = path.size();
         for(int i=0; i<size; i++)
         {
           if(((String)path.get(unremove)).startsWith(str.substring(0, str.indexOf("]"))))
           {
             path.remove(unremove);
           }
           else
           {
             unremove ++;
           }
         }
      }
    });
    tree.setCellRenderer(new ElementTreeCellRenderer());
    tree.expandRow(1);
    //final DefaultMutableTreeNode selectedNode;
    final JPopupMenu popup = new JPopupMenu();
    final JMenuItem command = new JMenuItem("vacate");
      command.addActionListener(new ActionListener(){
        public void actionPerformed(ActionEvent e)
        {
          parentCommunityOfSelectedHost = (String)((DefaultMutableTreeNode)selectedNode.getParent().getParent()).getUserObject();
          parentCommunityOfSelectedHost = parentCommunityOfSelectedHost.substring(
            parentCommunityOfSelectedHost.indexOf(" "), parentCommunityOfSelectedHost.length()).trim();
          String mgmtAgent = getMgmtAgentOfCommunity(parentCommunityOfSelectedHost, c);
          /*List memberNodes = new ArrayList();
          for(Enumeration enums = selectedNode.children(); enums.hasMoreElements();)
          {
            DefaultMutableTreeNode agentNode = (DefaultMutableTreeNode)enums.nextElement();
            memberNodes.add((String)agentNode.getUserObject());
          }*/
          selectedHost = (String)selectedNode.getUserObject();
          publishRequest("vacateHost", selectedHost, mgmtAgent, null);
          //refreshAfterVacate();
        }
      });
      popup.add(command);

      tree.addMouseListener(new MouseAdapter(){
        public void mousePressed(MouseEvent e) {
          maybeShowPopup(e);
        }
        public void mouseReleased(MouseEvent e) {
          maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
          if (e.isPopupTrigger())
          {
            int row = tree.getRowForLocation(e.getX(), e.getY());
            TreePath path = tree.getPathForRow(row);
            if(path != null)
            {
              selectedNode = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
              if(selectedNode == null)
                return;
              if(!(selectedNode instanceof EntityNode))
                return;
              //if(((DefaultMutableTreeNode)selectedNode.getParent()).getUserObject().equals("hosts"))
              if(((EntityInfo)((EntityNode)selectedNode).getUserObject()).getType() == EntityInfo.HOST)
              {
                String communityName = (String)((DefaultMutableTreeNode)selectedNode.getParent().getParent()).getUserObject();
                communityName = communityName.substring(communityName.indexOf(" "), communityName.length()).trim();
                String mgmt = getMgmtAgentOfCommunity(communityName, c);
                if(mgmt != null)
                  popup.show(e.getComponent(), e.getX(), e.getY());
              }
            }
          }
        }
      });

    for(Iterator it = nodes.iterator(); it.hasNext();)
    {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)it.next();
      nodeandpath.put(new TreePath(node.getPath()).toString(), node);
    }

    return tree;
  }

  /**
   * Sub tree to show all communities.
   * @param table the hashtable who contains all information of communities
   * @param nodes the list to record all nodes in the tree
   * @return root of the sub tree.
   */
  private DefaultMutableTreeNode buildCommunitySubTree(Hashtable table, List nodes)
  {
    DefaultMutableTreeNode root = new DefaultMutableTreeNode("Communities");

    for(Enumeration enums = table.keys(); enums.hasMoreElements();)
    {
      String name = (String)enums.nextElement();
      DefaultMutableTreeNode cnode = new DefaultMutableTreeNode("Community: " + name);
      nodes.add(cnode);
      try{
      //Hashtable contents = (Hashtable)table.get(name);
        List contents = (List)table.get(name);
      //String[][] attributes = (String[][])contents.get("attribute");
        Attributes attributes = (Attributes)contents.get(0);
        if(attributes.size() > 0)
        {
          DefaultMutableTreeNode attrs = new DefaultMutableTreeNode("attributes");
          nodes.add(attrs);
          for(NamingEnumeration nes = attributes.getAll(); nes.hasMore();)
          {
            try{
              String str;
              Attribute attr = (Attribute)nes.next();
              if(attr.size() == 1)
              {
                str = attr.getID() + " " + (String)attr.get();
              }
              else
              {
                str = attr.getID();
              }
              DefaultMutableTreeNode nvnode = new DefaultMutableTreeNode(str);
              if(attr.size() > 1)
              {
                for(NamingEnumeration subattrs = attr.getAll(); subattrs.hasMore();)
                {
                  String subattr = (String)subattrs.next();
                  DefaultMutableTreeNode subattrNode = new DefaultMutableTreeNode(subattr);
                  nvnode.add(subattrNode);
                  nodes.add(subattrNode);
                }
              }
              attrs.add(nvnode);
              nodes.add(nvnode);
            }catch(NoSuchElementException e){continue;} //in case the attribute doesn't have a value
          }
          cnode.add(attrs);
        }
        Hashtable entities = (Hashtable)contents.get(1);
        if(entities.size() > 0)
        {
          DefaultMutableTreeNode entityNode = new DefaultMutableTreeNode("entities");
          nodes.add(entityNode);
          List tempEntity = new ArrayList(); //for sorting the entity names
          for(Enumeration enEnums = entities.keys(); enEnums.hasMoreElements();)
          {
            tempEntity.add((String)enEnums.nextElement());
          }
          Collections.sort(tempEntity);
          for(int i=0; i<tempEntity.size(); i++)
          {
            String entityName = (String)tempEntity.get(i);
            DefaultMutableTreeNode nameNode = new DefaultMutableTreeNode(entityName);
            Attributes entityAttributes = (Attributes)entities.get(entityName);
            for(NamingEnumeration nes = entityAttributes.getAll(); nes.hasMore();)
            {
              try{
                Attribute attr = (Attribute)nes.next();
                String str;
                if(attr.size() == 1)
                {
                  str = attr.getID() + " " + (String)attr.get();
                }
                else
                {
                  str = attr.getID();
                }
                DefaultMutableTreeNode nvnode = new DefaultMutableTreeNode(str);
                if(attr.size() > 1)
                {
                  for(NamingEnumeration subattrs = attr.getAll(); subattrs.hasMore();)
                  {
                    String subattr = (String)subattrs.next();
                    DefaultMutableTreeNode subattrNode = new DefaultMutableTreeNode(subattr);
                    nvnode.add(subattrNode);
                    nodes.add(subattrNode);
                  }
                }
                nameNode.add(nvnode);
                nodes.add(nvnode);
              }catch(NoSuchElementException e){continue;} //in case the attribute doesn't have a value
            }
            nodes.add(nameNode);
            entityNode.add(nameNode);
          }
          cnode.add(entityNode);
        }

        Hashtable hosts = (Hashtable)contents.get(2);
        if(hosts.size() > 0)
        {
          DefaultMutableTreeNode hostNode = new DefaultMutableTreeNode("hosts");
          for(Enumeration enum = hosts.keys(); enum.hasMoreElements();)
          {
            String hostName = (String)enum.nextElement();
            //DefaultMutableTreeNode hostNameNode = new DefaultMutableTreeNode(hostName);
            EntityInfo hostEI = new EntityInfo(hostName, "host");
            EntityNode hostNameNode = new EntityNode(hostEI);
            hostNode.add(hostNameNode);
            nodes.add(hostNameNode);
            Hashtable nodetable = (Hashtable)hosts.get(hostName);
            for(Enumeration node_enum = nodetable.keys(); node_enum.hasMoreElements();)
            {
              String nodeName = (String)node_enum.nextElement();
              //DefaultMutableTreeNode nodeNameNode = new DefaultMutableTreeNode(nodeName);
              EntityInfo nodeEI = new EntityInfo(nodeName, "node");
              EntityNode nodeNameNode = new EntityNode(nodeEI);
              hostNameNode.add(nodeNameNode);
              nodes.add(nodeNameNode);
              List agents = (List)nodetable.get(nodeName);
              for(int i=0; i<agents.size(); i++)
              {
                //DefaultMutableTreeNode agentNode = new DefaultMutableTreeNode((String)agents.get(i));
                EntityInfo agentEI = new EntityInfo((String)agents.get(i), "agent");
                EntityNode agentNode = new EntityNode(agentEI);
                nodeNameNode.add(agentNode);
                nodes.add(nodeNameNode);
              }
            }
          }
          cnode.add(hostNode);
          nodes.add(hostNode);
        }

        Hashtable allNodes = (Hashtable)contents.get(3);
        if(allNodes.size() > 0)
        {
          DefaultMutableTreeNode nodesTitle = new DefaultMutableTreeNode("nodes");
          for(Enumeration enum = allNodes.keys(); enum.hasMoreElements();)
          {
            String nodeName = (String)enum.nextElement();
            DefaultMutableTreeNode nodeNameNode = new DefaultMutableTreeNode(nodeName);
            nodesTitle.add(nodeNameNode);
            nodes.add(nodeNameNode);
            List agents = (List)allNodes.get(nodeName);
            if(agents.size() > 0)
            {
              for(int i=0; i<agents.size(); i++)
              {
                DefaultMutableTreeNode agentNode = new DefaultMutableTreeNode((String)agents.get(i));
                nodeNameNode.add(agentNode);
                nodes.add(agentNode);
              }
            }
          }
          cnode.add(nodesTitle);
          nodes.add(nodesTitle);
        }
      }catch(NamingException e){e.printStackTrace();}

      root.add(cnode);
    }
    return root;
  }

  private String societyNode = " ";
  private DefaultMutableTreeNode buildTopologyTree(Hashtable table, List nodes)
  {
    DefaultMutableTreeNode root = new DefaultMutableTreeNode("Topology");
    for(Enumeration enums = table.keys(); enums.hasMoreElements();)
    {
      String key = (String)enums.nextElement();
      String address = (String)table.get(key);
      if(key.endsWith(" Node"))
      {
        key = key.substring(0, key.indexOf(" "));
        societyNode = key;
      }
      DefaultMutableTreeNode keyNode = new DefaultMutableTreeNode(key);
      nodes.add(keyNode);
      DefaultMutableTreeNode adNode = new DefaultMutableTreeNode(address);
      keyNode.add(adNode);
      nodes.add(adNode);
      root.add(keyNode);
    }
    return root;
  }

  /**
   *
   */
  private DefaultMutableTreeNode buildWebserverTree(Hashtable table, List nodes)
  {
    DefaultMutableTreeNode root = new DefaultMutableTreeNode("Webservers");
    for(Enumeration enums = table.keys(); enums.hasMoreElements();)
    {
      String name = (String)enums.nextElement();
      DefaultMutableTreeNode nameNode = new DefaultMutableTreeNode(name);
      nodes.add(nameNode);
      GlobalEntry ge = (GlobalEntry)table.get(name);
      DefaultMutableTreeNode http = new DefaultMutableTreeNode("Http Address");
      nodes.add(http);
      DefaultMutableTreeNode host = new DefaultMutableTreeNode(ge.getHttpAddress().getHostName()
        + ": " + ge.getHttpAddress().getHostAddress());
      nodes.add(host);
      http.add(host);
      DefaultMutableTreeNode port = new DefaultMutableTreeNode("port: " + ge.getHttpPort());
      nodes.add(port);
      http.add(port);
      DefaultMutableTreeNode https = new DefaultMutableTreeNode("Https Address");
      nodes.add(https);
      DefaultMutableTreeNode sh = new DefaultMutableTreeNode(ge.getHttpsAddress().getHostName()
        + ": " + ge.getHttpsAddress().getHostAddress());
      nodes.add(sh);
      DefaultMutableTreeNode sp = new DefaultMutableTreeNode("port: " + ge.getHttpsPort());
      nodes.add(sp);
      https.add(sh);
      https.add(sp);
      nameNode.add(http);
      nameNode.add(https);
      root.add(nameNode);
    }
    return root;
  }

  private DefaultMutableTreeNode buildAgentsTree(Hashtable table, List nodes)
  {
    DefaultMutableTreeNode root = new DefaultMutableTreeNode("Agents");
    for(Enumeration enum = table.keys(); enum.hasMoreElements();)
    {
      String name = (String)enum.nextElement();
      DefaultMutableTreeNode nameNode = new DefaultMutableTreeNode(name);
      nodes.add(nameNode);
      //MTImpl mt = (MTImpl)table.get(name);
      //DefaultMutableTreeNode mtNode = null;
      //try{
      DefaultMutableTreeNode mtNode = new DefaultMutableTreeNode("client: " + (String)table.get(name));
      //}catch(java.rmi.server.ServerNotActiveException e){e.printStackTrace();}
      nameNode.add(mtNode);
      nodes.add(mtNode);
      root.add(nameNode);
    }
    return root;
  }

  /**
   * Expand some nodes in the tree.
   * @param atree the tree
   * @param expandPath the paths need to be expanded
   */
  private void expandTreeNode(JTree atree, List expandPath)
  {
    for(Iterator it = expandPath.iterator(); it.hasNext();)
    {
      String tp = (String)it.next();
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)nodeandpath.get(tp);
      try{
        atree.scrollPathToVisible(new TreePath((
          (DefaultMutableTreeNode)node.getFirstChild()).getPath()));
      }catch(NullPointerException e){continue;} //nodes before refresh may not exist in current
       //model, this will cause null pointer, ignore it.
      model.reload(node);
    }
  }

  /**
   * Get all agents in current society.
   * @return a vector contains names of all agents.
   */
  protected static Vector getAgentFromServlet()
   {
    Vector clusterNames = new Vector();
    try{
     //generate a plain text, one agent name each line.
     URL url1 = new URL("http://" + host + ":" + port + "/agents?scope=all");
     URLConnection connection = url1.openConnection();
     ((HttpURLConnection)connection).setRequestMethod("PUT");
     connection.setDoInput(true);
     //connection.setDoOutput(true);
     InputStream is = connection.getInputStream();
     BufferedReader in = new BufferedReader(new InputStreamReader(is));
     String name = in.readLine();
     boolean flag = false;
     while(name != null)
     {
        if(name.indexOf("</ol>") != -1)
          break;
        if(flag)
        {
          name = name.substring(0, name.indexOf("</a>"));
          String cluster = name.substring(name.lastIndexOf(">")+1, name.length());
          if(!cluster.endsWith("Node"))
            clusterNames.add(cluster);
        }
        if(name.indexOf("<ol>") != -1)
          flag = true;
       name = in.readLine();
     }
    }catch(Exception o){o.printStackTrace();}
    return clusterNames;
  }

  /**
   * Using the agent from above method, get information about yellowpage from the servlet.
   */
  private Hashtable getInfoFromServlet()
  {
    Hashtable idc = null;
    ObjectInputStream oin = null;
    if(url1 != null)
    {
      try{
       URLConnection connection = url1.openConnection();
       connection.setDoInput(true);
       connection.setDoOutput(true);
       InputStream ins = connection.getInputStream();
       oin = new ObjectInputStream(ins);
      }catch(IOException e){log.error(e.getMessage());}
    }
    else
    {
      try{
        // First, try using the servlet in the TestAgent
        url1 = new URL("http://" + host + ":" + port + "/$" + agent + "/robustness");
        URLConnection connection = url1.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        InputStream ins = connection.getInputStream();
        oin = new ObjectInputStream(ins);
      } catch(Exception e){
        url1 = null;
      }

      Vector agents = getAgentFromServlet();
      int i=0;
      // If the TestAgent wasn't found, try all agents one by one until find the agent with the servlet
      while (oin == null) {
       try {
        String agent = (String)agents.get(i);
        url1 = new URL("http://" + host + ":" + port + "/$" +
          agent + "/robustness");
        URLConnection connection = url1.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        InputStream ins = connection.getInputStream();
        oin = new ObjectInputStream(ins);
       } catch(Exception e){
        oin = null;
        i++;
       }
      }
    }
    if (oin != null) {
      try {
        while(true)
          idc = (Hashtable)oin.readObject();
      } catch(Exception e){return idc;}
    }
    return idc;
  }

  /**
   * Refresh the tree.
   */
  protected void refresh()
  {
    List expandPath = (ArrayList)path.clone();
        Point point = lastVisiblePoint;
        pane.remove(tree);
        pane.revalidate();
        pane.repaint();
        JTree tree2 = getTreeFromCommunity(expandPath);
        pane.getViewport().add(tree2);
        expandTreeNode(tree2, expandPath);
        pane.getViewport().setViewPosition(point);
  }

  /**
   * Send the information of current selected node to the servlet, so the servlet
   * can publish relative vacate request.
   * @param hostName current selected host
   * @param communityName community of selected host
   * @param nodes a list of nodes under selected host
   * @param mgmtAgent management agent of the community which the selected host exists in
   */
  public static String publishRequest(String command, String param1, String param2, String param3)
  {
    String result = null;
    try{
      HttpURLConnection conn = (HttpURLConnection)url1.openConnection();
      conn.setRequestMethod("PUT");
      conn.setDoInput(true);
      conn.setDoOutput(true);
      OutputStream os = conn.getOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(os);
      Vector vs = new Vector();
      vs.add(command);
      if(command.equals("vacateHost"))
      {
         vs.add(param1); //host name
         vs.add(param2); //management agent name
      }
      else if(command.equals("moveAgent"))
      {
        vs.add(param1); //agent name
        vs.add(param2); //source node name
        vs.add(param3); //destination node name
      }
      oos.writeObject(vs);
      oos.close();
      //need to fix: if conn.setDoInput(false) and don't request the InputStream,
      //the servlet doesn't work.
      InputStream is = conn.getInputStream();
      ObjectInputStream iis = new ObjectInputStream(is);
      result = (String)iis.readObject();
    }catch(Exception e){
      log.error(e.getMessage());
    }
    return result;
  }

  private String getMgmtAgentOfCommunity(String communityName, Hashtable communities)
  {
    List contents = (List)communities.get(communityName);
    Attributes attrs = (Attributes)contents.get(0);
    try{
     if(attrs.get("CommunityManager") != null)
      return (String)attrs.get("CommunityManager").get(0);
    }catch(NamingException e){e.printStackTrace();}
    return null;
  }

  /**
   * Position the frame in the middle of screen.
   * @param f the frame need be repositioned
   */
  private static void relocateFrame(JFrame f)
  {
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    f.setLocation(screenSize.width / 2 - f.getSize().width / 2,
		screenSize.height / 2 - f.getSize().height / 2);
  }

  private boolean isVacateSucceed = false;
  /**
   * Create a thread to listen the community changing. If the community changes,
   * refresh the tree and stop the thread. This method is used after user clicks
   * 'vacate' button.
   */
  private void refreshAfterVacate()
  {
    Thread timer = new Thread() {
        public void run() {
          try{
           while (true) {
             /*  Hashtable currentTable = getInfoFromServlet();
               Hashtable ctable = (Hashtable)currentTable.get("Communities");
               List contents = (List)ctable.get(parentCommunityOfSelectedHost);
               Hashtable htable = (Hashtable)contents.get(2);
               if(!htable.containsKey(selectedHost))
               {
                 refresh();
                 isVacateSucceed = true;
               }*/
               String result = publishRequest("checkChange", null, null, null);
               if(result.equals("succeed"))
               {
                 refresh();
                 //isVacateSucceed = true;
               }
               sleep(10000);
           }
          }catch(InterruptedException e)
          { return; }
        }
      };
      timer.setPriority(Thread.MIN_PRIORITY);
      timer.start();
   }

   public static boolean isNodeInHost(String hostName, String nodeName)
   {
     Hashtable nodes = (Hashtable)topology.get(hostName);
     return nodes.containsKey(nodeName);
   }

   public static boolean isAgentInNode(String nodeName, String agentName)
   {
     for(Iterator it = topology.values().iterator(); it.hasNext();)
     {
       Hashtable nodes = (Hashtable)it.next();
       if(nodes.containsKey(nodeName))
       {
         Set agents = (Set)nodes.get(nodeName);
         return agents.contains(agentName);
       }
     }
     return false;
   }

  public static void main(String[] args)
  {
    switch (args.length) {
      case 3:
        agent = args[2];
      case 2:
        port = args[1];
      case 1:
        host = args[0];
    }
    JFrame frame = new JFrame("Robustness UI");
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        System.exit(0); }
     });
    frame.setSize(600, 500);
    relocateFrame(frame);
    JPanel ui = new RobustnessUI();
    frame.getContentPane().setLayout(new BorderLayout());
    frame.getContentPane().add(ui, BorderLayout.CENTER);
    frame.setVisible(true);
  }

  //enable setting distinct font for each node.
  private class ElementTreeCellRenderer extends DefaultTreeCellRenderer
  {
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
        boolean expanded, boolean leaf, int row, boolean hasFocus)
    {
      Component result = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
      this.setClosedIcon(null);
      this.setOpenIcon(null);
      this.setLeafIcon(null);
      this.setFont(new Font("Default", Font.PLAIN, 12));
      if(node.isRoot())
      {
        //this.setFont(new Font("Default", Font.BOLD, 14));
        return this;
      }
      Object obj = node.getUserObject();
      if(obj == null) return this;
      String str;
      if(obj instanceof EntityInfo)
        str = ((EntityInfo)obj).toString();
      else
        str = ((String)obj).trim();
      //if(str.equals("Topology") || str.equals("Agents") || str.equals("Webservers")
        //|| str.equals("Communities") || str.equals("MessageTransports"))
      if(str.startsWith("Community:"))
      {
        this.setFont(new Font("Dialog", Font.BOLD, 12));
        ((JLabel)result).setForeground(Color.blue);
      }
      if(str.equals("attributes") || str.equals("entities") || str.equals("hosts") || str.equals("nodes"))
      {
        ((JLabel)result).setForeground(Color.red);
      }
      if(str.equals(societyNode))
        ((JLabel)result).setForeground(Color.red);

      return this;
    }
  }

}
