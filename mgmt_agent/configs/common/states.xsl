<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="html" indent="yes"/>

<xsl:variable name="currentState">currentState</xsl:variable>

<xsl:key name="states" match="//property[@name=$currentState]" use="@value" />

<xsl:template match="/">
  <xsl:variable name="community">
    <xsl:value-of select="//community/@name" />
  </xsl:variable>
<html>
  <head>
    <title>
      <xsl:text>All States In </xsl:text>
      <xsl:value-of select="$community" />
    </title>
  </head>
  <body><br /><br />
    <h1><xsl:value-of select="$community" /></h1>
    <br />
    <ul>
    <xsl:apply-templates select="//property[@name=$currentState and generate-id(.)=generate-id(key('states', @value))]" />
    </ul>
  </body>
</html>
</xsl:template>

<xsl:template match="//property[@name=$currentState]">
  <li>
    <h3><xsl:value-of select="@value" />
    <xsl:text>: </xsl:text>
    <xsl:value-of select="count(key('states', @value))" /></h3>
  </li>
  <ul>
    <xsl:apply-templates select="//agent">
        <xsl:with-param name="state" select="@value" />
        <xsl:sort />
    </xsl:apply-templates>
  </ul><br />
</xsl:template>

<xsl:template match="agent">
  <xsl:param name="state" />
  <xsl:variable name="agent">
    <xsl:value-of select="@name" />
  </xsl:variable>
  <xsl:for-each select=".//property">
    <xsl:variable name="name"><xsl:value-of select="@name" /></xsl:variable>
    <xsl:variable name="tmp"><xsl:value-of select="@value" /></xsl:variable>
    <xsl:choose>
      <xsl:when test="$name=$currentState">
        <xsl:choose>
        <xsl:when test="$tmp=$state">
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