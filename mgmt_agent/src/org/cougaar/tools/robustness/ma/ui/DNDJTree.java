/*
 * <copyright>
 *  Copyright 2001-2002 Mobile Intelligence Corp.
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 *
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

import java.awt.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.naming.directory.Attributes;
import org.cougaar.util.log.Logger;
import org.cougaar.tools.csmart.ui.viewer.CSMART;

import org.cougaar.tools.csmart.ui.tree.DNDTree;
import org.cougaar.tools.csmart.ui.tree.DMTNArray;

/**
 * A drag-and-drop tree. This tree is used in RobustnessUI.
 */
public class DNDJTree extends JTree
   implements DropTargetListener,DragSourceListener,DragGestureListener, Autoscroll
{
  DropTarget dropTarget = null;  // enables component to be a dropTarget
  DragSource dragSource = null;  // enables component to be a dragSource
  DefaultMutableTreeNode[] dragSourceNodes;     // The (TreeNodes) that are being dragged from here
  private static Hashtable emptyNodes = new Hashtable(); //saves all empty nodes after DND

  private transient Logger log;

  /**
   * Initialize DropTarget and DragSource and JTree.
   */

  public DNDJTree(DefaultTreeModel model) {
    super(model);
    log = CSMART.createLogger(this.getClass().getName());
    setUI(new SpecialMetalTreeUI());
    dropTarget = new DropTarget(this, this);
    dragSource = new DragSource();
    dragSource.createDefaultDragGestureRecognizer
      (this,
       DnDConstants.ACTION_COPY_OR_MOVE,
       this);
  }

  private class SpecialMetalTreeUI extends javax.swing.plaf.metal.MetalTreeUI {
    public SpecialMetalTreeUI() {
      super();
    }

    protected MouseListener createMouseListener() {
      return new SpecialMouseHandler();
    }

    public class SpecialMouseHandler extends MouseHandler {
      MouseEvent pressEvent;
      public void mousePressed(MouseEvent e) {
        pressEvent = e;
        dragSourceNodes = null;
      }
      public void mouseReleased(MouseEvent e) {
        if (dragSourceNodes == null && pressEvent != null)
        { super.mousePressed(pressEvent); }
      }
    }
  }

  /** Utility to select a node in the tree **/
  public void selectNode(TreeNode node) {
    DefaultTreeModel model = (DefaultTreeModel) getModel();
    TreePath path = new TreePath(model.getPathToRoot(node));
    setSelectionPath(path);
  }

  /** Utility to expand a node in the tree **/
  public void expandNode(TreeNode node) {
    DefaultTreeModel model = (DefaultTreeModel) getModel();
    TreePath path = new TreePath(model.getPathToRoot(node));
    expandPath(path);
  }

  protected int isDroppable(DataFlavor[] o, DefaultMutableTreeNode target)
  {
    Object obj = target.getUserObject();
    if(obj instanceof EntityInfo)
    {
      EntityInfo ei = (EntityInfo)obj;
      for(int i=0; i<o.length; i++)
      {
        if( o[i].equals(EntityInfo.INFO_FLAVOR) || o[i].equals(EntityInfoArray.INFO_ARRAY_FLAVOR))
        {
          if(dragSourceNodes[0] instanceof EntityNode)
          {
            EntityNode sen = (EntityNode)dragSourceNodes[0];
            //components only can be relocated in the same community
            if(!getParentCommunityOfNode(sen).equals(getParentCommunityOfNode((EntityNode)target)))
            {  return DnDConstants.ACTION_NONE; }
            //components only can be relocated to different parents
            for(int j=0; j<dragSourceNodes.length; j++)
            {
              EntityNode temp = (EntityNode)dragSourceNodes[j];
              if((new TreePath(target.getPath()))
                .equals(new TreePath(((DefaultMutableTreeNode)temp.getParent()).getPath())))
              {  return DnDConstants.ACTION_NONE; }
            }

            EntityInfo sei = (EntityInfo)sen.getUserObject();
            if(sei.getType() == EntityInfo.AGENT) //an agent only can be a child of a node
              if(ei.getType() == EntityInfo.NODE)
              { return DnDConstants.ACTION_MOVE; }
            if(sei.getType() == EntityInfo.NODE) //a node only can be a child of a host
              if(ei.getType() == EntityInfo.HOST)
              {  return DnDConstants.ACTION_MOVE; }
          }
        }
      }
    }
    return DnDConstants.ACTION_NONE;
  }

  public int addElement(Transferable transferable, DefaultMutableTreeNode target,
                        DefaultMutableTreeNode before)
  {
    Object data = null;
    try {
      data = transferable.getTransferData(transferable.getTransferDataFlavors()[0]);
    } catch (Exception e) {
      e.printStackTrace();
      return DnDConstants.ACTION_NONE;
    }
    if (data instanceof EntityInfoArray) {
      EntityInfoArray infos = (EntityInfoArray) data;
      EntityNode[] nodes = infos.getNodes();
      for (int i = 0; i < nodes.length; i++) {
        addElement(nodes[i], target, before);
      }
    } else {
      EntityInfo info = (EntityInfo)data;
      EntityNode en = new EntityNode(info);
      addElement(en, target, before);
    }
    return DnDConstants.ACTION_MOVE;
  }

  private void addElement(DefaultMutableTreeNode source,
                          DefaultMutableTreeNode target,
                          DefaultMutableTreeNode before)
  {
    EntityInfo cto = (EntityInfo) source.getUserObject();
    EntityNode newNode =
      new EntityNode(cto);
    Attributes attrs = cto.getAttributes();
    if(attrs != null) {
      newNode.addAttributes(attrs);
    }
    int ix = target.getChildCount(); // Drop at end by default
    DefaultTreeModel model = (DefaultTreeModel) getModel();
    if (before != null) {       // If before specified, put it there.
      ix = model.getIndexOfChild(target, before);
    }
    model.insertNodeInto(newNode, target, ix);
    int n = source.getChildCount();
    DefaultMutableTreeNode[] children = new DefaultMutableTreeNode[n];
    for (int i = 0; i < n; i++) {
      children[i] = (DefaultMutableTreeNode) source.getChildAt(i);
    }
    for (int i = 0; i < n; i++) {
      model.insertNodeInto(children[i], newNode, i);
    }
    scrollPathToVisible(new TreePath(newNode.getPath()));
  }


  private DefaultMutableTreeNode getDropTarget(Point location) {
    TreePath path = getPathForLocation(location.x, location.y);
    if (path != null) {
      return (DefaultMutableTreeNode) path.getLastPathComponent();
    } else if (isRootVisible()) {
      return null;
    } else {
      return (DefaultMutableTreeNode) ((DefaultTreeModel) getModel()).getRoot();
    }
  }

  /**
   * Return the action that is appropriate for this intended drop
   **/
  private int testDrop(DropTargetDragEvent event) {
    DataFlavor[] possibleFlavors = event.getCurrentDataFlavors();
    DefaultMutableTreeNode target = getDropTarget(event.getLocation());
    if (target != null) {
      if (target.getAllowsChildren()) {
        int action = isDroppable(possibleFlavors, target);
        // if can't drop on the target, see if you can drop on its parent
        if (action == DnDConstants.ACTION_NONE) {
          target = (DefaultMutableTreeNode)target.getParent();
          if (target != null) {
            action = isDroppable(possibleFlavors, target);
          }
          return action;
        }
        return action;
      } else {
	// target doesn't allow children, but maybe can be sibling
        target = (DefaultMutableTreeNode) target.getParent();
	if (target.getAllowsChildren()) {
          int action = isDroppable(possibleFlavors, target);
          return action;
	}
      }
    }
    return DnDConstants.ACTION_NONE;
  }

  /**
   * Return true if the target allows this flavor.
   */
  private boolean isAllowed(DataFlavor[] possibleFlavors,
                            DefaultMutableTreeNode target) {
    if (target.getAllowsChildren()) {
      int result = isDroppable(possibleFlavors, target);
      return (result == DnDConstants.ACTION_MOVE ||
              result == DnDConstants.ACTION_COPY);
    } else {
      return false;
      }
  }

  /**
   * A drop has occurred. Determine if the tree can accept the dropped item
   * and if so, insert the item into the tree, otherwise reject the item.
   */
  public void drop(DropTargetDropEvent event) {
    int action = DnDConstants.ACTION_NONE;
    DefaultMutableTreeNode target = getDropTarget(event.getLocation());
    DefaultMutableTreeNode after = null;
    // allow drop if target allows children of this type
    // or if an ancestor allows children of this type
    DataFlavor[] possibleFlavors = event.getCurrentDataFlavors();
    Transferable transferable = event.getTransferable();
    while (target != null) {
      if (isAllowed(possibleFlavors, target)) {
        action = addElement(transferable, target, after);
        break;
      } else {
        after = target;
        target = (DefaultMutableTreeNode)after.getParent();
      }
    }
    boolean success;
    switch (action) {
    case DnDConstants.ACTION_MOVE:
    case DnDConstants.ACTION_COPY:
      event.acceptDrop(action);
      EntityNode newParent = (EntityNode)target;
      for(int i=0; i<dragSourceNodes.length; i++)
      {
        EntityNode source = (EntityNode)dragSourceNodes[i];
        EntityNode oldParent = (EntityNode)source.getParent();
        RobustnessUI.addDNDElement(getParentCommunityOfNode(source), source, newParent);
        //do real job in blackboard
        RobustnessUI.publishRequest("moveAgent", ((EntityInfo)source.getUserObject()).getName(),
          ((EntityInfo)oldParent.getUserObject()).getName(), ((EntityInfo)newParent.getUserObject()).getName());
      }
      success = true;
      break;
    default:
      event.rejectDrop();
      success = false;
      break;
    }
    event.getDropTargetContext().dropComplete(success);
  }


  /**
   * Decide what, if anything, should be dragged. If multi-drag is
   * supported and the node under the mouse is in the current
   * selection we attempt to drag the entire selection. If any node in
   * the selection is not draggable we drag nothing. If multi-drag is
   * not supported or if the node under the mouse is not in the
   * current selection, we drag only the node under the mouse if it is
   * draggable.
   * If tree is not editable, return.
   */

  public void dragGestureRecognized(DragGestureEvent event) {
    InputEvent ie = event.getTriggerEvent();
    // ignore right mouse events
    if ((ie.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
      return;
    }
    DefaultMutableTreeNode target = getDropTarget(event.getDragOrigin());
    if (target == null) {
      return; // Nothing to drag.
    }
    TreePath[] paths = getSelectionPaths();
    boolean doMultiDrag =
      paths != null && paths.length > 1 && supportsMultiDrag();
    if (doMultiDrag) {
      boolean targetIsSelected = false;
      for (int i = 0; i < paths.length; i++) {
        DefaultMutableTreeNode node =
          (DefaultMutableTreeNode) paths[i].getLastPathComponent();
        if (!isDraggable(node)) {
          doMultiDrag = false;
          break;
        }
        if (node == target) {
         targetIsSelected = true;
        }
      }
      if (!targetIsSelected) {
        doMultiDrag = false;
      }
    }
    if (doMultiDrag) {
      dragSourceNodes = new DefaultMutableTreeNode[paths.length];
      for (int i = 0; i < paths.length; i++) {
        dragSourceNodes[i] =
          (DefaultMutableTreeNode) paths[i].getLastPathComponent();
      }
      Transferable draggableObject = null;
      try {
        draggableObject = makeDraggableObject(new DMTNArray(dragSourceNodes));
      } catch (IllegalArgumentException iae) {
        if (log.isErrorEnabled()) {
          log.error("Illegal argument exception: " + iae);
        }
      }
      if (draggableObject == null) {
        return; // if couldn't make draggable object, then return
      }
      dragSource.startDrag(event, null,
                           draggableObject, this);
    } else {
      selectNode(target);
      if (isDraggable(target)) {
        dragSourceNodes = new DefaultMutableTreeNode[] {target};
        Transferable draggableObject = null;
        try {
          draggableObject = makeDraggableObject(target);
        } catch (IllegalArgumentException iae) {
          if (log.isErrorEnabled()) {
            log.error("Illegal argument exception: " + iae);
          }
        }
        if (draggableObject == null) {
          return; // if couldn't make draggable object, then return
        }
        dragSource.startDrag(event, null,
                             draggableObject, this);
      }
    }
  }

  protected boolean supportsMultiDrag() {
    return true;
  }

  public boolean isDraggable(Object selected)
  {
    DefaultMutableTreeNode node = (DefaultMutableTreeNode)selected;
    if(node instanceof EntityNode)
    {
      return (((EntityInfo)((EntityNode)node).getUserObject()).getType() == EntityInfo.AGENT
        && ((EntityNode)selected).getParent() instanceof EntityNode);
    }
    return false;
  }

  /**
   * DragSourceListener and DropTargetListener interface.
   */

  /**
   * Dragging has ended; remove dragged object from tree.
   */

  public void dragDropEnd (DragSourceDropEvent event) {
    if (event.getDropSuccess()
        && event.getDropAction() == DnDConstants.ACTION_MOVE) {
      removeElement();
    }
  }

  /**
   * Removes a dragged element from this tree.
   */

  public void removeElement(){
    if (dragSourceNodes != null) {
      for (int i = 0; i < dragSourceNodes.length; i++) {
        EntityNode parent = (EntityNode)dragSourceNodes[i].getParent();
        ((DefaultTreeModel) getModel())
          .removeNodeFromParent(dragSourceNodes[i]);
        //record the empty node
        if(parent.getChildCount() == 0)
        {
          String node = ((EntityInfo)parent.getUserObject()).getName();
          String host = ((EntityInfo)((EntityNode)parent.getParent()).getUserObject()).getName();
          String community = (String)((DefaultMutableTreeNode)parent.getParent().getParent().getParent()).getUserObject();
          community = community.substring(community.indexOf(" "), community.length()).trim();
          addEmptyNode(community, host, node);
        }
      }
      dragSourceNodes = null;
    }
  }

  /**
   * This message goes to DragSourceListener, informing it that the dragging
   * has entered the DropSite.
   */

  public void dragEnter (DragSourceDragEvent event) {
  }

  /**
   * This message goes to DragSourceListener, informing it that the dragging
   * is currently ocurring over the DropSite.
   */

  public void dragOver (DragSourceDragEvent event) {
  }

  /**
   * This message goes to DragSourceListener, informing it that the dragging
   * has exited the DropSite.
   */

  public void dragExit (DragSourceEvent event) {
  }

  /**
   * is invoked when the user changes the dropAction
   *
   */

  public void dropActionChanged ( DragSourceDragEvent event) {
  }

  /**
   * Dragging over the DropSite.
   */

  public void dragEnter (DropTargetDragEvent event) {
    int action = testDrop(event);
    if (action == DnDConstants.ACTION_NONE) {
      event.rejectDrag();
    }
    else {
      event.acceptDrag(DnDConstants.ACTION_MOVE);
    }
  }

  /**
   * Drag operation is going on.
   */

  public void dragOver (DropTargetDragEvent event) {
    int action = testDrop(event);
    if (action == DnDConstants.ACTION_NONE) {
      event.rejectDrag();
    } else
      event.acceptDrag(DnDConstants.ACTION_MOVE);
  }

  /**
   * Exited DropSite without dropping.
   */

  public void dragExit (DropTargetEvent event) {
  }

  /**
   * User modifies the current drop gesture
   */

  public void dropActionChanged (DropTargetDragEvent event) {
    int action = testDrop(event);
    if (action == DnDConstants.ACTION_NONE) {
      event.rejectDrag();
    }
    else {
      event.acceptDrag(DnDConstants.ACTION_MOVE);
    }
  }

  /**
   * End DragSourceListener interface.
   */

  /**
   * A drag gesture has been initiated.
   */

  public Transferable makeDraggableObject(Object selected)
  {
    if(selected instanceof DefaultMutableTreeNode)
    {
      if(selected instanceof EntityNode) {
        return (EntityInfo)((EntityNode)selected).getUserObject();
      }
    }
    else if(selected instanceof DMTNArray)
    {
       return new EntityInfoArray((DMTNArray)selected);
    }

    throw new IllegalArgumentException("Not a DefaultMutableTreeNode or DMTNArray");
  }

  private String getParentCommunityOfNode(EntityNode node) {
    EntityInfo info = (EntityInfo)node.getUserObject();
    String comm = null;
    if(info.getType() == EntityInfo.HOST) {
      comm = (String)((DefaultMutableTreeNode)node.getParent().getParent()).getUserObject();
    }
    else if(info.getType() == EntityInfo.NODE) {
      comm = (String)((DefaultMutableTreeNode)node.getParent().getParent().getParent()).getUserObject();
    }
    else if(info.getType() == EntityInfo.AGENT) {
      comm = (String)((DefaultMutableTreeNode)node.getParent().getParent().getParent().getParent()).getUserObject();
    }
    if(comm == null) {
      return null;
    }
    return comm.substring(comm.indexOf(": ")+1, comm.length()).trim();
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


    protected static void addEmptyNode(String community, String host, String node)
    {
      if(!emptyNodes.containsKey(community))
      {
        Hashtable hosts = new Hashtable();
        java.util.List nodes = new ArrayList();
        nodes.add(node);
        hosts.put(host, nodes);
        emptyNodes.put(community, hosts);
      }
      else
      {
        Hashtable hosts = (Hashtable)emptyNodes.get(community);
        if(hosts.containsKey(host))
        {
          java.util.List nodes = (java.util.List)hosts.get(host);
          nodes.add(node);
        }
        else
        {
          java.util.List nodes = new ArrayList();
          nodes.add(node);
          hosts.put(host, nodes);
        }
      }
    }

    protected static void removeEmptyNode(String community, String host, String node)
    {
      Hashtable hosts = (Hashtable)emptyNodes.get(community);
      java.util.List nodes = (java.util.List)hosts.get(host);
      nodes.remove(node);
      if(nodes.size() == 0) {
        hosts.remove(host);
      }
      if(hosts.size() == 0) {
        emptyNodes.remove(community);
      }
    }

    protected static Hashtable getEmptyNodes()
    { return emptyNodes; }
}



