<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="xml" indent="yes"/>

<xsl:variable name="xml2" select="document('tempSociety.xml')" />

<xsl:template match="/">
  <society>
    <xsl:copy-of select="$xml2/society/@*" />
    <xsl:apply-templates select="//Host" />
  </society>
</xsl:template>

<xsl:template match="Host">
  <host>
    <xsl:attribute name="name">
      <xsl:value-of select="@Name" />
    </xsl:attribute>
    <xsl:apply-templates select=".//NodeID" />
  </host>
</xsl:template>

<xsl:template match="NodeID">
  <xsl:variable name="currentNode" select="@Name" />
  <node>
    <xsl:attribute name="name">
      <xsl:value-of select="$currentNode" />
    </xsl:attribute>
   <!--copy components of the only node in xml2 to each new node here-->
    <xsl:for-each select="$xml2/society/host/node[@name=($currentNode)]/component">
      <component>
        <xsl:copy-of select="node()|@*" />
      </component>
    </xsl:for-each>
    <xsl:apply-templates select="/Society/Node[@Name=($currentNode)]/Agent" >
      <xsl:with-param name="nodeName" select="$currentNode" />
    </xsl:apply-templates>
  </node>
</xsl:template>

<xsl:template match="Agent">
  <xsl:variable name="currentAgent" select="@Name" />
  <xsl:variable name="managerAgent">ARManager</xsl:variable>
  <!--Automatically creat the manager agent.-->
  <xsl:if test="contains($currentAgent, $managerAgent)">
    <agent>
      <xsl:attribute name="name">
        <xsl:value-of select="$currentAgent" />
      </xsl:attribute>
      <xsl:attribute name="class">org.cougaar.core.agent.SimpleAgent</xsl:attribute>
    </agent>
  </xsl:if>
  <xsl:if test="not(contains($currentAgent, $managerAgent))">
  <!--Get match agent in xml2 file and copy all contents of that agent.-->
  <xsl:for-each select="$xml2/society/host/node/agent">
    <xsl:variable name="origAgent" select="@name" />
    <xsl:choose>
      <xsl:when test="$currentAgent=$origAgent">
        <agent>
          <xsl:copy-of select="node()|@*" />
 	</agent>
      </xsl:when>
    </xsl:choose>
  </xsl:for-each>
  </xsl:if>
</xsl:template>

</xsl:stylesheet>
