<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="html" indent="yes"/>

<xsl:template match="/">
  <xsl:variable name="agent">
    <xsl:value-of select="//agent/@name" />
  </xsl:variable>
  <html>
    <head>
    <title>
      <xsl:text>Agent:</xsl:text>
      <xsl:value-of select="$agent" />
    </title>
    </head>
    <body><br /><br />
      <center>
        <h1><xsl:value-of select="$agent" /></h1>
        <table border="1" cellpadding="10" cellspacing="0">
        <th><xsl:text>Property Name</xsl:text></th>
        <th><xsl:text>Property Value</xsl:text></th>
        <xsl:for-each select="//property">
           <tr>
             <td><xsl:value-of select="@name" /></td>
             <td><xsl:value-of select="@value" /></td>
           </tr>
        </xsl:for-each>
      </table>
      </center>
    </body>
  </html>
</xsl:template>

</xsl:stylesheet>