<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="html" indent="yes"/>

<xsl:template match="/">
<html>
  <head>
    <title>
      <xsl:text>Communities</xsl:text>
    </title>
  </head>
  <body>
      <H1><xsl:text>All Communities</xsl:text></H1>
  <ol>
  <xsl:for-each select="//community">
    <li>
     <xsl:element name="a">
       <xsl:attribute name="href">
         <xsl:text>./ar?showcommunity=</xsl:text>
         <xsl:value-of select="@name" />
       </xsl:attribute>
       <xsl:value-of select="@name" />
     </xsl:element>
    </li>
  </xsl:for-each>
  </ol>
  </body>
</html>
</xsl:template>
</xsl:stylesheet>