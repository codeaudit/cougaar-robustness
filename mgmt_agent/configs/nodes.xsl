<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="html" indent="yes"/>

<xsl:variable name="nodeName">node</xsl:variable>

<xsl:template match="/">
  <xsl:variable name="community">
    <xsl:value-of select="//community/@name" />
  </xsl:variable>
  <html>
  <head>
    <title>
      <xsl:text>All Nodes In </xsl:text>
      <xsl:value-of select="$community" />
    </title>
  </head>
  <body>
    <h1><xsl:value-of select="$community" /></h1>
    <br /><br />
    <ul>
      <xsl:apply-templates select="//node">
        <xsl:sort />
      </xsl:apply-templates>
    </ul>
  </body>
  </html>
</xsl:template>

<xsl:template match="node">
  <li>
    <xsl:variable name="node">
      <xsl:value-of select="@name" />
    </xsl:variable>
    <xsl:element name="a">
    <xsl:attribute name="href">
      <xsl:text>./ar?nodeAttributes=</xsl:text>
        <xsl:value-of select="$node" />
    </xsl:attribute>
    <h2><xsl:value-of select="$node" /></h2>
    </xsl:element>
    <ul>
      <xsl:apply-templates select="//agent">
        <xsl:with-param name="parent" select="$node" />
        <xsl:sort />
      </xsl:apply-templates>
    </ul>
  </li><br />
</xsl:template>

<xsl:template match="agent">
  <xsl:param name="parent" />
  <xsl:variable name="agent">
    <xsl:value-of select="@name" />
  </xsl:variable>
  <xsl:for-each select=".//property">
    <xsl:variable name="name"><xsl:value-of select="@name" /></xsl:variable>
    <xsl:variable name="tmp"><xsl:value-of select="@value" /></xsl:variable>
    <xsl:choose>
      <xsl:when test="$name=$nodeName">
        <xsl:choose>
        <xsl:when test="$tmp=$parent">
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