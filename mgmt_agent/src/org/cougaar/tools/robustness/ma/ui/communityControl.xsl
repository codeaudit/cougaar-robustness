<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="html" indent="yes"/>

<xsl:variable name="community" select="//community/@name" />

<xsl:template match="/">
<html>
  <head>
    <title>
      <xsl:text>Community:</xsl:text>
      <xsl:value-of select="$community" />
    </title>
  </head>

  <script language="JavaScript">
     function changeAgent() {
       var x = document.forms.myForm1.mobileagent;
       var node = x.options[x.selectedIndex].label;
       var orignode = document.forms.myForm1.orignode;
       orignode.readOnly = false;
       orignode.value = node;
       orignode.readOnly = true;
       var destinationnode = document.forms.myForm1.destinationnode;
       for(var i=0; i &lt; destinationnode.length; i++) {
         //destinationnode.remove(i);
         var value = destinationnode.options[i].text;
         if(node == value)
           destinationnode.options[i].disabled = true;
         else
           destinationnode.options[i].disabled = false;
       }
     }

     function changeOp() {
       var x = document.forms.myForm1.operation;
       var op = x.options[x.selectedIndex].text;
       var destinationnode = document.forms.myForm1.destinationnode;
       if (op == "Move")
         destinationnode.disabled = false;
       else
         destinationnode.disabled = true;
     }
  </script>

  <body>
  <form name="myForm">
    <table border="0" width="98%">
      <tr><td align="left" bgcolor="white">
         <input type="submit" name="communities" value="List Communities" />
      </td></tr>
  </table></form>
  <h2>Create a request:</h2>
  <form name="myForm1">
  <xsl:variable name="hasticket"><xsl:value-of select="string-length(.//operation)" /></xsl:variable>
  <table border="0" cellpadding="8" cellspacing="0">
     <tr><td>Operation</td>
         <td><select name="operation" onclick="changeOp()">
            <xsl:variable name="move">Move</xsl:variable>
            <xsl:variable name="remove">Remove</xsl:variable>
            <xsl:variable name="op"><xsl:value-of select=".//operation" /></xsl:variable>
            <xsl:element name="option">
              <xsl:if test="$op=$move">
                <xsl:attribute name="selected" />
              </xsl:if>
              <xsl:text>Move</xsl:text>
            </xsl:element>
            <xsl:element name="option">
                <xsl:if test="$op=$remove">
                   <xsl:attribute name="selected" />
                </xsl:if>
              <xsl:text>Remove</xsl:text>
            </xsl:element>
          </select>
         </td></tr>
     <tr>
       <td>Mobile Agent</td>
       <td><select name="mobileagent" onclick="changeAgent()">
         <xsl:apply-templates select=".//agent">
           <xsl:with-param name="select" select=".//mobileAgent" />
         </xsl:apply-templates>
       </select></td>
     </tr>
     <tr>
       <td>Origin Node</td>
       <td>
         <xsl:element name="input">
           <xsl:attribute name="name">orignode</xsl:attribute>
           <xsl:attribute name="type">text</xsl:attribute>
           <xsl:attribute name="size">20</xsl:attribute>
           <xsl:attribute name="value">
             <xsl:if test="$hasticket>0">
               <xsl:value-of select=".//origNode" />
             </xsl:if>
             <xsl:if test="not($hasticket>0)">
               <xsl:value-of select=".//agent[1]/@node" />
             </xsl:if>
           </xsl:attribute>
         </xsl:element>
       </td>
     </tr>
     <tr>
       <td>Destination Node</td>
       <td>
         <xsl:element name="select">
           <xsl:attribute name="name">destinationnode</xsl:attribute>
           <xsl:variable name="op"><xsl:value-of select=".//operation" /></xsl:variable>
           <xsl:variable name="remove">Remove</xsl:variable>
           <xsl:if test="$op=$remove">
               <xsl:attribute name="disabled">true</xsl:attribute>
           </xsl:if>
          <xsl:variable name="currentnode"><xsl:value-of select=".//agent[1]/@node" /></xsl:variable>
          <xsl:variable name="destNode"><xsl:value-of select=".//destNode" /></xsl:variable>
          <xsl:for-each select=".//node">
             <xsl:variable name="node"><xsl:value-of select="@name" /></xsl:variable>
             <xsl:element name="option">
               <xsl:if test="$node=$currentnode">
                 <xsl:attribute name="disabled">true</xsl:attribute>
               </xsl:if>
               <xsl:if test="$currentnode=$destNode">
                 <xsl:attribute name="selected">selected</xsl:attribute>
               </xsl:if>
               <xsl:value-of select="$node" />
             </xsl:element>
          </xsl:for-each>
       </xsl:element></td>
     </tr>
     <!--<tr><td>Force Restart</td>
         <td><select name="forcerestart">
            <xsl:variable name="true">true</xsl:variable>
            <xsl:variable name="false">false</xsl:variable>
            <xsl:variable name="fr"><xsl:value-of select=".//forceRestart" /></xsl:variable>
            <xsl:element name="option">
                <xsl:if test="$fr=$false">
                   <xsl:attribute name="selected" />
                </xsl:if>
              <xsl:text>false</xsl:text>
            </xsl:element>
            <xsl:element name="option">
              <xsl:if test="$fr=$true">
                <xsl:attribute name="selected" />
              </xsl:if>
              <xsl:text>true</xsl:text>
            </xsl:element>
          </select>
         </td></tr>-->
     <tr><td>
       <input type="submit" />
     </td></tr>
  </table>
  </form>
  <xsl:apply-templates select=".//AgentControls" />
  </body>
</html>
</xsl:template>

<xsl:template match="agent">
  <xsl:param name="select" />
  <xsl:variable name="name"><xsl:value-of select="@name" /></xsl:variable>
  <xsl:element name="option">
      <xsl:attribute name="label">
         <xsl:value-of select="@node" />
      </xsl:attribute>
      <xsl:if test="$name=$select">
        <xsl:attribute name="selected">selected</xsl:attribute>
      </xsl:if>
    <xsl:value-of select="@name" />
  </xsl:element>
</xsl:template>

<xsl:template match="AgentControls">
  <xsl:variable name="count"><xsl:value-of select="count(//AgentControl)" /></xsl:variable>
  <form name="myForm2">
  <xsl:if test="not($count=0)">
    <br /><h2>Current Requests:</h2>
    <table border="1" cellpadding="1" cellspacing="1" width="95%" bordercolordark="#660000" bordercolorlight="#cc9966">
      <tr>
        <th>UID</th>
        <th>Ticket</th>
        <th>Status</th>
        <th />
      </tr>
      <xsl:for-each select="AgentControl">
        <xsl:sort select="./UID" />
        <tr>
          <td><xsl:value-of select="./UID" /></td>
          <td><xsl:value-of select="./Ticket" /></td>
          <xsl:variable name="status"><xsl:value-of select="./Status" /></xsl:variable>
          <xsl:variable name="failure">FAILURE</xsl:variable>
          <xsl:variable name="progress">In progress</xsl:variable>
          <xsl:element name="td">
            <xsl:attribute name="bgcolor">
              <xsl:if test="$status=$failure">
                <xsl:text>#FFBBBB</xsl:text>
              </xsl:if>
              <xsl:if test="$status=$progress">
                <xsl:text>#FFFFBB</xsl:text>
              </xsl:if>
              <xsl:if test="not($status=$failure or $status=$progress)">
                <xsl:text>#BBFFBB</xsl:text>
              </xsl:if>
            </xsl:attribute>
            <xsl:value-of select="./Status" />
          </xsl:element>
          <td><xsl:element name="input">
            <xsl:attribute name="type">submit</xsl:attribute>
            <xsl:attribute name="name"><xsl:value-of select="./UID" /></xsl:attribute>
            <xsl:attribute name="value">remove</xsl:attribute>
          </xsl:element></td>
        </tr>
      </xsl:for-each>
    </table>
    <br /><center><input type="submit" name="action" value="refresh" /></center>
  </xsl:if>
  </form>
</xsl:template>

</xsl:stylesheet>
