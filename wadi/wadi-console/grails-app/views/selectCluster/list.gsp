<html>
    <head>
        <meta name="layout" content="main"></meta>
        <meta name="title" content="Clusters"></meta>
    </head>
    <body>
	This page displays the available clusters. A cluster must be selected to enable the other functionalities.
	<p/>

	<table id="tableToFilter">
		<tr>
			<th>Cluster Name</th>
			<th>Peers</th>
			<th>Action</th>
		</tr>
		<g:def var="selectedClusterName" value="${session[SelectClusterController.SESSION_KEY_SELECTED_CLUSTER_NAME]}" />
	    <g:each in="${dispatchers}" var="dispatcher">
	    	<g:def var="cluster" value="${dispatcher.getCluster()}" />
	    	<g:def var="clusterName" value="${cluster.getClusterName()}" />
		    <tr>
		    	<td>${clusterName}</td>
		    	<td>
			    <g:each in="${cluster.getRemotePeers().values()}" var="peer">
			    	${peer.getName()}
			    	<br/>
			    </g:each>
			    ${cluster.getLocalPeer().getName()}
		    	</td>
		    	<td>
			    	<g:if test="${clusterName == selectedClusterName}">
			    		<a href="${createLink(action:'unselect')}">unselect</a>
					</g:if>
					<g:else>
			    		<a href="${createLink(action:'select', params:[clusterName:clusterName])}">select</a>
					</g:else>
		    	</td>
		    </tr>
	    </g:each>
    </table>
    </body>
</html>
