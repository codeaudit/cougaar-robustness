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
package org.cougaar.tools.robustness.ma.util;

import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;

/**
 */

public class NodeStatistics {

  public static final String statsDir = "nodestats";
  public static final String fileExtension = ".nodestats";

  protected String path;
  protected Map current = new HashMap();
  protected Map original = new HashMap();

  public NodeStatistics(String communityName) {
    this.path = getPath(communityName);
  }

  public NodeStatistics(String     communityName,
                        Collection values) {
    this(communityName);
    for (Iterator it = values.iterator(); it.hasNext();) {
      put((StatCalc)it.next());
    }
  }

  public synchronized void put(StatCalc sc) {
    current.put(sc.getNodeName(), sc);
  }

  public synchronized void putOriginal(StatCalc sc) {
    original.put(sc.getNodeName(), sc);
    current.put(sc.getNodeName(), sc.clone());
  }

  public synchronized StatCalc get(String nodeName) {
    return (StatCalc)current.get(nodeName);
  }

  public synchronized StatCalc getOriginal(String nodeName) {
    return (StatCalc)original.get(nodeName);
  }

  public synchronized List list() {
    return new ArrayList(current.keySet());
  }

  public synchronized List listOriginal() {
    return new ArrayList(original.keySet());
  }

  public synchronized Collection values() {
    return new ArrayList(current.values());
  }

  public boolean contains(String id) {
    return current.containsKey(id);
  }

  public boolean containsOriginal(String id) {
    return original.containsKey(id);
  }

  public synchronized void remove(String id) {
    if (current.containsKey(id)) {
      current.remove(id);
    }
  }

  public void load() {
    File f = new File(path);
    if (f.exists() && f.canRead()) {
      parse(path);
    }
  }

  public void save() {
    write(path);
  }

  protected void write(String path) {
    try {
      FileWriter fw = new FileWriter(path);
      BufferedWriter bw = new BufferedWriter(fw, 60000);
      bw.write(toXML());
      bw.close();
      fw.close();
    } catch (Exception ex) {
      System.out.println("Unable to write NodeStatistics to file " + path);
    }
  }

  protected String getPath(String communityName) {
    File baseDir = new File(System.getProperty("org.cougaar.workspace"));
    File fullDir = new File(baseDir + File.separator + statsDir);
    if (!fullDir.exists() && baseDir.canWrite()) {
      fullDir.mkdir();
    }
    File xmlFile = new File(fullDir + File.separator + communityName + fileExtension);
    return xmlFile.getAbsolutePath();
  }

  protected void parse(String path) {
    try {
      XMLReader xr = new org.apache.xerces.parsers.SAXParser();
      SaxHandler myHandler = new SaxHandler();
      xr.setContentHandler(myHandler);
      InputSource is = new InputSource(path);
      xr.parse(is);
    } catch (Exception ex) {
      System.out.println("Exception parsing stats file '" + path + "', " + ex);
      //ex.printStackTrace();
    }
  }

  public String toXML() {
    StringBuffer sb = new StringBuffer("<NodeStatistics>\n");
    for (Iterator it = original.values().iterator(); it.hasNext();) {
      StatCalc sc = (StatCalc)it.next();
      if (!current.containsKey(sc.getNodeName())) {
        sb.append("  " + sc.toXML() + "\n");
      }
    }
    for (Iterator it = current.values().iterator(); it.hasNext();) {
      StatCalc sc = (StatCalc)it.next();
      sb.append("  " + sc.toXML() + "\n");
    }
    sb.append("</NodeStatistics>");
    return sb.toString();
  }

  class SaxHandler extends DefaultHandler {

    public void startElement(String uri, String localname, String rawname,
                             Attributes p3) {
      String community = "";
      String node = "";
      int samples = 0;
      double sum = 0;
      double sumSquares = 0;
      double high = 0;
      double low = 0;
      try {
        if (localname.equals("Item")) {
          for (int i = 0; i < p3.getLength(); i++) {
            if (p3.getLocalName(i).equals("community")) {
              community = p3.getValue(i);
            } else if (p3.getLocalName(i).equals("node")) {
              node = p3.getValue(i);
            } else if (p3.getLocalName(i).equals("samples")) {
              samples = Integer.parseInt(p3.getValue(i));
            } else if (p3.getLocalName(i).equals("sum")) {
              sum = Double.parseDouble(p3.getValue(i));
            } else if (p3.getLocalName(i).equals("sumSquares")) {
              sumSquares = Double.parseDouble(p3.getValue(i));
            } else if (p3.getLocalName(i).equals("high")) {
              high = Double.parseDouble(p3.getValue(i));
            } else if (p3.getLocalName(i).equals("low")) {
              low = Double.parseDouble(p3.getValue(i));
            }
          }
          StatCalc sc = new StatCalc(community, node, samples, sum, sumSquares, high, low);
          original.put(node, sc);
          current.put(node, sc.clone());
        }
      } catch (Exception ex) {
        System.out.println("Exception parsing statCalc data");
        //ex.printStackTrace();
      }
    }
  }

}
