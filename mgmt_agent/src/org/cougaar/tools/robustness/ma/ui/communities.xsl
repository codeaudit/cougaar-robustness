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
      <H1><xsl:text>All Robustness Communities</xsl:text></H1><br />
  <table border="0" cellpadding="10" cellspacing="0">
  <xsl:for-each select="//community">
    <tr>
      <td><H3><xsl:value-of select="@name" /></H3></td>
      <td>
        <xsl:element name="a">
          <xsl:attribute name="href">
            <xsl:text>./ar?showcommunity=</xsl:text>
            <xsl:value-of select="@name" />
          </xsl:attribute>
          <xsl:text>Status</xsl:text>
        </xsl:element>
      </td>
      <td>
        <xsl:element name="a">
        <xsl:attribute name="href">
          <xsl:text>./ar?control=</xsl:text>
          <xsl:value-of select="@name" />
        </xsl:attribute>
        <xsl:text>Control</xsl:text>
        </xsl:element>
      </td>
    </tr>
  </xsl:for-each>
  </table>
  </body>
</html>
</xsl:template>
</xsl:stylesheet>