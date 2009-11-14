<table id="tableToFilter">
	<tr>
		<th>Partition Index</th>
		<th>
			Hosting Peer
			<g:render template="/shared/filter" model="[filterName:'hostingPeerFilter', tableToFilter:'tableToFilter', index:1]"/>
		</th>
		<th>
			Version
			<g:render template="/shared/filter" model="[filterName:'versionFilter', tableToFilter:'tableToFilter', index:2]"/>
		</th>
	</tr>
	<g:each in="${partitionBalancingInfo.getPartitionInfos()}" var="partitionInfo">
		<tr>
			<td>${partitionInfo.getIndex()}</td>
			<td>${partitionInfo.getOwner().getName()}</td>
			<td>${partitionInfo.getVersion()}</td>
		</tr>
	</g:each>
</table>
