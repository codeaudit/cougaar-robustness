<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="html" indent="yes"/>

<xsl:variable name="community"><xsl:value-of select="//community/@name" /></xsl:variable>

<xsl:template match="/">
<html>
  <head>
    <title>
      <xsl:text>Community:</xsl:text>
      <xsl:value-of select="$community" />
    </title>
    <style type="text/css">
       tr  {background: window;}
       td  {color: windowtext; font: menu;}

       thead td	{background: buttonface; font: menu; font-weight: bold; border: 1px outset white;
                 cursor: default; padding-top: 0; padding: bottom: 0;
                 border-top: 1px solid buttonhighlight;
                 border-left: 1px solid buttonhighlight;
                 border-right: 1px solid buttonshadow;
                 border-bottom: 1px solid buttonshadow;
                 height: 12px;
                }
       thead .arrow  {font-family: webdings; color: black; padding: 0; font-size: 10px;
                      height: 11px; width: 10px; overflow: hidden;
                      margin-bottom: 2; margin-top: -3; padding: 0; padding-top: 0; padding-bottom: 2;}
    </style>
  </head>
  <body>


  <script language="JavaScript">
    var dom = (document.getElementsByTagName) ? true : false;
var ie5 = (document.getElementsByTagName &amp;&amp; document.all) ? true : false;
var arrowUp, arrowDown;

if (ie5 || dom)
	initSortTable();

function initSortTable() {
	arrowUp = document.createElement("SPAN");
	var tn = document.createTextNode("");
	arrowUp.appendChild(tn);
	arrowUp.className = "arrow";

	arrowDown = document.createElement("SPAN");
	var tn = document.createTextNode("");
	arrowDown.appendChild(tn);
	arrowDown.className = "arrow";
}



function sortTable(tableNode, nCol, bDesc, sType) {
	var tBody = tableNode.tBodies[0];
	var trs = tBody.rows;
	var trl= trs.length;
	var a = new Array();

        for (var i=0; i &lt; trl; i++) {a[i] = trs[i];}

	var start = new Date;
	window.status = "Sorting data...";
	a.sort(compareByColumn(nCol,bDesc,sType));
	window.status = "Sorting data done";

	for (var i = 0; i &lt; trl; i++) {
		tBody.appendChild(a[i]);
		window.status = "Updating row " + (i + 1) + " of " + trl +
						" (Time spent: " + (new Date - start) + "ms)";
	}

	// check for onsort
	if (typeof tableNode.onsort == "string")
		tableNode.onsort = new Function("", tableNode.onsort);
	if (typeof tableNode.onsort == "function")
		tableNode.onsort();
}

function CaseInsensitiveString(s) {
	return String(s).toUpperCase();
}

function parseDate(s) {
	return Date.parse(s.replace(/\-/g, '/'));
}

function toNumber(s) {
    return Number(s.replace(/[^0-9\.]/g, ""));
}

function compareByColumn(nCol, bDescending, sType) {
	var c = nCol;
	var d = bDescending;

	var fTypeCast = String;

	if (sType == "Number")
		fTypeCast = Number;
	else if (sType == "Date")
		fTypeCast = parseDate;
	else if (sType == "CaseInsensitiveString")
		fTypeCast = CaseInsensitiveString;

	return function (n1, n2) {
		if (fTypeCast(getInnerText(n1.cells[c])) &lt; fTypeCast(getInnerText(n2.cells[c])))
			return d ? -1 : +1;
		if (fTypeCast(getInnerText(n1.cells[c])) > fTypeCast(getInnerText(n2.cells[c])))
			return d ? +1 : -1;
		return 0;
	};
}

function sortColumnWithHold(e) {
	// find table element
	var el = ie5 ? e.srcElement : e.target;
	var table = getParent(el, "TABLE");

	// backup old cursor and onclick
	var oldCursor = table.style.cursor;
	var oldClick = table.onclick;

	// change cursor and onclick
	table.style.cursor = "wait";
	table.onclick = null;

	// the event object is destroyed after this thread but we only need
	// the srcElement and/or the target
	var fakeEvent = {srcElement : e.srcElement, target : e.target};

	// call sortColumn in a new thread to allow the ui thread to be updated
	// with the cursor/onclick
	window.setTimeout(function () {
		sortColumn(fakeEvent);
		// once done resore cursor and onclick
		table.style.cursor = oldCursor;
		table.onclick = oldClick;
	}, 100);
}

function sortColumn(e) {
	var tmp = e.target ? e.target : e.srcElement;
	var tHeadParent = getParent(tmp, "THEAD");
	var el = getParent(tmp, "TD");

	if (tHeadParent == null)
		return;

	if (el != null) {
		var p = el.parentNode;
		var i;

		// typecast to Boolean
		el._descending = !Boolean(el._descending);

		if (tHeadParent.arrow != null) {
			if (tHeadParent.arrow.parentNode != el) {
				tHeadParent.arrow.parentNode._descending = null;	//reset sort order
			}
			tHeadParent.arrow.parentNode.removeChild(tHeadParent.arrow);
		}

		if (el._descending)
			tHeadParent.arrow = arrowUp.cloneNode(true);
		else
			tHeadParent.arrow = arrowDown.cloneNode(true);

		el.appendChild(tHeadParent.arrow);



		// get the index of the td
		var cells = p.cells;
		var l = cells.length;
		for (i = 0; i &lt; l; i++) {
			if (cells[i] == el) break;
		}

		var table = getParent(el, "TABLE");

		sortTable(table,i,el._descending, el.getAttribute("type"));
	}
}


function getInnerText(el) {
	if (ie5) return el.innerText;	//Not needed but it is faster

	var str = "";

	var cs = el.childNodes;
	var l = cs.length;
	for (var i = 0; i &lt; l; i++) {
		switch (cs[i].nodeType) {
			case 1: //ELEMENT_NODE
				str += getInnerText(cs[i]);
				break;
			case 3:	//TEXT_NODE
				str += cs[i].nodeValue;
				break;
		}

	}

	return str;
}

function getParent(el, pTagName) {
	if (el == null) return null;
	else if (el.nodeType == 1 &amp;&amp; el.tagName.toLowerCase() == pTagName.toLowerCase())	// Gecko bug, supposed to be uppercase
		return el;
	else
		return getParent(el.parentNode, pTagName);
}
  </script>

  <form name="myForm">
    <table border="0" width="98%">
      <tr><td align="left" bgcolor="white">
         <input type="submit" name="communities" value="List Communities" />
      </td></tr>
  </table></form>
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
      <h2><xsl:text>NODES</xsl:text></h2>
      <table border="1" cellpadding="8" cellspacing="0">
        <thead><tr>
          <th style="width: 60px;" align="center" valign="middle"><xsl:text>NodeName</xsl:text></th>
          <th style="width: 60px;" align="center" valign="middle"><xsl:text>Status</xsl:text></th>
          <th style="width: 60px;" align="center" valign="middle"><xsl:text>Vote</xsl:text></th>
          <th style="width: 60px;" align="center" valign="middle"><xsl:text>Last</xsl:text></th>
          <th style="width: 60px;" align="center" valign="middle"><xsl:text>Expired</xsl:text></th>
        </tr></thead>
        <tbody>
        <xsl:apply-templates select=".//node">
           <xsl:with-param name="remoteNode"><xsl:value-of select=".//remoteNode" /></xsl:with-param>
        </xsl:apply-templates>
        </tbody>
      </table>
      <br />
      <h2><xsl:text>AGENTS</xsl:text></h2>
      <table border="0" cellpadding="0" cellspacing="0">
        <tr>
         <td height="26"></td>
         <xsl:element name="td">
           <xsl:attribute name="rowspan"><xsl:value-of select="count(//agent)+1" /></xsl:attribute>
           <xsl:attribute name="colspan">10</xsl:attribute>
      <table border="1" cellpadding="8" cellspacing="0" onclick="sortColumn(event)">
        <thead>
          <tr valign="middle">
            <td style="width: 60px;" align="center" valign="middle"><xsl:text>AgentName</xsl:text></td>
            <td style="width: 60px;" align="center" valign="middle"><xsl:text>Status</xsl:text></td>
            <td style="width: 60px;" align="center" valign="middle"><xsl:text>Node</xsl:text></td>
            <td style="width: 60px;" align="center" valign="middle"><xsl:text>Last</xsl:text></td>
            <td style="width: 60px;" align="center" valign="middle"><xsl:text>Expired</xsl:text></td>
          </tr>
        </thead>
        <tbody>
          <xsl:apply-templates select=".//agents" />
        </tbody>
       </table>
       </xsl:element></tr>
       <xsl:apply-templates select=".//agent" />
       </table>
    </center>
  </body>

</html>
</xsl:template>

<xsl:template match="agents">
  <xsl:for-each select="//agent">
     <xsl:sort select="@name" />
     <tr>
       <td><xsl:value-of select="@name" /></td>
       <td><xsl:value-of select="@status" /></td>
       <td><xsl:value-of select="@node" /></td>
       <td align="right"><xsl:value-of select="@last" /></td>
       <td><xsl:value-of select="@expired" /></td>
     </tr>
   </xsl:for-each>
</xsl:template>

<xsl:template match="agent">
  <tr>
    <td height="30" valign="middle"><xsl:number value="position()" format="1" /></td>
  </tr>
</xsl:template>

<xsl:template match="node">
  <xsl:param name="remoteNode" />
  <xsl:variable name="name"><xsl:value-of select="@name" /></xsl:variable>
  <xsl:element name="tr">
    <xsl:if test="$remoteNode=$name">
      <xsl:attribute name="style">
       <xsl:text>background-color:beige</xsl:text>
      </xsl:attribute>
    </xsl:if>
    <td>
      <xsl:element name="a">
          <xsl:attribute name="href">
            <xsl:text>./ar?showcommunity=</xsl:text>
            <xsl:value-of select="$community" />
            <xsl:text>&amp;node=</xsl:text>
            <xsl:value-of select="@name" />
          </xsl:attribute>
          <xsl:value-of select="@name" />
      </xsl:element>
    </td>
    <td><xsl:value-of select="@status" /></td>
    <td><xsl:value-of select="@vote" /></td>
    <td><xsl:value-of select="@last" /></td>
    <td><xsl:value-of select="@expired" /></td>
  </xsl:element>
</xsl:template>

</xsl:stylesheet>