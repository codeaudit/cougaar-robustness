package org.cougaar.tools.robustness.ma.ui;

import java.io.*;
import java.awt.datatransfer.*;
import java.util.*;
import javax.naming.directory.Attributes;
import javax.naming.directory.Attribute;

/**
 * Transferable object represents one element in society. This object is used
 * in DNDtree for RobustnessUI.
 */
public class EntityInfo implements Transferable, Serializable {

  final public static DataFlavor INFO_FLAVOR =
    new DataFlavor(EntityInfo.class, "Entity Information");

  static DataFlavor flavors[] = {INFO_FLAVOR};

  public static final int HOST = 0;
  public static final int NODE = 1;
  public static final int AGENT = 2;

  private String name = null; //element name
  private int type; //element type
  private EntityInfo Parent = null; //parent of the element
  private Vector Children = null; //children of the element
  private Attributes attrs = null; //attributes of agent
  private boolean isInMgmtCommunity = false; //if this entity is in management community
  private String title; //the string showed in the tree

  public EntityInfo(String name, String type, Attributes attrs, boolean isInMgmtCommunity) {
    Children = new Vector();
    this.name = name;
    this.title = name; //set title default to name of the entity, user can change it by setTitle().
    this.isInMgmtCommunity = isInMgmtCommunity;
    if(type.equals("host")) {
      this.type = HOST;
    }
    else if(type.equals("node")) {
      this.type = NODE;
    }
    else
    {
      this.type = AGENT;
      this.attrs = attrs;
    }
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public int getType()
  { return this.type; }

  public Attributes getAttributes()
  { return this.attrs; }

  public void setType(String type)
  {
    if(type.equals("host")) {
      this.type = HOST;
    }
    else if(type.equals("node")) {
      this.type = NODE;
    }
    else {
      this.type = AGENT;
    }
  }

  public boolean isAgent() {
    return getType()==AGENT;
  }

  public boolean isInMgmtCommunity() {
    return this.isInMgmtCommunity;
  }

  public void add(EntityInfo info) {
    info.setParent(this);
    Children.add(info);
  }

  public void remove(EntityInfo info) {
    info.setParent(null);
    Children.remove(info);
  }

  public EntityInfo getParent() {
    return Parent;
  }

  public void setParent(EntityInfo parent) {
    Parent = parent;
  }

  public Vector getChildren() {
    return Children;
  }

  public Object clone() {
    return new EntityInfo(name, convertTypeToString(type), attrs, isInMgmtCommunity);
  }

  public String convertTypeToString(int type)
  {
    if(type == HOST) {
      return "host";
    }
    else if(type == NODE) {
      return "node";
    }
    else {
      return "agent";
    }
  }

  public String toString() {
    return title;
  }

  public boolean equals(EntityInfo newei)
  {
    return this.name.equals(newei.getName()) && this.type == newei.getType();
  }


  // --------- Transferable --------------

  public boolean isDataFlavorSupported(DataFlavor df) {
    return df.equals(INFO_FLAVOR);
  }

  /** implements Transferable interface */
  public Object getTransferData(DataFlavor df)
      throws UnsupportedFlavorException, IOException {
    if (df.equals(INFO_FLAVOR)) {
      return this;
      //return infos;
    }
    else throw new UnsupportedFlavorException(df);
  }

  /** implements Transferable interface */
  public DataFlavor[] getTransferDataFlavors() {
    return flavors;
  }

  // --------- Serializable --------------

   private void writeObject(java.io.ObjectOutputStream out) throws IOException {
     out.defaultWriteObject();
   }

   private void readObject(java.io.ObjectInputStream in)
     throws IOException, ClassNotFoundException {
     in.defaultReadObject();
   }
}
