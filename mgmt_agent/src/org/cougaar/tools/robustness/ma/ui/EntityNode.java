package org.cougaar.tools.robustness.ma.ui;

import javax.swing.tree.*;

/**
 * A tree node who contains a transferable object as a user object. This node
 * is used in DNDtree for RobustnessUI.
 */
public class EntityNode extends DefaultMutableTreeNode  {

  public EntityNode(EntityInfo info) {
    super(info);
  }

  /** Override a few methods... */
  public boolean isLeaf() {
    return ((EntityInfo) getUserObject()).isAgent();
  }

  public boolean getAllowsChildren() {
    return !((EntityInfo) getUserObject()).isAgent();
  }

  public void add(DefaultMutableTreeNode child) {
    super.add(child);
    //System.out.println(child + " added to " + this);

    EntityInfo childPI = (EntityInfo) ((EntityNode) child).getUserObject();

    EntityInfo oldParent = childPI.getParent();
    //if (parent != null) oldParent.remove(childPI);

    EntityInfo newParent = (EntityInfo) getUserObject();

    newParent.add(childPI);
  }

  public void remove(DefaultMutableTreeNode child) {
    super.remove(child);
    //System.out.println(child + " removed from " + this);

    EntityInfo childPI = (EntityInfo) ((EntityNode) child).getUserObject();

    EntityInfo ParentPI = (EntityInfo) getUserObject();
    if (parent != null) ParentPI.remove(childPI);
  }
}
