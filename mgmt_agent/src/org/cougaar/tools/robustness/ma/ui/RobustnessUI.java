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
//import java.util.List;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import javax.naming.directory.Attributes;
import javax.naming.directory.Attribute;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import org.w3c.dom.Document;
import org.apache.xml.serialize.XMLSerializer;
import org.apache.xml.serialize.OutputFormat;

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
  //private static Hashtable topology;

  /** Save all elements been relocated but still not executed in the real society. */
  private static Hashtable dndList = new Hashtable();

  /** Record all agents who have one xml window opens. */
  private Hashtable agentsWithXml = new Hashtable();

  /** Records all expanded paths of exist xml windows. */
  private Hashtable expandedXmlPaths = new Hashtable();

  /** Records all mgmt communities and their mgmt agents. */
  private Hashtable mgmtAgents = new Hashtable();

  /** xml document of current right clicked element. */
  private Document tmpXML = null;

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
    Hashtable table = getInfoFromServlet(); //get hashtable fetched from the servlet
    if(table == null) {
      return null;
    }
    java.util.List nodes = new ArrayList();
    path.clear(); //empty the expanded list of the tree
    nodeandpath.clear(); //empty the node and path list.
    //topology = (Hashtable)table.get("Topology"); //get topology list of the society.
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
    tree.addTreeExpansionListener(new MyTreeExpansionListener(path, null));
    tree.setCellRenderer(new ElementTreeCellRenderer());
    tree.expandRow(1); //expand the root
    final JPopupMenu popup1 = new JPopupMenu();
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
          String mgmtAgent = (String)mgmtAgents.get(parentCommunityOfSelectedHost);
          //String selectedHost = (String)selectedNode.getUserObject();
          String selectedHost = ((EntityInfo)selectedNode.getUserObject()).getName();
          publishRequest("vacateHost", selectedHost, mgmtAgent, null);
        }
      });
      popup1.add(command);

    //command 'kill': remove the selected agent from the society.
    final JMenuItem kill = new JMenuItem("kill");
    kill.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        EntityInfo ei = (EntityInfo)selectedNode.getUserObject();
        String parentNode = ei.getParent().getName();
        publishRequest("removeAgent", ei.getName(), parentNode, null);
        ((DefaultTreeModel)tree.getModel()).removeNodeFromParent(selectedNode);
      }
    });

    //command 'view health status xml': show xml format of health status of selected
    //agent in an extra window.
    final JMenuItem viewXML = new JMenuItem("view health status xml");
    viewXML.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        EntityInfo ei = (EntityInfo)selectedNode.getUserObject();
        final String agentName = ei.getName();
        if(tmpXML != null)
        {
          JFrame viewer = new JFrame("XML viewer of " + agentName);
          viewer.setSize(500, 400);
          viewer.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
              agentsWithXml.remove(agentName); }
          });
          DOMTree mytree = new DOMTree();
          mytree.addTreeExpansionListener(new MyTreeExpansionListener(expandedXmlPaths, agentName));
          mytree.setRowHeight(18);
          mytree.setFont(new Font("dialog", Font.PLAIN, 12));
          mytree.setDocument(tmpXML);
          mytree.expandRow(0);
          JScrollPane tree_sp = new JScrollPane(mytree);
          tree_sp.setBackground(Color.white);
          viewer.getContentPane().add(tree_sp, BorderLayout.CENTER);
          viewer.show();
          agentsWithXml.put(agentName, tree_sp);
        }
      }
    });

    final JPopupMenu popup2 = new JPopupMenu();

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
              if(selectedNode == null) {
                return;
              }
              if(!(selectedNode instanceof EntityNode)) {
                return;
              }
              EntityInfo ei = (EntityInfo)((EntityNode)selectedNode).getUserObject();
              if(ei.getType() == EntityInfo.HOST)
              {
                if(ei.isInMgmtCommunity()) {
                  popup1.show(e.getComponent(), e.getX(), e.getY());
                }
              }
              else if(ei.getType() == EntityInfo.AGENT)
              {
                popup2.removeAll();
                if(ei.isInMgmtCommunity())
                {
                  popup2.add(viewXML);
                  popup2.add(kill);
                  String name = ei.getName();
                  tmpXML = fetchXmlFromServlet(name);
                  if(tmpXML != null) {
                      viewXML.setEnabled(true);
                  }
                  else {
                      viewXML.setEnabled(false);
                  }
                }
                else
                {
                   popup2.add(kill);
                }
                popup2.show(e.getComponent(), e.getX(), e.getY());
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
  private DefaultMutableTreeNode buildCommunitySubTree(Hashtable table, java.util.List nodes)
  {
    DefaultMutableTreeNode root = new DefaultMutableTreeNode("Communities");
    Hashtable restartHosts = getRestartHosts(table); //get all restart hosts in the society

    for(Enumeration enums = table.keys(); enums.hasMoreElements();)
    {
      String name = (String)enums.nextElement(); //community name
      java.util.List needDNDList = new ArrayList(); //the list of all nodes under this community who've been drag and drop before refresh
      if(dndList.containsKey(name)) {
        needDNDList = (java.util.List)dndList.get(name);
      }
      boolean isMgmtCommunity = false;
      String mgmtAgent = getMgmtAgentOfCommunity(name, table);
      if(mgmtAgent != null)
      {
        isMgmtCommunity = true;
        mgmtAgents.put(name, mgmtAgent);
      }
      Hashtable emptyNodes = null; //save all empty nodes of this community
      if(DNDJTree.getEmptyNodes().containsKey(name)) {
        emptyNodes = (Hashtable)DNDJTree.getEmptyNodes().get(name);
      }

      DefaultMutableTreeNode cnode = new DefaultMutableTreeNode("Community: " + name);
      nodes.add(cnode);
      try{
        java.util.List contents = (java.util.List)table.get(name);

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
                str = attr.getID() + " = " + (String)attr.get();
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
          java.util.List tempEntity = new ArrayList(); //for sorting the entity names
          for(Enumeration enEnums = entities.keys(); enEnums.hasMoreElements();)
          {
            tempEntity.add((String)enEnums.nextElement());
          }
          Collections.sort(tempEntity);
          for(int i=0; i<tempEntity.size(); i++)
          {
            String entityName = (String)tempEntity.get(i); //entity name
            Attributes entityAttributes = (Attributes)entities.get(entityName); //attributes of this entity
            String parentName = "";
            if(entityName.endsWith(")"))
            {
              parentName = entityName.substring(entityName.indexOf("(")+1, entityName.indexOf(")"));
              entityName = entityName.substring(0, entityName.indexOf("(")).trim();
            }
            //DefaultMutableTreeNode nameNode = new DefaultMutableTreeNode(entityName);
            EntityInfo ei = new EntityInfo(entityName, "agent", entityAttributes, isMgmtCommunity);
            ei.setParent(new EntityInfo(parentName, "node", null, isMgmtCommunity));
            ei.setTitle(entityName + "  (" + parentName + ")");
            EntityNode nameNode = new EntityNode(ei);
            nodes.addAll(nameNode.addAttributes(entityAttributes));
            nodes.add(nameNode);
            entityNode.add(nameNode);
          }
          cnode.add(entityNode);
        }

        Hashtable hosts = (Hashtable)contents.get(2); //hosts and their children
        java.util.List needRemoveList = new ArrayList();
        if(hosts.size() > 0)
        {
          DefaultMutableTreeNode hostNode = new DefaultMutableTreeNode("hosts");
          Hashtable namesAndNodes = new Hashtable();
          for(Enumeration enum = hosts.keys(); enum.hasMoreElements();)
          {
            String hostName = (String)enum.nextElement(); //host name
            java.util.List emptys = null;
            if(emptyNodes != null)
            {
              if(emptyNodes.containsKey(hostName)) {
                emptys = (java.util.List)emptyNodes.get(hostName);
              }
            }

            java.util.List nodeNames = new ArrayList();
            EntityInfo hostEI = new EntityInfo(hostName, "host", null, isMgmtCommunity);
            EntityNode hostNameNode = new EntityNode(hostEI);
            namesAndNodes.put(hostName, hostNameNode);
            hostNode.add(hostNameNode);
            nodes.add(hostNameNode);
            Hashtable nodetable = (Hashtable)hosts.get(hostName); //all nodes of this host
            for(Enumeration node_enum = nodetable.keys(); node_enum.hasMoreElements();)
            {
              String nodeName = (String)node_enum.nextElement(); //node name
              nodeNames.add(nodeName);
              EntityInfo nodeEI = new EntityInfo(nodeName, "node", null, isMgmtCommunity);
              EntityNode nodeNameNode = new EntityNode(nodeEI);
              namesAndNodes.put(nodeName, nodeNameNode);
              hostNameNode.add(nodeNameNode);
              nodes.add(nodeNameNode);
              java.util.List agents = (java.util.List)nodetable.get(nodeName); //all agents of this node
              for(int i=0; i<agents.size(); i++)
              {
                String agentName = (String)agents.get(i);
                Attributes attrs = (Attributes)entities.get(agentName + "  (" + nodeName + ")");
                EntityInfo agentEI = new EntityInfo(agentName, "agent", attrs, isMgmtCommunity);
                EntityNode agentNode = new EntityNode(agentEI);
                nodes.addAll(agentNode.addAttributes(agentEI.getAttributes()));
                namesAndNodes.put((String)agents.get(i), agentNode);
                nodeNameNode.add(agentNode);
                nodes.add(nodeNameNode);
              }
              pendingDND(nodeNameNode, agents, needDNDList, needRemoveList, namesAndNodes, nodes);
            }
            if(isMgmtCommunity)
            {
              if(restartHosts.containsKey(hostName)) //add the restart node of restart hosts
              {
                  String restartName = (String)restartHosts.get(hostName);
                  if(!nodeNames.contains(restartName))
                  {
                    EntityInfo ei = new EntityInfo(restartName, "node", null, isMgmtCommunity);
                    EntityNode en = new EntityNode(ei);
                    hostNameNode.add(en);
                    nodes.add(en);
                    nodeNames.add(restartName);
                    pendingDND(en, new ArrayList(), needDNDList, needRemoveList, namesAndNodes, nodes);
                  }
              }
            }
            //add empty nodes
            if(emptys != null)
            {
              for(int i=0; i<emptys.size(); i++)
              {
                String emptyNode = (String)emptys.get(i);
                if(nodeNames.contains(emptyNode)) {
                  DNDJTree.removeEmptyNode(name, hostName, emptyNode);
                }
                else
                {
                  EntityInfo ei = new EntityInfo(emptyNode, "node", null, isMgmtCommunity);
                  EntityNode en = new EntityNode(ei);
                  hostNameNode.add(en);
                  nodes.add(en);
                  nodeNames.add(emptyNode);
                }
              }
            }
          }

          //add hosts who have restart nodes but not in this community
          if(isMgmtCommunity)
          {
            for(Enumeration rn_enums = restartHosts.keys(); rn_enums.hasMoreElements();)
            {
              String rhost = (String)rn_enums.nextElement();
              if(!hosts.containsKey(rhost))
              {
                EntityInfo rei = new EntityInfo(rhost, "host", null, isMgmtCommunity);
                EntityNode ren = new EntityNode(rei);
                EntityInfo rnei = new EntityInfo((String)restartHosts.get(rhost), "node", null, isMgmtCommunity);
                EntityNode rnen = new EntityNode(rnei);
                ren.add(rnen);
                ren.add(rnen);
                hostNode.add(ren);
                nodes.add(ren);
                pendingDND(rnen, new ArrayList(), needDNDList, needRemoveList, namesAndNodes, nodes);
              }
            }
          }
          //add hosts who have empty nodes but not in this community
          if(emptyNodes != null)
          {
            for(Enumeration empty_enums = emptyNodes.keys(); empty_enums.hasMoreElements();)
            {
              String emptyHost = (String)empty_enums.nextElement();
              if(!hosts.containsKey(emptyHost))
              {
                EntityInfo eei = new EntityInfo(emptyHost, "host", null, isMgmtCommunity);
                EntityNode een = new EntityNode(eei);
                java.util.List emptys = (java.util.List)emptyNodes.get(emptyHost);
                for(int i=0; i<emptys.size(); i++)
                {
                  String emptyNode = (String)emptys.get(i);
                  EntityInfo enei = new EntityInfo(emptyNode, "node", null, isMgmtCommunity);
                  EntityNode enen = new EntityNode(enei);
                  een.add(enen);
                  nodes.add(enen);
                }
                hostNode.add(een);
                nodes.add(een);
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
            java.util.List agents = (java.util.List)allNodes.get(nodeName);
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
   * Check the given node, if this node contains some agents who are alread
   * relocated by user, if yes, remove the agent from the tree. also check if the node
   * is a target parent of some relocated agents, if yes, add the agent.
   */
  private void pendingDND(DefaultMutableTreeNode node, java.util.List agents,
    java.util.List needDNDList, java.util.List needRemoveList,
    Hashtable namesAndNodes, java.util.List nodes)
  {
    String nodeName = ((EntityInfo)node.getUserObject()).getName();
    try{
      for(int i=0; i<needDNDList.size(); i++)
      {
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
             node.remove(removeNode);
             namesAndNodes.remove(sourceName);
             nodes.remove(removeNode);
          }
          else
          {
            if(!needRemoveList.contains(Integer.toString(i))) {
              needRemoveList.add(Integer.toString(i));
            }
          }
        }
        EntityInfo targetInfo = (EntityInfo)newParentNode.getUserObject();
        if(targetInfo.getName().equals(nodeName) && targetInfo.getType() == EntityInfo.NODE)
        {
          if(agents.contains(sourceName))
          {
            if(!needRemoveList.contains(Integer.toString(i))) {
              needRemoveList.add(Integer.toString(i));
            }
          }
          else
          {//System.out.println("Add agent " + sourceName + " to node " + nodeName);
            EntityNode newen = new EntityNode(sourceInfo);
            node.add(newen);
            newen.addAttributes(sourceInfo.getAttributes());
            namesAndNodes.put(sourceName, newen);
            nodes.add(newen);
          }
        }
      }
    }catch(Exception e){;}
  }

  /**
   * Expand some nodes in the tree. This method is used to expand necessary paths after
   * refresh.
   * @param atree the tree
   * @param expandPath the paths need to be expanded
   */
  private void expandTreeNode(JTree atree, java.util.List expandPath)
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
     //URL url1 = new URL("http://" + host + ":" + port + "/agents?scope=all");
     URL url1 = new URL("http://" + host + ":" + port + "/agents?scope=all&format=text");
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
       /* if(name.indexOf("</ol>") != -1) {
          break;
        }
        if(flag)
        {
          name = name.substring(0, name.indexOf("</a>"));
          String cluster = name.substring(name.lastIndexOf(">")+1, name.length());
          if(!cluster.endsWith("Node")) {
            clusterNames.add(cluster);
          }
        }
        if(name.indexOf("<ol>") != -1) {
          flag = true;
        }*/
       clusterNames.add(name);
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
      }catch(IOException e){return null;}
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
       //already checked all agents and no match servlet found.
       if(i == agents.size())
       {
         log.error("Can't find robustness servlet.");
         System.exit(0);
       }
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
      } catch(EOFException e){return idc;}
      catch(Exception e){log.error(e.getMessage());}
    }
    return idc;
  }

  /**
   * Refresh the tree.
   * @param tree the tree before refresh
   */
  protected void refresh(JTree tree)
  {
    java.util.List expandPath = (ArrayList)path.clone();
    Point point = lastVisiblePoint;
    pane.remove(tree);
    pane.revalidate();
    pane.repaint();
    JTree tree2 = getTreeFromCommunity();
    if(tree2 == null) {
      return;
    }
    pane.getViewport().add(tree2);
    expandTreeNode(tree2, expandPath);
    if(point != null) {
      pane.getViewport().setViewPosition(point);
    }

    //also refresh all open xml windows
    for(Enumeration enums = agentsWithXml.keys(); enums.hasMoreElements();)
    {
      String agentName = (String)enums.nextElement();
      JScrollPane xml_sp = (JScrollPane)agentsWithXml.get(agentName);
      Point xmlLastPoint = xml_sp.getViewport().getViewPosition();
      //Document doc = (Document)publishRequest("viewXml", agentName, null, null);
      Document doc = fetchXmlFromServlet(agentName);
      if(doc != null)
      {
        ArrayList expandedPaths = (ArrayList)expandedXmlPaths.get(agentName);
        java.util.List temp = (ArrayList)expandedPaths.clone();
        expandedXmlPaths.remove(agentName);
        xml_sp.remove(xml_sp.getViewport().getComponent(0));
        xml_sp.revalidate();
        xml_sp.repaint();
        DOMTree mytree = new DOMTree();
        mytree.addTreeExpansionListener(new MyTreeExpansionListener(expandedXmlPaths, agentName));
        mytree.setRowHeight(18);
        mytree.setFont(new Font("dialog", Font.PLAIN, 12));
        mytree.setDocument(doc);
        //mytree.expandRow(1);
        Hashtable domtreePaths = mytree.getNodesAndPaths();
        for(int i=0; i<temp.size(); i++)
        {
          String dompath = (String)temp.get(i);
          DefaultMutableTreeNode node = (DefaultMutableTreeNode)domtreePaths.get(dompath);
          mytree.scrollPathToVisible(new TreePath(((DefaultMutableTreeNode)node.getFirstChild()).getPath()));
          ((DefaultTreeModel)mytree.getModel()).reload(node);
        }
        //((DefaultTreeModel)mytree.getModel()).reload();
        xml_sp.getViewport().add(mytree);
        xml_sp.getViewport().setViewPosition(xmlLastPoint);
      }
    }
  }

  /**
   * Send command to servlet and get result from the servlet.
   * @param command the command. Valid commands include:
   *        'checkChange' -- Check if the community has any changes since last checking.
   *        'vacateHost' -- send a vacate request of the selected host
   *        'moveAgent' -- move the selected agents
   *        'removeAgent' -- kill selected agent
   *        'getParentHost' -- get parent host name of given node.
   * @param param1
   * @param param2
   * @param param3 For 'checkChange', the three parameters are all null. For 'vacateHost',
   *               param1 is name of vacate host, param2 is name of the management agent
   *               of parent community of the host, param3 is null. For 'moveAgent',
   *               param1 is name of the moving agent, param2 is name of source node,
   *               param3 is name of target node. For 'removeAgent', param1 is name of the
   *               killed agent, param2 is the node name it belongs. For 'getParentHost',
   *               param1 is the node name.
   * @return an object of the result.
   */
  public static Object publishRequest(String command, String param1, String param2, String param3)
  {
    Object result = null;
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
      else if(command.equals("removeAgent"))
      {
        vs.add(param1);
        vs.add(param2);
      }
      else if(command.equals("viewXml"))
      {
        vs.add(param1);
      }
      else if(command.equals("getParentHost")) {
        vs.add(param1);
      }
      oos.writeObject(vs);
      oos.close();
      //need to fix: if conn.setDoInput(false) and don't request the InputStream,
      //the servlet doesn't work.
      InputStream is = conn.getInputStream();
      ObjectInputStream iis = new ObjectInputStream(is);
      result = iis.readObject();
    }catch(Exception e){
      log.error(e.getMessage());
    }
    return result;
  }

  /**
   * Get health status xml of selected agent through servlet.
   * @param agentName selected agent
   * @return the xml document. 'null' means no health status object for selected agent.
   */
  private Document fetchXmlFromServlet(String agentName)
  {
    Document doc = null;
    //check health status object for this agent in every management agent until find
    //the object.
    for(Iterator it = mgmtAgents.values().iterator(); it.hasNext();)
    {
      String mgmtAgent = (String)it.next();
      try{
        URL url = new URL("http://" + host + ":" + port + "/$" + mgmtAgent + "/robustness");
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(os);
        Vector vs = new Vector();
        vs.add("viewXml");
        vs.add(agentName);
        oos.writeObject(vs);
        oos.close();
        InputStream is = conn.getInputStream();
        ObjectInputStream iis = new ObjectInputStream(is);
        doc = (Document)iis.readObject();
        if(doc != null) {
          return doc;
        }
      }catch(Exception e){
        log.error(e.getMessage());
      }
    }
    log.debug("no health status xml found for: " + agentName);
    return doc;
  }

  /**
   * Get name of management agent of given community.
   * @param communityName the community be searched
   * @param communities the list of all information of the society
   * @return name of the management agent
   */
  private String getMgmtAgentOfCommunity(String communityName, Hashtable communities)
  {
    java.util.List contents = (java.util.List)communities.get(communityName);
    Attributes attrs = (Attributes)contents.get(0);
    try{
     if(attrs.get("CommunityManager") != null) {
      return (String)attrs.get("CommunityManager").get(0);
     }
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
               String result = (String)publishRequest("checkChange", null, null, null);
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
    * Get a list of all hosts who contain empty nodes.
    * @param table the list contains all community information
    * @return a list names of all restart hosts.
    */
   private Hashtable getRestartHosts(Hashtable table)
   {
     Hashtable restartHosts = new Hashtable();
     for(Enumeration enums = table.keys(); enums.hasMoreElements();)
     {
       String name = (String)enums.nextElement(); //community name
       java.util.List contents = (java.util.List)table.get(name);
       Attributes attrs = (Attributes)contents.get(0);
       if(attrs.size() > 0)
       {
         try{
           for(NamingEnumeration nes = attrs.getAll(); nes.hasMore();)
           {
             Attribute attr = (Attribute)nes.next();
             if(attr.getID().equalsIgnoreCase("RestartNode")) //'RestartNode' should only contains one value
             {
               if(attr.size() == 1)
               {
                String restartNode = (String)attr.get();
                //find parent host of this node
                String host = (String)publishRequest("getParentHost", restartNode, null, null);
                restartHosts.put(host, restartNode);
               }
               else
               {
                 for(NamingEnumeration subattrs = attr.getAll(); subattrs.hasMore();)
                 {
                   String restartNode = (String)subattrs.next();
                   String host = (String)publishRequest("getParentHost", restartNode, null, null);
                   restartHosts.put(host, restartNode);
                 }
               }
             }
           }
         }catch(NamingException e){e.printStackTrace();}
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
      java.util.List elems = (java.util.List)dndList.get(communityName);
      elems.add(new EntityNode[]{sourceEN, targetEN});
    }
    else
    {
      java.util.List elems = new ArrayList();
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
    java.util.List elems = (java.util.List)dndList.get(communityName);
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
    if(elems.size() == 0) {
      dndList.remove(communityName);
    }
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
      if(obj == null) {
        return this;
      }
      String str;
      if(obj instanceof EntityInfo)
      {
        str = ((EntityInfo)obj).toString();
        //set color to different health status
        try{
         if(((EntityInfo)obj).isInMgmtCommunity() && ((EntityInfo)obj).getType() == EntityInfo.AGENT)
         {
          for(int i=0; i<node.getChildCount(); i++)
          {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
            String child_uo = (String)child.getUserObject();
            if(child_uo.startsWith("RunState"))
            {
              if(!child_uo.endsWith("NORMAL")) {
                ((JLabel)result).setForeground(Color.MAGENTA);
              }
              else {
                ((JLabel)result).setForeground(Color.black);
              }
              return this;
            }
          }
         }
        }catch(Exception e){return this; }
      }
      else
      {
        str = ((String)obj).trim();
      }
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

  private class MyTreeExpansionListener implements TreeExpansionListener
  {
     private java.util.List uiPaths = null;
     private Hashtable xmlPaths = null;
     private String xmlName = null;
     public MyTreeExpansionListener(Object expandPaths, String agentName)
     {
       if(agentName == null) {
         uiPaths = (java.util.List)expandPaths;
       }
       else
       {
         xmlPaths = (Hashtable)expandPaths;
         xmlName = agentName;
       }
     }

     public void treeExpanded(TreeExpansionEvent e) {
       if(xmlName == null) {
         uiPaths.add(e.getPath().toString());
       }
       else
       {
         if(xmlPaths.containsKey(xmlName)) {
           ((java.util.List)xmlPaths.get(xmlName)).add(e.getPath().toString());
         }
         else
         {
           java.util.List temp = new ArrayList();
           temp.add(e.getPath().toString());
           xmlPaths.put(xmlName, temp);
         }
       }
     }
     public void treeCollapsed(TreeExpansionEvent e) {
      /* if(xmlName == null)
       {
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
       else
       {
         List temp = (List)xmlPaths.get(xmlName);
         temp.remove(e.getPath().toString());
         if(temp.size() == 0)
           xmlPaths.remove(xmlName);
       }*/
       String str = e.getPath().toString();
       int size = 0;
       int unremove = 0;
       java.util.List temp;
       if(xmlName == null)
       {
         size = path.size();
         temp = path;
       }
       else
       {
         temp = (java.util.List)xmlPaths.get(xmlName);
         size = temp.size();
       }
       for(int i=0; i<size; i++)
       {
         if(((String)temp.get(unremove)).startsWith(str.substring(0, str.indexOf("]"))))
         {
             temp.remove(unremove);
         }
         else
         {
             unremove ++;
         }
       }
       if(xmlName != null)
       {
         if(temp.size() == 0) {
           xmlPaths.remove(xmlName);
         }
       }
     }
  }

}
