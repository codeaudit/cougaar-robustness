package org.cougaar.tools.robustness.ma.test;

import java.sql.*;
import java.io.*;
import java.util.*;
import java.net.URL;

import org.cougaar.util.DBConnectionPool;
import org.cougaar.util.DBProperties;
import org.cougaar.util.Parameters;
import org.cougaar.util.ConfigFinder;

/**
 * Read a database defined in cougaar.rc file, fetch data of table "community_attribute"
 * and "community_entity_attribute" and build a xml file based on the data.
 */
public class DatabaseExporter
{
  private static final String QUERY_FILE = "DBInitializer.q";
  private static Connection conn;
  private static final String extension = ".csv";
  private static final String communityTable = "community_attribute";
  private static final String entityTable = "community_entity_attribute";

  static
  {
    try{
      DBProperties dbp = DBProperties.readQueryFile(QUERY_FILE);
      String dbStyle = dbp.getDBType();
      insureDriverClass(dbStyle);
      String database = dbp.getProperty("database");
      String username = dbp.getProperty("username");
      String password = dbp.getProperty("password");
      try{
        conn = DBConnectionPool.getConnection(database, username, password);
      }catch(SQLException e){e.printStackTrace();}
    }catch(IOException e){e.printStackTrace();}
    catch(Exception e){e.printStackTrace();}
  }

  /**
   * Set up available JDBC driver
   */
  private static void insureDriverClass(String dbtype) throws SQLException, ClassNotFoundException {
    String driverParam = "driver." + dbtype;
    String driverClass = Parameters.findParameter(driverParam);
    if (driverClass == null) {
      // this is likely a "cougaar.rc" problem.
      // Parameters should be modified to help generate this exception:
      throw new SQLException("Unable to find driver class for \""+
                             driverParam+"\" -- check your \"cougaar.rc\"");
    }
    Class.forName(driverClass);
  }

  public DatabaseExporter(String file)
  {
    if(!file.endsWith(".xml"))
      file += ".xml";
    File xmlFile = new File(file);
    new DatabaseExporter(xmlFile);
  }

  public DatabaseExporter(File file)
  {
    if(file.exists())
    {
     try{
       file.delete();
       file.createNewFile();
     }catch(IOException e){e.printStackTrace();}
    }
    Hashtable community_attrs = getDataFromCommunityTable();
    Hashtable community_entities = getDataFromEntityTable();
    writeXmlFile(file, community_attrs, community_entities);
  }

  private Hashtable getDataFromCommunityTable()
  {
    Hashtable communities = new Hashtable();
    try{
      Statement s = conn.createStatement();
      ResultSet rs = s.executeQuery("select * from " + communityTable);
      while(rs.next())
      {
        String communityName = rs.getString("COMMUNITY_ID");
        List attrs;
        if(!keyExists(communities, communityName)) //it's the first time get this community
        {
          attrs = new ArrayList();
          communities.put(communityName, attrs);
        }
        else
          attrs = (List)communities.get(communityName);
        Attribute attr = new Attribute(rs.getString("ATTRIBUTE_ID"), rs.getString("ATTRIBUTE_VALUE"));
        attrs.add(attr);
      }
    }catch(SQLException e){e.printStackTrace();}
    return communities;
  }

  private Hashtable getDataFromEntityTable()
  {
    Hashtable communities = new Hashtable();
    try{
      Statement s = conn.createStatement();
      ResultSet rs = s.executeQuery("select * from " + entityTable);
      while(rs.next())
      {
        String communityName = rs.getString("COMMUNITY_ID");
        Hashtable entities;
        if(!keyExists(communities, communityName))
        {
          entities = new Hashtable();
          communities.put(communityName, entities);
        }
        else
          entities = (Hashtable)communities.get(communityName);

        String entityName = rs.getString("ENTITY_ID");
        List attrs;
        if(!keyExists(entities, entityName))
        {
          attrs = new ArrayList();
          entities.put(entityName, attrs);
        }
        else
          attrs = (List)entities.get(entityName);

        Attribute attr = new Attribute(rs.getString("ATTRIBUTE_ID"), rs.getString("ATTRIBUTE_VALUE"));
        attrs.add(attr);
      }
    }catch(SQLException e){e.printStackTrace();}
    return communities;
  }

  private void writeXmlFile(File file, Hashtable community_attrs, Hashtable community_entities)
  {
    try{
      RandomAccessFile rfile = new RandomAccessFile(file, "rw");
      rfile.write(version.getBytes());
      rfile.write(dtd.getBytes());
      rfile.write("<Communities>\n".getBytes());

      for(Enumeration enums = community_attrs.keys(); enums.hasMoreElements();)
      {
        String communityID = (String)enums.nextElement();
        List attrs = (List)community_attrs.get(communityID);
        Hashtable entities = null;
        rfile.write(new String("  <Community Name=\"" + communityID + "\" >\n").getBytes());
        writeAttributes(rfile, attrs, "    ");
        entities = (Hashtable)community_entities.get(communityID); //get entities
        if(entities != null)
        {
          for(Enumeration enum = entities.keys(); enum.hasMoreElements();)
          {
             String entityID = (String)enum.nextElement();
             List entity_attrs = (List)entities.get(entityID);
             rfile.write(new String("    <Entity Name=\"" + entityID + "\" >\n").getBytes());
             writeAttributes(rfile, entity_attrs, "      ");
             rfile.write("    </Entity>\n".getBytes());
          }
        }
        rfile.write("  </Community>\n".getBytes());
      }

      //in case some communities only have entities, check the two hashtables
      List differs = differentKeys(community_attrs, community_entities);
      if(differs.size() > 0)
      {
        for(int i=0; i<differs.size(); i++)
        {
          String communityID = (String)differs.get(i);
          Hashtable entities = (Hashtable)community_entities.get(communityID);
          rfile.write(new String("  <Community Name=\"" + communityID + "\" >\n").getBytes());
          for(Enumeration enum = entities.keys(); enum.hasMoreElements();)
          {
             String entityID = (String)enum.nextElement();
             List entity_attrs = (List)entities.get(entityID);
             rfile.write(new String("    <Entity Name=\"" + entityID + "\" >\n").getBytes());
             writeAttributes(rfile, entity_attrs, "      ");
             rfile.write("    </Entity>\n".getBytes());
          }
          rfile.write("  </Community>\n".getBytes());
        }
      }

      rfile.write("</Communities>\n".getBytes());
    }catch(IOException e){e.printStackTrace();}
  }

  private void writeAttributes(RandomAccessFile file, List attrs, String space)
  {
    try{
      for(int i=0; i<attrs.size(); i++) //write attributes of the community
        {
          Attribute attr = (Attribute)attrs.get(i);
          file.write(new String(space + "<Attribute ID=\"" + attr.name + "\" Value=\"" +
            attr.value + "\" />\n").getBytes());
        }
    }catch(IOException e){e.printStackTrace();}
  }

  private List differentKeys(Hashtable a, Hashtable b)
  {
    List differs = new ArrayList();
    for(Enumeration enums = b.keys(); enums.hasMoreElements();)
    {
      String key = (String)enums.nextElement();
      if(!a.containsKey(key))
        differs.add(key);
    }
    return differs;
  }

  private boolean keyExists(Hashtable table, String key)
  {
    for(Enumeration enums = table.keys(); enums.hasMoreElements();)
    {
      if(((String)enums.nextElement()).equals(key))
        return true;
    }
    return false;
  }

  public static void main(String args[])
  {
    if(args.length == 0)
      System.out.println("please give the xml file name");
    new DatabaseExporter(args[0]);
  }

  private class Attribute
  {
    public String name;
    public String value;
    public Attribute(String name, String value)
    {
      this.name = name;
      this.value = value;
    }
  }

  private static final String dtd = "<!DOCTYPE Communities [\n" +
               "<!ELEMENT Communities (Community+)>\n" +
               "<!ELEMENT Community (Attribute+, Entity*)>\n" +
               "<!ATTLIST Community Name CDATA #REQUIRED>\n\n" +
               "<!ELEMENT AttributeID EMPTY>\n" +
               "<!ATTLIST AttributeID ID CDATA #REQUIRED>\n" +
               "<!ATTLIST AttributeID Access (manager|member|associate|world) #IMPLIED>\n\n" +
               "<!ELEMENT Entity (Attribute*)>\n" +
               "<!ATTLIST Entity Name CDATA #REQUIRED>\n\n" +
               "<!ELEMENT Attribute EMPTY>\n" +
               "<!ATTLIST Attribute ID CDATA #REQUIRED>\n" +
               "<!ATTLIST Attribute Value CDATA #REQUIRED>\n" +
               "]>\n\n";
  private static final String version = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
}