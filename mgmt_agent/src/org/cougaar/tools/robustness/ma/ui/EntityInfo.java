package org.cougaar.tools.robustness.ma.ui;

import java.io.*;
import java.awt.datatransfer.*;
import java.util.*;

public class EntityInfo implements Transferable, Serializable {

  final public static DataFlavor INFO_FLAVOR =
    new DataFlavor(EntityInfo.class, "Entity Information");

  static DataFlavor flavors[] = {INFO_FLAVOR };

  public static final int HOST = 0;
  public static final int NODE = 1;
  public static final int AGENT = 2;

  private String name = null;
  private int type;
  private EntityInfo Parent = null;
  private Vector Children = null;


  public EntityInfo(String name, String type) {
    Children = new Vector();
    this.name = name;
    if(type.equals("host"))
      this.type = HOST;
    else if(type.equals("node"))
      this.type = NODE;
    else
      this.type = AGENT;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getType()
  { return this.type; }

  public void setType(String type)
  {
    if(type.equals("host"))
      this.type = HOST;
    else if(type.equals("node"))
      this.type = NODE;
    else
      this.type = AGENT;
  }

  public boolean isAgent() {
    return getType()==AGENT;
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
    return new EntityInfo(name, convertTypeToString(type));
  }

  public String convertTypeToString(int type)
  {
    if(type == HOST)
      return "host";
    else if(type == NODE)
      return "node";
    else
      return "agent";
  }

  public String toString() {
    return name;
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
