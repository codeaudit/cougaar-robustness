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
package org.cougaar.tools.robustness.ma.test;

import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import java.io.*;
import java.util.*;

/**
 * Converts an XML community description file into CSV files.
 */
public class XmlToCsv extends DefaultHandler
{
  private static String defaultXmlFilename   = "communities.xml";
  private static String communityCsvFilename = "community_attribute.csv";
  private static String entityCsvFilename    = "community_entity_attribute.csv";
  private Hashtable calist = new Hashtable();
  private Hashtable celist = new Hashtable();
  private StringBuffer accumulator = new StringBuffer();
  private Vector attributeList;
  private Hashtable entityList;
  private String comName; //community name
  private String entityName;
  private boolean firstEntity;

  public void convert(String xmlFileName) {
    String communityCsvPath = communityCsvFilename;
    String entityCsvPath = entityCsvFilename;

    try{
      File file = new File(xmlFileName);
      String parentDir = file.getParent();
      if (parentDir == null)
        parentDir = "";
      else
        parentDir = parentDir + "/";
      communityCsvPath = parentDir + communityCsvFilename;
      entityCsvPath = parentDir + entityCsvFilename;

      SAXParserFactory spf = SAXParserFactory.newInstance();
      spf.setValidating(false);
      SAXParser parser = spf.newSAXParser();
      org.xml.sax.InputSource input = new InputSource(xmlFileName);
      parser.parse(input, this);
    }catch(Exception e){e.printStackTrace();}
    writeCommunityCsvFile(communityCsvPath, calist);
    writeEntityCsvFile(entityCsvPath, celist);
  }

  public void characters(char[] buffer, int start, int length)
  { accumulator.append(buffer, start, length); }

  public void startElement(String name, String localName, String qname, Attributes attributes)
  {
    accumulator.setLength(0);
    if(qname.equals("Community"))
    {
      comName = attributes.getValue("Name");
      attributeList = new Vector();
      entityList = new Hashtable();
      firstEntity = true;
    }
    else if(qname.equals("Attribute"))
    {
      String attrname = attributes.getValue("ID");
      String value = attributes.getValue("Value");
      attributeList.add(new NVPair(attrname, value));
    }
    else if(qname.equals("Entity"))
    {
      if(firstEntity)
      {
        Vector tempList = (Vector)attributeList.clone();
        calist.put(comName, tempList);
        attributeList.clear();
        firstEntity = false;
      }
      entityName = attributes.getValue("Name");
    }
  }

  public void endElement(String name, String localName, String qname)
  {
    if(qname.equalsIgnoreCase("community"))
    {
      if(!calist.containsKey(comName)) //the community doesn't have entities
      {
        calist.put(comName, attributeList);
        return;
      }
      celist.put(comName, entityList);
    }
    if(qname.equalsIgnoreCase("entity"))
    {
      entityList.put(entityName, (Vector)attributeList.clone());
      attributeList.clear();
    }
  }


  public void warning(SAXParseException e)
  { System.err.println("WARNING: line " + e.getLineNumber() + ": " + e.getMessage());}

  public void error(SAXParseException e)
  { System.err.println("ERROR: line " + e.getLineNumber() + ": " + e.getMessage()); }

  private void writeCommunityCsvFile(String filename, Hashtable attributes)
  {
    try{
      RandomAccessFile file  = new RandomAccessFile(filename, "rw");
      file.write("ASSEMBLY_ID,COMMUNITY_ID,ATTRIBUTE_ID,ATTRIBUTE_VALUE,BLANK\n".getBytes());
      for(Enumeration enums = attributes.keys(); enums.hasMoreElements();)
      {
        String communityName = (String)enums.nextElement();
        List attrs = (List)attributes.get(communityName);
        for(int i=0; i<attrs.size(); i++)
        {
          NVPair pair = (NVPair)attrs.get(i);
          file.write(("\"COMM-DEFAULT_CONFIG\", \"" + communityName + "\", \"" + pair.getName() + "\", \"" + pair.getValue() + "\"\n").getBytes());
        }
      }
    }catch(IOException e){e.printStackTrace();}
  }

  private void writeEntityCsvFile(String filename, Hashtable entitys)
  {
    try{
      RandomAccessFile file = new RandomAccessFile(filename, "rw");
      file.write("ASSEMBLY_ID,COMMUNITY_ID,ENTITY_ID,ATTRIBUTE_ID,ATTRIBUTE_VALUE,BLANK\n".getBytes());
      for(Enumeration enums = entitys.keys(); enums.hasMoreElements();)
      {
        String communityName = (String)enums.nextElement();
        Hashtable ents = (Hashtable)entitys.get(communityName);
        for(Enumeration ent = ents.keys(); ent.hasMoreElements();)
        {
          String entName = (String)ent.nextElement();
          List attrs = (List)ents.get(entName);
          for(int i=0; i<attrs.size(); i++)
          {
            NVPair pair = (NVPair)attrs.get(i);
            file.write(("\"COMM-DEFAULT_CONFIG\", \"" + communityName + "\", \"" + entName + "\", \"" + pair.getName()
               + "\", \"" + pair.getValue() + "\"\n").getBytes());
          }
        }
      }
    }catch(IOException e){e.printStackTrace();}
  }

  private class NVPair
  {
    private String name;
    private String value;
    public NVPair(String name, String value)
    {
      this.name = name;
      this.value = value;
    }
    public String getName()
    { return this.name; }
    public String getValue()
    { return this.value; }
  }

  public static void main(String[] args) {
    XmlToCsv converter = new XmlToCsv();
    if (args.length > 0)
      converter.convert(args[0]);
    else
      converter.convert(defaultXmlFilename);
  }
}