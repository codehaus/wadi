<html>
    <head>
        <meta name="layout" content="main"></meta>
        <meta name="title" content="Service Spaces"></meta>
    </head>
    <body>
	This page displays the available ServiceSpaces and the peers hosting them.
	<p/>

	<table id="tableToFilter">
		<tr>
			<th>ServiceSpace</th>
			<th>Hosting Peers</th>
		</tr>
	    <g:each in="${nameToServiceSpaceInfos}" var="entry">
		    <g:def var="serviceSpaceName" value="${entry.getKey()}"/>
		    <g:def var="serviceSpaceInfos" value="${entry.getValue()}"/>
		    <tr>
		    	<td>${serviceSpaceName}</td>
		    	<td></td>
		    </tr>
		    <g:each in="${serviceSpaceInfos}" var="serviceSpaceInfo">
		    <tr>
		    	<td>&nbsp;</td>
		    	<td>${serviceSpaceInfo.getHostingPeer().getName()}</td>
		    </tr>
		    </g:each>
	    </g:each>
    </table>
    </body>
</html>
