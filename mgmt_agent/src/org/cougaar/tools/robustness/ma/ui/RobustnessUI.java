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
 * Utility for viewing the Robustness Communities, sending vacate commands and
 * relocating agents.
 */
public class RobustnessUI extends JPanel
{
  private static Logger log = Logging.getLogger(org.cougaar.tools.robustness.ma.ui.RobustnessUI.class.getName());
  //private JTree tree;
  /** Save all expanded pathes, these pathes will be expand again during refresh. */
  private ArrayList path = new ArrayList();
  //private DefaultTreeModel model;

  /** Record all nodes and relative pathes in the tree. */
  private Hashtable nodeandpath = new Hashtable();

  /** Record the most currently visible point in the scroll pane,
  so the scroll pane will be set at the same viewing position during refresh. */
  private Point lastVisiblePoint = null;

  /** Container of the tree. */
  private JScrollPane pane;

  /** Selected node of the tree. */
  private DefaultMutableTreeNode selectedNode = null;

  /** Auto refresh button. */
  private JButton autoRefresh;

  /** URL to link to Robustness servlet. */
  private static URL url1 = null;

  /** Parameters to link to the servlet. */
  private static String host = "localhost";
  private static String port = "8800";
  private static String agent = "TestAgent";

  /** This panel. */
  private static JPanel mainPane;

  /** Save topology of the whole society. This list is used to check if one node is contained
   *  in a host or one agent is contained in a node.*/
  private static Hashtable topology;

  /** Save all elements been relocated but still not executed in the real society. */
  private static Hashtable dndList = new Hashtable();

  public RobustnessUI()
  {
    mainPane = this;
    pane = new JScrollPane();
    final JTree tree = getTreeFromCommunity();
    pane.getViewport().add(tree);

    //get the most recently view position, put the scroll pane to this position
    //during refresh.
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

    /**
     * Button auto refresh: let user select refresh interval then refresh automatically
     * based on this interval. If the auto refresh is on, the button will change to
     * 'stop auto refresh' and let user terminate the auto refresh.
     */
    autoRefresh = new JButton("start auto refresh");
    autoRefresh.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        if(autoRefresh.getText().equals("start auto refresh"))
        {
          String op = JOptionPane.showInputDialog(mainPane, "Please input auto refresh interval (s):", "Input Interval",
            JOptionPane.PLAIN_MESSAGE);
          int interval = 0;
          if(op != null)
          {
            try{
              interval = Integer.parseInt(op) * 1000;
              if(interval <= 0)
              {
                JOptionPane.showMessageDialog(mainPane, "Please input an integer larger than 0.", "error",
                 JOptionPane.ERROR_MESSAGE);
                return;
              }
            }catch(NumberFormatException o)
            {
              JOptionPane.showMessageDialog(mainPane, "Please input an integer.", "error",
                JOptionPane.ERROR_MESSAGE);
              return;
            }
            autoRefresh(interval, tree);
          }
        }
        else //terminate the timer
        {
          timer.interrupt();
        }
      }
    });

    /**
     * button refresh: use this button to refresh the tree.
     */
    JButton refresh = new JButton("      refresh      ");
    refresh.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      { refresh(tree);      }
    });

    JPanel controlP = new JPanel();
    controlP.setBackground(Color.white);
    controlP.add(autoRefresh);
    controlP.add(refresh);

    this.setBackground(Color.white);
    this.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
    this.add(controlP, BorderLayout.SOUTH);
  }

  /**
   * Get structure tree of the society.
   * @return the tree
   */
  private JTree getTreeFromCommunity()
  {
    List nodes = new ArrayList();
    path.clear(); //empty the expanded list of the tree
    nodeandpath.clear(); //empty the node and path list.
    Hashtable table = getInfoFromServlet(); //get hashtable fetehed from the servlet
    topology = (Hashtable)table.get("Topology"); //get topology list of the society.
    DefaultMutableTreeNode root = new DefaultMutableTreeNode();
    final Hashtable c = (Hashtable)table.get("Communities");
    DefaultMutableTreeNode communities = buildCommunitySubTree(c, nodes);
    root.add(communities);
    nodes.add(communities);

    DefaultTreeModel model = new DefaultTreeModel(root);
    final JTree tree = new DNDJTree(model);
    tree.setRowHeight(17);

    //TreeExpansionListener: save the path into list if expanded, remove the path
    //from the list if collapsed.
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
    tree.expandRow(1); //expand the root
    final JPopupMenu popup = new JPopupMenu();
    //command 'vacate': only the host in management community can do a vacate. After
    //a host receives a vacate command, it will send a vacate request to blackboard,
    //the blackboard will choose one host and move all nodes of this host to the
    //selected host.
    final JMenuItem command = new JMenuItem("vacate");
      command.addActionListener(new ActionListener(){
        public void actionPerformed(ActionEvent e)
        {
          String parentCommunityOfSelectedHost = (String)((DefaultMutableTreeNode)selectedNode.getParent().getParent()).getUserObject();
          parentCommunityOfSelectedHost = parentCommunityOfSelectedHost.substring(
            parentCommunityOfSelectedHost.indexOf(" "), parentCommunityOfSelectedHost.length()).trim();
          String mgmtAgent = getMgmtAgentOfCommunity(parentCommunityOfSelectedHost, c);
          String selectedHost = (String)selectedNode.getUserObject();
          publishRequest("vacateHost", selectedHost, mgmtAgent, null);
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
    List restartHosts = getRestartHosts(table); //get all restart hosts in the society

    for(Enumeration enums = table.keys(); enums.hasMoreElements();)
    {
      String name = (String)enums.nextElement(); //community name
      List needDNDList = new ArrayList(); //the list of all nodes under this community who've been drag and drop before refresh
      if(dndList.containsKey(name))
        needDNDList = (List)dndList.get(name);
      DefaultMutableTreeNode cnode = new DefaultMutableTreeNode("Community: " + name);
      nodes.add(cnode);
      try{
        List contents = (List)table.get(name);

        Attributes attributes = (Attributes)contents.get(0); //attributes of this community
        if(attributes.size() > 0)
        {
          DefaultMutableTreeNode attrs = new DefaultMutableTreeNode("attributes");
          nodes.add(attrs);
          for(NamingEnumeration nes = attributes.getAll(); nes.hasMore();)
          {
            try{
              String str;
              Attribute attr = (Attribute)nes.next();
              if(attr.size() == 1) //if this attribute only contains one value, e.g. name xxxx, then put the attribute as a single tree node.
              {
                str = attr.getID() + " " + (String)attr.get();
              }
              else //if the attribute contains several values, e.g. role=manager, member..., the attributeID
                //is set as one tree node and all the values are it's children nodes.
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

        Hashtable entities = (Hashtable)contents.get(1); //entities of this community
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
            String entityName = (String)tempEntity.get(i); //entity name
            DefaultMutableTreeNode nameNode = new DefaultMutableTreeNode(entityName);
            Attributes entityAttributes = (Attributes)entities.get(entityName); //attributes of this entity
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

        Hashtable hosts = (Hashtable)contents.get(2); //hosts and their children
        List needRemoveList = new ArrayList();
        if(hosts.size() > 0)
        {
          DefaultMutableTreeNode hostNode = new DefaultMutableTreeNode("hosts");
          Hashtable namesAndNodes = new Hashtable();
          for(Enumeration enum = hosts.keys(); enum.hasMoreElements();)
          {
            String hostName = (String)enum.nextElement(); //host name
            List nodeNames = new ArrayList();
            EntityInfo hostEI = new EntityInfo(hostName, "host");
            EntityNode hostNameNode = new EntityNode(hostEI);
            namesAndNodes.put(hostName, hostNameNode);
            hostNode.add(hostNameNode);
            nodes.add(hostNameNode);
            Hashtable nodetable = (Hashtable)hosts.get(hostName); //all nodes of this host
            for(Enumeration node_enum = nodetable.keys(); node_enum.hasMoreElements();)
            {
              String nodeName = (String)node_enum.nextElement(); //node name
              nodeNames.add(nodeName);
              EntityInfo nodeEI = new EntityInfo(nodeName, "node");
              EntityNode nodeNameNode = new EntityNode(nodeEI);
              namesAndNodes.put(nodeName, nodeNameNode);
              hostNameNode.add(nodeNameNode);
              nodes.add(nodeNameNode);
              List agents = (List)nodetable.get(nodeName); //all agents of this node
              for(int i=0; i<agents.size(); i++)
              {
                EntityInfo agentEI = new EntityInfo((String)agents.get(i), "agent");
                EntityNode agentNode = new EntityNode(agentEI);
                namesAndNodes.put((String)agents.get(i), agentNode);
                nodeNameNode.add(agentNode);
                nodes.add(nodeNameNode);
              }

              //for every node, check if this node contains some agents who are already
              //relocated by user, if yes, remove the agent from the tree. also check if the node
              //is a target parent of some relocated agents, if yes, add the agent.
              for(int i=0; i<needDNDList.size(); i++)
              {
                String order = Integer.toString(i);
                EntityNode[] ens = (EntityNode[])needDNDList.get(i);
                EntityNode sourceNode = ens[0];
                EntityNode newParentNode = ens[1];
                EntityInfo sourceInfo = (EntityInfo)sourceNode.getUserObject();
                String sourceName = sourceInfo.getName();
                if(sourceInfo.getParent().getName().equals(nodeName) && sourceInfo.getParent().getType()==EntityInfo.NODE)
                {
                  //check if the node still contains the agent, if yes, remove it from the tree.
                  //if no, remove it from the list(which means the agent is relocated by the society).
                  if(agents.contains(sourceName))
                  {//System.out.println("Delete agent " + sourceName + " from node " + nodeName);
                    EntityNode removeNode = (EntityNode)namesAndNodes.get(sourceName);
                    nodeNameNode.remove(removeNode);
                    namesAndNodes.remove(sourceName);
                    nodes.remove(removeNode);
                  }
                  else
                  {
                    if(!needRemoveList.contains(order))
                      needRemoveList.add(order);
                  }
                }
                EntityInfo targetInfo = (EntityInfo)newParentNode.getUserObject();
                if(targetInfo.getName().equals(nodeName) && targetInfo.getType() == EntityInfo.NODE)
                {
                  if(agents.contains(sourceName))
                  {
                    if(!needRemoveList.contains(order))
                      needRemoveList.add(order);
                  }
                  else
                  {//System.out.println("Add agent " + sourceName + " to node " + nodeName);
                    EntityNode newen = new EntityNode(sourceInfo);
                    nodeNameNode.add(newen);
                    namesAndNodes.put(sourceName, newen);
                    nodes.add(newen);
                  }
                }
              }
            }

            if(restartHosts.contains(hostName)) //add the restart node of restart hosts
            {
              String restartName = hostName + "-RestartNode";
              if(!nodeNames.contains(restartName))
              {
                EntityInfo ei = new EntityInfo(hostName + "-RestartNode", "node");
                EntityNode en = new EntityNode(ei);
                hostNameNode.add(en);
                nodes.add(en);
              }
            }
          }
          cnode.add(hostNode);
          nodes.add(hostNode);
        }
        Collections.sort(needRemoveList);
        for(int i=needRemoveList.size()-1; i>=0; i--)
        {
          int j = Integer.parseInt((String)needRemoveList.get(i));
          needDNDList.remove(j);//System.out.println("remove from needDNDList: " + j);
        }

        Hashtable allNodes = (Hashtable)contents.get(3); //all nodes and agents of this community
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

  /**
   * Expand some nodes in the tree. This method is used to expand necessary paths after
   * refresh.
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
       catch(NoSuchElementException e){continue;} //nodes maybe removed by user, ignore it.
      ((DefaultTreeModel)atree.getModel()).reload(node);
    }
  }

  /**
   * Get all agents in current society from the servlet.
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
   * Using the agent from above method, get society information from robustness servlet.
   * @return a hashtable records all information
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
   * @param tree the tree before refresh
   */
  protected void refresh(JTree tree)
  {
    List expandPath = (ArrayList)path.clone();
    Point point = lastVisiblePoint;
    pane.remove(tree);
    pane.revalidate();
    pane.repaint();
    JTree tree2 = getTreeFromCommunity();
    pane.getViewport().add(tree2);
    expandTreeNode(tree2, expandPath);
    if(point != null)
      pane.getViewport().setViewPosition(point);
  }

  /**
   * Send command to servlet and get result from the servlet.
   * @param command the command. Valid commands include:
   *        'checkChange' -- Check if the community has any changes since last checking.
   *        'vacateHost' -- send a vacate request of the selected host
   *        'moveAgent' -- move the selected agents
   * @param param1
   * @param param2
   * @param param3 For 'checkChange', the three parameters are all null. For 'vacateHost',
   *               param1 is name of vacate host, param2 is name of the management agent
   *               of parent community of the host, param3 is null. For 'moveAgent',
   *               param1 is name of the moving agent, param2 is name of source node,
   *               param3 is name of target node.
   * @return a string value of the result.
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

  /**
   * Get name of management agent of given community.
   * @param communityName the community be searched
   * @param communities the list of all information of the society
   * @return name of the management agent
   */
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

  private Thread timer = null; //auto refresh thread
  /**
   * Create a thread to listen the community changing. If the community changes,
   * refresh the tree.
   * @param interval interval to listen the changes.
   * @param tree current society structure tree
   */
  private void autoRefresh(final int interval, final JTree tree)
  {
    timer = new Thread() {
        public void run() {
          try{
           while (true) {
               String result = publishRequest("checkChange", null, null, null);
               if(result.equals("succeed"))
               {
                 refresh(tree);
               }
               sleep(interval);
           }
          }catch(InterruptedException e)
          {
            autoRefresh.setText("start auto refresh");
            timer = null;
            return;
          }
        }
      };
      timer.setPriority(Thread.MIN_PRIORITY);
      autoRefresh.setText("stop auto refresh");
      timer.start();
   }

   /**
    * Is given node is contained in the host?
    * @param hostName the host
    * @param nodeName the node
    * @return a boolean value
    */
   public static boolean isNodeInHost(String hostName, String nodeName)
   {
     Hashtable nodes = (Hashtable)topology.get(hostName);
     return nodes.containsKey(nodeName);
   }

   /**
    * Is given agent is contained in the node
    * @param nodeName the node be checked
    * @param agentName the agent
    * @return a boolean value
    */
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

   /**
    * Get a list of all hosts who contain empty nodes.
    * @param table the list contains all community information
    * @return a list names of all restart hosts.
    */
   private List getRestartHosts(Hashtable table)
   {
     List restartHosts = new ArrayList();
     for(Enumeration enums = table.keys(); enums.hasMoreElements();)
     {
       String name = (String)enums.nextElement();
       Hashtable entities = (Hashtable)((List)table.get(name)).get(1);
       for(Enumeration eenums = entities.keys(); eenums.hasMoreElements();)
       {
         String entityName = (String)eenums.nextElement();
         Attributes entityAttributes = (Attributes)entities.get(entityName);
         try{
           for(NamingEnumeration nes = entityAttributes.getAll(); nes.hasMore();)
           {
             Attribute attr = (Attribute)nes.next();
             if(attr.getID().equals("RestartHosts"))
             {
               if(attr.size() == 1)
                 restartHosts.add(attr.get());
               else
               {
                  for(NamingEnumeration subattrs = attr.getAll(); subattrs.hasMore();)
                  {
                    restartHosts.add((String)subattrs.next());
                  }
                }
             }
           }
         }catch(NamingException e){log.error(e.getMessage());}
       }
     }
     return restartHosts;
   }

   /**
    * Add drag and drop element into the list. This method is used after user does one drag and
    * drop.
    * @param communityName where does the DND happen? (drag source and drop target only can be
    *                      under the same community)
    * @param sourceEN drag source
    * @param targetEN drop target
    */
  protected static void addDNDElement(String communityName, EntityNode sourceEN, EntityNode targetEN)
  {
    if(dndList.containsKey(communityName))
    {
      List elems = (List)dndList.get(communityName);
      elems.add(new EntityNode[]{sourceEN, targetEN});
    }
    else
    {
      List elems = new ArrayList();
      elems.add(new EntityNode[]{sourceEN, targetEN});
      dndList.put(communityName, elems);
    }
  }

  /**
   * Remove one element from drag and drop list. This method is called when the DND
   * element is really relocated in the society.
   * @param communityName where does the DND happen
   * @param sourceEN drag source
   * @param targetEN drop target
   */
  protected static void removeDNDElement(String communityName, EntityNode sourceEN, EntityNode targetEN)
  {
    List elems = (List)dndList.get(communityName);
    for(int i=0; i<elems.size(); i++)
    {
      EntityNode[] ens = (EntityNode[])elems.get(i);
      if(((EntityInfo)ens[0].getUserObject()).equals((EntityInfo)sourceEN.getUserObject())
        && ((EntityInfo)ens[1].getUserObject()).equals((EntityInfo)targetEN.getUserObject()))
      {
        elems.remove(i);
        break;
      }
    }
    if(elems.size() == 0)
      dndList.remove(communityName);
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

  //enable setting distinct font, color for each node.
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
      if(str.startsWith("Community:"))
      {
        this.setFont(new Font("Dialog", Font.BOLD, 12));
        ((JLabel)result).setForeground(Color.blue);
      }
      if(str.equals("attributes") || str.equals("entities") || str.equals("hosts") || str.equals("nodes"))
      {
        ((JLabel)result).setForeground(Color.red);
      }

      return this;
    }
  }

}
