<html>
    <head>
        <meta name="layout" content="main"></meta>
        <meta name="title" content="Service Spaces"></meta>
    </head>
    <body>
    This page displays the number of envelopes, i.e. messages, exchanged by peers.
	<p/>

	<table id="tableToFilter">
		<tr>
			<th>Hosting Peer</th>
			<th># of sent Envelopes</th>
			<th># of received Envelopes</th>
		</tr>
		<g:each in="${countingInfos}" var="countingInfo">
			<tr>
				<td>${countingInfo.hostingPeer.name}</td>
				<td>${countingInfo.outboundEnvelopeCpt}</td>
				<td>${countingInfo.inboundEnvelopeCpt}</td>
			</tr>
		</g:each>
	</table>
    </body>
</html>
