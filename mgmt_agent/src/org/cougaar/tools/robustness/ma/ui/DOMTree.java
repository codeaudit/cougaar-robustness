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

import java.io.Serializable;
import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.*;

/**
 * Displays a DOM document in a tree control.
 */
public class DOMTree
    extends JTree
    {
    /** Default constructor. */
    public DOMTree() {
        this(null);
        }

    /** Constructs a tree with the specified document. */
    public DOMTree(Document document) {
        super(new Model());

        // set tree properties
        setRootVisible(false);

        // set properties
        setDocument(document);

        } // <init>()

    //
    // Public methods
    //

    /** Sets the document. */
    public void setDocument(Document document) {
        ((Model)getModel()).setDocument(document);
        //expandRow(0);
        }

    /** Returns the document. */
    public Document getDocument() {
        return ((Model)getModel()).getDocument();
        }

    /** get the org.w3c.Node for a MutableTreeNode. */
    public Node getNode(Object treeNode) {
        return ((Model)getModel()).getNode(treeNode);
    }

    /** get all tree nodes and relative tree paths. */
    public Hashtable getNodesAndPaths() {
      Hashtable nodes = ((Model)getModel()).nodeMap;
      Hashtable nodesAndPaths = new Hashtable();
      for(Iterator it = nodes.keySet().iterator(); it.hasNext();)
      {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)it.next();
        TreePath path = new TreePath(node.getPath());
        nodesAndPaths.put(path.toString(), node);
      }
      return nodesAndPaths;
    }

    //
    // Classes
    //

    /**
     * DOM tree model.
     *
     * @author  Andy Clark, IBM
     * @version
     */
    static class Model
        extends DefaultTreeModel
        implements Serializable
        {

        //
        // Data
        //

        /** Document. */
        private Document document;
        /** Node Map. */
        public Hashtable nodeMap = new Hashtable();


        //
        // Constructors
        //

        /** Default constructor. */
        public Model() {
            this(null);
            }

        /** Constructs a model from the specified document. */
        public Model(Document document) {
            super(new DefaultMutableTreeNode());
            setDocument(document);
            }

        //
        // Public methods
        //

        /** Sets the document. */
        public synchronized void setDocument(Document document) {

            // save document
            this.document = document;

            // clear tree and re-populate
            ((DefaultMutableTreeNode)getRoot()).removeAllChildren();
            nodeMap.clear();
            buildTree();
            fireTreeStructureChanged(this, new Object[] { getRoot() }, new int[0], new Object[0]);

            } // setDocument(Document)

        /** Returns the document. */
        public Document getDocument() {
            return document;
            }

        /** get the org.w3c.Node for a MutableTreeNode. */
        public Node getNode(Object treeNode) {
            return (Node)nodeMap.get(treeNode);
        }

        //
        // Private methods
        //

        /** Builds the tree. */
        private void buildTree() {

            // is there anything to do?
            if (document == null) { return; }

            // iterate over children of this node
            NodeList nodes = document.getChildNodes();
            int len = (nodes != null) ? nodes.getLength() : 0;
            MutableTreeNode root = (MutableTreeNode)getRoot();
            for (int i = 0; i < len; i++) {
                Node node = nodes.item(i);
                switch (node.getNodeType()) {
                    case Node.DOCUMENT_NODE: {
                        root = insertDocumentNode(node, root);
                        break;
                        }

                    case Node.ELEMENT_NODE: {
                        insertElementNode(node, root);
                        break;
                        }

                    default: // ignore

                    } // switch

                } // for

            } // buildTree()

        /** Inserts a node and returns a reference to the new node. */
        private MutableTreeNode insertNode(String what, MutableTreeNode where) {

            MutableTreeNode node = new DefaultMutableTreeNode(what);
            insertNodeInto(node, where, where.getChildCount());
            return node;

            } // insertNode(Node,MutableTreeNode):MutableTreeNode

        /** Inserts the document node. */
        private MutableTreeNode insertDocumentNode(Node what, MutableTreeNode where) {
            MutableTreeNode treeNode = insertNode(what.getNodeName(), where);
            nodeMap.put(treeNode, what);
            return treeNode;
            }

        /** Inserts an element node. */
        private MutableTreeNode insertElementNode(Node what, MutableTreeNode where) {
          MutableTreeNode element;
            // build up name
            StringBuffer name = new StringBuffer();
           // name.append('<');
            name.append(what.getNodeName());
            NamedNodeMap attrs = what.getAttributes();
            int attrCount = (attrs != null) ? attrs.getLength() : 0;
            for (int i = 0; i < attrCount; i++) {
                Node attr = attrs.item(i);
                name.append("  ");
                name.append(attr.getNodeName());
                name.append("=\"");
                name.append(attr.getNodeValue());
                name.append('"');
                }
           // name.append('>');

            // insert element node

            element = insertNode(name.toString(), where);

            nodeMap.put(element, what);

            // gather up attributes and children nodes
            NodeList children = what.getChildNodes();
            int len = (children != null) ? children.getLength() : 0;
            for (int i = 0; i < len; i++) {
                Node node = children.item(i);
                switch (node.getNodeType()) {
                    case Node.CDATA_SECTION_NODE: {
                       insertCDataSectionNode( node, element ); //Add a Section Node
                       break;
                      }
                    case Node.TEXT_NODE: {
                        insertTextNode(node, element);
                        break;
                        }
                    case Node.ELEMENT_NODE: {
                        insertElementNode(node, element);
                        break;
                        }
                    }
                }

            return element;

            } // insertElementNode(Node,MutableTreeNode):MutableTreeNode

        /** Inserts a text node. */
        private MutableTreeNode insertTextNode(Node what, MutableTreeNode where) {
            String value = what.getNodeValue().trim();
            if (value.length() > 0) {
                MutableTreeNode treeNode = insertNode(value, where);
                nodeMap.put(treeNode, what);
                return treeNode;
                }
            return null;
            }


      /** Inserts a CData Section Node. */
      private MutableTreeNode insertCDataSectionNode(Node what, MutableTreeNode where) {
         StringBuffer CSectionBfr = new StringBuffer();
         //--- optional --- CSectionBfr.append( "<![CDATA[" );
         CSectionBfr.append( what.getNodeValue() );
         //--- optional --- CSectionBfr.append( "]]>" );
         if (CSectionBfr.length() > 0) {
            MutableTreeNode treeNode = insertNode(CSectionBfr.toString(), where);
            nodeMap.put(treeNode, what);
            return treeNode;
            }
         return null;
         }


      } // class Model



    } // class DOMTree
