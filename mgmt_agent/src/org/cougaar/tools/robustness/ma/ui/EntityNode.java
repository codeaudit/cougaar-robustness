package org.cougaar.tools.robustness.ma.ui;

import javax.swing.tree.*;
import javax.naming.directory.Attributes;
import javax.naming.directory.Attribute;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import java.util.List;
import java.util.ArrayList;
import java.util.NoSuchElementException;

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
    //return ((EntityInfo) getUserObject()).isAgent();
    return false;
  }

  public boolean getAllowsChildren() {
    //return !((EntityInfo) getUserObject()).isAgent();
    return true;
  }

  public void add(DefaultMutableTreeNode child) {
    super.add(child);
    //System.out.println(child + " added to " + this);

    if(child instanceof EntityNode)
    {
      EntityInfo childPI = (EntityInfo) ((EntityNode) child).getUserObject();

      EntityInfo oldParent = childPI.getParent();
      //if (parent != null) oldParent.remove(childPI);

      EntityInfo newParent = (EntityInfo) getUserObject();

      newParent.add(childPI);
    }
  }

  public void remove(DefaultMutableTreeNode child) {
    super.remove(child);
    //System.out.println(child + " removed from " + this);

    EntityInfo childPI = (EntityInfo) ((EntityNode) child).getUserObject();

    EntityInfo ParentPI = (EntityInfo) getUserObject();
    if (parent != null) ParentPI.remove(childPI);
  }

  /**
   * Add attributes to this node. This method is used only when the node is an agent.
   * @param attrs
   * @return
   */
  public List addAttributes(Attributes attrs)
  {
    List nodes = new ArrayList();
    try{
      for(NamingEnumeration nes = attrs.getAll(); nes.hasMore();)
      {
        try{
          Attribute attr = (Attribute)nes.next();
          String str;
          if(attr.size() == 1)
          {
            str = attr.getID() + " = " + (String)attr.get();
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
          add(nvnode);
          nodes.add(nvnode);
        }catch(NoSuchElementException e){continue;} //in case the attribute doesn't have a value
      }
    }catch(NamingException e){e.printStackTrace();}
    return nodes;
  }
}
