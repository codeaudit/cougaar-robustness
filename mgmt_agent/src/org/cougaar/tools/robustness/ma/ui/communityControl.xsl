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
     //This function is called when user select one mobile agent. It fills the original
     //node field and disable relative node in destination.
     function changeAgent() {
       var x = document.forms.myForm1.mobileagent;
       var node = x.options[x.selectedIndex].label;
       var orignode = document.forms.myForm1.orignode;
       orignode.readOnly = false;
       orignode.value = node;
       orignode.readOnly = true;
       var destinationnode = document.forms.myForm1.destinationnode;
       var selected = false;
       for(var i=0; i &lt; destinationnode.length; i++) {
         //destinationnode.remove(i);
         var value = destinationnode.options[i].text;
         if(node == value) {
           destinationnode.options[i].disabled = true;
           destinationnode.options[i].selected = false;
         }
         else {
           destinationnode.options[i].disabled = false;
           if(selected == false) {
               destinationnode.options[i].selected = true;
               selected = true;
           }
           else
               destinationnode.options[i].selected = false;
         }
       }
     }

     //This option is called when user change selection in option field. If user
     //select 'remove', disable the destination field.
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
    <table border="0" width="98%">
      <tr>
        <form name="myForm"><td align="left" bgcolor="white">
         <input type="submit" name="showcommunity" value="Show Community Data" />
        </td></form>
        <form name="lbForm"><td align="left" bgcolor="white">
          <input type="submit" name="loadBalance" value="Load Balance" />
          <!--<xsl:element name="input">
            <xsl:attribute name="type">submit</xsl:attribute>
            <xsl:attribute name="name">loadBalance</xsl:attribute>
            <xsl:attribute name="value">Load Balance</xsl:attribute>
          </xsl:element>-->
        </td></form>
        <td width="65%" />
      </tr>
  </table>
  <h2>Create a request:</h2>
  <form name="myForm1">
  <xsl:variable name="hasticket"><xsl:value-of select="string-length(.//operation)" /></xsl:variable>
  <table border="0" cellpadding="8" cellspacing="0">
     <!-- Operation field: include two options: move and remove. -->
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
     <!--mobile agent field is a list of all agents in current robustness community, user
         can select one (or more?) at one time.-->
     <tr>
       <td>Mobile Agent</td>
       <td><select name="mobileagent" size="1" onclick="changeAgent()">
         <xsl:apply-templates select=".//agent">
           <xsl:with-param name="select" select=".//mobileAgent" />
         </xsl:apply-templates>
       </select></td>
     </tr>
     <!--Original node field is an uneditable field. It fills the value automatically
         when user selects the mobile agent.-->
     <tr>
       <td>Origin Node</td>
       <td>
         <xsl:element name="input">
           <xsl:attribute name="name">orignode</xsl:attribute>
           <xsl:attribute name="type">text</xsl:attribute>
           <xsl:attribute name="size">20</xsl:attribute>
           <xsl:attribute name="value">
             <xsl:if test="$hasticket>0">
               <xsl:variable name="mobileAgent"><xsl:value-of select=".//CurrentTicket/mobileAgent" /></xsl:variable>
               <xsl:value-of select=".//agent[@name=$mobileAgent]/location/@current" />
             </xsl:if>
             <xsl:if test="not($hasticket>0)">
               <xsl:value-of select=".//agent[1]/location/@current" />
             </xsl:if>
           </xsl:attribute>
         </xsl:element>
       </td>
     </tr>
     <!--Destionation node field is a list of all nodes in current robustness community,
         it is disabled when user selects 'remove' operation.-->
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
          <xsl:variable name="currentnode"><xsl:value-of select=".//agent[1]/location/@current" /></xsl:variable>
          <xsl:for-each select=".//healthMonitor">
            <xsl:variable name="nodeType">node</xsl:variable>
            <xsl:variable name="type"><xsl:value-of select="@type" /></xsl:variable>
            <xsl:if test="$type=$nodeType">
             <xsl:variable name="node"><xsl:value-of select="@name" /></xsl:variable>
             <xsl:variable name="dead">DEAD</xsl:variable>
             <xsl:variable name="state"><xsl:value-of select="./status/@state" /></xsl:variable>
             <xsl:if test="not($state=$dead)">
             <xsl:element name="option">
               <xsl:if test="$hasticket>0">
                 <xsl:variable name="mobileAgent"><xsl:value-of select="//CurrentTicket/mobileAgent" /></xsl:variable>
                 <xsl:variable name="mobilenode"><xsl:value-of select="//agent[@name=$mobileAgent]/location/@current" /></xsl:variable>
                 <xsl:if test="$node=$mobilenode">
                    <xsl:attribute name="disabled">true</xsl:attribute>
                 </xsl:if>
                 <xsl:if test="not($node=$mobilenode)">
		    <xsl:attribute name="distabled">false</xsl:attribute>
                    <xsl:attribute name="selected">selected</xsl:attribute>
                 </xsl:if>
               </xsl:if>
               <xsl:if test="not($hasticket>0)">
                 <xsl:if test="$node=$currentnode">
                   <xsl:attribute name="disabled">true</xsl:attribute>
                 </xsl:if>
                 <xsl:if test="not($node=$currentnode)">
                   <xsl:attribute name="disabled">false</xsl:attribute>
                   <xsl:attribute name="selected">selected</xsl:attribute>
                 </xsl:if>
               </xsl:if>
               <xsl:value-of select="$node" />
             </xsl:element>
             </xsl:if>
            </xsl:if>
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
         <xsl:value-of select="./location/@current" />
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
          <td align="center" valign="middle"><xsl:element name="input">
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

