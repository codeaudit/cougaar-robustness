package org.cougaar.tools.robustness.ma.ui;

import java.io.*;
import java.awt.datatransfer.*;
import java.util.*;
import org.cougaar.tools.csmart.ui.tree.DMTNArray;

/**
 * Transferable object represents a list of elements in the society. This object
 * is used in DNDtree for RobustnessUI.
 */
public class EntityInfoArray implements Transferable, java.io.Serializable {
  public EntityNode[] enodes;

  final public static DataFlavor INFO_ARRAY_FLAVOR =
    new DataFlavor(EntityInfoArray.class, "Entity Information Array");

  static DataFlavor flavors[] = {INFO_ARRAY_FLAVOR };

    public EntityInfoArray(DMTNArray nodes) {
        this.enodes = new EntityNode[nodes.nodes.length];
        for(int i=0; i<nodes.nodes.length; i++)
        {
          enodes[i] = (EntityNode)nodes.nodes[i];
        }
    }

    public EntityNode[] getNodes()
    { return enodes; }

    // --------- Transferable --------------

  public boolean isDataFlavorSupported(DataFlavor df) {
    return df.equals(INFO_ARRAY_FLAVOR);
  }

  /** implements Transferable interface */
  public Object getTransferData(DataFlavor df)
      throws UnsupportedFlavorException, IOException {
    if (df.equals(INFO_ARRAY_FLAVOR)) {
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
