package org.cougaar.tools.robustness.ma.ui;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.metal.*;
import javax.swing.tree.*;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.dnd.peer.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class DNDJTree extends JTree
                  implements TreeSelectionListener,
                  DragGestureListener, DropTargetListener,
                  DragSourceListener, Autoscroll {


  /** Stores the parent Frame of the component */
  private JPanel Parent = null;

  /** Stores the selected node info */
  protected TreePath SelectedTreePath = null;
  protected EntityNode SelectedNode = null;

  /** Variables needed for DnD */
  private DragSource dragSource = null;
  private DragSourceContext dragSourceContext = null;

  //private Timer _timerHover;

  public DNDJTree(DefaultTreeModel model, JPanel parent)
  {
    super(model);
    Parent = parent;

    addTreeSelectionListener(this);
    dragSource = DragSource.getDefaultDragSource() ;
    DragGestureRecognizer dgr =
      dragSource.createDefaultDragGestureRecognizer(
        this,                             //DragSource
        DnDConstants.ACTION_COPY_OR_MOVE, //specifies valid actions
        this                              //DragGestureListener
      );


    /* Eliminates right mouse clicks as valid actions - useful especially
     * if you implement a JPopupMenu for the JTree
     */
    dgr.setSourceActions(dgr.getSourceActions() & ~InputEvent.BUTTON3_MASK);

    /* First argument:  Component to associate the target with
     * Second argument: DropTargetListener
    */
    DropTarget dropTarget = new DropTarget(this, this);
  }

  /** Returns The selected node */
  public EntityNode getSelectedNode() {
    return SelectedNode;
  }

  private String getParentCommunityOfNode(EntityNode node) {
    EntityInfo info = (EntityInfo)node.getUserObject();
    String comm = null;
    if(info.getType() == EntityInfo.HOST)
      comm = (String)((DefaultMutableTreeNode)node.getParent().getParent()).getUserObject();
    else if(info.getType() == EntityInfo.NODE)
      comm = (String)((DefaultMutableTreeNode)node.getParent().getParent().getParent()).getUserObject();
    else if(info.getType() == EntityInfo.AGENT)
      comm = (String)((DefaultMutableTreeNode)node.getParent().getParent().getParent().getParent()).getUserObject();
    if(comm == null)
      return null;
    return comm.substring(comm.indexOf(": ")+1, comm.length()).trim();
  }

  ///////////////////////// Interface stuff ////////////////////


  /** DragGestureListener interface method */
  public void dragGestureRecognized(DragGestureEvent e) {
    //Get the selected node
    EntityNode dragNode = getSelectedNode();
    if (dragNode != null) {
      //the node moving is still not avaliable, ignore it
      if(((EntityInfo)dragNode.getUserObject()).getType() == EntityInfo.NODE)
        return;

      //Get the Transferable Object
      Transferable transferable = (Transferable) dragNode.getUserObject();

    /*  //Select the appropriate cursor;
      Cursor cursor = DragSource.DefaultCopyNoDrop;
      //Cursor cursor = DragSource.DefaultCopyDrop;
      int action = e.getDragAction();
      if (action == DnDConstants.ACTION_MOVE)
        cursor = DragSource.DefaultMoveNoDrop;*/

      //begin the drag
      dragSource.startDrag(e, null, transferable, this);
    }
  }

  /** DragSourceListener interface method */
  public void dragDropEnd(DragSourceDropEvent dsde) {
  }

  /** DragSourceListener interface method */
  public void dragEnter(DragSourceDragEvent dsde) {
    /*Point pt = dsde.getLocation();
    final TreePath path = getClosestPathForLocation(pt.x, pt.y);
     _timerHover = new Timer(1000, new ActionListener()
     {
       public void actionPerformed(ActionEvent e)
       {
         if (isRootPath(path))
           return;	// Do nothing if we are hovering over the root node
         if (isExpanded(path))
           collapsePath(path);
         else
           expandPath(path);
       }
     });
     _timerHover.setRepeats(false);	// Set timer to one-shot mode*/
  }

  /** DragSourceListener interface method */
  public void dragOver(DragSourceDragEvent dsde) {
  }

  /** DragSourceListener interface method */
  public void dropActionChanged(DragSourceDragEvent dsde) {
  }

  /** DragSourceListener interface method */
  public void dragExit(DragSourceEvent dsde) {
  }




  /** DropTargetListener interface method - What we do when drag is released */
  public void drop(DropTargetDropEvent e) {
    try {
      //_timerHover.stop();
      Transferable tr = e.getTransferable();

      //flavor not supported, reject drop
      if (!tr.isDataFlavorSupported( EntityInfo.INFO_FLAVOR)) e.rejectDrop();

      //cast into appropriate data type
      EntityInfo childInfo =
        (EntityInfo) tr.getTransferData( EntityInfo.INFO_FLAVOR );

      //get new parent node
      Point loc = e.getLocation();
      TreePath destinationPath = getPathForLocation(loc.x, loc.y);

      final String msg = testDropTarget(destinationPath, SelectedTreePath);
      if (msg != null) {
        System.out.println(msg);
        e.rejectDrop();

        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            JOptionPane.showMessageDialog(
                 Parent, msg, "Error Dialog", JOptionPane.ERROR_MESSAGE
            );
          }
        });
        return;
      }


      EntityNode newParent =
        (EntityNode) destinationPath.getLastPathComponent();

      //get old parent node
      EntityNode oldParent = (EntityNode) getSelectedNode().getParent();

      //do real job in blackboard
      RobustnessUI.publishRequest("moveAgent", ((EntityInfo)getSelectedNode().getUserObject()).getName(),
        ((EntityInfo)oldParent.getUserObject()).getName(), ((EntityInfo)newParent.getUserObject()).getName());


      int action = e.getDropAction();
      boolean copyAction = (action == DnDConstants.ACTION_COPY);

      //make new child node
      EntityNode newChild = new EntityNode(childInfo);
      Vector children = childInfo.getChildren();
      Vector copy = (Vector)children.clone();
      childInfo.getChildren().clear();
      for(int i=0; i<copy.size(); i++)
      {
        EntityInfo ei = (EntityInfo)copy.get(i);
        EntityNode en = new EntityNode(ei);
        newChild.add(en);
      }

      try {
        if (!copyAction) oldParent.remove(getSelectedNode());
        newParent.add(newChild);

        if (copyAction) e.acceptDrop (DnDConstants.ACTION_COPY);
        else e.acceptDrop (DnDConstants.ACTION_MOVE);
      }
      catch (java.lang.IllegalStateException ils) {
        e.rejectDrop();
      }

      e.getDropTargetContext().dropComplete(true);

      //expand nodes appropriately - this probably isnt the best way...
      DefaultTreeModel model = (DefaultTreeModel) getModel();
      model.reload(oldParent);
      model.reload(newParent);
      TreePath parentPath = new TreePath(newParent.getPath());
      expandPath(parentPath);
    }
    catch (IOException io) { e.rejectDrop(); }
    catch (UnsupportedFlavorException ufe) {e.rejectDrop();}
  } //end of method


  /** DropTaregetListener interface method */
  public void dragEnter(DropTargetDragEvent e) {
  }

  /** DropTaregetListener interface method */
  public void dragExit(DropTargetEvent e) {
  }

  /** DropTaregetListener interface method */
  public void dragOver(DropTargetDragEvent e) {
    //set cursor location. Needed in setCursor method
    Point cursorLocationBis = e.getLocation();
        TreePath destinationPath =
      getPathForLocation(cursorLocationBis.x, cursorLocationBis.y);

    //_timerHover.restart();


    // if destination path is okay accept drop...
    if (testDropTarget(destinationPath, SelectedTreePath) == null){
    	e.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE ) ;
    }
    // ...otherwise reject drop
    else {
    	e.rejectDrag() ;
    }
  }

  /** DropTaregetListener interface method */
  public void dropActionChanged(DropTargetDragEvent e) {
  }


  /** TreeSelectionListener - sets selected node */
  public void valueChanged(TreeSelectionEvent evt) {
    SelectedTreePath = evt.getNewLeadSelectionPath();
    if (SelectedTreePath == null) {
      SelectedNode = null;
      return;
    }
    DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)SelectedTreePath.getLastPathComponent();
    if(selectedNode instanceof EntityNode && selectedNode.getParent() instanceof EntityNode)
      SelectedNode = (EntityNode)selectedNode;
    else
    {
      SelectedNode = null;
      return;
    }
    //SelectedNode =
      //(EntityNode)SelectedTreePath.getLastPathComponent();
  }

  /** Convenience method to test whether drop location is valid
  @param destination The destination path
  @param dropper The path for the node to be dropped
  @return null if no problems, otherwise an explanation
  */
  private String testDropTarget(TreePath destination, TreePath dropper) {
    //Typical Tests for dropping
    //Test 1.
    boolean destinationPathIsNull = destination == null;
    if (destinationPathIsNull)
      return "Invalid drop location.";

    //Test 2.
   /* EntityNode node = (EntityNode) destination.getLastPathComponent();
    if ( !node.getAllowsChildren() )
      return "This node does not allow children";*/
    DefaultMutableTreeNode node = (DefaultMutableTreeNode)destination.getLastPathComponent();
    if(!(node instanceof EntityNode))
      return "This node cannot be a target";
    else if(!node.getAllowsChildren())
      return "This node does not allow children";
    String comm = getParentCommunityOfNode((EntityNode)node);
    if(comm == null)
      return "This node cannot be a target";
    if(!comm.equals(getParentCommunityOfNode(getSelectedNode())))
          return "Node only can be moved under same community";
    else{
      EntityInfo targetEI = (EntityInfo)node.getUserObject();
      //a node only can be a child of a host
      EntityInfo ei = (EntityInfo)getSelectedNode().getUserObject();
      if(ei.getType() == EntityInfo.NODE)
      {
        if(targetEI.getType() == EntityInfo.NODE)
          return "This node does not allow a node child";
        //this node is not allowed to move to one host who already contains the same node.
        if(RobustnessUI.isNodeInHost(targetEI.getName(), ei.getName()))
            return "The target host already contains a same node.";
      }
      //an agent only can be a child of a node
      if(ei.getType() == EntityInfo.AGENT)
      {
        if(targetEI.getType() == EntityInfo.HOST)
          return "This node does not allow an agent child";
        if(RobustnessUI.isAgentInNode(targetEI.getName(), ei.getName()))
            return "The target node already contains a same agent.";
      }
    }

    if (destination.equals(dropper))
      return "Destination cannot be same as source";

    //Test 3.
    if ( dropper.isDescendant(destination))
       return "Destination node cannot be a descendant.";

    //Test 4.
    if ( dropper.getParentPath().equals(destination))
       return "Destination node cannot be a parent.";

    return null;
  }

  private boolean isRootPath(TreePath path)
  {
     return isRootVisible() && getRowForPath(path) == 0;
  }

    public static final Insets scrollInsets = new Insets( 8, 8, 8, 8 );
    // Implementation of Autoscroll interface
    public Insets getAutoscrollInsets( )
    {
      Rectangle r = getVisibleRect( );
      Dimension size = getSize( );
      Insets i = new Insets( r.y + scrollInsets.top, r.x + scrollInsets.left,
         size.height - r.y - r.height + scrollInsets.bottom,
         size.width - r.x - r.width + scrollInsets.right );
      return i;
    }

    public void autoscroll( Point location )
    {
      JScrollPane scroller =
         (JScrollPane)SwingUtilities.getAncestorOfClass( JScrollPane.class, this );
      if ( scroller != null )
       {
          JScrollBar hBar = scroller.getHorizontalScrollBar( );
          JScrollBar vBar = scroller.getVerticalScrollBar( );
          Rectangle r = getVisibleRect( );
          if ( location.x <= r.x + scrollInsets.left )
          {
             // Need to scroll left
             hBar.setValue( hBar.getValue( ) - hBar.getUnitIncrement( -1 ) );
          }
          if ( location.y <= r.y + scrollInsets.top )
          {
            // Need to scroll up
            vBar.setValue( vBar.getValue( ) - vBar.getUnitIncrement( -1 ) );
          }
          if ( location.x >= r.x + r.width - scrollInsets.right )
          {
            // Need to scroll right
            hBar.setValue( hBar.getValue( ) + hBar.getUnitIncrement( 1 ) );
          }
          if ( location.y >= r.y + r.height - scrollInsets.bottom )
          {
            // Need to scroll down
            vBar.setValue( vBar.getValue( ) + vBar.getUnitIncrement( 1 ) );
          }
        }
      }

  //program entry point
  public static final void main(String args[]) {

    JFrame f = new JFrame();
    f.setSize(500, 400);
    Container c = f.getContentPane();
    JPanel pane = new JPanel(new BorderLayout());

    //Generate your family "tree"
    DefaultMutableTreeNode root = new DefaultMutableTreeNode("Communities");
    DefaultMutableTreeNode com1 = new DefaultMutableTreeNode("Community: com1");
    DefaultMutableTreeNode com2 = new DefaultMutableTreeNode("Community: com2");
    DefaultMutableTreeNode attr1 = new DefaultMutableTreeNode("attributes");
    DefaultMutableTreeNode name = new DefaultMutableTreeNode("Name com1");
    DefaultMutableTreeNode type = new DefaultMutableTreeNode("Type community");
    DefaultMutableTreeNode hosts1 = new DefaultMutableTreeNode("hosts");
    EntityInfo ei;
    ei = new EntityInfo("qing", "host");
    EntityNode en1 = new EntityNode(ei);
    ei = new EntityInfo("ManagerNode1", "node");
    EntityNode en2 = new EntityNode(ei);
    ei = new EntityInfo("ManagerAgent1", "agent");
    EntityNode en3 = new EntityNode(ei);
    ei = new EntityInfo("ManagerAgent2", "agent");
    EntityNode en4 = new EntityNode(ei);
    ei = new EntityInfo("ManagerNode2", "node");
    EntityNode en5 = new EntityNode(ei);
    ei = new EntityInfo("ron", "host");
    EntityNode ron = new EntityNode(ei);
    ei = new EntityInfo("SecurityNode1", "node");
    EntityNode sen2 = new EntityNode(ei);
    ei = new EntityInfo("SecurityAgent1", "agent");
    EntityNode sen3 = new EntityNode(ei);
    ei = new EntityInfo("SecurityAgent2", "agent");
    EntityNode sen4 = new EntityNode(ei);
    ei = new EntityInfo("SecurityNode2", "node");
    EntityNode sen5 = new EntityNode(ei);

    DefaultMutableTreeNode hosts2 = new DefaultMutableTreeNode("hosts");
    ei = new EntityInfo("qing", "host");
    EntityNode en6 = new EntityNode(ei);
    ei = new EntityInfo("ManagerNode1", "node");
    EntityNode en7 = new EntityNode(ei);
    ei = new EntityInfo("ManagerAgent1", "agent");
    EntityNode en8 = new EntityNode(ei);
    ei = new EntityInfo("ManagerAgent2", "agent");

    attr1.add(name);
    attr1.add(type);
    com1.add(attr1);
    en2.add(en3);
    en2.add(en4);
    en1.add(en2);
    en1.add(en5);
    hosts1.add(en1);
    sen2.add(sen3);
    sen2.add(sen4);
    ron.add(sen2);
    ron.add(sen5);
    hosts1.add(ron);
    com1.add(hosts1);
    root.add(com1);

    en6.add(en7);
    en6.add(en8);
    hosts2.add(en6);
    com2.add(hosts2);
    root.add(com2);


    //add the tree to the frame and display the frame.
    DefaultTreeModel model = new DefaultTreeModel(root);
    pane.add(new DNDJTree(model, pane), BorderLayout.CENTER);
    c.add(pane);
    f.pack();
    f.show();
    f.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        System.exit(0); }
     });
  }

} //end of DnDJTree
