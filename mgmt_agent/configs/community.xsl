<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="html" indent="yes"/>

<xsl:variable name="nodeName">node</xsl:variable>
<xsl:variable name="currentState">currentState</xsl:variable>
<xsl:variable name="currentStatus">currentStatus</xsl:variable>

<xsl:template match="/">
  <xsl:variable name="community">
    <xsl:value-of select="//community/@name" />
  </xsl:variable>
<html>
  <head>
    <title>
      <xsl:text>Community:</xsl:text>
      <xsl:value-of select="$community" />
    </title>
  </head>
  <body>
  <br /><br />
    <center>
      <h1>
       <xsl:element name="a">
       <xsl:attribute name="href">
         <xsl:text>./ar?community=</xsl:text>
         <xsl:value-of select="$community" />
       </xsl:attribute>
         <xsl:value-of select="$community" />
       </xsl:element>
      </h1><br />
      <table border="1" cellpadding="10" cellspacing="0">
        <th><xsl:text>AgentName</xsl:text></th>
        <th><xsl:element name="a">
           <xsl:attribute name="href">
             <xsl:text>./ar?listNode=node</xsl:text>
           </xsl:attribute>
           <xsl:text>NodeName</xsl:text>
        </xsl:element></th>
        <th>
          <xsl:element name="a">
          <xsl:attribute name="href">
            <xsl:text>./ar?listState=</xsl:text>
            <xsl:value-of select="$community" />
          </xsl:attribute>
           <xsl:text>CurrentState</xsl:text>
          </xsl:element>
        </th>
        <th>
          <xsl:element name="a">
          <xsl:attribute name="href">
            <xsl:text>./ar?listStatus=</xsl:text>
            <xsl:value-of select="$community" />
          </xsl:attribute>
           <xsl:text>CurrentStatus</xsl:text>
          </xsl:element>
        </th>
        <xsl:apply-templates select=".//agent" />
       </table>
    </center>
  </body>

</html>
</xsl:template>

<xsl:template match="agent">
     <tr>
       <td>
         <xsl:element name="a">
         <xsl:attribute name="href">
           <xsl:text>./ar?agentAttributes=</xsl:text>
           <xsl:value-of select="@name" />
         </xsl:attribute>
         <xsl:value-of select="@name" />
         </xsl:element>
       </td>
       <xsl:apply-templates select=".//property" />
     </tr>
</xsl:template>

<xsl:template match="property">
   <xsl:variable name="name"><xsl:value-of select="@name" /></xsl:variable>
   <xsl:choose>
     <xsl:when test="$name=$nodeName">
        <td>
          <xsl:element name="a">
          <xsl:attribute name="href">
            <xsl:text>./ar?nodeAttributes=</xsl:text>
            <xsl:value-of select="@value" />
          </xsl:attribute>
          <xsl:value-of select="@value" />
          </xsl:element>
        </td>
     </xsl:when>
     <xsl:when test="$name=$currentState">
        <td>
          <xsl:value-of select="@value" />
        </td>
     </xsl:when>
     <xsl:when test="$name=$currentStatus">
        <td><xsl:value-of select="@value" /></td>
     </xsl:when>
   </xsl:choose>
</xsl:template>
</xsl:stylesheet>