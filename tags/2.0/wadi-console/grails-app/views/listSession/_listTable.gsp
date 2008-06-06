<table id="tableToFilter">
	<tr>
		<th>
			Session Name
			<g:render template="/shared/filter" model="[filterName:'sessionNameFilter', tableToFilter:'tableToFilter', index:0]"/>
		</th>
		<th>
			Contextualiser Name
			<g:render template="/shared/filter" model="[filterName:'contextualiserNameFilter', tableToFilter:'tableToFilter', index:1]"/>
		</th>
		<th>
			Contextualiser Index
			<g:render template="/shared/filter" model="[filterName:'contextualiserIndexFilter', tableToFilter:'tableToFilter', index:2]"/>
		</th>
		<th>
			Hosting Peer
			<g:render template="/shared/filter" model="[filterName:'hostingPeerFilter', tableToFilter:'tableToFilter', index:3]"/>
		</th>
	</tr>
	<g:each in="${sessions}" var="sessionInfo">
		<tr>
			<td>${sessionInfo.name}</td>
			<td>${sessionInfo.contextualiserName}</td>
			<td>${sessionInfo.contextualiserIndex}</td>
			<td>${sessionInfo.hostingPeer.name}</td>
		</tr>
	</g:each>
</table>