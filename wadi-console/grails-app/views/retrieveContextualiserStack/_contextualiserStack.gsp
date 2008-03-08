<table id="tableToFilter">
	<tr>
		<th>Contextualiser Name</th>
		<th>Contextualiser Index</th>
	</tr>
	<g:each in="${contextualiserStack}" var="contextualiserInfo">
		<tr>
			<td>${contextualiserInfo.getName()}</td>
			<td>${contextualiserInfo.getIndex()}</td>
		</tr>
	</g:each>
</table>
