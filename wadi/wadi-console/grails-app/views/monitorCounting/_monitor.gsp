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
