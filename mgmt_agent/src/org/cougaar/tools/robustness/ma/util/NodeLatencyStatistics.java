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
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;

import org.cougaar.util.ConfigFinder;

/**
 */

public class NodeLatencyStatistics {

  protected static final String FILENAME = "nodestats.xml";
  protected String path;
  protected Map scMap = new HashMap();

  public NodeLatencyStatistics() {
  }

  public void put(StatCalc sc) {
    scMap.put(sc.getNodeName(), sc);
  }

  public StatCalc get(String nodeName) {
    return (StatCalc)scMap.get(nodeName);
  }

  public List list() {
    return new ArrayList(scMap.keySet());
  }

  public boolean contains(String id) {
    return scMap.containsKey(id);
  }

  public void remove(String id) {
    if (scMap.containsKey(id)) {
      scMap.remove(id);
    }
  }

  public void load() {
    File xmlFile = ConfigFinder.getInstance().locateFile(FILENAME);
    if (xmlFile != null) {
      path = xmlFile.getAbsolutePath();
      parse(path);
    }
  }

  public void save() {
    if (path == null) {
      List dirList = ConfigFinder.getInstance().getConfigPath();
      if (!dirList.isEmpty()) {
        path = dirList.get(0) + FILENAME;
        path = path.replaceAll("file:", "");
      } else {
        path = FILENAME;
      }
    }
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
      System.out.println("Unable to write NodeLatencyStatistics to file " + path);
    }
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
    StringBuffer sb = new StringBuffer("<NodeLatencyStatistics>\n");
    for (Iterator it = scMap.values().iterator(); it.hasNext();) {
      StatCalc sc = (StatCalc)it.next();
      sb.append("  " + sc.toXML() + "\n");
    }
    sb.append("</NodeLatencyStatistics>");
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
          put(new StatCalc(community, node, samples, sum, sumSquares, high, low));
        }
      } catch (Exception ex) {
        System.out.println("Exception parsing statCalc data");
        //ex.printStackTrace();
      }
    }
  }

}
