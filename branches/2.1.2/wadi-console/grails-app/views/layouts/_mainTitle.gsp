<g:def var="selectedClusterName" value="${session[SelectClusterController.SESSION_KEY_SELECTED_CLUSTER_NAME]}" />
<g:if test="${null != selectedClusterName}">
	<h2>${pageProperty(name:'meta.title')} - ${selectedClusterName}</h2>
</g:if>
<g:else>
	<h2>${pageProperty(name:'meta.title')}</h2>
</g:else>
