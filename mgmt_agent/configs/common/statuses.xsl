<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="html" indent="yes"/>

<xsl:variable name="currentStatus">currentStatus</xsl:variable>

<xsl:key name="status" match="//property[@name=$currentStatus]" use="@value" />

<xsl:template match="/">
  <xsl:variable name="community">
    <xsl:value-of select="//community/@name" />
  </xsl:variable>
<html>
  <head>
    <title>
      <xsl:text>All Statuses In </xsl:text>
      <xsl:value-of select="$community" />
    </title>
  </head>
  <body><br /><br />
    <h1><xsl:value-of select="$community" /></h1>
    <br />
    <ul>
    <xsl:apply-templates select="//property[@name=$currentStatus and generate-id(.)=generate-id(key('status', @value))]" />
    </ul>
  </body>
</html>
</xsl:template>

<xsl:template match="//property[@name=$currentStatus]">
  <li>
    <h3><xsl:value-of select="@value" />
    <xsl:text>: </xsl:text>
    <xsl:value-of select="count(key('status', @value))" /></h3>
  </li>
  <ul>
    <xsl:apply-templates select="//agent">
        <xsl:with-param name="status" select="@value" />
        <xsl:sort />
    </xsl:apply-templates>
  </ul><br />
</xsl:template>

<xsl:template match="agent">
  <xsl:param name="status" />
  <xsl:variable name="agent">
    <xsl:value-of select="@name" />
  </xsl:variable>
  <xsl:for-each select=".//property">
    <xsl:variable name="name"><xsl:value-of select="@name" /></xsl:variable>
    <xsl:variable name="tmp"><xsl:value-of select="@value" /></xsl:variable>
    <xsl:choose>
      <xsl:when test="$name=$currentStatus">
        <xsl:choose>
        <xsl:when test="$tmp=$status">
          <li>
            <xsl:element name="a">
            <xsl:attribute name="href">
            <xsl:text>./ar?agentAttributes=</xsl:text>
            <xsl:value-of select="$agent" />
            </xsl:attribute>
            <xsl:value-of select="$agent" />
            </xsl:element>
          </li>
        </xsl:when>
        </xsl:choose>
      </xsl:when>
    </xsl:choose>
  </xsl:for-each>
</xsl:template>

</xsl:stylesheet>