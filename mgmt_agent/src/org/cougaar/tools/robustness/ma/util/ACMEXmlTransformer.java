package org.cougaar.tools.robustness.ma.util;

import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import java.io.*;
import java.util.*;

public class ACMEXmlTransformer {
  private String societyXML = null;
  private String acmeXML = null;
  private String resultXML = null;
  private String xslt = null;

  public ACMEXmlTransformer( String societyXML,
                             String acmeXML,
                             String resultXML,
                             String xslt) {
    this.societyXML = societyXML;
    this.acmeXML = acmeXML;
    this.resultXML = resultXML;
    this.xslt = xslt;
  }

  protected void transform() {
    //make sure the two xml files are in the same directory as xslt's, if not, copy
    //them to a temp file.
    File xsltF = new File(xslt);
    File tempFile = new File(xsltF.getParentFile(), "tempSociety.xml");
    copyFile(new File(societyXML), tempFile);
    File acmeF = new File(acmeXML);
    File tempAcme = null;
    if(acmeF.getParent() != null) {
      if(!acmeF.getParent().equals(xsltF.getParent())) {
        tempAcme = new File(xsltF.getParentFile(), acmeF.getName());
        copyFile(acmeF, tempAcme);
      }
    }
    else {
      if(xsltF.getParent() != null) {
        tempAcme = new File(xsltF.getParentFile(), acmeF.getName());
        copyFile(acmeF, tempAcme);
      }
    }

    //get the agents appear in socity file but not in host file
    try{
      //get the agents appear in both xml files
      TransformerFactory tFactory = TransformerFactory.newInstance();
      String compareXSL;
      if(tempAcme != null)
        compareXSL = getCompareXSL(tempAcme);
      else
        compareXSL = getCompareXSL(acmeF);
      Transformer transformer = tFactory.newTransformer(new StreamSource(new StringReader(compareXSL)));
      StringWriter writer = new StringWriter();
      transformer.transform(new StreamSource(tempFile), new StreamResult(writer));
      List both = getListFromAgents(writer.toString());

      //get all agents in society file
      transformer = tFactory.newTransformer(new StreamSource(new StringReader(listAgentsXSL)));
      writer = new StringWriter();
      transformer.transform(new StreamSource(tempFile), new StreamResult(writer));
      List all = getListFromAgents(writer.toString());

      //give the difference
      List s = compareLists(all, both);
      if(s.size() > 0){
        System.out.println("Warning!! Following agents are not listed in " + acmeXML);
        System.out.println(s);
      }
    }catch(Exception e){e.printStackTrace();}

    //do the real job: combine both xml files and write into the result file.
    File resultF = new File(resultXML);
    try{
      TransformerFactory tFactory = TransformerFactory.newInstance();
      Transformer transformer = tFactory.newTransformer(new StreamSource(xsltF));
      StringWriter writer = new StringWriter();
      if(tempAcme != null)
        transformer.transform(new StreamSource(tempAcme), new StreamResult(resultF));
      else
        transformer.transform(new StreamSource(acmeF), new StreamResult(resultF));
    }catch(Exception e){e.printStackTrace();}

    //delete all temp files
    tempFile.delete();
    //if(tempAcme != null) tempAcme.delete();
  }

  private void copyFile(File fromFile, File toFile){
    FileInputStream from = null;
    FileOutputStream to = null;
    try {
      from = new FileInputStream(fromFile);
      to = new FileOutputStream(toFile);
      byte[] buffer = new byte[4096];
      int bytes_read;

      while((bytes_read = from.read(buffer)) != -1)
        to.write(buffer, 0, bytes_read);
    }catch(IOException e){e.printStackTrace();}
    finally {
      if (from != null) try{from.close();}catch(IOException e){}
      if(to != null)try{to.close();}catch(IOException e){}
    }
  }

  private List getListFromAgents(String str) {
    List list = new ArrayList();
    StringTokenizer tokens = new StringTokenizer(str, "<");
    tokens.nextToken(); //the first one is xml specification
    while(tokens.hasMoreTokens()) {
      String temp = tokens.nextToken();
      int first = temp.indexOf("\"");
      int next = temp.lastIndexOf("\"");
      list.add(temp.substring(first+1, next));
    }
    return list;
  }

  //Get all elements in A but not in B.
  private List compareLists(List A, List B) {
    List s = new ArrayList();
    for(Iterator it = A.iterator(); it.hasNext();){
      String t = (String)it.next();
      if(!B.contains(t))
        s.add(t);
    }
    return s;
  }

  public static void main(String args[]) {
    if(args.length != 4) {
      System.out.println("Please define four parameters to run this class:\n " +
        "1. society xml file. 2. host xml file. 3. result xml file. " +
        "4. xslt file.");
    }
    else {
      ACMEXmlTransformer transformer = new ACMEXmlTransformer(args[0], args[1], args[2], args[3]);
      transformer.transform();
    }
  }

  //This xsl string get all the agents in both society file and host file.
  private String getCompareXSL(File xml) {
    return
   "<?xml version=\"1.0\"?>" +
   "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">" +
   "<xsl:variable name=\"xml2\" select=\"document('" + xml.getPath() + "')\" />" +
   "<xsl:template match=\"/\">" +
   "<xsl:apply-templates select=\"/society/host/node/agent\" />" +
   "</xsl:template>" +
   "<xsl:template match=\"agent\">" +
   "<xsl:variable name=\"origAgent\" select=\"@name\" />" +
   "<xsl:for-each select=\"$xml2/Society/Node/Agent\">" +
   "<xsl:variable name=\"currentAgent\" select=\"@Name\" />" +
   "<xsl:choose><xsl:when test=\"$currentAgent=$origAgent\">" +
   "<Agent><xsl:attribute name=\"name\">" +
   "<xsl:value-of select=\"$currentAgent\" /></xsl:attribute></Agent>" +
   "</xsl:when></xsl:choose>" +
   "</xsl:for-each>" +
   "</xsl:template></xsl:stylesheet>";
   }

   //the xsl string list all agents appear in society xml file.
   private final static String listAgentsXSL =
   "<?xml version=\"1.0\"?>" +
   "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">" +
   "<xsl:template match=\"/\">" +
   "<xsl:apply-templates select=\"/society/host/node/agent\" />" +
   "</xsl:template>" +
   "<xsl:template match=\"agent\">" +
   "<Agent><xsl:attribute name=\"name\">" +
   "<xsl:value-of select=\"@name\" /></xsl:attribute></Agent>" +
   "</xsl:template></xsl:stylesheet>";
}